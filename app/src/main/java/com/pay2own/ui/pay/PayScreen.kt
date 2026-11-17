package com.pay2own.ui.pay

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pay2own.R
import com.pay2own.domain.Loan
import com.pay2own.domain.PaidOffCopy
import com.pay2own.domain.displayPhone
import com.pay2own.domain.formatMoney
import com.pay2own.domain.selectedAmount
import com.pay2own.domain.unpaidInstallments
import androidx.compose.ui.text.style.TextAlign
import com.pay2own.ui.components.HeroAmount
import com.pay2own.ui.components.LabeledValue
import com.pay2own.ui.components.LoadingState
import com.pay2own.ui.components.MessageState
import com.pay2own.ui.components.OfflineBanner
import com.pay2own.ui.components.PaidOffMessage
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.PrimaryButton
import com.pay2own.ui.components.ScrollColumn
import com.pay2own.ui.components.SecondaryButton
import com.pay2own.ui.components.SectionTitle
import com.pay2own.ui.components.errorBody
import com.pay2own.ui.components.errorTitle
import com.pay2own.ui.components.providerName
import com.pay2own.ui.restriction.RestrictionViewModel

@Composable
fun PayRoute(
    onSignedOut: () -> Unit,
    onDone: () -> Unit,
    onReceipt: (String) -> Unit,
    viewModel: PayViewModel = hiltViewModel(),
    restrictions: RestrictionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val restriction by restrictions.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }
    PayScreen(
        state = state,
        onRetry = viewModel::refresh,
        onSelectCount = viewModel::selectInstallments,
        onSelectBalance = viewModel::selectBalance,
        onContinue = viewModel::continueToConfirm,
        onBackToChoose = viewModel::backToChoose,
        onPay = viewModel::pay,
        onRetryPayment = viewModel::retryPayment,
        onDone = onDone,
        onReceipt = onReceipt,
        paidOff = restriction.copy,
    )
}

@Composable
fun PayScreen(
    state: PayUiState,
    onRetry: () -> Unit,
    onSelectCount: (Int) -> Unit,
    onSelectBalance: () -> Unit,
    onContinue: () -> Unit,
    onBackToChoose: () -> Unit,
    onPay: () -> Unit,
    onRetryPayment: () -> Unit = onRetry,
    onDone: () -> Unit,
    onReceipt: (String) -> Unit,
    paidOff: PaidOffCopy = PaidOffCopy.PaidOff,
) {
    val onBack = if (state.step == PayStep.Confirm) onBackToChoose else null
    Pay2OwnScaffold(title = stringResource(R.string.pay_title), onBack = onBack) { padding ->
        val loan = state.loan
        when {
            state.loading && loan == null -> LoadingState(padding)
            loan == null && state.error != null -> MessageState(
                padding, errorTitle(state.error), errorBody(state.error), stringResource(R.string.retry), onRetry,
            )
            loan == null -> LoadingState(padding)
            state.step == PayStep.Done -> PayDone(padding, state, onDone, onReceipt, onRetryPayment)
            state.step == PayStep.Waiting -> PayWaiting(padding, state)
            state.step == PayStep.Confirm -> PayConfirm(padding, loan, state, onPay, onRetry)
            else -> PayChoose(padding, loan, state, paidOff, onSelectCount, onSelectBalance, onContinue, onRetry)
        }
    }
}

@Composable
private fun PayChoose(
    padding: androidx.compose.foundation.layout.PaddingValues,
    loan: Loan,
    state: PayUiState,
    paidOff: PaidOffCopy,
    onSelectCount: (Int) -> Unit,
    onSelectBalance: () -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
) {
    val unpaid = unpaidInstallments(loan)
    ScrollColumn(padding) {
        if (state.offline) OfflineBanner()
        if (unpaid.isEmpty()) {
            PaidOffMessage(paidOff)
            return@ScrollColumn
        }
        AmountChoice(
            selected = !state.payBalance && state.installmentCount == 1,
            label = stringResource(R.string.pay_this),
            detail = formatMoney(loan.currency, unpaid.first().amountMinor),
            onSelect = { onSelectCount(1) },
        )
        if (unpaid.size >= 3) {
            val count = if (state.payBalance) 2 else state.installmentCount.coerceIn(2, unpaid.size - 1)
            AmountChoice(
                selected = !state.payBalance && state.installmentCount >= 2,
                label = stringResource(R.string.pay_several),
                detail = stringResource(R.string.pay_count, count) + " · " + formatMoney(loan.currency, selectedAmount(loan, count, false)),
                onSelect = { onSelectCount(count) },
            )
            if (!state.payBalance && state.installmentCount >= 2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onSelectCount((state.installmentCount - 1).coerceAtLeast(2)) },
                        enabled = state.installmentCount > 2,
                    ) { Text(stringResource(R.string.pay_decrease)) }
                    TextButton(
                        onClick = { onSelectCount((state.installmentCount + 1).coerceAtMost(unpaid.size - 1)) },
                        enabled = state.installmentCount < unpaid.size - 1,
                    ) { Text(stringResource(R.string.pay_increase)) }
                }
            }
        }
        if (unpaid.size >= 2) {
            AmountChoice(
                selected = state.payBalance,
                label = stringResource(R.string.pay_balance),
                detail = formatMoney(loan.currency, loan.remainingMinor),
                onSelect = onSelectBalance,
            )
        }
        if (state.error != null) Text(errorBody(state.error), color = MaterialTheme.colorScheme.error)
        if (state.offline) SecondaryButton(stringResource(R.string.retry), onRetry)
        PrimaryButton(stringResource(R.string.continue_action), onContinue, enabled = !state.offline)
    }
}

@Composable
private fun AmountChoice(selected: Boolean, label: String, detail: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        ColumnText(label, detail)
    }
}

@Composable
private fun ColumnText(label: String, detail: String) {
    androidx.compose.foundation.layout.Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PayConfirm(
    padding: androidx.compose.foundation.layout.PaddingValues,
    loan: Loan,
    state: PayUiState,
    onPay: () -> Unit,
    onRetry: () -> Unit,
) {
    val amount = selectedAmount(loan, state.installmentCount, state.payBalance)
    ScrollColumn(padding) {
        SectionTitle(stringResource(R.string.pay_confirm_title))
        HeroAmount(formatMoney(loan.currency, amount))
        Text(
            stringResource(R.string.pay_approve),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pay_confirm")
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        LabeledValue(stringResource(R.string.setup_momo), displayPhone(loan.phoneMasked))
        LabeledValue(stringResource(R.string.pay_momo_provider), providerName(loan.provider))
        if (state.error != null) Text(errorBody(state.error), color = MaterialTheme.colorScheme.error)
        if (state.offline) SecondaryButton(stringResource(R.string.retry), onRetry)
        PrimaryButton(
            label = stringResource(R.string.pay_submit, formatMoney(loan.currency, amount)),
            onClick = onPay,
            enabled = !state.submitting && !state.offline,
            tag = "pay_submit",
        )
    }
}

@Composable
private fun PayWaiting(
    padding: androidx.compose.foundation.layout.PaddingValues,
    state: PayUiState,
) {
    ScrollColumn(padding) {
        Text(
            stringResource(R.string.pay_waiting),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .testTag("pay_waiting")
                .semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
        )
        Text(stringResource(R.string.pay_approve_body))
        if (state.reference != null) {
            LabeledValue(stringResource(R.string.receipt_reference), state.reference)
        }
    }
}

@Composable
private fun PayDone(
    padding: androidx.compose.foundation.layout.PaddingValues,
    state: PayUiState,
    onDone: () -> Unit,
    onReceipt: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val title = when (state.result) {
        PayResult.Success -> stringResource(R.string.pay_success)
        PayResult.Failure -> stringResource(R.string.pay_failure)
        else -> stringResource(R.string.pay_timeout)
    }
    val body = when (state.result) {
        PayResult.Success -> stringResource(
            R.string.pay_success_body,
            formatMoney(state.resultCurrency, state.resultAmountMinor),
            state.reference.orEmpty(),
        )
        PayResult.Failure -> stringResource(R.string.pay_failure_body)
        else -> stringResource(R.string.pay_timeout_body)
    }
    ScrollColumn(padding) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .testTag(if (state.result == PayResult.Success) "pay_success" else "pay_result")
                .semantics { heading() },
        )
        Text(body)
        if (state.result == PayResult.Success && state.reference != null) {
            PrimaryButton(stringResource(R.string.pay_view_receipt), { onReceipt(state.reference) })
        }
        if (state.result == PayResult.Failure) {
            SecondaryButton(stringResource(R.string.retry), onRetry)
        }
        PrimaryButton(stringResource(R.string.pay_done), onDone)
    }
}
