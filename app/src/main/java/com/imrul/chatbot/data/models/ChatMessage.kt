package com.imrul.chatbot.data.models

import java.util.UUID

enum class MessageType {
    USER,
    BOT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis(),
    val eventData: EventExtractionResponse? = null,
    val isEventMessage: Boolean = false
)
