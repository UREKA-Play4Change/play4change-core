package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.BadgeQueryUseCase
import com.ureka.play4change.web.user.dto.UserBadgeResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/profile")
class BadgeController(private val badgeQueryUseCase: BadgeQueryUseCase) {

    @GetMapping("/badges")
    fun getUserBadges(@AuthenticationPrincipal userId: String): ResponseEntity<List<UserBadgeResponse>> =
        ResponseEntity.ok(badgeQueryUseCase.getUserBadges(userId).map { UserBadgeResponse.from(it) })
}
