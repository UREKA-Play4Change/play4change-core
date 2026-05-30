package com.ureka.play4change.auth

import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.application.port.DeviceTokenUseCase
import com.ureka.play4change.auth.adapter.inbound.security.RateLimitService
import com.ureka.play4change.auth.adapter.inbound.web.AuthController
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.auth.port.inbound.AuthUseCase
import com.ureka.play4change.auth.port.inbound.OAuthUseCase
import com.ureka.play4change.infra.config.SecurityConfig
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AuthController::class])
@Import(SecurityConfig::class)
class AuthControllerValidationTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockkBean lateinit var authUseCase: AuthUseCase
    @MockkBean lateinit var oAuthUseCase: OAuthUseCase
    @MockkBean lateinit var deviceTokenUseCase: DeviceTokenUseCase
    @MockkBean lateinit var tokenService: TokenService
    @MockkBean lateinit var meterRegistry: MeterRegistry
    @MockkBean lateinit var rateLimitService: RateLimitService

    @BeforeEach
    fun setup() {
        every { rateLimitService.tryConsume(any(), any()) } returns true
    }

    @Test
    fun `POST auth magic-link with blank email returns 400 with error list`() {
        mockMvc.perform(
            post("/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors").isArray)
    }

    @Test
    fun `POST auth magic-link with non-email string returns 400`() {
        mockMvc.perform(
            post("/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"not-an-email"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Validation failed"))
    }

    @Test
    fun `POST auth refresh with blank refreshToken returns 400`() {
        mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Validation failed"))
    }
}
