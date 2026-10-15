package com.mini.recorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mini.recorder.MainActivity
import com.mini.recorder.R

object RecordingNotifications {
    const val CHANNEL_RECORDING = "recording"
    const val CHANNEL_ALERTS = "alerts"
    const val NOTIF_ID = 1001
    const val ALERT_ID = 1002

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val recording = NotificationChannel(
            CHANNEL_RECORDING,
            "錄音狀態",
            NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            "錄音提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { enableVibration(true) }
        manager.createNotificationChannel(recording)
        manager.createNotificationChannel(alerts)
    }

    private fun serviceIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, RecordingService::class.java).setAction(action)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun recordingNotification(
        context: Context,
        title: String,
        subtitle: String,
        paused: Boolean
    ): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_stat_record)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(contentIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (paused) {
            builder.addAction(0, "繼續", serviceIntent(context, RecordingService.ACTION_RESUME, 11))
        } else {
            builder.addAction(0, "暫停", serviceIntent(context, RecordingService.ACTION_PAUSE, 12))
        }
        builder.addAction(0, "標記", serviceIntent(context, RecordingService.ACTION_MARK, 13))
        builder.addAction(0, "停止", serviceIntent(context, RecordingService.ACTION_STOP, 14))
        return builder.build()
    }

    fun placeholderNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_stat_record)
            .setContentTitle("準備錄音…")
            .setOngoing(true)
            .setSilent(true)
            .build()

    fun alert(context: Context, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_record)
            .setContentTitle("錄音提醒")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ALERT_ID, notification) }
    }

    fun cancel(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIF_ID) }
    }
}
