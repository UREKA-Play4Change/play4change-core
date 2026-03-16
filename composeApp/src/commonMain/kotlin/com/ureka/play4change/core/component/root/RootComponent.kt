package com.ureka.play4change.core.component.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.ureka.play4change.features.about.presentation.DefaultAboutComponent
import com.ureka.play4change.features.auth.presentation.DefaultLoginComponent
import com.ureka.play4change.features.home.presentation.DefaultHomeComponent
import com.ureka.play4change.features.profile.presentation.DefaultProfileComponent
import com.ureka.play4change.features.splash.presentation.DefaultSplashComponent
import com.ureka.play4change.features.task.presentation.DefaultTaskComponent

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>

    fun navigateToLogin()
    fun navigateToHome()
    fun navigateToTask(userTaskId: String)
    fun navigateToProfile()
    fun navigateToAbout()
    fun navigateBack()

    sealed class Child {
        data class Splash(val component: DefaultSplashComponent) : Child()
        data class Login(val component: DefaultLoginComponent) : Child()
        data class Home(val component: DefaultHomeComponent) : Child()
        data class Task(val component: DefaultTaskComponent) : Child()
        data class Profile(val component: DefaultProfileComponent) : Child()
        data class About(val component: DefaultAboutComponent) : Child()
    }
}
