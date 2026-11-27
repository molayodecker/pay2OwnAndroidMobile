package com.pay2own.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pay2own.R
import com.pay2own.ui.theme.Cyan
import com.pay2own.ui.theme.Ice
import com.pay2own.ui.theme.Navy

enum class AppTab { Home, Payments, Loan, Support }

@Composable
fun AppBottomBar(
    selected: AppTab,
    onHome: () -> Unit,
    onPayments: () -> Unit,
    onLoan: () -> Unit,
    onSupport: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ice)
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabItem(AppTab.Home, selected, stringResource(R.string.tab_home), stringResource(R.string.tab_home), Icons.Filled.Home, onHome)
        TabItem(AppTab.Payments, selected, stringResource(R.string.tab_payments), stringResource(R.string.nav_history), Icons.Filled.ReceiptLong, onPayments)
        TabItem(AppTab.Loan, selected, stringResource(R.string.tab_loan), stringResource(R.string.nav_schedule), Icons.Filled.Smartphone, onLoan)
        TabItem(AppTab.Support, selected, stringResource(R.string.tab_support), stringResource(R.string.nav_support), Icons.Filled.SupportAgent, onSupport)
    }
}

@Composable
private fun TabItem(
    tab: AppTab,
    selected: AppTab,
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val active = tab == selected
    val tint = if (active) Cyan else Navy
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (active) Cyan.copy(alpha = 0.15f) else Ice),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(
            label,
            color = tint,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
