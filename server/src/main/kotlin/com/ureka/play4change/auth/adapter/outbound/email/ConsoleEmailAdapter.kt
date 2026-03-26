package com.ureka.play4change.auth.adapter.outbound.email

import com.ureka.play4change.auth.port.outbound.EmailPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
@ConditionalOnProperty(
    name = ["spring.mail.password"],
    havingValue = "",
    matchIfMissing = true
)
class ConsoleEmailAdapter : EmailPort {

    private val log = LoggerFactory.getLogger(ConsoleEmailAdapter::class.java)

    override fun sendMagicLink(toEmail: String, magicLink: String) {
        log.info("=== MAGIC LINK (dev mode — no real email sent) ===")
        log.info("To: $toEmail")
        log.info("Link: $magicLink")
        log.info("===================================================")
    }
}
