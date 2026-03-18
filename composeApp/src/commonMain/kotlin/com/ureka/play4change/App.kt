package com.ureka.play4change

import androidx.compose.animation.*
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
import com.ureka.play4change.features.home.ui.HomeScreen
import com.ureka.play4change.features.profile.ui.ProfileScreen
import com.ureka.play4change.features.splash.ui.SplashScreen
import com.ureka.play4change.features.task.ui.TaskScreen

@Composable
fun App(root: RootComponent) {
    UrekaTheme {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                val stack by root.childStack.subscribeAsState()
                val activeChild = stack.active.instance
                AnimatedContent(
                    targetState = activeChild,
                    transitionSpec = {
                        when (targetState) {
                            is RootComponent.Child.Task,
                            is RootComponent.Child.Profile,
                            is RootComponent.Child.About ->
                                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                            else ->
                                fadeIn() togetherWith fadeOut()
                        }
                    },
                    label = "root_navigation"
                ) { child ->
                    when (child) {
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
                                onNavigateToHome = root::navigateToHome//remove later todo()
                            )
                        is RootComponent.Child.Home ->
                            HomeScreen(
                                component = child.component,
                                onNavigateToTask = root::navigateToTask,
                                onNavigateToProfile = root::navigateToProfile,
                                onNavigateToAbout = root::navigateToAbout
                            )
                        is RootComponent.Child.Task ->
                            TaskScreen(
                                component = child.component,
                                onNavigateBack = root::navigateBack
                            )
                        is RootComponent.Child.Profile ->
                            ProfileScreen(
                                component = child.component,
                                onNavigateToAbout = root::navigateToAbout,
                                onSignedOut = root::navigateToLogin
                            )
                        is RootComponent.Child.About ->
                            AboutScreen(
                                component = child.component,
                                onNavigateBack = root::navigateBack
                            )
                    }
                }
            }
        }
    }
}
