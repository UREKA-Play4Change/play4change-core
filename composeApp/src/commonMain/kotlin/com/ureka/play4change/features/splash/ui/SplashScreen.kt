package com.ureka.play4change.features.splash.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.LogoSize
import com.ureka.play4change.design.components.UrekaLogo
import com.ureka.play4change.features.splash.presentation.DefaultSplashComponent
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.app_tagline

@Composable
fun SplashScreen(component: DefaultSplashComponent) {
    BaseView(component = component, contentAlignment = Alignment.Center) { _, _, _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UrekaLogo(size = LogoSize.Large)

                Spacer(Modifier.height(Spacing.l))

                var taglineVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(500); taglineVisible = true }
                AnimatedVisibility(
                    taglineVisible,
                    enter = fadeIn(tween(500)) + slideInVertically { it / 3 }
                ) {
                    Text(
                        text = stringResource(Res.string.app_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            var loaderVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(800); loaderVisible = true }
            AnimatedVisibility(
                loaderVisible,
                enter = fadeIn(tween(400)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.xxxl)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.width(120.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondaryContainer,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}
