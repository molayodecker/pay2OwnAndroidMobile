package com.pay2own.ui.schedule

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
import com.pay2own.domain.Installment
import com.pay2own.domain.dueLabel
import com.pay2own.domain.formatIsoDate
import com.pay2own.domain.formatMoney
import com.pay2own.domain.todayIso
import com.pay2own.ui.components.LabeledValue
import com.pay2own.ui.components.LoadingState
import com.pay2own.ui.components.MessageState
import com.pay2own.ui.components.OfflineBanner
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.ScrollColumn
import com.pay2own.ui.components.SectionTitle
import com.pay2own.ui.components.errorBody
import com.pay2own.ui.components.errorTitle

@Composable
fun ScheduleRoute(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    ScheduleScreen(state, onBack, viewModel::refresh)
}

@Composable
fun ScheduleScreen(state: ScheduleUiState, onBack: () -> Unit, onRetry: () -> Unit) {
    Pay2OwnScaffold(title = stringResource(R.string.schedule_title), onBack = onBack) { padding ->
        val loan = state.loan
        when {
            state.loading && loan == null -> LoadingState(padding)
            loan == null && state.error != null -> MessageState(
                padding, errorTitle(state.error), errorBody(state.error), stringResource(R.string.retry), onRetry,
            )
            loan != null -> ScrollColumn(padding) {
                if (state.offline) OfflineBanner()
                LabeledValue(stringResource(R.string.schedule_phone), loan.model)
                LabeledValue(stringResource(R.string.schedule_price), formatMoney(loan.currency, loan.priceMinor))
                LabeledValue(stringResource(R.string.schedule_deposit), formatMoney(loan.currency, loan.depositMinor))
                LabeledValue(stringResource(R.string.schedule_started), formatIsoDate(loan.startedOn))
                SectionTitle(stringResource(R.string.schedule_heading))
                val today = todayIso()
                loan.schedule.forEach { item ->
                    ScheduleRow(item, loan.currency, today)
                }
                SectionTitle(stringResource(R.string.terms_heading))
                Text(loan.terms, style = MaterialTheme.typography.bodyLarge)
            }
            else -> LoadingState(padding)
        }
    }
}

@Composable
private fun ScheduleRow(item: Installment, currency: String, today: String) {
    val status = when (dueLabel(item.status, item.dueDate, today)) {
        "paid" -> stringResource(R.string.schedule_paid)
        "overdue" -> stringResource(R.string.schedule_overdue)
        "due_today" -> stringResource(R.string.schedule_due_today)
        else -> stringResource(R.string.schedule_unpaid)
    }
    val description = stringResource(
        R.string.schedule_row,
        item.sequence,
        formatIsoDate(item.dueDate),
        formatMoney(currency, item.amountMinor),
        status,
    )
    Text(description, modifier = Modifier.semantics { })
}
