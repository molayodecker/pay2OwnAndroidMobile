package com.pay2own.domain

fun unpaidInstallments(loan: Loan): List<Installment> =
    loan.schedule.filter { it.status != "paid" }.sortedBy { it.sequence }

fun canPay(loan: Loan, offline: Boolean): Boolean {
    if (offline) return false
    if (loan.accountStatus == "paid_off") return false
    if (loan.pendingReference != null) return false
    if (loan.next == null) return false
    if (loan.remainingMinor <= 0L) return false
    return true
}

fun selectedAmount(loan: Loan, count: Int, payBalance: Boolean): Long {
    if (payBalance) return loan.remainingMinor
    return unpaidInstallments(loan).take(count.coerceAtLeast(1)).sumOf { it.amountMinor }
}
