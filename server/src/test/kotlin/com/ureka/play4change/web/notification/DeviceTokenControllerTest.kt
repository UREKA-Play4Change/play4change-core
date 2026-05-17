package com.ureka.play4change.web.notification

import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.application.port.DeviceTokenUseCase
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.infra.config.SecurityConfig
import com.ureka.play4change.web.user.DeviceTokenController
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.justRun
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [DeviceTokenController::class])
@Import(SecurityConfig::class)
class DeviceTokenControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var deviceTokenUseCase: DeviceTokenUseCase

    @MockkBean
    private lateinit var tokenService: TokenService

    @MockkBean
    private lateinit var meterRegistry: MeterRegistry

    private fun userAuth(userId: String = "user-1") = authentication(
        UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    )

    @Test
    fun `POST notifications device-token with valid JWT and valid body returns 204`() {
        justRun { deviceTokenUseCase.register("user-1", "fcm-token-abc", "ANDROID") }

        mockMvc.perform(
            post("/notifications/device-token")
                .with(userAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"fcm-token-abc","platform":"ANDROID"}""")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `POST notifications device-token without JWT returns 401`() {
        mockMvc.perform(
            post("/notifications/device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"fcm-token-abc","platform":"ANDROID"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST notifications device-token with invalid platform returns 400`() {
        mockMvc.perform(
            post("/notifications/device-token")
                .with(userAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"fcm-token-abc","platform":"WINDOWS"}""")
        )
            .andExpect(status().isBadRequest)
    }
}
