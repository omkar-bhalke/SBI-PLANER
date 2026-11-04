package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
  private var cachedToken: String? = null
  private var cachedRefreshToken: String? = null
  private var cachedDeviceId: String? = null
  private var currentBaseUrl: String = "https://radhewins.buzz/"
  private var _apiService: FinPulseApiService? = null

  fun setAuthToken(token: String?) {
    cachedToken = token
  }

  fun setRefreshToken(refreshToken: String?) {
    cachedRefreshToken = refreshToken
  }

  fun setDeviceId(deviceId: String?) {
    cachedDeviceId = deviceId
  }

  fun setBaseUrl(url: String) {
    val formatted = if (url.endsWith("/")) url else "$url/"
    if (formatted != currentBaseUrl) {
      currentBaseUrl = formatted
      _apiService = null
    }
  }

  private val authInterceptor = Interceptor { chain ->
    val original = chain.request()
    val builder = original.newBuilder()

    cachedToken?.let { token ->
      builder.header("Authorization", "Bearer $token")
    }

    cachedDeviceId?.let { devId ->
      builder.header("X-Device-Id", devId)
    }

    builder.header("Accept", "application/json")
    chain.proceed(builder.build())
  }

  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
  }

  private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(authInterceptor)
    .addInterceptor(loggingInterceptor)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  val apiService: FinPulseApiService
    get() {
      if (_apiService == null) {
        _apiService = Retrofit.Builder()
          .baseUrl(currentBaseUrl)
          .client(okHttpClient)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create(FinPulseApiService::class.java)
      }
      return _apiService!!
    }
}
