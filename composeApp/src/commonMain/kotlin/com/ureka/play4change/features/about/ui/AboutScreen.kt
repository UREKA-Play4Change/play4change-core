package com.ureka.play4change.features.about.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.EmojiObjects
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalUriHandler
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
import play4change.composeapp.generated.resources.about_author_label
import play4change.composeapp.generated.resources.about_author_name
import play4change.composeapp.generated.resources.about_footer
import play4change.composeapp.generated.resources.about_institution
import play4change.composeapp.generated.resources.about_project_body
import play4change.composeapp.generated.resources.about_project_title
import play4change.composeapp.generated.resources.about_questionnaire_body
import play4change.composeapp.generated.resources.about_questionnaire_cta
import play4change.composeapp.generated.resources.about_questionnaire_title
import play4change.composeapp.generated.resources.about_student_number
import play4change.composeapp.generated.resources.about_supervisors
import play4change.composeapp.generated.resources.about_title
import play4change.composeapp.generated.resources.app_tagline

@OptIn(ExperimentalMaterial3Api::class)
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
        val uriHandler = LocalUriHandler.current
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(460.dp)
                    .offset(y = (-160).dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .align(Alignment.TopCenter)
            )
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .offset(x = (-100).dp, y = 100.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        CircleShape
                    )
                    .align(Alignment.BottomStart)
            )
        }
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

                // Questionnaire
                val questionnaireUrl = "https://docs.google.com/forms/d/e/1FAIpQLSc5NJKJRL8_7TKLTz1dgkpgB2xewTEbgh7dJb1fgRhP9bp8DA/viewform?usp=sharing&ouid=107778068121226062653"
                AboutCard(
                    title = stringResource(Res.string.about_questionnaire_title),
                    icon = Icons.Rounded.Assignment
                ) {
                    Text(
                        text = stringResource(Res.string.about_questionnaire_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(Spacing.s))
                    Surface(
                        onClick = { uriHandler.openUri(questionnaireUrl) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                vertical = Spacing.m,
                                horizontal = Spacing.l
                            ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.about_questionnaire_cta),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
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
