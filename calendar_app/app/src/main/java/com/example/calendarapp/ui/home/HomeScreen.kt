package com.example.calendarapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarapp.model.CalendarEvent
import com.example.calendarapp.ui.theme.DarkBlue
import com.example.calendarapp.ui.theme.LightBlueBg
import com.example.calendarapp.ui.theme.LightGrayBg
import com.example.calendarapp.ui.theme.TextGray
import com.example.calendarapp.ui.theme.White

@Composable
fun HomeScreen(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    events: List<CalendarEvent>,
    onDeleteEvent: (CalendarEvent) -> Unit,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    viewAllEvents: Boolean,
    onViewAllChanged: (Boolean) -> Unit,
    selectedMonth: String,
    onMonthSelected: (String) -> Unit,
    onAddEventClick: () -> Unit
) {
    // Filter events dynamically based on day, month and search query
    val filteredEvents = events.filter { event ->
        val monthMatches = event.month.equals(selectedMonth, ignoreCase = true)
        val dayMatches = viewAllEvents || event.day == selectedDay
        val searchMatches = searchQuery.isBlank() || event.title.contains(searchQuery, ignoreCase = true)
        monthMatches && dayMatches && searchMatches
    }

    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showMonthDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            BottomNavBar(activeTab = activeTab, onTabSelected = onTabSelected)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEventClick,
                shape = CircleShape,
                containerColor = DarkBlue,
                contentColor = White,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        },
        containerColor = White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Section with Calendar (White background, shadow at the bottom)
            Surface(
                color = White,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    spotColor = DarkBlue.copy(alpha = 0.05f)
                )
            ) {
                Column {
                    TopBar(
                        selectedMonth = selectedMonth,
                        onMonthClick = { showMonthDialog = true },
                        searchQuery = searchQuery,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onNotificationsClick = { showNotificationsDialog = true }
                    )
                    CalendarGrid(
                        selectedDay = selectedDay,
                        onDaySelected = onDaySelected,
                        events = events,
                        selectedMonth = selectedMonth
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            
            // Bottom Section with Events
            TodaySection(
                selectedDay = selectedDay,
                eventCount = filteredEvents.size,
                viewAllEvents = viewAllEvents,
                onViewAllToggle = { onViewAllChanged(!viewAllEvents) }
            )
            
            EventList(
                events = filteredEvents,
                onDeleteEvent = onDeleteEvent
            )
            
            Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB
        }
    }

    // Notification Dialog
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Upcoming Notifications", color = DarkBlue, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Here are your upcoming calendar alerts:", color = TextGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    NotificationRow("Reminder: MORNING WALK starts soon!", "06:30 AM")
                    Spacer(modifier = Modifier.height(8.dp))
                    NotificationRow("Workout: GYM TIME!", "08:00 AM")
                    Spacer(modifier = Modifier.height(8.dp))
                    NotificationRow("Meeting: PILOTS MEETING", "10:00 AM")
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Dismiss", color = DarkBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Month Selector Dialog
    if (showMonthDialog) {
        AlertDialog(
            onDismissRequest = { showMonthDialog = false },
            title = { Text("Select Month", color = DarkBlue, fontWeight = FontWeight.Bold) },
            text = {
                val months = listOf(
                    listOf("January", "February", "March"),
                    listOf("April", "May", "June"),
                    listOf("July", "August", "September"),
                    listOf("October", "November", "December")
                )
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    months.forEach { rowMonths ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowMonths.forEach { month ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedMonth == month) LightBlueBg else Color.Transparent)
                                        .clickable {
                                            onMonthSelected(month)
                                            showMonthDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = month.take(3), // Jan, Feb, Mar...
                                        color = if (selectedMonth == month) DarkBlue else TextGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {},
            containerColor = White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun NotificationRow(message: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LightGrayBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = DarkBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = time,
            color = TextGray,
            fontSize = 10.sp
        )
    }
}
