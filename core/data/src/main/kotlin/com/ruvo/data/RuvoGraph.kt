package com.ruvo.data

import android.content.Context
import com.ruvo.data.db.RuvoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The app's small hand-written dependency graph. Created once in the Application, read by every screen.
 * Kept deliberately simple instead of Hilt so the project builds with few moving parts.
 */
object RuvoGraph {
    lateinit var repository: RuvoRepository
        private set
    lateinit var settings: SettingsStore
        private set
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val isReady: Boolean get() = ::repository.isInitialized

    fun init(context: Context) {
        if (isReady) return
        val app = context.applicationContext
        settings = SettingsStore(app)
        repository = RuvoRepository(RuvoDatabase.open(app).dao(), settings, appScope)
    }
}
