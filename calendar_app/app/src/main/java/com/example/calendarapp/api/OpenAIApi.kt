package com.example.calendarapp.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// --- OpenAI Compatible Data Models ---

data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val tools: List<Tool>? = null,
    val tool_choice: String? = "auto"
)

data class Message(
    val role: String,
    val content: String? = null,
    val tool_calls: List<ToolCall>? = null,
    val tool_call_id: String? = null,
    val name: String? = null
)

data class Tool(
    val type: String = "function",
    val function: FunctionDeclaration
)

data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: JsonSchema
)

data class JsonSchema(
    val type: String = "object",
    val properties: Map<String, JsonSchemaProperty>,
    val required: List<String>
)

data class JsonSchemaProperty(
    val type: String,
    val description: String
)

data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val index: Int,
    val message: ResponseMessage,
    val finish_reason: String?
)

data class ResponseMessage(
    val role: String,
    val content: String?,
    val tool_calls: List<ToolCall>?
)

data class ToolCall(
    val id: String,
    val type: String,
    val function: FunctionCall
)

data class FunctionCall(
    val name: String,
    val arguments: String // This is a JSON string!
)

// --- Retrofit API Interface ---

interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String? = null, // Used by OpenRouter
        @Header("X-Title") title: String? = null, // Used by OpenRouter
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

object FallbackApiClient {
    fun create(baseUrl: String): OpenAIApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIApi::class.java)
    }
}
