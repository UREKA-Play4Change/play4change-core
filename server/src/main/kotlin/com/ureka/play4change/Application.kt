package com.ureka.play4change

import com.ureka.play4change.auth.application.JwtProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.ureka.play4change"])
@EnableScheduling
@EnableConfigurationProperties(JwtProperties::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}