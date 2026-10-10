package com.kitchenguard360.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kitchenguard360.app.KitchenGuardApplication
import com.kitchenguard360.app.MainActivity
import com.kitchenguard360.app.data.model.SafetyLevel

class NotificationHelper(
    private val context: Context = KitchenGuardApplication.instance
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kitchen Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications for LPG gas leaks, smoke, and flame emergencies"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSafetyAlertNotification(
        title: String,
        message: String,
        severity: SafetyLevel
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = when (severity) {
            SafetyLevel.DANGER, SafetyLevel.SENSOR_FAULT, SafetyLevel.OFFLINE -> android.R.drawable.ic_dialog_alert
            SafetyLevel.WARNING -> android.R.drawable.ic_dialog_info
            SafetyLevel.SAFE -> android.R.drawable.ic_menu_info_details
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        val notificationId = when (severity) {
            SafetyLevel.DANGER -> 1001
            SafetyLevel.WARNING -> 1002
            SafetyLevel.SENSOR_FAULT -> 1003
            SafetyLevel.OFFLINE -> 1004
            SafetyLevel.SAFE -> 1000
        }

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "kitchenguard360_safety_alerts"
    }
}
