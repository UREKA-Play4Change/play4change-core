package com.ureka.play4change

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.ureka.play4change.core.component.root.RootComponent
import com.ureka.play4change.design.UrekaTheme
import com.ureka.play4change.features.about.presentation.AboutEffect
import com.ureka.play4change.features.about.ui.AboutScreen
import com.ureka.play4change.features.auth.presentation.LoginEffect
import com.ureka.play4change.features.auth.ui.LoginScreen
import com.ureka.play4change.features.explore.presentation.ExploreEffect
import com.ureka.play4change.features.explore.ui.ExploreScreen
import com.ureka.play4change.features.home.presentation.HomeEffect
import com.ureka.play4change.features.home.ui.HomeScreen
import com.ureka.play4change.features.profile.presentation.ProfileEffect
import com.ureka.play4change.features.profile.ui.ProfileScreen
import com.ureka.play4change.features.splash.presentation.SplashEffect
import com.ureka.play4change.features.splash.ui.SplashScreen
import com.ureka.play4change.features.task.presentation.TaskEffect
import com.ureka.play4change.features.task.ui.TaskScreen

@Composable
fun App(root: RootComponent) {
    UrekaTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            val childStack by root.childStack.subscribeAsState()
            AnimatedContent(
                targetState = childStack,
                transitionSpec = {
                    val forward = targetState.items.size >= initialState.items.size
                    val easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
                    if (forward) {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(350, easing = easing)
                        ) + fadeIn(tween(200)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = tween(350, easing = easing)
                        ) + fadeOut(tween(150))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { -it / 3 },
                            animationSpec = tween(350, easing = easing)
                        ) + fadeIn(tween(200)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(350, easing = easing)
                        ) + fadeOut(tween(150))
                    }
                },
                contentKey = { it.active.instance::class },
                label = "root_navigation"
            ) { stack ->
                when (val child = stack.active.instance) {
                    is RootComponent.Child.Splash -> {
                        LaunchedEffect(child.component) {
                            child.component.effects.collect { effect ->
                                when (effect as SplashEffect) {
                                    SplashEffect.NavigateToLogin -> root.navigateToLogin()
                                    SplashEffect.NavigateToHome  -> root.navigateToHome()
                                }
                            }
                        }
                        SplashScreen(child.component)
                    }
                    is RootComponent.Child.Login -> {
                        LaunchedEffect(child.component) {
                            child.component.effects.collect { effect ->
                                when (effect as LoginEffect) {
                                    LoginEffect.NavigateToAbout -> root.navigateToAbout()
                                    LoginEffect.NavigateToHome  -> root.navigateToHome()
                                }
                            }
                        }
                        LoginScreen(child.component)
                    }
                    is RootComponent.Child.Home -> {
                        LaunchedEffect(child.component) {
                            child.component.effects.collect { effect ->
                                when (val e = effect as HomeEffect) {
                                    is HomeEffect.NavigateToTask -> root.navigateToTask(e.userTaskId)
                                    HomeEffect.NavigateToProfile -> root.navigateToProfile()
                                    HomeEffect.NavigateToAbout   -> root.navigateToAbout()
                                    HomeEffect.NavigateToExplore -> root.navigateToExplore()
                                    HomeEffect.LoggedOut         -> root.navigateToLogin()
                                }
                            }
                        }
                        HomeScreen(child.component)
                    }
                    is RootComponent.Child.Task -> {
                        LaunchedEffect(child.component) {
                            child.component.effects.collect { effect ->
                                when (effect as TaskEffect) {
                                    TaskEffect.NavigateBack -> root.navigateBack()
                                }
                            }
                        }
                        TaskScreen(child.component)
                    }
                    is RootComponent.Child.Profile -> {
                        LaunchedEffect(child.component) {
                            child.component.effects.collect { effect ->
                                when (effect as ProfileEffect) {
                                    ProfileEffect.NavigateBack -> root.navigateBack()
                                }
                            }
                        }
                        ProfileScreen(child.component)
                    }
                    is RootComponent.Child.About -> {
                        LaunchedEffect(child.component) {
                            child.component.effects.collect { effect ->
                                when (effect as AboutEffect) {
                                    AboutEffect.NavigateBack -> root.navigateBack()
                                }
                            }
                        }
                        AboutScreen(child.component)
                    }
                    is RootComponent.Child.Explore -> {
                        LaunchedEffect(child.component) {
                            child.component.effects.collect { effect ->
                                when (effect as ExploreEffect) {
                                    ExploreEffect.NavigateBack  -> root.navigateBack()
                                    ExploreEffect.TopicSwitched -> { /* optional: show snackbar */ }
                                }
                            }
                        }
                        ExploreScreen(child.component)
                    }
                }
            }
        }
    }
}
