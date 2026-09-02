/**
 * Universal Dhikr aggregation.
 *
 * The client never writes a total. It writes immutable operation documents whose ids it chooses,
 * and this file folds them into the per-user and worldwide figures.
 *
 * Exactly-once is the whole problem, and it is solved in one place: before an operation is
 * counted, a marker document at users/{uid}/applied/{opId} is claimed inside the same transaction
 * that moves the totals. A function retry, a client retry after a timeout, and a client that
 * rewrites the same operation all converge on that marker and stop. Clients cannot write the
 * marker collection at all — the security rules deny every path they do not explicitly allow.
 */

const {onDocumentCreated, onDocumentWritten} = require("firebase-functions/v2/firestore");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const logger = require("firebase-functions/logger");

initializeApp();
const db = getFirestore();

const GLOBAL = () => db.collection("aggregates").doc("global");

/** Days since the Unix epoch, UTC — the same unit the client stores on each operation. */
function currentEpochDay() {
  return Math.floor(Date.now() / 86400000);
}

/**
 * A single operation, folded into the user's total and the worldwide total.
 *
 * onDocumentWritten rather than onDocumentCreated: a client whose confirmation was lost may
 * rewrite the same document, and that rewrite should still settle the totals if the original
 * create was never processed. The marker makes the repetition harmless either way.
 */
exports.applyOperation = onDocumentWritten(
    {document: "users/{uid}/ops/{opId}", retry: true},
    async (event) => {
      const after = event.data && event.data.after;
      if (!after || !after.exists) return;

      const {uid, opId} = event.params;
      const operation = after.data();
      const delta = Number(operation.delta);

      if (!Number.isFinite(delta) || Number.isNaN(delta)) {
        logger.warn("discarding operation with a non-numeric delta", {uid, opId});
        return;
      }
      // Server-side bound, mirroring the security rules. Rules already reject these, but a
      // function must not depend on rules having been the only way in.
      if (delta < -1000 || delta > 100000000) {
        logger.warn("discarding out-of-range delta", {uid, opId, delta});
        return;
      }

      const marker = db.collection("users").doc(uid).collection("applied").doc(opId);
      const userRef = db.collection("users").doc(uid);
      const today = currentEpochDay();

      await db.runTransaction(async (tx) => {
        const existing = await tx.get(marker);
        if (existing.exists) return; // Already counted. Nothing to do, ever.

        const userSnapshot = await tx.get(userRef);
        const globalSnapshot = await tx.get(GLOBAL());

        const previousUserTotal = Number((userSnapshot.data() || {}).total || 0);
        const previousGlobalTotal = Number((globalSnapshot.data() || {}).total || 0);

        // Totals never go below zero. An undo that would push a total negative is clamped rather
        // than rejected, so the user's local state and the server's cannot diverge permanently.
        const userTotal = Math.max(0, previousUserTotal + delta);
        const globalTotal = Math.max(0, previousGlobalTotal + delta);

        const countsForToday = Number(operation.epochDay) === today;
        const globalData = globalSnapshot.data() || {};
        const globalDayIsCurrent = Number(globalData.todayEpochDay || 0) === today;
        const previousGlobalToday = globalDayIsCurrent ? Number(globalData.todayTotal || 0) : 0;

        const userData = userSnapshot.data() || {};
        const userDayIsCurrent = Number(userData.todayEpochDay || 0) === today;
        const previousUserToday = userDayIsCurrent ? Number(userData.todayTotal || 0) : 0;

        tx.set(userRef, {
          total: userTotal,
          todayTotal: countsForToday ? Math.max(0, previousUserToday + delta) : previousUserToday,
          todayEpochDay: today,
          updatedAt: Date.now(),
        }, {merge: true});

        tx.set(GLOBAL(), {
          total: globalTotal,
          todayTotal: countsForToday ?
            Math.max(0, previousGlobalToday + delta) :
            previousGlobalToday,
          todayEpochDay: today,
          updatedAt: Date.now(),
        }, {merge: true});

        tx.set(marker, {
          delta,
          kind: operation.kind || "COUNT_DELTA",
          appliedAt: Date.now(),
        });
      });
    },
);

/** One participant, counted once, when their profile document first appears. */
exports.countParticipant = onDocumentCreated("users/{uid}", async () => {
  await GLOBAL().set({
    participantCount: FieldValue.increment(1),
    updatedAt: Date.now(),
  }, {merge: true});
});

/**
 * Nightly reconciliation.
 *
 * The incremental path above is transactional and idempotent, so this should never find a
 * discrepancy — which is exactly why it is worth running. A stored aggregate that nothing ever
 * checks is an aggregate that drifts silently; this recomputes the worldwide total from the
 * per-user totals and repairs it if they disagree.
 */
exports.reconcileGlobalTotal = onSchedule("every day 03:00", async () => {
  let total = 0;
  let participants = 0;

  let cursor = null;
  for (;;) {
    let query = db.collection("users").orderBy("__name__").limit(500);
    if (cursor) query = query.startAfter(cursor);
    const page = await query.get();
    if (page.empty) break;

    page.docs.forEach((doc) => {
      total += Number(doc.data().total || 0);
      participants += 1;
    });
    cursor = page.docs[page.docs.length - 1];
    if (page.size < 500) break;
  }

  const snapshot = await GLOBAL().get();
  const stored = Number((snapshot.data() || {}).total || 0);
  if (stored !== total) {
    logger.warn("global total drifted; repairing", {stored, recomputed: total});
  }

  await GLOBAL().set({
    total,
    participantCount: participants,
    reconciledAt: Date.now(),
    updatedAt: Date.now(),
  }, {merge: true});
});
