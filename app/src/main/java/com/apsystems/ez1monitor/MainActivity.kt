package com.apsystems.ez1monitor

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apsystems.ez1monitor.data.notifications.NotificationHelper
import com.apsystems.ez1monitor.data.prefs.AppPrefsSource
import com.apsystems.ez1monitor.data.repository.EZ1DataSource
import com.apsystems.ez1monitor.ui.dashboard.DashboardScreen
import com.apsystems.ez1monitor.ui.dashboard.DashboardViewModel
import com.apsystems.ez1monitor.ui.debug.DebugScreen
import com.apsystems.ez1monitor.ui.debug.DebugViewModel
import com.apsystems.ez1monitor.ui.setup.SetupScreen
import com.apsystems.ez1monitor.ui.setup.SetupViewModel
import java.io.File

private object Routes {
    const val SETUP = "setup"
    const val DASHBOARD = "dashboard"
    const val DEBUG = "debug"
}

class SetupViewModelFactory(
    private val prefs: AppPrefsSource,
    private val dataSource: EZ1DataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SetupViewModel(prefs, dataSource) as T
}

class DashboardViewModelFactory(
    private val prefs: AppPrefsSource,
    private val dataSource: EZ1DataSource,
    private val notificationHelper: NotificationHelper
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DashboardViewModel(prefs, dataSource, notificationHelper) as T
}

class DebugViewModelFactory(
    private val logDir: File?,
    private val prefs: AppPrefsSource,
    private val appVersion: String,
    private val androidVersion: String,
    private val createShareUri: (File) -> Uri?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DebugViewModel(logDir, prefs, appVersion, androidVersion, createShareUri) as T
}

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled by NotificationHelper.canPost() check at post time */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val app = application as EZ1Application

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val savedIp by app.prefs.ipAddress.collectAsState(initial = null)
                val isDemoMode by app.prefs.isDemoMode.collectAsState(initial = null)

                if (savedIp == null || isDemoMode == null) return@MaterialTheme

                val startDestination = when {
                    isDemoMode!! -> Routes.DASHBOARD
                    savedIp!!.isNotBlank() -> Routes.DASHBOARD
                    else -> Routes.SETUP
                }

                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Routes.SETUP) {
                        val vm: SetupViewModel = viewModel(
                            factory = SetupViewModelFactory(app.prefs, app.repository)
                        )
                        SetupScreen(
                            viewModel = vm,
                            onConnected = {
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.SETUP) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.DASHBOARD) {
                        val currentDemoMode by app.prefs.isDemoMode.collectAsState(initial = false)
                        val dataSource = remember(currentDemoMode) {
                            DataSourceCreator.create(currentDemoMode)
                        }
                        val vm: DashboardViewModel = viewModel(
                            key = "dashboard-$currentDemoMode",
                            factory = DashboardViewModelFactory(app.prefs, dataSource, app.notificationHelper)
                        )
                        DashboardScreen(
                            viewModel = vm,
                            onNavigateToSettings = {
                                navController.navigate(Routes.SETUP)
                            },
                            onNavigateToDebug = if (BuildConfig.DEBUG) {
                                { navController.navigate(Routes.DEBUG) }
                            } else null
                        )
                    }

                    if (BuildConfig.DEBUG) {
                        composable(Routes.DEBUG) {
                            val vm: DebugViewModel = viewModel(
                                factory = DebugViewModelFactory(
                                    logDir = File(filesDir, "logs"),
                                    prefs = app.prefs,
                                    appVersion = BuildConfig.VERSION_NAME,
                                    androidVersion = android.os.Build.VERSION.RELEASE,
                                    createShareUri = { file ->
                                        runCatching {
                                            FileProvider.getUriForFile(
                                                this@MainActivity,
                                                "${packageName}.fileprovider",
                                                file
                                            )
                                        }.getOrNull()
                                    }
                                )
                            )
                            DebugScreen(
                                viewModel = vm,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
