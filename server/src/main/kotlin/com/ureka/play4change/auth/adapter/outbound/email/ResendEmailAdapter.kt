package com.ureka.play4change.auth.adapter.outbound.email

import com.ureka.play4change.auth.application.ResendProperties
import com.ureka.play4change.auth.port.outbound.EmailPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
@Primary
@ConditionalOnProperty(name = ["resend.api-key"], matchIfMissing = false)
class ResendEmailAdapter(private val props: ResendProperties) : EmailPort {

    private val log = LoggerFactory.getLogger(ResendEmailAdapter::class.java)
    private val restTemplate = RestTemplate()

    override fun sendMagicLink(toEmail: String, magicLink: String) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(props.apiKey)
        }
        val body = mapOf(
            "from" to props.from,
            "to" to listOf(toEmail),
            "subject" to "Your Play4Change login link",
            "text" to """
                Hello!

                Click the link below to sign in to Play4Change.
                This link expires in 15 minutes and can only be used once.

                $magicLink

                If you did not request this, ignore this email.
            """.trimIndent()
        )
        try {
            restTemplate.postForEntity(
                "https://api.resend.com/emails",
                HttpEntity(body, headers),
                Map::class.java
            )
            log.debug("Magic link email sent via Resend to {}", toEmail)
        } catch (ex: Exception) {
            log.error("Failed to send magic link email via Resend to {}: {}", toEmail, ex.message)
            throw IllegalStateException("Email delivery failed: ${ex.message}", ex)
        }
    }
}
