package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ReportEntity
import com.example.data.model.SwipeMatchEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserProfile
import com.example.security.CryptoHelper
import com.example.security.FirebaseAuthService
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class AppRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val userProfileDao = db.userProfileDao()
    private val swipeDao = db.swipeDao()
    private val chatDao = db.chatDao()
    private val reportDao = db.reportDao()
    private val auditDao = db.auditDao()

    private val firestore: FirebaseFirestore get() = Firebase.firestore
    private val activeChatListeners = mutableMapOf<String, ListenerRegistration>()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser = _currentUser.asStateFlow()

    companion object {
        const val ADMIN_EMAIL = "u2504040@rajagiri.edu.in"
    }

    private fun isTargetAdmin(email: String): Boolean {
        return email.trim().equals(ADMIN_EMAIL, ignoreCase = true)
    }

    suspend fun initialize() {
        // App starts unauthenticated; user signs in or registers with @rajagiri.edu.in email
    }

    suspend fun loginWithEmail(email: String): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        if (!isValidRajagiriEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Access restricted: Email must end with @rajagiri.edu.in"))
        }

        val user = userDao.getUserByEmail(cleanEmail)
        return if (user != null) {
            if (user.isSuspended) {
                Result.failure(IllegalStateException("This account has been suspended by Campus Safety."))
            } else {
                val shouldBeAdmin = isTargetAdmin(cleanEmail)
                val expectedAdminName = if (shouldBeAdmin && (user.name == "Officer Thomas Kurian" || user.name.isBlank())) "Mohammed Mishal M" else user.name
                val syncedUser = if (user.isAdmin != shouldBeAdmin || (shouldBeAdmin && user.name != expectedAdminName)) {
                    val updated = user.copy(
                        name = expectedAdminName,
                        isAdmin = shouldBeAdmin,
                        avatarEmoji = if (shouldBeAdmin && user.showBadges) "🛡️" else "🎓"
                    )
                    userDao.updateUser(updated)
                    updated
                } else {
                    user
                }
                _currentUser.value = syncedUser
                Result.success(syncedUser)
            }
        } else {
            Result.failure(IllegalArgumentException("No account registered with this email. Please register your student profile first."))
        }
    }

    suspend fun loginOrRegisterGoogleUser(email: String, displayName: String?): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        if (!isValidRajagiriEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Access restricted: Email must end with @rajagiri.edu.in"))
        }

        val existing = userDao.getUserByEmail(cleanEmail)
        if (existing != null) {
            if (existing.isSuspended) {
                return Result.failure(IllegalStateException("This account has been suspended by Campus Safety."))
            }
            val shouldBeAdmin = isTargetAdmin(cleanEmail)
            val expectedAdminName = if (shouldBeAdmin && (existing.name == "Officer Thomas Kurian" || existing.name.isBlank())) "Mohammed Mishal M" else existing.name
            val syncedUser = if (existing.isAdmin != shouldBeAdmin || (shouldBeAdmin && existing.name != expectedAdminName)) {
                val updated = existing.copy(
                    name = expectedAdminName,
                    isAdmin = shouldBeAdmin,
                    avatarEmoji = if (shouldBeAdmin && existing.showBadges) "🛡️" else "🎓"
                )
                userDao.updateUser(updated)
                updated
            } else {
                existing
            }
            _currentUser.value = syncedUser
            return Result.success(syncedUser)
        }

        val isOfficer = isTargetAdmin(cleanEmail)
        val name = if (isOfficer) "Mohammed Mishal M" else (displayName?.ifBlank { null } ?: cleanEmail.substringBefore("@").replace(".", " ").capitalizeWords())
        val newStudent = UserEntity(
            id = UUID.randomUUID().toString(),
            email = cleanEmail,
            name = name,
            age = 21,
            department = if (isOfficer) "Campus Safety Directorate" else "B.Tech Computer Science",
            year = 2026,
            bio = if (isOfficer) "Official Rajagiri Campus Moderation and Student Welfare Coordinator." else "Verified Rajagiri student. Excited to meet fellow campus peers!",
            interests = if (isOfficer) "Campus Safety, Ethics, Student Affairs" else "Campus Life, Tech, Music",
            avatarColorIndex = if (isOfficer) 8 else (0..6).random(),
            avatarEmoji = "🎓",
            isVerified = true,
            isAdmin = isOfficer,
            isVisibleInSparks = !isOfficer,
            showBadges = false
        )
        userDao.insertUser(newStudent)
        _currentUser.value = newStudent
        return Result.success(newStudent)
    }

    suspend fun registerStudent(
        email: String,
        name: String,
        age: Int,
        department: String,
        year: Int,
        bio: String,
        interests: String,
        avatarEmoji: String
    ): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        if (!isValidRajagiriEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Registration failed: Only @rajagiri.edu.in domains are permitted."))
        }

        val existing = userDao.getUserByEmail(cleanEmail)
        if (existing != null) {
            val shouldBeAdmin = isTargetAdmin(cleanEmail)
            val expectedAdminName = if (shouldBeAdmin && (existing.name == "Officer Thomas Kurian" || existing.name.isBlank())) "Mohammed Mishal M" else existing.name
            val syncedUser = if (existing.isAdmin != shouldBeAdmin || (shouldBeAdmin && existing.name != expectedAdminName)) {
                val updated = existing.copy(
                    name = expectedAdminName,
                    isAdmin = shouldBeAdmin,
                    avatarEmoji = if (shouldBeAdmin && existing.showBadges) "🛡️" else "🎓"
                )
                userDao.updateUser(updated)
                updated
            } else {
                existing
            }
            _currentUser.value = syncedUser
            return Result.success(syncedUser)
        }

        val isOfficer = isTargetAdmin(cleanEmail)
        val studentName = if (isOfficer) "Mohammed Mishal M" else name.ifBlank { "Rajagiri Student" }
        val newStudent = UserEntity(
            id = UUID.randomUUID().toString(),
            email = cleanEmail,
            name = studentName,
            age = if (age in 17..35) age else 21,
            department = department.ifBlank { "B.Tech Computer Science" },
            year = if (year in 2020..2030) year else 2026,
            bio = bio.ifBlank { "Excited to meet fellow Rajagiri students! 👋" },
            interests = interests.ifBlank { "Campus Life, Music, Coffee" },
            avatarColorIndex = if (isOfficer) 8 else (0..6).random(),
            avatarEmoji = avatarEmoji.ifBlank { "🎓" },
            isVerified = true,
            isAdmin = isOfficer,
            isVisibleInSparks = !isOfficer,
            showBadges = false
        )
        userDao.insertUser(newStudent)

        // Also cache UserProfile
        val profile = UserProfile(
            id = newStudent.id,
            email = cleanEmail,
            name = newStudent.name,
            bio = bio,
            interests = if (interests.isNotBlank()) interests.split(",").map { it.trim() } else emptyList(),
            department = department,
            year = year,
            age = age,
            isVerified = true
        )
        userProfileDao.insertProfile(profile)

        _currentUser.value = newStudent
        return Result.success(newStudent)
    }

    suspend fun registerStudentWithProfile(profile: UserProfile): Result<UserEntity> {
        val cleanEmail = profile.email.trim().lowercase()
        if (!isValidRajagiriEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Registration failed: Only @rajagiri.edu.in accounts are permitted."))
        }

        // Cache UserProfile locally in Room
        userProfileDao.insertProfile(profile)

        // Sync with Firestore
        try {
            FirebaseAuthService.getInstance(userProfileDao, userDao).saveAndSyncUserProfile(profile)
        } catch (_: Exception) {}

        // Insert or update UserEntity for app matching and messaging
        val userEntity = profile.toUserEntity()
        userDao.insertUser(userEntity)
        _currentUser.value = userEntity

        return Result.success(userEntity)
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return userProfileDao.getProfileById(userId)
    }

    fun getUserProfileFlow(userId: String): Flow<UserProfile?> {
        return userProfileDao.getProfileFlow(userId)
    }

    suspend fun syncAllPendingProfiles() {
        FirebaseAuthService.getInstance(userProfileDao, userDao).syncPendingProfiles()
    }

    fun logout() {
        _currentUser.value = null
    }

    fun isValidRajagiriEmail(email: String): Boolean {
        val trimmed = email.trim().lowercase()
        return trimmed.endsWith("@rajagiri.edu.in") && trimmed.length > "@rajagiri.edu.in".length
    }

    suspend fun updateSparksVisibility(userId: String, isVisible: Boolean) {
        userDao.setSparksVisibility(userId, isVisible)
        val updated = userDao.getUserById(userId)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated
        }
    }

    suspend fun updateAdminBadgeVisibility(userId: String, showBadges: Boolean) {
        userDao.setShowBadges(userId, showBadges)
        val updated = userDao.getUserById(userId)
        if (updated != null && _currentUser.value?.id == userId) {
            _currentUser.value = updated
        }
        try {
            val profile = userProfileDao.getProfileById(userId)
            if (profile != null) {
                val updatedProfile = profile.copy(showBadges = showBadges)
                userProfileDao.updateProfile(updatedProfile)
                FirebaseAuthService.getInstance(userProfileDao, userDao).saveAndSyncUserProfile(updatedProfile)
            }
        } catch (_: Exception) {}
    }

    // Discovery Deck flow
    fun getDiscoveryCandidatesFlow(currentUserId: String): Flow<List<UserEntity>> {
        return db.userDao().getAllActiveUsersFlow().map { allUsers ->
            val swipedIds = swipeDao.getSwipedUserIds(currentUserId).toSet()
            allUsers.filter { it.id != currentUserId && !it.isAdmin && !it.isSuspended && !swipedIds.contains(it.id) }
        }
    }

    suspend fun resetDiscoveryDeck(userId: String) {
        swipeDao.deleteSwipesForUser(userId)
    }

    suspend fun recordSwipe(
        fromUserId: String,
        targetUserId: String,
        action: String // "LIKE", "PASS", "SUPER_SPARK"
    ): Boolean {
        var isMutual = false
        if (action == "LIKE" || action == "SUPER_SPARK") {
            val existingOtherSwipe = swipeDao.getSwipe(targetUserId, fromUserId)
            if (existingOtherSwipe?.action == "LIKE" || existingOtherSwipe?.action == "SUPER_SPARK") {
                isMutual = true
                swipeDao.updateSwipe(existingOtherSwipe.copy(isMutualMatch = true))
            }
        }

        val swipe = SwipeMatchEntity(
            id = UUID.randomUUID().toString(),
            fromUserId = fromUserId,
            toUserId = targetUserId,
            action = action,
            isMutualMatch = isMutual
        )
        swipeDao.insertSwipe(swipe)
        return isMutual
    }

    fun getMutualMatchesUsersFlow(currentUserId: String): Flow<List<UserEntity>> {
        return swipeDao.getMutualMatchesFlow(currentUserId).map { matches ->
            val otherUserIds = matches.map { if (it.fromUserId == currentUserId) it.toUserId else it.fromUserId }.distinct()
            otherUserIds.mapNotNull { userDao.getUserById(it) }
        }
    }

    // Encrypted Chat flow with Cloud Firestore Real-time synchronization
    fun getConversationMessagesFlow(conversationId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForConversationFlow(conversationId)
    }

    fun attachRealtimeChatListener(conversationId: String, coroutineScope: CoroutineScope) {
        if (activeChatListeners.containsKey(conversationId)) return

        try {
            val registration = firestore.collection("chats")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.w("AppRepository", "Firestore chat listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val incoming = snapshot.documents.mapNotNull { doc ->
                            try {
                                ChatMessageEntity(
                                    id = doc.getString("id") ?: doc.id,
                                    conversationId = doc.getString("conversationId") ?: conversationId,
                                    senderId = doc.getString("senderId") ?: "",
                                    receiverId = doc.getString("receiverId") ?: "",
                                    encryptedContent = doc.getString("encryptedContent") ?: "",
                                    iv = doc.getString("iv") ?: "",
                                    plainSnippet = doc.getString("plainSnippet") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                    isRead = doc.getBoolean("isRead") ?: false
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (incoming.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                chatDao.insertMessages(incoming)
                            }
                        }
                    }
                }
            activeChatListeners[conversationId] = registration
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "Failed attaching Firestore listener: ${e.message}")
        }
    }

    fun detachRealtimeChatListener(conversationId: String) {
        activeChatListeners.remove(conversationId)?.remove()
    }

    suspend fun sendMessage(
        senderId: String,
        receiverId: String,
        plainText: String
    ): ChatMessageEntity {
        val convId = CryptoHelper.buildConversationId(senderId, receiverId)
        val encryption = CryptoHelper.encrypt(plainText, convId)

        val message = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = convId,
            senderId = senderId,
            receiverId = receiverId,
            encryptedContent = encryption.cipherTextBase64,
            iv = encryption.ivBase64,
            plainSnippet = plainText,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        // 1. Immediately cache in Room database
        chatDao.insertMessage(message)

        // 2. Publish to Cloud Firestore real-time collection for instant partner sync
        try {
            val firestoreMap = mapOf(
                "id" to message.id,
                "conversationId" to message.conversationId,
                "senderId" to message.senderId,
                "receiverId" to message.receiverId,
                "encryptedContent" to message.encryptedContent,
                "iv" to message.iv,
                "plainSnippet" to message.plainSnippet,
                "timestamp" to message.timestamp,
                "isRead" to message.isRead
            )
            firestore.collection("chats")
                .document(convId)
                .collection("messages")
                .document(message.id)
                .set(firestoreMap)
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "Error syncing message to Firestore: ${e.message}")
        }

        return message
    }

    // Report Safety Incident flow with encrypted transcript package
    suspend fun reportAccount(
        reporterId: String,
        reportedUserId: String,
        reasonCategory: String,
        description: String
    ): Result<ReportEntity> {
        val reporter = userDao.getUserById(reporterId) ?: return Result.failure(Exception("Reporter not found"))
        val reported = userDao.getUserById(reportedUserId) ?: return Result.failure(Exception("Reported user not found"))

        val convId = CryptoHelper.buildConversationId(reporterId, reportedUserId)
        val messages = chatDao.getMessagesForConversation(convId)

        // Build audit transcript snapshot
        val transcriptBuilder = StringBuilder()
        if (messages.isEmpty()) {
            transcriptBuilder.append("[No chat history recorded between these users. Profile content reported.]")
        } else {
            messages.takeLast(15).forEach { msg ->
                val senderLabel = if (msg.senderId == reporterId) reporter.email else reported.email
                val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(msg.timestamp)
                transcriptBuilder.append("[$timeStr] $senderLabel: ${msg.plainSnippet}\n")
            }
        }

        val report = ReportEntity(
            id = UUID.randomUUID().toString(),
            reporterId = reporterId,
            reportedUserId = reportedUserId,
            reporterEmail = reporter.email,
            reportedEmail = reported.email,
            reportedUserName = reported.name,
            reasonCategory = reasonCategory,
            description = description,
            status = "PENDING",
            chatTranscriptSnapshot = transcriptBuilder.toString(),
            createdAt = System.currentTimeMillis()
        )

        reportDao.insertReport(report)
        userDao.incrementWarning(reportedUserId)

        // Add audit trail entry
        auditDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                adminEmail = "SYSTEM_AUTOMATION",
                action = "INCIDENT_REPORT_SUBMITTED",
                targetUserId = reportedUserId,
                targetUserEmail = reported.email,
                reason = "Report category: $reasonCategory by ${reporter.email}"
            )
        )

        return Result.success(report)
    }

    // Admin Moderation flows
    fun getAllReportsFlow(): Flow<List<ReportEntity>> = reportDao.getAllReportsFlow()

    fun getAllUsersForAdminFlow(): Flow<List<UserEntity>> = userDao.getAllUsersForAdminFlow()

    fun getAllAuditLogsFlow(): Flow<List<AuditLogEntity>> = auditDao.getAllAuditLogsFlow()

    fun getPendingReportsCountFlow(): Flow<Int> = reportDao.getPendingReportsCountFlow()

    suspend fun adminResolveReport(
        reportId: String,
        newStatus: String,
        adminNote: String,
        adminEmail: String,
        suspendUser: Boolean = false
    ) {
        val report = reportDao.getReportById(reportId) ?: return
        reportDao.updateReportStatus(reportId, newStatus, adminNote)

        if (suspendUser) {
            userDao.setSuspension(report.reportedUserId, true)
            auditDao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    adminEmail = adminEmail,
                    action = "SUSPENDED_USER",
                    targetUserId = report.reportedUserId,
                    targetUserEmail = report.reportedEmail,
                    reason = "Action taken on incident #$reportId: $adminNote"
                )
            )
        } else {
            auditDao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    adminEmail = adminEmail,
                    action = "RESOLVED_INCIDENT",
                    targetUserId = report.reportedUserId,
                    targetUserEmail = report.reportedEmail,
                    reason = "Status updated to $newStatus: $adminNote"
                )
            )
        }
    }

    suspend fun adminToggleSuspension(userId: String, isSuspended: Boolean, adminEmail: String, reason: String) {
        val user = userDao.getUserById(userId) ?: return
        userDao.setSuspension(userId, isSuspended)
        auditDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                adminEmail = adminEmail,
                action = if (isSuspended) "SUSPENDED_USER" else "REINSTATED_USER",
                targetUserId = userId,
                targetUserEmail = user.email,
                reason = reason
            )
        )
    }

    suspend fun adminToggleVerification(userId: String, isVerified: Boolean, adminEmail: String) {
        val user = userDao.getUserById(userId) ?: return
        userDao.setVerification(userId, isVerified)
        auditDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                adminEmail = adminEmail,
                action = if (isVerified) "VERIFIED_STUDENT" else "REVOKED_VERIFICATION",
                targetUserId = userId,
                targetUserEmail = user.email,
                reason = "Manual verification toggle by campus safety officer"
            )
        )
    }

    suspend fun adminEmergencyAuditTranscripts(userId: String, adminEmail: String, justification: String): List<String> {
        val user = userDao.getUserById(userId) ?: return emptyList()
        val allMessages = chatDao.getAllMessagesForUserFlow(userId).first()
        auditDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                adminEmail = adminEmail,
                action = "EMERGENCY_CHAT_AUDIT_ACCESSED",
                targetUserId = userId,
                targetUserEmail = user.email,
                reason = "Justification: $justification"
            )
        )
        return allMessages.map { msg ->
            val time = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(msg.timestamp)
            "[$time] (${msg.senderId}): ${msg.plainSnippet}"
        }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
