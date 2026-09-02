package com.lillah.dhikr.ui.screens.universal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lillah.dhikr.core.time.grouped
import com.lillah.dhikr.domain.sync.AuthMethod
import com.lillah.dhikr.domain.sync.formatContributionPercent
import com.lillah.dhikr.ui.components.SectionHeader
import com.lillah.dhikr.ui.components.SoftCard
import com.lillah.dhikr.ui.components.softShadow
import com.lillah.dhikr.ui.theme.LocalAppGradients
import com.lillah.dhikr.ui.theme.Motion
import com.lillah.dhikr.ui.theme.Radii
import com.lillah.dhikr.ui.theme.Spacing
import java.text.DateFormat
import java.util.Date

/**
 * The community dashboard: what everyone has contributed, and what this person has contributed to
 * it. Built from the same cards, gradients and spacing as the rest of the app, so it reads as
 * another room in the same house rather than a bolted-on feature.
 */
@Composable
fun UniversalScreen(
    state: UniversalUiState,
    onSignIn: (AuthMethod) -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        item {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                Spacer(Modifier.height(Spacing.s))
                SectionHeader(
                    title = "Universal Dhikr",
                    subtitle = "What everyone is remembering, together",
                )
                Spacer(Modifier.height(Spacing.l))
                GlobalCard(state)
                Spacer(Modifier.height(Spacing.m))
                ContributionCard(state)
                Spacer(Modifier.height(Spacing.m))
            }
        }

        item {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                SectionHeader(title = "Your dhikr", subtitle = "Counted on this device")
                Spacer(Modifier.height(Spacing.m))
                PersonalTotalsCard(state)
                Spacer(Modifier.height(Spacing.m))
                SyncCard(state, onSyncNow = onSyncNow)
            }
        }

        item {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                Spacer(Modifier.height(Spacing.m))
                AccountCard(
                    state = state,
                    onSignIn = onSignIn,
                    onSignOut = onSignOut,
                )
                AnimatedVisibility(visible = state.message != null) {
                    Column {
                        Spacer(Modifier.height(Spacing.m))
                        MessageCard(state.message.orEmpty(), onDismissMessage)
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun GlobalCard(state: UniversalUiState) {
    val gradients = LocalAppGradients.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(20.dp, Radii.cardLarge, gradients.hero.first(), alpha = 0.42f)
            .clip(Radii.cardLarge)
            .background(Brush.linearGradient(gradients.hero))
            .padding(Spacing.xxl),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Icon(
                    Icons.Rounded.Public,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "UNIVERSAL DHIKR",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }

            Spacer(Modifier.height(Spacing.l))

            if (state.hasGlobalFigures) {
                CountingNumber(
                    value = state.globalTotal,
                    color = Color.White,
                    fontSize = 40.sp,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Total dhikr worldwide",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                )
            } else {
                Text(
                    text = if (state.backendConfigured) "—" else "Not connected",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = if (state.backendConfigured) {
                        "Sign in to join the worldwide count."
                    } else {
                        "This build has no cloud attached. Everything below is counted and kept " +
                            "on your device."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }

            if (state.hasGlobalFigures) {
                Spacer(Modifier.height(Spacing.xl))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    GlobalFigure(
                        icon = Icons.Rounded.Group,
                        value = state.figures?.participantCount?.grouped() ?: "0",
                        label = "taking part",
                    )
                    GlobalFigure(
                        icon = Icons.Rounded.Public,
                        value = state.figures?.globalToday?.grouped() ?: "0",
                        label = "worldwide today",
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalFigure(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun ContributionCard(state: UniversalUiState) {
    val gradients = LocalAppGradients.current

    SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.xxl) {
        Text(
            text = "YOUR CONTRIBUTION",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.m))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CountingNumber(
                value = state.userContribution,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 38.sp,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Your total dhikr",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        if (state.hasGlobalFigures) {
            Spacer(Modifier.height(Spacing.xl))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radii.tile)
                    .background(Brush.horizontalGradient(gradients.accent(1)))
                    .padding(vertical = Spacing.l),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatContributionPercent(state.contributionPercent),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Text(
                        text = "of the Universal Dhikr",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.m))
            Text(
                text = "Every share of this number is small. That is rather the point — it is a " +
                    "very large number, and you are part of it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PersonalTotalsCard(state: UniversalUiState) {
    val totals = state.totals
    SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.xl) {
        TotalRow("Today", totals.today.grouped())
        Divider()
        TotalRow("This week", totals.week.grouped())
        Divider()
        TotalRow("This month", totals.month.grouped())
        Divider()
        TotalRow("This year", totals.year.grouped())
        Divider()
        TotalRow("All time on this device", totals.allTimeLocal.grouped(), emphasised = true)
    }
}

@Composable
private fun TotalRow(label: String, value: String, emphasised: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (emphasised) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = if (emphasised) FontWeight.Bold else FontWeight.SemiBold,
            color = if (emphasised) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun SyncCard(state: UniversalUiState, onSyncNow: () -> Unit) {
    val status = state.syncStatus
    val icon = when {
        !status.backendConfigured -> Icons.Rounded.CloudOff
        status.hasPending -> Icons.Rounded.CloudQueue
        status.signedIn -> Icons.Rounded.CloudDone
        else -> Icons.Rounded.CloudOff
    }
    val headline = when {
        !status.backendConfigured -> "Saved on this device"
        !status.signedIn -> "Not syncing"
        status.syncing -> "Syncing…"
        status.hasPending -> "Sync pending"
        else -> "Everything synced"
    }
    val detail = when {
        !status.backendConfigured ->
            "No cloud is attached to this build. Nothing leaves your phone, and nothing is lost."
        !status.signedIn ->
            "Your dhikr are safe on this device. Sign in to add them to the Universal Dhikr."
        status.hasPending ->
            "${status.pendingTotal.grouped()} dhikr waiting to upload. They are already counted " +
                "here and will sync when there is a connection."
        status.lastSyncAt != null ->
            "Last synced ${formatTimestamp(status.lastSyncAt)}."
        else -> "Ready to sync."
    }

    SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.l) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.size(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (status.signedIn && !status.syncing) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSyncNow,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Sync now",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    state: UniversalUiState,
    onSignIn: (AuthMethod) -> Unit,
    onSignOut: () -> Unit,
) {
    val gradients = LocalAppGradients.current

    SoftCard(Modifier.fillMaxWidth(), contentPadding = Spacing.xl) {
        if (state.account.isSignedIn) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(gradients.accent(4))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.account.displayName?.take(1)?.uppercase() ?: "•",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.size(Spacing.m))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.account.displayName ?: "Signed in",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = state.account.email
                            ?: state.account.method?.label?.let { "via $it" }
                            ?: "Account connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onSignOut) { Text("Sign out") }
            }
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = "Signing out keeps every dhikr on this device. Nothing is deleted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "Join the Universal Dhikr",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = if (state.availableMethods.isEmpty()) {
                    "Accounts are not set up in this build. The counter, your history and your " +
                        "statistics all work exactly as they do now."
                } else {
                    "Sign in to add what you have already counted to the worldwide total, and to " +
                        "keep it safe if you change phone. Your existing " +
                        "${state.totals.allTimeLocal.grouped()} dhikr come with you."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.availableMethods.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.l))
                state.availableMethods.forEach { method ->
                    SignInButton(
                        method = method,
                        enabled = !state.signingIn,
                        onClick = { onSignIn(method) },
                    )
                    Spacer(Modifier.height(Spacing.s))
                }
            }
        }
    }
}

@Composable
private fun SignInButton(method: AuthMethod, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.chip)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, Radii.chip)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.l, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.Login,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(Spacing.s))
        Text(
            text = "Continue with ${method.label}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MessageCard(message: String, onDismiss: () -> Unit) {
    SoftCard(
        Modifier.fillMaxWidth(),
        contentPadding = Spacing.l,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}

/** Large figures ease in rather than snapping, which makes a growing worldwide total feel alive. */
@Composable
private fun CountingNumber(
    value: Long,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = Motion.emphasized(Motion.Celebration),
        label = "figure",
    )
    Text(
        text = animated.toLong().grouped(),
        style = MaterialTheme.typography.displayMedium.copy(fontSize = fontSize),
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = TextAlign.Center,
    )
}

private fun formatTimestamp(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))
