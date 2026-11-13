package com.ruvo.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Short confirmations after an action, such as "Saved ₹250 · runway 24 → 23 days". Any screen can post one; the app
 * shell shows it briefly above the dock and screen readers announce it. Kept tiny on purpose: one line, no buttons.
 */
object Notices {
    sealed interface Notice {
        /** A payment or money-in was saved. The shell fills in the runway change once the numbers settle. */
        data class Saved(val amount: Long, val moneyIn: Boolean, val runwayBefore: Int) : Notice
        /** Any other short line. */
        data class Text(val line: String) : Notice
    }

    private val events = MutableSharedFlow<Notice>(extraBufferCapacity = 4)
    val flow: SharedFlow<Notice> = events.asSharedFlow()

    fun post(n: Notice) { events.tryEmit(n) }
}
