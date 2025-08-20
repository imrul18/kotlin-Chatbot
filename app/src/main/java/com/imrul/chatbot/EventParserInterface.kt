package com.imrul.chatbot

import com.imrul.chatbot.data.service.EventParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Interface for parsing events from text input.
 * This class provides a simple way to convert text input into a normalized event block.
 */
class EventParserInterface {
    private val eventParser = EventParser()
    
    /**
     * Parses the input text and returns a normalized event block.
     * This method is designed to be called from a non-coroutine context.
     * 
     * @param input The text to parse
     * @param callback The callback to receive the result
     */
    fun parseEvent(input: String, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = eventParser.parseEvent(input)
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback("Error parsing event: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Parses the input text and returns a normalized event block.
     * This method is designed to be called from a coroutine context.
     * 
     * @param input The text to parse
     * @return The normalized event block as a string
     */
    suspend fun parseEventSuspend(input: String): String {
        return try {
            eventParser.parseEvent(input)
        } catch (e: Exception) {
            "Error parsing event: ${e.message}"
        }
    }
}