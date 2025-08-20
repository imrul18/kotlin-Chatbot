package com.imrul.chatbot

import com.imrul.chatbot.data.models.ChatMessage
import com.imrul.chatbot.data.models.MessageType
import com.imrul.chatbot.ui.ChatViewModel
import org.junit.Test
import org.junit.Assert.*

class EventParsingTest {

    @Test
    fun testEventParsing() {
        // Create a ChatViewModel instance
        val viewModel = ChatViewModel()
        
        // Access the private methods using reflection
        val isEventJsonMethod = ChatViewModel::class.java.getDeclaredMethod("isEventJson", String::class.java)
        isEventJsonMethod.isAccessible = true
        
        val parseEventJsonMethod = ChatViewModel::class.java.getDeclaredMethod("parseEventJson", String::class.java)
        parseEventJsonMethod.isAccessible = true
        
        // Example from the issue description
        val eventText = """event-1
                                                                                                    title: Prototype Review
                                                                                                    start: 2025-08-20 14:00:00
                                                                                                    end: 2025-08-20 14:45:00
                                                                                                    location: Factory Leipzig
                                                                                                    notes: Present my findings"""
        
        // Test isEventJson
        val isEvent = isEventJsonMethod.invoke(viewModel, eventText) as Boolean
        assertTrue("Should detect as an event", isEvent)
        
        // Test parseEventJson
        val eventData = parseEventJsonMethod.invoke(viewModel, eventText)
        assertNotNull("Should parse event data", eventData)
        
        // Create a ChatMessage with the event data
        val message = ChatMessage(
            content = eventText,
            type = MessageType.BOT,
            isEventMessage = isEvent,
            eventData = eventData
        )
        
        // Verify the ChatMessage
        assertTrue("isEventMessage should be true", message.isEventMessage)
        assertNotNull("eventData should not be null", message.eventData)
        assertEquals("Should have 1 event", 1, message.eventData?.eventCount)
        assertEquals("Event title should match", "Prototype Review", message.eventData?.events?.get(0)?.title)
        assertEquals("Event start time should match", "2025-08-20 14:00:00", message.eventData?.events?.get(0)?.startTime)
        assertEquals("Event end time should match", "2025-08-20 14:45:00", message.eventData?.events?.get(0)?.endTime)
        assertEquals("Event location should match", "Factory Leipzig", message.eventData?.events?.get(0)?.location)
        assertEquals("Event notes should match", "Present my findings", message.eventData?.events?.get(0)?.notes)
    }
}