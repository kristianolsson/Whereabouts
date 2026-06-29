package com.kristianolsson.whereabouts

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GeoResult(val countryCode: String, val ip: String)

/**
 * Fetches the device's current public IP and derives its country code.
 *
 * Primary:  https://ipinfo.io/json   → {"country":"US","ip":"1.2.3.4",...}
 * Fallback: http://ip-api.com/json   → {"countryCode":"US","query":"1.2.3.4",...}
 *
 * Must be called from a background thread.
 */
object GeoLocator {
    private const val TAG = "GeoLocator"
    private const val PRIMARY_URL = "https://ipinfo.io/json"
    private const val FALLBACK_URL = "http://ip-api.com/json"
    private const val TIMEOUT_MS = 8_000

    fun lookup(): GeoResult? = tryPrimary() ?: tryFallback()

    private fun tryPrimary(): GeoResult? = try {
        val json = fetchJson(PRIMARY_URL)
        val obj = JSONObject(json)
        val country = obj.optString("country", "")
        val ip = obj.optString("ip", "")
        if (country.length == 2) GeoResult(country.uppercase(), ip) else null
    } catch (e: Exception) {
        Log.w(TAG, "Primary lookup failed: ${e.message}")
        null
    }

    private fun tryFallback(): GeoResult? = try {
        val json = fetchJson(FALLBACK_URL)
        val obj = JSONObject(json)
        val country = obj.optString("countryCode", "")
        val ip = obj.optString("query", "")
        if (country.length == 2) GeoResult(country.uppercase(), ip) else null
    } catch (e: Exception) {
        Log.w(TAG, "Fallback lookup failed: ${e.message}")
        null
    }

    private fun fetchJson(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        return try {
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }
}
