package com.mini.recorder.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {

    private val DATE_TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val TITLE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun formatDuration(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "$h:${pad2(m)}:${pad2(s)}" else "${pad2(m)}:${pad2(s)}"
    }

    private fun pad2(value: Long): String = if (value < 10) "0$value" else value.toString()

    private fun format(formatter: DateTimeFormatter, ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(formatter)

    fun formatDateTime(ms: Long): String = format(DATE_TIME_FORMATTER, ms)

    fun defaultTitle(ms: Long): String = format(TITLE_FORMATTER, ms)
}
