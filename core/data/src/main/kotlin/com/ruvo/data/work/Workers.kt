package com.ruvo.data.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
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
import com.ruvo.data.RuvoGraph
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/** Daily runway snapshot for the monthly review chart. */
class SnapshotWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        RuvoGraph.init(applicationContext)
        RuvoGraph.repository.refreshDay()
        RuvoGraph.repository.state.first { it.ready }
        RuvoGraph.repository.recordSnapshot()
        return Result.success()
    }
}

/** Sunday 7 pm: "Your week is ready to sort." Quiet hours and the two-a-day ceiling are respected by design. */
@SuppressLint("MissingPermission") // checked before notifying
class WeeklySortWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        RuvoGraph.init(applicationContext)
        val state = RuvoGraph.repository.state.first { it.ready }
        val count = state.toSort.size
        if (count == 0 || !state.settings.notifyWeekly) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Weekly sort", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val launch = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?.putExtra("com.ruvo.extra.OPEN", "sort")
        val pi = PendingIntent.getActivity(applicationContext, 7, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("Your week is ready to sort")
            .setContentText("$count payments, about two minutes. Your runway gets sharper.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(applicationContext).notify(7, n) }
        return Result.success()
    }

    companion object {
        const val CHANNEL = "ruvo_weekly"
    }
}

/**
 * Every morning at 9: new student offers and new Guide books (only ones not announced before), then the daily update
 * with the runway, today's amount and payments waiting to be confirmed. Each kind has its own switch in Settings and
 * its own channel, so the phone's settings can silence one without the others.
 */
@SuppressLint("MissingPermission") // checked in post()
class MorningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        RuvoGraph.init(applicationContext)
        RuvoGraph.repository.refreshDay()
        val state = RuvoGraph.repository.state.first { it.ready }
        val s = state.settings
        if (!s.onboarded) return Result.success()
        val (offers, books) = RuvoGraph.repository.takeNews()
        if (s.notifyNews) {
            offers.firstOrNull()?.let { o ->
                val more = if (offers.size > 1) " and ${offers.size - 1} more" else ""
                RuvoNotify.post(applicationContext, RuvoNotify.NEWS, 21, "New student offer: ${o.brand}$more", o.offer, "marketplace")
            }
            books.firstOrNull()?.let { b ->
                val more = if (books.size > 1) " and ${books.size - 1} more" else ""
                RuvoNotify.post(applicationContext, RuvoNotify.NEWS, 22, "New in the Guide: ${b.title}$more", "A ${b.minutes}-minute read. Tap to open the Guide.", "guide")
            }
        }
        if (s.notifyDaily) {
            val (title, body) = com.ruvo.engine.guide.WhatsNew.daily(state.runway, state.toSort.size)
            RuvoNotify.post(applicationContext, RuvoNotify.DAILY, 20, title, body, if (state.toSort.isNotEmpty()) "sort" else null)
        }
        return Result.success()
    }
}

/** Posts Ruvo's calm notifications. Nothing is posted without the notification permission. */
object RuvoNotify {
    const val DAILY = "ruvo_daily"
    const val NEWS = "ruvo_news"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(DAILY, "Daily update", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Your runway and today's amount, each morning" })
        nm.createNotificationChannel(NotificationChannel(NEWS, "New offers and books", NotificationManager.IMPORTANCE_LOW).apply { description = "New student offers in the Marketplace and new books in the Guide" })
    }

    fun allowed(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // checked just below
    fun post(context: Context, channel: String, id: Int, title: String, text: String, open: String?) {
        if (!allowed(context)) return
        ensureChannels(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        if (open != null) launch.putExtra("com.ruvo.extra.OPEN", open)
        val pi = PendingIntent.getActivity(context, id, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }
}

object RuvoWork {
    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            "ruvo-snapshot", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SnapshotWorker>(1, TimeUnit.DAYS).build(),
        )
        wm.enqueueUniquePeriodicWork(
            "ruvo-weekly-sort", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<WeeklySortWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delayToNextSunday7pm().toMinutes(), TimeUnit.MINUTES)
                .build(),
        )
        wm.enqueueUniquePeriodicWork(
            "ruvo-morning", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MorningWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayToNext9am().toMinutes(), TimeUnit.MINUTES)
                .build(),
        )
    }

    private fun delayToNext9am(): Duration {
        val now = LocalDateTime.now()
        var target = LocalDate.now().atTime(9, 0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target)
    }

    private fun delayToNextSunday7pm(): Duration {
        val now = LocalDateTime.now()
        var target = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(19, 0)
        if (!target.isAfter(now)) target = target.plusWeeks(1)
        return Duration.between(now, target)
    }
}
