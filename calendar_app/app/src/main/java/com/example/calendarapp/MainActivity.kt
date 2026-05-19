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
                    
                    var selectedDay by remember { mutableIntStateOf(currentDay) }
                    var activeTab by remember { mutableStateOf("calendar") }
                    var searchQuery by remember { mutableStateOf("") }
                    var viewAllEvents by remember { mutableStateOf(false) }
                    var selectedMonth by remember { mutableStateOf(currentMonth) }
                    
                    val eventsList = remember {
                        mutableStateListOf<CalendarEvent>(
                            CalendarEvent(day = currentDay, month = currentMonth, title = "MORNING WALK", time = "06:30 AM - 07:00 AM"),
                            CalendarEvent(day = currentDay, month = currentMonth, title = "GYM TIME", time = "08:00 AM - 09:20 AM"),
                            CalendarEvent(day = currentDay, month = currentMonth, title = "PILOTS MEETING", time = "10:00 AM - 10:30 AM", link = "https://meet.google.com/abc-xyz", fileNames = listOf("MeetingAgenda.pdf")),
                            CalendarEvent(day = if (currentDay > 2) currentDay - 2 else 28, month = currentMonth, title = "DOCTOR APPOINTMENT", time = "11:00 AM - 12:00 PM"),
                            CalendarEvent(day = if (currentDay < 23) currentDay + 5 else 3, month = currentMonth, title = "PROJECT SPRINT REVIEW", time = "03:00 PM - 04:30 PM", link = "https://zoom.us/j/sprint-123", fileNames = listOf("SprintBacklog.xlsx")),
                            CalendarEvent(day = if (currentDay < 19) currentDay + 9 else 5, month = currentMonth, title = "FLIGHT TO NEW YORK", time = "02:00 PM - 06:00 PM", link = "https://flightstatus.com/NYC-778", fileNames = listOf("BoardingPass.pdf", "TicketDetails.txt")),
                            CalendarEvent(day = 15, month = "January", title = "NEW YEAR PLANNING", time = "10:00 AM - 11:30 AM"),
                            CalendarEvent(day = 10, month = "March", title = "SPRING CLEANING", time = "09:00 AM - 12:00 PM")
                        )
                    }

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                selectedDay = selectedDay,
                                onDaySelected = { day -> 
                                    selectedDay = day
                                    // Turn off "view all" if they click a specific day to keep navigation natural
                                    viewAllEvents = false 
                                },
                                events = eventsList.toList(),
                                onDeleteEvent = { event -> eventsList.remove(event) },
                                activeTab = activeTab,
                                onTabSelected = { tab -> activeTab = tab },
                                searchQuery = searchQuery,
                                onSearchQueryChanged = { searchQuery = it },
                                viewAllEvents = viewAllEvents,
                                onViewAllChanged = { viewAllEvents = it },
                                selectedMonth = selectedMonth,
                                onMonthSelected = { selectedMonth = it },
                                onAddEventClick = {
                                    navController.navigate("add_event")
                                }
                            )
                        }
                        composable("add_event") {
                            AddEventScreen(
                                selectedDay = selectedDay,
                                selectedMonth = selectedMonth,
                                onSaveEvent = { title, time, chosenDay, link, fileNames ->
                                    eventsList.add(
                                        CalendarEvent(
                                            day = chosenDay,
                                            month = selectedMonth,
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
