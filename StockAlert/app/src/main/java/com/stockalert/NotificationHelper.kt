package com.stockalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID   = "stock_price_alerts"
    private const val CHANNEL_NAME = "Stock Price Alerts"
    private const val CHANNEL_DESC = "Scheduled stock price notifications"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH          // shows as heads-up popup
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
            enableLights(true)
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    /**
     * Posts a notification with all stock prices.
     * @param timeLabel e.g. "10:30 AM"
     * @param lines     list of formatted price strings
     */
    fun postPriceAlert(
        context: Context,
        timeLabel: String,
        lines: List<String>,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tap notification → open app
        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = lines.joinToString("\n")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📈 Stock Alert — $timeLabel IST")
            .setContentText(lines.firstOrNull() ?: "Prices updated")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setBigContentTitle("📈 Stock Alert — $timeLabel IST")
                    .setSummaryText("${lines.size} stocks")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(notificationId, notification)
    }
}
