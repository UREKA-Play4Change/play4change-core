package com.ureka.play4change.infra.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig(
    @Value("\${ai.generation.pool-size:3}") private val poolSize: Int
) {

    @Bean("generationExecutor")
    fun generationExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = poolSize
        maxPoolSize = poolSize
        queueCapacity = 25
        setThreadNamePrefix("gen-")
        initialize()
    }
}
