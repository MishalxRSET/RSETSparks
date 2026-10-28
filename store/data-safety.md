# Google Play Data safety answers

These answers match what the code actually does (see `AppRepository.kt` and the privacy policy). If you add analytics, crash reporting or push notifications, update this file and the Play Console form.

## Overview questions

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (Firebase uses TLS) |
| Do you provide a way for users to request that their data is deleted? | **Yes**: in-app *Profile → Delete Account* and the web page `delete-account.html` |
| Shared with third parties? | **No**. Google Firebase acts as a *service provider* processing data on our behalf, which Play does not count as sharing |

## Data types collected

For every row: **Collected = Yes, Shared = No, Processed ephemerally = No**.

| Category → Type | Required or optional | Purposes |
|---|---|---|
| Personal info → **Name** | Required | App functionality, Account management |
| Personal info → **Email address** | Required | App functionality, Account management |
| Personal info → **User IDs** (Firebase UID) | Required | App functionality, Account management |
| Personal info → **Other info** (age, department, graduation year, bio, interests) | Required | App functionality |
| Photos and videos → **Photos** (profile photo) | Optional | App functionality |
| Messages → **Other in-app messages** (chats between matches) | Optional (only if the user chats) | App functionality, Fraud prevention / security / compliance (moderation of reported chats) |
| App activity → **Other user-generated content** (likes/passes, reports, blocks) | Required | App functionality, Fraud prevention / security / compliance |
| App info and performance → **Diagnostics**: *No* (no Crashlytics/analytics in v1) | — | — |
| Device or other IDs: *No* (App Check / Play Integrity tokens are not stored or linked to the user by the app) | — | — |

**Not collected:** location, contacts, financial info, health, web browsing, audio, files, calendar, advertising ID.
