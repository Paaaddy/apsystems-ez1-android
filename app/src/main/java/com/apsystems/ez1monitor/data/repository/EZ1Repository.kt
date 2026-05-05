package com.apsystems.ez1monitor.data.repository

import com.apsystems.ez1monitor.data.api.EZ1ApiService
import com.apsystems.ez1monitor.data.api.models.AlarmInfo
import com.apsystems.ez1monitor.data.api.models.DeviceInfo
import com.apsystems.ez1monitor.data.api.models.OutputData
import com.apsystems.ez1monitor.data.api.models.toInverterOn
import com.apsystems.ez1monitor.data.api.models.toStatusParam
import com.google.gson.GsonBuilder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.apsystems.ez1monitor.BuildConfig
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class EZ1Result<out T> {
    data class Success<T>(val value: T) : EZ1Result<T>()
    data class Failure(val message: String, val cause: Throwable? = null) : EZ1Result<Nothing>()
}

/**
 * Handles all communication with the EZ1 inverter's local HTTP API.
 *
 * A Mutex serializes all network calls — prevents poll responses from
 * overwriting in-flight command results (or vice versa).
 *
 * Retrofit is rebuilt whenever the IP/port changes, since base URL is
 * determined at runtime from user configuration.
 */
class EZ1Repository {

    private val mutex = Mutex()
    private var currentBaseUrl: String = ""
    private var apiService: EZ1ApiService? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .build()
    }

    private fun getOrBuildService(ip: String, port: Int): EZ1ApiService {
        val baseUrl = "http://$ip:$port/"
        if (baseUrl != currentBaseUrl || apiService == null) {
            Timber.d("Building Retrofit for %s", baseUrl)
            val gson = GsonBuilder().setLenient().create()
            apiService = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(EZ1ApiService::class.java)
            currentBaseUrl = baseUrl
        }
        return apiService!!
    }

    suspend fun getDeviceInfo(ip: String, port: Int): EZ1Result<DeviceInfo> = mutex.withLock {
        runCatching {
            val service = getOrBuildService(ip, port)
            val response = service.getDeviceInfo()
            val data = response.data
                ?: return@withLock EZ1Result.Failure("Device returned no data")

            // Fingerprint check: verify this is actually an EZ1 by checking expected fields
            if (data.deviceId == null || data.maxPower == null || data.minPower == null) {
                return@withLock EZ1Result.Failure("Connected device is not an EZ1 inverter")
            }

            EZ1Result.Success(
                DeviceInfo(
                    deviceId = data.deviceId,
                    devVer = data.devVer ?: "Unknown",
                    ssid = data.ssid ?: "",
                    ipAddr = data.ipAddr ?: ip,
                    maxPower = data.maxPower,
                    minPower = data.minPower
                )
            )
        }.getOrElse { e ->
            Timber.e(e, "getDeviceInfo failed")
            EZ1Result.Failure(friendlyError(e), e)
        }
    }

    suspend fun getOutputData(ip: String, port: Int): EZ1Result<OutputData> = mutex.withLock {
        runCatching {
            val data = getOrBuildService(ip, port).getOutputData().data
                ?: return@withLock EZ1Result.Failure("No output data in response")
            EZ1Result.Success(
                OutputData(
                    p1 = data.p1 ?: 0f,
                    p2 = data.p2 ?: 0f,
                    e1 = data.e1 ?: 0f,
                    e2 = data.e2 ?: 0f,
                    te1 = data.te1 ?: 0f,
                    te2 = data.te2 ?: 0f
                )
            )
        }.getOrElse { e ->
            Timber.e(e, "getOutputData failed")
            EZ1Result.Failure(friendlyError(e), e)
        }
    }

    suspend fun getOnOff(ip: String, port: Int): EZ1Result<Boolean> = mutex.withLock {
        runCatching {
            val data = getOrBuildService(ip, port).getOnOff().data
                ?: return@withLock EZ1Result.Failure("No on/off data in response")
            val status = data.status ?: return@withLock EZ1Result.Failure("Missing status field")
            EZ1Result.Success(status.toInverterOn())
        }.getOrElse { e ->
            Timber.e(e, "getOnOff failed")
            EZ1Result.Failure(friendlyError(e), e)
        }
    }

    suspend fun setOnOff(ip: String, port: Int, on: Boolean): EZ1Result<Boolean> = mutex.withLock {
        runCatching {
            // on=true → status=0 (EZ1 convention: 0=on, 1=off)
            val statusParam = on.toStatusParam()
            Timber.d("setOnOff on=%b statusParam=%d", on, statusParam)
            val data = getOrBuildService(ip, port).setOnOff(statusParam).data
                ?: return@withLock EZ1Result.Failure("No response data")
            val newStatus = data.status ?: return@withLock EZ1Result.Success(on)
            EZ1Result.Success(newStatus.toInverterOn())
        }.getOrElse { e ->
            Timber.e(e, "setOnOff failed")
            EZ1Result.Failure(friendlyError(e), e)
        }
    }

    suspend fun getMaxPower(ip: String, port: Int): EZ1Result<Int> = mutex.withLock {
        runCatching {
            val data = getOrBuildService(ip, port).getMaxPower().data
                ?: return@withLock EZ1Result.Failure("No max power data")
            EZ1Result.Success(data.maxPower ?: 0)
        }.getOrElse { e ->
            Timber.e(e, "getMaxPower failed")
            EZ1Result.Failure(friendlyError(e), e)
        }
    }

    suspend fun setMaxPower(ip: String, port: Int, watts: Int, min: Int, max: Int): EZ1Result<Int> = mutex.withLock {
        val clamped = watts.coerceIn(min, max)
        runCatching {
            val data = getOrBuildService(ip, port).setMaxPower(clamped).data
                ?: return@withLock EZ1Result.Failure("No response data")
            EZ1Result.Success(data.maxPower ?: clamped)
        }.getOrElse { e ->
            Timber.e(e, "setMaxPower failed")
            EZ1Result.Failure(friendlyError(e), e)
        }
    }

    suspend fun getAlarm(ip: String, port: Int): EZ1Result<AlarmInfo> = mutex.withLock {
        runCatching {
            val data = getOrBuildService(ip, port).getAlarm().data
                ?: return@withLock EZ1Result.Failure("No alarm data")
            EZ1Result.Success(
                AlarmInfo(
                    offGrid = data.offGrid?.lowercase() == "alarm",
                    shortCircuit1 = data.shortCircuitError1?.lowercase() == "alarm",
                    shortCircuit2 = data.shortCircuitError2?.lowercase() == "alarm",
                    outputFault = data.outputFault?.lowercase() == "alarm"
                )
            )
        }.getOrElse { e ->
            Timber.e(e, "getAlarm failed")
            EZ1Result.Failure(friendlyError(e), e)
        }
    }

    private fun friendlyError(e: Throwable): String = when (e) {
        is java.net.SocketTimeoutException -> "Check you're on the same WiFi as the inverter"
        is java.net.ConnectException -> "Cannot reach device — check IP and port"
        is java.net.UnknownHostException -> "Device not found at this address"
        is IOException -> "Network error: ${e.message}"
        else -> "Unexpected error: ${e.message}"
    }
}
