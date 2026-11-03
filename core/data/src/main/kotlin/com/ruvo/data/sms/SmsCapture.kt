package com.ruvo.data.sms

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ruvo.data.RuvoGraph
import com.ruvo.data.db.TxnEntity
import com.ruvo.engine.IndianFormat
import com.ruvo.sms.SmsMessage
import com.ruvo.sms.SmsParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Listens for new SMS. Only bank and UPI sender IDs are read (or any sender while demo SMS is on, so the
 * emulator's test messages work). Parsing happens here on the phone; nothing is uploaded.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        RuvoGraph.init(context)
        val pending = goAsync()
        RuvoGraph.appScope.launch {
            try {
                val s = RuvoGraph.settings.settings.first()
                if (!s.realSms && !s.demoSms) return@launch
                val parser = SmsParser(allowAnySender = s.demoSms)
                // Multi-part SMS arrive as pieces from the same sender; join them before parsing.
                val joined = messages.groupBy { it.originatingAddress ?: "" }.map { (sender, parts) ->
                    val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(parts.first().timestampMillis), ZoneId.systemDefault())
                    SmsMessage(sender, parts.joinToString("") { it.messageBody ?: "" }, at)
                }
                val parsed = joined.mapNotNull(parser::parse)
                val added = RuvoGraph.repository.importSms(parsed)
                added.forEach { SpendNotifier.show(context, it) }
            } finally {
                pending.finish()
            }
        }
    }
}

/** One-time scan of the last 90 days of the inbox, run when the user turns on real SMS reading. */
object SmsInboxScanner {
    suspend fun scan(context: Context, days: Int = 90): Int {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return 0
        val since = System.currentTimeMillis() - days * 86_400_000L
        val parser = SmsParser()
        val found = mutableListOf<SmsMessage>()
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} >= ?", arrayOf(since.toString()), "${Telephony.Sms.DATE} ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val sender = c.getString(0) ?: continue
                // Skip anything that is not from a bank sender ID before even reading the body.
                if (parser.bankFor(sender) == null) continue
                val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(2)), ZoneId.systemDefault())
                found += SmsMessage(sender, c.getString(1) ?: "", at)
            }
        }
        val parsed = found.mapNotNull(parser::parse)
        return RuvoGraph.repository.importSms(parsed).size
    }
}

/** The quiet notification after a spend is captured: "₹180 Swiggy · Fuel", with a one-tap "Shared?". */
object SpendNotifier {
    const val CHANNEL = "ruvo_spends"
    const val EXTRA_TXN = "com.ruvo.extra.TXN"
    const val EXTRA_ACTION = "com.ruvo.extra.ACTION"
    const val ACTION_SPLIT = "split"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Captured spends", NotificationManager.IMPORTANCE_LOW).apply {
                description = "A quiet note when Ruvo reads a payment from a bank SMS"
            },
        )
    }

    @SuppressLint("MissingPermission") // checked just below
    fun show(context: Context, t: TxnEntity) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        fun pi(action: String?, code: Int) = PendingIntent.getActivity(
            context, code,
            Intent(launch).putExtra(EXTRA_TXN, t.id).also { if (action != null) it.putExtra(EXTRA_ACTION, action) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val sign = if (t.direction == "CREDIT") "+" else ""
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("$sign${IndianFormat.rupees(t.amount)} ${t.merchant}")
            .setContentText(if (t.direction == "CREDIT") "Money in. Your runway updated." else "Fuel · waiting in your weekly sort")
            .setContentIntent(pi(null, t.id.hashCode()))
            .addAction(0, "Shared?", pi(ACTION_SPLIT, t.id.hashCode() + 1))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(t.id.hashCode(), n) }
    }
}
