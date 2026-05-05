package com.apsystems.ez1monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apsystems.ez1monitor.data.prefs.AppPreferences
import com.apsystems.ez1monitor.data.repository.EZ1Repository
import com.apsystems.ez1monitor.ui.dashboard.DashboardScreen
import com.apsystems.ez1monitor.ui.dashboard.DashboardViewModel
import com.apsystems.ez1monitor.ui.setup.SetupScreen
import com.apsystems.ez1monitor.ui.setup.SetupViewModel

private object Routes {
    const val SETUP = "setup"
    const val DASHBOARD = "dashboard"
}

class SetupViewModelFactory(
    private val prefs: AppPreferences,
    private val repository: EZ1Repository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SetupViewModel(prefs, repository) as T
}

class DashboardViewModelFactory(
    private val prefs: AppPreferences,
    private val repository: EZ1Repository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DashboardViewModel(prefs, repository) as T
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as EZ1Application

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val savedIp by app.prefs.ipAddress.collectAsState(initial = null)

                // Still loading from DataStore
                if (savedIp == null) return@MaterialTheme

                val startDestination =
                    if (savedIp!!.isNotBlank()) Routes.DASHBOARD else Routes.SETUP

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
                        val vm: DashboardViewModel = viewModel(
                            factory = DashboardViewModelFactory(app.prefs, app.repository)
                        )
                        DashboardScreen(
                            viewModel = vm,
                            onNavigateToSettings = {
                                navController.navigate(Routes.SETUP)
                            }
                        )
                    }
                }
            }
        }
    }
}
