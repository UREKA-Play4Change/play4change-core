package com.ureka.play4change.core.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.ureka.play4change.features.about.presentation.DefaultAboutComponent
import com.ureka.play4change.features.auth.presentation.DefaultLoginComponent
import com.ureka.play4change.features.home.presentation.DefaultHomeComponent
import com.ureka.play4change.features.profile.presentation.DefaultProfileComponent
import com.ureka.play4change.features.splash.presentation.DefaultSplashComponent
import com.ureka.play4change.features.task.presentation.DefaultTaskComponent
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext, KoinComponent {

    private val navigation = StackNavigation<Config>()

    override val childStack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Splash,
        handleBackButton = true,
        childFactory = ::createChild
    )

    override fun navigateToLogin()                 { navigation.replaceAll(Config.Login) }
    override fun navigateToHome()                  { navigation.replaceAll(Config.Home) }
    override fun navigateToTask(userTaskId: String){ navigation.push(Config.Task(userTaskId)) }
    override fun navigateToProfile()               { navigation.push(Config.Profile) }
    override fun navigateToAbout()                 { navigation.push(Config.About) }
    override fun navigateBack()                    { navigation.pop() }

    private fun createChild(config: Config, context: ComponentContext): RootComponent.Child {
        return when (config) {
            Config.Splash -> RootComponent.Child.Splash(
                get { parametersOf(context) }
            )
            Config.Login -> RootComponent.Child.Login(
                get { parametersOf(context, { navigation.push(Config.About) }) }
            )
            Config.Home -> RootComponent.Child.Home(
                get { parametersOf(context) }
            )
            is Config.Task -> RootComponent.Child.Task(
                get { parametersOf(context, config.userTaskId) }
            )
            Config.Profile -> RootComponent.Child.Profile(
                get { parametersOf(context) }
            )
            Config.About -> RootComponent.Child.About(
                get { parametersOf(context) }
            )
        }
    }

    @Serializable
    private sealed interface Config {
        @Serializable data object Splash  : Config
        @Serializable data object Login   : Config
        @Serializable data object Home    : Config
        @Serializable data class  Task(val userTaskId: String) : Config
        @Serializable data object Profile : Config
        @Serializable data object About   : Config
    }
}
