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
import com.ureka.play4change.features.auth.platform.ActivityHolder
import com.ureka.play4change.features.auth.platform.SocialLoginController

class MainActivity : ComponentActivity() {

    private lateinit var root: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Bind the activity and create the Facebook login controller before the UI loads.
        ActivityHolder.bind(this)
        SocialLoginController.current = SocialLoginController()

        root = retainedComponent { DefaultRootComponent(it) }
        setContent { App(root) }
        handleIntent(intent)
    }

    override fun onDestroy() {
        SocialLoginController.current = null
        ActivityHolder.release()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        SocialLoginController.current?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val token = intent.data
            ?.takeIf { it.path?.startsWith("/auth/verify") == true }
            ?.getQueryParameter("token")
            ?: return
        root.handleDeepLink(token)
    }
}