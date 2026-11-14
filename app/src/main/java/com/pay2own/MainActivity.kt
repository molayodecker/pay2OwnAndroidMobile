package com.pay2own

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pay2own.ui.theme.Pay2OwnTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private var isAdminActive by mutableStateOf(false)
    private var isDeviceOwner by mutableStateOf(false)
    private var nextDue by mutableStateOf("Next installment: not loaded")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Pay2OwnTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Pay2Own")

                        Text(nextDue)
                        Button(onClick = { fetchNextInstallment() }) {
                            Text("Load my next installment")
                        }

                        Text(if (isAdminActive) "Device admin: ACTIVE" else "Device admin: inactive")
                        Text(if (isDeviceOwner) "Device owner: YES" else "Device owner: no")
                        Text(
                            "Test app: you can activate device administration and " +
                                    "manually test the Android screen lock. " +
                                    "No automatic payment restrictions are enabled."
                        )

                        Button(onClick = { refreshStatus() }) {
                            Text("Refresh status")
                        }

                        Button(
                            enabled = isAdminActive,
                            onClick = {
                                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE)
                                        as DevicePolicyManager
                                try {
                                    dpm.lockNow()
                                } catch (error: SecurityException) {
                                    android.widget.Toast.makeText(
                                        this@MainActivity,
                                        "Screen-lock permission is missing. Reactivate Pay2Own administration.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        ) {
                            Text("Test screen lock")
                        }

                        Button(
                            enabled = !isAdminActive,
                            onClick = {
                                val receiver = ComponentName(
                                    this@MainActivity,
                                    Pay2OwnDeviceAdminReceiver::class.java
                                )
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, receiver)
                                    putExtra(
                                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                        "Activate Pay2Own administration for this test phone. " +
                                                "No locking or payment restrictions are configured."
                                    )
                                }
                                startActivity(intent)
                            }
                        ) {
                            Text("Open administrator setup")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val receiver = ComponentName(this, Pay2OwnDeviceAdminReceiver::class.java)
        isAdminActive = dpm.isAdminActive(receiver)
        isDeviceOwner = dpm.isDeviceOwnerApp(packageName)
    }

    private fun fetchNextInstallment() {
        nextDue = "Loading…"
        lifecycleScope.launch {
            try {
                nextDue = withContext(Dispatchers.IO) {
                    val url = URL("${BuildConfig.API_BASE_URL}/api/devices/${BuildConfig.TEST_IMEI}/next-installment")
                    val connection = url.openConnection() as HttpURLConnection
                    try {
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.DEVICE_TOKEN}")
                        when (val code = connection.responseCode) {
                            200 -> {
                                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                                val amount = BigDecimal.valueOf(json.getLong("amount_minor"), 2)
                                "Next installment #${json.getInt("sequence")}: " +
                                        "${json.getString("currency")} ${amount.toPlainString()} " +
                                        "due ${json.getString("due_date")}"
                            }
                            401 -> "This phone isn't signed in"
                            404 -> "No installment due"
                            else -> error("Server returned $code")
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                nextDue = "Could not load: ${e.message}"
            }
        }
    }
}