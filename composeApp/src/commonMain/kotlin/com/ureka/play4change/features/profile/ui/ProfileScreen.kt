package com.ureka.play4change.features.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.ShimmerBox
import com.ureka.play4change.design.components.XpBar
import com.ureka.play4change.features.profile.presentation.DefaultProfileComponent
import com.ureka.play4change.features.profile.presentation.ProfileEffect
import com.ureka.play4change.features.profile.presentation.ProfileEvents
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.cancel
import play4change.composeapp.generated.resources.ok
import play4change.composeapp.generated.resources.profile_accuracy_label
import play4change.composeapp.generated.resources.profile_points_label
import play4change.composeapp.generated.resources.profile_sign_out
import play4change.composeapp.generated.resources.profile_sign_out_confirm_body
import play4change.composeapp.generated.resources.profile_sign_out_confirm_title
import play4change.composeapp.generated.resources.profile_streak_label
import play4change.composeapp.generated.resources.profile_title
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    component: DefaultProfileComponent,
    onNavigateToAbout: () -> Unit,
    onSignedOut: () -> Unit
) {
    LaunchedEffect(component) {
        component.effects.collect { effect ->
            when (effect as ProfileEffect) {
                ProfileEffect.NavigateToAbout -> onNavigateToAbout()
                ProfileEffect.SignedOut        -> onSignedOut()
            }
        }
    }

    BaseView(component = component, screenAlignment = Alignment.TopStart) { state, onEvent ->
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(Res.string.profile_title)) }
                )
            }
        ) { innerPadding ->
            if (state.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m)
                ) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(Spacing.huge))
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(Spacing.huge))
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(Spacing.xxxl))
                }
            } else {
                val profile = state.profile
                if (profile != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(Spacing.l)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.l)
                    ) {
                        // Name + Level
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Spacing.l)) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = profile.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(Spacing.m))
                                Text(
                                    text = "Level ${profile.level}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(Spacing.xs))
                                XpBar(
                                    progress = profile.currentDay.toFloat() / profile.totalDays.toFloat()
                                )
                                Spacer(Modifier.height(Spacing.xs))
                                Text(
                                    text = "Day ${profile.currentDay} of ${profile.totalDays}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.m)
                        ) {
                            StatCard(
                                label = stringResource(Res.string.profile_streak_label),
                                value = "${profile.streakDays}🔥",
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = stringResource(Res.string.profile_points_label),
                                value = "${profile.totalPoints}⭐",
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = stringResource(Res.string.profile_accuracy_label),
                                value = "${(profile.accuracy * 100).roundToInt()}%",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // About link
                        TextButton(
                            onClick = { onEvent(ProfileEvents.OpenAbout) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "About U!REKA →",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Sign out
                        Button(
                            onClick = { onEvent(ProfileEvents.RequestSignOut) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.profile_sign_out))
                        }
                    }
                }
            }

            // Sign out dialog
            if (state.showSignOutDialog) {
                AlertDialog(
                    onDismissRequest = { onEvent(ProfileEvents.DismissSignOut) },
                    title = { Text(stringResource(Res.string.profile_sign_out_confirm_title)) },
                    text = { Text(stringResource(Res.string.profile_sign_out_confirm_body)) },
                    confirmButton = {
                        TextButton(onClick = { onEvent(ProfileEvents.ConfirmSignOut) }) {
                            Text(
                                text = stringResource(Res.string.ok),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onEvent(ProfileEvents.DismissSignOut) }) {
                            Text(stringResource(Res.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
