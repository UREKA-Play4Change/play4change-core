package com.ureka.play4change.di.features

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.features.explanation.data.http.HttpExplanationRepository
import com.ureka.play4change.features.explanation.data.mock.MockExplanationRepository
import com.ureka.play4change.features.explanation.domain.repository.ExplanationRepository
import com.ureka.play4change.features.explanation.presentation.DefaultExplanationComponent
import io.ktor.client.HttpClient
import org.koin.dsl.module

val explanationModule = module {
    single<ExplanationRepository> {
        val config = get<NetworkConfig>()
        if (config.useMocks) MockExplanationRepository()
        else HttpExplanationRepository(get<HttpClient>())
    }

    factory { (context: ComponentContext, sessionId: String) ->
        DefaultExplanationComponent(
            componentContext = context,
            sessionId = sessionId,
            repository = get()
        )
    }
}
