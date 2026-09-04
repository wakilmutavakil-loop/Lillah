package com.lillah.dhikr.domain.sync

import androidx.compose.runtime.Immutable

enum class SyncOperationKind {
    /** One counting action: a tap, or an undo as a negative delta. */
    COUNT_DELTA,

    /**
     * A single operation carrying the history a device already held before it was ever attached
     * to an account — the counts from before this feature existed.
     */
    BASELINE,
}

enum class SyncState {
    /** Queued, or attempted and not yet confirmed. Retried indefinitely; never discarded. */
    PENDING,
    SYNCED,
}

enum class AuthMethod(val id: String, val label: String) {
    Google("google", "Google"),
    Facebook("facebook", "Facebook"),
}

@Immutable
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val method: AuthMethod?,
)

/** The cloud figures behind the Universal Dhikr board. */
@Immutable
data class RemoteFigures(
    val globalTotal: Long = 0,
    /** Null when the server could not give today's figure — never a fabricated zero. */
    val globalToday: Long? = null,
    val participantCount: Long = 0,
    val userTotal: Long = 0,
    val updatedAt: Long = 0,
)

@Immutable
data class SyncStatus(
    val backendConfigured: Boolean = false,
    val signedIn: Boolean = false,
    val syncing: Boolean = false,
    val pendingOperations: Int = 0,
    /** Sum of the deltas still queued, so the UI can say what is waiting rather than just that something is. */
    val pendingTotal: Long = 0,
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
) {
    val hasPending: Boolean get() = pendingOperations > 0
}

/**
 * Percentage of the worldwide total contributed by one user.
 *
 * Kept as a function rather than a stored field: it is derived from two numbers that each move on
 * their own, and a stored copy would go stale the moment either did.
 */
fun contributionPercent(userTotal: Long, globalTotal: Long): Double =
    if (globalTotal <= 0L || userTotal <= 0L) 0.0
    else (userTotal.toDouble() / globalTotal.toDouble()) * 100.0

/**
 * Formats a share that is usually a very small number. Fixed decimal places would print "0.00%"
 * for most real users, so the precision follows the magnitude down to a floor that still reads as
 * a real contribution rather than as zero.
 */
fun formatContributionPercent(percent: Double): String {
    if (percent <= 0.0) return "0%"
    val decimals = when {
        percent >= 10.0 -> 1
        percent >= 1.0 -> 2
        percent >= 0.01 -> 4
        percent >= 0.00001 -> 5
        else -> return "< 0.00001%"
    }
    // Trailing zeros are noise on a figure this small: a share of 0.0015% should read as
    // 0.0015%, not 0.00150%.
    val rendered = String.format(java.util.Locale.getDefault(), "%.${decimals}f", percent)
    val separator = java.text.DecimalFormatSymbols.getInstance().decimalSeparator
    val trimmed = if (rendered.contains(separator)) {
        rendered.trimEnd('0').trimEnd(separator)
    } else {
        rendered
    }
    return "$trimmed%"
}
