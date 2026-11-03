# RSETSparks on iOS

The iOS app is the **same Kotlin/Compose code** as Android (`shared/`), wrapped in a thin SwiftUI shell (`iosApp/`).
Screens, the ViewModel and validation are shared. Each platform has its own Firebase backend behind the
`AuthBackend` / `AppBackend` interfaces (`shared/src/commonMain/kotlin/com/example/data/Backends.kt`):

| | Android | iOS |
|---|---|---|
| Firebase | official Android SDK, `app/.../AppRepository.kt` + `AuthService.kt` | GitLive Kotlin wrapper over the Firebase iOS SDK 11.8.0, `shared/src/iosMain/.../data/ios/` |
| Google Sign-In | Credential Manager (`AndroidGoogleSignIn.kt`) | GoogleSignIn SDK (`iosApp/iosApp/GoogleSignInBridge.swift`) |
| Photo picker | camera intent + Android photo picker | UIImagePickerController + PHPicker (`PhotoPicker.ios.kt`) |
| Push | FCM + Cloud Functions | not yet: needs APNs, which needs an Apple Developer membership |

When you change Firestore reads or writes, change **both** backends the same way; they are written to mirror each other.

## What you need

- **A Mac with Apple silicon** (M1 or newer). Compose Multiplatform no longer builds for Intel simulators.
- **Xcode 16 or newer** from the App Store. Open it once to install the iOS Simulator.
- **A JDK 17+** for Gradle. `brew install --cask temurin@21`, or point `JAVA_HOME` at Android Studio's bundled JDK.
- **XcodeGen**: `brew install xcodegen`
- **Node 20+** for the Firebase setup script: `brew install node`

No Apple Developer membership is needed to run on the **Simulator**.

## One-time setup

```bash
git clone https://github.com/MishalxRSET/RSETSparks.git && cd RSETSparks
node scripts/doctor.mjs           # shows what's missing, including the iOS items on a Mac
node scripts/setup-firebase.mjs   # re-run it; it now also registers the iOS app
```

The setup script is safe to re-run on the existing `rsetsparks01` project. It:

- registers the iOS app `com.rsetsparks.app` with Firebase;
- writes `iosApp/iosApp/GoogleService-Info.plist`;
- puts the Google Sign-In URL scheme (`REVERSED_CLIENT_ID`) into `iosApp/Configuration/Config.xcconfig`.

Commit both files.

## Run it

```bash
cd iosApp
xcodegen                     # generates RSETSparks.xcodeproj from project.yml
open RSETSparks.xcodeproj
```

In Xcode, pick an iPhone simulator and press **▶ Run**.

- The first build takes a few minutes: Xcode downloads the Firebase and GoogleSignIn packages, and the
  "Compile Kotlin framework" build phase runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`.
- After editing Kotlin, just press ▶ again; the build phase recompiles it.
- If Xcode says it can't find module `Shared`, build once more. The framework is produced by that first build phase.

Without `GoogleService-Info.plist` the app still launches, but shows the "Firebase isn't configured" screen.

## Test checklist (Simulator)

Same flows as HANDOFF §5, on the Spark plan:

- [ ] Email/password sign-up, then the verification email, then the app unlocks.
- [ ] Google sign-in with a Rajagiri account. A non-Rajagiri account is refused.
- [ ] Onboarding (gender, "Show me", 18+), then the deck shows the Android test accounts and they see you.
- [ ] Like each other → "It's a Spark!" → chat both ways with an Android phone.
- [ ] Edit profile, report + block, unmatch, then delete account.
- [ ] Dark mode (Simulator: ⌘⇧A) and the iOS edge-swipe back from a chat.

Photos and push are hidden while `rsetsparks.blaze=false` (gradle.properties), exactly as on Android.

## Running on your own iPhone and TestFlight (later)

Needs the **Apple Developer Program** (US$99/year).

1. Enrol at <https://developer.apple.com/programs/> as an individual. Approval takes 1–2 days.
2. Put your Team ID in `iosApp/Configuration/Config.xcconfig` (`DEVELOPMENT_TEAM = ABCDE12345`), then run `xcodegen`.
3. Plug in the iPhone, select it in Xcode, and Run. Xcode manages the signing certificates.
4. TestFlight: **Product → Archive → Distribute App → App Store Connect**. Create the app record first in
   App Store Connect with bundle ID `com.rsetsparks.app`.

### Push notifications on iOS (needs the membership)

1. Apple Developer → Keys → create an **APNs key** and upload it in Firebase console → Project settings →
   Cloud Messaging → Apple app configuration.
2. Add the **Push Notifications** capability. That's an `entitlements:` entry in `project.yml`.
3. Add the FirebaseMessaging package product, register for remote notifications in `iOSApp.swift`, and pass the
   FCM token to Kotlin through a `PushRegistrar` implementation, as Android's `AndroidPushRegistrar` does.
4. The Cloud Functions already send to every token in `privateProfiles/{uid}.fcmTokens`. They need an `apns`
   section in the payload (`functions/notify.js`) so iOS shows the alert.

## App Store review notes

- **Age rating:** answer the questionnaire as a dating app, which gives **18+**. In the description, state that
  the app is for Rajagiri students aged 18+.
- **Sign in with Apple (guideline 4.8)** isn't required: the app only accepts accounts from an educational
  institution (Rajagiri), which is an explicit exception. Say so in the review notes.
- **User-generated content (guideline 1.2):** report, block, moderation and the terms are all in place. Mention
  the moderator console and the 24-hour response promise.
- **Account deletion (5.1.1(v)):** Profile → Delete Account.
- **App Privacy labels** (App Store Connect): the same data as `store/data-safety.md` and
  `iosApp/iosApp/PrivacyInfo.xcprivacy`. That's name, email, user ID, photos, sensitive info (orientation) and
  other user content (messages), all linked to the user, used for app functionality, and not used for tracking.
- **Reviewer account:** the same Rajagiri test account as Google Play (FIREBASE_SETUP §7).
