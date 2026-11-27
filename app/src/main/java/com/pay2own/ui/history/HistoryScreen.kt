package com.pay2own.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pay2own.R
import com.pay2own.domain.Payment
import com.pay2own.domain.formatIsoDate
import com.pay2own.domain.formatMoney
import com.pay2own.domain.shortReference
import com.pay2own.ui.components.AppBottomBar
import com.pay2own.ui.components.AppTab
import com.pay2own.ui.components.LoadingState
import com.pay2own.ui.components.MessageState
import com.pay2own.ui.components.OfflineBanner
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.errorBody
import com.pay2own.ui.components.errorTitle
import com.pay2own.ui.components.paymentStatusLabel
import com.pay2own.ui.theme.Cyan
import com.pay2own.ui.theme.Ice
import com.pay2own.ui.theme.Navy

@Composable
fun HistoryRoute(
    onHome: () -> Unit,
    onLoan: () -> Unit,
    onSupport: () -> Unit,
    onReceipt: (String) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    HistoryScreen(state, onHome, onLoan, onSupport, viewModel::refresh, onReceipt)
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onHome: () -> Unit,
    onLoan: () -> Unit,
    onSupport: () -> Unit,
    onRetry: () -> Unit,
    onReceipt: (String) -> Unit,
) {
    Pay2OwnScaffold(
        title = "",
        showTopBar = false,
        bottomBar = { AppBottomBar(AppTab.Payments, onHome, onPayments = {}, onLoan, onSupport) },
    ) { padding ->
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
                Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium)
                if (state.offline) OfflineBanner()
                state.payments.forEach { payment ->
                    PaymentRow(payment, onClick = { onReceipt(payment.reference) })
                    HorizontalDivider(color = Ice)
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: Payment, onClick: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val status = paymentStatusLabel(payment.status)
    val amount = formatMoney(payment.currency, payment.amountMinor)
    val date = formatIsoDate(payment.createdAt)
    val caption = if (payment.sequences.size == 1) {
        stringResource(R.string.history_installment_line, payment.sequences.first(), date)
    } else if (payment.sequences.isEmpty()) {
        stringResource(R.string.history_deposit_line, date)
    } else {
        stringResource(R.string.history_installments, payment.sequences.joinToString(", ")) + " · $date"
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Ice),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Cyan)
        }
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(amount, style = MaterialTheme.typography.titleLarge)
                Text(status, color = Cyan, fontWeight = FontWeight.SemiBold)
            }
            Text(caption, color = Navy.copy(alpha = 0.7f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(shortReference(payment.reference), color = Navy.copy(alpha = 0.45f))
                IconButton(onClick = { clipboard.setText(AnnotatedString(payment.reference)) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.history_copy), tint = Navy.copy(alpha = 0.45f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
