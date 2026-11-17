package com.pay2own.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface Pay2OwnApi {
    @GET("api/devices/{imei}/loan")
    suspend fun loan(@Path("imei") imei: String): LoanDto

    @POST("api/devices/{imei}/payments")
    suspend fun startPayment(
        @Path("imei") imei: String,
        @Body body: StartPaymentDto,
    ): StartedDto

    @GET("api/devices/{imei}/payments/{reference}")
    suspend fun payment(
        @Path("imei") imei: String,
        @Path("reference") reference: String,
    ): PaymentDto

    @GET("api/devices/{imei}/payments")
    suspend fun payments(@Path("imei") imei: String): PaymentsDto

    @POST("api/devices/{imei}/push-token")
    suspend fun pushToken(
        @Path("imei") imei: String,
        @Body body: PushTokenDto,
    ): Response<ResponseBody>

    @GET("api/devices/{imei}/restriction")
    suspend fun restriction(@Path("imei") imei: String): RestrictionDto

    @POST("api/devices/{imei}/release-confirmed")
    suspend fun releaseConfirmed(@Path("imei") imei: String): Response<ResponseBody>
}
