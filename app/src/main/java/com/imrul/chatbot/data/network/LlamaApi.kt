package com.imrul.chatbot.data.network

import com.imrul.chatbot.data.models.LlamaRequest
import com.imrul.chatbot.data.models.LlamaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface LlamaApi {
    @Streaming
    @POST("generate")
    suspend fun generateResponse(
        @Body request: LlamaRequest
    ): Response<LlamaResponse>
}