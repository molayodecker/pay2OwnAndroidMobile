package com.pay2own.domain

import java.net.URLDecoder

data class EnrollLink(val token: String, val imei: String)

fun parseEnrollLink(uri: String?): EnrollLink? {
    if (uri.isNullOrBlank()) return null
    val base = uri.substringBefore('?').trimEnd('/')
    if (base != "pay2own://enroll") return null
    val params = uri.substringAfter('?', "")
        .split('&')
        .mapNotNull { pair ->
            val parts = pair.split('=', limit = 2)
            if (parts.size != 2 || parts[0].isEmpty()) return@mapNotNull null
            val name = decode(parts[0]) ?: return@mapNotNull null
            val value = decode(parts[1]) ?: return@mapNotNull null
            name to value
        }
        .toMap()
    val token = params["token"].orEmpty().trim()
    val imei = params["imei"].orEmpty().trim()
    if (!validToken(token) || !validImei(imei)) return null
    return EnrollLink(token, imei)
}

private fun decode(value: String): String? = try {
    URLDecoder.decode(value, Charsets.UTF_8.name())
} catch (_: IllegalArgumentException) {
    null
}
