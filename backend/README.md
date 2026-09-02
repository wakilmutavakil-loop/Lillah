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

## 5. Deploy rules and functions

```bash
npm install -g firebase-tools
firebase login
cd backend
firebase use --add          # select your project
firebase deploy --only firestore:rules,firestore:indexes,functions
```

Cloud Functions require the Blaze plan. The workload here is one small transaction per batch of
counting and a nightly reconciliation pass.

---

## How the data is laid out

```
users/{uid}                  profile; total, todayTotal — written only by the functions
users/{uid}/ops/{opId}       one immutable operation per counting action
users/{uid}/applied/{opId}   idempotency marker; no client can read or write this
aggregates/global            worldwide total, today's total, participant count
```

### Why it cannot double count

The client generates a UUID for every counting action and uses it as the document id. A retry
after a timeout therefore rewrites the *same* document rather than adding a second one.

`applyOperation` then claims `users/{uid}/applied/{opId}` inside the same transaction that moves
the totals. Whichever way an operation arrives twice — a client retry, a function retry, a
rewritten document — the second attempt finds the marker and stops. Totals are clamped at zero so
an undo can never drive one negative.

`reconcileGlobalTotal` recomputes the worldwide figure from the per-user totals nightly. It should
never find a discrepancy; running it is how you know that stays true.

### What the rules enforce

- A user can read and write only their own documents.
- Operations are append-only and immutable — no client can edit or delete one after the fact.
- `total`, `todayTotal` and the applied markers are closed to every client. The only writer is the
  aggregation function, which runs with Admin credentials and bypasses rules.
- `aggregates/global` is readable by any signed-in user and writable by none.
- Every path not explicitly allowed is denied.

Test the rules before trusting them:

```bash
firebase emulators:start --only firestore
```

---

## Running without a backend

With no `backend.properties`, `BackendFactory` returns `UnconfiguredBackend` and
`UnconfiguredAuthGateway`. The app behaves like a device that is permanently offline — a state the
sync engine already handles — so counting, history, statistics, themes and the guidebook all work
exactly as they did in v1.0.0. The Universal Dhikr tab explains that no cloud is attached rather
than showing an error.
