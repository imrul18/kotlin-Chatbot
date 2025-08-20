package com.imrul.chatbot.data.models

/**
 * Represents a calendar event extracted from text.
 */
data class CalendarEvent(
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String? = null,
    val notes: String? = null,
    val reminder: String? = null
)

/**
 * Represents a response containing extracted calendar events.
 */
data class EventExtractionResponse(
    val events: List<CalendarEvent>,
    val eventCount: Int
)