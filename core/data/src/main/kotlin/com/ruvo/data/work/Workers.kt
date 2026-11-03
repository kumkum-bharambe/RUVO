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
        if (count == 0) return Result.success()
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
    }

    private fun delayToNextSunday7pm(): Duration {
        val now = LocalDateTime.now()
        var target = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(19, 0)
        if (!target.isAfter(now)) target = target.plusWeeks(1)
        return Duration.between(now, target)
    }
}
