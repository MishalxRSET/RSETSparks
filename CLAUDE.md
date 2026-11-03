# RSETSparks: notes for Claude Code

Campus dating app for verified Rajagiri students (18+). Kotlin Multiplatform + Compose Multiplatform on Firebase:
one shared UI/ViewModel (`shared/`) for an Android app (`app/`) and an iOS app (`iosApp/`).
The owner is preparing the first Google Play release; the iOS app is at the Simulator stage ([guides/IOS.md](guides/IOS.md)).
**Start with [HANDOFF.md](HANDOFF.md)**: it has the current status and the ordered checklist of what to do next.

## First thing in every session

```bash
node scripts/doctor.mjs        # what's installed / configured / missing (add --json for machine-readable)
git status && git log --oneline -5
```

## Commands

| Task | Command |
|---|---|
| Unit tests (shared + Android) | `./gradlew :shared:testAndroidHostTest :app:testDebugUnitTest` |
| Type-check the iOS Kotlin (works on Linux/Windows too) | `./gradlew :shared:compileKotlinIosSimulatorArm64` |
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
| iOS project (Mac only) | `cd iosApp && xcodegen && open RSETSparks.xcodeproj`, then ▶ in Xcode |
| iOS tests (Mac only) | `./gradlew :shared:iosSimulatorArm64Test` |

On Windows, use `gradlew.bat` if `./gradlew` doesn't work in the current shell.

## Verify (before every commit, deploy or release)

Run in order and keep going after a failure so the report is complete. Report a pass/fail table.

1. `node scripts/doctor.mjs --json`. Missing Firebase config blocks running the app, not the tests.
2. `./gradlew :shared:testAndroidHostTest :app:testDebugUnitTest :app:assembleDebug :shared:compileKotlinIosSimulatorArm64`
3. `cd firebase-tests && npm ci && npm test`. All rules tests must pass; if `firestore.rules` changed, a matching test must exist.
4. `cd functions && npm ci && npm test`
5. If `keystore.properties` exists: `./gradlew :app:bundleRelease` (report AAB path and size; never read the keystore files).

## Layout

Package/namespace is `com.example` everywhere; the store ID is `com.rsetsparks.app` on both platforms.

```
shared/src/commonMain/kotlin/com/example/    shared by Android and iOS (no android.* or platform.* imports)
  AppLinks.kt                       privacy/terms/deletion URLs + support email (setup-firebase.mjs rewrites them)
  data/Backends.kt                  AuthBackend, AppBackend, PushRegistrar interfaces + CampusEmail
  data/model/                       plain data classes; UserProfile.validate (18+, required fields)
  data/repository/FirestoreMappers.kt  map <-> model conversion (unit-tested)
  ui/App.kt                         RSETSparksApp(): session routing, tabs, theme (both hosts call this)
  ui/viewmodel/RSETSparksViewModel.kt  SessionState machine + all UI actions
  ui/screens/, ui/components/, ui/theme/
  platform/PhotoPicker.kt           expect/actual camera + gallery picker
shared/src/commonMain/composeResources/   in-app logo (generated)
shared/src/commonTest/              DomainLogicTest (runs on Android host and iOS simulator)
shared/src/androidMain/             Android actuals (photo picker)
shared/src/iosMain/                 iOS actuals: MainViewController.kt, data/ios/ (Firebase via GitLive), PhotoPicker.ios.kt
app/src/main/java/com/example/      Android host
  MainActivity.kt                   splash, wires backends into the ViewModel, notification deep links
  RSETSparksApplication.kt          Firebase + App Check init, notification channels
  data/repository/AppRepository.kt  AppBackend on the official Android Firebase SDK
  security/AuthService.kt, AndroidGoogleSignIn.kt   Firebase Auth + Credential Manager (hd=rajagiri.edu.in)
  notifications/                    FCM service, channels, AndroidPushRegistrar
app/src/debug|release/.../AppCheckInstaller.kt   debug vs Play Integrity provider
iosApp/               SwiftUI shell: project.yml (XcodeGen), Info.plist, PrivacyInfo.xcprivacy, GoogleSignInBridge.swift
functions/            Cloud Functions (Node 22, asia-south1): push on new match / message
firestore.rules, storage.rules, firestore.indexes.json   server-side security (source of truth)
firebase-tests/       emulator tests for the rules
docs/                 privacy policy, terms, account deletion (deployed to Firebase Hosting)
guides/               FIREBASE_SETUP.md, RELEASE_ANDROID.md, IOS.md
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
- **Two Firebase backends, one behaviour.** Android's `AppRepository`/`AuthService` and iOS's `IosFirestoreBackend`/
  `IosAuthBackend` must read and write the same collections and fields. Change both together and keep
  `FirestoreMappers` as the single place that turns maps into models.
- `rsetsparks.blaze` in `gradle.properties` (→ `BuildConfig.BLAZE` and shared `SharedBuildConfig.BLAZE`) says whether the Firebase project is on Blaze.
  When false, anything needing Cloud Storage or Cloud Functions (photos, push) must stay hidden in the app and skipped by `setup-firebase.mjs`.
- Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts` for every Play upload. For App Store builds the
  same pair lives in `iosApp/project.yml` (`CURRENT_PROJECT_VERSION`, `MARKETING_VERSION`).

## Conventions

- Kotlin style matches the existing files: Compose screens take state + callbacks and the ViewModel owns logic.
  Backend functions are `suspend` or return `Flow`.
- UI and logic go in `shared/commonMain`. Only code that needs a platform API goes in `androidMain`/`iosMain`/`app/`.
  Use kotlinx-datetime / `util/Time.kt` instead of `java.*`, and `Res.drawable.*` for images.
- Use `MaterialTheme.colorScheme.*` for text/tint colours so dark mode works. Brand constants are in `ui/theme/Color.kt`.
- Run unit tests plus the rules/functions tests relevant to a change before committing.

## Things only the human can do

Browser sign-ins (`firebase login`, Google), Firebase console clicks (Blaze billing, enabling sign-in providers,
Storage "Get started", App Check, creating `admins/{uid}`), Play Console, Apple Developer / App Store Connect,
Xcode on a Mac, and anything that needs their passwords.
Interactive scripts such as `node scripts/setup-firebase.mjs` need a real terminal. Ask the user to run them
with the `!` prefix in Claude Code (`! node scripts/setup-firebase.mjs`) or in their own terminal.
