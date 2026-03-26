package com.ureka.play4change.auth.adapter.outbound.email

import com.ureka.play4change.auth.port.outbound.EmailPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["spring.mail.password"], matchIfMissing = false)
class SmtpEmailAdapter(private val mailSender: JavaMailSender) : EmailPort {

    override fun sendMagicLink(toEmail: String, magicLink: String) {
        val message = SimpleMailMessage()
        message.setTo(toEmail)
        message.subject = "Your U!REKA login link"
        message.text = """
            Hello!

            Click the link below to sign in to U!REKA Play4Change.
            This link expires in 15 minutes and can only be used once.

            $magicLink

            If you did not request this, ignore this email.
        """.trimIndent()
        mailSender.send(message)
    }
}
