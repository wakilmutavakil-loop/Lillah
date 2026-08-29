package com.lillah.dhikr.core.time

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateFormats {
    private val dayMonth = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    private val shortDay = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val dayOnly = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    fun full(date: LocalDate): String = date.format(dayMonth)
    fun weekday(date: LocalDate): String = date.format(shortDay)
    fun monthYear(date: LocalDate): String = date.format(monthYear)
    fun dayMonth(date: LocalDate): String = date.format(dayOnly)

    fun weekdayInitial(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
}

/** Human-friendly counts: 1,240 rather than 1240 once numbers get long. */
fun Int.grouped(): String = String.format(Locale.getDefault(), "%,d", this)
fun Long.grouped(): String = String.format(Locale.getDefault(), "%,d", this)
