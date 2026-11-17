package com.pay2own.reminders

import android.app.Application
import com.pay2own.BuildConfig
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid

fun initCrashReporting(app: Application) {
    val dsn = BuildConfig.SENTRY_DSN
    if (dsn.isBlank()) return
    SentryAndroid.init(app) { options ->
        options.dsn = dsn
        options.isSendDefaultPii = false
        options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
            event.request = null
            event.user = null
            event.serverName = null
            event.message?.message = redact(event.message?.message)
            event.breadcrumbs?.forEach { crumb ->
                crumb.message = redact(crumb.message)
            }
            event
        }
    }
}

private val secretPatterns = listOf(
    Regex("""(?i)bearer\s+[A-Za-z0-9._\-+/=]+"""),
    Regex("""\b[a-fA-F0-9]{64}\b"""),
    Regex("""/api/devices/\d{15}"""),
    Regex("""\b\d{15}\b"""),
    Regex("""(?<!\d)(?:\+233\d{9}|0\d{9})(?!\d)"""),
)

fun redact(value: String?): String? = value?.let { text ->
    secretPatterns.fold(text) { acc, pattern -> pattern.replace(acc, "[redacted]") }
}
