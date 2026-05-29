package com.ureka.play4change.web.swagger

import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.application.port.DeviceTokenUseCase
import com.ureka.play4change.auth.adapter.inbound.security.RateLimitService
import com.ureka.play4change.auth.adapter.inbound.web.AuthController
import com.ureka.play4change.auth.application.AccessTokenClaims
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

// Default profile: Swagger accessible without authentication

@WebMvcTest(controllers = [AuthController::class])
@Import(SecurityConfig::class)
class SwaggerDefaultProfileTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockkBean lateinit var tokenService: TokenService
    @MockkBean lateinit var meterRegistry: MeterRegistry
    @MockkBean lateinit var rateLimitService: RateLimitService
    @MockkBean lateinit var authUseCase: AuthUseCase
    @MockkBean lateinit var oAuthUseCase: OAuthUseCase
    @MockkBean lateinit var deviceTokenUseCase: DeviceTokenUseCase

    @BeforeEach
    fun setup() {
        every { rateLimitService.tryConsume(any(), any()) } returns true
    }

    @Test
    fun `swagger is accessible without auth in default profile`() {
        // In @WebMvcTest context there is no real Swagger resource handler so the response will be
        // 404 (no controller matched) rather than 200. The important assertion is that security
        // did NOT block the request with 401/403 — it let it through.
        val result = mockMvc.perform(get("/swagger-ui.html")).andReturn()
        val status = result.response.status
        assert(status != 401 && status != 403) {
            "Expected security to allow Swagger in default profile but got HTTP $status"
        }
    }
}

// Prod profile: Swagger is protected

@WebMvcTest(controllers = [AuthController::class])
@Import(SecurityConfig::class)
@ActiveProfiles("prod")
class SwaggerAccessControlTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockkBean lateinit var tokenService: TokenService
    @MockkBean lateinit var meterRegistry: MeterRegistry
    @MockkBean lateinit var rateLimitService: RateLimitService
    @MockkBean lateinit var authUseCase: AuthUseCase
    @MockkBean lateinit var oAuthUseCase: OAuthUseCase
    @MockkBean lateinit var deviceTokenUseCase: DeviceTokenUseCase

    @BeforeEach
    fun setup() {
        every { rateLimitService.tryConsume(any(), any()) } returns true
    }

    @Test
    fun `swagger returns 401 without JWT in prod profile`() {
        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `swagger returns 403 with USER JWT in prod profile`() {
        val auth = UsernamePasswordAuthenticationToken(
            "user-1", null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        mockMvc.perform(get("/swagger-ui.html").with(authentication(auth)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `swagger is reachable with ADMIN JWT in prod profile`() {
        every { tokenService.parseAccessToken(any()) } returns AccessTokenClaims("admin-1", "ADMIN")
        val auth = UsernamePasswordAuthenticationToken(
            "admin-1", null,
            listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        )
        // Security allows ADMIN through; 404 is expected in @WebMvcTest (no real Swagger handler)
        val result = mockMvc.perform(get("/swagger-ui.html").with(authentication(auth))).andReturn()
        val status = result.response.status
        assert(status != 401 && status != 403) {
            "Expected ADMIN to access Swagger in prod profile but got HTTP $status"
        }
    }
}
