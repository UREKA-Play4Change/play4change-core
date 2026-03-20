package com.ureka.play4change.di.features

import com.ureka.play4change.features.task.data.mock.MockTaskRepository
import com.ureka.play4change.features.task.domain.repository.TaskRepository
import com.ureka.play4change.features.task.presentation.DefaultTaskComponent
import org.koin.dsl.module

val taskModule = module {
    single<TaskRepository> { MockTaskRepository() }
    factory { (context: com.arkivanov.decompose.ComponentContext,
               userTaskId: String) ->
        DefaultTaskComponent(context, userTaskId, get())
    }
}
