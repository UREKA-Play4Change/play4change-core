package com.ureka.play4change.di.features

import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.features.task.data.http.HttpTaskRepository
import com.ureka.play4change.features.task.data.mock.MockTaskRepository
import com.ureka.play4change.features.task.domain.repository.TaskRepository
import com.ureka.play4change.features.task.presentation.DefaultTaskComponent
import io.ktor.client.HttpClient
import org.koin.dsl.module

val taskModule = module {
    single<TaskRepository> {
        val config = get<NetworkConfig>()
        if (config.useMocks) MockTaskRepository()
        else HttpTaskRepository(get<HttpClient>())
    }
    factory { (context: com.arkivanov.decompose.ComponentContext,
               userTaskId: String) ->
        DefaultTaskComponent(context, userTaskId, get())
    }
}
