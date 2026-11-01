package com.ultraviolette.navigator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultraviolette.navigator.ui.ClusterViewModel
import com.ultraviolette.navigator.ui.SpeedometerScreen
import com.ultraviolette.navigator.ui.theme.UltravioletteNavigatorTheme

class MainActivity : ComponentActivity() {
    private val clusterViewModel: ClusterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureFullscreen()

        setContent {
            val data by clusterViewModel.data.collectAsStateWithLifecycle()
            UltravioletteNavigatorTheme(
                darkTheme = data.isDarkMode,
                brandTheme = data.brandTheme
            ) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    clusterViewModel.onLocationPermissionResult(granted)
                }

                val phoneStateLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    val granted = result[Manifest.permission.READ_PHONE_STATE] == true ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            result[Manifest.permission.READ_BASIC_PHONE_STATE] == true)
                    clusterViewModel.onPhoneStatePermissionResult(granted)
                }

                val bluetoothLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    clusterViewModel.onBluetoothPermissionResult(granted)
                }

                LaunchedEffect(Unit) {
                    if (hasLocationPermission()) {
                        clusterViewModel.startSpeedUpdates()
                        clusterViewModel.startWeatherUpdates()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                    if (!hasPhoneStatePermission()) {
                        phoneStateLauncher.launch(phoneStatePermissions())
                    } else {
                        clusterViewModel.onPhoneStatePermissionResult(true)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (!hasBluetoothConnectPermission()) {
                            bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            clusterViewModel.onBluetoothPermissionResult(true)
                        }
                    }
                }

                val trips by clusterViewModel.trips.collectAsStateWithLifecycle()
                SpeedometerScreen(
                    data = data,
                    trips = trips,
                    onRideModeClick = { clusterViewModel.cycleRideMode() },
                    onRideModeSelect = { clusterViewModel.setRideMode(it) },
                    onEndRide = { clusterViewModel.endCurrentTrip() },
                    onBikeNameChange = { clusterViewModel.setBikeName(it) },
                    onBrandThemeChange = { clusterViewModel.setBrandTheme(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        if (hasLocationPermission()) {
            clusterViewModel.startSpeedUpdates()
            clusterViewModel.startWeatherUpdates()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return fine == PackageManager.PERMISSION_GRANTED ||
            coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun hasPhoneStatePermission(): Boolean {
        val phone = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (phone) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_BASIC_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    private fun phoneStatePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_BASIC_PHONE_STATE
            )
        } else {
            arrayOf(Manifest.permission.READ_PHONE_STATE)
        }

    private fun hasBluetoothConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun configureFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
