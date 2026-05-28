package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.DeviceTokenUseCase
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class RegisterDeviceTokenRequest(
    val token: String = "",
    val platform: String = ""
)

@RestController
@RequestMapping("/notifications/device-token")
class DeviceTokenController(private val deviceTokenUseCase: DeviceTokenUseCase) {

    @PostMapping
    fun register(
        @RequestBody request: RegisterDeviceTokenRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<Unit> {
        val validPlatforms = setOf("ANDROID", "IOS")
        val isValid = request.token.isNotBlank() && request.platform.uppercase() in validPlatforms
        return if (isValid) {
            deviceTokenUseCase.register(userId, request.token, request.platform)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.badRequest().build()
        }
    }
}
