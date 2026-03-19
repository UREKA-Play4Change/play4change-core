package com.ureka.play4change.features.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.BadgeSize
import com.ureka.play4change.design.components.DayDots
import com.ureka.play4change.design.components.LogoSize
import com.ureka.play4change.design.components.PointsDisplay
import com.ureka.play4change.design.components.RoadmapView
import com.ureka.play4change.design.components.StreakBadge
import com.ureka.play4change.design.components.TaskCardShimmer
import com.ureka.play4change.design.components.HeroCardShimmer
import com.ureka.play4change.design.components.RoadmapNodeShimmer
import com.ureka.play4change.design.components.UrekaLogo
import com.ureka.play4change.design.components.XpBar
import com.ureka.play4change.features.home.presentation.DefaultHomeComponent
import com.ureka.play4change.features.home.presentation.HomeEffect
import com.ureka.play4change.features.home.presentation.HomeEvents
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.home_completed_today
import play4change.composeapp.generated.resources.home_greeting
import play4change.composeapp.generated.resources.home_no_task
import play4change.composeapp.generated.resources.home_roadmap
import play4change.composeapp.generated.resources.home_start_challenge
import play4change.composeapp.generated.resources.home_this_week
import play4change.composeapp.generated.resources.home_todays_challenge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    component: DefaultHomeComponent,
    onNavigateToTask: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToExplore: () -> Unit = {}
) {
    LaunchedEffect(component) {
        component.effects.collect { effect ->
            when (val homeEffect = effect as HomeEffect) {
                is HomeEffect.NavigateToTask -> onNavigateToTask(homeEffect.userTaskId)
                HomeEffect.NavigateToProfile -> onNavigateToProfile()
                HomeEffect.NavigateToAbout   -> onNavigateToAbout()
            }
        }
    }

    BaseView(component = component, screenAlignment = Alignment.TopStart) { state, onEvent ->
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { UrekaLogo(size = LogoSize.Small) },
                    navigationIcon = {
                        state.homeData?.let {
                            StreakBadge(
                                streakDays = it.streakDays,
                                size = BadgeSize.Compact,
                                modifier = Modifier.padding(start = Spacing.s)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToExplore) {
                            Icon(Icons.Rounded.Explore, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground)
                        }
                        val initial = (state.homeData?.userName?.firstOrNull() ?: 'U')
                            .toString().uppercase()
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onEvent(HomeEvents.OpenProfile) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.width(Spacing.s))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        ) { innerPadding ->
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
                            .padding(innerPadding)
                            .padding(horizontal = Spacing.l),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.l)
                    ) {
                        item {
                            // Hero card
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(Modifier.padding(Spacing.l)) {
                                    Text(
                                        text = stringResource(
                                            Res.string.home_greeting,
                                            data.userName.split(" ").first()
                                        ),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.height(Spacing.s))
                                    PointsDisplay(points = data.totalPoints)
                                    Spacer(Modifier.height(Spacing.xs))
                                    Text(
                                        text = "Level ${data.level}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.height(Spacing.xs))
                                    XpBar(
                                        progress = data.xpProgress,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(Spacing.m))
                                    Text(
                                        text = stringResource(Res.string.home_this_week),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.height(Spacing.xs))
                                    DayDots(weekProgress = data.weekProgress)
                                }
                            }
                        }

                        // Today's task card — immediately after hero
                        if (!data.todayCompleted) {
                            data.todayTask?.let { task ->
                                item {
                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                                        onClick = { onEvent(HomeEvents.StartTask(task.id)) }
                                    ) {
                                        Column(Modifier.padding(Spacing.l)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = {
                                                        Text(
                                                            stringResource(Res.string.home_todays_challenge),
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = MaterialTheme.colorScheme.secondary,
                                                        labelColor = MaterialTheme.colorScheme.onSecondary
                                                    )
                                                )
                                                Spacer(Modifier.weight(1f))
                                                Text(
                                                    "+${task.pointsReward} pts",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(Modifier.height(Spacing.xs))
                                            Text(
                                                task.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(Spacing.m))
                                            Button(
                                                onClick = { onEvent(HomeEvents.StartTask(task.id)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary
                                                )
                                            ) {
                                                Text(
                                                    stringResource(Res.string.home_start_challenge),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Row(
                                        Modifier.padding(Spacing.l),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.CheckCircle, null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(Spacing.xs))
                                        Text(
                                            stringResource(Res.string.home_completed_today),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            SectionHeader(stringResource(Res.string.home_roadmap))
                        }

                        item {
                            RoadmapView(
                                nodes = data.roadmapNodes,
                                onNodeClick = { node ->
                                    onEvent(HomeEvents.StartTask(node.dayIndex.toString()))
                                }
                            )
                        }

                        item { Spacer(Modifier.height(Spacing.xxl)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.m, bottom = Spacing.xs)
    )
}

@Composable
private fun HomeShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.m)
    ) {
        ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            HeroCardShimmer(Modifier.padding(Spacing.l))
        }
        ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            TaskCardShimmer()
        }
        repeat(3) {
            RoadmapNodeShimmer(Modifier.padding(vertical = Spacing.xs))
        }
    }
}
