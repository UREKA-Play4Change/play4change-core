package com.ureka.play4change.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.language")
class LanguageProperties {
    var supportedLanguages: List<String> = listOf("en")
}
