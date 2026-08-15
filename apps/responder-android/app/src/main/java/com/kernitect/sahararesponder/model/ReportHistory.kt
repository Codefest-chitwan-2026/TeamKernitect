package com.kernitect.sahararesponder.model

fun fullReportHistory(reports: List<ResponderIncident>): List<ResponderIncident> =
    reports.sortedWith(compareByDescending<ResponderIncident> { it.receivedAt }.thenByDescending { it.timestamp })

fun homeRecentReports(reports: List<ResponderIncident>): List<ResponderIncident> = fullReportHistory(reports).take(2)

fun shouldShowMoreReports(reports: List<ResponderIncident>): Boolean = reports.size > 2
