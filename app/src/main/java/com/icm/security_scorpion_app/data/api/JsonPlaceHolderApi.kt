package com.icm.security_scorpion_app.data.api

import com.icm.security_scorpion_app.data.CameraModel
import com.icm.security_scorpion_app.data.DeviceModel
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface JsonPlaceHolderApi {
    @GET("devices")
    fun getDevices(): Call<List<DeviceModel>>

    @PUT("devices/{id}")
    fun updateDevice(
        @Path("id") deviceId: String,
        @Body deviceData: DeviceUpdateRequest
    ): Call<Void>

    @GET("device-groups/auth")
    fun getDevicesByAuth(
        @Query("username") username: String,
        @Query("password") password: String
    ): Call<List<DeviceModel>>

    @GET("cameras/by-group")
    fun getCamerasByGroup(
        @Path("id") groupId: String,
    ) : Call<List<CameraModel>>
}
