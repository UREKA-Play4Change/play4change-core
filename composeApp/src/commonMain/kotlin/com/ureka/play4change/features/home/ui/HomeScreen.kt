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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.BadgeSize
import com.ureka.play4change.design.components.DayDots
import com.ureka.play4change.design.components.LogoSize
import com.ureka.play4change.design.components.PointsDisplay
import com.ureka.play4change.design.components.RoadmapView
import com.ureka.play4change.design.components.ShimmerBox
import com.ureka.play4change.design.components.StreakBadge
import com.ureka.play4change.design.components.UrekaLogo
import com.ureka.play4change.design.components.XpBar
import com.ureka.play4change.features.home.presentation.DefaultHomeComponent
import com.ureka.play4change.features.home.presentation.HomeEffect
import com.ureka.play4change.features.home.presentation.HomeEvents
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.home_greeting
import play4change.composeapp.generated.resources.home_no_task
import play4change.composeapp.generated.resources.home_roadmap
import play4change.composeapp.generated.resources.home_this_week
import play4change.composeapp.generated.resources.home_todays_challenge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    component: DefaultHomeComponent,
    onNavigateToTask: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAbout: () -> Unit
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

                        // Today's task sticky card
                        if (!data.todayCompleted) {
                            data.todayTask?.let { task ->
                                item {
                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.large,
                                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                                    ) {
                                        Row(
                                            Modifier.padding(Spacing.l),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = {
                                                        Text(
                                                            task.domain,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                )
                                                Spacer(Modifier.height(Spacing.xxs))
                                                Text(
                                                    task.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(Spacing.xxs))
                                                Text(
                                                    stringResource(Res.string.home_todays_challenge),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.width(Spacing.m))
                                            FilledTonalButton(
                                                onClick = { onEvent(HomeEvents.StartTask(task.id)) }
                                            ) {
                                                Text("+${task.pointsReward}")
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(Spacing.xxl))
                                }
                            }
                        } else {
                            item { Spacer(Modifier.height(Spacing.xxl)) }
                        }
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
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(120.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(40.dp))
        repeat(4) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                ShimmerBox(modifier = Modifier.size(52.dp))
                ShimmerBox(modifier = Modifier.weight(1f).height(52.dp))
            }
        }
    }
}
