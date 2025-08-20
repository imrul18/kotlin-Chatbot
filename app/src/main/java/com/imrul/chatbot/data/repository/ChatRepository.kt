package com.imrul.chatbot.data.repository

import android.util.Log
import com.google.gson.Gson
import com.imrul.chatbot.data.models.LlamaRequest
import com.imrul.chatbot.data.models.LlamaResponse
import com.imrul.chatbot.data.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader

class ChatRepository {
    private val gson = Gson()
    private val client = NetworkModule.okHttpClient
    private val baseUrl = NetworkModule.BASE_URL + "generate"

    fun generateResponse(prompt: String): Flow<String> = flow {
        // add log here
        Log.d("ChatRepository", "Generating response for prompt: $prompt")
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
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    if (line?.isNotEmpty() == true) {
                        try {
                            val llamaResponse = gson.fromJson(line, LlamaResponse::class.java)
                            emit(llamaResponse.response)

                            if (llamaResponse.done) {
                                break
                            }
                        } catch (e: Exception) {
                            // Skip malformed JSON
                        }
                    }
                }

                reader.close()
            } else {
                throw Exception("Response body is null")
            }
        } else {
            throw Exception("API call failed with code ${response.code}")
        }
    }.flowOn(Dispatchers.IO)
}
