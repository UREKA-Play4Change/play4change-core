package com.ureka.play4change

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.retainedComponent
import com.ureka.play4change.core.component.root.DefaultRootComponent
import com.ureka.play4change.core.component.root.RootComponent

class MainActivity : ComponentActivity() {

    private lateinit var root: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        root = retainedComponent { DefaultRootComponent(it) }
        setContent { App(root) }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri = intent.data ?: return
        val token = uri.getQueryParameter("token") ?: return
        when (uri.host) {
            "auth" -> root.handleDeepLink(token)
            "account" -> root.handleRecoveryEmailVerification(token)
        }
    }
}
