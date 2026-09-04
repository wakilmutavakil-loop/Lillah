package com.lillah.dhikr.data.backend

import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.persistentCacheSettings
import com.lillah.dhikr.domain.sync.RemoteFigures
import kotlinx.coroutines.tasks.await

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
    ): Result<Unit> = runCatching {
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

    override suspend fun fetchFigures(todayEpochDay: Long): Result<RemoteFigures> = runCatching {
        val collection = firestore.collection(CONTRIBUTIONS)

        // Aggregations are computed by the server and billed per thousand entries scanned, so the
        // worldwide total costs a handful of reads however many people are counting.
        val worldwide = collection
            .aggregate(AggregateField.sum(FIELD_TOTAL), AggregateField.count())
            .get(AggregateSource.SERVER)
            .await()

        val today = collection
            .whereEqualTo(FIELD_TODAY_DAY, todayEpochDay)
            .aggregate(AggregateField.sum(FIELD_TODAY_TOTAL))
            .get(AggregateSource.SERVER)
            .await()

        RemoteFigures(
            globalTotal = worldwide.longOf(AggregateField.sum(FIELD_TOTAL)),
            globalToday = today.longOf(AggregateField.sum(FIELD_TODAY_TOTAL)),
            participantCount = worldwide.count,
            userTotal = 0, // The device is the authority on the user's own figure.
            updatedAt = System.currentTimeMillis(),
        )
    }

    /** `sum()` comes back as a Number whose concrete type Firestore does not promise. */
    private fun com.google.firebase.firestore.AggregateQuerySnapshot.longOf(
        field: AggregateField,
    ): Long = (get(field) as? Number)?.toLong() ?: 0L

    private companion object {
        const val CONTRIBUTIONS = "contributions"
        const val FIELD_TOTAL = "total"
        const val FIELD_TODAY_TOTAL = "todayTotal"
        const val FIELD_TODAY_DAY = "todayEpochDay"
    }
}
