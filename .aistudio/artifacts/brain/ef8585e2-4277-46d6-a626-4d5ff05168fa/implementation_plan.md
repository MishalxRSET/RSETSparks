# Real-Time Chat, Live Email Verification, and Admin Badge Visibility Toggle

Deliver genuine real-time campus chat via Firestore snapshot listeners and Room caching, live Firebase Auth email link verification without OTP simulation, and set administrator Mohammed Mishal M's profile badges and titles behind an explicit "Show Badges" toggle (defaulting to OFF).

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed based on your specifications:
> - **Real-time Email Verification**: Firebase Auth email link (`sendEmailVerification()`) with live `user.reload()` verification checks. All OTP simulation code will be completely eradicated.
> - **Admin Identity & Badge Toggle**: The safety administrator profile name is set to **Mohammed Mishal M** (`u2504040@rajagiri.edu.in`). Badges and titles ("Campus Safety Officer") are strictly gated behind a `showBadges` boolean toggle (default **OFF**) accessible in both Profile Settings and the Admin Console.
> - **Non-Admin View Guard**: When any non-admin student views the admin profile (in Chat, Matches, Discovery, or Profile Preview), no badges or officer titles will be rendered unless `showBadges` is actively enabled by the admin.
> - **Real-Time Chat Engine**: Cloud Firestore collection snapshot listeners (`addSnapshotListener`) synchronizing bidirectional message streams into the existing Room database cache, with all automated mock replies eliminated.

---

### 1. Overview & Core Concept

- **What It Does**:
  1. **Authentic Real-Time Chat**: Replaces simulated responses with persistent Firestore snapshot listeners for bidirectional messaging between matched campus students, backed by Room for offline resilience.
  2. **Official Email Verification**: Sends real verification links via Firebase Authentication. Provides live verification reload status checks and a "Resend Email" option, removing all temporary OTP simulation code.
  3. **Admin Badge Privacy Control**: Sets the campus administrator's identity to **Mohammed Mishal M**. Hides the Campus Safety Officer badge, shield, and title by default until the admin explicitly switches on "Show Badges" in Profile Settings or the Admin Console.
- **Target Audience / Persona**:
  - **Campus Students (@rajagiri.edu.in)**: Experience secure, authentic verification and real-time messaging with peers. When chatting or viewing Mohammed Mishal M, they see a normal student profile unless the admin turns on official badges.
  - **Administrator (Mohammed Mishal M)**: Retains full administrative and safety moderation tools while controlling whether their official officer badge and title are visible to students.
- **Key Value**: 100% elimination of mock/simulation code; authentic Firebase-backed real-time flows; privacy-first administrative role representation.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  - **Email Verification Flow**:
    1. Student registers or logs in with their `@rajagiri.edu.in` email.
    2. If `user.isEmailVerified` is false, `EmailVerificationScreen` appears.
    3. UI displays a clean verification card with the student's email, an instruction to check their campus inbox, and a "Resend Verification Link" button with countdown feedback.
    4. A reactive polling loop and an interactive "Check Verification Status" button execute `auth.currentUser?.reload()`.
    5. As soon as `user.isEmailVerified` is true, the user is automatically transitioned into the main app.
  - **Admin Badge Toggle Flow**:
    1. Mohammed Mishal M logs in and opens **Profile Settings** or the **Admin Console**.
    2. A dedicated Material 3 switch card displays: `"Show Officer Badge & Title"` with subtitle `"Display Campus Safety Officer badge and title to students"`.
    3. Default state is **OFF**.
    4. When **OFF**: In Discovery cards, Match previews, Chat header, and Profile modal, Mohammed Mishal M appears with name "Mohammed Mishal M", college department, and bio—without the Campus Safety Officer badge, shield icon, or officer title.
    5. When **ON**: Non-admin students inspecting Mohammed Mishal M's profile see the golden verified shield, "Campus Safety Officer" badge, and administrative title.
  - **Real-Time Messaging Flow**:
    1. User opens a chat conversation.
    2. Firestore snapshot listener attaches to `/matches/{matchId}/messages`.
    3. Messages sync instantaneously between devices with message status indicators (`sent`, `delivered`, `read`).
    4. No fake automated responses are generated; messages are genuine peer-to-peer transmissions.

- **Visual Identity & Theme**:
  - **Material 3 Tokens**: Crimson primary (`#8B0000`), Dark Crimson (`#5A0000`), Gold verified accent (`#F59E0B`), and Subtle Surface containers.
  - **Badge Display States**:
    - *Admin Badges Hidden*: Clean minimalist avatar, student name, and standard department label.
    - *Admin Badges Shown*: Elevated amber border around avatar, shield icon badge `Icons.Default.VerifiedUser`, and a distinct "Campus Safety Officer" pill tag.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Live Polling + Manual Trigger for Email Verification**
  - *Chosen Approach*: Periodic `reload()` check every 4 seconds while `EmailVerificationScreen` is active, combined with a prominent "I've Verified My Email" button.
  - *Why*: Eliminates user frustration caused by waiting for token refresh or requiring app restarts after clicking the verification link in mobile browser/Gmail.
  - *Alternatives Considered*: Manual-only button (requires extra user clicks) or app restart requirement (poor UX).

- **Decision 2: Firestore Snapshot Listeners with Room as Single Source of Truth**
  - *Chosen Approach*: `AppRepository` observes Firestore `/matches/{matchId}/messages` snapshots, upserts documents into Room `MessageDao`, and UI observes Room Flow.
  - *Why*: Ensures instant UI responsiveness, full offline cache capability, smooth scrolling without UI jank, and seamless reconnection handling.

- **Decision 3: Dual-Location Admin Badge Switch (Profile & Admin Console)**
  - *Chosen Approach*: Place the "Show Badge" toggle in both `ProfileScreen` (Settings section) and `AdminScreen` (Safety Preferences card). Both read and write to the same `showBadges` state on the admin user document.
  - *Why*: Direct compliance with user clarification, offering immediate convenience regardless of which screen the admin is currently managing.

- **Decision 4: Complete Eradication of Simulation**
  - *Chosen Approach*: Remove all `generateMockResponse` coroutine delays and fake OTP code blocks across `AuthScreen`, `AppRepository`, and ViewModels.
  - *Why*: Direct user requirement to eliminate simulation and ensure genuine production behaviors.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Firebase Services Layer                         │
│  - FirebaseAuth: sendEmailVerification(), reload(), isEmailVerified   │
│  - Cloud Firestore: /users/{userId}, /matches/{matchId}/messages      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Live Snapshots & Auth State
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             AppRepository                              │
│  - syncMessages(matchId): Flow<List<MessageEntity>> via Firestore      │
│  - sendRealMessage(matchId, text): Task<Void> to Firestore             │
│  - updateAdminBadgeVisibility(showBadges: Boolean)                     │
│  - checkEmailVerificationLive(): Result<Boolean>                       │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │ Room Cache Upsert              │ UI StateFlow
                    ▼                                ▼
       ┌────────────────────────┐       ┌────────────────────────┐
       │     Room Local DB      │       │  RSETSparksViewModel   │
       │ (UserDao, MessageDao)  │       │ (StateFlow, Handlers)  │
       └────────────────────────┘       └───────────┬────────────┘
                                                    │
                 ┌──────────────────────────────────┼──────────────────────────────────┐
                 ▼                                  ▼                                  ▼
      ┌───────────────────────┐          ┌───────────────────────┐          ┌───────────────────────┐
      │  EmailVerifyScreen    │          │      ChatScreen       │          │ Profile & AdminScreen │
      │ - Real-time reload()  │          │ - Live Firestore msg  │          │ - "Show Badges" Switch│
      │ - No mock OTP         │          │ - No fake responses   │          │ - Non-admin gate view │
      └───────────────────────┘          └───────────────────────┘          └───────────────────────┘
```

- **Data Models & State Additions**:
  1. `UserProfile` & `UserEntity`:
     - Add `showBadges: Boolean = false` (defaults to `false`).
     - Admin default name updated to `"Mohammed Mishal M"`.
     - `getDisplayTitle(isViewerAdmin: Boolean)`: Returns `"Campus Safety Officer"` if and only if `showBadges == true` (or viewer is admin inspecting self/console).
     - `shouldShowBadge(isViewerAdmin: Boolean)`: Returns `true` only if `showBadges == true`.
  2. `ChatMessage` / `MessageEntity`:
     - Ensure Firestore timestamp, senderId, receiverId, and text fields map cleanly for real-time synchronization.

- **Component Implementation Map**:
  - `app/src/main/java/com/example/data/model/UserProfile.kt` & `UserEntity.kt`:
    - Add `showBadges` field, update admin constants and getter functions.
  - `app/src/main/java/com/example/security/FirebaseAuthService.kt`:
    - Add `sendEmailVerification()`, `reloadCurrentUser()`, and `isEmailVerified()`.
  - `app/src/main/java/com/example/data/repository/AppRepository.kt`:
    - Implement real-time Firestore message listeners and remove mock response generators.
    - Implement `setShowBadges(show: Boolean)` persisting to Firestore and Room.
  - `app/src/main/java/com/example/ui/screens/AuthScreen.kt`:
    - Remove OTP simulation dialog; introduce authentic `EmailVerificationScreen` with live reload status.
  - `app/src/main/java/com/example/ui/screens/ProfileScreen.kt` & `AdminScreen.kt`:
    - Add the "Show Officer Badge & Title" switch for Mohammed Mishal M.
    - Gate officer badge and title rendering based on `adminUser.showBadges`.
  - `app/src/main/java/com/example/ui/screens/ChatScreen.kt` & `MatchesScreen.kt`:
    - When rendering chat counterpart details or profile inspection dialogs for an admin user, conditionally display badge and title ONLY if `counterpart.showBadges == true`.
