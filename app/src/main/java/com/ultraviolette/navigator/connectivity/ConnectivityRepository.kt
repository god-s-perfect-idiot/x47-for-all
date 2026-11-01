package com.ultraviolette.navigator.connectivity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ConnectivitySample(
    /** Cellular bars 0–5. Zero when no cell radio / airplane. */
    val cellularBars: Int = 0,
    /** True when a cellular transport is up (SIM / registered). */
    val cellularAvailable: Boolean = false,
    /** True when associated to a Wi‑Fi access point. */
    val wifiConnected: Boolean = false,
    /** Wi‑Fi strength 0–3 (outer→inner arcs). Meaningful when connected. */
    val wifiBars: Int = 0,
    /** True while at least one Bluetooth profile / ACL link is connected. */
    val bluetoothConnected: Boolean = false
)

/**
 * Live cellular + Wi‑Fi + Bluetooth status for the cluster top bar.
 *
 * Cellular bars prefer [TelephonyManager] signal level when phone-state
 * permission is granted; otherwise they fall back to transport availability.
 * Wi‑Fi uses [ConnectivityManager] + RSSI from [WifiInfo].
 * Bluetooth uses profile connection state + ACL broadcasts.
 */
class ConnectivityRepository(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager =
        appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val wifiManager =
        appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothAdapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun hasPhoneStatePermission(): Boolean {
        val phone = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (phone) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.READ_BASIC_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    fun hasBluetoothConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun observeConnectivity(): Flow<ConnectivitySample> = callbackFlow {
        val knownNetworks = linkedSetOf<Network>()
        var cellularTransport = false
        var wifiTransport = false
        var cellularLevel: Int? = null // Android 0–4 when known
        var wifiRssi: Int? = null
        var bluetoothConnected = readBluetoothConnected()

        fun publish() {
            val bars = when {
                cellularLevel != null -> mapCellularLevelToBars(cellularLevel!!)
                cellularTransport -> FALLBACK_CELLULAR_BARS
                else -> 0
            }
            trySend(
                ConnectivitySample(
                    cellularBars = bars,
                    cellularAvailable = cellularTransport || bars > 0,
                    wifiConnected = wifiTransport,
                    wifiBars = if (wifiTransport) mapWifiRssiToBars(wifiRssi) else 0,
                    bluetoothConnected = bluetoothConnected
                )
            )
        }

        fun refreshWifiRssiFromActive() {
            wifiRssi = readWifiRssi()
        }

        fun refreshTransports() {
            cellularTransport = false
            wifiTransport = false
            wifiRssi = null
            val stale = mutableListOf<Network>()
            knownNetworks.forEach { n ->
                val c = connectivityManager.getNetworkCapabilities(n)
                if (c == null) {
                    stale += n
                    return@forEach
                }
                if (c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    cellularTransport = true
                }
                if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    wifiTransport = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        (c.transportInfo as? WifiInfo)?.rssi?.let { wifiRssi = it }
                    }
                }
            }
            knownNetworks.removeAll(stale.toSet())
            if (wifiTransport && wifiRssi == null) refreshWifiRssiFromActive()
            publish()
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                knownNetworks += network
                refreshTransports()
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                knownNetworks += network
                refreshTransports()
            }

            override fun onLost(network: Network) {
                knownNetworks -= network
                refreshTransports()
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        // Seed capabilities for networks the callback has already delivered.
        refreshTransports()

        val telephonyCloser = startTelephonyListen { level ->
            cellularLevel = level
            publish()
        }

        val bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                bluetoothConnected = readBluetoothConnected()
                publish()
            }
        }
        val bluetoothFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dpConnectionAction)
            addAction(BluetoothHeadsetConnectionAction)
        }
        ContextCompat.registerReceiver(
            appContext,
            bluetoothReceiver,
            bluetoothFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        bluetoothConnected = readBluetoothConnected()
        publish()

        awaitClose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            telephonyCloser()
            runCatching { appContext.unregisterReceiver(bluetoothReceiver) }
        }
    }

    private fun readBluetoothConnected(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!hasBluetoothConnectPermission()) return false
        return runCatching {
            if (!adapter.isEnabled) return false
            ConnectedProfiles.any { profile ->
                adapter.getProfileConnectionState(profile) == BluetoothProfile.STATE_CONNECTED
            }
        }.getOrDefault(false)
    }

    private fun startTelephonyListen(onLevel: (Int) -> Unit): () -> Unit {
        if (!hasPhoneStatePermission()) return {}

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    onLevel(signalStrength.level.coerceIn(0, 4))
                }
            }
            runCatching {
                telephonyManager.registerTelephonyCallback(appContext.mainExecutor, callback)
            }
            // Seed current level when available.
            runCatching {
                telephonyManager.signalStrength?.level?.let { onLevel(it.coerceIn(0, 4)) }
            }
            ({ runCatching { telephonyManager.unregisterTelephonyCallback(callback) } })
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    onLevel(signalStrength.level.coerceIn(0, 4))
                }
            }
            @Suppress("DEPRECATION")
            runCatching {
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            }
            ({
                @Suppress("DEPRECATION")
                runCatching {
                    telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
                }
            })
        }
    }

    @Suppress("DEPRECATION")
    private fun readWifiRssi(): Int? {
        return runCatching {
            if (!wifiManager.isWifiEnabled) return null
            val info = wifiManager.connectionInfo ?: return null
            // Not associated.
            if (info.networkId == -1) return null
            info.rssi
        }.getOrNull()
    }

    private companion object {
        /** When cell is up but we cannot read ASU/RSSI. */
        const val FALLBACK_CELLULAR_BARS = 3

        val ConnectedProfiles: IntArray = buildList {
            add(BluetoothProfile.A2DP)
            add(BluetoothProfile.HEADSET)
            add(BluetoothProfile.GATT)
            add(BluetoothProfile.SAP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(BluetoothProfile.HID_DEVICE)
            }
        }.toIntArray()

        /** Profile-specific connection broadcasts (string form — avoid SDK coupling). */
        const val BluetoothA2dpConnectionAction =
            "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED"
        const val BluetoothHeadsetConnectionAction =
            "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED"

        /** Android level 0–4 → five cluster bars. */
        fun mapCellularLevelToBars(level: Int): Int = when (level.coerceIn(0, 4)) {
            0 -> 0
            1 -> 1
            2 -> 2
            3 -> 4
            else -> 5
        }

        /** RSSI → 0–3 wifi arcs. Missing RSSI still shows a mid strength. */
        fun mapWifiRssiToBars(rssi: Int?): Int {
            if (rssi == null) return 2
            @Suppress("DEPRECATION")
            return WifiManager.calculateSignalLevel(rssi, 4).coerceIn(0, 3)
        }
    }
}
