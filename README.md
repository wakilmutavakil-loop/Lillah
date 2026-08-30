# Dhikr — ذِكْر

A digital tasbih and daily remembrance app for Android. Offline-first, no account,
no tracking, and built so that keeping a habit feels better than breaking one.

<sub>Package `com.lillah.dhikr` · Kotlin · Jetpack Compose · minSdk 26 · targetSdk 35</sub>

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

**Themes.** Seven palettes, each chosen from a gallery where every card paints itself in its own
colours. The whole app cross-fades into the choice. Light, dark, and system.

**Guidebook.** Seven short articles on dhikr and on the app itself, written to be read.

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
│   ├── local/       Room entities, DAOs, mappers
│   ├── prefs/       DataStore settings
│   ├── seed/        shipped adhkar
│   ├── guide/       guidebook content
│   └── repository/  Dhikr, Stats, Gamification
├── domain/
│   ├── model/       UI-shaped models, independent of Room
│   └── gamification/streaks, growth stages, achievement catalog
└── ui/
    ├── theme/       palettes, derived colour schemes, type, shape, motion
    ├── components/  counter, rings, charts, covers, cards, sheets
    ├── navigation/  routes, nav bar, NavHost
    ├── vm/          ViewModel plumbing
    └── screens/     home, collections, editor, progress, guide, settings, manage
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
./gradlew :app:assembleRelease   # release APK, R8-shrunk (~2 MB)
./gradlew :app:testDebugUnitTest # 50 unit and screen-render tests
```

Outputs land in `app/build/outputs/apk/`.

### Signing

`assembleRelease` produces an installable APK with no setup, falling back to the local debug key.
For a real signing key, drop a `keystore.properties` in the project root — it is gitignored, and no
key material is committed:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

---

## Tests

50 tests, all JVM — no device or emulator needed.

- **Domain** — streak edges (an untouched today, duplicate days, unsorted input), growth stage
  monotonicity, achievement unlock rules and metric coverage.
- **Data** — range filling, week boundaries across locales, and the seeded content: unique names
  within a collection (restore matches on name, so a duplicate would be unrestorable), Arabic
  actually in Arabic script, the after-prayer tasbih keeping its traditional counts.
- **UI** — every screen rendered under Robolectric, including the states easiest to get wrong: an
  empty collection list, a fully complete collection, no history at all, and all seven palettes.

---

## Built to extend

Deliberately left in place for what comes next:

- **Localisation** — user-facing strings sit in composables and in Kotlin content objects
  (`GuideContent`, `SeedData`) rather than being scattered. Swapping either for a locale-aware
  provider is a contained change, and the layout is already RTL-ready via auto-mirrored icons and
  `supportsRtl`.
- **Sync and backup** — repositories are the only thing that touch storage, and the count ledger is
  an append-friendly per-day table.
- **Widgets and reminders** — the same repositories serve a Glance widget or a scheduled worker
  without change.

---

## A note on the content

The adhkar shipped here are widely transmitted, written with full harakat, and cited where a
well-known narration exists. The app is a counter and a record, not a scholarly reference. Every
seeded item stays editable so any wording or count can be corrected to the reading you follow.
