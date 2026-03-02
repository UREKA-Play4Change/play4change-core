package com.ureka.play4change

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.ureka.play4change.core.component.root.RootComponent
import com.ureka.play4change.features.LoginView
import com.ureka.play4change.features.SplashView

@Composable
@Preview
fun App(root: RootComponent) {
    MaterialTheme {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Children(
                    stack = root.childStack,
                    modifier = Modifier.fillMaxSize(),
                    animation = stackAnimation(slide())
                ) {
                    when (val child = it.instance) {
                        is RootComponent.Child.Splash -> SplashView(child.component)
                        is RootComponent.Child.Login -> LoginView(child.component)
                    }
                }
            }
        }
    }
}