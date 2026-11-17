package com.pay2own.domain

data class Installment(
    val sequence: Int,
    val dueDate: String,
    val amountMinor: Long,
    val status: String,
)

data class Loan(
    val customerName: String,
    val phoneMasked: String,
    val provider: String,
    val model: String,
    val currency: String,
    val priceMinor: Long,
    val depositMinor: Long,
    val remainingMinor: Long,
    val paidMinor: Long,
    val startedOn: String,
    val loanStatus: String,
    val accountStatus: String,
    val installmentCount: Int,
    val paidCount: Int,
    val next: Installment?,
    val pendingReference: String?,
    val schedule: List<Installment>,
    val terms: String,
) {
    val progress: Float
        get() = if (priceMinor <= 0L) 0f else (paidMinor.toFloat() / priceMinor.toFloat()).coerceIn(0f, 1f)
}

data class Payment(
    val reference: String,
    val amountMinor: Long,
    val currency: String,
    val status: String,
    val createdAt: String,
    val sequences: List<Int>,
)

data class StartedPayment(
    val reference: String,
    val status: String,
)

sealed interface Outcome<out T> {
    data class Fresh<T>(val value: T) : Outcome<T>
    data class Cached<T>(val value: T) : Outcome<T>
    data object SignedOut : Outcome<Nothing>
    data object NotFound : Outcome<Nothing>
    data object Conflict : Outcome<Nothing>
    data object Busy : Outcome<Nothing>
    data object Offline : Outcome<Nothing>
    data object BadRequest : Outcome<Nothing>
    data object Failed : Outcome<Nothing>
}

fun <T> Outcome<T>.valueOrNull(): T? = when (this) {
    is Outcome.Fresh -> value
    is Outcome.Cached -> value
    else -> null
}
