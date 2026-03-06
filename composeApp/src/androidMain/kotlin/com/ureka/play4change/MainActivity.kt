package com.ureka.play4change

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.retainedComponent
import com.ureka.play4change.core.component.root.DefaultRootComponent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val root = retainedComponent {
            DefaultRootComponent(it)
        }
        setContent {
            App(root)
        }
    }
}