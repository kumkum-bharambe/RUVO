package com.ruvo.app

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** First flow end to end: open the app, load a sample student, see the runway and the three tabs. */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun sampleStudentShowsRunway() {
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Explore with a sample student").fetchSemanticsNodes().isNotEmpty() || rule.onAllNodes(hasContentDescription("Runway", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        if (rule.onAllNodesWithText("Explore with a sample student").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Explore with a sample student").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Aarav, 19 · Hostel").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Aarav, 19 · Hostel").performClick()
        }
        rule.waitUntil(10_000) { rule.onAllNodes(hasContentDescription("Runway", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Money").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Where it is, where it went").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Goals").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Money with a future").fetchSemanticsNodes().isNotEmpty() }
    }
}
