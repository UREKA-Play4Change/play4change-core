package com.ureka.play4change

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.ureka.play4change.core.component.root.DefaultRootComponent
import com.ureka.play4change.di.appModule
import com.ureka.play4change.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform.getKoin

private val koinInit by lazy {
    startKoin { modules(appModule) }
}

// Called from Swift's onOpenURL handler.
private var deepLinkHandler: ((String) -> Unit)? = null

fun handleMagicLinkToken(token: String) {
    deepLinkHandler?.invoke(token)
}

/**
 * Called from Swift AppDelegate's
 * `application(_:didRegisterForRemoteNotificationsWithDeviceToken:)`.
 *
 * Swift side:
 * ```swift
 * func application(_ application: UIApplication,
 *                  didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
 *     let hex = deviceToken.map { String(format: "%02x", $0) }.joined()
 *     MainViewControllerKt.handleAPNsToken(tokenHex: hex)
 * }
 * ```
 */
fun handleAPNsToken(tokenHex: String) {
    CoroutineScope(Dispatchers.Default).launch {
        runCatching {
            getKoin().get<NotificationRepository>().registerDeviceToken(tokenHex, "IOS")
        }
    }
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
        onDispose { deepLinkHandler = null }
    }
    App(root)
}
