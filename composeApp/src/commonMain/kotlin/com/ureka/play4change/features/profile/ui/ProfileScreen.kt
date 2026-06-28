package com.ureka.play4change.features.profile.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.core.model.Badge
import com.ureka.play4change.core.model.BadgeIconType
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.ShimmerBox
import com.ureka.play4change.features.profile.domain.model.RecoveryEmail
import com.ureka.play4change.features.profile.presentation.DefaultProfileComponent
import com.ureka.play4change.features.profile.presentation.ProfileEvents
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.badge_explorer
import play4change.composeapp.generated.resources.badge_first_photo
import play4change.composeapp.generated.resources.badge_first_task
import play4change.composeapp.generated.resources.badge_perfect_quiz
import play4change.composeapp.generated.resources.badge_streak_3
import play4change.composeapp.generated.resources.badge_streak_7
import play4change.composeapp.generated.resources.badges_empty
import play4change.composeapp.generated.resources.badges_title
import play4change.composeapp.generated.resources.cancel
import play4change.composeapp.generated.resources.pagination_next
import play4change.composeapp.generated.resources.pagination_page_of
import play4change.composeapp.generated.resources.pagination_previous
import play4change.composeapp.generated.resources.profile_edit_name
import play4change.composeapp.generated.resources.profile_language_english
import play4change.composeapp.generated.resources.profile_language_label
import play4change.composeapp.generated.resources.profile_name_hint
import play4change.composeapp.generated.resources.profile_points_label
import play4change.composeapp.generated.resources.profile_preferences_section
import play4change.composeapp.generated.resources.profile_save
import play4change.composeapp.generated.resources.profile_streak_label
import play4change.composeapp.generated.resources.profile_title
import play4change.composeapp.generated.resources.recovery_email_add
import play4change.composeapp.generated.resources.recovery_email_add_dialog_title
import play4change.composeapp.generated.resources.recovery_email_confirm
import play4change.composeapp.generated.resources.recovery_email_input_label
import play4change.composeapp.generated.resources.recovery_email_load_error
import play4change.composeapp.generated.resources.recovery_email_max_reached
import play4change.composeapp.generated.resources.recovery_email_pending
import play4change.composeapp.generated.resources.recovery_email_remove
import play4change.composeapp.generated.resources.recovery_email_verified
import play4change.composeapp.generated.resources.recovery_email_verify_dialog_hint
import play4change.composeapp.generated.resources.recovery_email_verify_dialog_title
import play4change.composeapp.generated.resources.recovery_email_verify_submit
import play4change.composeapp.generated.resources.recovery_email_verify_token_label
import play4change.composeapp.generated.resources.recovery_emails_section

private const val BADGE_PAGE_SIZE = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(component: DefaultProfileComponent) {
    BaseView(
        component = component,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = { component.onEvent(ProfileEvents.NavigateBack) }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.cancel)
                        )
                    }
                }
            )
        }
    ) { state, _, innerPadding ->

        if (state.languagePickerVisible) {
            LanguagePickerDialog(
                currentLanguage = state.profile?.preferredLanguage ?: "en",
                onSelect = { component.onEvent(ProfileEvents.LanguageSelected(it)) },
                onDismiss = { component.onEvent(ProfileEvents.DismissLanguagePicker) }
            )
        }

        if (state.addEmailDialogVisible) {
            AddRecoveryEmailDialog(
                emailInput = state.recoveryEmailInput,
                isSaving = state.isSavingRecoveryEmail,
                dialogError = state.recoveryEmailDialogError,
                onInputChange = { component.onEvent(ProfileEvents.RecoveryEmailInputChanged(it)) },
                onConfirm = { component.onEvent(ProfileEvents.SubmitAddRecoveryEmail) },
                onDismiss = { component.onEvent(ProfileEvents.DismissAddRecoveryEmailDialog) }
            )
        }

        if (state.verifyDialogVisible) {
            VerifyRecoveryEmailDialog(
                tokenInput = state.verifyTokenInput,
                isVerifying = state.isVerifyingRecoveryEmail,
                dialogError = state.verifyDialogError,
                onInputChange = { component.onEvent(ProfileEvents.VerifyTokenInputChanged(it)) },
                onConfirm = { component.onEvent(ProfileEvents.SubmitVerifyRecoveryEmail) },
                onDismiss = { component.onEvent(ProfileEvents.DismissVerifyRecoveryEmailDialog) }
            )
        }

        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(460.dp)
                    .offset(y = (-160).dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .align(Alignment.TopCenter)
            )
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .offset(x = (-100).dp, y = 100.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        CircleShape
                    )
                    .align(Alignment.BottomStart)
            )
        }
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(Spacing.xxxl))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(Spacing.huge))
            }
        } else {
            val profile = state.profile
            if (profile != null) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start  = Spacing.l,
                        end    = Spacing.l,
                        top    = innerPadding.calculateTopPadding() + Spacing.m,
                        bottom = innerPadding.calculateBottomPadding() + Spacing.xl
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                    modifier = Modifier.fillMaxSize()
                ) {

                    // ── Gradient hero card ─────────────────────────────────────
                    item {
                        val heroGradient = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                        val animatedPoints by animateIntAsState(
                            targetValue = profile.totalPoints,
                            animationSpec = tween(900),
                            label = "pts"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(Brush.linearGradient(heroGradient))
                                .padding(Spacing.xl)
                        ) {
                            Column {
                                // Name / edit row
                                if (state.isEditingName) {
                                    OutlinedTextField(
                                        value = state.nameInput,
                                        onValueChange = {
                                            component.onEvent(ProfileEvents.NameInputChanged(it))
                                        },
                                        label = { Text(stringResource(Res.string.profile_name_hint)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !state.isSavingName
                                    )
                                    Spacer(Modifier.height(Spacing.s))
                                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                        TextButton(
                                            onClick = { component.onEvent(ProfileEvents.CancelEditName) },
                                            enabled = !state.isSavingName
                                        ) {
                                            Text(stringResource(Res.string.cancel))
                                        }
                                        TextButton(
                                            onClick = { component.onEvent(ProfileEvents.SaveName) },
                                            enabled = state.nameInput.trim().length >= 2 && !state.isSavingName
                                        ) {
                                            Text(stringResource(Res.string.profile_save))
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profile.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = profile.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { component.onEvent(ProfileEvents.EditName) }
                                        ) {
                                            Icon(
                                                Icons.Rounded.Edit,
                                                contentDescription = stringResource(Res.string.profile_edit_name),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(Spacing.l))

                                // Stats row — streak + points
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatColumn(
                                        emoji = "🔥",
                                        value = "${profile.streakDays}d",
                                        label = stringResource(Res.string.profile_streak_label),
                                        valueColor = MaterialTheme.colorScheme.tertiary
                                    )
                                    StatColumn(
                                        emoji = "⚡",
                                        value = "$animatedPoints",
                                        label = stringResource(Res.string.profile_points_label),
                                        valueColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // ── Preferences section ────────────────────────────────────
                    item {
                        SectionHeader(stringResource(Res.string.profile_preferences_section))
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { component.onEvent(ProfileEvents.ShowLanguagePicker) }
                                .padding(vertical = Spacing.m, horizontal = Spacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌐", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.size(Spacing.m))
                            Text(
                                text = stringResource(Res.string.profile_language_label),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = languageDisplayName(
                                    profile.preferredLanguage,
                                    stringResource(Res.string.profile_language_english)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.size(Spacing.xs))
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // ── Recovery Emails section ────────────────────────────────
                    item {
                        val count = if (!state.isLoadingRecoveryEmails && !state.recoveryEmailLoadFailed)
                            state.recoveryEmails.size else -1
                        SectionHeader(
                            title = stringResource(Res.string.recovery_emails_section),
                            trailing = if (count >= 0) "$count/3" else null
                        )
                    }

                    when {
                        state.isLoadingRecoveryEmails -> item {
                            ShimmerBox(modifier = Modifier.fillMaxWidth().height(64.dp))
                        }
                        state.recoveryEmailLoadFailed -> item {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.m),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                                ) {
                                    Icon(
                                        Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.recovery_email_load_error),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        else -> items(state.recoveryEmails) { email ->
                            RecoveryEmailItem(
                                email = email,
                                onVerify = { component.onEvent(ProfileEvents.ShowVerifyRecoveryEmailDialog) },
                                onRemove = { component.onEvent(ProfileEvents.RemoveRecoveryEmail(email.id)) }
                            )
                        }
                    }

                    item {
                        val atLimit = state.recoveryEmails.size >= 3
                        if (atLimit) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                                ) {
                                    Icon(
                                        Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.recovery_email_max_reached),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Surface(
                                onClick = { component.onEvent(ProfileEvents.ShowAddRecoveryEmailDialog) },
                                shape = MaterialTheme.shapes.medium,
                                color = Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.m),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.size(Spacing.xs))
                                    Text(
                                        text = stringResource(Res.string.recovery_email_add),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // ── Badges section ─────────────────────────────────────────
                    item {
                        SectionHeader(stringResource(Res.string.badges_title))
                    }

                    val badgesOnPage = profile.badges
                        .drop(state.badgePage * BADGE_PAGE_SIZE)
                        .take(BADGE_PAGE_SIZE)
                    val totalBadgePages = maxOf(1, (profile.badges.size + BADGE_PAGE_SIZE - 1) / BADGE_PAGE_SIZE)

                    if (badgesOnPage.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.badges_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.l)
                            )
                        }
                    } else {
                        val badgeRows = badgesOnPage.chunked(3)
                        items(badgeRows) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.m)
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

                    if (totalBadgePages > 1) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.s),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { component.onEvent(ProfileEvents.PreviousBadgePage) },
                                    enabled = state.badgePage > 0
                                ) {
                                    Text(stringResource(Res.string.pagination_previous))
                                }
                                Text(
                                    text = stringResource(Res.string.pagination_page_of, state.badgePage + 1, totalBadgePages),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { component.onEvent(ProfileEvents.NextBadgePage) },
                                    enabled = state.badgePage < totalBadgePages - 1
                                ) {
                                    Text(stringResource(Res.string.pagination_next))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LanguagePickerDialog(
    currentLanguage: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val english = stringResource(Res.string.profile_language_english)
    val languages = listOf("en" to english)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.profile_language_label)) },
        text = {
            Column {
                languages.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = code == currentLanguage,
                            onClick = { onSelect(code) }
                        )
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

private fun languageDisplayName(code: String, english: String): String =
    if (code == "en") english else code

@Composable
private fun StatColumn(
    emoji: String,
    value: String,
    label: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BadgeItem(badge: Badge, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(60.dp)) {
            Box(
                Modifier
                    .size(60.dp)
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
                Icon(
                    Icons.Rounded.Lock, null,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
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
private fun RecoveryEmailItem(email: RecoveryEmail, onVerify: () -> Unit, onRemove: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            // Avatar circle with email icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (email.verified) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Email,
                    contentDescription = null,
                    tint = if (email.verified) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Address + status chip
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = email.email,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (email.verified) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (email.verified) stringResource(Res.string.recovery_email_verified)
                               else stringResource(Res.string.recovery_email_pending),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (email.verified) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Verify action (pending only)
            if (!email.verified) {
                TextButton(
                    onClick = onVerify,
                    contentPadding = PaddingValues(horizontal = Spacing.s, vertical = Spacing.xxs)
                ) {
                    Text(
                        text = stringResource(Res.string.recovery_email_verify_submit),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Delete
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(Res.string.recovery_email_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun VerifyRecoveryEmailDialog(
    tokenInput: String,
    isVerifying: Boolean,
    dialogError: String?,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.recovery_email_verify_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.recovery_email_verify_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(Res.string.recovery_email_verify_token_label)) },
                    singleLine = true,
                    enabled = !isVerifying,
                    isError = dialogError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (dialogError != null) {
                    Text(
                        text = dialogError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = tokenInput.trim().isNotBlank() && !isVerifying
            ) { Text(stringResource(Res.string.recovery_email_verify_submit)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isVerifying) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun AddRecoveryEmailDialog(
    emailInput: String,
    isSaving: Boolean,
    dialogError: String?,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.recovery_email_add_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(Res.string.recovery_email_input_label)) },
                    singleLine = true,
                    enabled = !isSaving,
                    isError = dialogError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (dialogError != null) {
                    Text(
                        text = dialogError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = emailInput.trim().isNotBlank() && !isSaving
            ) { Text(stringResource(Res.string.recovery_email_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
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
