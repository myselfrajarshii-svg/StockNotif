package com.stockalert

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.TimeZone

object AlarmScheduler {

    const val ACTION_STOCK_ALERT = "com.stockalert.ACTION_STOCK_ALERT"
    const val EXTRA_TIME_LABEL   = "time_label"
    const val EXTRA_ALARM_INDEX  = "alarm_index"

    /**
     * Schedules all user-configured alert times.
     * Cancels any existing alarms first to avoid duplicates.
     */
    fun scheduleAll(context: Context) {
        val times = PrefsManager.getTimes(context)
        cancelAll(context, times.size + 5)   // cancel a few extra slots to be safe

        if (!PrefsManager.isEnabled(context)) return

        times.forEachIndexed { index, timeStr ->
            scheduleSingle(context, timeStr, index)
        }
    }

    /**
     * Schedules a single daily alarm for [timeStr] = "HH:MM" in IST.
     */
    private fun scheduleSingle(context: Context, timeStr: String, index: Int) {
        val parts = timeStr.split(":")
        if (parts.size != 2) return
        val hour   = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_STOCK_ALERT
            putExtra(EXTRA_TIME_LABEL,  timeStr)
            putExtra(EXTRA_ALARM_INDEX, index)
        }

        val pi = PendingIntent.getBroadcast(
            context,
            index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build a Calendar in IST for today at HH:MM
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(ist).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE,      minute)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the time has already passed today, schedule for tomorrow
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // setExactAndAllowWhileIdle ensures it fires even in Doze mode
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pi
        )
    }

    fun cancelAll(context: Context, maxSlots: Int = 10) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until maxSlots) {
            val pi = PendingIntent.getBroadcast(
                context, i,
                Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_STOCK_ALERT
                },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: continue
            am.cancel(pi)
        }
    }
}
