package com.stockalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import java.util.Calendar
import java.util.TimeZone

/**
 * Fires when AlarmManager triggers at a scheduled IST time.
 * 1. Checks if today is a market day (Mon–Fri, within NSE hours)
 * 2. Fetches prices from Yahoo Finance on a background thread
 * 3. Posts a push notification with all prices
 * 4. Reschedules itself for the same time tomorrow
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_STOCK_ALERT) return

        val timeLabel  = intent.getStringExtra(AlarmScheduler.EXTRA_TIME_LABEL) ?: "?"
        val alarmIndex = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_INDEX, 0)

        // Acquire WakeLock so the device stays awake while we do network I/O
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "StockAlert:AlarmReceiver"
        )
        wl.acquire(30_000L)  // max 30 seconds

        // Use goAsync() so we can do work off the main thread
        val pending = goAsync()

        Thread {
            try {
                if (isMarketDay()) {
                    fetchAndNotify(context, timeLabel)
                }
                // Reschedule for same time tomorrow
                rescheduleForTomorrow(context, timeLabel, alarmIndex)
            } finally {
                wl.release()
                pending.finish()
            }
        }.start()
    }

    /** NSE is open Mon–Fri. We skip weekends (and rely on Yahoo Finance returning
     *  stale/no data for public holidays, which we handle gracefully). */
    private fun isMarketDay(): Boolean {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(ist)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
    }

    private fun fetchAndNotify(context: Context, timeLabel: String) {
        val symbols = PrefsManager.getStocks(context)
        val prices  = StockFetcher.fetchPrices(symbols)

        val lines = symbols.map { sym ->
            val key = sym.trim().uppercase()
            StockFetcher.formatLine(sym, prices[key])
        }

        // Format "10:30" → "10:30 AM" / "15:00" → "3:00 PM"
        val displayTime = formatDisplayTime(timeLabel)

        NotificationHelper.postPriceAlert(
            context     = context,
            timeLabel   = displayTime,
            lines       = lines,
            notificationId = (timeLabel.replace(":", "").toIntOrNull() ?: 1030)
        )
    }

    private fun formatDisplayTime(timeStr: String): String {
        val parts  = timeStr.split(":")
        val hour   = parts.getOrNull(0)?.toIntOrNull() ?: return timeStr
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val amPm   = if (hour < 12) "AM" else "PM"
        val h12    = when {
            hour == 0  -> 12
            hour > 12  -> hour - 12
            else       -> hour
        }
        return "%d:%02d %s".format(h12, minute, amPm)
    }

    private fun rescheduleForTomorrow(context: Context, timeLabel: String, index: Int) {
        // Just re-run full schedule — it's idempotent
        AlarmScheduler.scheduleAll(context)
    }
}
