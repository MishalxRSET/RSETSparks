package com.example.data.model

/** A message in `matches/{matchId}/messages`. */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val isPending: Boolean = false // Written locally but not yet acknowledged by the server
)
