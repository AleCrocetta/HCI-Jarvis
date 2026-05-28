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

                    val deletedEventIndices = remember { mutableStateMapOf<String, Int>() }

                    val jarvisViewModel: com.example.calendarapp.ui.JarvisViewModel = viewModel()
                    val chatHistory by jarvisViewModel.chatHistory.collectAsState(initial = emptyList())

                    onSpeechRecognized = { text ->
                        jarvisViewModel.handleUserPrompt(
                            prompt = text,
                            currentEvents = eventsList.toList(),
                            onAddEvent = { event -> 
                                eventsList.add(event)
                                selectedDay = event.day
                                selectedDayMonth = event.month
                                selectedDayYear = event.year
                                selectedMonth = event.month
                                selectedYear = event.year
                                viewAllEvents = false
                            },
                            onRemoveEvent = { id -> eventsList.removeAll { it.id == id } },
                            onModifyEvent = { modifiedEvent ->
                                val index = eventsList.indexOfFirst { it.id == modifiedEvent.id }
                                if (index != -1) {
                                    eventsList[index] = modifiedEvent
                                    selectedDay = modifiedEvent.day
                                    selectedDayMonth = modifiedEvent.month
                                    selectedDayYear = modifiedEvent.year
                                    selectedMonth = modifiedEvent.month
                                    selectedYear = modifiedEvent.year
                                    viewAllEvents = false
                                }
                            }
                        )
                    }

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
                                onSendClick = { text ->
                                    jarvisViewModel.handleUserPrompt(
                                        prompt = text,
                                        currentEvents = eventsList.toList(),
                                        onAddEvent = { event -> 
                                            eventsList.add(event)
                                            selectedDay = event.day
                                            selectedDayMonth = event.month
                                            selectedDayYear = event.year
                                            selectedMonth = event.month
                                            selectedYear = event.year
                                            viewAllEvents = false
                                        },
                                        onRemoveEvent = { id -> eventsList.removeAll { it.id == id } },
                                        onModifyEvent = { modifiedEvent ->
                                            val index = eventsList.indexOfFirst { it.id == modifiedEvent.id }
                                            if (index != -1) {
                                                eventsList[index] = modifiedEvent
                                                selectedDay = modifiedEvent.day
                                                selectedDayMonth = modifiedEvent.month
                                                selectedDayYear = modifiedEvent.year
                                                selectedMonth = modifiedEvent.month
                                                selectedYear = modifiedEvent.year
                                                viewAllEvents = false
                                            }
                                        }
                                    )
                                },
                                onMicClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Jarvis to add or arrange tasks...")
                                    }
                                    speechRecognizerLauncher.launch(intent)
                                },
                                chatHistory = chatHistory
                            )
                        }
                        composable("add_event") {
                            AddEventScreen(
                                selectedDay = selectedDay,
                                selectedMonth = selectedMonth,
                                selectedYear = selectedYear,
                                onSaveEvent = { title, time, chosenDay, link, fileNames, year ->
                                    eventsList.add(
                                        CalendarEvent(
                                            day = chosenDay,
                                            month = selectedMonth,
                                            year = year,
                                            title = title.uppercase(),
                                            time = time,
                                            link = link.takeIf { it.isNotBlank() },
                                            fileNames = fileNames
                                        )
                                    )
                                    // Set focus to the new day so the user sees their event immediately!
                                    selectedDay = chosenDay
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
