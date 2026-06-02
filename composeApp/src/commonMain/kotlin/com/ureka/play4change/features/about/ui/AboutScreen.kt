package com.ureka.play4change.features.about.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.EmojiObjects
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.LogoSize
import com.ureka.play4change.design.components.UrekaLogo
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
import play4change.composeapp.generated.resources.app_tagline

private val techStack = listOf(
    "Kotlin Multiplatform",
    "Compose Multiplatform",
    "Decompose",
    "Ktor",
    "Spring Boot",
    "PostgreSQL + pgvector",
    "Mistral AI",
    "Flyway"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(component: DefaultAboutComponent) {
    BaseView(
        component = component,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = { component.onEvent(AboutEvents.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { _, _, innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.l)
        ) {
            // ── Hero ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.l)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .padding(vertical = Spacing.xxl, horizontal = Spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    UrekaLogo(size = LogoSize.Large, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(Spacing.m))
                    Text(
                        text = stringResource(Res.string.app_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Content cards ─────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                // The Project
                AboutCard(
                    title = stringResource(Res.string.about_project_title),
                    icon = Icons.Rounded.EmojiObjects
                ) {
                    Text(
                        text = stringResource(Res.string.about_project_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }

                // Architecture
                AboutCard(
                    title = stringResource(Res.string.about_architecture_title),
                    icon = Icons.Rounded.AccountTree
                ) {
                    Text(
                        text = stringResource(Res.string.about_architecture_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }

                // Built With
                AboutCard(
                    title = stringResource(Res.string.about_built_with),
                    icon = Icons.Rounded.Code
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
                    ) {
                        techStack.forEach { tech ->
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = tech,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }

                // Author
                AboutCard(
                    title = stringResource(Res.string.about_author_label),
                    icon = Icons.Rounded.Person
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.about_author_name),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "Nº ${stringResource(Res.string.about_student_number)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.s,
                                    vertical = Spacing.xxs
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(Res.string.about_institution),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.m))
                    Text(
                        text = stringResource(Res.string.about_supervisors),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }

                // Footer
                Text(
                    text = stringResource(Res.string.about_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xl)
                )
            }
        }
    }
}

@Composable
private fun AboutCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
