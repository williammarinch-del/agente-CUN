package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object CopFormatter {
    private val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val integerFormatter = DecimalFormat("#,##0", symbols)

    fun format(amountCop: Long, showPlusSign: Boolean = false): String {
        val absValue = kotlin.math.abs(amountCop)
        val formattedNumber = integerFormatter.format(absValue)
        return when {
            amountCop < 0 -> "-$ $formattedNumber"
            showPlusSign && amountCop > 0 -> "+$ $formattedNumber"
            else -> "$ $formattedNumber"
        }
    }

    fun formatCompact(amountCop: Long): String {
        val abs = kotlin.math.abs(amountCop)
        val prefix = if (amountCop < 0) "-" else ""
        return when {
            abs >= 1_000_000L -> {
                val millions = abs / 1_000_000.0
                val rounded = if (millions % 1.0 == 0.0) {
                    "${millions.toInt()}M"
                } else {
                    String.format(Locale.US, "%.1fM", millions).replace(".0M", "M")
                }
                "${prefix}$ $rounded"
            }
            abs >= 1_000L -> {
                val thousands = abs / 1_000.0
                val rounded = if (thousands % 1.0 == 0.0) {
                    "${thousands.toInt()}k"
                } else {
                    String.format(Locale.US, "%.0fk", thousands)
                }
                "${prefix}$ $rounded"
            }
            else -> "${prefix}$ $abs"
        }
    }

    fun parseCopInput(raw: String): Long? {
        val digitsOnly = raw.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return null
        return digitsOnly.toLongOrNull()
    }

    fun calculateSaleSubtotal(quantity: Int, unitPriceCop: Long): Long {
        if (quantity <= 0 || unitPriceCop <= 0L) return 0L
        return quantity.toLong() * unitPriceCop
    }

    fun calculateSaleTotal(quantity: Int, unitPriceCop: Long, discountCop: Long): Long {
        val subtotal = calculateSaleSubtotal(quantity, unitPriceCop)
        return subtotal - discountCop.coerceAtLeast(0L)
    }
}

object DateUtils {
    private val monthNamesShort = arrayOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic"
    )

    private val monthNamesFull = arrayOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )

    private val dayNamesShort = arrayOf(
        "Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"
    )

    private val dayNamesFull = arrayOf(
        "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
    )

    fun toEpochDay(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, monthZeroBased, dayOfMonth, 0, 0, 0)
        }
        return TimeUnit.MILLISECONDS.toDays(utc.timeInMillis)
    }

    fun todayEpochDay(): Long {
        val local = Calendar.getInstance()
        return toEpochDay(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun currentTimeText(): String {
        val local = Calendar.getInstance()
        val hour = local.get(Calendar.HOUR_OF_DAY)
        val minute = local.get(Calendar.MINUTE)
        return formatTime(hour, minute)
    }

    fun formatTime(hourOfDay: Int, minute: Int): String {
        return "${hourOfDay.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    fun parseHourMinute(timeText: String): Pair<Int, Int>? {
        val parts = timeText.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    fun epochDayFromUtcMillis(utcMillis: Long): Long {
        return TimeUnit.MILLISECONDS.toDays(utcMillis)
    }

    fun utcMillisFromEpochDay(epochDay: Long): Long {
        return TimeUnit.DAYS.toMillis(epochDay)
    }

    private fun utcCalendar(epochDay: Long): Calendar {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = TimeUnit.DAYS.toMillis(epochDay)
        }
    }

    fun formatShortDate(epochDay: Long): String {
        val today = todayEpochDay()
        if (epochDay == today) return "Hoy"
        if (epochDay == today - 1) return "Ayer"
        val cal = utcCalendar(epochDay)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = monthNamesShort[cal.get(Calendar.MONTH)]
        return "$day $month"
    }

    fun formatNumericDate(epochDay: Long): String {
        val cal = utcCalendar(epochDay)
        val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val year = cal.get(Calendar.YEAR)
        return "$day/$month/$year"
    }

    fun formatFullDate(epochDay: Long): String {
        val cal = utcCalendar(epochDay)
        val dow = dayNamesFull[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = monthNamesFull[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$dow, $day de $month de $year"
    }

    fun shortDayLabel(epochDay: Long): String {
        val today = todayEpochDay()
        if (epochDay == today) return "Hoy"
        val cal = utcCalendar(epochDay)
        return dayNamesShort[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun lastSevenDays(referenceEpochDay: Long = todayEpochDay()): List<Long> {
        return (6 downTo 0).map { offset -> referenceEpochDay - offset }
    }
}
