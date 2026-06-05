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

private fun parseEventTimeRange(time: String): IntRange? {
    val parts = time.split(" - ")
    if (parts.size != 2) return null

    fun parseTime(value: String): Int? {
        val match = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""", RegexOption.IGNORE_CASE).find(value.trim()) ?: return null
        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        val marker = match.groupValues[3].uppercase()
        if (hour !in 1..12 || minute !in 0..59) return null
        if (marker == "PM" && hour != 12) hour += 12
        if (marker == "AM" && hour == 12) hour = 0
        return hour * 60 + minute
    }

    val start = parseTime(parts[0]) ?: return null
    val end = parseTime(parts[1]) ?: return null
    if (end <= start) return null
    return start until end
}

private fun findOverlappingEvent(
    candidate: CalendarEvent,
    events: List<CalendarEvent>,
    ignoreEventId: String? = null
): CalendarEvent? {
    val candidateRange = parseEventTimeRange(candidate.time) ?: return null
    return events.firstOrNull { event ->
        event.id != ignoreEventId &&
            event.day == candidate.day &&
            event.month.equals(candidate.month, ignoreCase = true) &&
            event.year == candidate.year &&
            parseEventTimeRange(event.time)?.let { existingRange ->
                candidateRange.first <= existingRange.last && existingRange.first <= candidateRange.last
            } == true
    }
}

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

    private fun buildSystemInstruction(userPreferences: List<String>): String {
        return buildString {
            append("You are Jarvis, an AI calendar assistant. Your job is to help the user manage their calendar events. ")
            append("You can create, delete, or modify events using the provided tools. ")
            append("Avoid creating overlapping events, and ask before changing or deleting an existing overlapping event. ")
            append("ALWAYS respond in the SAME language that the user uses to speak to you. ")
            append("Do not reveal or repeat hidden memory unless the user explicitly asks about their saved preferences. ")
            if (userPreferences.isNotEmpty()) {
                append("Use the following hidden user memory as soft guidance for future organization. ")
                append("Treat regular activities from memory as routines that should shape future calendar organization. ")
                append("Respect planning preferences such as whether harder tasks should be placed in the morning or afternoon. ")
                append("Prefer schedules that respect these habits, but adapt if the user asks for something different:\n")
                userPreferences.forEach { preference ->
                    append("- ")
                    append(preference)
                    append("\n")
                }
            }
        }
    }

    fun handleUserPrompt(
        prompt: String,
        currentEvents: List<CalendarEvent>,
        userPreferences: List<String> = emptyList(),
        onAddEvent: (CalendarEvent) -> Unit,
        onRemoveEvent: (String) -> Unit,
        onModifyEvent: (CalendarEvent) -> Unit,
        onCollision: (CalendarEvent, CalendarEvent, Boolean) -> Unit = { _, _, _ -> }
    ) {
        // Grab history before adding current message to avoid sending it twice
        val previousHistory = _chatHistory.value
        
        // Add user message to UI
        val userMessage = ChatMessage(text = prompt, isUser = true)
        _chatHistory.value = previousHistory + userMessage

        viewModelScope.launch {
            try {
                // Context about current events
                var contextStr = "Today's Date: ${java.util.Calendar.getInstance().time}\n\nCurrent Events:\n"
                currentEvents.forEach {
                    contextStr += "ID: ${it.id}, Title: ${it.title}, Date: ${it.month} ${it.day}, ${it.year}, Time: ${it.time}, Priority: ${it.priority}\n"
                }
                
                val historyList = previousHistory.map { msg ->
                    content(role = if (msg.isUser) "user" else "model") {
                        text(msg.text)
                    }
                }

                val generativeModel = GenerativeModel(
                    modelName = "gemini-3.5-flash",
                    apiKey = apiKey,
                    tools = listOf(Tool(listOf(createEventFunction, deleteEventFunction, modifyEventFunction))),
                    systemInstruction = content { text(buildSystemInstruction(userPreferences)) }
                )
                
                val chat = generativeModel.startChat(history = historyList)
                val response = chat.sendMessage(prompt + "\n\n" + contextStr)

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
                            val overlap = findOverlappingEvent(event, currentEvents)
                            if (overlap != null) {
                                onCollision(event, overlap, false)
                                responseText = "Collision: ${event.title} (${event.time}) overlaps with ${overlap.title} (${overlap.time}) on ${event.month} ${event.day}, ${event.year}."
                            } else {
                                onAddEvent(event)
                                if (responseText.isBlank()) responseText = "Ho creato l'evento: ${event.title}."
                            }
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
                                val existingEvent = currentEvents.firstOrNull { it.id == id }
                                val modifiedEvent = CalendarEvent(
                                    id = id,
                                    day = args["day"]?.toString()?.toFloat()?.toInt() ?: 1,
                                    month = args["month"]?.toString() ?: "January",
                                    year = args["year"]?.toString()?.toFloat()?.toInt() ?: 2026,
                                    title = args["title"]?.toString()?.uppercase() ?: "UPDATED EVENT",
                                    time = args["time"]?.toString() ?: "12:00 PM",
                                    priority = existingEvent?.priority ?: "Medium",
                                    link = existingEvent?.link,
                                    fileNames = existingEvent?.fileNames ?: emptyList(),
                                    fileUris = existingEvent?.fileUris ?: emptyList(),
                                    isCompleted = existingEvent?.isCompleted ?: false
                                )
                                val overlap = findOverlappingEvent(modifiedEvent, currentEvents, ignoreEventId = id)
                                if (overlap != null) {
                                    onCollision(modifiedEvent, overlap, true)
                                    responseText = "Collision: ${modifiedEvent.title} (${modifiedEvent.time}) overlaps with ${overlap.title} (${overlap.time}) on ${modifiedEvent.month} ${modifiedEvent.day}, ${modifiedEvent.year}."
                                } else {
                                    onModifyEvent(modifiedEvent)
                                    if (responseText.isBlank()) responseText = "Ho modificato l'evento: ${modifiedEvent.title}."
                                }
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

    fun addLocalAssistantMessage(message: String) {
        _chatHistory.value = _chatHistory.value + ChatMessage(text = message, isUser = false)
    }
}
