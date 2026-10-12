package org.agripedia.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.agripedia.app.AgriPediaApp
import org.agripedia.app.MainActivity
import org.agripedia.app.R
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Local reminders. They only concern tasks present in the user's own calendar
 * (created by the user or derived from the dates of the user's crops) — no
 * universal agronomic reminder is ever sent.
 */
object Reminders {
    const val CHANNEL_ID = "tasks"
    private const val WORK_NAME = "daily-task-reminders"
    private const val SUMMARY_ID = 1000

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(R.string.notif_channel_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notifyDueTasks(context: Context, titles: List<String>, overdue: Int) {
        if (titles.isEmpty() || !canPost(context)) return
        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_CALENDAR, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val style = NotificationCompat.InboxStyle()
        titles.take(6).forEach { style.addLine(it) }
        val text = if (overdue > 0) context.resources.getQuantityString(R.plurals.notif_overdue, overdue, overdue)
        else context.resources.getQuantityString(R.plurals.notif_due, titles.size, titles.size)
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(text)
            .setStyle(style)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(SUMMARY_ID, n)
        } catch (_: SecurityException) { /* permission revoked meanwhile */ }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as AgriPediaApp).container
        if (!container.settings.settings.first().notificationsEnabled) return Result.success()
        val today = LocalDate.now()
        val due = container.userRepository.dueReminders(today)
        val overdue = due.count { it.dueDate < today.toEpochDay() }
        Reminders.notifyDueTasks(applicationContext, due.map { it.title }, overdue)
        return Result.success()
    }
}
