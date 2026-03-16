package com.ureka.play4change.features.splash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.UrekaLogo
import com.ureka.play4change.features.splash.presentation.DefaultSplashComponent
import com.ureka.play4change.features.splash.presentation.SplashEffect
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.app_tagline
import play4change.composeapp.generated.resources.splash_loading

@Composable
fun SplashScreen(
    component: DefaultSplashComponent,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    LaunchedEffect(component) {
        component.effects.collect { effect ->
            when (effect as SplashEffect) {
                SplashEffect.NavigateToLogin -> onNavigateToLogin()
                SplashEffect.NavigateToHome  -> onNavigateToHome()
            }
        }
    }

    BaseView(component = component) { _, _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            UrekaLogo()
            Spacer(modifier = Modifier.height(Spacing.xl))
            Text(
                text = stringResource(Res.string.app_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xxxl))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.m))
            Text(
                text = stringResource(Res.string.splash_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
