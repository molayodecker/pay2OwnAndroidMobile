package com.pay2own.domain

import java.util.Calendar
import java.util.Locale

fun maskPhone(phone: String): String {
    if (phone.length < 8) return "••••"
    val bullets = phone.length - 7
    return phone.take(3) + "•".repeat(bullets) + phone.takeLast(4)
}

/** Shows a masked number. Masks a raw MoMo number if one ever arrives. */
fun displayPhone(value: String): String {
    if (value.contains('•')) return value
    val digits = value.count { it.isDigit() }
    return if (digits >= 8) maskPhone(value) else value
}

fun validImei(value: String): Boolean = value.length == 15 && value.all { it.isDigit() }

fun validToken(value: String): Boolean =
    value.length == 64 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

fun formatMoney(currency: String, minor: Long): String {
    val sign = if (minor < 0) "-" else ""
    val amount = kotlin.math.abs(minor)
    val code = currency.ifBlank { "GHS" }
    return "%s%s %d.%02d".format(sign, code, amount / 100, amount % 100)
}

private val months = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

fun firstName(name: String): String = name.trim().substringBefore(' ').ifBlank { name.trim() }

fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return ""
    if (parts.size == 1) return parts[0].take(2).uppercase(Locale.ROOT)
    return (parts.first().take(1) + parts.last().take(1)).uppercase(Locale.ROOT)
}

fun spacedPhone(value: String): String {
    val shown = displayPhone(value)
    val match = Regex("(\\d{3})(•+)(\\d{4})").matchEntire(shown) ?: return shown
    return "${match.groupValues[1]} ${match.groupValues[2]} ${match.groupValues[3]}"
}

fun shortReference(reference: String): String {
    if (reference.length <= 18) return reference
    return reference.take(10) + "…" + reference.takeLast(6)
}

fun formatWeekday(iso: String): String {
    val parts = iso.take(10).split("-")
    if (parts.size != 3) return formatIsoDate(iso)
    val year = parts[0].toIntOrNull() ?: return formatIsoDate(iso)
    val month = parts[1].toIntOrNull() ?: return formatIsoDate(iso)
    val day = parts[2].toIntOrNull() ?: return formatIsoDate(iso)
    if (month !in 1..12) return formatIsoDate(iso)
    val calendar = Calendar.getInstance(Locale.US)
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    calendar.set(Calendar.DAY_OF_MONTH, day)
    val weekday = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)
        ?: return formatIsoDate(iso)
    return "$weekday $day ${months[month - 1]} $year"
}

fun formatIsoDate(iso: String): String {
    val date = iso.take(10)
    val parts = date.split("-")
    if (parts.size != 3) return iso
    val month = parts[1].toIntOrNull() ?: return iso
    val day = parts[2].toIntOrNull() ?: return iso
    if (month !in 1..12) return iso
    return "$day ${months[month - 1]} ${parts[0]}"
}

fun todayIso(calendar: Calendar = Calendar.getInstance()): String {
    return String.format(
        Locale.ROOT,
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
    )
}

fun dueLabel(status: String, due: String, today: String): String = when {
    status == "paid" -> "paid"
    due < today -> "overdue"
    due == today -> "due_today"
    else -> "scheduled"
}
