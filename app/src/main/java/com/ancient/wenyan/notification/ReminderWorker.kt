package com.ancient.wenyan.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.ancient.wenyan.MainActivity
import com.ancient.wenyan.R
import com.ancient.wenyan.data.WenYanRepository
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val repository = WenYanRepository.getInstance(context)
        val heatmapStats = repository.computeHeatmapStats()
        val stats = repository.computeStats()

        val title = "文言背诵 · 每日提醒"
        val message = when {
            heatmapStats.currentStreak > 0 -> "连胜已保持 ${heatmapStats.currentStreak} 天 🔥 今日还有 ${stats.dueCards} 句待复习，快来打卡吧！"
            stats.dueCards > 0 -> "温故而知新，今日有 ${stats.dueCards} 句古诗文等待复习！"
            else -> "日拱一卒，功不唐捐。今天来背诵一首新的古典诗文吧！"
        }

        sendNotification(title, message)
        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        // Notification permission check for Android 13+ (API 33) and system settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        if (!androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "每日背诵提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "每日定时提醒用户打卡并完成文言文 FSRS 间隔复习"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val CHANNEL_ID = "wenyan_reminder_channel"
        const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "wenyan_daily_reminder_work"

        fun scheduleDailyReminder(context: Context, hour: Int = 21, minute: Int = 0) {
            val now = LocalDateTime.now()
            var targetTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (!targetTime.isAfter(now)) {
                targetTime = targetTime.plusDays(1)
            }

            val initialDelayMinutes = maxOf(1L, Duration.between(now, targetTime).toMinutes())

            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancelDailyReminder(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
