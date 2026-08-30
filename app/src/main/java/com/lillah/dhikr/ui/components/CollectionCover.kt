package com.lillah.dhikr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lillah.dhikr.domain.model.CollectionProgress
import com.lillah.dhikr.ui.theme.ArabicText
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing
import java.io.File

/**
 * The cover for a collection, in the two sizes the app uses.
 *
 * A user-chosen photo takes over the whole card and gets a scrim so the title stays readable on
 * any image; otherwise the built-in vector artwork is drawn. Progress reads as a filled bar and a
 * count, and a finished collection is marked with a check rather than being greyed out.
 */
@Composable
fun CollectionCoverCard(
    progress: CollectionProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    shape: Shape = Radii.cover,
    compact: Boolean = false,
) {
    val collection = progress.collection
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource, pressedScale = 0.98f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .softShadow(16.dp, shape, MaterialTheme.colorScheme.primary, alpha = 0.24f)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        val coverPath = collection.coverImagePath
        if (!coverPath.isNullOrBlank()) {
            AsyncImage(
                model = File(coverPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CoverArtworkCanvas(
                artwork = collection.artwork,
                seed = collection.id,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Bottom scrim: the text sits over artwork and photos alike.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.18f),
                        1f to Color.Black.copy(alpha = 0.66f),
                    )
                )
        )

        if (progress.isComplete) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.m)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Completed today",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(if (compact) Spacing.m else Spacing.xl),
        ) {
            if (!compact && !collection.arabicName.isNullOrBlank()) {
                Text(
                    text = collection.arabicName,
                    style = ArabicText.Caption,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = collection.name,
                style = if (compact) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.headlineSmall
                },
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(if (compact) 6.dp else Spacing.m))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(Radii.chip)
                    .background(Color.White.copy(alpha = 0.28f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.fraction)
                        .height(6.dp)
                        .clip(Radii.chip)
                        .background(Color.White)
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${progress.completedToday} of ${progress.itemCount} today",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
                if (!compact && progress.totalToday > 0) {
                    Text(
                        text = "${progress.totalToday} counted",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}
