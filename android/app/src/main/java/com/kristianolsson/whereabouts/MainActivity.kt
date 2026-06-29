package com.kristianolsson.whereabouts

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.text.DateFormat
import java.util.Date

/**
 * Simple UI that shows the current flag, IP, country code, and last-updated time.
 *
 * The UI refreshes via:
 *  - onResume (reads SharedPreferences)
 *  - a dynamic BroadcastReceiver that FlagService fires after each geo lookup
 *
 * The Refresh button starts/re-pings FlagService which immediately triggers a
 * new geo lookup (bypassing the 1.5 s debounce on first call from MainActivity).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var textFlag: TextView
    private lateinit var textCountry: TextView
    private lateinit var textIp: TextView
    private lateinit var textLastUpdated: TextView
    private lateinit var btnRefresh: Button

    // Receives broadcasts from FlagService while this activity is visible.
    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshUi()
        }
    }

    // Handles POST_NOTIFICATIONS permission on Android 13+.
    private val notificationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Start regardless of outcome; service handles missing permission gracefully.
            startFlagService()
        }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textFlag = findViewById(R.id.textFlag)
        textCountry = findViewById(R.id.textCountry)
        textIp = findViewById(R.id.textIp)
        textLastUpdated = findViewById(R.id.textLastUpdated)
        btnRefresh = findViewById(R.id.btnRefresh)

        btnRefresh.setOnClickListener {
            it.isEnabled = false
            startFlagService()
            // Re-enable after a short wait so users can't spam taps
            it.postDelayed({ it.isEnabled = true }, 2_000)
        }

        requestNotificationPermAndStart()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
        val filter = IntentFilter(FlagService.BROADCAST_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(updateReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(updateReceiver) } catch (_: Exception) { }
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------

    private fun refreshUi() {
        val code = Prefs.getCountryCode(this)
        val ip = Prefs.getIp(this)
        val ts = Prefs.getLastUpdated(this)

        textFlag.text = if (code.isNotEmpty()) FlagEmoji.flagForCountryCode(code) else "🏳"
        textCountry.text = if (code.isNotEmpty()) code else getString(R.string.unknown)
        textIp.text = ip.ifEmpty { "—" }
        textLastUpdated.text = if (ts > 0L) {
            getString(
                R.string.last_updated,
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ts))
            )
        } else {
            getString(R.string.not_yet_updated)
        }
    }

    // -------------------------------------------------------------------------
    // Service
    // -------------------------------------------------------------------------

    private fun requestNotificationPermAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startFlagService()
        }
    }

    private fun startFlagService() {
        val intent = Intent(this, FlagService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
