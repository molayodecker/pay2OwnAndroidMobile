package com.pay2own.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pay2own.R
import com.pay2own.domain.Payment
import com.pay2own.domain.formatIsoDate
import com.pay2own.domain.formatMoney
import com.pay2own.ui.components.LoadingState
import com.pay2own.ui.components.MessageState
import com.pay2own.ui.components.OfflineBanner
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.errorBody
import com.pay2own.ui.components.errorTitle
import com.pay2own.ui.components.paymentStatusLabel

@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onReceipt: (String) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    HistoryScreen(state, onBack, viewModel::refresh, onReceipt)
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onReceipt: (String) -> Unit,
) {
    Pay2OwnScaffold(title = stringResource(R.string.history_title), onBack = onBack) { padding ->
        when {
            state.loading && state.payments.isEmpty() && state.error == null -> LoadingState(padding)
            state.payments.isEmpty() && state.error != null -> MessageState(
                padding, errorTitle(state.error), errorBody(state.error), stringResource(R.string.retry), onRetry,
            )
            state.payments.isEmpty() -> MessageState(
                padding,
                stringResource(R.string.history_empty_title),
                stringResource(R.string.history_empty_body),
            )
            else -> com.pay2own.ui.components.ScrollColumn(padding) {
                if (state.offline) OfflineBanner()
                state.payments.forEach { payment ->
                    PaymentRow(payment, onClick = { onReceipt(payment.reference) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: Payment, onClick: () -> Unit) {
    val status = paymentStatusLabel(payment.status)
    val amount = formatMoney(payment.currency, payment.amountMinor)
    val date = formatIsoDate(payment.createdAt)
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(amount, style = MaterialTheme.typography.titleLarge)
        Text("$date · $status")
        Text(payment.reference, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
