package com.ureka.play4change

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.ureka.play4change.core.component.root.DefaultRootComponent
import com.ureka.play4change.di.appModule
import org.koin.core.context.startKoin

private val koinInit by lazy {
    startKoin { modules(appModule) }
}

// Called from Swift's onOpenURL handler.
private var deepLinkHandler: ((String) -> Unit)? = null
private var recoveryEmailVerificationHandler: ((String) -> Unit)? = null

fun handleMagicLinkToken(token: String) {
    deepLinkHandler?.invoke(token)
}

fun handleRecoveryEmailVerificationToken(token: String) {
    recoveryEmailVerificationHandler?.invoke(token)
}

fun MainViewController() = ComposeUIViewController {
    koinInit // ensure Koin is started before first component
    val root = remember {
        val lifecycle = LifecycleRegistry()
        val root = DefaultRootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle)
        )
        lifecycle.resume()
        root
    }
    DisposableEffect(root) {
        deepLinkHandler = { token -> root.handleDeepLink(token) }
        recoveryEmailVerificationHandler = { token -> root.handleRecoveryEmailVerification(token) }
        onDispose {
            deepLinkHandler = null
            recoveryEmailVerificationHandler = null
        }
    }
    App(root)
}
