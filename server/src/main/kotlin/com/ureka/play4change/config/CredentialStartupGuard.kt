package com.ureka.play4change.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class CredentialStartupGuard(private val environment: Environment) {

    private val log = LoggerFactory.getLogger(CredentialStartupGuard::class.java)

    @PostConstruct
    fun validate() {
        val isProd = environment.activeProfiles.contains("prod")
        val dbPassword = environment.getProperty("spring.datasource.password") ?: ""
        val minioAccessKey = environment.getProperty("minio.access-key") ?: ""
        val minioSecretKey = environment.getProperty("minio.secret-key") ?: ""

        if (isProd) {
            check(dbPassword != "play4change") {
                "SPRING_DATASOURCE_PASSWORD is set to the development default — use a strong password in production"
            }
            check(minioAccessKey != "minioadmin") {
                "MINIO_ACCESS_KEY is set to the development default — use a strong access key in production"
            }
            check(minioSecretKey != "minioadmin") {
                "MINIO_SECRET_KEY is set to the development default — use a strong secret key in production"
            }
        } else {
            if (dbPassword == "play4change") log.warn("DB password is using the development default — do NOT use this in production")
            if (minioAccessKey == "minioadmin") log.warn("MinIO access key is using the development default — do NOT use this in production")
        }
    }
}
