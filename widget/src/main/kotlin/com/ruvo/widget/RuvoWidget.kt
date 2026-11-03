package com.ruvo.widget

import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import android.app.PendingIntent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ruvo.data.RuvoGraph
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.RunwayResult
import kotlinx.coroutines.flow.first

/** Home-screen widget: the runway number and its one-line meaning. Tap opens Ruvo. */
class RuvoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        RuvoGraph.init(context)
        val state = RuvoGraph.repository.state.first { it.ready }
        val r = state.runway
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        provideContent { WidgetBody(r, launch) }
    }

    companion object {
        /** Called by the app whenever the numbers change, so the widget stays within a minute of a new spend. */
        suspend fun refresh(context: Context) = RuvoWidget().updateAll(context)
    }
}

private val Ink = Color(0xFF16131D)
private val Paper = Color(0xFFF4EFE7)
private val Fuel = Color(0xFF3CF2C6)
private val Warning = Color(0xFFFF5C78)

@Composable
private fun WidgetBody(r: RunwayResult, launch: Intent) {
    val subline = when {
        r.unlimited -> "No end at this pace"
        r.daysToNextIncome == null -> "At your usual pace"
        r.makesIt -> "${IndianFormat.days(r.spareDays)} spare"
        else -> "${IndianFormat.days(-(r.bufferDays ?: 0))} short"
    }
    Column(
        GlanceModifier.fillMaxSize().background(Ink).cornerRadius(28.dp).padding(16.dp).clickable(actionStartActivity(launch)),
        verticalAlignment = Alignment.Vertical.Bottom,
    ) {
        Text("RUNWAY", style = TextStyle(color = ColorProvider(Paper.copy(alpha = 0.7f)), fontSize = 11.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(4.dp))
        Row(verticalAlignment = Alignment.Vertical.Bottom) {
            Text(if (r.unlimited) "∞" else r.runwayDays.toString(), style = TextStyle(color = ColorProvider(Paper), fontSize = 52.sp, fontWeight = FontWeight.Bold))
            Text(" days", style = TextStyle(color = ColorProvider(Paper), fontSize = 16.sp))
        }
        Text(subline, style = TextStyle(color = ColorProvider(if (r.makesIt) Fuel else Warning), fontSize = 14.sp, fontWeight = FontWeight.Medium))
    }
}

class RuvoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RuvoWidget()
}

/** Quick Settings tile: one tap from anywhere to add a cash spend. */
class QuickAddTile : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = (packageManager.getLaunchIntentForPackage(packageName) ?: return)
            .putExtra("com.ruvo.extra.OPEN", "quickadd")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 11, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
