package com.ureka.play4change.web.badge

import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.application.port.BadgeQueryUseCase
import com.ureka.play4change.application.port.RecentEarnerDto
import com.ureka.play4change.application.port.TopicBadgeStatsDto
import com.ureka.play4change.application.port.UserBadgeDto
import com.ureka.play4change.domain.topic.PageResult
import com.ureka.play4change.auth.adapter.inbound.security.RateLimitService
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.infrastructure.config.SecurityConfig
import com.ureka.play4change.web.admin.AdminBadgeController
import com.ureka.play4change.web.user.BadgeController
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

@WebMvcTest(controllers = [BadgeController::class, AdminBadgeController::class])
@Import(SecurityConfig::class)
class BadgeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var badgeQueryUseCase: BadgeQueryUseCase

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

    private fun adminAuth(adminId: String = "admin-1") = authentication(
        UsernamePasswordAuthenticationToken(adminId, null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
    )

    @Test
    fun `GET profile badges with valid JWT returns user badges`() {
        val badge = UserBadgeDto(
            microCompetenceName = "Java Basics",
            description = "Core Java concepts",
            topicTitle = "Java Intro",
            earnedAt = OffsetDateTime.parse("2025-01-15T10:00:00Z")
        )
        every { badgeQueryUseCase.getUserBadgesPaged("user-1", 0, 50) } returns
            PageResult(content = listOf(badge), page = 0, size = 50, totalElements = 1L, totalPages = 1)

        mockMvc.perform(get("/profile/badges").with(userAuth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].microCompetenceName").value("Java Basics"))
            .andExpect(jsonPath("$.content[0].description").value("Core Java concepts"))
            .andExpect(jsonPath("$.content[0].topicTitle").value("Java Intro"))
    }

    @Test
    fun `GET profile badges without JWT returns 401`() {
        mockMvc.perform(get("/profile/badges"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET admin topics topicId badges with ADMIN JWT returns stats`() {
        val stats = TopicBadgeStatsDto(
            totalIssued = 10,
            enrolledCount = 20,
            earnedPercentage = 50.0,
            recentEarners = listOf(RecentEarnerDto("user-1", OffsetDateTime.parse("2025-01-15T10:00:00Z")))
        )
        every { badgeQueryUseCase.getTopicBadgeStats("topic-1") } returns stats

        mockMvc.perform(get("/admin/topics/topic-1/badges").with(adminAuth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalIssued").value(10))
            .andExpect(jsonPath("$.enrolledCount").value(20))
            .andExpect(jsonPath("$.earnedPercentage").value(50.0))
            .andExpect(jsonPath("$.recentEarners[0].userId").value("user-1"))
    }

    @Test
    fun `GET admin topics topicId badges with USER JWT returns 403`() {
        mockMvc.perform(get("/admin/topics/topic-1/badges").with(userAuth()))
            .andExpect(status().isForbidden)
    }
}
