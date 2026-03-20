package com.ureka.play4change.features.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.ureka.play4change.features.home.presentation.HomeEvents
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.cancel
import play4change.composeapp.generated.resources.home_completed_today
import play4change.composeapp.generated.resources.home_greeting
import play4change.composeapp.generated.resources.home_no_task
import play4change.composeapp.generated.resources.home_roadmap
import play4change.composeapp.generated.resources.home_start_challenge
import play4change.composeapp.generated.resources.home_this_week
import play4change.composeapp.generated.resources.home_todays_challenge
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

                    // Today's task card
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
