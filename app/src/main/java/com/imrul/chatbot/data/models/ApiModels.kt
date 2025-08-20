package com.imrul.chatbot.data.models

import com.google.gson.annotations.SerializedName

data class LlamaRequest(
    val model: String = "llama2",
    val prompt: String
)

data class LlamaResponse(
    val model: String,
    @SerializedName("created_at")
    val createdAt: String,
    val response: String,
    val done: Boolean,
    @SerializedName("done_reason")
    val doneReason: String? = null,
    val context: List<Int>? = null,
    val totalDuration: Long? = null,
    val loadDuration: Long? = null,
    val promptEvalCount: Int? = null,
    val promptEvalDuration: Long? = null,
    val evalCount: Int? = null,
    val evalDuration: Long? = null
)