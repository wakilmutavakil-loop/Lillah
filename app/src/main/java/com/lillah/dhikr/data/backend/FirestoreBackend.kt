package com.lillah.dhikr.data.backend

import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.persistentCacheSettings
import com.lillah.dhikr.domain.sync.RemoteFigures
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Firestore, with no server code behind it.
 *
 * The whole cloud is one collection:
 * ```
 * contributions/{uid}   { total, todayTotal, todayEpochDay, updatedAt }
 * ```
 *
 * A signed-in person may write exactly one document — their own — and it holds nothing but
 * numbers. No name, no email, no record of which dhikr was said. The worldwide figure is a
 * server-side `sum()` across that collection, so there is no counter document to maintain, nothing
 * to keep consistent, and no single row for every device on earth to contend over.
 *
 * That last point is why this shape was chosen over an incrementing global counter: Firestore
 * sustains about one write per second to any single document, which a shared counter would hit
 * long before the app was interesting. Summing on read has no such ceiling.
 */
class FirestoreBackend : DhikrBackend {

    override val isConfigured = true

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                // Firestore's own cache answers reads while offline, so the last known worldwide
                // figure survives a connection dropping mid-session.
                .setLocalCacheSettings(persistentCacheSettings {})
                .build()
        }
    }

    override suspend fun publishContribution(
        uid: String,
        total: Long,
        todayTotal: Long,
        todayEpochDay: Long,
    ): Result<Unit> = call {
        firestore.collection(CONTRIBUTIONS).document(uid).set(
            mapOf(
                "total" to total,
                "todayTotal" to todayTotal,
                "todayEpochDay" to todayEpochDay,
                "updatedAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        ).await()
        Unit
    }

    override suspend fun fetchFigures(todayEpochDay: Long): Result<RemoteFigures> {
        val collection = firestore.collection(CONTRIBUTIONS)

        // Aggregations are computed by the server and billed per thousand entries scanned, so the
        // worldwide total costs a handful of reads however many people are counting. This one is
        // unfiltered, so it needs no index and is the figure the board is really about.
        val worldwide = call {
            collection
                .aggregate(AggregateField.sum(FIELD_TOTAL), AggregateField.count())
                .get(AggregateSource.SERVER)
                .await()
        }.getOrElse { return Result.failure(it) }

        // Today's figure is a *filtered* aggregation, which Firestore will only serve once a
        // composite index over (todayEpochDay, todayTotal) exists. Asking for it in the same
        // breath as the total meant one missing index blanked the entire board, so it is asked
        // for separately and allowed to come back unknown.
        val today = call {
            collection
                .whereEqualTo(FIELD_TODAY_DAY, todayEpochDay)
                .aggregate(AggregateField.sum(FIELD_TODAY_TOTAL))
                .get(AggregateSource.SERVER)
                .await()
        }.getOrNull()

        return Result.success(
            RemoteFigures(
                globalTotal = worldwide.longOf(AggregateField.sum(FIELD_TOTAL)),
                globalToday = today?.longOf(AggregateField.sum(FIELD_TODAY_TOTAL)),
                participantCount = worldwide.count,
                userTotal = 0, // The device is the authority on the user's own figure.
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    /**
     * Runs a Firestore call under a deadline, and turns its failures into something a person can
     * act on.
     *
     * The deadline is not optional. A Firestore write is accepted into the local cache
     * immediately and its Task only completes when the server acknowledges — so with no route to
     * the server it never completes at all, and an awaiting caller waits forever. Timing out
     * costs nothing here: the contribution is an absolute total, so the next attempt simply sends
     * the same figure again.
     */
    private suspend fun <T> call(block: suspend () -> T): Result<T> {
        val outcome = withTimeoutOrNull(TIMEOUT_MILLIS) { runCatching { block() } }
            ?: return Result.failure(
                BackendUnreachable(
                    "Could not reach the world count. Your dhikr are safe here — try again when " +
                        "you have a connection."
                )
            )
        return outcome.recoverCatching { throw it.asBackendError() }
    }

    private fun Throwable.asBackendError(): Throwable = when {
        this !is FirebaseFirestoreException -> this
        code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> BackendRefused(
            "The database is not accepting writes yet. Publish the security rules in the Firebase " +
                "console (Firestore Database → Rules → Publish), then try again."
        )
        code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> BackendUnreachable(
            "Could not reach the world count. Your dhikr are safe here — try again when you have " +
                "a connection."
        )
        code == FirebaseFirestoreException.Code.NOT_FOUND -> BackendRefused(
            "This project has no Firestore database yet. Open the Firebase console → Firestore " +
                "Database → Create database, choose production mode, then publish the rules."
        )
        code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> BackendRefused(
            "The world count has used up today's free Firestore quota. Your dhikr are safe here " +
                "and will be added when it resets."
        )
        code == FirebaseFirestoreException.Code.UNAUTHENTICATED -> BackendRefused(
            "Your sign-in has expired. Sign out and back in to keep contributing."
        )
        code == FirebaseFirestoreException.Code.FAILED_PRECONDITION -> BackendRefused(
            "The world count needs an index Firestore has not built yet. Open the link in the " +
                "Firebase console's error log, or wait a few minutes and try again."
        )
        else -> this
    }

    /** `sum()` comes back as a Number whose concrete type Firestore does not promise. */
    private fun com.google.firebase.firestore.AggregateQuerySnapshot.longOf(
        field: AggregateField,
    ): Long = (get(field) as? Number)?.toLong() ?: 0L

    private companion object {
        /** Generous enough for a slow connection, short enough not to look frozen. */
        const val TIMEOUT_MILLIS = 20_000L
        const val CONTRIBUTIONS = "contributions"
        const val FIELD_TOTAL = "total"
        const val FIELD_TODAY_TOTAL = "todayTotal"
        const val FIELD_TODAY_DAY = "todayEpochDay"
    }
}
