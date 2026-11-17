package com.pay2own.ui

import com.pay2own.domain.Outcome

fun Outcome<*>.toScreenError(): ScreenError = when (this) {
    is Outcome.Offline -> ScreenError.Offline
    is Outcome.NotFound -> ScreenError.NotFound
    is Outcome.Conflict -> ScreenError.Conflict
    is Outcome.Busy -> ScreenError.Busy
    is Outcome.BadRequest -> ScreenError.BadRequest
    else -> ScreenError.Failed
}
