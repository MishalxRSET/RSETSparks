package com.example.data.model

/** An append-only moderation record in `auditLogs/{id}`. */
data class AuditLogEntity(
    val id: String,
    val adminEmail: String,
    val action: String, // "SUSPENDED_USER", "REINSTATED_USER", "RESOLVED_INCIDENT", "EMERGENCY_CHAT_AUDIT_ACCESSED", ...
    val targetUserId: String,
    val targetUserEmail: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
