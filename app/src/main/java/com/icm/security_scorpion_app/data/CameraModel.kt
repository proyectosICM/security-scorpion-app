package com.icm.security_scorpion_app.data

data class CameraModel(
    var id: Long = 0L,
    var name: String,
    var localUrl: String,
    var publicUrl: String,
    var active: Boolean,
    var usernameEncrypted: String,
    var passwordEncrypted: String,
    var deviceGroupModel: Long = 0L,

)
