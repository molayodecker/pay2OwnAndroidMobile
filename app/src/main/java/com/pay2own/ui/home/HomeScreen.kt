package com.pay2own.ui.home

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.pay2own.domain.RestrictionState
import com.pay2own.domain.canPay
import com.pay2own.domain.firstName
import com.pay2own.domain.formatMoney
import com.pay2own.domain.formatWeekday
import com.pay2own.domain.initials
import com.pay2own.reminders.firebaseReady
import com.pay2own.ui.components.AppBottomBar
import com.pay2own.ui.components.AppTab
import com.pay2own.ui.components.LoadingState
import com.pay2own.ui.components.MessageState
import com.pay2own.ui.components.OfflineBanner
import com.pay2own.ui.components.PaidProgress
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.components.PrimaryButton
import com.pay2own.ui.components.ScrollColumn
import com.pay2own.ui.components.SecondaryButton
import com.pay2own.ui.components.StatusBadge
import com.pay2own.ui.theme.Cyan
import com.pay2own.ui.theme.Ice
import com.pay2own.ui.theme.Mist
import com.pay2own.ui.theme.Navy
import com.pay2own.ui.theme.White
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
    HomeScreen(
        state,
        restriction.copy,
        locked = restriction.state == RestrictionState.Active,
        onRetry = viewModel::refresh,
        onPay = onPay,
        onHistory = onHistory,
        onSchedule = onSchedule,
        onSupport = onSupport,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    paidOff: PaidOffCopy = PaidOffCopy.PaidOff,
    locked: Boolean = false,
    onRetry: () -> Unit,
    onPay: () -> Unit,
    onHistory: () -> Unit,
    onSchedule: () -> Unit,
    onSupport: () -> Unit,
) {
    Pay2OwnScaffold(
        title = "",
        showTopBar = false,
        bottomBar = {
            if (state.loan != null) {
                AppBottomBar(AppTab.Home, onHome = {}, onPayments = onHistory, onLoan = onSchedule, onSupport = onSupport)
            }
        },
    ) { padding ->
        val loan = state.loan
        when {
            state.loading && loan == null -> LoadingState(padding)
            loan == null && state.error != null -> MessageState(
                padding, errorTitle(state.error), errorBody(state.error), stringResource(R.string.retry), onRetry,
            )
            loan != null -> HomeContent(padding, loan, state.offline, paidOff, locked, onPay, onHistory)
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
    locked: Boolean,
    onPay: () -> Unit,
    onHistory: () -> Unit,
) {
    val context = LocalContext.current
    val paidOffScreen = loan.accountStatus == "paid_off"
    ScrollColumn(padding) {
        if (offline) OfflineBanner()
        BrandRow(initials(loan.customerName))
        Text(
            stringResource(R.string.home_hello, firstName(loan.customerName)),
            style = MaterialTheme.typography.headlineMedium,
        )
        BalanceCard(loan)
        if (paidOffScreen) {
            PaidOffCard(paidOff, onHistory)
        } else {
            NextPaymentCard(loan, offline, onPay)
        }
        if (loan.pendingReference != null) {
            Text(stringResource(R.string.home_waiting), modifier = Modifier.fillMaxWidth())
        }
        if (locked) {
            Text(stringResource(R.string.lock_apps_paused), modifier = Modifier.fillMaxWidth())
            SecondaryButton(stringResource(R.string.lock_phone)) {
                try {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
                } catch (_: ActivityNotFoundException) {
                }
            }
        }
    }
}

@Composable
private fun BrandRow(letters: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorResource(R.color.ic_launcher_background)),
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Navy, fontWeight = FontWeight.Bold)) { append("Pay") }
                    withStyle(SpanStyle(color = Cyan, fontWeight = FontWeight.Bold)) { append("2") }
                    withStyle(SpanStyle(color = Navy, fontWeight = FontWeight.Bold)) { append("own") }
                },
                fontSize = 22.sp,
            )
        }
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Ice),
            contentAlignment = Alignment.Center,
        ) {
            Text(letters, color = Navy, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BalanceCard(loan: Loan) {
    val percent = (loan.progress * 100).toInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Navy)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Smartphone, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                Text(loan.model, color = Color.White)
            }
            StatusBadge(loan.accountStatus, onDark = true)
        }
        Text(stringResource(R.string.home_remaining), color = Color.White.copy(alpha = 0.75f))
        Text(
            formatMoney(loan.currency, loan.remainingMinor),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
        )
        PaidProgress(loan.progress, stringResource(R.string.home_progress_description, percent))
        Text(
            stringResource(
                R.string.home_progress,
                formatMoney(loan.currency, loan.paidMinor),
                formatMoney(loan.currency, loan.priceMinor),
            ),
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun NextPaymentCard(loan: Loan, offline: Boolean, onPay: () -> Unit) {
    val next = loan.next ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Mist)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.home_next_amount), color = Navy.copy(alpha = 0.7f))
                Text(formatMoney(loan.currency, next.amountMinor), style = MaterialTheme.typography.headlineMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.home_due_label), color = Navy.copy(alpha = 0.7f))
                Text(formatWeekday(next.dueDate), fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
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
private fun PaidOffCard(copy: PaidOffCopy, onHistory: () -> Unit) {
    val message = when (copy) {
        PaidOffCopy.PaidOff -> R.string.home_paid_off
        PaidOffCopy.StillRestricted -> R.string.home_restrictions_on
        PaidOffCopy.FinishingUnlock -> R.string.home_finishing_unlock
        PaidOffCopy.FullyYours -> R.string.home_fully_yours
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Mist)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Cyan),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = White, modifier = Modifier.size(16.dp))
            }
            Text(stringResource(message), style = MaterialTheme.typography.titleMedium)
        }
        SecondaryButton(stringResource(R.string.home_see_history), onHistory)
    }
}
