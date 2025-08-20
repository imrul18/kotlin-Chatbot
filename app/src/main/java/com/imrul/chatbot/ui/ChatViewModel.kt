package com.imrul.chatbot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imrul.chatbot.data.models.ChatMessage
import com.imrul.chatbot.data.models.EventExtractionResponse
import com.imrul.chatbot.data.models.MessageType
import com.imrul.chatbot.data.repository.ChatRepository
import com.imrul.chatbot.data.service.EventExtractionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()
    private val eventExtractionService = EventExtractionService()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentBotMessage = MutableStateFlow("")
    val currentBotMessage: StateFlow<String> = _currentBotMessage.asStateFlow()

    private val _fullBotResponse = MutableStateFlow("")


    fun sendMessage(message: String) {
        if (message.isBlank()) return

        // Add user message to the list
        val userMessage = ChatMessage(
            content = message,
            type = MessageType.USER
        )
        _messages.update { it + userMessage }

        // Reset current bot message
        _currentBotMessage.value = ""

        // Set loading state
        _isLoading.value = true

        var today = java.time.LocalDate.now().toString()
        var prePrompt = "You are an event parser. From the given input, extract zero or more events. If there are no events, return nothing.\nIrrelevant, non-event-related, or insufficiently detailed input should also return nothing.\nIf there is one or more events, output them in the exact format below with sequential numbering starting at event-1.\nDo not add any extra text, explanations, or commentary.\n\nREFERENCE_DATE: $today\nTIMEZONE: Europe/Berlin\nOUTPUT FORMAT (exactly, no extra text):\nevent-1\ntitle: <title>\nstart: <YYYY-MM-DD HH:MM:SS>\nend: <YYYY-MM-DD HH:MM:SS>\nlocation: <location or empty>\nnotes: <notes or empty>\nevent-2\ntitle: <title>\nstart: <YYYY-MM-DD HH:MM:SS>\nend: <YYYY-MM-DD HH:MM:SS>\nlocation: <location or empty>\nnotes: <notes or empty>\n...\n\nInput: "

        viewModelScope.launch {
            try {
                // Reset full bot response
                _fullBotResponse.value = ""

                    // Use regular chat flow
                    repository.generateResponse(prePrompt + message).collect { response ->
                        // Append to full bot response
                        _fullBotResponse.update { it + response }

                        // Update current bot message for typing animation
                        _currentBotMessage.value = _fullBotResponse.value
                    }

                    // Check if the response contains event data in JSON format
                    val isEventMessage = isEventJson(_fullBotResponse.value)
                    var eventData: EventExtractionResponse? = null

                    if (isEventMessage) {
                        try {
                            eventData = parseEventJson(_fullBotResponse.value)
                        } catch (e: Exception) {
                            // If parsing fails, treat as a regular message
                        }
                    }

                    // Add complete bot message to the list
                    val botMessage = ChatMessage(
                        content = _fullBotResponse.value,
                        type = MessageType.BOT,
                        isEventMessage = isEventMessage,
                        eventData = eventData
                    )
                    _messages.update { it + botMessage }

                // Reset current bot message
                _currentBotMessage.value = ""
                _fullBotResponse.value = ""
            } catch (e: Exception) {
                // Handle error
                val errorMessage = ChatMessage(
                    content = "Error: ${e.message}",
                    type = MessageType.BOT
                )
                _messages.update { it + errorMessage }
            } finally {
                // Reset loading state
                _isLoading.value = false
            }
        }
    }

    /**
     * Determines if the message is likely requesting event extraction.
     */
    private fun shouldExtractEvents(message: String): Boolean {
        // Check if the message contains any of the event keywords
        val lowerMessage = message.lowercase()

        // Simple heuristic: if the message is long and contains date/time patterns, it's likely an event
        val containsDateTimePatterns = lowerMessage.contains(Regex("\\d+:\\d+|\\d+ (am|pm)|\\d+ (january|february|march|april|may|june|july|august|september|october|november|december)|\\d+/\\d+|next (monday|tuesday|wednesday|thursday|friday|saturday|sunday)|tomorrow|next week"))

        // If the message is long (likely contains event details) and has date/time patterns
        return containsDateTimePatterns && message.length > 30
    }

    /**
     * Handles event extraction and formats the response.
     */
    private suspend fun handleEventExtraction(message: String) {
        eventExtractionService.extractEvents(message).collect { response ->
            // Format the extracted events
            val formattedResponse = formatEventResponse(response)

            // Update the bot message for typing animation
            _fullBotResponse.value = formattedResponse
            _currentBotMessage.value = formattedResponse

            // Add the formatted response as a bot message
            val botMessage = ChatMessage(
                content = formattedResponse,
                type = MessageType.BOT,
                isEventMessage = true,
                eventData = response
            )
            _messages.update { it + botMessage }
        }
    }

    /**
     * Formats the event extraction response into a readable message.
     */
    private fun formatEventResponse(response: EventExtractionResponse): String {
        if (response.eventCount == 0) {
            return "I couldn't find any events in your message."
        }

        val sb = StringBuilder()

        response.events.forEachIndexed { index, event ->
            if (index > 0) sb.append("\n")

            sb.append("Event-${index + 1}\n")
            sb.append("title: ${event.title}\n")
            sb.append(" start: ${event.startTime}\n")
            sb.append(" end: ${event.endTime}\n")

            event.location?.let { sb.append(" location: $it\n") }
            event.notes?.let { sb.append(" notes: $it\n") }
            event.reminder?.let { sb.append(" reminder: $it\n") }
        }

        return sb.toString()
    }

    /**
     * Checks if the given text is a representation of an event.
     * Supports both JSON format and text-based format.
     */
    private fun isEventJson(text: String): Boolean {
        // Check for JSON format
        try {
            val trimmedText = text.trim()
            if (trimmedText.startsWith("{") && trimmedText.endsWith("}")) {
                val jsonObject = com.google.gson.JsonParser().parse(trimmedText).asJsonObject
                if (jsonObject.has("eventCount") && jsonObject.has("events")) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Not a valid JSON, continue to check text format
        }

        // Check for text-based format (event-N followed by event details)
        val eventPattern = """event-\d+\s+title:""".toRegex()
        return eventPattern.containsMatchIn(text)
    }

    /**
     * Parses the given text into an EventExtractionResponse object.
     * Supports both JSON format and text-based format.
     */
    private fun parseEventJson(text: String): EventExtractionResponse {
        // Try parsing as JSON first
        try {
            val trimmedText = text.trim()
            if (trimmedText.startsWith("{") && trimmedText.endsWith("}")) {
                val gson = com.google.gson.Gson()
                return gson.fromJson(trimmedText, EventExtractionResponse::class.java)
            }
        } catch (e: Exception) {
            // Not a valid JSON, try text format
        }

        // Parse text-based format
        return parseTextBasedEventFormat(text)
    }

    /**
     * Parses the text-based event format into an EventExtractionResponse object.
     * Format example:
     * event-1
     * title: Event Title
     * start: YYYY-MM-DD HH:MM:SS
     * end: YYYY-MM-DD HH:MM:SS
     * location: Location
     * notes: Notes
     */
    private fun parseTextBasedEventFormat(text: String): EventExtractionResponse {
        val events = mutableListOf<com.imrul.chatbot.data.models.CalendarEvent>()

        // Split by event-N to get individual events
        val eventBlocks = text.split("""(?=event-\d+)""".toRegex()).filter { it.isNotBlank() }

        for (block in eventBlocks) {
            val lines = block.trim().split("\n")

            var title = ""
            var startTime = ""
            var endTime = ""
            var location: String? = null
            var notes: String? = null

            for (line in lines) {
                val trimmedLine = line.trim()
                when {
                    trimmedLine.startsWith("title:") -> title = trimmedLine.substringAfter("title:").trim()
                    trimmedLine.startsWith("start:") -> startTime = trimmedLine.substringAfter("start:").trim()
                    trimmedLine.startsWith("end:") -> endTime = trimmedLine.substringAfter("end:").trim()
                    trimmedLine.startsWith("location:") -> location = trimmedLine.substringAfter("location:").trim()
                    trimmedLine.startsWith("notes:") -> notes = trimmedLine.substringAfter("notes:").trim()
                }
            }

            // Only add if we have the required fields
            if (title.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                events.add(com.imrul.chatbot.data.models.CalendarEvent(
                    title = title,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    notes = notes
                ))
            }
        }

        return com.imrul.chatbot.data.models.EventExtractionResponse(events, events.size)
    }
}
