package com.kristianolsson.whereabouts

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var textFlag: TextView
    private lateinit var textCountry: TextView
    private lateinit var textIp: TextView
    private lateinit var textLastUpdated: TextView
    private lateinit var btnRefresh: Button
    private lateinit var enableSwitch: Switch

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { refreshUi() }
    }

    private val notificationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startFlagService()
            } else {
                // Revert switch without triggering the listener
                enableSwitch.setOnCheckedChangeListener(null)
                enableSwitch.isChecked = false
                Prefs.setServiceEnabled(this, false)
                enableSwitch.setOnCheckedChangeListener(switchListener)
            }
        }

    private val switchListener = { _: android.widget.CompoundButton, isChecked: Boolean ->
        Prefs.setServiceEnabled(this, isChecked)
        if (isChecked) {
            requestNotificationPermAndStart()
        } else {
            stopService(Intent(this, FlagService::class.java))
        }
        Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textFlag = findViewById(R.id.textFlag)
        textCountry = findViewById(R.id.textCountry)
        textIp = findViewById(R.id.textIp)
        textLastUpdated = findViewById(R.id.textLastUpdated)
        btnRefresh = findViewById(R.id.btnRefresh)
        enableSwitch = findViewById(R.id.switchEnable)

        enableSwitch.isChecked = Prefs.isServiceEnabled(this)
        enableSwitch.setOnCheckedChangeListener(switchListener)

        btnRefresh.setOnClickListener {
            it.isEnabled = false
            startFlagService()
            it.postDelayed({ it.isEnabled = true }, 2_000)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
        if (Prefs.isServiceEnabled(this)) startFlagService()
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

    private fun refreshUi() {
        val code = Prefs.getCountryCode(this)
        val ip = Prefs.getIp(this)
        val ts = Prefs.getLastUpdated(this)

        textFlag.text = if (code.isNotEmpty()) FlagEmoji.flagForCountryCode(code) else "🏳"
        textCountry.text = if (code.isNotEmpty()) code else getString(R.string.unknown)
        textIp.text = ip.ifEmpty { "—" }
        textLastUpdated.text = if (ts > 0L) {
            getString(R.string.last_updated, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ts)))
        } else {
            getString(R.string.not_yet_updated)
        }
    }

    private fun requestNotificationPermAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startFlagService()
            requestBatteryOptimizationExemption()
        }
    }

    // Samsung (and other OEMs) throttle network callbacks in background unless the app
    // is exempted from battery optimization. Without this, the flag won't update when
    // the screen is off or the app isn't in the foreground.
    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
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
