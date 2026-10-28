package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "swipes")
data class SwipeMatchEntity(
    @PrimaryKey val id: String,
    val fromUserId: String,
    val toUserId: String,
    val action: String, // "LIKE", "PASS", "SUPER_SPARK"
    val isMutualMatch: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
