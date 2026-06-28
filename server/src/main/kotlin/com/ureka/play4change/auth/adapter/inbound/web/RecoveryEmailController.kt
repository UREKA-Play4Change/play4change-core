package com.ureka.play4change.auth.adapter.inbound.web

import com.ureka.play4change.auth.application.RecoveryEmailService
import com.ureka.play4change.auth.domain.model.RecoveryEmail
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

data class AddRecoveryEmailRequest(
    @field:NotBlank @field:Email val email: String
)

data class RecoveryEmailResponse(
    val id: String,
    val email: String,
    val verified: Boolean
)

@RestController
class RecoveryEmailController(private val service: RecoveryEmailService) {

    @GetMapping("/account/recovery-emails")
    fun list(@AuthenticationPrincipal userId: String): ResponseEntity<List<RecoveryEmailResponse>> =
        ResponseEntity.ok(service.listForUser(userId).map { it.toResponse() })

    @PostMapping("/account/recovery-emails")
    fun add(
        @Valid @RequestBody request: AddRecoveryEmailRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<MessageResponse> {
        service.addRecoveryEmail(userId, request.email)
        return ResponseEntity.accepted().body(MessageResponse("Verification email sent. Check your inbox."))
    }

    @DeleteMapping("/account/recovery-emails/{id}")
    fun remove(
        @PathVariable id: String,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<Void> {
        service.removeRecoveryEmail(userId, id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Email link lands here as GET with token query param.
     * Redirects to the mobile deep link; the app completes verification via POST below.
     * The token is NOT consumed here.
     */
    @GetMapping("/auth/recovery-email/verify")
    fun verifyRedirect(@RequestParam token: String): ResponseEntity<Void> {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.LOCATION, "play4change://account/recovery-email/verify?token=$token")
        return ResponseEntity(headers, HttpStatus.FOUND)
    }

    /** App calls this after extracting the token from the deep link. */
    @PostMapping("/auth/recovery-email/verify")
    fun verify(@Valid @RequestBody body: MagicLinkVerifyRequest): ResponseEntity<MessageResponse> {
        service.verifyRecoveryEmail(body.token)
        return ResponseEntity.ok(MessageResponse("Recovery email verified successfully."))
    }

    private fun RecoveryEmail.toResponse() = RecoveryEmailResponse(id = id, email = email, verified = verified)
}