package com.icm.security_scorpion_app.data.api

import com.icm.security_scorpion_app.utils.GlobalSettings
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GlobalSettings.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: JsonPlaceHolderApi by lazy {
        retrofit.create(JsonPlaceHolderApi::class.java)
    }
}