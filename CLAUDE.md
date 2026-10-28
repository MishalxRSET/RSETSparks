# RSETSparks: notes for Claude Code

Campus dating app for verified Rajagiri students (18+). Native Android (Kotlin + Jetpack Compose) on Firebase.
The owner is preparing the first Google Play release. **Start with [HANDOFF.md](HANDOFF.md)**: it has the current
status and the ordered checklist of what to do next on this machine.

## First thing in every session

```bash
node scripts/doctor.mjs        # what's installed / configured / missing (add --json for machine-readable)
git status && git log --oneline -5
```

## Commands

| Task | Command |
|---|---|
| Unit tests | `./gradlew :app:testDebugUnitTest` |
| Debug build | `./gradlew :app:assembleDebug` |
| Install on connected phone/emulator | `./gradlew :app:installDebug` |
| Release bundle (needs `keystore.properties`) | `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab` |
| Signing fingerprints | `./gradlew :app:signingReport` |
| Firestore/Storage rules tests (Java + Node) | `cd firebase-tests && npm ci && npm test` |
| Cloud Functions tests | `cd functions && npm ci && npm test` |
| Deploy rules/indexes/functions/legal pages | `npx firebase-tools@14 deploy --only firestore:rules,firestore:indexes,storage,functions,hosting` |
| Function logs | `npx firebase-tools@14 functions:log` |
| Regenerate icons/splash/store art | `python3 scripts/generate_brand_assets.py` |
| App logs | `adb logcat -s AppRepository AuthService FirebaseMessaging` |

On Windows, use `gradlew.bat` if `./gradlew` doesn't work in the current shell.

## Verify (before every commit, deploy or release)

Run in order and keep going after a failure so the report is complete. Report a pass/fail table.

1. `node scripts/doctor.mjs --json`. Missing Firebase config blocks running the app, not the tests.
2. `./gradlew :app:testDebugUnitTest :app:assembleDebug`
3. `cd firebase-tests && npm ci && npm test`. All rules tests must pass; if `firestore.rules` changed, a matching test must exist.
4. `cd functions && npm ci && npm test`
5. If `keystore.properties` exists: `./gradlew :app:bundleRelease` (report AAB path and size; never read the keystore files).

## Layout

```
app/src/main/java/com/example/      (namespace is com.example; applicationId is com.rsetsparks.app)
  MainActivity.kt                   splash, session routing, notification deep links
  RSETSparksApplication.kt          Firebase + App Check init, notification channels
  data/model/                       plain data classes (no Room)
  data/repository/AppRepository.kt  ALL Firestore/Storage access
  data/repository/FirestoreMappers.kt  map <-> model conversion (unit-tested)
  security/AuthService.kt           Firebase Auth: Google (hd=rajagiri.edu.in) + email/password
  notifications/                    FCM service, channels, token registration
  ui/viewmodel/RSETSparksViewModel.kt  SessionState machine + all UI actions
  ui/screens/, ui/components/, ui/theme/
app/src/debug|release/.../AppCheckInstaller.kt   debug vs Play Integrity provider
functions/            Cloud Functions (Node 22, asia-south1): push on new match / message
firestore.rules, storage.rules, firestore.indexes.json   server-side security (source of truth)
firebase-tests/       emulator tests for the rules
docs/                 privacy policy, terms, account deletion (deployed to Firebase Hosting)
guides/               FIREBASE_SETUP.md, RELEASE_ANDROID.md
store/                Play listing text, Data safety answers, graphics
scripts/              setup-firebase.mjs, doctor.mjs, generate_brand_assets.py
```

## Rules that must stay true

- **Security lives in `firestore.rules` / `storage.rules`**, not in the app. Any data-model change needs a rules change
  plus a test in `firebase-tests/rules.test.js`, and `npm test` there must pass before deploying.
- Every account needs a verified `@rajagiri.edu.in` email; everyone is **18+** (`UserProfile.validate` and the rules both enforce it).
- Chats are **not end-to-end encrypted**. Never claim E2E anywhere (UI, listing, policy).
- Other students must never see someone's email. It lives only in `privateProfiles/{uid}`.
- New data collected → update `docs/privacy-policy.html` **and** `store/data-safety.md`.
- Never commit or print `keystore.properties`, `*.jks`, or passwords. `app/google-services.json` and `.firebaserc` are fine to commit.
- Moderators (`admins/{uid}`) are created only in the Firebase console, never from the app.
- `rsetsparks.blaze` in `gradle.properties` (→ `BuildConfig.BLAZE`) says whether the Firebase project is on Blaze.
  When false, anything needing Cloud Storage or Cloud Functions (photos, push) must stay hidden in the app and skipped by `setup-firebase.mjs`.
- Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts` for every Play upload.

## Conventions

- Kotlin style matches the existing files: Compose screens take state + callbacks and the ViewModel owns logic.
  Repository functions are `suspend` or return `Flow`.
- Use `MaterialTheme.colorScheme.*` for text/tint colours so dark mode works. Brand constants are in `ui/theme/Color.kt`.
- Run unit tests plus the rules/functions tests relevant to a change before committing.

## Things only the human can do

Browser sign-ins (`firebase login`, Google), Firebase console clicks (Blaze billing, enabling sign-in providers,
Storage "Get started", App Check, creating `admins/{uid}`), Play Console, and anything that needs their passwords.
Interactive scripts such as `node scripts/setup-firebase.mjs` need a real terminal. Ask the user to run them
with the `!` prefix in Claude Code (`! node scripts/setup-firebase.mjs`) or in their own terminal.
