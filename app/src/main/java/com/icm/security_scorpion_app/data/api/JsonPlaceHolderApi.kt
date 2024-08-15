package com.icm.security_scorpion_app.data.api

import com.icm.security_scorpion_app.data.DeviceModel
import retrofit2.Call
import retrofit2.http.GET

interface JsonPlaceHolderApi {
    @GET("devices")
    fun getDevices(): Call<List<DeviceModel>>
}
