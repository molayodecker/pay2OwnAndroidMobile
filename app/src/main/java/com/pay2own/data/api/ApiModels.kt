package com.pay2own.data.api

import com.pay2own.domain.Installment
import com.pay2own.domain.Loan
import com.pay2own.domain.Payment
import com.pay2own.domain.StartedPayment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RestrictionDto(val state: String = "")

@Serializable
data class InstallmentDto(
    val sequence: Int = 0,
    @SerialName("due_date") val dueDate: String = "",
    @SerialName("amount_minor") val amountMinor: Long = 0,
    val status: String = "",
)

@Serializable
data class LoanDto(
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("phone_masked") val phoneMasked: String = "",
    @SerialName("momo_provider") val momoProvider: String = "",
    val model: String = "",
    val currency: String = "GHS",
    @SerialName("price_minor") val priceMinor: Long = 0,
    @SerialName("deposit_minor") val depositMinor: Long = 0,
    @SerialName("remaining_minor") val remainingMinor: Long = 0,
    @SerialName("paid_minor") val paidMinor: Long = 0,
    @SerialName("started_on") val startedOn: String = "",
    @SerialName("loan_status") val loanStatus: String = "",
    @SerialName("account_status") val accountStatus: String = "",
    @SerialName("installment_count") val installmentCount: Int = 0,
    @SerialName("paid_count") val paidCount: Int = 0,
    val next: InstallmentDto? = null,
    @SerialName("pending_reference") val pendingReference: String? = null,
    val schedule: List<InstallmentDto> = emptyList(),
    val terms: String = "",
)

@Serializable
data class PaymentDto(
    val reference: String = "",
    val currency: String = "GHS",
    @SerialName("amount_minor") val amountMinor: Long = 0,
    val status: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val sequences: List<Int> = emptyList(),
)

@Serializable
data class PaymentsDto(
    val payments: List<PaymentDto> = emptyList(),
)

@Serializable
data class StartPaymentDto(
    val installments: Int? = null,
    @SerialName("pay_balance") val payBalance: Boolean? = null,
)

@Serializable
data class StartedDto(
    val reference: String = "",
    val status: String = "",
)

@Serializable
data class PushTokenDto(
    val token: String,
)

fun InstallmentDto.toDomain() = Installment(sequence, dueDate, amountMinor, status)

fun LoanDto.toDomain() = Loan(
    customerName = customerName,
    phoneMasked = phoneMasked,
    provider = momoProvider,
    model = model,
    currency = currency,
    priceMinor = priceMinor,
    depositMinor = depositMinor,
    remainingMinor = remainingMinor,
    paidMinor = paidMinor,
    startedOn = startedOn,
    loanStatus = loanStatus,
    accountStatus = accountStatus,
    installmentCount = installmentCount,
    paidCount = paidCount,
    next = next?.toDomain(),
    pendingReference = pendingReference,
    schedule = schedule.map { it.toDomain() },
    terms = terms,
)

fun PaymentDto.toDomain() = Payment(reference, amountMinor, currency, status, createdAt, sequences)

fun StartedDto.toDomain() = StartedPayment(reference, status)
