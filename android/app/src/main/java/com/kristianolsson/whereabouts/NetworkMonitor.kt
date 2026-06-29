package com.kristianolsson.whereabouts

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

/**
 * Registers a ConnectivityManager.NetworkCallback that fires [onNetworkChange] whenever
 * any network becomes available or is lost (covers VPN connect/disconnect).
 */
class NetworkMonitor(
    context: Context,
    private val onNetworkChange: () -> Unit
) {
    private val TAG = "NetworkMonitor"
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available: $network")
            onNetworkChange()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost: $network")
            onNetworkChange()
        }

        // Fires when switching between networks (e.g. WiFi A → WiFi B) without
        // losing internet connectivity — onAvailable/onLost don't fire in that case.
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                Log.d(TAG, "Network capabilities changed (validated): $network")
                onNetworkChange()
            }
        }
    }

    fun register() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            cm.registerNetworkCallback(request, callback)
            Log.d(TAG, "NetworkCallback registered")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register NetworkCallback: ${e.message}")
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
