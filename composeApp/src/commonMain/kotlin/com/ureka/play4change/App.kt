package com.ureka.play4change

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.ureka.play4change.core.component.root.RootComponent
import com.ureka.play4change.design.UrekaTheme
import com.ureka.play4change.features.about.ui.AboutScreen
import com.ureka.play4change.features.auth.ui.LoginScreen
import com.ureka.play4change.features.explore.ui.ExploreScreen
import com.ureka.play4change.features.home.ui.HomeScreen
import com.ureka.play4change.features.profile.ui.ProfileScreen
import com.ureka.play4change.features.splash.ui.SplashScreen
import com.ureka.play4change.features.task.ui.TaskScreen

@Composable
fun App(root: RootComponent) {
    UrekaTheme {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                        is RootComponent.Child.Splash ->
                            SplashScreen(
                                component = child.component,
                                onNavigateToLogin = root::navigateToLogin,
                                onNavigateToHome = root::navigateToHome
                            )
                        is RootComponent.Child.Login ->
                            LoginScreen(
                                component = child.component,
                                onNavigateToAbout = root::navigateToAbout,
                                onNavigateToHome = root::navigateToHome
                            )
                        is RootComponent.Child.Home ->
                            HomeScreen(
                                component = child.component,
                                onNavigateToTask = root::navigateToTask,
                                onNavigateToProfile = root::navigateToProfile,
                                onNavigateToAbout = root::navigateToAbout,
                                onNavigateToExplore = root::navigateToExplore
                            )
                        is RootComponent.Child.Task ->
                            TaskScreen(
                                component = child.component,
                                onNavigateBack = root::navigateBack
                            )
                        is RootComponent.Child.Profile ->
                            ProfileScreen(
                                component = child.component,
                                onNavigateBack = root::navigateBack,
                                onNavigateToAbout = root::navigateToAbout,
                                onSignedOut = root::navigateToLogin
                            )
                        is RootComponent.Child.About ->
                            AboutScreen(
                                component = child.component,
                                onNavigateBack = root::navigateBack
                            )
                        is RootComponent.Child.Explore ->
                            ExploreScreen(
                                component = child.component,
                                onNavigateBack = root::navigateBack
                            )
                    }
                }
            }
        }
    }
}
