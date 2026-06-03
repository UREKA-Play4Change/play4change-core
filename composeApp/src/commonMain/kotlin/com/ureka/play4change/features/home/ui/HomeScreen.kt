package com.ureka.play4change.features.home.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.core.currentHour
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.BadgeSize
import com.ureka.play4change.design.components.HeroCardShimmer
import com.ureka.play4change.design.components.LogoSize
import com.ureka.play4change.design.components.StreakBadge
import com.ureka.play4change.design.components.TaskCardShimmer
import com.ureka.play4change.design.components.UrekaLogo
import com.ureka.play4change.features.home.domain.model.PendingReviewSummary
import com.ureka.play4change.features.home.domain.model.TaskSummary
import com.ureka.play4change.features.home.presentation.DefaultHomeComponent
import com.ureka.play4change.features.home.presentation.HomeEvents
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.cancel
import play4change.composeapp.generated.resources.home_completed_today
import play4change.composeapp.generated.resources.home_daily_reviews
import play4change.composeapp.generated.resources.home_task_generating
import play4change.composeapp.generated.resources.home_enroll_prompt_body
import play4change.composeapp.generated.resources.home_enroll_prompt_cta
import play4change.composeapp.generated.resources.home_enroll_prompt_title
import play4change.composeapp.generated.resources.home_greeting_afternoon
import play4change.composeapp.generated.resources.home_greeting_evening
import play4change.composeapp.generated.resources.home_greeting_morning
import play4change.composeapp.generated.resources.home_greeting_night
import play4change.composeapp.generated.resources.home_no_task
import play4change.composeapp.generated.resources.home_pts_suffix
import play4change.composeapp.generated.resources.home_review_cta
import play4change.composeapp.generated.resources.home_start_challenge
import play4change.composeapp.generated.resources.home_struggle_cta
import play4change.composeapp.generated.resources.home_todays_challenge
import play4change.composeapp.generated.resources.home_todays_challenges
import play4change.composeapp.generated.resources.home_your_score
import play4change.composeapp.generated.resources.nav_about
import play4change.composeapp.generated.resources.nav_drawer_menu
import play4change.composeapp.generated.resources.nav_log_out
import play4change.composeapp.generated.resources.nav_log_out_confirm_body
import play4change.composeapp.generated.resources.nav_log_out_confirm_title
import play4change.composeapp.generated.resources.nav_profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(component: DefaultHomeComponent) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BaseView(
        component = component,
        onRetry = { component.onEvent(HomeEvents.RetryLoad) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { UrekaLogo(size = LogoSize.Small) },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Rounded.Menu,
                                contentDescription = stringResource(Res.string.nav_drawer_menu)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { component.onEvent(HomeEvents.OpenExplore) }) {
                        Icon(
                            Icons.Rounded.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        drawerState = drawerState,
        drawerContent = {
            Spacer(Modifier.height(Spacing.xl))
            UrekaLogo(size = LogoSize.Small, modifier = Modifier.padding(horizontal = Spacing.l))
            Spacer(Modifier.height(Spacing.l))
            HorizontalDivider()
            Spacer(Modifier.height(Spacing.s))
            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.nav_profile)) },
                selected = false,
                icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null) },
                onClick = {
                    coroutineScope.launch { drawerState.close() }
                    component.onEvent(HomeEvents.OpenProfile)
                },
                modifier = Modifier.padding(horizontal = Spacing.s)
            )
            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.nav_about)) },
                selected = false,
                icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                onClick = {
                    coroutineScope.launch { drawerState.close() }
                    component.onEvent(HomeEvents.OpenAbout)
                },
                modifier = Modifier.padding(horizontal = Spacing.s)
            )
            Spacer(Modifier.height(Spacing.s))
            HorizontalDivider()
            Spacer(Modifier.height(Spacing.s))
            NavigationDrawerItem(
                label = {
                    Text(
                        stringResource(Res.string.nav_log_out),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                selected = false,
                icon = {
                    Icon(
                        Icons.AutoMirrored.Rounded.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    coroutineScope.launch { drawerState.close() }
                    component.onEvent(HomeEvents.RequestLogOut)
                },
                modifier = Modifier.padding(horizontal = Spacing.s)
            )
        }
    ) { state, onEvent, innerPadding ->

        // Log-out confirmation dialog
        if (state.showLogOutDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(HomeEvents.DismissLogOut) },
                title = { Text(stringResource(Res.string.nav_log_out_confirm_title)) },
                text  = { Text(stringResource(Res.string.nav_log_out_confirm_body)) },
                confirmButton = {
                    TextButton(onClick = { onEvent(HomeEvents.ConfirmLogOut) }) {
                        Text(
                            stringResource(Res.string.nav_log_out),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(HomeEvents.DismissLogOut) }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }

        // Enroll prompt dialog
        if (state.showEnrollPrompt) {
            AlertDialog(
                onDismissRequest = { onEvent(HomeEvents.DismissEnrollPrompt) },
                title = { Text(stringResource(Res.string.home_enroll_prompt_title)) },
                text  = { Text(stringResource(Res.string.home_enroll_prompt_body)) },
                confirmButton = {
                    Button(onClick = {
                        onEvent(HomeEvents.DismissEnrollPrompt)
                        onEvent(HomeEvents.OpenExplore)
                    }) {
                        Text(stringResource(Res.string.home_enroll_prompt_cta))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(HomeEvents.DismissEnrollPrompt) }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
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
            HomeShimmer(modifier = Modifier.padding(innerPadding))
        } else {
            val data = state.homeData
            if (data == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("😕", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(Spacing.l))
                    Text(
                        text = stringResource(Res.string.home_no_task),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m)
                ) {

                    // ── HERO ─────────────────────────────────────────────────────────
                    item {
                        val greetingRes = remember {
                            val hour = currentHour()
                            when (hour) {
                                in 5..11  -> Res.string.home_greeting_morning
                                in 12..17 -> Res.string.home_greeting_afternoon
                                in 18..21 -> Res.string.home_greeting_evening
                                else      -> Res.string.home_greeting_night
                            }
                        }
                        val animatedPoints by animateIntAsState(
                            targetValue = data.totalPoints,
                            animationSpec = tween(durationMillis = 900),
                            label = "points"
                        )
                        Column(modifier = Modifier.padding(horizontal = Spacing.l)) {
                            Spacer(Modifier.height(Spacing.xl))

                            // Greeting row with optional streak pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(greetingRes, greetingName(data.userName)),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (data.streakDays > 0) {
                                    Spacer(Modifier.width(Spacing.s))
                                    StreakBadge(streakDays = data.streakDays, size = BadgeSize.Compact)
                                }
                            }

                            Spacer(Modifier.height(Spacing.l))

                            // Score card — violet → teal gradient
                            val scoreGradient = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(brush = Brush.linearGradient(colors = scoreGradient))
                                    .padding(horizontal = Spacing.xl, vertical = Spacing.xl)
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(Res.string.home_your_score),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                    Spacer(Modifier.height(Spacing.xs))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "$animatedPoints",
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(Modifier.width(Spacing.xs))
                                        Text(
                                            text = stringResource(Res.string.home_pts_suffix),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }
                                }
                                // Decorative star — semi-transparent watermark
                                Text(
                                    text = "⭐",
                                    style = MaterialTheme.typography.displayMedium,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = Spacing.s)
                                )
                            }
                        }
                    }

                    // ── TODAY'S CHALLENGES ────────────────────────────────────────────
                    item {
                        SectionHeader(
                            title = stringResource(Res.string.home_todays_challenges),
                            modifier = Modifier.padding(horizontal = Spacing.l)
                        )
                    }

                    items(data.todayTasks, key = { it.topicId }) { entry ->
                        when {
                            entry.struggleOpen -> StruggleTaskCard(
                                topicTitle = entry.topicTitle,
                                onContinue = { onEvent(HomeEvents.ContinueStruggle(entry.struggleEnrollmentId)) },
                                modifier = Modifier.padding(horizontal = Spacing.l)
                            )
                            entry.completed -> CompletedTaskCard(
                                topicTitle = entry.topicTitle,
                                modifier = Modifier.padding(horizontal = Spacing.l)
                            )
                            entry.isGenerating -> GeneratingTaskCard(
                                topicTitle = entry.topicTitle,
                                modifier = Modifier.padding(horizontal = Spacing.l)
                            )
                            entry.task != null -> PendingTaskCard(
                                task = entry.task,
                                topicTitle = entry.topicTitle,
                                onStart = { onEvent(HomeEvents.StartTask(entry.topicId)) },
                                modifier = Modifier.padding(horizontal = Spacing.l)
                            )
                        }
                    }

                    // ── PEER REVIEWS (conditional) ────────────────────────────────────
                    if (data.pendingReviews.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(Res.string.home_daily_reviews),
                                modifier = Modifier.padding(horizontal = Spacing.l)
                            )
                        }
                        items(data.pendingReviews, key = { it.reviewId }) { review ->
                            ReviewCard(
                                review = review,
                                modifier = Modifier.padding(horizontal = Spacing.l)
                            )
                        }
                    }

                    item { Spacer(Modifier.height(Spacing.xxl)) }
                }
            }
        }
    }
}

// ── Private card composables ──────────────────────────────────────────────────

@Composable
private fun PendingTaskCard(
    task: TaskSummary,
    topicTitle: String,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        onClick = onStart
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left accent bar in brand teal
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.secondary)
            )
            Column(modifier = Modifier.padding(Spacing.l)) {
                // Topic + points reward
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (topicTitle.isNotEmpty()) topicTitle
                               else stringResource(Res.string.home_todays_challenge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "+${task.pointsReward} pts",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.m))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.home_start_challenge),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedTaskCard(
    topicTitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.secondary)
            )
            Row(
                modifier = Modifier
                    .padding(Spacing.l)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(Modifier.weight(1f)) {
                    if (topicTitle.isNotEmpty()) {
                        Text(
                            text = topicTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(Res.string.home_completed_today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratingTaskCard(
    topicTitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline)
            )
            Row(
                modifier = Modifier
                    .padding(Spacing.l)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(Modifier.weight(1f)) {
                    if (topicTitle.isNotEmpty()) {
                        Text(
                            text = topicTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(Res.string.home_task_generating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StruggleTaskCard(
    topicTitle: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        onClick = onContinue
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error)
            )
            Column(modifier = Modifier.padding(Spacing.l)) {
                if (topicTitle.isNotEmpty()) {
                    Text(
                        text = topicTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(Spacing.s))
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.home_struggle_cta),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: PendingReviewSummary,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left accent bar in brand amber
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.tertiary)
            )
            Row(
                modifier = Modifier
                    .padding(Spacing.l)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                Icon(
                    Icons.Rounded.RateReview,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Column(Modifier.weight(1f)) {
                    if (review.topicTitle.isNotEmpty()) {
                        Text(
                            text = review.topicTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(Res.string.home_review_cta),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Support composables ───────────────────────────────────────────────────────

/**
 * Section header with uppercase label + full-width trailing divider line.
 */
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.s, bottom = Spacing.xs),
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
    }
}

/**
 * Derives the first name from an email address or a display name for use in the greeting.
 * Non-letter characters are treated as word separators; only the first word is returned,
 * capitalised.
 * Examples: "Radesh Govind" → "Radesh", "radesh.govind@gmail.com" → "Radesh".
 */
internal fun greetingName(userName: String): String {
    val raw = if (userName.contains('@')) userName.substringBefore('@') else userName
    val words = raw
        .map { if (it.isLetter()) it else ' ' }
        .joinToString("")
        .trim()
        .split("\\s+".toRegex())
        .filter { it.isNotEmpty() }
    return words.firstOrNull()?.replaceFirstChar { it.uppercaseChar() } ?: "there"
}

@Composable
private fun HomeShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m)
    ) {
        Spacer(Modifier.height(Spacing.xl))
        HeroCardShimmer(Modifier.padding(vertical = Spacing.s))
        Spacer(Modifier.height(Spacing.xs))
        ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            TaskCardShimmer()
        }
        ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            TaskCardShimmer()
        }
    }
}
