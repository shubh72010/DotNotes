package com.folius.dotnotes.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * NetworkModule — singleton Retrofit instance for OpenRouter API.
 * Extracted from NoteViewModel companion to keep network infra out of ViewModels.
 */
object NetworkModule {
    private const val BASE_URL = "https://openrouter.ai/api/v1/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: OpenRouterApi by lazy { retrofit.create(OpenRouterApi::class.java) }
}
