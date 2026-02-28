package com.ureka.play4change.core.component.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>

    sealed class Child {
        data class Splash(val component: Any) : Child()
        data class Login(val component: Any) : Child()
    }
}