# Firebase setup

RSETSparks stores everything in **your own** Firebase project: Authentication, Cloud Firestore, Cloud Storage,
and Cloud Functions plus Cloud Messaging for push notifications.
Do this once before the first Play Store build.

## Quick path: the setup script (about 15 minutes)

On your computer (Windows, macOS or Linux), with [Node.js 20+](https://nodejs.org) and Android Studio installed:

```bash
node scripts/setup-firebase.mjs
```

It signs you in to Firebase, creates the project and registers the Android app with your signing fingerprints.
It also creates Firestore in Mumbai, downloads `app/google-services.json`, and deploys the security rules, indexes and
push-notification functions. For the three things the CLI can't do (Blaze billing, sign-in providers, Storage bucket),
it opens the right console page and waits for you. Re-running it is safe.

After it finishes, do §5 (make yourself a moderator), §6 (App Check) and §7 (reviewer account) below.
The numbered sections are the same steps done by hand, if you prefer that or the script fails.

## 1. Create the project

1. Go to <https://console.firebase.google.com> → **Add project** → name it `rsetsparks`.
   Google Analytics is not used; you can turn it off.
2. **Upgrade to Blaze** (⚙ → Usage and billing → Modify plan). Cloud Storage (profile photos) and Cloud Functions (push notifications) need it.
   Campus-scale usage normally stays inside the free quota. Set a budget alert (for example ₹500/month) so there are no surprises.

## 2. Register the Android app

1. Project overview → **Add app → Android**.
2. Package name: `com.rsetsparks.app` (this can never change after the first Play upload).
3. Add SHA-1 and SHA-256 fingerprints. Google Sign-In fails without them. Add all three:
   - **Debug key** (your computer): `./gradlew :app:signingReport` → `debug` variant.
   - **Upload key**: `keytool -list -v -keystore my-upload-key.jks -alias upload`.
   - **Play App Signing key**: after the first upload, Play Console → *Test and release → App integrity → App signing*.
4. Download `google-services.json` and put it at `app/google-services.json`.
   It isn't secret, so it's fine to commit. Re-download it after adding fingerprints.

## 3. Authentication

Authentication → **Sign-in method**:

- Enable **Google**. Set the public-facing name to *RSETSparks* and the support email.
- Enable **Email/Password**. Leave "Email link (passwordless)" off.

Authentication → **Settings**:

- *User account linking*: keep the default **Link accounts that use the same email** (one account per email address).
- *Authorized domains*: the defaults are fine.

Authentication → **Templates**: edit the *Email address verification* and *Password reset* templates so the sender name reads **RSETSparks**.

> Rajagiri email runs on Google Workspace, so most students will use "Continue with Rajagiri Google account".
> The app passes `hd=rajagiri.edu.in` to the Google account picker and re-checks the domain after sign-in.
> The Firestore rules check it a third time on every request.

## 4. Firestore and Storage

1. Firestore Database → **Create database** → *Production mode* → location **asia-south1 (Mumbai)**.
2. Storage → **Get started** → same location.
3. Deploy the rules and indexes from this repo:

   ```bash
   npm install -g firebase-tools
   firebase login
   firebase use --add            # pick your project, alias "default"
   (cd functions && npm ci)
   firebase deploy --only firestore:rules,firestore:indexes,storage,functions
   ```

   The `functions` deploy sends push notifications for new matches and messages (`functions/index.js`, region asia-south1).
   The first deploy turns on the Cloud Functions, Cloud Build and Artifact Registry APIs for you.

   The rules are covered by tests. Run them locally before every deploy:

   ```bash
   cd firebase-tests && npm ci && npm test
   cd ../functions && npm ci && npm test
   ```

## 5. Make yourself a moderator

Admins are documents in the `admins` collection. Nobody can create one from the app.

1. Sign in to the app once with your account, so it gets a Firebase UID.
2. Authentication → Users → copy your **User UID**.
3. Firestore → **Start collection** `admins` → Document ID = that UID → add a field `role` (string) = `moderator`.
4. Restart the app. The *Campus Safety Console* appears in the bottom bar.

To remove a moderator, delete their document.

## 6. App Check (recommended before launch)

App Check makes sure only the genuine app from the Play Store can talk to your Firebase project.

1. App Check → Apps → your Android app → **Play Integrity** → enter the SHA-256 of the *Play App Signing* key.
2. Debug builds use the debug provider. On first launch Logcat prints
   `Enter this debug secret into the allow list in the Firebase Console: ...`.
   Add that token under App Check → Apps → ⋮ → *Manage debug tokens*.
3. Watch App Check → **Metrics** for a few days after launch. When almost all requests are verified, click **Enforce** for Firestore, Storage and Authentication.

## 7. Reviewer test account

Google and Apple reviewers can't get a Rajagiri email, so you have to give them one:

1. Ask the college IT office for a dedicated account (for example `rsetsparks.review@rajagiri.edu.in`).
   If that isn't possible, use one of your own spare accounts.
2. Sign up in the app with **email + password** (not Google, because reviewers can't pass Google's 2-step verification), verify it, and complete the profile.
3. Add a second test account that has liked the reviewer account, so the reviewer sees a match and can open a chat.
4. Put the email and password in Play Console → *App content → App access* (and later in App Store Connect → *App Review Information*).
