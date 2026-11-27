package com.pay2own.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pay2own.R
import com.pay2own.domain.PaidOffCopy
import com.pay2own.ui.theme.Cyan
import com.pay2own.ui.theme.Navy
import com.pay2own.ui.theme.White
import com.pay2own.ui.theme.Yellow
import com.pay2own.ui.ScreenError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pay2OwnScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    showTopBar: Boolean = true,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showTopBar) TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = bottomBar,
        content = content,
    )
}

@Composable
fun ScrollColumn(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun LoadingState(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.loading),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun MessageState(
    padding: PaddingValues,
    title: String,
    body: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyLarge)
        if (action != null && onAction != null) {
            PrimaryButton(action, onAction)
        }
    }
}

@Composable
fun errorTitle(error: ScreenError): String = when (error) {
    ScreenError.Offline -> stringResource(R.string.error_offline_title)
    ScreenError.NotFound -> stringResource(R.string.error_not_found_title)
    ScreenError.Conflict -> stringResource(R.string.error_conflict_title)
    ScreenError.Busy -> stringResource(R.string.error_busy_title)
    ScreenError.BadRequest -> stringResource(R.string.error_amount_title)
    ScreenError.Failed -> stringResource(R.string.error_failed_title)
}

@Composable
fun errorBody(error: ScreenError): String = when (error) {
    ScreenError.Offline -> stringResource(R.string.error_offline_body)
    ScreenError.NotFound -> stringResource(R.string.error_not_found_body)
    ScreenError.Conflict -> stringResource(R.string.error_conflict_body)
    ScreenError.Busy -> stringResource(R.string.error_busy_body)
    ScreenError.BadRequest -> stringResource(R.string.error_amount_body)
    ScreenError.Failed -> stringResource(R.string.error_failed_body)
}

@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean = true, tag: String? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Yellow,
            contentColor = Navy,
            disabledContainerColor = Yellow.copy(alpha = 0.4f),
            disabledContentColor = Navy.copy(alpha = 0.4f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SecondaryButton(label: String, onClick: () -> Unit) {
    SecondaryButton(label, icon = null, onClick = onClick)
}

@Composable
fun SecondaryButton(label: String, icon: ImageVector?, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun HeroAmount(amount: String) {
    Text(
        text = amount,
        style = MaterialTheme.typography.displayLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun PaidOffMessage(copy: PaidOffCopy = PaidOffCopy.PaidOff) {
    val message = when (copy) {
        PaidOffCopy.PaidOff -> R.string.home_paid_off
        PaidOffCopy.StillRestricted -> R.string.home_restrictions_on
        PaidOffCopy.FinishingUnlock -> R.string.home_finishing_unlock
        PaidOffCopy.FullyYours -> R.string.home_fully_yours
    }
    HeroAmount(stringResource(R.string.status_paid_off))
    Text(
        stringResource(message),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val (container, content) = if (onDark) onDarkStatusColors(status) else statusColors(status)
    val label = statusLabel(status)
    Text(
        text = label,
        color = content,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
    )
}

@Composable
fun statusLabel(status: String): String = when (status) {
    "due_soon" -> stringResource(R.string.status_due_soon)
    "overdue" -> stringResource(R.string.status_overdue)
    "paid_off" -> stringResource(R.string.status_paid_off)
    else -> stringResource(R.string.status_up_to_date)
}

@Composable
private fun onDarkStatusColors(status: String): Pair<Color, Color> = when (status) {
    "paid_off", "due_soon", "overdue" -> Yellow to Navy
    else -> Cyan to White
}

@Composable
private fun statusColors(status: String): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return when (status) {
        "overdue" -> if (dark) Color(0xFFF9D523) to Color(0xFF0D1859) else Color(0xFF0D1859) to Color(0xFFFFFFFF)
        "due_soon" -> Color(0xFFF9D523) to Color(0xFF0D1859)
        "paid_off" -> if (dark) Color(0xFF2CAADF) to Color(0xFF0D1859) else Color(0xFFD6F0FB) to Color(0xFF0D1859)
        else -> if (dark) Color(0xFF1A4F72) to Color(0xFF2CAADF) else Color(0xFFD6F0FB) to Color(0xFF0D1859)
    }
}

@Composable
fun PaidProgress(fraction: Float, description: String) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        color = MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction.coerceIn(0f, 1f), 0f..1f)
                contentDescription = description
            },
    )
}

@Composable
fun OfflineBanner() {
    Text(
        stringResource(R.string.offline_banner),
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable
fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun LinkRow(label: String, onClick: () -> Unit) {
    SecondaryButton(label, onClick)
}

@Composable
fun providerName(code: String): String = when (code) {
    "mtn" -> stringResource(R.string.provider_mtn)
    "vod" -> stringResource(R.string.provider_vod)
    "atl" -> stringResource(R.string.provider_atl)
    else -> stringResource(R.string.provider_other)
}

@Composable
fun paymentStatusLabel(status: String): String = when (status) {
    "success" -> stringResource(R.string.payment_success)
    "failed" -> stringResource(R.string.payment_failed)
    else -> stringResource(R.string.payment_pending)
}

@Composable
fun ActionRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        content()
    }
}
