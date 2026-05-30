package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.DeviceTokenUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class RegisterDeviceTokenRequest(
    @field:NotBlank @field:Size(max = 1024) val token: String = "",
    @field:NotBlank @field:Pattern(regexp = "ANDROID|IOS", message = "must be ANDROID or IOS") val platform: String = ""
)

@RestController
@RequestMapping("/notifications/device-token")
class DeviceTokenController(private val deviceTokenUseCase: DeviceTokenUseCase) {

    @PostMapping
    fun register(
        @Valid @RequestBody request: RegisterDeviceTokenRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<Unit> {
        deviceTokenUseCase.register(userId, request.token, request.platform)
        return ResponseEntity.noContent().build()
    }
}
