package com.stockalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android clears all AlarmManager alarms on reboot.
 * This receiver listens for BOOT_COMPLETED and reschedules them.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AlarmScheduler.scheduleAll(context)
        }
    }
}
