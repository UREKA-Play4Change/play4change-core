package com.ureka.play4change.features.about.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.UrekaLogo
import com.ureka.play4change.features.about.presentation.AboutEffect
import com.ureka.play4change.features.about.presentation.AboutEvents
import com.ureka.play4change.features.about.presentation.DefaultAboutComponent
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.about_architecture_body
import play4change.composeapp.generated.resources.about_architecture_title
import play4change.composeapp.generated.resources.about_author_label
import play4change.composeapp.generated.resources.about_author_name
import play4change.composeapp.generated.resources.about_built_with
import play4change.composeapp.generated.resources.about_footer
import play4change.composeapp.generated.resources.about_institution
import play4change.composeapp.generated.resources.about_project_body
import play4change.composeapp.generated.resources.about_project_title
import play4change.composeapp.generated.resources.about_student_number
import play4change.composeapp.generated.resources.about_supervisors
import play4change.composeapp.generated.resources.about_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    component: DefaultAboutComponent,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(component) {
        component.effects.collect { effect ->
            when (effect as AboutEffect) {
                AboutEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    BaseView(component = component) { _, onEvent ->
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(Res.string.about_title)) },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(AboutEvents.NavigateBack) }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(Spacing.l)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.l)
            ) {
                UrekaLogo(modifier = Modifier.fillMaxWidth())
                HorizontalDivider()

                // Project section
                SectionCard(title = stringResource(Res.string.about_project_title)) {
                    Text(
                        text = stringResource(Res.string.about_project_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Architecture section
                SectionCard(title = stringResource(Res.string.about_architecture_title)) {
                    Text(
                        text = stringResource(Res.string.about_architecture_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Built with
                SectionCard(title = stringResource(Res.string.about_built_with)) {
                    val items = listOf(
                        "Kotlin Multiplatform" to "Cross-platform framework",
                        "Compose Multiplatform" to "Declarative UI",
                        "Decompose" to "Navigation & component lifecycle",
                        "Koin" to "Dependency injection",
                        "Ktor" to "HTTP client",
                        "Spring Boot" to "Backend API",
                        "PostgreSQL + pgvector" to "Database & semantic search",
                        "OpenAI API" to "AI content generation"
                    )
                    items.forEach { (tech, description) ->
                        Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
                            Text(
                                text = tech,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Author
                SectionCard(title = stringResource(Res.string.about_author_label)) {
                    Text(
                        text = stringResource(Res.string.about_author_name),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "No. ${stringResource(Res.string.about_student_number)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = stringResource(Res.string.about_institution),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = stringResource(Res.string.about_supervisors),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Footer
                Text(
                    text = stringResource(Res.string.about_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(Spacing.l)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(Spacing.s))
            content()
        }
    }
}
