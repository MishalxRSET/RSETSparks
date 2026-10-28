package com.example.data.repository

import android.net.Uri
import android.util.Log
import com.example.data.model.AuditLogEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ReportEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreMappers.blockId
import com.example.data.repository.FirestoreMappers.pairId
import com.example.data.repository.FirestoreMappers.swipeId
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * All RSETSparks data lives in Cloud Firestore (with its built-in offline cache) and profile photos in
 * Cloud Storage. Access control is enforced server-side by firestore.rules and storage.rules; the
 * checks here only exist to give friendly errors.
 */
class AppRepository(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val storage: FirebaseStorage = Firebase.storage
) {
    companion object {
        private const val TAG = "AppRepository"
        private const val DISCOVERY_LIMIT = 200L
        private const val TRANSCRIPT_MESSAGES = 30L
    }

    private val users get() = db.collection("users")
    private val privateProfiles get() = db.collection("privateProfiles")
    private val swipes get() = db.collection("swipes")
    private val matches get() = db.collection("matches")
    private val blocks get() = db.collection("blocks")
    private val reports get() = db.collection("reports")
    private val auditLogs get() = db.collection("auditLogs")

    // ---------------------------------------------------------------- profile

    suspend fun isAdmin(uid: String): Boolean =
        runCatching { db.collection("admins").document(uid).get().await().exists() }.getOrDefault(false)

    /** Streams the signed-in user's own profile; emits null until onboarding has created it. */
    fun observeOwnProfile(uid: String, email: String, isAdmin: Boolean): Flow<UserEntity?> = callbackFlow {
        val reg = users.document(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "Profile listener failed", error)
                close(error)
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            // Offline, the local cache can't tell "no profile yet" from "not downloaded yet"; wait for the server.
            if (!snap.exists() && snap.metadata.isFromCache) return@addSnapshotListener
            val data = snap.data
            trySend(if (snap.exists() && data != null) FirestoreMappers.userFromMap(uid, data, email, isAdmin) else null)
        }
        awaitClose { reg.remove() }
    }

    suspend fun createProfile(uid: String, email: String, profile: UserProfile, isAdmin: Boolean): Result<Unit> = runCatching {
        val photoUrl = resolvePhoto(uid, profile.photoUri)
        val data = FirestoreMappers.userToMap(
            id = uid,
            profile = profile,
            photoUrl = photoUrl,
            colorIndex = (0..6).random(),
            visible = !isAdmin // Moderators start hidden from the discovery deck
        ) + mapOf("createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp())
        privateProfiles.document(uid)
            .set(mapOf("email" to email, "createdAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        users.document(uid).set(data).await()
        Unit
    }

    /** Edits the editable profile fields; moderation fields are untouched (and protected by the rules). */
    suspend fun updateProfile(uid: String, profile: UserProfile): Result<Unit> = runCatching {
        val photoUrl = resolvePhoto(uid, profile.photoUri)
        users.document(uid).update(
            mapOf(
                "name" to profile.name.trim(),
                "age" to profile.age.toLong(),
                "department" to profile.department,
                "year" to profile.year.toLong(),
                "bio" to profile.bio.trim(),
                "interests" to profile.interests,
                "photoUrl" to (photoUrl ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    /** Uploads a newly picked photo, keeps an already-uploaded one, or deletes the photo when removed. */
    private suspend fun resolvePhoto(uid: String, photoUri: String?): String? = when {
        photoUri == null -> {
            runCatching { storage.reference.child("profilePhotos/$uid/avatar.jpg").delete().await() }
            null
        }
        photoUri.startsWith("https://") -> photoUri
        else -> uploadProfilePhoto(uid, Uri.parse(photoUri))
    }

    private suspend fun uploadProfilePhoto(uid: String, localUri: Uri): String {
        val ref = storage.reference.child("profilePhotos/$uid/avatar.jpg")
        ref.putFile(localUri, com.google.firebase.storage.StorageMetadata.Builder().setContentType("image/jpeg").build()).await()
        return ref.downloadUrl.await().toString()
    }

    // ---------------------------------------------------------------- push notifications

    /** Stores this device's FCM token so Cloud Functions can notify the student. */
    suspend fun registerPushToken(uid: String, email: String, token: String) {
        privateProfiles.document(uid)
            .set(mapOf("email" to email, "fcmTokens" to FieldValue.arrayUnion(token)), SetOptions.merge()).await()
    }

    suspend fun unregisterPushToken(uid: String, email: String, token: String) {
        privateProfiles.document(uid)
            .set(mapOf("email" to email, "fcmTokens" to FieldValue.arrayRemove(token)), SetOptions.merge()).await()
    }

    fun observePushEnabled(uid: String): Flow<Boolean> = callbackFlow {
        val reg = privateProfiles.document(uid).addSnapshotListener { snap, _ ->
            trySend(snap?.getBoolean("pushEnabled") ?: true)
        }
        awaitClose { reg.remove() }
    }

    suspend fun setPushEnabled(uid: String, email: String, enabled: Boolean) {
        privateProfiles.document(uid).set(mapOf("email" to email, "pushEnabled" to enabled), SetOptions.merge()).await()
    }

    suspend fun getUser(id: String): UserEntity? = fetchUser(id)

    suspend fun updateVisibility(uid: String, visible: Boolean) {
        users.document(uid).update(mapOf("visible" to visible, "updatedAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun updateShowBadges(uid: String, showBadges: Boolean) {
        users.document(uid).update(mapOf("showBadges" to showBadges, "updatedAt" to FieldValue.serverTimestamp())).await()
    }

    // ---------------------------------------------------------------- discovery & swipes

    /**
     * Visible, non-suspended students the user hasn't swiped on or blocked (in either direction).
     * Campus-scale: the whole visible roster fits in one query.
     */
    fun discoveryCandidates(uid: String): Flow<List<UserEntity>> {
        val roster = users.whereEqualTo("visible", true).whereEqualTo("suspended", false)
            .limit(DISCOVERY_LIMIT).snapshots { id, data -> FirestoreMappers.userFromMap(id, data) }
        val swiped = swipes.whereEqualTo("from", uid).snapshots { _, data -> data["to"] as? String }
        return combine(roster, swiped, blockedUserIds(uid)) { people, swipedIds, blockedIds ->
            val hidden = swipedIds.filterNotNull().toSet() + blockedIds
            people.filter { it.id != uid && it.id !in hidden }
        }
    }

    /** Records a swipe; for likes, creates the match when the other student already liked back. */
    suspend fun recordSwipe(uid: String, targetId: String, action: String): Boolean {
        swipes.document(swipeId(uid, targetId)).set(
            mapOf("from" to uid, "to" to targetId, "action" to action, "createdAt" to FieldValue.serverTimestamp())
        ).await()
        if (action != "LIKE" && action != "SUPER_SPARK") return false

        val theirs = runCatching { swipes.document(swipeId(targetId, uid)).get().await() }.getOrNull()
        val likedBack = theirs?.getString("action").let { it == "LIKE" || it == "SUPER_SPARK" }
        if (!likedBack) return false

        val match = matches.document(pairId(uid, targetId))
        val created = runCatching {
            match.set(
                mapOf(
                    "users" to listOf(uid, targetId).sorted(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastMessageAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        // If both students liked at the same moment the other device may have created it first.
        return created.isSuccess || runCatching { match.get().await().exists() }.getOrDefault(false)
    }

    /** Brings back everyone the user passed on. Likes are kept so pending matches still work. */
    suspend fun resetPasses(uid: String) {
        val passes = swipes.whereEqualTo("from", uid).whereEqualTo("action", "PASS").get().await()
        passes.documents.chunked(400).forEach { chunk ->
            db.runBatch { batch -> chunk.forEach { batch.delete(it.reference) } }.await()
        }
    }

    // ---------------------------------------------------------------- matches & chat

    fun matchedUsers(uid: String): Flow<List<UserEntity>> {
        val partnerIds = matches.whereArrayContains("users", uid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .snapshots { _, data -> (data["users"] as? List<*>)?.firstOrNull { it != uid } as? String }
        return combine(partnerIds, blockedUserIds(uid)) { ids, blocked -> ids.filterNotNull().filter { it !in blocked } }
            .map { ids -> ids.mapNotNull { fetchUser(it) }.filter { !it.isSuspended } }
    }

    private suspend fun fetchUser(id: String): UserEntity? = runCatching {
        val snap = users.document(id).get().await()
        snap.data?.let { FirestoreMappers.userFromMap(id, it) }
    }.getOrNull()

    fun messages(uid: String, partnerId: String): Flow<List<ChatMessage>> = callbackFlow {
        val reg = matches.document(pairId(uid, partnerId)).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(500)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    // Most likely the match was removed (unmatch/block); show an empty conversation.
                    Log.w(TAG, "Chat listener stopped", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.documents.orEmpty().map {
                    FirestoreMappers.messageFromMap(it.id, it.data.orEmpty(), it.metadata.hasPendingWrites())
                })
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendMessage(uid: String, partnerId: String, text: String) {
        val match = matches.document(pairId(uid, partnerId))
        val trimmed = text.trim().take(2000)
        db.runBatch { batch ->
            batch.set(
                match.collection("messages").document(),
                mapOf("senderId" to uid, "text" to trimmed, "createdAt" to FieldValue.serverTimestamp())
            )
            batch.update(
                match,
                mapOf("lastMessage" to trimmed.take(120), "lastMessageAt" to FieldValue.serverTimestamp(), "lastSenderId" to uid)
            )
        }.await()
    }

    suspend fun unmatch(uid: String, partnerId: String) {
        matches.document(pairId(uid, partnerId)).delete().await()
    }

    // ---------------------------------------------------------------- safety

    private fun blockedUserIds(uid: String): Flow<Set<String>> {
        val mine = blocks.whereEqualTo("blocker", uid).snapshots { _, data -> data["blocked"] as? String }
        val theirs = blocks.whereEqualTo("blocked", uid).snapshots { _, data -> data["blocker"] as? String }
        return combine(mine, theirs) { a, b -> (a + b).filterNotNull().toSet() }
    }

    /** Blocks a student: hides them both ways and ends any match. */
    suspend fun blockUser(uid: String, targetId: String) {
        blocks.document(blockId(uid, targetId)).set(
            mapOf("blocker" to uid, "blocked" to targetId, "createdAt" to FieldValue.serverTimestamp())
        ).await()
        runCatching { unmatch(uid, targetId) }
    }

    suspend fun reportUser(
        reporter: UserEntity,
        target: UserEntity,
        reason: String,
        description: String
    ): Result<Unit> = runCatching {
        reports.add(
            mapOf(
                "reporterId" to reporter.id,
                "reporterEmail" to reporter.email,
                "reportedUserId" to target.id,
                "reportedUserName" to target.name,
                "reason" to reason,
                "description" to description.trim().take(1000),
                "transcript" to buildTranscript(reporter, target),
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    private suspend fun buildTranscript(reporter: UserEntity, target: UserEntity): String {
        val recent = runCatching {
            matches.document(pairId(reporter.id, target.id)).collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(TRANSCRIPT_MESSAGES).get().await()
                .documents.map { FirestoreMappers.messageFromMap(it.id, it.data.orEmpty(), false) }.reversed()
        }.getOrDefault(emptyList())
        if (recent.isEmpty()) return "[No chat history between these students. Profile content reported.]"
        val fmt = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
        return recent.joinToString("\n") { msg ->
            val who = if (msg.senderId == reporter.id) reporter.name else target.name
            "[${fmt.format(Date(msg.timestamp))}] $who: ${msg.text}"
        }
    }

    /**
     * Removes the student's data (photo, swipes, blocks, matches, own messages, profile).
     * The Firebase Auth account itself is deleted afterwards by the caller via AuthService.
     */
    suspend fun deleteAllUserData(uid: String) {
        runCatching { storage.reference.child("profilePhotos/$uid/avatar.jpg").delete().await() }

        val myMatches = matches.whereArrayContains("users", uid).get().await().documents
        for (match in myMatches) {
            val mine = match.reference.collection("messages").whereEqualTo("senderId", uid).get().await().documents
            mine.chunked(400).forEach { chunk -> db.runBatch { b -> chunk.forEach { b.delete(it.reference) } }.await() }
            match.reference.delete().await()
        }
        val refs = swipes.whereEqualTo("from", uid).get().await().documents.map { it.reference } +
            blocks.whereEqualTo("blocker", uid).get().await().documents.map { it.reference }
        refs.chunked(400).forEach { chunk -> db.runBatch { b -> chunk.forEach { b.delete(it) } }.await() }

        privateProfiles.document(uid).delete().await()
        users.document(uid).delete().await()
    }

    // ---------------------------------------------------------------- admin

    fun allReports(): Flow<List<ReportEntity>> =
        reports.orderBy("createdAt", Query.Direction.DESCENDING).limit(500)
            .snapshots { id, data -> FirestoreMappers.reportFromMap(id, data) }

    fun allUsersWithEmails(): Flow<List<UserEntity>> {
        val people = users.snapshots { id, data -> FirestoreMappers.userFromMap(id, data) }
        val emails = privateProfiles.snapshots { id, data -> id to (data["email"] as? String ?: "") }
        return combine(people, emails) { list, emailPairs ->
            val byId = emailPairs.toMap()
            list.map { it.copy(email = byId[it.id] ?: "") }.sortedBy { it.name.lowercase() }
        }
    }

    fun auditTrail(): Flow<List<AuditLogEntity>> =
        auditLogs.orderBy("createdAt", Query.Direction.DESCENDING).limit(500)
            .snapshots { id, data -> FirestoreMappers.auditLogFromMap(id, data) }

    private suspend fun audit(admin: UserEntity, action: String, targetId: String, targetEmail: String, reason: String) {
        auditLogs.add(
            mapOf(
                "adminUid" to admin.id,
                "adminEmail" to admin.email,
                "action" to action,
                "targetUserId" to targetId,
                "targetUserEmail" to targetEmail,
                "reason" to reason,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun adminResolveReport(admin: UserEntity, report: ReportEntity, status: String, note: String, suspend: Boolean) {
        reports.document(report.id).update(
            mapOf("status" to status, "adminNote" to note, "resolvedAt" to FieldValue.serverTimestamp(), "resolvedBy" to admin.id)
        ).await()
        if (suspend) {
            setSuspended(report.reportedUserId, true)
            audit(admin, "SUSPENDED_USER", report.reportedUserId, report.reportedEmail, "Incident ${report.id}: $note")
        } else {
            audit(admin, "RESOLVED_INCIDENT", report.reportedUserId, report.reportedEmail, "Status $status: $note")
        }
    }

    private suspend fun setSuspended(uid: String, suspended: Boolean) {
        users.document(uid).update(mapOf("suspended" to suspended, "updatedAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun adminToggleSuspension(admin: UserEntity, target: UserEntity, suspended: Boolean, reason: String) {
        setSuspended(target.id, suspended)
        audit(admin, if (suspended) "SUSPENDED_USER" else "REINSTATED_USER", target.id, target.email, reason)
    }

    suspend fun adminToggleVerification(admin: UserEntity, target: UserEntity, verified: Boolean) {
        users.document(target.id).update(mapOf("verified" to verified, "updatedAt" to FieldValue.serverTimestamp())).await()
        audit(admin, if (verified) "VERIFIED_STUDENT" else "REVOKED_VERIFICATION", target.id, target.email, "Manual verification change")
    }

    /** Reads every conversation of a student for an emergency investigation; always audit-logged first. */
    suspend fun adminEmergencyAudit(admin: UserEntity, target: UserEntity, justification: String): List<String> {
        audit(admin, "EMERGENCY_CHAT_AUDIT_ACCESSED", target.id, target.email, "Justification: $justification")
        val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        return matches.whereArrayContains("users", target.id).get().await().documents.flatMap { match ->
            match.reference.collection("messages").orderBy("createdAt").get().await().documents.map {
                val msg = FirestoreMappers.messageFromMap(it.id, it.data.orEmpty(), false)
                val who = if (msg.senderId == target.id) target.name else "match ${match.id.replace(target.id, "").trim('_').take(6)}"
                "[${fmt.format(Date(msg.timestamp))}] $who: ${msg.text}"
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun <T> Query.snapshots(mapper: (String, Map<String, Any?>) -> T): Flow<List<T>> = callbackFlow {
        val reg = addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "Query listener failed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snap?.documents.orEmpty().map { mapper(it.id, it.data.orEmpty()) })
        }
        awaitClose { reg.remove() }
    }
}
