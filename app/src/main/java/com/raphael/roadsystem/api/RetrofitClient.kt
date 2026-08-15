package com.raphael.roadsystem.api

import com.raphael.roadsystem.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // A BASE_URL deve terminar com '/' e ser apenas o domínio base.
    private const val BASE_URL = "https://us-central1-roadsystem-60da6.cloudfunctions.net/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Tempo para estabelecer conexão
            .readTimeout(30, TimeUnit.SECONDS)    // Tempo para ler a resposta (essencial para Cloud Functions)
            .writeTimeout(30, TimeUnit.SECONDS)   // Tempo para enviar dados
            .retryOnConnectionFailure(true)       // Tenta reconectar em caso de falhas intermitentes
            .build()
    }

    val instance: RoadSystemApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Vincula o cliente personalizado com timeouts aumentados
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(RoadSystemApi::class.java)
    }

    // ITEM: Novo cliente para Directions API com Interceptor de Identidade Android
    val googleMapsInstance: GoogleMapsApi by lazy {
        val identityInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Android-Package", "com.raphael.roadsystem")
                // O SHA-1 injetado via BuildConfig a partir do local.properties
                .addHeader("X-Android-Cert", BuildConfig.CERT_SHA1) 
                .build()
            chain.proceed(request)
        }

        val client = okHttpClient.newBuilder()
            .addInterceptor(identityInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(GoogleMapsApi::class.java)
    }
}
