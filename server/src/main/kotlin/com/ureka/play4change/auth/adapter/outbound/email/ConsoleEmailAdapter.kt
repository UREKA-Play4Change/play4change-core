package com.ureka.play4change.auth.adapter.outbound.email

import com.ureka.play4change.auth.port.outbound.EmailPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnMissingBean(EmailPort::class)
class ConsoleEmailAdapter : EmailPort {

    private val log = LoggerFactory.getLogger(ConsoleEmailAdapter::class.java)

    override fun sendMagicLink(toEmail: String, magicLink: String) {
        log.info("=== MAGIC LINK (dev mode — no real email sent) ===")
        log.info("To: $toEmail")
        log.info("Link: $magicLink")
        log.info("===================================================")
    }
}
