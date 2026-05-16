package com.reps.app.core.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.reps.app.BuildConfig
import com.reps.app.core.network.api.ApiNinjasApiService
import com.reps.app.core.network.api.OpenFoodFactsApiService
import com.reps.app.core.network.api.UsdaApiService
import com.reps.app.core.network.api.WgerApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideUsdaApiService(client: OkHttpClient): UsdaApiService =
        Retrofit.Builder()
            .baseUrl("https://api.nal.usda.gov/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(UsdaApiService::class.java)

    @Provides
    @Singleton
    fun provideOpenFoodFactsApiService(client: OkHttpClient): OpenFoodFactsApiService =
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenFoodFactsApiService::class.java)

    @Provides
    @Singleton
    fun provideApiNinjasApiService(client: OkHttpClient): ApiNinjasApiService =
        Retrofit.Builder()
            .baseUrl("https://api.api-ninjas.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiNinjasApiService::class.java)

    @Provides
    @Singleton
    fun provideWgerApiService(client: OkHttpClient): WgerApiService =
        Retrofit.Builder()
            .baseUrl("https://wger.de/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WgerApiService::class.java)
}
