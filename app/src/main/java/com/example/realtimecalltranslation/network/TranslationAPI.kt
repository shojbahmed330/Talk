package com.example.realtimecalltranslation.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.Response

data class TranslationRequest(
    val q: String,
    val target: String,
    val source: String = "auto"
)

data class TranslationResponse(
    val data: Data
) {
    data class Data(val translations: List<Translation>) {
        data class Translation(val translatedText: String)
    }
}

interface TranslationApi {
    @POST("language/translate/v2")
    suspend fun translate(
        @Body body: TranslationRequest,
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") apiHost: String = "google-translate1.p.rapidapi.com"
    ): Response<TranslationResponse>
}

object RetrofitInstance {
    val api: TranslationApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://google-translate1.p.rapidapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TranslationApi::class.java)
    }
}