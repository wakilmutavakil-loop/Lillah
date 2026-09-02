package com.lillah.dhikr.data.backend

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.lillah.dhikr.data.local.entity.SyncOperationEntity
import com.lillah.dhikr.domain.sync.AuthUser
import com.lillah.dhikr.domain.sync.RemoteFigures
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Firestore implementation.
 *
 * Layout:
 * ```
 * users/{uid}                    profile, and server-maintained totals
 * users/{uid}/ops/{opId}         one immutable operation per counting action
 * aggregates/global              server-maintained worldwide totals
 * ```
 *
 * Clients only ever create operation documents. Totals are folded in by a Cloud Function and are
 * closed to client writes by the security rules, so nobody can set their own total — or anybody
 * else's — by writing to the database directly.
 */
class FirestoreBackend : DhikrBackend {

    override val isConfigured = true

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                // Firestore's own cache serves reads while offline, so the board still has
                // figures to show when the network does not.
                .setLocalCacheSettings(persistentCacheSettings {})
                .build()
        }
    }

    override suspend fun push(
        uid: String,
        operations: List<SyncOperationEntity>,
    ): Result<Unit> = runCatching {
        // The operation id is the document id. Re-uploading an operation therefore overwrites its
        // own document rather than adding a second one, which is what makes a retry after a
        // timeout safe: the aggregation function only ever folds a given document in once.
        val batch = firestore.batch()
        val ops = firestore.collection(USERS).document(uid).collection(OPS)
        operations.forEach { operation ->
            batch.set(
                ops.document(operation.opId),
                mapOf(
                    "kind" to operation.kind,
                    "dhikrId" to operation.dhikrId,
                    "dhikrName" to operation.dhikrName,
                    "epochDay" to operation.epochDay,
                    "delta" to operation.delta,
                    "createdAt" to operation.createdAt,
                ),
                SetOptions.merge(),
            )
        }
        batch.commit().await()
        Unit
    }

    override suspend fun registerUser(user: AuthUser): Result<Unit> = runCatching {
        firestore.collection(USERS).document(user.uid).set(
            mapOf(
                "displayName" to user.displayName,
                "provider" to user.method?.id,
                "lastSeenAt" to System.currentTimeMillis(),
            ),
            // Merge, so this never clobbers the server-maintained totals on the same document.
            SetOptions.merge(),
        ).await()
        Unit
    }

    override suspend fun fetchFigures(uid: String?): Result<RemoteFigures> = runCatching {
        val global = firestore.collection(AGGREGATES).document(GLOBAL).get().await()
        val user = uid?.let { firestore.collection(USERS).document(it).get().await() }
        RemoteFigures(
            globalTotal = global.getLong("total") ?: 0,
            globalToday = global.getLong("todayTotal") ?: 0,
            participantCount = global.getLong("participantCount") ?: 0,
            userTotal = user?.getLong("total") ?: 0,
            updatedAt = global.getLong("updatedAt") ?: System.currentTimeMillis(),
        )
    }

    override fun observeFigures(uid: String?): Flow<RemoteFigures> {
        val globalFlow = documentFlow(firestore.collection(AGGREGATES).document(GLOBAL))
        val userFlow = if (uid == null) {
            flowOf(emptyMap())
        } else {
            documentFlow(firestore.collection(USERS).document(uid))
        }
        return combine(globalFlow, userFlow) { global, user ->
            RemoteFigures(
                globalTotal = global.longOr("total"),
                globalToday = global.longOr("todayTotal"),
                participantCount = global.longOr("participantCount"),
                userTotal = user.longOr("total"),
                updatedAt = global.longOr("updatedAt").takeIf { it > 0 }
                    ?: System.currentTimeMillis(),
            )
        }
    }

    private fun documentFlow(
        reference: com.google.firebase.firestore.DocumentReference,
    ): Flow<Map<String, Any?>> = callbackFlow {
        val registration = reference.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // A listener error is not fatal: the cached snapshot stays on screen and the
                // periodic sync will refresh it. Closing the flow would kill live updates.
                return@addSnapshotListener
            }
            trySend(snapshot?.data.orEmpty())
        }
        awaitClose { registration.remove() }
    }

    private fun Map<String, Any?>.longOr(key: String): Long = (this[key] as? Number)?.toLong() ?: 0

    private companion object {
        const val USERS = "users"
        const val OPS = "ops"
        const val AGGREGATES = "aggregates"
        const val GLOBAL = "global"
    }
}
