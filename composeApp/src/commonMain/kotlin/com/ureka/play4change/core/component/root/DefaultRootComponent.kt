package com.ureka.play4change.core.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.ureka.play4change.core.network.SessionEvent
import com.ureka.play4change.core.network.SessionEventBus
import com.ureka.play4change.core.network.TokenStorage
import com.ureka.play4change.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext, KoinComponent {

    private val navigation = StackNavigation<Config>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val authRepository: AuthRepository = get()
    private val tokenStorage: TokenStorage = get()

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        scope.launch {
            SessionEventBus.events.collect { event ->
                when (event) {
                    SessionEvent.SessionExpired -> navigateToLogin()
                }
            }
        }
    }

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

    override fun navigateToStruggle(enrollmentId: String) {
        navigation.push(Config.Struggle(enrollmentId))
    }

    override fun navigateBack() {
        navigation.pop()
    }

    override fun handleDeepLink(token: String) {
        scope.launch {
            try {
                authRepository.verifyMagicLink(token)
                navigation.replaceAll(Config.Home)
            } catch (_: Exception) {
                // Only redirect to Login when no valid session exists.
                // If the user is already authenticated (e.g. token already consumed by
                // manual entry racing with this deep link), do not disrupt their session.
                if (tokenStorage.getAccessToken() == null) {
                    navigation.replaceAll(Config.Login)
                }
            }
        }
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
            is Config.Struggle -> RootComponent.Child.Struggle(
                get { parametersOf(context, config.enrollmentId) }
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
        @Serializable data class  Struggle(val enrollmentId: String) : Config
    }
}
