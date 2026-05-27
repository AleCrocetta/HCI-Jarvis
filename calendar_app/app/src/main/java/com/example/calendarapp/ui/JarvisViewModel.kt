package com.example.calendarapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.model.CalendarEvent
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.defineFunction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class ChatMessage(val id: String = UUID.randomUUID().toString(), val text: String, val isUser: Boolean)

class JarvisViewModel : ViewModel() {
    
    private val apiKey = com.example.calendarapp.BuildConfig.GEMINI_API_KEY

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val createEventFunction = defineFunction(
        name = "create_event",
        description = "Creates a new calendar event.",
        parameters = listOf(
            Schema.str("title", "The title of the event"),
            Schema.str("time", "The time of the event (e.g., '10:00 AM - 11:30 AM')"),
            Schema.int("day", "The day of the month as an integer"),
            Schema.str("month", "The full name of the month in ENGLISH ONLY (e.g., 'January', 'May')"),
            Schema.int("year", "The year of the event")
        )
    )

    private val deleteEventFunction = defineFunction(
        name = "delete_event",
        description = "Deletes an existing calendar event by ID.",
        parameters = listOf(Schema.str("eventId", "The unique ID of the event to delete"))
    )

    private val modifyEventFunction = defineFunction(
        name = "modify_event",
        description = "Modifies an existing calendar event. You MUST provide the full updated state of the event, including all fields (title, time, day, month, year) even if they are unchanged.",
        parameters = listOf(
            Schema.str("eventId", "The unique ID of the event to modify"),
            Schema.str("title", "The new title of the event"),
            Schema.str("time", "The new time of the event"),
            Schema.int("day", "The new day of the month"),
            Schema.str("month", "The new month name in ENGLISH ONLY"),
            Schema.int("year", "The new year")
        )
    )
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        tools = listOf(Tool(listOf(createEventFunction, deleteEventFunction, modifyEventFunction))),
        systemInstruction = content { text("You are Jarvis, an AI calendar assistant. Your job is to help the user manage their calendar events. You can create, delete, or modify events using the provided tools. ALWAYS respond in the SAME language that the user uses to speak to you.") }
    )

    fun handleUserPrompt(
        prompt: String,
        currentEvents: List<CalendarEvent>,
        onAddEvent: (CalendarEvent) -> Unit,
        onRemoveEvent: (String) -> Unit,
        onModifyEvent: (CalendarEvent) -> Unit
    ) {
        // Add user message to UI
        val userMessage = ChatMessage(text = prompt, isUser = true)
        _chatHistory.value = _chatHistory.value + userMessage

        viewModelScope.launch {
            try {
                // Context about current events
                var contextStr = "Today's Date: ${java.util.Calendar.getInstance().time}\n\nCurrent Events:\n"
                currentEvents.forEach {
                    contextStr += "ID: ${it.id}, Title: ${it.title}, Date: ${it.month} ${it.day}, ${it.year}, Time: ${it.time}\n"
                }
                
                val chat = generativeModel.startChat()
                val response = chat.sendMessage(prompt + "\n" + contextStr)

                var responseText = response.text ?: ""

                response.functionCalls.forEach { call ->
                    when (call.name) {
                        "create_event" -> {
                            val args = call.args
                            val event = CalendarEvent(
                                day = args["day"]?.toString()?.toFloat()?.toInt() ?: 1,
                                month = args["month"]?.toString() ?: "January",
                                year = args["year"]?.toString()?.toFloat()?.toInt() ?: 2026,
                                title = args["title"]?.toString()?.uppercase() ?: "NEW EVENT",
                                time = args["time"]?.toString() ?: "12:00 PM"
                            )
                            onAddEvent(event)
                            if (responseText.isBlank()) responseText = "Ho creato l'evento: ${event.title}."
                        }
                        "delete_event" -> {
                            val id = call.args["eventId"]?.toString() ?: call.args.values.firstOrNull()?.toString()
                            if (id != null) {
                                onRemoveEvent(id)
                                if (responseText.isBlank()) responseText = "Ho eliminato l'evento richiesto."
                            }
                        }
                        "modify_event" -> {
                            val args = call.args
                            val id = args["eventId"]?.toString()
                            if (id != null) {
                                val modifiedEvent = CalendarEvent(
                                    id = id,
                                    day = args["day"]?.toString()?.toFloat()?.toInt() ?: 1,
                                    month = args["month"]?.toString() ?: "January",
                                    year = args["year"]?.toString()?.toFloat()?.toInt() ?: 2026,
                                    title = args["title"]?.toString()?.uppercase() ?: "UPDATED EVENT",
                                    time = args["time"]?.toString() ?: "12:00 PM"
                                )
                                onModifyEvent(modifiedEvent)
                                if (responseText.isBlank()) responseText = "Ho modificato l'evento: ${modifiedEvent.title}."
                            }
                        }
                    }
                }
                
                if (responseText.isNotBlank()) {
                    _chatHistory.value = _chatHistory.value + ChatMessage(text = responseText, isUser = false)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _chatHistory.value = _chatHistory.value + ChatMessage(text = "Si è verificato un errore: ${e.message}", isUser = false)
            }
        }
    }
}
