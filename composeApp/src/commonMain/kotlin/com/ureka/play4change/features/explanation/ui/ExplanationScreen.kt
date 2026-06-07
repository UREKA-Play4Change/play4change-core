package com.ureka.play4change.features.explanation.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.features.explanation.domain.model.ExplanationMessage
import com.ureka.play4change.features.explanation.presentation.DefaultExplanationComponent
import com.ureka.play4change.features.explanation.presentation.ExplanationEvents
import com.ureka.play4change.features.explanation.presentation.ExplanationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationScreen(component: DefaultExplanationComponent) {
    BaseView(
        component = component,
        onRetry = { component.onEvent(ExplanationEvents.RetryLoad) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Let's Understand Together",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
        }
    ) { state, onEvent, innerPadding ->
        when {
            state.isLoading || state.isGenerating -> GeneratingContent(innerPadding)
            else -> ActiveContent(state, onEvent, innerPadding)
        }
    }
}

@Composable
private fun GeneratingContent(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(Spacing.m))
        Text(
            text = "Preparing your personalised explanation…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xl)
        )
    }
}

@Composable
private fun ActiveContent(
    state: ExplanationState,
    onEvent: (ExplanationEvents) -> Unit,
    innerPadding: PaddingValues
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            item {
                ExplanationCard(text = state.explanationText ?: "")
            }

            if (state.messages.isNotEmpty()) {
                item {
                    Text(
                        text = "Conversation",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.s)
                    )
                }
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }

            if (state.isSending) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = Spacing.m),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }
            }

            item { Spacer(Modifier.height(Spacing.m)) }
        }

        BottomActions(state, onEvent)
    }
}

@Composable
private fun ExplanationCard(text: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(Spacing.l)) {
            Text(
                text = "Your explanation",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.6
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ExplanationMessage) {
    val isUser = message.role == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s)
            )
        }
    }
}

@Composable
private fun BottomActions(
    state: ExplanationState,
    onEvent: (ExplanationEvents) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        if (state.showInput) {
            OutlinedTextField(
                value = state.inputText,
                onValueChange = { onEvent(ExplanationEvents.InputChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What's still confusing you?") },
                shape = MaterialTheme.shapes.medium,
                maxLines = 4
            )
            Button(
                onClick = { onEvent(ExplanationEvents.SendMessage) },
                enabled = state.inputText.isNotBlank() && !state.isSending,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Ask")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            OutlinedButton(
                onClick = { onEvent(ExplanationEvents.ToggleInput) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !state.isResolving && !state.isSending
            ) {
                Text(if (state.showInput) "Cancel" else "I'm Confused")
            }

            Button(
                onClick = { onEvent(ExplanationEvents.Understood) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !state.isResolving && !state.isSending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (state.isResolving) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("I Understood")
                }
            }
        }
    }
}
