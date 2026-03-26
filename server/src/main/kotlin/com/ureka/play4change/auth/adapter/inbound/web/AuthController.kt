package com.ureka.play4change.auth.adapter.inbound.web

import com.ureka.play4change.auth.domain.model.TokenPair
import com.ureka.play4change.auth.port.inbound.AuthUseCase
import com.ureka.play4change.auth.port.inbound.OAuthUseCase
import com.ureka.play4change.auth.port.inbound.TokenUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authUseCase: AuthUseCase,
    private val oAuthUseCase: OAuthUseCase,
    private val tokenUseCase: TokenUseCase
) {
    @PostMapping("/magic-link")
    fun requestMagicLink(@RequestBody request: MagicLinkRequest): ResponseEntity<MessageResponse> {
        authUseCase.requestMagicLink(request.email)
        return ResponseEntity.accepted().body(MessageResponse("Magic link sent. Check your email."))
    }

    @GetMapping("/verify")
    fun verifyMagicLink(@RequestParam token: String): ResponseEntity<TokenResponse> =
        ResponseEntity.ok(authUseCase.verifyMagicLink(token).toResponse())

    @PostMapping("/oauth")
    fun oauthLogin(@RequestBody request: OAuthRequest): ResponseEntity<TokenResponse> =
        ResponseEntity.ok(oAuthUseCase.loginOrRegister(request.provider, request.idToken).toResponse())

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<TokenResponse> =
        ResponseEntity.ok(tokenUseCase.refresh(request.refreshToken).toResponse())

    @DeleteMapping("/logout")
    fun logout(@RequestBody request: RefreshRequest): ResponseEntity<Void> {
        tokenUseCase.revoke(request.refreshToken)
        return ResponseEntity.noContent().build()
    }

    private fun TokenPair.toResponse() = TokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = accessExpiresInSeconds
    )
}
