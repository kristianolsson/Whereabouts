package com.kristianolsson.whereabouts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log

/**
 * Listens for network connectivity changes via a BroadcastReceiver registered
 * inside the running service — the same pattern WeekNow uses for ACTION_DATE_CHANGED.
 * This is more reliable than ConnectivityManager.NetworkCallback on Samsung devices,
 * which can be throttled in background even for foreground services.
 */
class NetworkMonitor(
    private val context: Context,
    private val onNetworkChange: () -> Unit
) {
    private val TAG = "NetworkMonitor"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Connectivity changed: ${intent?.action}")
            onNetworkChange()
        }
    }

    fun register() {
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        Log.d(TAG, "Connectivity BroadcastReceiver registered")
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Not registered or already unregistered — safe to ignore
        }
    }
}
