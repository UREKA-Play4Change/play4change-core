package com.ureka.play4change.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.ureka.play4change.design.Spacing
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.app_university

@Composable
fun UrekaLogo(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // U!REKA wordmark: violet + teal accent
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "U",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            )
            Text(
                text = "!",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            )
            Text(
                text = "REKA",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(
            text = stringResource(Res.string.app_university),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )
    }
}
