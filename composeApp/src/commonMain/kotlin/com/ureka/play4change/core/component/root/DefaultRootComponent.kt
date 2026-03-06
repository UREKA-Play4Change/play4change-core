package com.ureka.play4change.core.component.root


import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

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

    private fun createChild(config: Config, context: ComponentContext): RootComponent.Child {
        return when (config) {
            is Config.Splash -> RootComponent.Child.Splash(
                component = {

                }
            )
            is Config.Login -> RootComponent.Child.Login(
                component = {

                }
            )
        }
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Splash : Config

        @Serializable
        data object Login: Config
    }
}