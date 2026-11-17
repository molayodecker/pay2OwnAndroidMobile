package com.pay2own.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pay2own.data.api.LoanDto
import com.pay2own.data.api.PaymentDto
import com.pay2own.data.api.toDomain
import com.pay2own.domain.Loan
import com.pay2own.domain.Payment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pay2ownStore by preferencesDataStore(name = "pay2own")

/**
 * The device token lives in Android Keystore, encrypted at rest.
 * It is never written into the app build or a crash report.
 */
@Singleton
class LocalStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveCredentials(token: String, imei: String) = withContext(Dispatchers.IO) {
        val secret = json.encodeToString(Secret.serializer(), Secret(token, imei))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deviceKey())
        val encrypted = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted
        appContext.pay2ownStore.edit { prefs ->
            prefs[CREDENTIALS] = Base64.encodeToString(payload, Base64.NO_WRAP)
        }
    }

    suspend fun readCredentials(): Credentials? = withContext(Dispatchers.IO) {
        val encoded = appContext.pay2ownStore.data.first()[CREDENTIALS] ?: return@withContext null
        try {
            val raw = Base64.decode(encoded, Base64.NO_WRAP)
            if (raw.size <= GCM_IV_BYTES) return@withContext null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                deviceKey(),
                GCMParameterSpec(128, raw.copyOfRange(0, GCM_IV_BYTES)),
            )
            val clear = cipher.doFinal(raw.copyOfRange(GCM_IV_BYTES, raw.size))
            val secret = json.decodeFromString(Secret.serializer(), clear.toString(Charsets.UTF_8))
            Credentials(secret.token, secret.imei)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun isConfirmed(): Boolean =
        appContext.pay2ownStore.data.first()[CONFIRMED] == true

    suspend fun setConfirmed(confirmed: Boolean) {
        appContext.pay2ownStore.edit { it[CONFIRMED] = confirmed }
    }

    suspend fun saveLoan(dto: LoanDto) {
        appContext.pay2ownStore.edit {
            it[LOAN] = json.encodeToString(LoanDto.serializer(), dto)
        }
    }

    suspend fun cachedLoan(): Loan? = decode(LOAN) { json.decodeFromString(LoanDto.serializer(), it).toDomain() }

    suspend fun savePayments(items: List<PaymentDto>) {
        appContext.pay2ownStore.edit {
            it[PAYMENTS] = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(PaymentDto.serializer()), items)
        }
    }

    suspend fun cachedPayments(): List<Payment>? = decode(PAYMENTS) { raw ->
        json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(PaymentDto.serializer()), raw)
            .map { it.toDomain() }
    }

    suspend fun readRestriction(): String? = appContext.pay2ownStore.data.first()[RESTRICTION]

    suspend fun writeRestriction(state: String) {
        appContext.pay2ownStore.edit { it[RESTRICTION] = state }
    }

    fun restrictionFlow(): Flow<String?> = appContext.pay2ownStore.data.map { it[RESTRICTION] }

    suspend fun clear() {
        appContext.pay2ownStore.edit { it.clear() }
    }

    private suspend fun <T> decode(key: androidx.datastore.preferences.core.Preferences.Key<String>, parse: (String) -> T): T? {
        val raw = appContext.pay2ownStore.data.first()[key] ?: return null
        return try {
            parse(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun deviceKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    @Serializable
    private data class Secret(val token: String, val imei: String)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "pay2own.device.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        val CREDENTIALS = stringPreferencesKey("credentials")
        val CONFIRMED = booleanPreferencesKey("confirmed")
        val LOAN = stringPreferencesKey("loan")
        val PAYMENTS = stringPreferencesKey("payments")
        val RESTRICTION = stringPreferencesKey("restriction_state")
    }
}

data class Credentials(val token: String, val imei: String)
