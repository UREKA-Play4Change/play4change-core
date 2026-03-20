package com.ureka.play4change.features.profile.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.core.model.Badge
import com.ureka.play4change.core.model.BadgeIconType
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.ShimmerBox
import com.ureka.play4change.design.components.XpBar
import com.ureka.play4change.features.profile.presentation.DefaultProfileComponent
import com.ureka.play4change.features.profile.presentation.ProfileEffect
import com.ureka.play4change.features.profile.presentation.ProfileEvents
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.badge_explorer
import play4change.composeapp.generated.resources.badge_first_photo
import play4change.composeapp.generated.resources.badge_first_task
import play4change.composeapp.generated.resources.badge_perfect_quiz
import play4change.composeapp.generated.resources.badge_streak_3
import play4change.composeapp.generated.resources.badge_streak_7
import play4change.composeapp.generated.resources.badges_title
import play4change.composeapp.generated.resources.cancel
import play4change.composeapp.generated.resources.profile_accuracy_label
import play4change.composeapp.generated.resources.profile_course_day
import play4change.composeapp.generated.resources.profile_level_label
import play4change.composeapp.generated.resources.profile_points_label
import play4change.composeapp.generated.resources.profile_sign_out_confirm_body
import play4change.composeapp.generated.resources.profile_sign_out_confirm_title
import play4change.composeapp.generated.resources.profile_sign_out_label
import play4change.composeapp.generated.resources.profile_streak_label
import play4change.composeapp.generated.resources.profile_title
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    component: DefaultProfileComponent,
    onNavigateBack: () -> Unit,
    onSignedOut: () -> Unit
) {
    LaunchedEffect(component) {
        component.effects.collect { effect ->
            when (effect as ProfileEffect) {
                ProfileEffect.NavigateBack -> onNavigateBack()
                ProfileEffect.SignedOut    -> onSignedOut()
            }
        }
    }

    BaseView(component = component, screenAlignment = Alignment.TopStart) { state, onEvent ->

        // Sign out confirmation dialog
        if (state.showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(ProfileEvents.DismissSignOut) },
                title = { Text(stringResource(Res.string.profile_sign_out_confirm_title)) },
                text  = { Text(stringResource(Res.string.profile_sign_out_confirm_body)) },
                confirmButton = {
                    TextButton(onClick = { onEvent(ProfileEvents.ConfirmSignOut) }) {
                        Text(
                            text = stringResource(Res.string.profile_sign_out_label),
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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.profile_title)) },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(ProfileEvents.NavigateBack) }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(Res.string.cancel)
                            )
                        }
                    }
                )
            },
            // ── FIXED BOTTOM BAR — always visible, never scrolls ──────────────
            bottomBar = {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = Spacing.l, vertical = Spacing.s)
                    ) {
                        HorizontalDivider()
                        Spacer(Modifier.height(Spacing.s))
                        TextButton(
                            onClick = { onEvent(ProfileEvents.RequestSignOut) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(Res.string.profile_sign_out_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
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

                    // ── SCROLLABLE BODY ─────────────────────────────────────────────
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start  = Spacing.l,
                            end    = Spacing.l,
                            top    = Spacing.m,
                            bottom = innerPadding.calculateBottomPadding() + Spacing.xl
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.m),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {

                        // ── Stats row ──────────────────────────────────────────────
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                listOf(
                                    Triple(
                                        "${profile.streakDays}d",
                                        stringResource(Res.string.profile_streak_label),
                                        "🔥"
                                    ),
                                    Triple(
                                        profile.totalPoints.toString(),
                                        stringResource(Res.string.profile_points_label),
                                        "⚡"
                                    ),
                                    Triple(
                                        "${(profile.accuracy * 100).roundToInt()}%",
                                        stringResource(Res.string.profile_accuracy_label),
                                        "🎯"
                                    )
                                ).forEach { (value, label, icon) ->
                                    ElevatedCard(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Column(
                                            Modifier.padding(Spacing.s),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(icon, style = MaterialTheme.typography.titleLarge)
                                            Text(
                                                value,
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    .copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── XP bar ─────────────────────────────────────────────────
                        item {
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(Spacing.m)) {
                                    Text(
                                        stringResource(Res.string.profile_level_label, profile.level),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(Spacing.xs))
                                    val xpProgress by animateFloatAsState(
                                        profile.currentDay.toFloat() / profile.totalDays.toFloat(),
                                        tween(800),
                                        label = "xp"
                                    )
                                    XpBar(
                                        progress = xpProgress,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(Spacing.xs))
                                    Text(
                                        stringResource(
                                            Res.string.profile_course_day,
                                            profile.currentDay,
                                            profile.totalDays
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // ── Badges section header ──────────────────────────────────
                        if (profile.badges.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(Res.string.badges_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = Spacing.xs)
                                )
                            }

                            // ── Badges grid — rows of 3 ───────────────────────────
                            val badgeRows = profile.badges.chunked(3)
                            items(badgeRows) { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    row.forEach { badge ->
                                        BadgeItem(badge, Modifier.weight(1f))
                                    }
                                    repeat(3 - row.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                    } // end LazyColumn
                }
            }
        } // end Scaffold
    }
}

@Composable
private fun BadgeItem(badge: Badge, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (badge.isUnlocked) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeEmoji(badge.iconType),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            if (!badge.isUnlocked) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Lock, null,
                        Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = resolveBadgeTitle(badge.titleKey),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            maxLines = 2
        )
    }
}

private fun badgeEmoji(iconType: BadgeIconType): String = when (iconType) {
    BadgeIconType.FIRST_STEP -> "👣"
    BadgeIconType.FLAME      -> "🔥"
    BadgeIconType.CALENDAR   -> "📅"
    BadgeIconType.STAR       -> "⭐"
    BadgeIconType.CAMERA     -> "📷"
    BadgeIconType.COMPASS    -> "🧭"
}

@Composable
private fun resolveBadgeTitle(key: String): String = when (key) {
    "badge_first_task"   -> stringResource(Res.string.badge_first_task)
    "badge_streak_3"     -> stringResource(Res.string.badge_streak_3)
    "badge_streak_7"     -> stringResource(Res.string.badge_streak_7)
    "badge_perfect_quiz" -> stringResource(Res.string.badge_perfect_quiz)
    "badge_first_photo"  -> stringResource(Res.string.badge_first_photo)
    "badge_explorer"     -> stringResource(Res.string.badge_explorer)
    else                 -> key
}
