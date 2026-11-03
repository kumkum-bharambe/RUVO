package com.ruvo.app

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ruvo.data.RuvoGraph
import com.ruvo.data.sms.SpendNotifier
import com.ruvo.data.work.RuvoWork
import com.ruvo.design.RuvoTheme
import com.ruvo.design.ThemeChoice
import com.ruvo.widget.RuvoWidget
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RuvoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RuvoGraph.init(this)
        SpendNotifier.ensureChannel(this)
        RuvoWork.schedule(this)
    }
}

/** Something a notification, shortcut or tile asked the app to open. */
sealed interface LaunchRequest {
    data object QuickAdd : LaunchRequest
    data object Sort : LaunchRequest
    data object Chat : LaunchRequest
    data class Txn(val id: String, val split: Boolean) : LaunchRequest
}

class MainActivity : ComponentActivity() {

    private val launchRequest = mutableStateOf<LaunchRequest?>(null)

    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handle(intent)

        // Keep the home-screen widget within a minute of any change.
        lifecycleScope.launch {
            RuvoGraph.repository.state
                .map { Triple(it.runway.runwayDays, it.runway.bufferDays, it.runway.unlimited) }
                .distinctUntilChanged()
                .debounce(1_500)
                .collect { runCatching { RuvoWidget.refresh(this@MainActivity) } }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                RuvoGraph.repository.refreshDay()
                RuvoGraph.repository.recordSnapshot()
            }
        }

        setContent {
            val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
            val theme = runCatching { ThemeChoice.valueOf(state.settings.theme) }.getOrDefault(ThemeChoice.SYSTEM)
            RuvoTheme(theme) {
                RuvoRoot(state, launchRequest.value) { launchRequest.value = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent ?: return
        val txn = intent.getStringExtra(SpendNotifier.EXTRA_TXN)
        launchRequest.value = when {
            txn != null -> LaunchRequest.Txn(txn, intent.getStringExtra(SpendNotifier.EXTRA_ACTION) == SpendNotifier.ACTION_SPLIT)
            intent.getStringExtra("com.ruvo.extra.OPEN") == "quickadd" -> LaunchRequest.QuickAdd
            intent.getStringExtra("com.ruvo.extra.OPEN") == "sort" -> LaunchRequest.Sort
            intent.getStringExtra("com.ruvo.extra.OPEN") == "chat" -> LaunchRequest.Chat
            else -> launchRequest.value
        }
    }
}
