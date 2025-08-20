package com.imrul.chatbot.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imrul.chatbot.data.models.CalendarEvent
import com.imrul.chatbot.data.models.ChatMessage
import com.imrul.chatbot.data.models.MessageType
import com.imrul.chatbot.ui.theme.BotMessageColor
import com.imrul.chatbot.ui.theme.UserMessageColor
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()

    Log.d("", "ChatScreen Messages: $messages")

    val isLoading by viewModel.isLoading.collectAsState()
    val currentBotMessage by viewModel.currentBotMessage.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(message)
            }

            // Show thinking indicator if loading but no response yet
            if (isLoading && currentBotMessage.isEmpty()) {
                item {
                    ThinkingIndicator()
                }
            }

            // Show typing animation if loading and response started
            if (isLoading && currentBotMessage.isNotEmpty()) {
                item {
                    TypingIndicator(currentBotMessage)
                }
            }
        }

        // Scroll to bottom when new message is added or when typing/thinking
        LaunchedEffect(messages.size, currentBotMessage, isLoading) {
            if (messages.isNotEmpty() || currentBotMessage.isNotEmpty() || isLoading) {
                coroutineScope.launch {
                    val index = when {
                        isLoading -> messages.size
                        messages.isNotEmpty() -> messages.size - 1
                        else -> 0
                    }
                    if (index >= 0) {
                        listState.animateScrollToItem(index)
                    }
                }
            }
        }

        // Input field and send button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Type a message") },
                maxLines = 3
            )
            Button(
                onClick = {
                    if (inputText.isNotBlank() && !isLoading) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = !isLoading && inputText.isNotBlank(),
                shape = CircleShape,
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUserMessage = message.type == MessageType.USER
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isUserMessage) UserMessageColor else BotMessageColor,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            if (message.isEventMessage && message.eventData != null) {
                // Display event message with calendar button
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Display the event details
                    message.eventData.events.forEachIndexed { index, event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "Start: ${event.startTime}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Text(
                                    text = "End: ${event.endTime}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                event.location?.let {
                                    Text(
                                        text = "Location: $it",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                event.notes?.let {
                                    Text(
                                        text = "Notes: $it",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                // Add to Calendar button
                                Button(
                                    onClick = {
                                        addToCalendar(context, event)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Text("Add to Calendar")
                                }
                            }
                        }
                    }
                }
            } else {
                // Display regular message
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Adds an event to the device calendar.
 */
private fun addToCalendar(context: android.content.Context, event: CalendarEvent) {
    // Check for calendar permissions
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        val hasWriteCalendarPermission = context.checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasWriteCalendarPermission) {
            // Request permission through the activity
            if (context is android.app.Activity) {
                android.widget.Toast.makeText(context, "Calendar permission is required to add events", android.widget.Toast.LENGTH_LONG).show()
                context.requestPermissions(
                    arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR),
                    com.imrul.chatbot.MainActivity.CALENDAR_PERMISSION_REQUEST_CODE
                )
                return
            } else {
                android.widget.Toast.makeText(context, "Calendar permission is required to add events", android.widget.Toast.LENGTH_LONG).show()
                return
            }
        }
    }

    // Create intent to add event to calendar
    val intent = android.content.Intent(android.content.Intent.ACTION_INSERT)
        .setData(android.provider.CalendarContract.Events.CONTENT_URI)
        .putExtra(android.provider.CalendarContract.Events.TITLE, event.title)
        .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, event.notes ?: "")
        .putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, event.location ?: "")

    // Parse start and end times
    try {
        val startDateTime = java.time.LocalDateTime.parse(
            event.startTime.replace(" ", "T")
        )
        val endDateTime = java.time.LocalDateTime.parse(
            event.endTime.replace(" ", "T")
        )

        val startMillis = startDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
    } catch (e: Exception) {
        // If parsing fails, just open the calendar with the basic info
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Could not open calendar: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun ThinkingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BotMessageColor,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Thinking",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Thinking...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun TypingIndicator(currentText: String) {
    var displayText by remember { mutableStateOf("") }
    var currentIndex by remember { mutableStateOf(0) }
    var showCursor by remember { mutableStateOf(true) }

    // Effect to animate the typing
    LaunchedEffect(currentText) {
        // Reset if new text is received
        if (currentText.length < displayText.length) {
            displayText = ""
            currentIndex = 0
        }

        // Animate typing for new characters
        while (currentIndex < currentText.length) {
            displayText = currentText.substring(0, currentIndex + 1)
            currentIndex++
            kotlinx.coroutines.delay(50) // Adjust speed of typing animation
        }
    }

    // Blinking cursor effect
    LaunchedEffect(Unit) {
        while (true) {
            showCursor = !showCursor
            kotlinx.coroutines.delay(500) // Blink every 500ms
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BotMessageColor,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Typing",
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Typing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Show cursor at the end
                if (displayText.length < currentText.length && showCursor) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .height(2.dp)
                            .width(8.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}
