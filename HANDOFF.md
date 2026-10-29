# Handoff: from cloud session to local Claude Code

**Written:** 2026-10-28, at the point the work was merged to `main` (PR #1).
**For:** Claude Code running on the owner's own computer (probably Windows), plus the owner.
**Read with:** [CLAUDE.md](CLAUDE.md) for commands, layout and the rules that must stay true.

## Where things stand

Done and verified in the cloud (no device, no real Firebase project):

- [x] App moved from device-local Room to Firebase (Auth, Firestore, Storage, FCM). Builds clean.
- [x] Brand: crest icon (adaptive + themed), splash, Material 3 light/dark theme, store graphics.
- [x] Store requirements: 18+ gate, email verification, report/block/unmatch, in-app account deletion,
      privacy policy / terms / deletion pages in `docs/`, honest copy (no E2E claims).
- [x] v1.1: push notifications (Cloud Functions + FCM) and profile editing. `versionName 1.1.0`, `versionCode 2`.
- [x] Release build: `com.rsetsparks.app`, R8, signing via `keystore.properties`, App Check.
- [x] Tests: 6 app unit tests, 21 security-rules tests, 5 function tests. CI in `.github/workflows/android.yml`.

**Never tested** (impossible without the owner's accounts and a device), so this is the main job now:

- [ ] A real Firebase project (none exists yet)
- [ ] The app on a real phone or emulator against that project
- [ ] Google Sign-In, the verification email, photo upload, matching between two accounts, chat, push notifications,
      account deletion

## The plan: work through in order, ticking boxes as you go

Legend: 🤖 = Claude Code can do it alone · 👤 = needs the owner (browser, console, passwords, phone)

### 1. Machine check
- [x] 🤖 `node scripts/doctor.mjs` and fix anything marked ✖ that's in your power (PATH, JAVA_HOME, `local.properties`).
      Common Windows fixes: JAVA_HOME → `C:\Program Files\Android\Android Studio\jbr`; add `%JAVA_HOME%\bin` and
      `%LOCALAPPDATA%\Android\Sdk\platform-tools` to PATH (tell the owner; changing PATH needs a new terminal).
- [x] 🤖 `./gradlew :app:testDebugUnitTest :app:assembleDebug`. This also creates `~/.android/debug.keystore`.
- [x] 🤖 `cd firebase-tests && npm ci && npm test` and `cd functions && npm ci && npm test`.

### 2. Upload key (do before Firebase so its fingerprint gets registered)
- [ ] 👤 The owner creates it and chooses the password. Give them this command; don't generate or see the password yourself:
      `keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
- [ ] 👤 Owner creates `keystore.properties` (format in `guides/RELEASE_ANDROID.md` §1) and backs up the `.jks` and password.
- [ ] 🤖 Confirm with `node scripts/doctor.mjs` (the "Upload key" line), without opening the file.

### 3. Firebase project
> **Free (Spark) mode is on:** `rsetsparks.blaze=false` in `gradle.properties`. The app hides profile photos and
> push notifications, and the setup script skips Blaze billing, the Storage bucket and Cloud Functions. To turn
> them on later: upgrade the project to Blaze (set a budget alert), set `rsetsparks.blaze=true`, re-run the script.
- [x] 👤 Owner runs `! node scripts/setup-firebase.mjs` (interactive: browser login, then console steps for Blaze billing,
      sign-in providers and the Storage bucket). It creates the project, registers the app with fingerprints, creates Firestore
      (asia-south1), writes `app/google-services.json` and `.firebaserc`, deploys rules, indexes, functions and the
      legal pages (Firebase Hosting), and rewrites the URLs in `strings.xml`.
- [x] 🤖 `node scripts/doctor.mjs`: `google-services.json`, `.firebaserc` and the legal URLs should all be ✔.
- [x] 🤖 Open `https://<project-id>.web.app/privacy-policy.html` (curl it) to confirm the policy is live.
- [x] 🤖 Commit `app/google-services.json`, `.firebaserc`, `app/src/main/res/values/strings.xml` on a branch and open a PR.

### 4. First run on a device
- [x] 👤 Start an emulator with Google Play (Android Studio → Device Manager) or plug in a phone (USB debugging on).
- [x] 🤖 `adb devices`, then `./gradlew :app:installDebug`, then `adb shell am start -n com.rsetsparks.app/com.example.MainActivity`.
- [x] 🤖 Find the App Check debug token: `adb logcat -d | grep -i "debug secret"`. Give it to the owner to add under
      Firebase console → App Check → Apps → ⋮ → Manage debug tokens. Not needed until App Check is *enforced*,
      but harmless to do now.
- [x] 👤 Owner signs in with their Rajagiri Google account and completes onboarding.
- [x] 👤 Make the owner a moderator: Firestore → collection `admins` → doc ID = their UID (Authentication → Users) →
      field `role` = `moderator`. Restart the app; the Campus Safety Console tab appears.

### 5. End-to-end test (two accounts, ideally two devices or an emulator + phone)
Run through this and record pass/fail in this file. For each failure, read `adb logcat`, fix it, add a test where possible, and open a PR.
- [x] Email/password sign-up → verification email arrives → app unlocks automatically
- [x] Google sign-in with a non-Rajagiri account is refused
- [x] Onboarding: under 18 is refused; photo from camera and from gallery both upload (photos N/A on Spark)
- [x] A and B like each other → both see "It's a Spark!" and get a push if the app is backgrounded (push N/A on Spark)
- [x] Chat both ways in real time; with the app closed, a message push arrives and tapping it opens the chat (push N/A on Spark)
- [x] Edit profile (change photo, bio) → the other account sees the change
- [ ] Profile → Notifications off → no more pushes (N/A on Spark; retest on Blaze)
- [x] Report (with "also block") → the report appears in the moderator console; the blocked users vanish for each other
- [x] Unmatch closes the chat for both
- [x] Delete account (sign in again first if asked) → data gone in Firestore, auth user gone
- [x] Dark mode and a small screen look right

Useful while testing: `npx firebase-tools@14 functions:log` (push problems), Firestore console (data),
`adb logcat -s AppRepository AuthService FirebaseMessaging`.

> If any Firestore listener fails with `FAILED_PRECONDITION … requires an index`, the indexes are still building
> (a few minutes after the first deploy) or weren't deployed: `npx firebase-tools@14 deploy --only firestore:indexes`.

### 6. Toward Play Store (`guides/RELEASE_ANDROID.md`)
- [ ] 👤 Play Console developer account (US$25 + ID verification).
- [ ] 👤 Reviewer test account on `@rajagiri.edu.in` (ask college IT), set up with email + password and a match.
- [ ] 🤖 `./gradlew :app:bundleRelease` and check the output (`apksigner`/`jarsigner` verify).
- [ ] 👤 Upload to Internal testing → then 🤖 add the **Play App Signing** SHA-1/SHA-256 to Firebase
      (`npx firebase-tools@14 apps:android:sha:create <appId> <sha>`); without this, Google Sign-In fails in Play builds.
- [ ] 👤 Screenshots (4–8) from the release build using test profiles.
- [ ] 👤 Closed test with 12+ testers for 14 days → production. Answers for the forms are in `store/`.
- [ ] 👤 App Check: once the metrics look healthy, enforce for Firestore, Storage and Auth.

## Optional: fewer permission prompts (owner decides)

To let Claude Code run the routine build and test commands without asking each time, the owner can create
`.claude/settings.json` with the content below. It also blocks reading the signing secrets. Review it before adding.

```json
{
  "permissions": {
    "allow": [
      "Bash(node scripts/doctor.mjs:*)",
      "Bash(./gradlew :app:testDebugUnitTest:*)",
      "Bash(./gradlew :app:assembleDebug:*)",
      "Bash(./gradlew :app:installDebug:*)",
      "Bash(./gradlew :app:bundleRelease:*)",
      "Bash(npm ci:*)",
      "Bash(npm test:*)",
      "Bash(adb devices:*)",
      "Bash(adb logcat:*)"
    ],
    "deny": [
      "Read(./keystore.properties)",
      "Read(./**/*.jks)"
    ]
  }
}
```

## Open decisions for the owner

- Public **support email**: currently their student email in `strings.xml` and `docs/*.html`.
- **Approval from Rajagiri** for using the name. The app says it's an independent student app; written approval is safer.
- **iOS**: Compose Multiplatform (share this code) or a separate SwiftUI app. Needs a Mac and an Apple Developer account (US$99/yr).

## Known limitations

- Delete-account requires a sign-in within the last ~4 minutes (Firebase rule); the app tells the user.
- Messages a deleted user *received* remain in the partner's match subcollection, but the match doc is removed so
  nobody but admins can read them (stated in the privacy policy).
- Reports older than 12 months must be deleted manually (promised in the privacy policy). Consider a scheduled function later.

## Handoff log

Append a line per session: date, what was done, what's next.

- 2026-10-28 (cloud): built everything above; merged PR #1 to `main`. Next: §1–§3 on the owner's machine.
- 2026-10-28 (local, Windows): §1 done. Wrote `local.properties` (SDK at `%LOCALAPPDATA%\Android\Sdk`; use forward
  slashes, backslashes get eaten). Gradle auto-installed SDK Platform 36.1 + Build-Tools 36. Unit tests 7/7, debug APK
  builds, rules tests 21/21, function tests 5/5. `adb` not on PATH yet. Next: §2 upload key (owner), §3 Firebase.
- 2026-10-28 (local): §3 done on the free Spark plan. Project `rsetsparks01` (owner account alteracc39210@gmail.com),
  Firestore asia-south1, rules + indexes + legal pages deployed (https://rsetsparks01.web.app). New projects need the
  Firestore API enabled first (the script's 403 links to it). Debug fingerprints registered; re-run the script after §2
  to add the upload key's. Next: §2 upload key, §4 first run on a device (add platform-tools to PATH).
- 2026-10-28 (local): §4 done on a Redmi Note 8 (Android 11) over wireless debugging (`adb pair`, then the main-screen
  IP:port). Owner signed in, onboarded, is a moderator; the admin tab shows. Seen: admin console header fills the
  screen and pushes the stat cards off the bottom; app content looks dimmed in screenshots (MIUI force-dark?).
  Next: §5 with a second Rajagiri account.
- 2026-10-29 (local): UI fixes verified on the phone: admin header layout, moderator wording instead of
  "officer"/"official", collapsing admin banner, double bottom inset under the nav bar. Debug APK shared with a friend
  for §5. Branch `free-spark-plan` (not pushed yet). Next: §5 with two accounts, then push + PR; §2 upload key.
- 2026-10-29 (local): added gender (required), "Show me" (required, mutual deck filter), optional orientation,
  looking for, pronouns; existing accounts are asked once. Rules + privacy page deployed, fast-forwarded to `main`.
  §5 passed on two phones (Spark plan: photo upload and push checks N/A, retest them if the project moves to Blaze).
  Next: §2 upload key, then §6.
