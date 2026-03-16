package com.ureka.play4change.adapter

import arrow.core.left
import com.ureka.play4change.error.server.ServiceUnavailable
import com.ureka.play4change.model.GenerationRequest
import com.ureka.play4change.model.StruggleContext
import com.ureka.play4change.port.TaskGenerationPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnMissingBean(TaskGenerationPort::class)
class NoOpTaskGenerationAdapter : TaskGenerationPort {
    override suspend fun generateTasks(request: GenerationRequest) =
        ServiceUnavailable.DependencyUnavailable("AI features are disabled (missing API Key)").left()

    override suspend fun generateAdaptiveBranch(context: StruggleContext) =
        ServiceUnavailable.DependencyUnavailable("AI features are disabled").left()

    override suspend fun healthCheck() = false
}