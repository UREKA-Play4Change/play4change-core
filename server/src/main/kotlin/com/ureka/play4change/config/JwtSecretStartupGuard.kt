package com.ureka.play4change.config

import com.ureka.play4change.auth.application.JwtProperties
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

private const val DEV_DEFAULT_SECRET = "local-dev-secret-change-in-production-must-be-32-chars-minimum"
private const val MIN_PROD_SECRET_LENGTH = 64

@Component
class JwtSecretStartupGuard(
    private val jwtProperties: JwtProperties,
    private val environment: Environment
) {
    private val log = LoggerFactory.getLogger(JwtSecretStartupGuard::class.java)

    @PostConstruct
    fun validate() {
        val isProd = environment.activeProfiles.contains("prod")
        if (isProd) {
            check(jwtProperties.secret != DEV_DEFAULT_SECRET) {
                "JWT_SECRET is set to the development default — this is insecure in production. Set a strong random secret."
            }
            check(jwtProperties.secret.length >= MIN_PROD_SECRET_LENGTH) {
                "JWT_SECRET is too short (${jwtProperties.secret.length} chars). Production requires >= $MIN_PROD_SECRET_LENGTH characters."
            }
        } else if (jwtProperties.secret == DEV_DEFAULT_SECRET) {
            log.warn("JWT_SECRET is using the development default — do NOT use this in production")
        }
    }
}
