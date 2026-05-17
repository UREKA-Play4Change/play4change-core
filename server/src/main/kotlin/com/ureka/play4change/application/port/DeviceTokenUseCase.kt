package com.ureka.play4change.application.port

interface DeviceTokenUseCase {
    fun register(userId: String, token: String, platform: String)
    fun deleteForUser(userId: String)
}
