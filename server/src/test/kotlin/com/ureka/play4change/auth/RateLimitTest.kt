package com.ureka.play4change.auth

import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.auth.adapter.inbound.security.ManualTimeMeter
import com.ureka.play4change.auth.adapter.inbound.security.RateLimitService
import com.ureka.play4change.auth.adapter.inbound.web.AuthController
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.auth.port.inbound.AuthUseCase
import com.ureka.play4change.infra.config.SecurityConfig
import io.github.bucket4j.TimeMeter
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration

@WebMvcTest(controllers = [AuthController::class])
@Import(SecurityConfig::class, RateLimitTest.TestConfig::class, RateLimitService::class)
class RateLimitTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun timeMeter(): TimeMeter = ManualTimeMeter(0L)
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var rateLimitService: RateLimitService
    @Autowired lateinit var timeMeter: TimeMeter

    @MockkBean lateinit var authUseCase: AuthUseCase
    // TokenService implements TokenUseCase — one mock satisfies both injection points
    @MockkBean lateinit var tokenService: TokenService
    @MockkBean(relaxed = true) lateinit var meterRegistry: MeterRegistry

    @BeforeEach
    fun reset() {
        rateLimitService.clear()
        every { authUseCase.requestMagicLink(any()) } just Runs
    }

    @Test
    fun `5 magic-link requests succeed, 6th returns 429`() {
        repeat(5) {
            mockMvc.perform(
                post("/auth/magic-link")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"test@example.com"}""")
                    .header("X-Forwarded-For", "203.0.113.10")
            ).andExpect(status().isAccepted)
        }
        mockMvc.perform(
            post("/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"test@example.com"}""")
                .header("X-Forwarded-For", "203.0.113.10")
        ).andExpect(status().isTooManyRequests)
    }

    @Test
    fun `after window resets, requests succeed again`() {
        repeat(5) {
            mockMvc.perform(
                post("/auth/magic-link")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"test@example.com"}""")
                    .header("X-Forwarded-For", "203.0.113.20")
            ).andExpect(status().isAccepted)
        }
        mockMvc.perform(
            post("/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"test@example.com"}""")
                .header("X-Forwarded-For", "203.0.113.20")
        ).andExpect(status().isTooManyRequests)

        // Advance manual clock past the 10-minute window
        (timeMeter as ManualTimeMeter).addTime(Duration.ofMinutes(11))

        mockMvc.perform(
            post("/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"test@example.com"}""")
                .header("X-Forwarded-For", "203.0.113.20")
        ).andExpect(status().isAccepted)
    }

    @Test
    fun `a different IP is not rate-limited by another IP count`() {
        repeat(5) {
            mockMvc.perform(
                post("/auth/magic-link")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"test@example.com"}""")
                    .header("X-Forwarded-For", "203.0.113.30")
            ).andExpect(status().isAccepted)
        }
        // IP A is now exhausted — IP B must still succeed
        mockMvc.perform(
            post("/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"test@example.com"}""")
                .header("X-Forwarded-For", "203.0.113.31")
        ).andExpect(status().isAccepted)
    }

    @Test
    fun `rate-limited response includes Retry-After header`() {
        repeat(5) {
            mockMvc.perform(
                post("/auth/magic-link")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"test@example.com"}""")
                    .header("X-Forwarded-For", "203.0.113.40")
            ).andExpect(status().isAccepted)
        }
        mockMvc.perform(
            post("/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"test@example.com"}""")
                .header("X-Forwarded-For", "203.0.113.40")
        )
            .andExpect(status().isTooManyRequests)
            .andExpect(header().exists("Retry-After"))
    }
}
