package com.example.data.ios

import com.example.data.AppBackend
import com.example.data.model.AuditLogEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ReportEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreMappers
import com.example.data.repository.FirestoreMappers.blockId
import com.example.data.repository.FirestoreMappers.pairId
import com.example.data.repository.FirestoreMappers.swipeId
import com.example.util.formatDayTime
import com.example.util.formatLogTime
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.File
import dev.gitlive.firebase.storage.FirebaseStorage
import dev.gitlive.firebase.storage.FirebaseStorageMetadata
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import platform.Foundation.NSURL

/**
 * iOS port of the Android AppRepository (app/.../data/repository/AppRepository.kt), using the GitLive
 * wrapper over the Firebase iOS SDK. Keep the two in step: same collections, fields, queries and limits.
 * Access control is enforced by firestore.rules / storage.rules, not here.
 */
class IosFirestoreBackend(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val storage: FirebaseStorage = Firebase.storage
) : AppBackend {

    private companion object {
        const val DISCOVERY_LIMIT = 200
        const val TRANSCRIPT_MESSAGES = 30
    }

    private val users get() = db.collection("users")
    private val privateProfiles get() = db.collection("privateProfiles")
    private val swipes get() = db.collection("swipes")
    private val matches get() = db.collection("matches")
    private val blocks get() = db.collection("blocks")
    private val reports get() = db.collection("reports")
    private val auditLogs get() = db.collection("auditLogs")

    // ---------------------------------------------------------------- profile

    override suspend fun isAdmin(uid: String): Boolean =
        runCatching { db.collection("admins").document(uid).get().exists }.getOrDefault(false)

    override fun observeOwnProfile(uid: String, email: String, isAdmin: Boolean): Flow<UserEntity?> =
        users.document(uid).snapshots
            // Offline, the local cache can't tell "no profile yet" from "not downloaded yet"; wait for the server.
            .filter { it.exists || !it.metadata.isFromCache }
            .map { snap -> if (snap.exists) FirestoreMappers.userFromMap(uid, snap.raw(), email, isAdmin) else null }

    override suspend fun createProfile(uid: String, email: String, profile: UserProfile, isAdmin: Boolean): Result<Unit> = runCatching {
        val photoUrl = resolvePhoto(uid, profile.photoUri)
        val data = FirestoreMappers.userToMap(
            id = uid,
            profile = profile,
            photoUrl = photoUrl,
            colorIndex = (0..6).random(),
            visible = !isAdmin // Moderators start hidden from the discovery deck
        ) + mapOf("createdAt" to FieldValue.serverTimestamp, "updatedAt" to FieldValue.serverTimestamp)
        privateProfiles.document(uid).set(mapOf("email" to email, "createdAt" to FieldValue.serverTimestamp), merge = true)
        users.document(uid).set(data)
    }

    override suspend fun updateProfile(uid: String, profile: UserProfile): Result<Unit> = runCatching {
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
                "updatedAt" to FieldValue.serverTimestamp
            ) + FirestoreMappers.profileDetailsToMap(profile)
        )
    }

    /** Uploads a newly picked photo (file:// URL), keeps an already-uploaded one, or deletes it when removed. */
    private suspend fun resolvePhoto(uid: String, photoUri: String?): String? {
        val ref = storage.reference.child("profilePhotos/$uid/avatar.jpg")
        return when {
            photoUri == null -> {
                runCatching { ref.delete() }
                null
            }
            photoUri.startsWith("https://") -> photoUri
            else -> {
                val url = NSURL.URLWithString(photoUri) ?: error("Couldn't read the selected photo.")
                ref.putFile(File(url), FirebaseStorageMetadata(contentType = "image/jpeg"))
                ref.getDownloadUrl()
            }
        }
    }

    override suspend fun getUser(id: String): UserEntity? = runCatching {
        val snap = users.document(id).get()
        if (snap.exists) FirestoreMappers.userFromMap(id, snap.raw()) else null
    }.getOrNull()

    override suspend fun updateVisibility(uid: String, visible: Boolean) {
        users.document(uid).update(mapOf("visible" to visible, "updatedAt" to FieldValue.serverTimestamp))
    }

    override suspend fun updateShowBadges(uid: String, showBadges: Boolean) {
        users.document(uid).update(mapOf("showBadges" to showBadges, "updatedAt" to FieldValue.serverTimestamp))
    }

    // ---------------------------------------------------------------- push notifications (APNs not wired yet)

    override suspend fun registerPushToken(uid: String, email: String, token: String) {
        privateProfiles.document(uid).set(mapOf("email" to email, "fcmTokens" to FieldValue.arrayUnion(token)), merge = true)
    }

    override suspend fun unregisterPushToken(uid: String, email: String, token: String) {
        privateProfiles.document(uid).set(mapOf("email" to email, "fcmTokens" to FieldValue.arrayRemove(token)), merge = true)
    }

    override fun observePushEnabled(uid: String): Flow<Boolean> =
        privateProfiles.document(uid).snapshots
            .map { snap -> (snap.raw()["pushEnabled"] as? Boolean) ?: ((snap.raw()["pushEnabled"] as? Number)?.toInt() != 0) }
            .catch { emit(true) }

    override suspend fun setPushEnabled(uid: String, email: String, enabled: Boolean) {
        privateProfiles.document(uid).set(mapOf("email" to email, "pushEnabled" to enabled), merge = true)
    }

    // ---------------------------------------------------------------- discovery & swipes

    override fun discoveryCandidates(uid: String): Flow<List<UserEntity>> {
        val roster = users.where { all("visible" equalTo true, "suspended" equalTo false) }
            .limit(DISCOVERY_LIMIT).listen { id, data -> FirestoreMappers.userFromMap(id, data) }
        val swiped = swipes.where { "from" equalTo uid }.listen { _, data -> data["to"] as? String }
        return combine(roster, swiped, blockedUserIds(uid)) { people, swipedIds, blockedIds ->
            val hidden = swipedIds.filterNotNull().toSet() + blockedIds
            people.filter { it.id != uid && it.id !in hidden }
        }
    }

    override suspend fun recordSwipe(uid: String, targetId: String, action: String): Boolean {
        swipes.document(swipeId(uid, targetId)).set(
            mapOf("from" to uid, "to" to targetId, "action" to action, "createdAt" to FieldValue.serverTimestamp)
        )
        if (action != "LIKE" && action != "SUPER_SPARK") return false

        val theirs = runCatching { swipes.document(swipeId(targetId, uid)).get() }.getOrNull()
        val likedBack = theirs?.takeIf { it.exists }?.raw()?.get("action").let { it == "LIKE" || it == "SUPER_SPARK" }
        if (!likedBack) return false

        val match = matches.document(pairId(uid, targetId))
        val created = runCatching {
            match.set(
                mapOf(
                    "users" to listOf(uid, targetId).sorted(),
                    "createdAt" to FieldValue.serverTimestamp,
                    "lastMessageAt" to FieldValue.serverTimestamp
                )
            )
        }
        // If both students liked at the same moment the other device may have created it first.
        return created.isSuccess || runCatching { match.get().exists }.getOrDefault(false)
    }

    override suspend fun resetPasses(uid: String) {
        val passes = swipes.where { all("from" equalTo uid, "action" equalTo "PASS") }.get().documents
        passes.chunked(400).forEach { chunk ->
            db.batch().apply { chunk.forEach { delete(it.reference) } }.commit()
        }
    }

    // ---------------------------------------------------------------- matches & chat

    override fun matchedUsers(uid: String): Flow<List<UserEntity>> {
        val partnerIds = matches.where { "users" contains uid }
            .orderBy("lastMessageAt", Direction.DESCENDING)
            .listen { _, data -> (data["users"] as? List<*>)?.firstOrNull { it != uid } as? String }
        return combine(partnerIds, blockedUserIds(uid)) { ids, blocked -> ids.filterNotNull().filter { it !in blocked } }
            .map { ids -> ids.mapNotNull { getUser(it) }.filter { !it.isSuspended } }
    }

    override fun messages(uid: String, partnerId: String): Flow<List<ChatMessage>> =
        matches.document(pairId(uid, partnerId)).collection("messages")
            .orderBy("createdAt", Direction.ASCENDING)
            .limitToLast(500)
            .snapshots
            .map { snap ->
                snap.documents.map {
                    FirestoreMappers.messageFromMap(it.id, it.raw(), it.metadata.hasPendingWrites, ::timestampMillis)
                }
            }
            // Most likely the match was removed (unmatch/block); show an empty conversation.
            .catch { emit(emptyList()) }

    override suspend fun sendMessage(uid: String, partnerId: String, text: String) {
        val match = matches.document(pairId(uid, partnerId))
        val trimmed = text.trim().take(2000)
        db.batch()
            .set(match.collection("messages").document, mapOf("senderId" to uid, "text" to trimmed, "createdAt" to FieldValue.serverTimestamp))
            .update(match, mapOf("lastMessage" to trimmed.take(120), "lastMessageAt" to FieldValue.serverTimestamp, "lastSenderId" to uid))
            .commit()
    }

    override suspend fun unmatch(uid: String, partnerId: String) {
        matches.document(pairId(uid, partnerId)).delete()
    }

    // ---------------------------------------------------------------- safety

    private fun blockedUserIds(uid: String): Flow<Set<String>> {
        val mine = blocks.where { "blocker" equalTo uid }.listen { _, data -> data["blocked"] as? String }
        val theirs = blocks.where { "blocked" equalTo uid }.listen { _, data -> data["blocker"] as? String }
        return combine(mine, theirs) { a, b -> (a + b).filterNotNull().toSet() }
    }

    override suspend fun blockUser(uid: String, targetId: String) {
        blocks.document(blockId(uid, targetId)).set(
            mapOf("blocker" to uid, "blocked" to targetId, "createdAt" to FieldValue.serverTimestamp)
        )
        runCatching { unmatch(uid, targetId) }
    }

    override suspend fun reportUser(reporter: UserEntity, target: UserEntity, reason: String, description: String): Result<Unit> = runCatching {
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
                "createdAt" to FieldValue.serverTimestamp
            )
        )
    }

    private suspend fun buildTranscript(reporter: UserEntity, target: UserEntity): String {
        val recent = runCatching {
            matches.document(pairId(reporter.id, target.id)).collection("messages")
                .orderBy("createdAt", Direction.DESCENDING).limit(TRANSCRIPT_MESSAGES).get()
                .documents.map { FirestoreMappers.messageFromMap(it.id, it.raw(), false, ::timestampMillis) }.reversed()
        }.getOrDefault(emptyList())
        if (recent.isEmpty()) return "[No chat history between these students. Profile content reported.]"
        return recent.joinToString("\n") { msg ->
            val who = if (msg.senderId == reporter.id) reporter.name else target.name
            "[${formatDayTime(msg.timestamp)}] $who: ${msg.text}"
        }
    }

    override suspend fun deleteAllUserData(uid: String) {
        runCatching { storage.reference.child("profilePhotos/$uid/avatar.jpg").delete() }

        val myMatches = matches.where { "users" contains uid }.get().documents
        for (match in myMatches) {
            val mine = match.reference.collection("messages").where { "senderId" equalTo uid }.get().documents
            mine.chunked(400).forEach { chunk -> db.batch().apply { chunk.forEach { delete(it.reference) } }.commit() }
            match.reference.delete()
        }
        val refs = swipes.where { "from" equalTo uid }.get().documents.map { it.reference } +
            blocks.where { "blocker" equalTo uid }.get().documents.map { it.reference }
        refs.chunked(400).forEach { chunk -> db.batch().apply { chunk.forEach { delete(it) } }.commit() }

        privateProfiles.document(uid).delete()
        users.document(uid).delete()
    }

    // ---------------------------------------------------------------- admin

    override fun allReports(): Flow<List<ReportEntity>> =
        reports.orderBy("createdAt", Direction.DESCENDING).limit(500)
            .listen { id, data -> FirestoreMappers.reportFromMap(id, data, ::timestampMillis) }

    override fun allUsersWithEmails(): Flow<List<UserEntity>> {
        val people = users.listen { id, data -> FirestoreMappers.userFromMap(id, data) }
        val emails = privateProfiles.listen { id, data -> id to (data["email"] as? String ?: "") }
        return combine(people, emails) { list, emailPairs ->
            val byId = emailPairs.toMap()
            list.map { it.copy(email = byId[it.id] ?: "") }.sortedBy { it.name.lowercase() }
        }
    }

    override fun auditTrail(): Flow<List<AuditLogEntity>> =
        auditLogs.orderBy("createdAt", Direction.DESCENDING).limit(500)
            .listen { id, data -> FirestoreMappers.auditLogFromMap(id, data, ::timestampMillis) }

    private suspend fun audit(admin: UserEntity, action: String, targetId: String, targetEmail: String, reason: String) {
        auditLogs.add(
            mapOf(
                "adminUid" to admin.id,
                "adminEmail" to admin.email,
                "action" to action,
                "targetUserId" to targetId,
                "targetUserEmail" to targetEmail,
                "reason" to reason,
                "createdAt" to FieldValue.serverTimestamp
            )
        )
    }

    override suspend fun adminResolveReport(admin: UserEntity, report: ReportEntity, status: String, note: String, suspend: Boolean) {
        reports.document(report.id).update(
            mapOf("status" to status, "adminNote" to note, "resolvedAt" to FieldValue.serverTimestamp, "resolvedBy" to admin.id)
        )
        if (suspend) {
            setSuspended(report.reportedUserId, true)
            audit(admin, "SUSPENDED_USER", report.reportedUserId, report.reportedEmail, "Incident ${report.id}: $note")
        } else {
            audit(admin, "RESOLVED_INCIDENT", report.reportedUserId, report.reportedEmail, "Status $status: $note")
        }
    }

    private suspend fun setSuspended(uid: String, suspended: Boolean) {
        users.document(uid).update(mapOf("suspended" to suspended, "updatedAt" to FieldValue.serverTimestamp))
    }

    override suspend fun adminToggleSuspension(admin: UserEntity, target: UserEntity, suspended: Boolean, reason: String) {
        setSuspended(target.id, suspended)
        audit(admin, if (suspended) "SUSPENDED_USER" else "REINSTATED_USER", target.id, target.email, reason)
    }

    override suspend fun adminToggleVerification(admin: UserEntity, target: UserEntity, verified: Boolean) {
        users.document(target.id).update(mapOf("verified" to verified, "updatedAt" to FieldValue.serverTimestamp))
        audit(admin, if (verified) "VERIFIED_STUDENT" else "REVOKED_VERIFICATION", target.id, target.email, "Manual verification change")
    }

    override suspend fun adminEmergencyAudit(admin: UserEntity, target: UserEntity, justification: String): List<String> {
        audit(admin, "EMERGENCY_CHAT_AUDIT_ACCESSED", target.id, target.email, "Justification: $justification")
        return matches.where { "users" contains target.id }.get().documents.flatMap { match ->
            match.reference.collection("messages").orderBy("createdAt").get().documents.map {
                val msg = FirestoreMappers.messageFromMap(it.id, it.raw(), false, ::timestampMillis)
                val who = if (msg.senderId == target.id) target.name else "match ${match.id.replace(target.id, "").trim('_').take(6)}"
                "[${formatLogTime(msg.timestamp)}] $who: ${msg.text}"
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Live query results; a failing listener (e.g. permission denied) yields an empty list, as on Android. */
    private fun <T> Query.listen(mapper: (String, Map<String, Any?>) -> T): Flow<List<T>> =
        snapshots.map { snap -> snap.documents.map { mapper(it.id, it.raw()) } }.catch { emit(emptyList()) }
}
