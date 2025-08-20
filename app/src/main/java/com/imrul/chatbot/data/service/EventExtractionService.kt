package com.imrul.chatbot.data.service

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.imrul.chatbot.data.models.CalendarEvent
import com.imrul.chatbot.data.models.EventExtractionResponse
import com.imrul.chatbot.data.models.LlamaRequest
import com.imrul.chatbot.data.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Service for extracting calendar events from text using the Llama API.
 */
class EventExtractionService {
    private val gson = Gson()
    private val client = NetworkModule.okHttpClient
    private val baseUrl = NetworkModule.BASE_URL + "generate"

    /**
     * Extracts calendar events from the given text.
     * 
     * @param text The text to extract events from
     * @return A flow emitting the extracted events response
     */
    fun extractEvents(text: String): Flow<EventExtractionResponse> = flow {
        val prompt = buildPrompt(text)
        val request = LlamaRequest(prompt = prompt)
        val requestJson = gson.toJson(request)

        val requestBody = requestJson.toRequestBody("application/json".toMediaTypeOrNull())
        val httpRequest = Request.Builder()
            .url(baseUrl)
            .post(requestBody)
            .build()

        val response = client.newCall(httpRequest).execute()
        
        if (response.isSuccessful) {
            val inputStream = response.body?.byteStream()
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                val responseBuilder = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    if (line?.isNotEmpty() == true) {
                        try {
                            val llamaResponse = gson.fromJson(line, com.imrul.chatbot.data.models.LlamaResponse::class.java)
                            responseBuilder.append(llamaResponse.response)
                            
                            if (llamaResponse.done) {
                                break
                            }
                        } catch (e: Exception) {
                            // Skip malformed JSON
                        }
                    }
                }
                
                reader.close()
                
                // Parse the complete response to extract events
                val extractedEvents = parseEventsFromResponse(responseBuilder.toString())
                emit(extractedEvents)
            } else {
                throw Exception("Response body is null")
            }
        } else {
            throw Exception("API call failed with code ${response.code}")
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Builds the prompt for the Llama model to extract events.
     */
    private fun buildPrompt(text: String): String {
        return """
            You are an AI assistant that extracts calendar events from text. 
            Extract all calendar events from the following text and format them as a JSON object.
            
            Format the response as follows:
            {
              "eventCount": number of events found,
              "events": [
                {
                  "title": "event title",
                  "startTime": "YYYY-MM-DD HH:MM",
                  "endTime": "YYYY-MM-DD HH:MM",
                  "location": "location if specified",
                  "notes": "any notes or details",
                  "reminder": "reminder time if specified"
                }
              ]
            }
            
            If no events are found, return {"eventCount": 0, "events": []}.
            Only include fields that are specified in the text.
            Do not include any explanations, just the JSON object.
            
            Text: $text
        """.trimIndent()
    }
    
    /**
     * Parses the response from the Llama model to extract events.
     */
    private fun parseEventsFromResponse(response: String): EventExtractionResponse {
        try {
            // Try to parse the entire response as JSON
            return gson.fromJson(response, EventExtractionResponse::class.java)
        } catch (e: JsonSyntaxException) {
            // If that fails, try to extract just the JSON part
            val jsonPattern = """\{[\s\S]*\}""".toRegex()
            val jsonMatch = jsonPattern.find(response)
            
            if (jsonMatch != null) {
                try {
                    return gson.fromJson(jsonMatch.value, EventExtractionResponse::class.java)
                } catch (e: JsonSyntaxException) {
                    // If JSON parsing still fails, return empty response
                    return EventExtractionResponse(emptyList(), 0)
                }
            }
            
            // If no JSON found, return empty response
            return EventExtractionResponse(emptyList(), 0)
        }
    }
}