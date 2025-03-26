package com.icm.security_scorpion_app.data
import java.util.UUID
data class  DeviceModel (
    var id: Long = 0L,
    var nameDevice: String,
    var ipLocal: String,
    var deviceGroupModel: DeviceGroupModel? = null
)

