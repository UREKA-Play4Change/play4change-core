package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.BadgeQueryUseCase
import com.ureka.play4change.web.admin.dto.PageResponse
import com.ureka.play4change.web.user.dto.UserBadgeResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/profile")
class BadgeController(private val badgeQueryUseCase: BadgeQueryUseCase) {

    @GetMapping("/badges")
    fun getUserBadges(
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<PageResponse<UserBadgeResponse>> {
        val result = badgeQueryUseCase.getUserBadgesPaged(userId, page, size)
        return ResponseEntity.ok(
            PageResponse(
                content = result.content.map { UserBadgeResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages
            )
        )
    }
}
