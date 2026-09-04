# Dhikr — ذِكْر

A digital tasbih and daily remembrance app for Android. Offline-first, and built so that keeping
a habit feels better than breaking one.

<sub>Package `com.lillah.dhikr` · v1.4.0 (versionCode 5) · Kotlin · Jetpack Compose · minSdk 26 · targetSdk 35</sub>

---

## What it does

**Counting.** The home screen is the counter. A large ring fills one bead per repetition —
individually below sixty, as a smooth arc above that — and answers each tap with a short haptic
pulse, an outward ring of light, and a number that arrives from below rather than swapping in
place. A completed round blooms once and then gets out of the way. Long-press to undo. Volume keys
can drive it too, so a long sitting can happen with the screen off.

**Adhkar and collections.** Ships with Morning Adhkar, Evening Adhkar, everyday tasbih, and
after-prayer remembrance — full Arabic with harakat, transliteration, English meaning, and a source
citation where a well-known narration exists. Everything is editable. Users can add their own
adhkar and group them into collections with their own artwork or a photo of their own.

**Progress.** Day, week and month views: a goal ring with a per-dhikr breakdown, seven animated
bars against the user's own previous week, and a month heatmap. Lifetime totals are framed as a
garden that grows and never shrinks. Twenty-one milestones show their progress while still locked.

**Themes.** Nine palettes, each chosen from a gallery where every card paints itself in its own
colours. The whole app cross-fades into the choice. Light, dark, and system.

**Universal Dhikr.** A community board showing the worldwide total, your contribution, and your
share of that total — recomputed from live figures rather than stored. Beneath it, today, this
week, this month, this year and all time. When there is something to add or the world figure has
gone stale, it invites you to connect; it never insists.

**Accounts, and where your data lives.** Optional Google or Facebook sign-in identifies **which
local profile** to show. Everything you count is stored on the device and stays there — several
people can share a phone, each with their own history, and none of them can see another's. The
only thing that ever leaves is a running total, and only to add your contribution to the
worldwide count.

**Guidebook.** Seven short articles on dhikr and on the app itself, written to be read.

---

## Nothing here can delete your data

This is the strongest guarantee the app makes, and it is enforced structurally rather than by
being careful.

There is no delete. Not in Settings, not in the editors, not in the data layer. `grep -rn "DELETE
FROM" app/src/main/java` returns nothing, because no such query exists. Every repository method
that removed something is gone; every DAO query that removed something is gone. Archiving is the
only way to put a dhikr or a collection out of sight, and it keeps the row and every count against
it. A replaced cover image is kept on disk rather than overwritten away.

`fallbackToDestructiveMigration` is absent and commented as permanently unwelcome: a migration gap
must fail loudly, not hand somebody an empty app.

The trade-off is real and deliberate: a dhikr created by mistake can only be archived, never
removed, and there is no in-app way to honour a "delete my data" request. Uninstalling remains the
only way to remove anything, and that is the operating system's doing, not the app's.

---

## Upgrading

Installing v1.4.0 over any earlier version is an ordinary update. There is no uninstall step, and
nothing is reset. Three things make that true, and all three are verified against the built APK
rather than assumed:

| | |
| --- | --- |
| **Same package id** | `com.lillah.dhikr`, unchanged since v1.0.0 |
| **Higher versionCode** | 1 → 2 → 3 → 4 → 5 |
| **Same signing key** | SHA-256 `35:DA:2A:FB…`, identical to the released v1.0.0 APK |

Every migration is additive. Version 2 added two tables. Version 3 adds profiles and a
`profileId` column defaulting to 1 — the device profile, which the migration creates and which
every existing row therefore already belongs to. Nobody's history moves, because there is only one
profile for it to be in.

Version 4 adds one table recording what the world count was last told, and alters nothing.
Version 5 changes no schema at all — it is a fix to the Universal Dhikr board only.

Achievements and counters needed a composite key, and SQLite cannot add one without rebuilding the
table. Rather than rebuild a table holding user data, their rows are **copied** into new
per-profile tables and the originals are left exactly where they are — still populated, never
written to again, never dropped.

Two test suites run against real databases built from the released schemas' own DDL and identity
hashes:

- `MigrationV1ToLatestTest` — 25,000 counts over 100 days survive the whole chain exactly, a
  half-finished round is not reset, adding 5,000 afterwards gives 30,000 with earlier days
  untouched, and empty, sparse, 4,000-row and already-migrated databases all open cleanly.
- `MigrationV2ToV3Test` — a v1.1.0 database keeps its counts, achievements, counters and sync
  queue; the device profile is created; and the pre-v3 tables are asserted to still hold their
  rows afterwards.

### Signing and upgrade continuity

`signing/dhikr-upgrade.jks` is committed deliberately. Android refuses an update signed by a
different key, and this is the key that signed the released v1.0.0 APK — losing it would strand
every existing install. It is debug-grade by origin and guards nothing: treat it as a compatibility
artifact, not a secret.

A `keystore.properties` at the project root overrides it for publishing. Be aware that doing so
breaks in-place updates for anyone who installed a build signed with the continuity key; for Play
Store distribution, enrol in Play App Signing before the first release rather than switching keys
afterwards.

---

## Design principles

The gamification is built to encourage without pressuring, which drove several decisions that a
generic habit tracker would make differently:

| Decision | Why |
| --- | --- |
| A day counts toward a streak if *anything* was counted — meeting the goal is not required | The goal is the floor, not a test |
| An untouched today never breaks a streak | The day is not over yet |
| The longest run is kept and still shown after a streak ends | Nothing already earned is taken away |
| Locked milestones show their progress instead of hiding | The list reads as what is ahead, not what is missing |
| Lifetime progress grows and never decreases; there is no rank | Nothing to lose, and nobody else's to compare against |
| No leaderboards, no social features, no notifications designed to create urgency | Worship is not a competition |

Copy was written in the same register. When a week is quieter than the last, the app says so as
information — *"A quieter week than the last one. Weeks differ; the habit is what carries."* — not
as a verdict.

---

## Architecture

Single Gradle module, layered by package.

```
com.lillah.dhikr
├── core/            clock, haptics, synthesised audio, cover storage, DI container
├── data/
│   ├── local/       Room entities, DAOs, mappers, migrations
│   ├── backend/     cloud abstraction, Firestore and Firebase Auth implementations
│   ├── prefs/       DataStore settings and account state
│   ├── seed/        shipped adhkar
│   ├── guide/       guidebook content
│   └── repository/  Dhikr, Stats, Gamification, Sync
├── domain/
│   ├── model/       UI-shaped models, independent of Room
│   ├── gamification/streaks, growth stages, achievement catalog
│   └── sync/        operation kinds, auth types, contribution maths
└── ui/
    ├── theme/       palettes, derived colour schemes, type, shape, motion
    ├── components/  counter, rings, charts, covers, cards, sheets
    ├── navigation/  routes, nav bar, NavHost
    ├── vm/          ViewModel plumbing
    └── screens/     home, collections, editor, progress, universal, guide, settings, manage
```

**State.** Unidirectional. ViewModels expose a single immutable `UiState` via `StateFlow`;
screens are stateless composables taking that state plus callbacks, which is what lets every one of
them be rendered in a test.

**Dependency injection** is a hand-rolled `AppContainer` of lazies rather than Hilt. One module,
one process-wide graph, no build-variant swapping — a container gives the same constructor
injection and the same testability without adding a second annotation processor to the build.

**Persistence.** Room holds adhkar, collections, achievements, a small counter table, and a
per-dhikr-per-day count ledger that every statistic is derived from. Anything re-derivable is
re-derived; only facts with no durable trace (how many times a collection was finished; how many
days a goal that has since changed was met) are stored as counters.

### Profiles

Signing in decides **which local profile** is on screen, not where the data goes.

Profile 1 is the device profile and always exists. The first account to sign in *adopts* it rather
than starting from zero, which is what makes an upgrade invisible: somebody with 25,000 dhikr from
before accounts existed signs in and still has 25,000. A different account signing in on the same
phone gets a profile of its own, seeded with its own copy of the shipped adhkar and completely
isolated — every query in the data layer is scoped by `profileId`, and the count aggregates join
through `dhikr` so one person's totals cannot include another's. Signing out returns to the device
profile; it hides data and never removes it.

`ProfileIsolationTest` covers all of it: adoption, isolation, switching back and forth, repeated
sign-ins not duplicating profiles, and milestones not being inherited.

### Sync

Counting is local and immediate; the network is downstream of it and never in the way.

Connecting publishes **one absolute running total**, in one document write, however much was
counted in between. That single choice settles three things at once:

- **It cannot double count.** Writing the same total twice leaves the cloud where it was. There is
  no idempotency key to get wrong, because nothing is being added.
- **It cannot lose anything.** What is still owed to the world count is not a queue that could be
  dropped — it is the difference between the device's lifetime total and the figure last accepted.
  A device that has never connected reports its whole history as waiting, with no migration step
  needed to discover that.
- **It costs one write.** An earlier design uploaded a document per counted tap, which would have
  exhausted a free Firestore quota at roughly fifty active people.

Only numbers are uploaded — a total and a date. Which dhikr somebody said, and how often, never
leaves the device.

Every count writes two rows in **one transaction**: the day ledger, and an append-only operation in
an outbox. A count that reached the device but not the queue would never reach the cloud, so
neither is allowed to commit without the other.

Each operation carries a client-generated UUID that becomes its remote document id, and the
aggregation function claims a marker document inside the same transaction that moves the totals.
That is what makes retries safe: a client retry, a function retry, and a rewritten document all
converge on the marker and stop. `SyncRepositoryTest` pushes the same operations three times and
asserts the total is unchanged.

Nothing is ever discarded. A failed push increments an attempt counter and stays queued; there is
no code path that deletes an unsynced operation. Signing out clears the session and nothing else.

**Existing history** — the counting a device did before this feature existed — is claimed once, as
a single operation whose id is derived from the device. The amount is the local lifetime total
minus everything the outbox has ever carried, which is exactly the pre-outbox history. Running the
claim repeatedly inserts nothing new, so 12,500 dhikr before the upgrade are 12,500 after it, and
not 25,000.

The account total shown is the server's figure plus anything still queued locally, so the number
never stalls mid-sync and a second device signing into the same account shows the account's total
rather than its own.

### Backend

Firestore, and nothing else — no server code, no scheduled jobs, no billing plan. The entire cloud
is one collection of `contributions/{uid}` documents holding a number, and the worldwide figure is
a server-side `sum()` across it.

That replaced a Cloud Function maintaining a single global counter, for two reasons: Firestore
sustains only about one write per second to any one document, so a shared counter contends and
fails long before the app gets interesting; and summing on read is billed per thousand entries
scanned, so the worldwide total costs a handful of reads at any scale. The rules let a signed-in
person write exactly one document — their own — and never lower it.

Setup, data layout and the rules are documented in [`backend/README.md`](backend/README.md).

**The app builds and runs with no backend at all.** Firebase is initialised programmatically from a
gitignored `backend.properties` rather than through the google-services Gradle plugin, which fails
the build outright when its config file is missing. Without credentials the app is a complete local
counter — profiles, statistics, milestones and all — and the Universal Dhikr tab says so plainly
rather than showing an error.

### Themes

Nine palettes, each authored as five seed colours from which the full Material 3 scheme is derived
for light and dark. The gradients shift hue rather than only lightness, surfaces step through six
distinguishable levels instead of four near-identical ones, and every gradient surface carries a
soft top-light — which is most of the difference between a coloured rectangle and something that
looks lit.

### Decisions worth knowing about

- **No `ON CONFLICT DO UPDATE`.** SQLite gained UPSERT in 3.24, which lands on Android 11, and this
  app supports Android 8. Counting uses insert-ignore then update inside a transaction instead.
- **A mutex around the live round.** The day ledger increments in SQL and is safe on its own, but
  the current round is a read-modify-write, and rapid tapping is the normal case here — two taps
  reading the same count would lose one.
- **Optimistic counting.** Taps are answered on the frame they happen and reconciled against the
  database a moment later. The override is dropped once the stored value catches up, so a change
  made elsewhere is never masked by a stale local one.
- **A midnight ticker.** Every "today"-scoped flow hangs off a flow that re-emits when the date
  turns over. Without it, an app left open overnight would keep writing into yesterday.
- **Covers are copied, not referenced.** A picked image is decoded, oriented from EXIF, downscaled
  and written into app storage. Content-URI grants can be revoked and the backing file can move;
  neither should cost a user their cover.
- **Feedback tones are synthesised at runtime** with `AudioTrack` rather than shipped as audio
  files — a decaying two-partial knock for a tap, three rising notes for a completed round. The APK
  carries no binary assets at all.
- **Artwork is drawn, not bundled.** Collection covers are Canvas vectors that keep their own
  intrinsic palette and are pulled only part-way toward the active theme, so a sunrise still reads
  as a sunrise in the Ocean theme.
- **No font binaries.** The system family renders Latin and Arabic well everywhere and guarantees a
  correct Naskh fallback. Character comes from weight, size and tracking.
- **Restore is additive.** Re-inserting the built-in adhkar would cascade away their recorded
  counts, so restore only adds back what is missing and never touches what is there.

### Themes

A palette is authored as five seed colours; the full Material 3 scheme for light and dark is
derived from them, so adding a theme means picking five colours rather than tuning forty.

---

## Building

Requires JDK 17+ and the Android SDK (platform 35, build-tools 35.0.0). The Gradle wrapper handles
the rest.

```bash
./gradlew :app:assembleDebug     # debug APK
./gradlew :app:assembleRelease   # release APK, R8-shrunk (~4 MB)
./gradlew :app:testDebugUnitTest # 99 unit, migration, profile, sync and screen tests
```

Cloud features need a `backend.properties`; see [`backend/README.md`](backend/README.md). Without
it the build succeeds and the app runs as a purely local counter.

Outputs land in `app/build/outputs/apk/`.

### Signing

`assembleRelease` signs with the continuity key by default, so its output installs over an existing
v1.0.0. To publish with your own key, add a gitignored `keystore.properties` at the project root —
and read the warning in "Signing and upgrade continuity" first:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

---

## Tests

99 tests, all JVM — no device or emulator needed.

- **Migration** — real v1 and v2 databases, built from the released schemas' own DDL and identity
  hashes, upgraded and read back through the production DAOs. See "Upgrading" above.
- **Profiles** — adoption, isolation between people sharing a phone, switching accounts, and
  seeding.
- **Sync** — an in-memory database against a fake backend shaped like the real collection: a
  thousand dhikr cost one write, connecting six times does not inflate the total, a failed connect
  leaves the counting untouched and still owed, and two people on one phone publish separately
  without either total absorbing the other.
- **Contribution maths** — the share calculation and its formatting, including the small
  percentages the board actually shows.

- **Domain** — streak edges (an untouched today, duplicate days, unsorted input), growth stage
  monotonicity, achievement unlock rules and metric coverage.
- **Data** — range filling, week boundaries across locales, and the seeded content: unique names
  within a collection (restore matches on name, so a duplicate would be unrestorable), Arabic
  actually in Arabic script, the after-prayer tasbih keeping its traditional counts.
- **UI** — every screen rendered under Robolectric, including the states easiest to get wrong: an
  empty collection list, a fully complete collection, no history at all, all seven palettes, and
  the Universal board signed in, signed out, mid-sync, prompting to connect, and with no backend
  configured.

---

## Built to extend

Deliberately left in place for what comes next:

- **Localisation** — user-facing strings sit in composables and in Kotlin content objects
  (`GuideContent`, `SeedData`) rather than being scattered. Swapping either for a locale-aware
  provider is a contained change, and the layout is already RTL-ready via auto-mirrored icons and
  `supportsRtl`.
- **Widgets and reminders** — the same repositories serve a Glance widget or a scheduled worker
  without change.
- **Cross-device history** — counting is deliberately device-local, so a second phone starts a
  fresh history for the same account. The operation log already carries what a future sync would
  need if that ever becomes wanted.

---

## A note on the content

The adhkar shipped here are widely transmitted, written with full harakat, and cited where a
well-known narration exists. The app is a counter and a record, not a scholarly reference. Every
seeded item stays editable so any wording or count can be corrected to the reading you follow.
