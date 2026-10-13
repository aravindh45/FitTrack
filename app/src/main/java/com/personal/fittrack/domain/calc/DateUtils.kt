package com.personal.fittrack.domain.calc

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Centralizes "what day/epoch-millis is it" so every call site agrees on the same
 * zone (device default) instead of each feature rolling its own date math. */
object DateUtils {

    private val NUMERIC_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")

    fun todayEpochDay(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).toEpochDay()

    fun epochDayFor(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toEpochDay()

    fun startOfWeekEpochDay(zone: ZoneId = ZoneId.systemDefault()): Long {
        val today = LocalDate.now(zone)
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        return monday.toEpochDay()
    }

    fun startOfMonthEpochDay(zone: ZoneId = ZoneId.systemDefault()): Long {
        val today = LocalDate.now(zone)
        return today.withDayOfMonth(1).toEpochDay()
    }

    /** "13/10/2026" - day/month/year, used as the centered header on each History daily card. */
    fun formatEpochDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(NUMERIC_DATE_FORMATTER)

    /** "8:30 PM" - shown next to each logged entry so you can tell apart a tea logged at
     * breakfast from the same tea logged again in the evening. */
    fun formatTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime().format(TIME_FORMATTER)
}
