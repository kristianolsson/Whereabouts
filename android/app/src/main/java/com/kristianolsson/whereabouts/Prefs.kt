package com.kristianolsson.whereabouts

import android.content.Context

/**
 * SharedPreferences wrapper: persists the last known country code, IP, and
 * update timestamp so the correct flag is shown instantly on app/service start.
 */
object Prefs {
    private const val PREF_FILE = "whereabouts_prefs"
    private const val KEY_COUNTRY_CODE = "country_code"
    private const val KEY_IP = "ip"
    private const val KEY_LAST_UPDATED = "last_updated"

    fun save(context: Context, countryCode: String, ip: String) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_COUNTRY_CODE, countryCode)
            .putString(KEY_IP, ip)
            .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
            .apply()
    }

    fun getCountryCode(context: Context): String =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getString(KEY_COUNTRY_CODE, "") ?: ""

    fun getIp(context: Context): String =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getString(KEY_IP, "") ?: ""

    fun getLastUpdated(context: Context): Long =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_UPDATED, 0L)
}
