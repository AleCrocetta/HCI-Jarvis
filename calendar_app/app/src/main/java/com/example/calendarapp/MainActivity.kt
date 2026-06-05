package com.example.calendarapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.content.Intent
import android.content.Context
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.calendarapp.model.CalendarEvent
import com.example.calendarapp.ui.addevent.AddEventScreen
import com.example.calendarapp.ui.home.HomeScreen
import com.example.calendarapp.ui.theme.CalendarAppTheme
import java.util.Calendar
import java.util.Locale

private data class MemoryRoutine(
    val label: String,
    val value: String,
    val title: String,
    val weekdays: Set<Int>,
    val repeatsDaily: Boolean,
    val startMinutes: Int,
    val endMinutes: Int
)

private data class PendingMemoryCollision(
    val routine: MemoryRoutine,
    val preferences: List<String>,
    val routineEvent: CalendarEvent,
    val conflictingEvent: CalendarEvent,
    val movedRoutineEvent: CalendarEvent?,
    val movedExistingEvent: CalendarEvent?
)

private data class PendingEventCollision(
    val newEvent: CalendarEvent,
    val conflictingEvent: CalendarEvent,
    val isModification: Boolean
)

class MainActivity : ComponentActivity() {

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0)
            if (!spokenText.isNullOrBlank()) {
                // We will handle this in the viewmodel via a callback stored in state or passing to VM directly.
                // For simplicity, let's just create a shared flow or static method.
                // To keep it simple in Compose, we can use a callback.
                onSpeechRecognized?.invoke(spokenText)
            }
        }
    }

    private var onSpeechRecognized: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalendarAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Dynamically resolve current system date (e.g. May 18th, 2026)
                    val calendar = remember { Calendar.getInstance() }
                    val currentDay = remember { calendar.get(Calendar.DAY_OF_MONTH) }
                    val currentMonth = remember { calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH) ?: "May" }
                    val currentYear = remember { calendar.get(Calendar.YEAR) }

                    // Dynamically compute the month and year for "next month" to pre-populate heatmap data
                    val nextMonthCalendar = remember {
                        Calendar.getInstance().apply {
                            add(Calendar.MONTH, 1)
                        }
                    }
                    val nextMonth = remember { nextMonthCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH) ?: "June" }
                    val nextMonthYear = remember { nextMonthCalendar.get(Calendar.YEAR) }
                    
                    var selectedDay by remember { mutableIntStateOf(currentDay) }
                    var selectedDayMonth by remember { mutableStateOf(currentMonth) }
                    var selectedDayYear by remember { mutableIntStateOf(currentYear) }
                    var activeTab by remember { mutableStateOf("calendar") }
                    var searchQuery by remember { mutableStateOf("") }
                    var viewAllEvents by remember { mutableStateOf(false) }
                    var selectedMonth by remember { mutableStateOf(currentMonth) }
                    var selectedYear by remember { mutableIntStateOf(currentYear) }
                    var highlightedEventId by remember { mutableStateOf<String?>(null) }
                    val memoryStore = remember { getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE) }
                    var showFirstRunPreferences by remember { mutableStateOf(false) }
                    val userPreferences = remember {
                        val savedPreferences = memoryStore.getString("user_preferences", "").orEmpty()
                        mutableStateListOf<String>().apply {
                            if (savedPreferences.isNotBlank()) {
                                addAll(savedPreferences.lines().filter { it.isNotBlank() })
                            } else {
                                add("Schedule training in the morning")
                                add("Gym sessions at 8:00 AM")
                                add("Meetings must have video link")
                                add("Highlight flight events with indicator")
                            }
                        }
                    }
                    fun saveUserPreferences() {
                        memoryStore.edit()
                            .putString("user_preferences", userPreferences.joinToString("\n"))
                            .putBoolean("preferences_saved", true)
                            .apply()
                    }
                    
                    val eventsList = remember {
                        mutableStateListOf<CalendarEvent>(
                            CalendarEvent(day = currentDay, month = currentMonth, year = currentYear, title = "MORNING WALK", time = "06:30 AM - 07:00 AM"),
                            CalendarEvent(day = currentDay, month = currentMonth, year = currentYear, title = "GYM TIME", time = "08:00 AM - 09:20 AM"),
                            CalendarEvent(day = currentDay, month = currentMonth, year = currentYear, title = "PILOTS MEETING", time = "10:00 AM - 10:30 AM", link = "https://meet.google.com/abc-xyz", fileNames = listOf("MeetingAgenda.pdf")),
                            CalendarEvent(day = if (currentDay > 2) currentDay - 2 else 28, month = currentMonth, year = currentYear, title = "DOCTOR APPOINTMENT", time = "11:00 AM - 12:00 PM"),
                            CalendarEvent(day = if (currentDay < 23) currentDay + 5 else 3, month = currentMonth, year = currentYear, title = "PROJECT SPRINT REVIEW", time = "03:00 PM - 04:30 PM", link = "https://zoom.us/j/sprint-123", fileNames = listOf("SprintBacklog.xlsx")),
                            CalendarEvent(day = if (currentDay < 19) currentDay + 9 else 5, month = currentMonth, year = currentYear, title = "FLIGHT TO NEW YORK", time = "02:00 PM - 06:00 PM", link = "https://flightstatus.com/NYC-778", fileNames = listOf("BoardingPass.pdf", "TicketDetails.txt")),
                            CalendarEvent(day = 15, month = "January", year = currentYear, title = "NEW YEAR PLANNING", time = "10:00 AM - 11:30 AM"),
                            CalendarEvent(day = 10, month = "March", year = currentYear, title = "SPRING CLEANING", time = "09:00 AM - 12:00 PM"),

                            // pre-populate next month with varying counts of tasks to verify calendar heatmap colors
                            // Day 2: 1 task (tests the yellow heatmap: size 1-2)
                            CalendarEvent(day = 2, month = nextMonth, year = nextMonthYear, title = "PREPARE REPORT", time = "09:00 AM - 10:00 AM"),

                            // Day 5: 2 tasks (tests the yellow heatmap: size 1-2)
                            CalendarEvent(day = 5, month = nextMonth, year = nextMonthYear, title = "DENTIST APPOINTMENT", time = "10:30 AM - 11:30 AM"),
                            CalendarEvent(day = 5, month = nextMonth, year = nextMonthYear, title = "GROCERY SHOPPING", time = "05:00 PM - 06:00 PM"),

                            // Day 10: 3 tasks (tests the orange heatmap: size 3-5)
                            CalendarEvent(day = 10, month = nextMonth, year = nextMonthYear, title = "TEAM SYNC", time = "09:00 AM - 09:30 AM"),
                            CalendarEvent(day = 10, month = nextMonth, year = nextMonthYear, title = "CLIENT DEMO", time = "11:00 AM - 12:00 PM"),
                            CalendarEvent(day = 10, month = nextMonth, year = nextMonthYear, title = "STRATEGY MEETING", time = "02:00 PM - 03:00 PM"),

                            // Day 15: 4 tasks (tests the orange heatmap: size 3-5)
                            CalendarEvent(day = 15, month = nextMonth, year = nextMonthYear, title = "CODE REVIEW", time = "10:00 AM - 11:00 AM"),
                            CalendarEvent(day = 15, month = nextMonth, year = nextMonthYear, title = "1ON1 WITH MANAGER", time = "01:30 PM - 02:00 PM"),
                            CalendarEvent(day = 15, month = nextMonth, year = nextMonthYear, title = "PRODUCT LAUNCH PLAN", time = "03:00 PM - 04:00 PM"),
                            CalendarEvent(day = 15, month = nextMonth, year = nextMonthYear, title = "FITNESS HOUR", time = "06:00 PM - 07:00 PM"),

                            // Day 20: 6 tasks (tests the red heatmap: size > 5)
                            CalendarEvent(day = 20, month = nextMonth, year = nextMonthYear, title = "BREAKFAST WITH TEAM", time = "08:00 AM - 09:00 AM"),
                            CalendarEvent(day = 20, month = nextMonth, year = nextMonthYear, title = "DESIGN THINKING WORKSHOP", time = "10:00 AM - 12:00 PM"),
                            CalendarEvent(day = 20, month = nextMonth, year = nextMonthYear, title = "LUNCH WITH PARTNER", time = "12:30 PM - 01:30 PM"),
                            CalendarEvent(day = 20, month = nextMonth, year = nextMonthYear, title = "MARKETING REVIEW", time = "02:00 PM - 03:00 PM"),
                            CalendarEvent(day = 20, month = nextMonth, year = nextMonthYear, title = "HR BRIEFING", time = "03:30 PM - 04:00 PM"),
                            CalendarEvent(day = 20, month = nextMonth, year = nextMonthYear, title = "YOGA SESSION", time = "05:30 PM - 06:30 PM"),

                            // Day 25: 7 tasks (tests the red heatmap: size > 5)
                            CalendarEvent(day = 25, month = nextMonth, year = nextMonthYear, title = "MORNING RUN", time = "07:00 AM - 07:45 AM"),
                            CalendarEvent(day = 25, month = nextMonth, year = nextMonthYear, title = "STANDUP MEETING", time = "09:30 AM - 09:45 AM"),
                            CalendarEvent(day = 25, month = nextMonth, year = nextMonthYear, title = "ARCHITECTURE REVIEW", time = "10:00 AM - 11:30 AM"),
                            CalendarEvent(day = 25, month = nextMonth, year = nextMonthYear, title = "LUNCH AND LEARN", time = "12:00 PM - 01:00 PM"),
                            CalendarEvent(day = 25, month = nextMonth, year = nextMonthYear, title = "BUG BASH", time = "01:30 PM - 03:30 PM"),
                            CalendarEvent(day = 25, month = nextMonth, year = nextMonthYear, title = "STAKEHOLDER SYNC", time = "04:00 PM - 04:30 PM"),
                            CalendarEvent(day = 25, month = nextMonth, year = nextMonthYear, title = "DINNER MEETING", time = "07:30 PM - 09:00 PM")
                        )
                    }
                    val monthsList = listOf(
                        "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"
                    )
                    var previousMemoryPreferences by remember { mutableStateOf(userPreferences.toList()) }
                    var pendingMemoryCollision by remember { mutableStateOf<PendingMemoryCollision?>(null) }
                    var pendingEventCollision by remember { mutableStateOf<PendingEventCollision?>(null) }

                    fun monthEpoch(month: String, year: Int): Int {
                        return year * 12 + monthsList.indexOf(month).coerceAtLeast(0)
                    }

                    fun monthFromEpoch(epoch: Int): Pair<String, Int> {
                        val monthIndex = ((epoch % 12) + 12) % 12
                        val year = Math.floorDiv(epoch, 12)
                        return monthsList[monthIndex] to year
                    }

                    fun activeMemoryWindow(): Set<Pair<String, Int>> {
                        val center = monthEpoch(selectedMonth, selectedYear)
                        return setOf(center - 1, center, center + 1).map { monthFromEpoch(it) }.toSet()
                    }

                    fun formatMemoryTime(totalMinutes: Int): String {
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

                    fun parseClockMinutes(hourText: String, minuteText: String, markerText: String): Int? {
                        var hour = hourText.toIntOrNull() ?: return null
                        val minute = minuteText.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
                        val marker = markerText.uppercase(Locale.US)
                        if (hour !in 1..12 || minute !in 0..59) return null
                        if (marker == "PM" && hour != 12) hour += 12
                        if (marker == "AM" && hour == 12) hour = 0
                        return hour * 60 + minute
                    }

                    fun parseClock24Minutes(hourText: String, minuteText: String): Int? {
                        val hour = hourText.toIntOrNull() ?: return null
                        val minute = minuteText.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
                        if (hour !in 0..23 || minute !in 0..59) return null
                        return hour * 60 + minute
                    }

                    fun normalizedMemoryText(value: String): String {
                        return value
                            .replace(Regex("""\bfoot\s+ball\b""", RegexOption.IGNORE_CASE), "football")
                            .replace(Regex("""\b(\d{1,2})\s+(\d{2})(?=\s*(?:AM|PM|am|pm|\bto\b|-))"""), "$1:$2")
                            .replace(Regex("""\bcalcio\b""", RegexOption.IGNORE_CASE), "football")
                            .replace(Regex("""\bpalestra\b""", RegexOption.IGNORE_CASE), "gym")
                            .replace(Regex("""\ballenamento\b""", RegexOption.IGNORE_CASE), "training")
                            .replace(Regex("""\bstudio\b""", RegexOption.IGNORE_CASE), "study")
                            .replace(Regex("""\blezione\b""", RegexOption.IGNORE_CASE), "class")
                            .replace(Regex("""\blavoro\b""", RegexOption.IGNORE_CASE), "work")
                            .replace(Regex("""\bmattina\b""", RegexOption.IGNORE_CASE), "morning")
                            .replace(Regex("""\bpomeriggio\b""", RegexOption.IGNORE_CASE), "afternoon")
                            .replace(Regex("""\bsera\b""", RegexOption.IGNORE_CASE), "evening")
                            .replace(Regex("""\bnotte\b""", RegexOption.IGNORE_CASE), "night")
                            .replace(Regex("""\bdifficile\b""", RegexOption.IGNORE_CASE), "hard")
                            .replace(Regex("""\bpreferisco\b""", RegexOption.IGNORE_CASE), "prefer")
                            .replace(Regex("""\btutti\s+i\s+giorni\b""", RegexOption.IGNORE_CASE), "every day")
                            .replace(Regex("""\bogni\s+giorno\b""", RegexOption.IGNORE_CASE), "every day")
                            .replace(Regex("""\bgiornalmente\b""", RegexOption.IGNORE_CASE), "every day")
                            .replace(Regex("""\bluned[iì]\b""", RegexOption.IGNORE_CASE), "monday")
                            .replace(Regex("""\bmarted[iì]\b""", RegexOption.IGNORE_CASE), "tuesday")
                            .replace(Regex("""\bmercoled[iì]\b""", RegexOption.IGNORE_CASE), "wednesday")
                            .replace(Regex("""\bgioved[iì]\b""", RegexOption.IGNORE_CASE), "thursday")
                            .replace(Regex("""\bvenerd[iì]\b""", RegexOption.IGNORE_CASE), "friday")
                            .replace(Regex("""\bsabato\b""", RegexOption.IGNORE_CASE), "saturday")
                            .replace(Regex("""\bdomenica\b""", RegexOption.IGNORE_CASE), "sunday")
                    }

                    fun normalizedActivityKey(value: String): String {
                        return normalizedMemoryText(value)
                            .lowercase(Locale.US)
                            .replace(Regex("""\b(i|usually|normally|do|have|play|go|to|the|at|on|every|and|from|regular|routine)\b"""), " ")
                            .replace(Regex("""\bday|daily\b"""), " ")
                            .replace(Regex("""\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)\b"""), " ")
                            .replace(Regex("""\b\d{1,2}(?::\d{2})?\s*(am|pm)?\s*(-|to)?\s*\d{0,2}(?::\d{2})?\s*(am|pm)?\b"""), " ")
                            .replace(Regex("""[^a-z0-9]+"""), " ")
                            .trim()
                    }

                    fun parseMemoryTimeRange(value: String): IntRange? {
                        val normalizedValue = normalizedMemoryText(value)
                        val rangeMatch = Regex("""\b(?:from\s+)?(\d{1,2})(?::(\d{2}))?\s*(AM|PM)?\s*(?:-|to)\s*(\d{1,2})(?::(\d{2}))?\s*(AM|PM)\b""", RegexOption.IGNORE_CASE)
                            .find(normalizedValue)
                        if (rangeMatch != null) {
                            val endMarker = rangeMatch.groupValues[6]
                            val startMarker = rangeMatch.groupValues[3].ifBlank { endMarker }
                            val start = parseClockMinutes(rangeMatch.groupValues[1], rangeMatch.groupValues[2], startMarker) ?: return null
                            val end = parseClockMinutes(rangeMatch.groupValues[4], rangeMatch.groupValues[5], endMarker) ?: return null
                            if (end > start) return start until end
                        }
                        val range24Match = Regex("""\b(?:from|dalle|da|alle)?\s*(\d{1,2})(?::(\d{2}))?\s*(?:-|to|alle|a)\s*(\d{1,2})(?::(\d{2}))\b""", RegexOption.IGNORE_CASE)
                            .find(normalizedValue)
                        if (range24Match != null) {
                            val start = parseClock24Minutes(range24Match.groupValues[1], range24Match.groupValues[2]) ?: return null
                            val end = parseClock24Minutes(range24Match.groupValues[3], range24Match.groupValues[4]) ?: return null
                            if (end > start) return start until end
                        }
                        val singleMatch = Regex("""\b(\d{1,2})(?::(\d{2}))?\s*(AM|PM)\b""", RegexOption.IGNORE_CASE).find(normalizedValue)
                        if (singleMatch != null) {
                            val start = parseClockMinutes(singleMatch.groupValues[1], singleMatch.groupValues[2], singleMatch.groupValues[3]) ?: return null
                            return start until (start + 60)
                        }
                        val single24Match = Regex("""\b(?:alle|at)\s*(\d{1,2})(?::(\d{2}))?\b""", RegexOption.IGNORE_CASE).find(normalizedValue) ?: return null
                        val start = parseClock24Minutes(single24Match.groupValues[1], single24Match.groupValues[2]) ?: return null
                        return start until (start + 60)
                    }

                    fun parseEventTimeRange(time: String): IntRange? {
                        return parseMemoryTimeRange(time)
                    }

                    fun rangesOverlap(first: IntRange, second: IntRange): Boolean {
                        return first.first <= second.last && second.first <= first.last
                    }

                    fun memoryEventTitle(label: String, value: String): String {
                        val normalizedValue = normalizedMemoryText(value)
                        val genericLabels = setOf(
                            "memory setting",
                            "task difficulty preference",
                            "sports routine",
                            "daily study time",
                            "work or class schedule",
                            "sleeping habits",
                            "meal habits",
                            "commute / travel time",
                            "break preferences",
                            "planning style",
                            "extra notes"
                        )
                        if (label.isNotBlank() && label.lowercase(Locale.US) !in genericLabels && !label.startsWith("Memory ", ignoreCase = true)) {
                            return label.uppercase(Locale.US)
                        }
                        val dayPattern = Regex("""\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)\b""", RegexOption.IGNORE_CASE)
                        val timePattern = Regex("""\b\d{1,2}(?::\d{2})?\s*(AM|PM)?\s*(?:-|to)?\s*\d{0,2}(?::\d{2})?\s*(AM|PM)?\b""", RegexOption.IGNORE_CASE)
                        val stopIndex = listOfNotNull(
                            dayPattern.find(normalizedValue)?.range?.first,
                            timePattern.find(normalizedValue)?.range?.first
                        ).minOrNull() ?: normalizedValue.length
                        val title = normalizedValue.take(stopIndex)
                            .replace(Regex("""\b(i|usually|normally|do|have|play|go|to|the|at|on|every|and|from)\b""", RegexOption.IGNORE_CASE), " ")
                            .replace(Regex("""\s+"""), " ")
                            .trim()
                        return title.ifBlank { label }.uppercase(Locale.US)
                    }

                    fun parseMemoryRoutines(preferences: List<String>): List<MemoryRoutine> {
                        val weekdayMap = mapOf(
                            "sunday" to Calendar.SUNDAY,
                            "sun" to Calendar.SUNDAY,
                            "monday" to Calendar.MONDAY,
                            "mon" to Calendar.MONDAY,
                            "tuesday" to Calendar.TUESDAY,
                            "tue" to Calendar.TUESDAY,
                            "tues" to Calendar.TUESDAY,
                            "wednesday" to Calendar.WEDNESDAY,
                            "wed" to Calendar.WEDNESDAY,
                            "thursday" to Calendar.THURSDAY,
                            "thu" to Calendar.THURSDAY,
                            "thur" to Calendar.THURSDAY,
                            "thurs" to Calendar.THURSDAY,
                            "friday" to Calendar.FRIDAY,
                            "fri" to Calendar.FRIDAY,
                            "saturday" to Calendar.SATURDAY,
                            "sat" to Calendar.SATURDAY
                        )
                        return preferences.mapNotNull { preference ->
                            val label = preference.substringBefore(":").trim().ifBlank { "Memory setting" }
                            val value = normalizedMemoryText(preference.substringAfter(":", preference).trim())
                            val repeatsDaily = Regex("""\b(every\s+day|daily)\b""", RegexOption.IGNORE_CASE).containsMatchIn(value)
                            val weekdays = weekdayMap.filterKeys { day ->
                                Regex("""\b$day\b""", RegexOption.IGNORE_CASE).containsMatchIn(value)
                            }.values.toSet()
                            val range = parseMemoryTimeRange(value)
                            if ((!repeatsDaily && weekdays.isEmpty()) || range == null || value.equals("Not specified", ignoreCase = true)) {
                                null
                            } else {
                                MemoryRoutine(
                                    label = label,
                                    value = value,
                                    title = memoryEventTitle(label, value),
                                    weekdays = weekdays,
                                    repeatsDaily = repeatsDaily,
                                    startMinutes = range.first,
                                    endMinutes = range.last + 1
                                )
                            }
                        }
                    }

                    fun memoryEventId(routine: MemoryRoutine, month: String, year: Int, day: Int, startMinutes: Int = routine.startMinutes): String {
                        val key = "${routine.label}:${routine.title}:$year:$month:$day:$startMinutes"
                            .lowercase(Locale.US)
                            .replace(Regex("""[^a-z0-9]+"""), "-")
                            .trim('-')
                        return "memory:$key"
                    }

                    fun memoryEventsForMonth(
                        preferences: List<String>,
                        month: String,
                        year: Int,
                        routines: List<MemoryRoutine> = parseMemoryRoutines(preferences)
                    ): List<CalendarEvent> {
                        val monthIndex = monthsList.indexOf(month)
                        if (monthIndex == -1) return emptyList()
                        val monthCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, monthIndex)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        return routines.flatMap { routine ->
                            (1..daysInMonth).mapNotNull { day ->
                                monthCalendar.set(Calendar.DAY_OF_MONTH, day)
                                if (routine.repeatsDaily || monthCalendar.get(Calendar.DAY_OF_WEEK) in routine.weekdays) {
                                    CalendarEvent(
                                        id = memoryEventId(routine, month, year, day),
                                        day = day,
                                        month = month,
                                        year = year,
                                        title = routine.title,
                                        time = "${formatMemoryTime(routine.startMinutes)} - ${formatMemoryTime(routine.endMinutes)}",
                                        priority = "Low"
                                    )
                                } else {
                                    null
                                }
                            }
                        }
                    }

                    fun memoryEventsForWindow(preferences: List<String>): List<CalendarEvent> {
                        val routines = parseMemoryRoutines(preferences)
                        val activeEvents = activeMemoryWindow().flatMap { (month, year) ->
                            memoryEventsForMonth(preferences, month, year, routines)
                        }
                        val dailyRoutines = routines.filter { it.repeatsDaily }
                        val yearlyDailyEvents = if (dailyRoutines.isEmpty()) {
                            emptyList()
                        } else {
                            monthsList.flatMap { month ->
                                memoryEventsForMonth(preferences, month, selectedYear, dailyRoutines)
                            }
                        }
                        return (activeEvents + yearlyDailyEvents).distinctBy { it.id }
                    }

                    fun removeOldUntaggedMemoryEvents(oldPreferences: List<String>) {
                        val loadedWindows = eventsList.map { it.month to it.year }.toSet() + activeMemoryWindow()
                        val oldGenerated = loadedWindows.flatMap { (month, year) -> memoryEventsForMonth(oldPreferences, month, year) }
                        eventsList.removeAll { event ->
                            !event.id.startsWith("memory:") && oldGenerated.any { generated ->
                                event.title == generated.title &&
                                    event.time == generated.time &&
                                    event.day == generated.day &&
                                    event.month.equals(generated.month, ignoreCase = true) &&
                                    event.year == generated.year
                            }
                        }
                    }

                    fun refreshMemoryEvents(preferences: List<String>, cleanupOldPreferences: List<String>? = null) {
                        cleanupOldPreferences?.let { removeOldUntaggedMemoryEvents(it) }
                        val activeWindow = activeMemoryWindow()
                        val generatedEvents = memoryEventsForWindow(preferences)
                        val generatedIds = generatedEvents.map { it.id }.toSet()
                        val generatedWindow = generatedEvents.map { it.month to it.year }.toSet() + activeWindow
                        eventsList.removeAll { event ->
                            event.id.startsWith("memory:") &&
                                ((event.month to event.year) !in generatedWindow || event.id !in generatedIds || generatedEvents.none { generated ->
                                    generated.title == event.title &&
                                        generated.day == event.day &&
                                        generated.month.equals(event.month, ignoreCase = true) &&
                                        generated.year == event.year &&
                                        generated.time == event.time
                                })
                        }
                        generatedEvents.forEach { memoryEvent ->
                            val collision = eventsList.any { event ->
                                !event.id.startsWith("memory:") &&
                                    event.day == memoryEvent.day &&
                                    event.month.equals(memoryEvent.month, ignoreCase = true) &&
                                    event.year == memoryEvent.year &&
                                    parseEventTimeRange(event.time)?.let { existingRange ->
                                        parseEventTimeRange(memoryEvent.time)?.let { memoryRange -> rangesOverlap(memoryRange, existingRange) }
                                    } == true
                            }
                            if (!collision && eventsList.none { it.id == memoryEvent.id }) {
                                eventsList.add(memoryEvent)
                            }
                        }
                    }

                    fun findBestOpenSlot(day: Int, month: String, year: Int, duration: Int, preferredStart: Int, ignoreEventId: String? = null): Int? {
                        val occupied = eventsList.filter { event ->
                            event.id != ignoreEventId &&
                                event.day == day &&
                                event.month.equals(month, ignoreCase = true) &&
                                event.year == year
                        }.mapNotNull { parseEventTimeRange(it.time) }
                        val dayStart = 7 * 60
                        val dayEnd = 23 * 60
                        return generateSequence(0) { it + 15 }
                            .takeWhile { dayStart + it + duration <= dayEnd }
                            .map { dayStart + it }
                            .filter { start -> occupied.none { rangesOverlap(start until (start + duration), it) } }
                            .minByOrNull { kotlin.math.abs(it - preferredStart) }
                    }

                    fun memoryLabelForText(value: String): String {
                        val normalizedValue = normalizedMemoryText(value)
                        return when {
                            Regex("""\b(hard|harder|difficult|difficulty|demanding|heavy|complex|intense|morning|afternoon|evening)\b""", RegexOption.IGNORE_CASE).containsMatchIn(normalizedValue) -> "Task difficulty preference"
                            Regex("""\b(football|gym|sport|train|training|run|running|yoga|basket|tennis|swim)\b""", RegexOption.IGNORE_CASE).containsMatchIn(normalizedValue) -> "Sports routine"
                            Regex("""\b(study|exam|homework|revision|lesson)\b""", RegexOption.IGNORE_CASE).containsMatchIn(normalizedValue) -> "Daily study time"
                            Regex("""\b(class|course|lecture|university|school|work|job|shift|meeting)\b""", RegexOption.IGNORE_CASE).containsMatchIn(normalizedValue) -> "Work or class schedule"
                            Regex("""\b(sleep|wake|bed|bedtime)\b""", RegexOption.IGNORE_CASE).containsMatchIn(normalizedValue) -> "Sleeping habits"
                            Regex("""\b(lunch|dinner|breakfast|meal|eat)\b""", RegexOption.IGNORE_CASE).containsMatchIn(normalizedValue) -> "Meal habits"
                            Regex("""\b(commute|travel|bus|train|drive|metro)\b""", RegexOption.IGNORE_CASE).containsMatchIn(normalizedValue) -> "Commute / travel time"
                            Regex("""\b(break|pause|rest)\b""", RegexOption.IGNORE_CASE).containsMatchIn(normalizedValue) -> "Break preferences"
                            Regex("""\b(plan|planning|organize|schedule)\b""", RegexOption.IGNORE_CASE).containsMatchIn(normalizedValue) -> "Planning style"
                            else -> "Extra notes"
                        }
                    }

                    fun detectRoutineFromPrompt(prompt: String): MemoryRoutine? {
                        val normalizedPrompt = normalizedMemoryText(prompt)
                        val hasRoutineLanguage = Regex("""\b(remember|memory|routine|regular|weekly|usually|every|monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun|play|do|have|train|gym|football|study|class|work|ricorda|memoria|regolare|settimanale|ogni|gioco|faccio|palestra|calcio|studio|lezione|lavoro)\b""", RegexOption.IGNORE_CASE)
                            .containsMatchIn(normalizedPrompt)
                        if (!hasRoutineLanguage) return null
                        return parseMemoryRoutines(listOf("${memoryLabelForText(normalizedPrompt)}: $normalizedPrompt")).firstOrNull()
                    }

                    fun detectPreferenceFromPrompt(prompt: String): Pair<String, String>? {
                        val isPlanningPreference = Regex("""\b(prefer|preference|like|hard|harder|difficult|difficulty|demanding|heavy|complex|intense|morning|afternoon|evening|night)\b""", RegexOption.IGNORE_CASE)
                            .containsMatchIn(normalizedMemoryText(prompt))
                        if (!isPlanningPreference) return null
                        val label = memoryLabelForText(normalizedMemoryText(prompt))
                        if (label == "Extra notes") return null
                        return label to prompt.trim()
                    }

                    fun updatePreferencesWithRoutine(preferences: List<String>, routine: MemoryRoutine): List<String> {
                        val targetLabel = routine.title.takeIf { it.isNotBlank() && it != "MEMORY SETTING" } ?: memoryLabelForText(routine.value)
                        val activityKey = normalizedActivityKey(routine.value)
                        var replaced = false
                        val updated = preferences.map { preference ->
                            val currentValue = normalizedMemoryText(preference.substringAfter(":", ""))
                            val currentKey = normalizedActivityKey(currentValue)
                            val currentName = preference.substringBefore(":").trim()
                            val sameName = currentName.equals(targetLabel, ignoreCase = true)
                            val sameActivity = activityKey.isNotBlank() && currentKey.isNotBlank() &&
                                (activityKey == currentKey || activityKey in currentKey || currentKey in activityKey)
                            if (sameName || sameActivity) {
                                replaced = true
                                "$targetLabel: ${routine.value}"
                            } else {
                                preference
                            }
                        }
                        return if (replaced) updated else updated + "$targetLabel: ${routine.value}"
                    }

                    fun updatePreferencesWithMemoryValue(preferences: List<String>, label: String, value: String): List<String> {
                        var replaced = false
                        val updated = preferences.map { preference ->
                            if (preference.startsWith("$label:", ignoreCase = true)) {
                                replaced = true
                                "$label: $value"
                            } else {
                                preference
                            }
                        }
                        return if (replaced) updated else updated + "$label: $value"
                    }

                    val deletedEventIndices = remember { mutableStateMapOf<String, Int>() }

                    val jarvisViewModel: com.example.calendarapp.ui.JarvisViewModel = viewModel()
                    val chatHistory by jarvisViewModel.chatHistory.collectAsState(initial = emptyList())

                    fun weekdayNames(weekdays: Set<Int>): String {
                        val names = listOf(
                            Calendar.MONDAY to "Monday",
                            Calendar.TUESDAY to "Tuesday",
                            Calendar.WEDNESDAY to "Wednesday",
                            Calendar.THURSDAY to "Thursday",
                            Calendar.FRIDAY to "Friday",
                            Calendar.SATURDAY to "Saturday",
                            Calendar.SUNDAY to "Sunday"
                        )
                        return names.filter { it.first in weekdays }.joinToString(" and ") { it.second }
                    }

                    fun movedRoutinePreference(routine: MemoryRoutine, startMinutes: Int): MemoryRoutine {
                        val duration = routine.endMinutes - routine.startMinutes
                        val value = "${routine.title.lowercase(Locale.US)} ${weekdayNames(routine.weekdays)} from ${formatMemoryTime(startMinutes)} to ${formatMemoryTime(startMinutes + duration)}"
                        return routine.copy(value = value, startMinutes = startMinutes, endMinutes = startMinutes + duration)
                    }

                    fun applyPreferences(preferences: List<String>) {
                        val oldPreferences = previousMemoryPreferences
                        userPreferences.clear()
                        userPreferences.addAll(preferences)
                        saveUserPreferences()
                        refreshMemoryEvents(preferences, cleanupOldPreferences = oldPreferences)
                        previousMemoryPreferences = preferences
                    }

                    fun focusEvent(event: CalendarEvent) {
                        highlightedEventId = event.id
                        selectedDay = event.day
                        selectedDayMonth = event.month
                        selectedDayYear = event.year
                        selectedMonth = event.month
                        selectedYear = event.year
                        viewAllEvents = false
                    }

                    fun findConflictingEvent(candidate: CalendarEvent, ignoreEventId: String? = null): CalendarEvent? {
                        val candidateRange = parseEventTimeRange(candidate.time) ?: return null
                        return eventsList.firstOrNull { event ->
                            event.id != ignoreEventId &&
                                event.day == candidate.day &&
                                event.month.equals(candidate.month, ignoreCase = true) &&
                                event.year == candidate.year &&
                                parseEventTimeRange(event.time)?.let { existingRange -> rangesOverlap(candidateRange, existingRange) } == true
                        }
                    }

                    fun dayOfWeekForEvent(event: CalendarEvent): Int? {
                        val monthIndex = monthsList.indexOfFirst { it.equals(event.month, ignoreCase = true) }
                        if (monthIndex == -1) return null
                        return runCatching {
                            Calendar.getInstance().apply {
                                set(Calendar.YEAR, event.year)
                                set(Calendar.MONTH, monthIndex)
                                set(Calendar.DAY_OF_MONTH, event.day)
                            }.get(Calendar.DAY_OF_WEEK)
                        }.getOrNull()
                    }

                    fun weekdayName(weekday: Int): String {
                        return when (weekday) {
                            Calendar.MONDAY -> "Monday"
                            Calendar.TUESDAY -> "Tuesday"
                            Calendar.WEDNESDAY -> "Wednesday"
                            Calendar.THURSDAY -> "Thursday"
                            Calendar.FRIDAY -> "Friday"
                            Calendar.SATURDAY -> "Saturday"
                            Calendar.SUNDAY -> "Sunday"
                            else -> "Monday"
                        }
                    }

                    fun routineFromEvent(event: CalendarEvent): MemoryRoutine? {
                        val range = parseEventTimeRange(event.time) ?: return null
                        val weekday = dayOfWeekForEvent(event) ?: return null
                        val title = event.title.uppercase(Locale.US)
                        val value = "${title.lowercase(Locale.US)} ${weekdayName(weekday)} from ${formatMemoryTime(range.first)} to ${formatMemoryTime(range.last + 1)}"
                        return MemoryRoutine(
                            label = title,
                            value = value,
                            title = title,
                            weekdays = setOf(weekday),
                            repeatsDaily = false,
                            startMinutes = range.first,
                            endMinutes = range.last + 1
                        )
                    }

                    fun sameRoutineSlot(first: CalendarEvent, second: CalendarEvent): Boolean {
                        return first.title.equals(second.title, ignoreCase = true) &&
                            first.time == second.time &&
                            dayOfWeekForEvent(first) == dayOfWeekForEvent(second)
                    }

                    fun maybeSaveRepeatedRoutine(event: CalendarEvent) {
                        if (event.id.startsWith("memory:")) return
                        val routine = routineFromEvent(event) ?: return
                        val alreadySaved = parseMemoryRoutines(userPreferences.toList()).any { saved ->
                            saved.title.equals(routine.title, ignoreCase = true) &&
                                saved.startMinutes == routine.startMinutes &&
                                saved.endMinutes == routine.endMinutes &&
                                routine.weekdays.any { it in saved.weekdays }
                        }
                        if (alreadySaved) return
                        val repeatedDates = eventsList
                            .filter { !it.id.startsWith("memory:") && sameRoutineSlot(it, event) }
                            .map { "${it.year}-${it.month}-${it.day}" }
                            .toSet()
                        if (repeatedDates.size >= 2) {
                            applyPreferences(updatePreferencesWithRoutine(userPreferences.toList(), routine))
                            jarvisViewModel.addLocalAssistantMessage("Success: ${routine.title} at ${event.time} repeats on ${weekdayName(routine.weekdays.first())}, so I saved it to memory as a routine.")
                        }
                    }

                    fun syncRoutineRename(oldEvent: CalendarEvent, newTitle: String): Boolean {
                        if (!oldEvent.id.startsWith("memory:") || oldEvent.title == newTitle) return false
                        val oldRoutine = routineFromEvent(oldEvent) ?: return false
                        val updatedRoutine = oldRoutine.copy(
                            label = newTitle.uppercase(Locale.US),
                            title = newTitle.uppercase(Locale.US),
                            value = oldRoutine.value.replace(oldRoutine.title.lowercase(Locale.US), newTitle.lowercase(Locale.US), ignoreCase = true)
                        )
                        var replaced = false
                        val oldActivityKey = normalizedActivityKey(oldRoutine.value)
                        val updatedPreferences = userPreferences.map { preference ->
                            val name = preference.substringBefore(":").trim()
                            val value = normalizedMemoryText(preference.substringAfter(":", preference).trim())
                            val sameName = name.equals(oldRoutine.title, ignoreCase = true)
                            val sameRoutine = oldActivityKey.isNotBlank() && normalizedActivityKey(value) == oldActivityKey
                            if (sameName || sameRoutine) {
                                replaced = true
                                "${updatedRoutine.title}: ${updatedRoutine.value}"
                            } else {
                                preference
                            }
                        }
                        applyPreferences(if (replaced) updatedPreferences else updatedPreferences + "${updatedRoutine.title}: ${updatedRoutine.value}")
                        eventsList.firstOrNull {
                            it.title == updatedRoutine.title &&
                                it.day == oldEvent.day &&
                                it.month.equals(oldEvent.month, ignoreCase = true) &&
                                it.year == oldEvent.year &&
                                it.time == oldEvent.time
                        }?.let { focusEvent(it) }
                        jarvisViewModel.addLocalAssistantMessage("Success: renamed the ${oldRoutine.title} routine to ${updatedRoutine.title} across the calendar.")
                        return true
                    }

                    fun saveEventToCalendar(event: CalendarEvent, isModification: Boolean) {
                        if (isModification) {
                            val index = eventsList.indexOfFirst { it.id == event.id }
                            if (index != -1) {
                                if (syncRoutineRename(eventsList[index], event.title)) {
                                    return
                                }
                                eventsList[index] = event
                            } else {
                                eventsList.add(event)
                            }
                        } else {
                            eventsList.add(event)
                        }
                        focusEvent(event)
                        if (!isModification) {
                            maybeSaveRepeatedRoutine(event)
                        }
                    }

                    fun eventDurationMinutes(event: CalendarEvent): Int? {
                        return parseEventTimeRange(event.time)?.let { range -> range.last - range.first + 1 }
                    }

                    fun resolvePendingEventCollision(choice: String) {
                        val memoryProposal = pendingMemoryCollision
                        if (memoryProposal != null) {
                            when (choice) {
                                "cancel" -> {
                                    pendingMemoryCollision = null
                                    pendingEventCollision = null
                                    jarvisViewModel.addLocalAssistantMessage("Success: cancelled ${memoryProposal.routine.title}. The calendar was left unchanged.")
                                }
                                "keep" -> {
                                    applyPreferences(memoryProposal.preferences)
                                    if (eventsList.none { it.id == memoryProposal.routineEvent.id }) {
                                        eventsList.add(memoryProposal.routineEvent)
                                    }
                                    focusEvent(memoryProposal.routineEvent)
                                    pendingMemoryCollision = null
                                    pendingEventCollision = null
                                    jarvisViewModel.addLocalAssistantMessage("Success: kept both ${memoryProposal.routine.title} and ${memoryProposal.conflictingEvent.title}.")
                                }
                                "replace" -> {
                                    eventsList.removeAll { it.id == memoryProposal.conflictingEvent.id }
                                    applyPreferences(memoryProposal.preferences)
                                    eventsList.firstOrNull { it.id == memoryProposal.routineEvent.id }?.let { focusEvent(it) }
                                    pendingMemoryCollision = null
                                    pendingEventCollision = null
                                    jarvisViewModel.addLocalAssistantMessage("Success: replaced ${memoryProposal.conflictingEvent.title} with ${memoryProposal.routine.title}.")
                                }
                                "reschedule" -> {
                                    val moved = memoryProposal.movedExistingEvent
                                    if (moved == null) {
                                        pendingMemoryCollision = null
                                        pendingEventCollision = null
                                        jarvisViewModel.addLocalAssistantMessage("Missing info: no free slot was found to reschedule ${memoryProposal.conflictingEvent.title}. No calendar changes were made.")
                                    } else {
                                        val index = eventsList.indexOfFirst { it.id == moved.id }
                                        if (index != -1) eventsList[index] = moved
                                        applyPreferences(memoryProposal.preferences)
                                        eventsList.firstOrNull { it.id == memoryProposal.routineEvent.id }?.let { focusEvent(it) }
                                        pendingMemoryCollision = null
                                        pendingEventCollision = null
                                        jarvisViewModel.addLocalAssistantMessage("Success: saved ${memoryProposal.routine.title} and rescheduled ${moved.title} to ${moved.time}.")
                                    }
                                }
                            }
                            return
                        }
                        val proposal = pendingEventCollision ?: return
                        when (choice) {
                            "cancel" -> {
                                pendingEventCollision = null
                                jarvisViewModel.addLocalAssistantMessage("Success: cancelled ${proposal.newEvent.title}. The calendar was left unchanged.")
                            }
                            "keep" -> {
                                saveEventToCalendar(proposal.newEvent, proposal.isModification)
                                pendingEventCollision = null
                                jarvisViewModel.addLocalAssistantMessage("Success: kept both ${proposal.newEvent.title} and ${proposal.conflictingEvent.title}.")
                            }
                            "replace" -> {
                                eventsList.removeAll { it.id == proposal.conflictingEvent.id }
                                saveEventToCalendar(proposal.newEvent, proposal.isModification)
                                pendingEventCollision = null
                                jarvisViewModel.addLocalAssistantMessage("Success: replaced ${proposal.conflictingEvent.title} with ${proposal.newEvent.title}.")
                            }
                            "reschedule" -> {
                                val duration = eventDurationMinutes(proposal.conflictingEvent)
                                val newStart = duration?.let {
                                    findBestOpenSlot(
                                        day = proposal.conflictingEvent.day,
                                        month = proposal.conflictingEvent.month,
                                        year = proposal.conflictingEvent.year,
                                        duration = it,
                                        preferredStart = parseEventTimeRange(proposal.conflictingEvent.time)?.first ?: (7 * 60),
                                        ignoreEventId = proposal.conflictingEvent.id
                                    )
                                }
                                if (duration == null || newStart == null) {
                                    pendingEventCollision = null
                                    jarvisViewModel.addLocalAssistantMessage("Missing info: no free slot was found to reschedule ${proposal.conflictingEvent.title}. No calendar changes were made.")
                                } else {
                                    saveEventToCalendar(proposal.newEvent, proposal.isModification)
                                    val index = eventsList.indexOfFirst { it.id == proposal.conflictingEvent.id }
                                    if (index != -1) {
                                        eventsList[index] = proposal.conflictingEvent.copy(time = "${formatMemoryTime(newStart)} - ${formatMemoryTime(newStart + duration)}")
                                    }
                                    pendingEventCollision = null
                                    jarvisViewModel.addLocalAssistantMessage("Success: saved ${proposal.newEvent.title} and rescheduled ${proposal.conflictingEvent.title}.")
                                }
                            }
                        }
                    }

                    fun firstRoutineCollision(routine: MemoryRoutine, preferences: List<String>): PendingMemoryCollision? {
                        val routineEvent = memoryEventsForWindow(preferences).firstOrNull { event ->
                            parseEventTimeRange(event.time)?.let { routineRange ->
                                eventsList.any { existing ->
                                    !existing.id.startsWith("memory:") &&
                                        existing.day == event.day &&
                                        existing.month.equals(event.month, ignoreCase = true) &&
                                        existing.year == event.year &&
                                        parseEventTimeRange(existing.time)?.let { existingRange -> rangesOverlap(routineRange, existingRange) } == true
                                }
                            } == true
                        } ?: return null
                        val conflict = eventsList.first { existing ->
                            !existing.id.startsWith("memory:") &&
                                existing.day == routineEvent.day &&
                                existing.month.equals(routineEvent.month, ignoreCase = true) &&
                                existing.year == routineEvent.year &&
                                parseEventTimeRange(existing.time)?.let { existingRange ->
                                    parseEventTimeRange(routineEvent.time)?.let { routineRange -> rangesOverlap(routineRange, existingRange) }
                                } == true
                        }
                        val duration = routine.endMinutes - routine.startMinutes
                        val movedRoutineStart = findBestOpenSlot(routineEvent.day, routineEvent.month, routineEvent.year, duration, routine.startMinutes)
                        val movedExistingStart = parseEventTimeRange(conflict.time)?.let { existingRange ->
                            findBestOpenSlot(conflict.day, conflict.month, conflict.year, existingRange.last - existingRange.first + 1, existingRange.first, ignoreEventId = conflict.id)
                        }
                        return PendingMemoryCollision(
                            routine = routine,
                            preferences = preferences,
                            routineEvent = routineEvent,
                            conflictingEvent = conflict,
                            movedRoutineEvent = movedRoutineStart?.let { routineEvent.copy(time = "${formatMemoryTime(it)} - ${formatMemoryTime(it + duration)}") },
                            movedExistingEvent = movedExistingStart?.let {
                                val existingRange = parseEventTimeRange(conflict.time) ?: return@let null
                                conflict.copy(time = "${formatMemoryTime(it)} - ${formatMemoryTime(it + existingRange.last - existingRange.first + 1)}")
                            }
                        )
                    }

                    fun describeCollision(proposal: PendingMemoryCollision): String {
                        val routineMove = proposal.movedRoutineEvent?.let { "1. Move ${proposal.routine.title} to ${it.time}." } ?: "1. No open slot found for moving the routine."
                        val taskMove = proposal.movedExistingEvent?.let { "2. Move ${proposal.conflictingEvent.title} to ${it.time}." } ?: "2. No open slot found for moving the existing task."
                        return "Your memory routine ${proposal.routine.title} (${proposal.routineEvent.time}, ${proposal.routineEvent.month} ${proposal.routineEvent.day}) collides with ${proposal.conflictingEvent.title} (${proposal.conflictingEvent.time}).\n$routineMove\n$taskMove\n3. Keep it unscheduled.\nReply with 1, 2, or 3."
                    }

                    fun handlePendingCollisionChoice(text: String): Boolean {
                        val proposal = pendingMemoryCollision ?: return false
                        val normalized = text.lowercase(Locale.US)
                        when {
                            normalized == "1" || "move routine" in normalized || "routine" in normalized -> {
                                val moved = proposal.movedRoutineEvent
                                if (moved == null) {
                                    jarvisViewModel.addLocalAssistantMessage("I could not find an open slot for the routine, so I left the memory unchanged.")
                                } else {
                                    val movedRoutine = movedRoutinePreference(proposal.routine, parseEventTimeRange(moved.time)?.first ?: proposal.routine.startMinutes)
                                    applyPreferences(updatePreferencesWithRoutine(proposal.preferences, movedRoutine))
                                    jarvisViewModel.addLocalAssistantMessage("Updated memory and moved ${proposal.routine.title} to ${moved.time}.")
                                }
                                pendingMemoryCollision = null
                                return true
                            }
                            normalized == "2" || "move existing" in normalized || "existing" in normalized || "task" in normalized -> {
                                val moved = proposal.movedExistingEvent
                                if (moved == null) {
                                    jarvisViewModel.addLocalAssistantMessage("I could not find an open slot for the existing task, so I left the memory unchanged.")
                                } else {
                                    val index = eventsList.indexOfFirst { it.id == moved.id }
                                    if (index != -1) eventsList[index] = moved
                                    applyPreferences(proposal.preferences)
                                    jarvisViewModel.addLocalAssistantMessage("Moved ${moved.title} to ${moved.time} and saved the routine in memory.")
                                }
                                pendingMemoryCollision = null
                                return true
                            }
                            normalized == "3" || "keep" in normalized || "cancel" in normalized || "no" in normalized -> {
                                pendingMemoryCollision = null
                                jarvisViewModel.addLocalAssistantMessage("I left the routine unscheduled and did not change memory.")
                                return true
                            }
                        }
                        jarvisViewModel.addLocalAssistantMessage("Please reply with 1 to move the routine, 2 to move the existing task, or 3 to keep it unscheduled.")
                        return true
                    }

                    fun handleLocalRoutineInput(text: String): Boolean {
                        if (handlePendingCollisionChoice(text)) return true
                        val routine = detectRoutineFromPrompt(text)
                        if (routine == null) {
                            return false
                        }
                        val preferences = updatePreferencesWithRoutine(userPreferences.toList(), routine)
                        val proposal = firstRoutineCollision(routine, preferences)
                        if (proposal != null) {
                            pendingMemoryCollision = proposal
                            pendingEventCollision = PendingEventCollision(proposal.routineEvent, proposal.conflictingEvent, false)
                        } else {
                            applyPreferences(preferences)
                            jarvisViewModel.addLocalAssistantMessage("Updated memory: ${routine.value}. I loaded matching routine events into the current timeline window.")
                        }
                        return true
                    }

                    fun sendToJarvis(text: String) {
                        if (handleLocalRoutineInput(text)) return
                        jarvisViewModel.handleUserPrompt(
                            prompt = text,
                            currentEvents = eventsList.toList(),
                            userPreferences = userPreferences.toList(),
                            onAddEvent = { event -> 
                                saveEventToCalendar(event, false)
                            },
                            onRemoveEvent = { id ->
                                val removedEvent = eventsList.firstOrNull { it.id == id }
                                eventsList.removeAll { it.id == id }
                                highlightedEventId = null
                                if (removedEvent != null) {
                                    selectedDay = removedEvent.day
                                    selectedDayMonth = removedEvent.month
                                    selectedDayYear = removedEvent.year
                                    selectedMonth = removedEvent.month
                                    selectedYear = removedEvent.year
                                    viewAllEvents = false
                                }
                            },
                            onModifyEvent = { modifiedEvent ->
                                saveEventToCalendar(modifiedEvent, true)
                            },
                            onCollision = { newEvent, conflictingEvent, isModification ->
                                pendingEventCollision = PendingEventCollision(newEvent, conflictingEvent, isModification)
                            }
                        )
                    }

                    LaunchedEffect(selectedMonth, selectedYear, userPreferences.toList()) {
                        refreshMemoryEvents(userPreferences.toList())
                    }

                    onSpeechRecognized = { text -> sendToJarvis(text) }

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                selectedDay = selectedDay,
                                selectedDayMonth = selectedDayMonth,
                                selectedDayYear = selectedDayYear,
                                onDaySelected = { day, month, year -> 
                                    selectedDay = day
                                    selectedDayMonth = month
                                    selectedDayYear = year
                                    // Turn off "view all" if they click a specific day to keep navigation natural
                                    viewAllEvents = false 
                                },
                                events = eventsList.toList(),
                                onDeleteEvent = { event ->
                                    val index = eventsList.indexOfFirst { it.id == event.id }
                                    if (index != -1) {
                                        deletedEventIndices[event.id] = index
                                        eventsList.removeAt(index)
                                    }
                                },
                                onRestoreEvent = { event ->
                                    val originalIndex = deletedEventIndices[event.id]
                                    if (originalIndex != null && originalIndex <= eventsList.size) {
                                        eventsList.add(originalIndex, event)
                                    } else {
                                        eventsList.add(event)
                                    }
                                    deletedEventIndices.remove(event.id)
                                },
                                onCompleteEvent = { event ->
                                    val index = eventsList.indexOfFirst { it.id == event.id }
                                    if (index != -1) {
                                        eventsList[index] = eventsList[index].copy(isCompleted = !eventsList[index].isCompleted)
                                    }
                                },
                                onEditEvent = { event ->
                                    val index = eventsList.indexOfFirst { it.id == event.id }
                                    if (index != -1) {
                                        val oldEvent = eventsList[index]
                                        val conflict = findConflictingEvent(event, ignoreEventId = event.id)
                                        if (conflict != null) {
                                            pendingEventCollision = PendingEventCollision(event, conflict, true)
                                        } else if (!syncRoutineRename(oldEvent, event.title)) {
                                            eventsList[index] = event
                                            highlightedEventId = event.id
                                            selectedDay = event.day
                                            selectedDayMonth = event.month
                                            selectedDayYear = event.year
                                            selectedMonth = event.month
                                            selectedYear = event.year
                                            viewAllEvents = false
                                            jarvisViewModel.addLocalAssistantMessage("Success: updated ${event.title}.")
                                        }
                                    }
                                },
                                activeTab = activeTab,
                                onTabSelected = { tab -> activeTab = tab },
                                searchQuery = searchQuery,
                                onSearchQueryChanged = { searchQuery = it },
                                viewAllEvents = viewAllEvents,
                                onViewAllChanged = { viewAllEvents = it },
                                selectedMonth = selectedMonth,
                                onMonthSelected = { selectedMonth = it },
                                selectedYear = selectedYear,
                                onYearSelected = { selectedYear = it },
                                onAddEventClick = {
                                    navController.navigate("add_event")
                                },
                                onSendClick = { text -> sendToJarvis(text) },
                                onMicClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Jarvis to add or arrange tasks...")
                                    }
                                    speechRecognizerLauncher.launch(intent)
                                },
                                chatHistory = chatHistory,
                                highlightedEventId = highlightedEventId,
                                userPreferences = userPreferences.toList(),
                                onSavePreferences = { preferences ->
                                    applyPreferences(preferences)
                                },
                                showFirstRunPreferences = showFirstRunPreferences,
                                onFirstRunPreferencesDone = { preferences ->
                                    showFirstRunPreferences = false
                                    applyPreferences(preferences)
                                },
                                pendingCollisionNewEvent = pendingEventCollision?.newEvent,
                                pendingCollisionExistingEvent = pendingEventCollision?.conflictingEvent,
                                onCancelNewEvent = { resolvePendingEventCollision("cancel") },
                                onKeepBothEvents = { resolvePendingEventCollision("keep") },
                                onReplaceExistingEvent = { resolvePendingEventCollision("replace") },
                                onRescheduleExistingEvent = { resolvePendingEventCollision("reschedule") }
                            )
                        }
                        composable("add_event") {
                            AddEventScreen(
                                selectedDay = selectedDay,
                                selectedMonth = selectedMonth,
                                selectedYear = selectedYear,
                                onSaveEvent = { title, time, chosenDay, link, fileNames, fileUris, year, priority ->
                                    val newEvent = CalendarEvent(
                                        day = chosenDay,
                                        month = selectedMonth,
                                        year = year,
                                        title = title.uppercase(),
                                        time = time,
                                        priority = priority,
                                        link = link.takeIf { it.isNotBlank() },
                                        fileNames = fileNames,
                                        fileUris = fileUris
                                    )
                                    val conflict = findConflictingEvent(newEvent)
                                    if (conflict != null) {
                                        pendingEventCollision = PendingEventCollision(newEvent, conflict, false)
                                        selectedDay = chosenDay
                                        selectedDayMonth = selectedMonth
                                        selectedDayYear = year
                                        selectedYear = year
                                    } else {
                                        saveEventToCalendar(newEvent, false)
                                    }
                                    navController.popBackStack()
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
