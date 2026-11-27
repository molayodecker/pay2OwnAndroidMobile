package com.pay2own.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pay2own.R
import com.pay2own.domain.Installment
import com.pay2own.domain.Loan
import com.pay2own.domain.dueLabel
import com.pay2own.domain.formatIsoDate
import com.pay2own.domain.formatMoney
import com.pay2own.domain.todayIso
import com.pay2own.ui.components.AppBottomBar
import com.pay2own.ui.components.AppTab
import com.pay2own.ui.components.LoadingState
import com.pay2own.ui.components.MessageState
import com.pay2own.ui.components.OfflineBanner
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.ScrollColumn
import com.pay2own.ui.components.SectionTitle
import com.pay2own.ui.components.errorBody
import com.pay2own.ui.components.errorTitle
import com.pay2own.ui.theme.Cyan
import com.pay2own.ui.theme.Ice
import com.pay2own.ui.theme.Navy
import com.pay2own.ui.theme.Yellow

@Composable
fun ScheduleRoute(
    onHome: () -> Unit,
    onPayments: () -> Unit,
    onSupport: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    ScheduleScreen(state, onHome, onPayments, onSupport, viewModel::refresh)
}

@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onHome: () -> Unit,
    onPayments: () -> Unit,
    onSupport: () -> Unit,
    onRetry: () -> Unit,
) {
    Pay2OwnScaffold(
        title = "",
        showTopBar = false,
        bottomBar = { AppBottomBar(AppTab.Loan, onHome, onPayments, onLoan = {}, onSupport) },
    ) { padding ->
        val loan = state.loan
        when {
            state.loading && loan == null -> LoadingState(padding)
            loan == null && state.error != null -> MessageState(
                padding, errorTitle(state.error), errorBody(state.error), stringResource(R.string.retry), onRetry,
            )
            loan != null -> ScrollColumn(padding) {
                Text(stringResource(R.string.schedule_title), style = MaterialTheme.typography.headlineMedium)
                if (state.offline) OfflineBanner()
                LoanSummary(loan)
                SectionTitle(stringResource(R.string.schedule_heading))
                ScheduleList(loan)
                SectionTitle(stringResource(R.string.terms_heading))
                Text(stringResource(R.string.terms_link), color = Cyan, style = MaterialTheme.typography.titleMedium)
                Text(loan.terms, style = MaterialTheme.typography.bodyLarge)
            }
            else -> LoadingState(padding)
        }
    }
}

@Composable
private fun LoanSummary(loan: Loan) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.schedule_phone),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(loan.model, style = MaterialTheme.typography.titleLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryFact(
                stringResource(R.string.schedule_price),
                formatMoney(loan.currency, loan.priceMinor),
                Modifier.weight(1f),
            )
            SummaryFact(
                stringResource(R.string.schedule_deposit),
                formatMoney(loan.currency, loan.depositMinor),
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryFact(
                stringResource(R.string.schedule_started),
                formatIsoDate(loan.startedOn),
                Modifier.weight(1f),
            )
            SummaryFact(
                stringResource(R.string.schedule_weekly),
                formatMoney(loan.currency, loan.schedule.firstOrNull()?.amountMinor ?: 0L),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ScheduleList(loan: Loan) {
    val today = todayIso()
    Column(modifier = Modifier.fillMaxWidth()) {
        loan.schedule.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = Ice)
            ScheduleRow(item, loan.currency, today, loan.next?.sequence)
        }
    }
}

@Composable
private fun ScheduleRow(item: Installment, currency: String, today: String, nextSequence: Int?) {
    val key = dueLabel(item.status, item.dueDate, today)
    val status = when {
        key == "paid" -> stringResource(R.string.schedule_paid)
        key == "overdue" -> stringResource(R.string.schedule_overdue)
        item.sequence == nextSequence -> stringResource(R.string.schedule_due_next)
        else -> stringResource(R.string.schedule_upcoming)
    }
    val pillContainer = when {
        key == "paid" -> Ice
        key == "overdue" -> Navy
        item.sequence == nextSequence -> Yellow
        else -> Ice
    }
    val pillContent = when {
        key == "overdue" -> Color.White
        else -> Navy
    }
    val description = stringResource(
        R.string.schedule_row,
        item.sequence,
        formatIsoDate(item.dueDate),
        formatMoney(currency, item.amountMinor),
        status,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (key == "paid") Cyan.copy(alpha = 0.15f) else Ice),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                item.sequence.toString(),
                color = if (key == "paid") Cyan else Navy,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            formatIsoDate(item.dueDate),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        Text(
            formatMoney(currency, item.amountMinor),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            status,
            color = pillContent,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(pillContainer)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
