package com.example.clinexusapp.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL =
        "https://clinexus-web-development.onrender.com/"

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    private val client =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    private val mainRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
    }

    val instance: ApiService by lazy {
        mainRetrofit.create(
            ApiService::class.java
        )
    }

    val appointmentInstance: AppointmentApiService by lazy {
        mainRetrofit.create(
            AppointmentApiService::class.java
        )
    }

    val addressInstance: AddressApiService by lazy {
        Retrofit.Builder()
            .baseUrl(
                "https://psgc.gitlab.io/api/"
            )
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(
                AddressApiService::class.java
            )
    }
}