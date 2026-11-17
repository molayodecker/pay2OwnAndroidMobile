package com.pay2own.debug

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pay2own.Pay2OwnDeviceAdminReceiver
import com.pay2own.R
import com.pay2own.ui.components.PrimaryButton
import com.pay2own.ui.components.SecondaryButton
import com.pay2own.ui.components.SectionTitle

@Composable
fun DebugTools() {
    val context = LocalContext.current
    var admin by remember { mutableStateOf(false) }
    var owner by remember { mutableStateOf(false) }
    val refresh = {
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val receiver = ComponentName(context, Pay2OwnDeviceAdminReceiver::class.java)
        admin = manager.isAdminActive(receiver)
        owner = manager.isDeviceOwnerApp(context.packageName)
    }
    LaunchedEffect(Unit) { refresh() }
    SectionTitle(stringResource(R.string.debug_heading))
    Text(stringResource(R.string.debug_body), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(if (admin) R.string.debug_admin_on else R.string.debug_admin_off))
    Text(stringResource(if (owner) R.string.debug_owner_on else R.string.debug_owner_off))
    SecondaryButton(stringResource(R.string.debug_refresh), refresh)
    PrimaryButton(stringResource(R.string.debug_lock), {
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        try {
            manager.lockNow()
        } catch (_: SecurityException) {
            Toast.makeText(context, context.getString(R.string.debug_lock_missing), Toast.LENGTH_LONG).show()
        }
    }, enabled = admin)
    if (!admin) {
        SecondaryButton(stringResource(R.string.debug_admin_setup)) {
            val receiver = ComponentName(context, Pay2OwnDeviceAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, receiver)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getString(R.string.debug_admin_explanation))
            }
            context.startActivity(intent)
        }
    }
}
