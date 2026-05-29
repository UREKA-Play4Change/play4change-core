package com.ureka.play4change.features.explore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.ShimmerBox
import com.ureka.play4change.features.explore.domain.model.Topic
import com.ureka.play4change.features.explore.domain.model.TopicIconType
import com.ureka.play4change.features.explore.presentation.DefaultExploreComponent
import com.ureka.play4change.features.explore.presentation.ExploreEvents
import com.ureka.play4change.features.explore.presentation.ExploreFilter
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.cancel
import play4change.composeapp.generated.resources.explore_enroll_confirm
import play4change.composeapp.generated.resources.explore_enroll_confirm_body
import play4change.composeapp.generated.resources.explore_enroll_confirm_title
import play4change.composeapp.generated.resources.explore_join
import play4change.composeapp.generated.resources.explore_leave
import play4change.composeapp.generated.resources.explore_leave_confirm
import play4change.composeapp.generated.resources.explore_leave_confirm_body
import play4change.composeapp.generated.resources.explore_leave_confirm_title
import play4change.composeapp.generated.resources.explore_subtitle
import play4change.composeapp.generated.resources.explore_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(component: DefaultExploreComponent) {
    BaseView(
        component = component,
        onRetry = { component.onEvent(ExploreEvents.LoadTopics) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.explore_title)) },
                navigationIcon = {
                    IconButton(onClick = { component.onEvent(ExploreEvents.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { state, onEvent, innerPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                repeat(4) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(112.dp))
                }
            }
        } else {
            val filteredTopics = state.topics.filter { topic ->
                when (state.filter) {
                    ExploreFilter.ALL       -> true
                    ExploreFilter.ACTIVE    -> topic.isActive
                    ExploreFilter.OPEN      -> topic.enrollmentStatus == null && !topic.isLocked
                    ExploreFilter.COMPLETED -> topic.isCompleted
                    ExploreFilter.ABANDONED -> topic.isAbandoned
                    ExploreFilter.LOCKED    -> topic.isLocked
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = Spacing.l)) {
                        Text(
                            stringResource(Res.string.explore_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Spacing.s))
                    }
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.l),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        ExploreFilter.entries.forEach { f ->
                            FilterChip(
                                selected = state.filter == f,
                                onClick = { onEvent(ExploreEvents.SetFilter(f)) },
                                label = {
                                    Text(filterLabel(f), style = MaterialTheme.typography.labelMedium)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.l))
                    Spacer(Modifier.height(Spacing.xs))
                }

                items(filteredTopics) { topic ->
                    TopicCard(
                        topic = topic,
                        modifier = Modifier.padding(horizontal = Spacing.l),
                        onEnroll = { onEvent(ExploreEvents.RequestEnroll(topic)) },
                        onLeave = { onEvent(ExploreEvents.RequestLeave(topic)) }
                    )
                }

                if (filteredTopics.isEmpty()) {
                    item {
                        Text(
                            text = emptyMessage(state.filter),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.l, vertical = Spacing.xl)
                        )
                    }
                }
            }
        }

        // Confirm enroll dialog
        state.pendingEnroll?.let {
            AlertDialog(
                onDismissRequest = { onEvent(ExploreEvents.DismissEnroll) },
                title = { Text(stringResource(Res.string.explore_enroll_confirm_title)) },
                text = { Text(stringResource(Res.string.explore_enroll_confirm_body)) },
                confirmButton = {
                    Button(onClick = { onEvent(ExploreEvents.ConfirmEnroll) }) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(Res.string.explore_enroll_confirm))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(ExploreEvents.DismissEnroll) }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }

        // Confirm leave dialog
        state.pendingLeave?.let {
            AlertDialog(
                onDismissRequest = { onEvent(ExploreEvents.DismissLeave) },
                title = { Text(stringResource(Res.string.explore_leave_confirm_title)) },
                text = { Text(stringResource(Res.string.explore_leave_confirm_body)) },
                confirmButton = {
                    Button(onClick = { onEvent(ExploreEvents.ConfirmLeave) }) {
                        Text(stringResource(Res.string.explore_leave_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(ExploreEvents.DismissLeave) }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun TopicCard(
    topic: Topic,
    modifier: Modifier = Modifier,
    onEnroll: () -> Unit,
    onLeave: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (topic.isActive) 3.dp else 1.dp
        )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            val accentColor = when {
                topic.isActive    -> MaterialTheme.colorScheme.secondary
                topic.isCompleted -> MaterialTheme.colorScheme.tertiary
                topic.isAbandoned -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                else              -> MaterialTheme.colorScheme.outlineVariant
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Column(modifier = Modifier.padding(Spacing.l)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = topicEmoji(topic.iconType),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    when {
                        topic.isActive -> Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        topic.isCompleted -> Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Completed",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        topic.isAbandoned -> Text(
                            text = "Abandoned",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                        topic.isLocked -> Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = topic.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.m))
                when {
                    topic.isActive -> Button(
                        onClick = onLeave,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(Res.string.explore_leave), style = MaterialTheme.typography.labelLarge)
                    }
                    topic.isCompleted -> Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Completed", style = MaterialTheme.typography.labelLarge)
                    }
                    topic.isAbandoned -> Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Abandoned", style = MaterialTheme.typography.labelLarge)
                    }
                    topic.isLocked -> Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Locked", style = MaterialTheme.typography.labelLarge)
                    }
                    else -> Button(
                        onClick = onEnroll,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(Res.string.explore_join), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

private fun filterLabel(filter: ExploreFilter): String = when (filter) {
    ExploreFilter.ALL       -> "All"
    ExploreFilter.ACTIVE    -> "Active"
    ExploreFilter.OPEN      -> "Open"
    ExploreFilter.COMPLETED -> "Completed"
    ExploreFilter.ABANDONED -> "Abandoned"
    ExploreFilter.LOCKED    -> "Locked"
}

private fun emptyMessage(filter: ExploreFilter): String = when (filter) {
    ExploreFilter.ALL       -> "No topics available."
    ExploreFilter.ACTIVE    -> "You have no active enrollments."
    ExploreFilter.OPEN      -> "No open topics to join right now."
    ExploreFilter.COMPLETED -> "You haven't completed any topics yet."
    ExploreFilter.ABANDONED -> "No abandoned topics."
    ExploreFilter.LOCKED    -> "No locked topics."
}

private fun topicEmoji(iconType: TopicIconType): String = when (iconType) {
    TopicIconType.SUSTAINABILITY -> "🌱"
    TopicIconType.DIGITAL        -> "💻"
    TopicIconType.HEALTH         -> "❤️"
    TopicIconType.ECONOMY        -> "♻️"
    TopicIconType.CULTURE        -> "🎭"
}
