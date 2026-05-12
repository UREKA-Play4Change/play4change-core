package com.ureka.play4change.auth.adapter.inbound.web

import com.ureka.play4change.auth.domain.model.TokenPair
import com.ureka.play4change.auth.port.inbound.AuthUseCase
import com.ureka.play4change.auth.port.inbound.OAuthUseCase
import com.ureka.play4change.auth.port.inbound.TokenUseCase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
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

    /**
     * Email link lands here as GET with token query param.
     * Redirects to the mobile app's custom URL scheme so the learner's device opens
     * the app and completes verification in-app via POST /auth/magic-link/verify.
     * The token is NOT consumed here — consumption happens in [verifyMagicLinkPost].
     */
    @GetMapping("/verify")
    fun verifyMagicLink(@RequestParam token: String): ResponseEntity<Void> {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.LOCATION, "play4change://auth/verify?token=$token")
        return ResponseEntity(headers, HttpStatus.FOUND)
    }

    /** Web frontend variant — POST body `{ "token": "..." }`. */
    @PostMapping("/magic-link/verify")
    fun verifyMagicLinkPost(@RequestBody body: MagicLinkVerifyRequest): ResponseEntity<TokenResponse> =
        ResponseEntity.ok(authUseCase.verifyMagicLink(body.token).toResponse())

    @PostMapping("/oauth")
    fun oauthLogin(@RequestBody request: OAuthRequest): ResponseEntity<TokenResponse> =
        ResponseEntity.ok(oAuthUseCase.loginOrRegister(request.provider, request.resolvedToken()).toResponse())

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<TokenResponse> =
        ResponseEntity.ok(tokenUseCase.refresh(request.refreshToken).toResponse())

    @PostMapping("/logout")
    fun logoutPost(@RequestBody request: RefreshRequest): ResponseEntity<Void> {
        tokenUseCase.revoke(request.refreshToken)
        return ResponseEntity.noContent().build()
    }

    private fun TokenPair.toResponse() = TokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = accessExpiresInSeconds
    )
}
