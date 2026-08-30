package com.lillah.dhikr.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lillah.dhikr.data.guide.GuideArticle
import com.lillah.dhikr.data.guide.GuideContent
import com.lillah.dhikr.data.guide.GuideIcon
import com.lillah.dhikr.ui.components.GradientCard
import com.lillah.dhikr.ui.components.SectionHeader
import com.lillah.dhikr.ui.components.SoftCard
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Spacing

fun GuideIcon.vector(): ImageVector = when (this) {
    GuideIcon.Heart -> Icons.Rounded.FavoriteBorder
    GuideIcon.Ring -> Icons.Rounded.DonutLarge
    GuideIcon.Sunrise -> Icons.Rounded.WbTwilight
    GuideIcon.Calendar -> Icons.Rounded.CalendarMonth
    GuideIcon.Feather -> Icons.Rounded.Create
    GuideIcon.Chart -> Icons.Rounded.Insights
    GuideIcon.Beads -> Icons.Rounded.AutoAwesome
}

@Composable
fun GuideScreen(
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    articles: List<GuideArticle> = GuideContent.articles,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        item {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                Spacer(Modifier.height(Spacing.s))
                SectionHeader(
                    title = "Guidebook",
                    subtitle = "Short reads on dhikr and on this app",
                )
                Spacer(Modifier.height(Spacing.l))

                val first = articles.firstOrNull()
                if (first != null) {
                    GradientCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenArticle(first.id) },
                    ) {
                        Text(
                            text = "Start here",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Spacer(Modifier.height(Spacing.s))
                        Text(
                            text = first.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = first.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        Spacer(Modifier.height(Spacing.l))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${first.minutes} min read",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Rounded.ChevronRight,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }

        items(articles.drop(1), key = { it.id }) { article ->
            Box(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs)) {
                ArticleRow(article = article, onClick = { onOpenArticle(article.id) })
            }
        }

        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun ArticleRow(article: GuideArticle, onClick: () -> Unit) {
    val gradients = LocalAppGradients.current
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = Spacing.l,
        elevation = 7.dp,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradients.accent(article.accentIndex))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    article.icon.vector(),
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = article.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${article.minutes} min read",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
