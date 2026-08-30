package com.lillah.dhikr.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.data.guide.GuideArticle
import com.lillah.dhikr.data.guide.GuideBlock
import com.lillah.dhikr.ui.components.SoftCard
import com.lillah.dhikr.ui.theme.ArabicText
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing

@Composable
fun GuideArticleScreen(
    article: GuideArticle?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val gradients = LocalAppGradients.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        Spacer(Modifier.height(Spacing.s))
        Row(
            modifier = Modifier.padding(horizontal = Spacing.screen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (article == null) {
            Spacer(Modifier.height(Spacing.xxl))
            Text(
                text = "That article could not be found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.screen),
            )
            return@Column
        }

        Spacer(Modifier.height(Spacing.l))

        Column(Modifier.padding(horizontal = Spacing.screen)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(gradients.accent(article.accentIndex))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    article.icon.vector(),
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(27.dp),
                )
            }
            Spacer(Modifier.height(Spacing.l))
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = article.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = "${article.minutes} min read",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        article.blocks.forEach { block ->
            GuideBlockView(
                block = block,
                accentIndex = article.accentIndex,
                modifier = Modifier.padding(horizontal = Spacing.screen),
            )
        }

        Spacer(Modifier.height(Spacing.navClearance))
    }
}

@Composable
private fun GuideBlockView(
    block: GuideBlock,
    accentIndex: Int,
    modifier: Modifier = Modifier,
) {
    val gradients = LocalAppGradients.current

    when (block) {
        is GuideBlock.Heading -> {
            Spacer(Modifier.height(Spacing.l))
            Text(
                text = block.text,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.s))
        }

        is GuideBlock.Paragraph -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                modifier = modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.m))
        }

        is GuideBlock.Bullets -> {
            Column(modifier.fillMaxWidth()) {
                block.items.forEach { item ->
                    Row(
                        modifier = Modifier.padding(bottom = Spacing.s),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                    ) {
                        Box(
                            Modifier
                                .padding(top = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(gradients.accent(accentIndex))
                                )
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.m))
        }

        is GuideBlock.Quote -> {
            Row(modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(if (block.attribution != null) 96.dp else 72.dp)
                        .clip(Radii.chip)
                        .background(Brush.verticalGradient(gradients.accent(accentIndex)))
                )
                Spacer(Modifier.width(Spacing.l))
                Column {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontStyle = FontStyle.Italic,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (block.attribution != null) {
                        Spacer(Modifier.height(Spacing.s))
                        Text(
                            text = block.attribution,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.l))
        }

        is GuideBlock.Tip -> {
            SoftCard(
                modifier = modifier.fillMaxWidth(),
                contentPadding = Spacing.l,
                elevation = 4.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    Icon(
                        Icons.Rounded.Lightbulb,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.m))
        }

        is GuideBlock.ArabicLine -> {
            SoftCard(
                modifier = modifier.fillMaxWidth(),
                contentPadding = Spacing.l,
                elevation = 4.dp,
            ) {
                Text(
                    text = block.arabic,
                    style = ArabicText.Title,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                if (block.transliteration != null) {
                    Text(
                        text = block.transliteration,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                if (block.meaning != null) {
                    Text(
                        text = block.meaning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.s))
        }
    }
}
