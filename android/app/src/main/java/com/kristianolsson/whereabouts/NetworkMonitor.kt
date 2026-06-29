package com.kristianolsson.whereabouts

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Watches the device's default network using registerDefaultNetworkCallback.
 * This fires when the active network changes — WiFi switch, VPN connect/disconnect —
 * which is exactly when the public IP may have changed. More reliable than
 * registerNetworkCallback on Samsung devices.
 */
class NetworkMonitor(
    context: Context,
    private val onNetworkChange: () -> Unit
) {
    private val TAG = "NetworkMonitor"
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Default network available: $network")
            onNetworkChange()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Default network lost: $network")
            onNetworkChange()
        }
    }

    fun register() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                cm.registerDefaultNetworkCallback(callback, handler)
            } else {
                cm.registerDefaultNetworkCallback(callback)
            }
            Log.d(TAG, "Default network callback registered")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    fun unregister() {
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            // Not registered or already unregistered — safe to ignore
        }
    }
}
