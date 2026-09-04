# Backend setup

The app compiles, installs and counts dhikr with none of this in place. Everything here adds
accounts, sync and the Universal Dhikr board on top of a counter that already works offline.

Nothing in this directory contains credentials, and nothing in the repository does. The app reads
its project keys from a gitignored `backend.properties` at the repository root.

---

## 1. Create the Firebase project

1. <https://console.firebase.google.com> → **Add project**.
2. Build → **Firestore Database** → Create database → production mode.
3. Build → **Authentication** → Get started.

## 2. Register the Android app

Project settings → **Your apps** → Add app → Android.

- Package name: `com.lillah.dhikr` — this must match exactly.
- SHA-1 certificate fingerprint: required for Google sign-in.

For builds made from this repository, the signing key is `signing/dhikr-upgrade.jks`:

```bash
keytool -list -v -keystore signing/dhikr-upgrade.jks \
  -alias androiddebugkey -storepass android | grep SHA1
```

If you later publish with your own key, add that key's SHA-1 too — and read the warning in the
main README about what changing the signing key does to existing installs.

You do **not** need to download `google-services.json`. The app is configured from properties
instead, so that it can still be built by someone who does not have your project.

## 3. Enable sign-in providers

Authentication → Sign-in method:

- **Google** — enable. Then Project settings → Your apps → Web app; its OAuth client id is the
  `googleWebClientId` below. (Web, not Android: Credential Manager requests an ID token audienced
  to the *server* client.)
- **Facebook** — enable, and paste in the App ID and App Secret from
  <https://developers.facebook.com>. Copy the OAuth redirect URI Firebase shows you into the
  Facebook app's *Valid OAuth Redirect URIs*. In the Facebook app, add the Android platform with
  package name `com.lillah.dhikr` and the same SHA-1, and generate a Client Token under
  Settings → Advanced.

Either provider alone is enough. The app offers only the ones it has credentials for.

## 4. Write `backend.properties`

At the repository root, next to `settings.gradle.kts`:

```properties
firebaseApiKey=AIza...
firebaseAppId=1:1234567890:android:abcdef...
firebaseProjectId=your-project-id
firebaseSenderId=1234567890

# Omit to leave Google sign-in out of the build.
googleWebClientId=1234567890-....apps.googleusercontent.com

# Omit both to leave Facebook sign-in out of the build.
facebookAppId=...
facebookClientToken=...
```

The first three come from Project settings → Your apps → Android app → SDK setup and
configuration. The file is gitignored; do not commit it.

Rebuild. On the Universal Dhikr tab, "Not connected" becomes a live figure and the sign-in
buttons appear.

Keep this file. It is gitignored, so a fresh checkout builds a working offline app with no cloud
attached until the file is supplied again.

## 5. Publish the security rules

No command line, no Cloud Functions, no billing. Paste the rules into the console:

1. Firebase console → **Firestore Database** → the **Rules** tab
2. Select everything in the editor and replace it with the contents of
   [`firestore.rules`](firestore.rules)
3. Click **Publish**

That is the whole deployment. There is no server code to run.

---

## How the data is laid out

One collection, holding nothing but numbers:

```
contributions/{uid}   { total, todayTotal, todayEpochDay, updatedAt }
```

No name, no email, no record of which dhikr was said — all of that stays on the device. What
reaches the cloud is an account id and a running total.

### Why there is no counter document

An earlier design kept a single `aggregates/global` document that every contribution incremented,
maintained by a Cloud Function. Two problems retired it:

- **Cost.** One document per counted tap meant a hundred dhikr cost a hundred writes plus a
  hundred function invocations. The free Firestore quota supported roughly fifty active people.
- **Contention.** Firestore sustains about one write per second to any single document. A shared
  counter would have started failing transactions long before the app was interesting, and fixing
  that properly means sharded counters and their own consistency problems.

The worldwide figure is now a server-side `sum()` across `contributions`, billed per thousand
entries scanned — a handful of reads however many people are counting — and there is no shared
document for anyone to contend over.

### Why it cannot double count

The client publishes an **absolute** running total, not an increment. Writing the same total twice
leaves the collection exactly where it was, so a retry after a timeout, a crash mid-write, or a
hundred repeated connects are all harmless. There is no idempotency key, because there is nothing
being added.

### What the rules enforce

- You may write exactly one document: the one named after your own account id.
- A total may never decrease, so a replayed or out-of-order write cannot roll somebody back.
- A total must be a non-negative integer below a billion; `todayTotal` cannot exceed it.
- Contributions cannot be deleted, matching the app itself.
- Every other path is denied.

Reads are open to any signed-in user, because Firestore evaluates rules for aggregation queries
too and the worldwide sum has to be allowed to run. What that exposes is a random account id and a
number.

### The one index

The headline worldwide total is an unfiltered `sum()` across the collection, which Firestore
serves with no index at all. The *today* figure is different: it filters on `todayEpochDay` and
sums `todayTotal`, and a filtered aggregation needs a composite index over both fields.

`firestore.indexes.json` declares it. If you deploy by pasting into the console rather than with
the CLI, you can skip it — the two figures are fetched separately and a missing index only makes
today's read as `—`. The lifetime total, the participant count and everybody's contribution are
unaffected. To add it later: Firebase console → Firestore Database → Indexes → Create index, on
collection `contributions`, fields `todayEpochDay` ascending then `todayTotal` ascending.

### Worth adding later

**App Check** would tie writes to genuine installs of your app, which is the proper answer to
somebody publishing an inflated total for themselves. The rules above cap the damage — one account
cannot touch another's figure, and cannot exceed the ceiling — but they cannot tell an honest
client from a modified one.

---

## Running without a backend

With no `backend.properties`, `BackendFactory` returns `UnconfiguredBackend` and
`UnconfiguredAuthGateway`. The app behaves like a device that is permanently offline — a state the
sync engine already handles — so counting, history, statistics, themes and the guidebook all work
exactly as they did in v1.0.0. The Universal Dhikr tab explains that no cloud is attached rather
than showing an error.
