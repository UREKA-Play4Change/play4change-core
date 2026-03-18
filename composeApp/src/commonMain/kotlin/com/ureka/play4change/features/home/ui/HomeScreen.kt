package com.ureka.play4change.features.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.DayDots
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
import play4change.composeapp.generated.resources.home_no_task
import play4change.composeapp.generated.resources.home_roadmap
import play4change.composeapp.generated.resources.home_start_challenge
import play4change.composeapp.generated.resources.home_this_week
import play4change.composeapp.generated.resources.home_todays_challenge
import play4change.composeapp.generated.resources.profile_title

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
                is HomeEffect.NavigateToTask    -> onNavigateToTask(homeEffect.userTaskId)
                HomeEffect.NavigateToProfile    -> onNavigateToProfile()
                HomeEffect.NavigateToAbout      -> onNavigateToAbout()
            }
        }
    }

    BaseView(component = component, screenAlignment = Alignment.TopStart) { state, onEvent ->
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { UrekaLogo() },
                    navigationIcon = {
                        state.homeData?.let {
                            StreakBadge(streakDays = it.streakDays, modifier = Modifier.padding(start = Spacing.s))
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEvent(HomeEvents.OpenProfile) }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(Res.string.profile_title),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
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
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(Spacing.l)) {
                                    PointsDisplay(points = data.totalPoints)
                                    Spacer(Modifier.height(Spacing.s))
                                    Text(
                                        text = "Level ${data.level}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.height(Spacing.xs))
                                    XpBar(progress = data.xpProgress)
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
                            Text(
                                text = stringResource(Res.string.home_roadmap),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            RoadmapView(
                                nodes = data.roadmapNodes,
                                onNodeClick = { node ->
                                    onEvent(HomeEvents.StartTask(node.dayIndex.toString()))
                                }
                            )
                        }

                        // Today's task card
                        data.todayTask?.let { task ->
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(Spacing.l)) {
                                        Text(
                                            text = stringResource(Res.string.home_todays_challenge),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(Modifier.height(Spacing.s))
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(Modifier.height(Spacing.m))
                                        Button(
                                            onClick = { onEvent(HomeEvents.StartTask(task.id)) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(Res.string.home_start_challenge))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(Spacing.xxl))
                            }
                        }
                    }
                }
            }
        }
    }
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
