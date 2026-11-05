package com.ruvo.engine.guide

import com.ruvo.engine.rupees
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GuideExtrasTest {

    @Test fun smallSpendsAddUpByTheYear() {
        val h = Tools.smallSpends(rupees(300), rupees(250))
        assertEquals(rupees(15_600), h.perYear)
        assertEquals(62, h.days)
        assertEquals(0, Tools.smallSpends(rupees(300), 0).days)
    }

    @Test fun growthIsMoreThanWhatYouPutIn() {
        val g = Tools.growth(rupees(500), 5, 8.0)
        assertEquals(rupees(30_000), g.putIn)
        assertTrue(g.endsAt in rupees(36_000)..rupees(38_000), "₹500 a month for 5 years at 8% is about ₹37,000, got ${g.endsAt}")
        assertEquals(g.putIn, Tools.growth(rupees(500), 5, 0.0).endsAt)
    }

    @Test fun inflationShrinksBuyingPower() {
        val later = Tools.inflation(rupees(10_000), 12, 6.0)
        assertTrue(later in rupees(4_900)..rupees(5_100), "At 6% prices roughly double in 12 years, got $later")
    }

    @Test fun emiMatchesTheStoryNumbers() {
        val loan = Tools.emi(rupees(12_000), 16.0, 6)
        assertTrue(loan.emi in rupees(2_080)..rupees(2_100), "EMI ${loan.emi}")
        assertTrue(loan.extra(rupees(12_000)) in rupees(500)..rupees(600), "Extra ${loan.extra(rupees(12_000))}")
        assertEquals(rupees(2_000), Tools.emi(rupees(12_000), 0.0, 6).emi)
    }

    @Test fun payingTheMinimumTakesYears() {
        val p = Tools.cardMinimum(rupees(12_000))
        assertTrue(p.cleared)
        assertTrue(p.months >= 36, "Minimum payments should take years, took ${p.months} months")
        assertTrue(p.interest > rupees(4_000), "Interest ${p.interest}")
    }

    @Test fun everyStoryChoiceLeadsSomewhereAndEveryPathEndsWithALesson() {
        Stories.all.forEach { s ->
            assertEquals(s.scenes.size, s.scenes.map { it.id }.toSet().size, "${s.id} scene ids")
            s.scenes.forEach { sc ->
                sc.choices.forEach { ch -> assertNotNull(s.scene(ch.next), "${s.id}/${sc.id} → ${ch.next}") }
                if (sc.isEnding) assertTrue(!sc.lesson.isNullOrBlank(), "${s.id}/${sc.id} ending needs a lesson")
            }
            // Walk every path from the start; none may loop forever.
            fun walk(id: String, depth: Int) {
                assertTrue(depth < 10, "${s.id} path too long or looping at $id")
                s.scene(id)!!.choices.forEach { walk(it.next, depth + 1) }
            }
            walk(s.start.id, 0)
        }
    }

    @Test fun glossaryFindsWholeWordsInReadingOrder() {
        val found = Glossary.inText("Pay the total due, not the minimum due. Your CIBIL score and the EMI matter.")
        assertEquals(listOf("Minimum due", "Credit score", "EMI"), found.map { it.word })
        assertTrue(Glossary.inText("Spinning tops").isEmpty(), "\"PIN\" inside a word must not match")
        assertEquals(Glossary.all.size, Glossary.all.map { it.word.lowercase() }.toSet().size)
        assertNotNull(Glossary.byWord("cibil"))
    }
}
