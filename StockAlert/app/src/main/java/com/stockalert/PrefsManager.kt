package com.stockalert

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user preferences: stock symbols and alert times.
 * All times are stored as IST (HH:MM strings).
 */
object PrefsManager {

    private const val PREFS_NAME  = "StockAlertPrefs"
    private const val KEY_STOCKS  = "stocks"        // comma-separated symbols
    private const val KEY_TIMES   = "alert_times"   // comma-separated "HH:MM" strings
    private const val KEY_ENABLED = "alerts_enabled"

    // Default stocks (Yahoo Finance format for NSE: symbol.NS)
    private val DEFAULT_STOCKS = listOf(
        "NIFTYBEES.NS",
        "JUNIORBEES.NS",
        "BANKBEES.NS",
        "GOLDBEES.NS",
        "SETFNIF50.NS"
    )

    // Default alert times in IST
    private val DEFAULT_TIMES = listOf("10:30", "15:00")

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStocks(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_STOCKS, null)
        return if (raw.isNullOrBlank()) DEFAULT_STOCKS
        else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setStocks(context: Context, stocks: List<String>) {
        prefs(context).edit().putString(KEY_STOCKS, stocks.joinToString(",")).apply()
    }

    fun getTimes(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_TIMES, null)
        return if (raw.isNullOrBlank()) DEFAULT_TIMES
        else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setTimes(context: Context, times: List<String>) {
        prefs(context).edit().putString(KEY_TIMES, times.joinToString(",")).apply()
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
