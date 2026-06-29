package com.kristianolsson.whereabouts

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

/**
 * Foreground Service that keeps the country flag visible in the status bar.
 *
 * Lifecycle:
 *  - onCreate: create notification channel, register NetworkCallback
 *  - onStartCommand: post initial notification (with cached flag), schedule refresh + alarm
 *  - onDestroy: unregister NetworkCallback, cancel alarm
 *
 * Network changes are debounced 1.5 s before querying ipinfo.io/ip-api.com.
 * AlarmManager backstop fires every 30 min in case a network event is missed.
 */
class FlagService : Service() {

    companion object {
        private const val TAG = "FlagService"
        private const val CHANNEL_ID = "whereabouts_flag_v1"
        const val NOTIFICATION_ID = 1001
        const val ACTION_REFRESH = "com.kristianolsson.whereabouts.ACTION_REFRESH"

        /** Broadcast sent to MainActivity (if alive) whenever the flag updates. */
        const val BROADCAST_UPDATED = "com.kristianolsson.whereabouts.UPDATED"

        private const val ALARM_REQUEST_CODE = 42
        private const val DEBOUNCE_MS = 5_000L
        private const val RETRY_DELAY_MS = 8_000L
        private const val MAX_RETRIES = 3
        private const val REFRESH_INTERVAL_MS = 30 * 60 * 1_000L

        private const val ICON_SIZE_PX = 128
    }

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = Runnable { doGeoLookup(attempt = 1) }
    private lateinit var networkMonitor: NetworkMonitor

    // -------------------------------------------------------------------------
    // Service lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        networkMonitor = NetworkMonitor(this) { scheduleRefresh() }
        networkMonitor.register()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")

        // Show the cached flag immediately so there is never a gap.
        val notification = buildNotification(Prefs.getCountryCode(this))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Always kick off a geo lookup (debounced so rapid calls collapse).
        scheduleRefresh()
        scheduleAlarm()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.unregister()
        handler.removeCallbacks(refreshRunnable)
        cancelAlarm()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // -------------------------------------------------------------------------
    // Geo lookup
    // -------------------------------------------------------------------------

    private fun scheduleRefresh() {
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, DEBOUNCE_MS)
    }

    private fun doGeoLookup(attempt: Int) {
        Thread {
            Log.d(TAG, "Geo lookup attempt $attempt")
            val result = GeoLocator.lookup()
            if (result != null) {
                Log.d(TAG, "Geo result: ${result.countryCode} / ${result.ip}")
                Prefs.save(this, result.countryCode, result.ip)
                updateNotification(result.countryCode)
                sendBroadcast(Intent(BROADCAST_UPDATED).apply { setPackage(packageName) })
            } else if (attempt < MAX_RETRIES) {
                Log.w(TAG, "Geo lookup failed (attempt $attempt) — retrying in ${RETRY_DELAY_MS}ms")
                handler.postDelayed({ doGeoLookup(attempt + 1) }, RETRY_DELAY_MS)
            } else {
                Log.w(TAG, "Geo lookup failed after $MAX_RETRIES attempts — keeping cached flag")
            }
        }.start()
    }

    // -------------------------------------------------------------------------
    // Notification
    // -------------------------------------------------------------------------

    private fun updateNotification(countryCode: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(countryCode))
    }

    private fun buildNotification(countryCode: String): Notification {
        val flag = if (countryCode.isNotEmpty()) FlagEmoji.flagForCountryCode(countryCode) else "🏳"
        val smallIcon = buildFlagIcon(flag)

        val openApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapIntent = PendingIntent.getActivity(
            this, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (countryCode.isNotEmpty()) "$flag  $countryCode" else "Detecting location…"

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText("Tap to open Whereabouts")
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    /**
     * Renders [flagEmoji] into a 128×128 bitmap suitable for use as a
     * notification small icon via [Icon.createWithBitmap].
     *
     * The emoji is drawn at full colour; Android displays it as a coloured
     * icon in the notification shade and as a white silhouette in the status
     * bar chip (system behaviour — same trade-off WeekNow uses for digits).
     */
    private fun buildFlagIcon(flagEmoji: String): Icon {
        val bmp = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = ICON_SIZE_PX * 0.80f
        }
        val x = ICON_SIZE_PX / 2f
        val y = ICON_SIZE_PX / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(flagEmoji, x, y, paint)
        return Icon.createWithBitmap(bmp)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Country Flag",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows the current country flag in the status bar"
            setShowBadge(false)
            setSound(null, AudioAttributes.Builder().build())
            enableVibration(false)
            enableLights(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // -------------------------------------------------------------------------
    // AlarmManager backstop (30-min periodic refresh)
    // -------------------------------------------------------------------------

    private fun scheduleAlarm() {
        val am = getSystemService(AlarmManager::class.java)
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + REFRESH_INTERVAL_MS,
            REFRESH_INTERVAL_MS,
            alarmPendingIntent()
        )
    }

    private fun cancelAlarm() {
        try {
            getSystemService(AlarmManager::class.java).cancel(alarmPendingIntent())
        } catch (e: Exception) { /* ignore */ }
    }

    private fun alarmPendingIntent(): PendingIntent {
        val intent = Intent(this, FlagService::class.java).apply {
            action = ACTION_REFRESH
        }
        return PendingIntent.getService(
            this, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
