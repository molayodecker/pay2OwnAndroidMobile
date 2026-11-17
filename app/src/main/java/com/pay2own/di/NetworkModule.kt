package com.pay2own.di

import android.content.Context
import androidx.work.WorkManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pay2own.BuildConfig
import com.pay2own.data.api.Pay2OwnApi
import com.pay2own.data.local.LocalStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun client(store: LocalStore): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val token = runBlocking { store.readCredentials()?.token }
                val request = if (token.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .header("User-Agent", "Pay2Own/${BuildConfig.VERSION_NAME}")
                        .build()
                }
                chain.proceed(request)
            })
        val pin = BuildConfig.CERT_PIN_SHA256.trim()
        if (pin.isNotEmpty()) {
            val formatted = if (pin.startsWith("sha256/")) pin else "sha256/$pin"
            builder.certificatePinner(
                CertificatePinner.Builder().add("api.pay2own.app", formatted).build(),
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit {
        val base = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun api(retrofit: Retrofit): Pay2OwnApi = retrofit.create(Pay2OwnApi::class.java)

    @Provides
    @Singleton
    fun workManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
}
