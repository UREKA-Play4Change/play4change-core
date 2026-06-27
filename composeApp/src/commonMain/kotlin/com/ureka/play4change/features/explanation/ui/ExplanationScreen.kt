package com.ureka.play4change.features.explanation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
    val chunks = remember(text) {
        text.split("\n\n")
            .filter { it.isNotBlank() }
            .flatMap { paragraph ->
                paragraph
                    .split(Regex("(?<=[.!?])\\s+(?=[A-Z\"'])"))
                    .filter { it.isNotBlank() }
            }
    }
    var currentIndex by remember { mutableStateOf(0) }
    val total = chunks.size

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            // Header row: label + counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your explanation",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${currentIndex + 1} / $total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Paragraph content — slides left/right based on direction
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(280)) { if (forward) it else -it } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(200)) { if (forward) -it else it } + fadeOut(tween(150)))
                },
                label = "paragraph"
            ) { index ->
                Text(
                    text = parseInlineMarkdown(chunks.getOrElse(index) { "" }),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .padding(vertical = Spacing.m)
                )
            }

            // Progress dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(total) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == currentIndex) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= currentIndex)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            // Prev / Next navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                OutlinedButton(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = currentIndex > 0
                ) {
                    Text("← Prev")
                }
                Button(
                    onClick = { if (currentIndex < total - 1) currentIndex++ },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = currentIndex < total - 1
                ) {
                    Text("Next →")
                }
            }
        }
    }
}

private fun parseInlineMarkdown(input: String): androidx.compose.ui.text.AnnotatedString =
    buildAnnotatedString {
        val pattern = Regex("""\*\*(.+?)\*\*|\*(.+?)\*""")
        var lastEnd = 0
        for (match in pattern.findAll(input)) {
            append(input.substring(lastEnd, match.range.first))
            val bold = match.groupValues[1]
            val italic = match.groupValues[2]
            when {
                bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                italic.isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
            }
            lastEnd = match.range.last + 1
        }
        append(input.substring(lastEnd))
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