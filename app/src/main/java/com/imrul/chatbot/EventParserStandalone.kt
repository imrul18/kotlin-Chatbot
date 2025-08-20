package com.imrul.chatbot

import kotlinx.coroutines.runBlocking

/**
 * A standalone event parser that can be used directly without any UI.
 * This class provides a simple way to parse events from text input.
 * 
 * Usage:
 * ```
 * val parser = EventParserStandalone()
 * val result = parser.parseEvent("add prototype review with max this friday at 2 in the afternoon for about 45 minutes at factory leipzig, notes: present my findings")
 * println(result)
 * ```
 */
class EventParserStandalone {
    private val eventParserInterface = EventParserInterface()
    
    /**
     * Parses the input text and returns a normalized event block.
     * This method blocks the current thread until the parsing is complete.
     * 
     * @param input The text to parse
     * @return The normalized event block as a string
     */
    fun parseEvent(input: String): String {
        return runBlocking {
            eventParserInterface.parseEventSuspend(input)
        }
    }
}

/**
 * Main function for testing the event parser.
 * This function can be used to test the event parser from the command line.
 * 
 * Usage:
 * ```
 * java -cp app.jar com.imrul.chatbot.EventParserStandaloneKt "add prototype review with max this friday at 2 in the afternoon for about 45 minutes at factory leipzig, notes: present my findings"
 * ```
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Please provide an input text to parse.")
        return
    }
    
    val input = args[0]
    val parser = EventParserStandalone()
    val result = parser.parseEvent(input)
    
    // Print the result without any additional text
    print(result)
}