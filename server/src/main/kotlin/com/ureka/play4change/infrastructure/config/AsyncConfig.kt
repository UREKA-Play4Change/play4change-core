package com.ureka.play4change.infrastructure.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionHandler

@Configuration
@EnableAsync
class AsyncConfig(
    @Value("\${ai.generation.pool-size:3}") private val poolSize: Int,
    @Value("\${ai.generation.queue-capacity:25}") private val queueCapacity: Int
) {

    private val log = LoggerFactory.getLogger(AsyncConfig::class.java)

    @Bean("generationExecutor")
    fun generationExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        corePoolSize = poolSize
        maxPoolSize = poolSize
        setQueueCapacity(this@AsyncConfig.queueCapacity)
        setThreadNamePrefix("gen-")
        setKeepAliveSeconds(60)
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(60)
        setRejectedExecutionHandler(RejectedExecutionHandler { r, _ ->
            log.error(
                "Generation task rejected — pool saturated (pool={}, queue={}). Task dropped: {}",
                poolSize, queueCapacity, r
            )
        })
        initialize()
    }

    /**
     * Coroutine scope backed by the same thread pool as [generationExecutor].
     * [TaskGenerationOrchestrator] and [HandleStruggleService] use this scope instead
     * of `runBlocking` so that:
     *   - The thread is not pinned for the duration of the AI call; structured coroutines
     *     can suspend on the same dispatcher.
     *   - Cancellation propagates correctly through `withTimeout`.
     *   - [SupervisorJob] ensures one failing generation doesn't cancel other in-flight jobs.
     */
    @Bean("generationCoroutineScope")
    fun generationCoroutineScope(
        @Qualifier("generationExecutor") executor: Executor
    ): CoroutineScope = CoroutineScope(executor.asCoroutineDispatcher() + SupervisorJob())
}
