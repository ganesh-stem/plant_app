package com.verdant.app.util

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

fun Float.asPercent(): String = "${(this * 100).roundToInt().coerceIn(0, 100)}%"

fun formatTimestamp(millis: Long): String =
    DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

fun formatFullDate(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

enum class DayBucket { TODAY, YESTERDAY, THIS_WEEK, EARLIER }

fun dayBucket(millis: Long, now: Long = System.currentTimeMillis()): DayBucket {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val day = 86_400_000L
    return when {
        millis >= start -> DayBucket.TODAY
        millis >= start - day -> DayBucket.YESTERDAY
        millis >= start - 6 * day -> DayBucket.THIS_WEEK
        else -> DayBucket.EARLIER
    }
}
