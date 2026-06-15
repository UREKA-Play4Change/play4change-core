package com.ureka.play4change.web.profile

import arrow.core.Either
import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.application.port.UpdateProfileNameCommand
import com.ureka.play4change.application.port.UpdateProfileNameUseCase
import com.ureka.play4change.application.port.GetUserProfileUseCase
import com.ureka.play4change.application.user.UserProfile
import com.ureka.play4change.auth.adapter.inbound.security.RateLimitService
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.NotFound
import com.ureka.play4change.infrastructure.config.SecurityConfig
import com.ureka.play4change.web.user.UserProfileController
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UserProfileController::class])
@Import(SecurityConfig::class)
class UserProfileControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var getUserProfileUseCase: GetUserProfileUseCase

    @MockkBean
    private lateinit var updateProfileNameUseCase: UpdateProfileNameUseCase

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

    private val stubProfile = UserProfile(
        userId = "user-1",
        name = "Radesh",
        email = "user@example.com",
        streakDays = 5,
        totalPoints = 150,
        accuracy = 0.85f,
        preferredLanguage = "en"
    )

    private fun userAuth(userId: String = "user-1") = authentication(
        UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    )

    // ── GET /profile ─────────────────────────────────────────────────────────

    @Test
    fun `GET profile with valid JWT returns 200 with preferredLanguage`() {
        every { getUserProfileUseCase.execute("user-1") } returns Either.Right(stubProfile)

        mockMvc.perform(get("/profile").with(userAuth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Radesh"))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.preferredLanguage").value("en"))
            .andExpect(jsonPath("$.streakDays").value(5))
            .andExpect(jsonPath("$.totalPoints").value(150))
    }

    @Test
    fun `GET profile without JWT returns 401`() {
        mockMvc.perform(get("/profile"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET profile when user not found returns 404`() {
        every { getUserProfileUseCase.execute("user-1") } returns
            Either.Left(NotFound.ResourceNotFound("User", "user-1"))

        mockMvc.perform(get("/profile").with(userAuth()))
            .andExpect(status().isNotFound)
    }

    // ── PATCH /profile ────────────────────────────────────────────────────────

    @Test
    fun `PATCH profile with valid JWT and valid name returns 200 with updated name`() {
        every {
            updateProfileNameUseCase.execute(UpdateProfileNameCommand("user-1", "Radesh"))
        } returns Either.Right(stubProfile)

        mockMvc.perform(
            patch("/profile")
                .with(userAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Radesh"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Radesh"))
            .andExpect(jsonPath("$.preferredLanguage").value("en"))
    }

    @Test
    fun `PATCH profile without JWT returns 401`() {
        mockMvc.perform(
            patch("/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Radesh"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `PATCH profile with blank name returns 400`() {
        every {
            updateProfileNameUseCase.execute(UpdateProfileNameCommand("user-1", ""))
        } returns Either.Left(BadRequest.InvalidField("name", "name must not be blank"))

        mockMvc.perform(
            patch("/profile")
                .with(userAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""}""")
        )
            .andExpect(status().isBadRequest)
    }
}
