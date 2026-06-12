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
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class ChatMessage(val id: String = UUID.randomUUID().toString(), val text: String, val isUser: Boolean)

private fun parseEventClockTime(value: String): Int? {
    val match = Regex("""^\s*(\d{1,2})(?::(\d{2}))?\s*(AM|PM)\s*$""", RegexOption.IGNORE_CASE).find(value.trim()) ?: return null
    var hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].ifBlank { "00" }.toIntOrNull() ?: return null
    val marker = match.groupValues[3].uppercase(Locale.US)
    if (hour !in 1..12 || minute !in 0..59) return null
    if (marker == "PM" && hour != 12) hour += 12
    if (marker == "AM" && hour == 12) hour = 0
    return hour * 60 + minute
}

private fun formatEventClockTime(totalMinutes: Int): String {
    val normalizedMinutes = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val hour24 = normalizedMinutes / 60
    val minute = normalizedMinutes % 60
    val marker = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when (val hour = hour24 % 12) {
        0 -> 12
        else -> hour
    }
    return String.format(Locale.US, "%02d:%02d %s", hour12, minute, marker)
}

private fun normalizeEventTimeRange(time: String): String {
    val parts = time.split(Regex("""\s*-\s*"""), limit = 2)
    if (parts.size == 2) {
        val start = parseEventClockTime(parts[0])
        val end = parseEventClockTime(parts[1])
        if (start != null && end != null && end > start) {
            return "${formatEventClockTime(start)} - ${formatEventClockTime(end)}"
        }
    }

    val start = parseEventClockTime(time) ?: return time
    return "${formatEventClockTime(start)} - ${formatEventClockTime(start + 60)}"
}

private fun parseEventTimeRange(time: String): IntRange? {
    val parts = normalizeEventTimeRange(time).split(" - ")
    if (parts.size != 2) return null

    fun parseTime(value: String): Int? {
        return parseEventClockTime(value)
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
    
    private val geminiApiKey = com.example.calendarapp.BuildConfig.GEMINI_API_KEY
    private val openRouterApiKey = com.example.calendarapp.BuildConfig.OPENROUTER_API_KEY
    private val primaryModelName = "gemini-2.5-flash"
    private val openRouterModels = listOf(
        "nvidia/nemotron-nano-9b-v2:free",
        "nvidia/nemotron-nano-12b-v2-vl:free",
        "openai/gpt-oss-20b:free",
        "google/gemma-4-26b-a4b-it:free",
        "nvidia/nemotron-3-nano-30b-a3b:free",
        "google/gemma-4-31b-it:free",
        "meta-llama/llama-3.3-70b-instruct:free",
        "qwen/qwen3-next-80b-a3b-instruct:free",
        "nvidia/nemotron-3-super-120b-a12b:free",
        "openai/gpt-oss-120b:free",
        "nvidia/nemotron-3-ultra-550b-a55b:free"
    )

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val createEventFunction = defineFunction(
        name = "create_event",
        description = "Creates a new calendar event. If the user does not mention a year, use the current calendar year.",
        parameters = listOf(
            Schema.str("title", "The title of the event"),
            Schema.str("time", "The time of the event (e.g., '10:00 AM - 11:30 AM')"),
            Schema.int("day", "The day of the month as an integer"),
            Schema.str("month", "The full name of the month in ENGLISH ONLY (e.g., 'January', 'May')"),
            Schema.int("year", "Optional year of the event. If omitted, use the current calendar year.")
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
            append("When creating an event, do not ask only for the year if the user omitted it; use ${Calendar.getInstance().get(Calendar.YEAR)}. ")
            append("Never claim an event was created, deleted, or modified unless you called the matching tool. ")
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

    data class ParsedFunctionCall(val name: String, val args: Map<String, Any>)

    private fun parseTextCalendarAction(text: String): List<ParsedFunctionCall> {
        val jsonText = Regex("""\{[\s\S]*\}""").find(text)?.value ?: return emptyList()
        val jsonObject = runCatching { JSONObject(jsonText) }.getOrNull() ?: return emptyList()
        val action = jsonObject.optString("action", jsonObject.optString("name", "")).trim()
        val name = when (action) {
            "create_event", "delete_event", "modify_event" -> action
            else -> return emptyList()
        }
        val argsObject = jsonObject.optJSONObject("arguments")
            ?: jsonObject.optJSONObject("args")
            ?: jsonObject
        val argsMap = mutableMapOf<String, Any>()
        argsObject.keys().forEach { key ->
            if (key != "action" && key != "name") {
                argsMap[key] = argsObject.get(key)
            }
        }
        return listOf(ParsedFunctionCall(name = name, args = argsMap))
    }

    private fun isCalendarMutationPrompt(prompt: String): Boolean {
        val normalized = prompt.lowercase()
        return Regex("""\b(add|create|schedule|book|set|move|change|modify|delete|remove|cancel|aggiungi|crea|creami|metti|inserisci|programma|pianifica|fissa|sposta|cambia|modifica|elimina|rimuovi|cancella|annulla)\b""")
            .containsMatchIn(normalized)
    }

    private suspend fun sendToOpenAiCompatibleApi(
        baseUrl: String,
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        historyList: List<ChatMessage>,
        newMessage: String
    ): Pair<String, List<ParsedFunctionCall>> {
        val api = com.example.calendarapp.api.FallbackApiClient.create(baseUrl)
        val messages = mutableListOf<com.example.calendarapp.api.Message>()
        messages.add(com.example.calendarapp.api.Message(role = "system", content = systemInstruction +
            "\nIf function calling is unavailable and the user asks to create, delete, or modify an event, respond with only strict JSON: {\"action\":\"create_event\",\"arguments\":{\"title\":\"...\",\"time\":\"10:00 AM - 11:00 AM\",\"day\":1,\"month\":\"January\"}}. Include \"year\" only if the user specified it."))
        historyList.forEach {
            messages.add(com.example.calendarapp.api.Message(role = if (it.isUser) "user" else "assistant", content = it.text))
        }
        messages.add(com.example.calendarapp.api.Message(role = "user", content = newMessage))

        val openAiTools = listOf(
            com.example.calendarapp.api.Tool(
                function = com.example.calendarapp.api.FunctionDeclaration(
                    name = "create_event",
                    description = "Creates a new calendar event.",
                    parameters = com.example.calendarapp.api.JsonSchema(
                        properties = mapOf(
                            "title" to com.example.calendarapp.api.JsonSchemaProperty("string", "The title of the event"),
                            "time" to com.example.calendarapp.api.JsonSchemaProperty("string", "The time of the event (e.g., '10:00 AM - 11:30 AM')"),
                            "day" to com.example.calendarapp.api.JsonSchemaProperty("integer", "The day of the month as an integer"),
                            "month" to com.example.calendarapp.api.JsonSchemaProperty("string", "The full name of the month in ENGLISH ONLY"),
                            "year" to com.example.calendarapp.api.JsonSchemaProperty("integer", "Optional year of the event. If omitted, use the current calendar year.")
                        ),
                        required = listOf("title", "time", "day", "month")
                    )
                )
            ),
            com.example.calendarapp.api.Tool(
                function = com.example.calendarapp.api.FunctionDeclaration(
                    name = "delete_event",
                    description = "Deletes an existing calendar event by ID.",
                    parameters = com.example.calendarapp.api.JsonSchema(
                        properties = mapOf(
                            "eventId" to com.example.calendarapp.api.JsonSchemaProperty("string", "The unique ID of the event to delete")
                        ),
                        required = listOf("eventId")
                    )
                )
            ),
            com.example.calendarapp.api.Tool(
                function = com.example.calendarapp.api.FunctionDeclaration(
                    name = "modify_event",
                    description = "Modifies an existing calendar event. You MUST provide the full updated state of the event, including all fields.",
                    parameters = com.example.calendarapp.api.JsonSchema(
                        properties = mapOf(
                            "eventId" to com.example.calendarapp.api.JsonSchemaProperty("string", "The unique ID of the event to modify"),
                            "title" to com.example.calendarapp.api.JsonSchemaProperty("string", "The new title of the event"),
                            "time" to com.example.calendarapp.api.JsonSchemaProperty("string", "The new time of the event"),
                            "day" to com.example.calendarapp.api.JsonSchemaProperty("integer", "The new day of the month"),
                            "month" to com.example.calendarapp.api.JsonSchemaProperty("string", "The new month name in ENGLISH ONLY"),
                            "year" to com.example.calendarapp.api.JsonSchemaProperty("integer", "The new year")
                        ),
                        required = listOf("eventId", "title", "time", "day", "month", "year")
                    )
                )
            )
        )

        val request = com.example.calendarapp.api.ChatCompletionRequest(
            model = modelName,
            messages = messages,
            tools = openAiTools
        )

        val response = api.createChatCompletion(
            authHeader = "Bearer $apiKey",
            referer = "https://calendarapp.example.com",
            title = "Calendar App",
            request = request
        )

        val choice = response.choices.firstOrNull()?.message ?: return Pair("", emptyList())
        val text = choice.content ?: ""
        val parsedCalls = choice.tool_calls?.mapNotNull { call ->
            try {
                val argsMap = mutableMapOf<String, Any>()
                val jsonObject = org.json.JSONObject(call.function.arguments)
                jsonObject.keys().forEach { key ->
                    argsMap[key] = jsonObject.get(key)
                }
                ParsedFunctionCall(name = call.function.name, args = argsMap)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()

        return Pair(text, parsedCalls.ifEmpty { parseTextCalendarAction(text) })
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

                val tools = listOf(Tool(listOf(createEventFunction, deleteEventFunction, modifyEventFunction)))
                val systemInstruction = content { text(buildSystemInstruction(userPreferences)) }
                val message = prompt +
                    "\n\nReply in the same language as this user message. If the user message mixes languages, use the dominant language of the user message." +
                    "\n\n" + contextStr

                suspend fun sendToGemini(): Pair<String, List<ParsedFunctionCall>> {
                    val res = GenerativeModel(
                        modelName = primaryModelName,
                        apiKey = geminiApiKey,
                        tools = tools,
                        systemInstruction = systemInstruction
                    ).startChat(history = historyList).sendMessage(message)
                    val parsedCalls = res.functionCalls.map {
                        ParsedFunctionCall(
                            name = it.name,
                            args = it.args.mapValues { entry -> entry.value ?: "" }
                        )
                    }
                    return Pair(res.text ?: "", parsedCalls)
                }

                suspend fun sendToOpenRouterFallback(): Pair<String, List<ParsedFunctionCall>> {
                    var lastException: Exception? = null
                    for (model in openRouterModels) {
                        try {
                            val result = sendToOpenAiCompatibleApi(
                                baseUrl = "https://openrouter.ai/api/",
                                apiKey = openRouterApiKey,
                                modelName = model,
                                systemInstruction = buildSystemInstruction(userPreferences),
                                historyList = previousHistory,
                                newMessage = message
                            )
                            if (result.second.isEmpty() && isCalendarMutationPrompt(prompt)) {
                                lastException = Exception("$model did not return a calendar tool call.")
                                continue
                            }
                            return result
                        } catch (e: Exception) {
                            lastException = e
                            if (e.message?.contains("401") == true) {
                                throw e // Stop iterating if the API key is unauthorized
                            }
                            // Otherwise continue to the next fallback model
                        }
                    }
                    throw lastException ?: Exception("All OpenRouter models failed.")
                }

                val responsePair = try {
                    sendToGemini()
                } catch (e: Exception) {
                    try {
                        sendToOpenRouterFallback()
                    } catch (openRouterException: Exception) {
                        val errorText = openRouterException.message ?: openRouterException.toString()
                        if (errorText.contains("401")) {
                            Pair("Errore: Impossibile contattare OpenRouter. Verifica di aver inserito correttamente la chiave OPENROUTER_API_KEY nel file .env (Errore 401). L'errore originale di Gemini era: ${e.message}", emptyList())
                        } else {
                            Pair("Errore di rete con OpenRouter: $errorText. L'errore originale di Gemini era: ${e.message}", emptyList())
                        }
                    }
                }

                var responseText = responsePair.first
                val functionCalls = responsePair.second

                functionCalls.forEach { call ->
                    when (call.name) {
                        "create_event" -> {
                            val args = call.args
                            val event = CalendarEvent(
                                day = args["day"]?.toString()?.toFloatOrNull()?.toInt() ?: 1,
                                month = args["month"]?.toString() ?: "January",
                                year = args["year"]?.toString()?.toFloatOrNull()?.toInt() ?: Calendar.getInstance().get(Calendar.YEAR),
                                title = args["title"]?.toString()?.uppercase() ?: "NEW EVENT",
                                time = normalizeEventTimeRange(args["time"]?.toString() ?: "12:00 PM")
                            )
                            val overlap = findOverlappingEvent(event, currentEvents)
                            if (overlap != null) {
                                onCollision(event, overlap, false)
                                responseText = "Collision: ${event.title} (${event.time}) overlaps with ${overlap.title} (${overlap.time}) on ${event.month} ${event.day}, ${event.year}."
                            } else {
                                onAddEvent(event)
                                if (responseText.isBlank()) responseText = "Created event: ${event.title}."
                            }
                        }
                        "delete_event" -> {
                            val args = call.args
                            val id = args["eventId"]?.toString() ?: args.values.firstOrNull()?.toString()
                            if (id != null) {
                                onRemoveEvent(id)
                                if (responseText.isBlank()) responseText = "Deleted the requested event."
                            }
                        }
                        "modify_event" -> {
                            val args = call.args
                            val id = args["eventId"]?.toString()
                            if (id != null) {
                                val existingEvent = currentEvents.firstOrNull { it.id == id }
                                val modifiedEvent = CalendarEvent(
                                    id = id,
                                    day = args["day"]?.toString()?.toFloatOrNull()?.toInt() ?: 1,
                                    month = args["month"]?.toString() ?: "January",
                                    year = args["year"]?.toString()?.toFloatOrNull()?.toInt() ?: 2026,
                                    title = args["title"]?.toString()?.uppercase() ?: "UPDATED EVENT",
                                    time = normalizeEventTimeRange(args["time"]?.toString() ?: "12:00 PM"),
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
                                    if (responseText.isBlank()) responseText = "Updated event: ${modifiedEvent.title}."
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
                _chatHistory.value = _chatHistory.value + ChatMessage(text = "An error occurred: ${e.message}", isUser = false)
            }
        }
    }

    fun addLocalAssistantMessage(message: String) {
        _chatHistory.value = _chatHistory.value + ChatMessage(text = message, isUser = false)
    }
}
