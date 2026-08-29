package com.lillah.dhikr.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Injected rather than static so streak and calendar logic can be tested across day boundaries,
 * midnight rollovers and time zones without touching the device clock.
 */
interface AppClock {
    fun zone(): ZoneId
    fun now(): Instant
    fun today(): LocalDate = LocalDate.now(zone())
    fun timeOfDay(): LocalTime = LocalTime.now(zone())
    fun todayEpochDay(): Long = today().toEpochDay()
    fun nowMillis(): Long = now().toEpochMilli()
}

class SystemAppClock(private val zoneId: ZoneId = ZoneId.systemDefault()) : AppClock {
    override fun zone(): ZoneId = zoneId
    override fun now(): Instant = Instant.now()
}
