package com.pay2own.ui.receipt

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pay2own.R
import com.pay2own.domain.formatIsoDate
import com.pay2own.domain.formatMoney
import com.pay2own.ui.components.HeroAmount
import com.pay2own.ui.components.LabeledValue
import com.pay2own.ui.components.LoadingState
import com.pay2own.ui.components.MessageState
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.PrimaryButton
import com.pay2own.ui.components.ScrollColumn
import com.pay2own.ui.components.errorBody
import com.pay2own.ui.components.errorTitle
import com.pay2own.ui.components.paymentStatusLabel

@Composable
fun ReceiptRoute(
    reference: String,
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(reference) { viewModel.load(reference) }
    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    ReceiptScreen(state, onBack) { viewModel.load(reference) }
}

@Composable
fun ReceiptScreen(
    state: ReceiptUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Pay2OwnScaffold(title = stringResource(R.string.receipt_title), onBack = onBack) { padding ->
        val payment = state.payment
        when {
            state.loading && payment == null -> LoadingState(padding)
            payment == null && state.error != null -> MessageState(
                padding, errorTitle(state.error), errorBody(state.error), stringResource(R.string.retry), onRetry,
            )
            payment != null -> {
                val context = LocalContext.current
                val amount = formatMoney(payment.currency, payment.amountMinor)
                val date = formatIsoDate(payment.createdAt)
                val status = paymentStatusLabel(payment.status)
                val shareText = stringResource(R.string.receipt_share_body, amount, date, payment.reference, status)
                ScrollColumn(padding) {
                    HeroAmount(amount)
                    LabeledValue(stringResource(R.string.receipt_date), date)
                    LabeledValue(stringResource(R.string.receipt_reference), payment.reference)
                    LabeledValue(stringResource(R.string.receipt_status), status)
                    if (payment.sequences.isNotEmpty()) {
                        LabeledValue(
                            stringResource(R.string.schedule_heading),
                            stringResource(R.string.history_installments, payment.sequences.joinToString(", ")),
                        )
                    }
                    PrimaryButton(stringResource(R.string.receipt_share), onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(send, context.getString(R.string.receipt_share)))
                    })
                }
            }
            else -> LoadingState(padding)
        }
    }
}
