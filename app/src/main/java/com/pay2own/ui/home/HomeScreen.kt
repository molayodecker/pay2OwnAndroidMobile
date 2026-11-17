package com.pay2own.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.messaging.FirebaseMessaging
import com.pay2own.R
import com.pay2own.domain.Loan
import com.pay2own.domain.PaidOffCopy
import com.pay2own.domain.canPay
import com.pay2own.domain.formatIsoDate
import com.pay2own.domain.formatMoney
import com.pay2own.reminders.firebaseReady
import com.pay2own.ui.components.HeroAmount
import com.pay2own.ui.components.LoadingState
import com.pay2own.ui.components.MessageState
import com.pay2own.ui.components.OfflineBanner
import com.pay2own.ui.components.PaidOffMessage
import com.pay2own.ui.components.PaidProgress
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.PrimaryButton
import com.pay2own.ui.components.ScrollColumn
import com.pay2own.ui.components.StatusBadge
import com.pay2own.ui.components.errorBody
import com.pay2own.ui.components.errorTitle
import com.pay2own.ui.restriction.RestrictionViewModel

@Composable
fun HomeRoute(
    onPay: () -> Unit,
    onHistory: () -> Unit,
    onSchedule: () -> Unit,
    onSupport: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    restrictions: RestrictionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val restriction by restrictions.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
                restrictions.refresh()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val context = LocalContext.current
    DisposableEffect(Unit) {
        if (!firebaseReady(context)) return@DisposableEffect onDispose {}
        val listener = OnSuccessListener<String> { viewModel.registerPushToken(it) }
        val task = FirebaseMessaging.getInstance().token
        task.addOnSuccessListener(listener)
        onDispose { }
    }
    HomeScreen(state, restriction.copy, viewModel::refresh, onPay, onHistory, onSchedule, onSupport)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    paidOff: PaidOffCopy = PaidOffCopy.PaidOff,
    onRetry: () -> Unit,
    onPay: () -> Unit,
    onHistory: () -> Unit,
    onSchedule: () -> Unit,
    onSupport: () -> Unit,
) {
    Pay2OwnScaffold(
        title = stringResource(R.string.app_name),
        bottomBar = {
            if (state.loan != null) HomeTabs(onHistory, onSchedule, onSupport)
        },
    ) { padding ->
        val loan = state.loan
        when {
            state.loading && loan == null -> LoadingState(padding)
            loan == null && state.error != null -> MessageState(
                padding, errorTitle(state.error), errorBody(state.error), stringResource(R.string.retry), onRetry,
            )
            loan != null -> HomeContent(padding, loan, state.offline, paidOff, onPay)
            else -> LoadingState(padding)
        }
    }
}

@Composable
private fun HomeContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    loan: Loan,
    offline: Boolean,
    paidOff: PaidOffCopy,
    onPay: () -> Unit,
) {
    ScrollColumn(padding) {
        if (offline) OfflineBanner()
        Text(
            stringResource(R.string.home_hello, loan.customerName),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        StatusBadge(loan.accountStatus, Modifier.align(Alignment.CenterHorizontally))
        if (loan.accountStatus == "paid_off") {
            PaidOffMessage(paidOff)
        } else {
            val next = loan.next
            if (next != null) {
                Text(
                    stringResource(R.string.home_next_amount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                HeroAmount(formatMoney(loan.currency, next.amountMinor))
                Text(
                    stringResource(R.string.home_due, formatIsoDate(next.dueDate)),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.home_installment, next.sequence, loan.installmentCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Text(
            stringResource(R.string.home_remaining),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            formatMoney(loan.currency, loan.remainingMinor),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val percent = (loan.progress * 100).toInt()
        PaidProgress(
            fraction = loan.progress,
            description = stringResource(R.string.home_progress_description, percent),
        )
        Text(
            stringResource(R.string.home_progress, formatMoney(loan.currency, loan.paidMinor), formatMoney(loan.currency, loan.priceMinor)),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (loan.pendingReference != null) {
            Text(stringResource(R.string.home_waiting), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        PrimaryButton(
            label = stringResource(R.string.pay_now),
            onClick = onPay,
            enabled = canPay(loan, offline),
            tag = "pay_now",
        )
    }
}

@Composable
private fun HomeTabs(onHistory: () -> Unit, onSchedule: () -> Unit, onSupport: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HomeTab(stringResource(R.string.tab_payments), stringResource(R.string.nav_history), onHistory)
            HomeTab(stringResource(R.string.tab_loan), stringResource(R.string.nav_schedule), onSchedule)
            HomeTab(stringResource(R.string.tab_support), stringResource(R.string.nav_support), onSupport)
        }
    }
}

@Composable
private fun HomeTab(label: String, description: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = description }) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}
