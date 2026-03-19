package com.ureka.play4change.features.explore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.ShimmerBox
import com.ureka.play4change.features.explore.domain.model.Topic
import com.ureka.play4change.features.explore.domain.model.TopicIconType
import com.ureka.play4change.features.explore.presentation.DefaultExploreComponent
import com.ureka.play4change.features.explore.presentation.ExploreEffect
import com.ureka.play4change.features.explore.presentation.ExploreEvents
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.cancel
import play4change.composeapp.generated.resources.explore_active
import play4change.composeapp.generated.resources.explore_join
import play4change.composeapp.generated.resources.explore_subtitle
import play4change.composeapp.generated.resources.explore_switch_confirm
import play4change.composeapp.generated.resources.explore_switch_confirm_body
import play4change.composeapp.generated.resources.explore_switch_confirm_title
import play4change.composeapp.generated.resources.explore_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    component: DefaultExploreComponent,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(component) {
        component.effects.collect { effect ->
            when (effect as ExploreEffect) {
                ExploreEffect.NavigateBack  -> onNavigateBack()
                ExploreEffect.TopicSwitched -> { /* optional: show snackbar */ }
            }
        }
    }

    BaseView(component = component, screenAlignment = Alignment.TopStart) { state, onEvent ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.explore_title)) },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(ExploreEvents.NavigateBack) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        ) { padding ->
            if (state.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m)
                ) {
                    repeat(4) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(96.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    item {
                        Text(
                            stringResource(Res.string.explore_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Spacing.m))
                    }
                    items(state.topics) { topic ->
                        TopicCard(
                            topic = topic,
                            onSwitch = { onEvent(ExploreEvents.RequestSwitch(topic)) }
                        )
                    }
                }
            }

            // Confirm switch dialog
            state.pendingSwitch?.let { _ ->
                AlertDialog(
                    onDismissRequest = { onEvent(ExploreEvents.DismissSwitch) },
                    title = { Text(stringResource(Res.string.explore_switch_confirm_title)) },
                    text = { Text(stringResource(Res.string.explore_switch_confirm_body)) },
                    confirmButton = {
                        Button(onClick = { onEvent(ExploreEvents.ConfirmSwitch) }) {
                            if (state.isLoading) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(Res.string.explore_switch_confirm))
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onEvent(ExploreEvents.DismissSwitch) }) {
                            Text(stringResource(Res.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TopicCard(topic: Topic, onSwitch: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            Modifier.padding(Spacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(topicEmoji(topic.iconType), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(topic.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    topic.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(Spacing.xs))
            if (topic.isActive) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(stringResource(Res.string.explore_active)) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            } else {
                FilledTonalButton(onClick = onSwitch) {
                    Text(stringResource(Res.string.explore_join))
                }
            }
        }
    }
}

private fun topicEmoji(iconType: TopicIconType): String = when (iconType) {
    TopicIconType.SUSTAINABILITY -> "🌱"
    TopicIconType.DIGITAL        -> "💻"
    TopicIconType.HEALTH         -> "❤️"
    TopicIconType.ECONOMY        -> "♻️"
    TopicIconType.CULTURE        -> "🎭"
}
