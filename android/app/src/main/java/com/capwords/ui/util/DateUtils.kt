package com.capwords.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    /** "Feb 23" style header, localized. */
    fun dayLabel(epochMillis: Long): String =
        SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(epochMillis))

    /** Midnight epoch for grouping rows by calendar day. */
    fun dayKey(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
