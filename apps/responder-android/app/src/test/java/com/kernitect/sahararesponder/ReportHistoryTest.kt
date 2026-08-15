package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.model.*
import org.junit.Assert.*
import org.junit.Test

class ReportHistoryTest {
    private fun report(number: Int) = ResponderIncident("SOS-$number", "HIGH", "Report $number", 27.0, 84.0, number.toLong(), 0, receivedAt = number.toLong())

    @Test fun homeShowsZeroOneOrTwoReports() {
        assertEquals(0, homeRecentReports(emptyList()).size)
        assertEquals(1, homeRecentReports(listOf(report(1))).size)
        assertEquals(2, homeRecentReports(listOf(report(1), report(2))).size)
    }
    @Test fun homeShowsExactlyLatestTwoFromLargerHistory() = assertEquals(listOf("SOS-4", "SOS-3"), homeRecentReports((1..4).map(::report)).map { it.id })
    @Test fun seeMoreAppearsOnlyAboveTwoReports() {
        assertFalse(shouldShowMoreReports((1..2).map(::report)))
        assertTrue(shouldShowMoreReports((1..3).map(::report)))
    }
    @Test fun fullHistoryContainsEveryReportNewestFirst() = assertEquals(listOf("SOS-4", "SOS-3", "SOS-2", "SOS-1"), fullReportHistory((1..4).map(::report)).map { it.id })
}
