# Publishing RSETSparks on Google Play

Work through this list top to bottom. Items marked **(you)** need your accounts or decisions; everything else is already in the repo.

## 0. One-time accounts (you)

- [ ] **Google Play Console developer account**: <https://play.google.com/console/signup>, one-time US$25.
  New *personal* accounts must run a **closed test with at least 12 testers for 14 continuous days** before they can apply for production access.
  Recruit 12+ classmates early.
- [ ] Identity verification for the developer account (government ID).
- [ ] Firebase project set up as described in [FIREBASE_SETUP.md](FIREBASE_SETUP.md).
- [ ] Enable **GitHub Pages** for this repo: Settings → Pages → *Deploy from a branch* → `main` / `/docs`.
  That publishes the privacy policy, terms and account-deletion pages at `https://mishalxrset.github.io/RSETSparks/`.
  If you use a different URL, update `app/src/main/res/values/strings.xml`.
  The repo must be public for free Pages; otherwise host the three `docs/*.html` files anywhere.
- [ ] Decide the public support email. It is currently your student email in `strings.xml` and `docs/*.html`.

## 1. Create the upload key (you, once — keep it forever)

```bash
keytool -genkeypair -v -keystore my-upload-key.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Back up `my-upload-key.jks` and its password somewhere safe (a password manager). It is git-ignored.
Play App Signing holds the real app-signing key, so a lost upload key can be reset, but that takes days.

Create `keystore.properties` in the project root (also git-ignored):

```properties
storeFile=my-upload-key.jks
storePassword=YOUR_PASSWORD
keyAlias=upload
keyPassword=YOUR_PASSWORD
```

For CI, add the same values as repository secrets. See the comments in `.github/workflows/android.yml`.

## 2. Build the release bundle

```bash
./gradlew :app:bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

Before every upload, bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`.

Smoke-test the exact release build on a real phone first:

```bash
./gradlew :app:installRelease
```

Check sign-up, verification email, onboarding with a photo, swiping between two accounts, matching, chat, report, block, unmatch and account deletion.

## 3. Play Console: create the app

*Create app* → name **RSETSparks** → App → Free → accept the declarations.

### Store listing (Grow → Store presence → Main store listing)

Text is in [`store/listing.md`](../store/listing.md). Graphics are in `store/graphics/`:

| Asset | File |
|---|---|
| App icon 512×512 | `store/graphics/play-icon-512.png` |
| Feature graphic 1024×500 | `store/graphics/play-feature-graphic-1024x500.png` |
| Phone screenshots (2–8) | **(you)** take them on a phone from the release build with real-looking test profiles. Don't use real students' photos without consent. |

Category: **Dating**. Contact email: your support email. Privacy policy URL: `https://mishalxrset.github.io/RSETSparks/privacy-policy.html`

### App content (Policy → App content)

| Section | Answer |
|---|---|
| Privacy policy | URL above |
| App access | *All or some functionality is restricted* → give the reviewer email + password from FIREBASE_SETUP §7 |
| Ads | No ads |
| Content rating | Fill in the IARC questionnaire honestly: category *Social/Communication*, users interact and exchange messages, users can share photos, dating app. Expect **18+ / Mature** |
| Target audience | **18 and over only**. Not designed for children |
| News app | No |
| Data safety | Use the answers in [`store/data-safety.md`](../store/data-safety.md) |
| Government app | No |
| Financial features | None |
| Health | None |
| Account deletion | In-app path: *Profile → Delete Account*. Web link: `https://mishalxrset.github.io/RSETSparks/delete-account.html` |

### Testing tracks

1. **Internal testing**: upload the AAB, add yourself, install from the Play link, run the smoke test again.
   Then add the Play App Signing SHA-1/SHA-256 to Firebase (FIREBASE_SETUP §2), otherwise Google Sign-In breaks in Play builds.
2. **Closed testing**: add 12+ testers (a Google Group is easiest) and keep it running for 14 days.
3. **Production**: apply for access after the closed test, then roll out (start at 20%).

## 4. After launch

- Check the moderation queue (Campus Safety Console) at least daily. The Terms promise action within 24 hours.
- Delete reports older than 12 months, as promised in the privacy policy.
- Turn on App Check enforcement once metrics look healthy (FIREBASE_SETUP §6).
- Watch *Android vitals* in Play Console for crashes and ANRs.

## Known limitations / next steps

- **No push notifications yet.** Students only see new matches and messages with the app open.
  Adding them needs Firebase Cloud Messaging plus a Cloud Function (Blaze is already on), which is a good v1.1 feature.
- **No profile editing after onboarding** apart from the visibility switch. Also a v1.1 candidate.
- Chats are protected by security rules, TLS and Google's at-rest encryption, but they are **not end-to-end encrypted**. Never claim E2E in the listing.
