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
            if (parts.size != 2 || parts[0].isEmpty()) null
            else decode(parts[0]) to decode(parts[1])
        }
        .toMap()
    val token = params["token"].orEmpty().trim()
    val imei = params["imei"].orEmpty().trim()
    if (!validToken(token) || !validImei(imei)) return null
    return EnrollLink(token, imei)
}

private fun decode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())
