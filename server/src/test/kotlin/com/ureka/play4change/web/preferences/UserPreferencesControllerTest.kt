package com.ureka.play4change.web.preferences

import arrow.core.Either
import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.application.port.UpdatePreferencesCommand
import com.ureka.play4change.application.port.UserPreferences
import com.ureka.play4change.application.port.UserPreferencesUseCase
import com.ureka.play4change.auth.adapter.inbound.security.RateLimitService
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.infra.config.SecurityConfig
import com.ureka.play4change.web.user.UserPreferencesController
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UserPreferencesController::class])
@Import(SecurityConfig::class)
class UserPreferencesControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var userPreferencesUseCase: UserPreferencesUseCase

    @MockkBean
    private lateinit var tokenService: TokenService

    @MockkBean
    private lateinit var rateLimitService: RateLimitService

    @MockkBean
    private lateinit var meterRegistry: MeterRegistry

    @BeforeEach
    fun setup() {
        every { rateLimitService.tryConsume(any(), any()) } returns true
    }

    private fun userAuth(userId: String = "user-1") = authentication(
        UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    )

    // ── PUT /profile/preferences ─────────────────────────────────────────────

    @Test
    fun `PUT profile preferences with valid JWT and valid body returns 200`() {
        every {
            userPreferencesUseCase.update(UpdatePreferencesCommand("user-1", "pt-PT", "Europe/Lisbon"))
        } returns Either.Right(UserPreferences(language = "pt-PT", timezone = "Europe/Lisbon"))

        mockMvc.perform(
            put("/profile/preferences")
                .with(userAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"language":"pt-PT","timezone":"Europe/Lisbon"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.language").value("pt-PT"))
            .andExpect(jsonPath("$.timezone").value("Europe/Lisbon"))
    }

    @Test
    fun `PUT profile preferences with invalid timezone returns 400`() {
        every {
            userPreferencesUseCase.update(UpdatePreferencesCommand("user-1", null, "Bad/Zone"))
        } returns Either.Left(BadRequest.InvalidField("timezone", "invalid ZoneId: Bad/Zone"))

        mockMvc.perform(
            put("/profile/preferences")
                .with(userAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"timezone":"Bad/Zone"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT profile preferences without JWT returns 401`() {
        mockMvc.perform(
            put("/profile/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"language":"pt-PT"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    // ── GET /profile/preferences ─────────────────────────────────────────────

    @Test
    fun `GET profile preferences returns current preferences`() {
        every { userPreferencesUseCase.get("user-1") } returns
            Either.Right(UserPreferences(language = "en", timezone = "UTC"))

        mockMvc.perform(get("/profile/preferences").with(userAuth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.language").value("en"))
            .andExpect(jsonPath("$.timezone").value("UTC"))
    }

    @Test
    fun `GET profile preferences without JWT returns 401`() {
        mockMvc.perform(get("/profile/preferences"))
            .andExpect(status().isUnauthorized)
    }
}
