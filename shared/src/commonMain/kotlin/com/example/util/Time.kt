package com.example.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun local(millis: Long): LocalDateTime =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())

private fun Int.two() = toString().padStart(2, '0')

/** "14:05" */
fun formatClock(millis: Long): String = local(millis).let { "${it.hour.two()}:${it.minute.two()}" }

/** "03 Nov, 14:05" */
fun formatDayTime(millis: Long): String =
    local(millis).let { "${it.day.two()} ${MONTHS[it.month.ordinal]}, ${it.hour.two()}:${it.minute.two()}" }

/** "03/11 14:05:09" */
fun formatLogTime(millis: Long): String =
    local(millis).let { "${it.day.two()}/${(it.month.ordinal + 1).two()} ${it.hour.two()}:${it.minute.two()}:${it.second.two()}" }
