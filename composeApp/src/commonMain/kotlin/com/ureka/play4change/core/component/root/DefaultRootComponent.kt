package com.ureka.play4change.core.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
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

    override fun navigateToLogin() {
        navigation.replaceAll(Config.Login)
    }

    override fun navigateToHome() {
        navigation.replaceAll(Config.Home)
    }

    override fun navigateToTask(userTaskId: String) {
        if (childStack.value.active.configuration != Config.Task(userTaskId)) {
            navigation.push(Config.Task(userTaskId))
        }
    }

    override fun navigateToProfile() {
        if (childStack.value.active.configuration != Config.Profile) {
            navigation.push(Config.Profile)
        }
    }

    override fun navigateToAbout() {
        if (childStack.value.active.configuration != Config.About) {
            navigation.push(Config.About)
        }
    }

    override fun navigateToExplore() {
        if (childStack.value.active.configuration != Config.Explore) {
            navigation.push(Config.Explore)
        }
    }

    override fun navigateBack() {
        navigation.pop()
    }

    private fun createChild(config: Config, context: ComponentContext): RootComponent.Child {
        return when (config) {
            Config.Splash -> RootComponent.Child.Splash(
                get { parametersOf(context) }
            )
            Config.Login -> RootComponent.Child.Login(
                get { parametersOf(context) }
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
            Config.Explore -> RootComponent.Child.Explore(
                get { parametersOf(context, { navigateBack() }) }
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
        @Serializable data object Explore : Config
    }
}
