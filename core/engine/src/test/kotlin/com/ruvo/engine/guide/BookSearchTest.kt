package com.ruvo.engine.guide

import org.junit.Test
import kotlin.test.assertTrue

class BookSearchTest {
    @Test fun themesFindTheirShelves() {
        assertTrue(Library.search("saving").any { it.shelf == Library.Shelf.SAVE })
        assertTrue(Library.search("spending").any { it.shelf == Library.Shelf.SPEND })
        assertTrue(Library.search("money").any { it.shelf == Library.Shelf.BASICS })
        assertTrue(Library.search("loan").all { it.shelf == Library.Shelf.CREDIT || it.title.contains("loan", true) || it.idea.contains("loan", true) })
    }

    @Test fun titleMatchesComeFirst() {
        val b = Library.all.first()
        assertTrue(Library.search(b.title).first() == b)
    }

    @Test fun blankFindsNothing() {
        assertTrue(Library.search("  ").isEmpty())
    }
}
