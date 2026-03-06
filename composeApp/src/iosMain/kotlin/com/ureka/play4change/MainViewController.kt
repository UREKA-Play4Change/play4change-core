package com.ureka.play4change

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.ureka.play4change.core.component.root.DefaultRootComponent

fun MainViewController() = ComposeUIViewController {
    val root = remember {
        val lifecycle = LifecycleRegistry()
        val root = DefaultRootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle)
        )
        lifecycle.resume()
        root
    }
    App(root)
}