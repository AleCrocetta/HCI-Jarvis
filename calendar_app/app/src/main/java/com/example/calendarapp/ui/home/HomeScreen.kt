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
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.calendarapp.model.CalendarEvent
import com.example.calendarapp.ui.ChatMessage
import com.example.calendarapp.ui.theme.DarkBlue
import com.example.calendarapp.ui.theme.LightBlueBg
import com.example.calendarapp.ui.theme.LightGrayBg
import com.example.calendarapp.ui.theme.TextGray
import com.example.calendarapp.ui.theme.White
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    selectedDay: Int,
    selectedDayMonth: String,
    selectedDayYear: Int,
    onDaySelected: (Int, String, Int) -> Unit,
    events: List<CalendarEvent>,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onRestoreEvent: (CalendarEvent) -> Unit,
    onCompleteEvent: (CalendarEvent) -> Unit,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    viewAllEvents: Boolean,
    onViewAllChanged: (Boolean) -> Unit,
    selectedMonth: String,
    onMonthSelected: (String) -> Unit,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    onAddEventClick: () -> Unit,
    onSendClick: (String) -> Unit = {},
    onMicClick: () -> Unit = {},
    chatHistory: List<ChatMessage> = emptyList()
) {
    // Filter events dynamically based on day, month, year and search query
    val filteredEvents = events.filter { event ->
        val yearMatches = event.year == selectedYear
        val monthMatches = event.month.equals(selectedMonth, ignoreCase = true)
        val dayMatches = viewAllEvents || event.day == selectedDay
        val searchMatches = searchQuery.isBlank() || event.title.contains(searchQuery, ignoreCase = true)
        if (searchQuery.isBlank()) {
            yearMatches && monthMatches && dayMatches
        } else {
            searchMatches
        }
    }
    val isSearching = searchQuery.isNotBlank()

    val coroutineScope = rememberCoroutineScope()
    val recentlyDeletedEvents = remember { mutableStateListOf<CalendarEvent>() }
    var showUndoSnackbar by remember { mutableStateOf(false) }
    var undoTimerJob by remember { mutableStateOf<Job?>(null) }

    val handleEventDeletion: (CalendarEvent) -> Unit = { event ->
        recentlyDeletedEvents.add(event)
        onDeleteEvent(event)
        showUndoSnackbar = true
        undoTimerJob?.cancel()
        undoTimerJob = coroutineScope.launch {
            delay(4000) // 4 seconds delay
            recentlyDeletedEvents.clear()
            showUndoSnackbar = false
        }
    }

    val handleUndoDelete: () -> Unit = {
        recentlyDeletedEvents.reversed().forEach { restoredEvent ->
            onRestoreEvent(restoredEvent)
        }
        recentlyDeletedEvents.clear()
        showUndoSnackbar = false
        undoTimerJob?.cancel()
    }

    var showMemoryDialog by remember { mutableStateOf(false) }
    val userPreferences = remember { 
        mutableStateListOf(
            "Schedule training in the morning",
            "Gym sessions at 8:00 AM",
            "Meetings must have video link",
            "Highlight flight events with indicator"
        )
    }
    var newPreferenceText by remember { mutableStateOf("") }
    
    var showMonthDialog by remember { mutableStateOf(false) }

    val monthsList = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    fun onPreviousMonth() {
        val currentIndex = monthsList.indexOf(selectedMonth)
        if (currentIndex == 0) {
            onMonthSelected("December")
            onYearSelected(selectedYear - 1)
        } else {
            onMonthSelected(monthsList[currentIndex - 1])
        }
    }
    
    fun onNextMonth() {
        val currentIndex = monthsList.indexOf(selectedMonth)
        if (currentIndex == 11) {
            onMonthSelected("January")
            onYearSelected(selectedYear + 1)
        } else {
            onMonthSelected(monthsList[currentIndex + 1])
        }
    }

    Scaffold(
        bottomBar = {
            JarvisBottomBar(
                onSendClick = onSendClick,
                onMicClick = onMicClick,
                onBrainClick = { showMemoryDialog = true }
            )
        },
        containerColor = White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            selectedYear = selectedYear,
                            onMonthClick = { showMonthDialog = true },
                            searchQuery = searchQuery,
                            onSearchQueryChanged = onSearchQueryChanged
                        )
                        CalendarGrid(
                            selectedDay = selectedDay,
                            selectedDayMonth = selectedDayMonth,
                            selectedDayYear = selectedDayYear,
                            onDaySelected = onDaySelected,
                            events = events,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            onMonthSelected = onMonthSelected,
                            onYearSelected = onYearSelected,
                            onPreviousMonth = { onPreviousMonth() },
                            onNextMonth = { onNextMonth() }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                
                // Bottom Section with Events (scrollable, locked to proportion!)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isSearching) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            if (filteredEvents.isEmpty()) {
                                Surface(
                                    color = LightGrayBg,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No matching events",
                                        color = TextGray,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                                    )
                                }
                            } else {
                                filteredEvents.forEachIndexed { index, event ->
                                    Surface(
                                        color = LightBlueBg,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onDaySelected(event.day, event.month, event.year)
                                                onMonthSelected(event.month)
                                                onYearSelected(event.year)
                                                onSearchQueryChanged("")
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = event.day.toString(),
                                                color = DarkBlue,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(40.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = event.title,
                                                    color = DarkBlue,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = event.time,
                                                    color = TextGray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Text(
                                                text = "${event.month.take(3)} ${event.year}",
                                                color = TextGray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    if (index < filteredEvents.size - 1) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        TodaySection(
                            selectedDay = selectedDay,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            eventCount = filteredEvents.size
                        )
                        
                        EventList(
                            events = filteredEvents,
                            onDeleteEvent = handleEventDeletion,
                            onCompleteEvent = onCompleteEvent
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp)) // Padding at bottom of event list
                }
            }

            // Beautiful Undo Banner!
            AnimatedVisibility(
                visible = showUndoSnackbar,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
                ),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 250)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    color = DarkBlue.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val count = recentlyDeletedEvents.size
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🗑️",
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (count == 1) "Task deleted" else "$count tasks deleted",
                                color = White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        
                        TextButton(
                            onClick = handleUndoDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF81C784)) // Sleek premium green for undo
                        ) {
                            Text(
                                text = "UNDO",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Memory & Preferences Area Dialog
    if (showMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showMemoryDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add, // Placeholder, custom psychology icon imported in TopBar but we can use fallback or custom Brain drawing!
                        contentDescription = null,
                        tint = DarkBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Memory Area",
                        color = DarkBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Customize your AI calendar preferences. Jarvis uses these to assist you:",
                        color = TextGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Add Preference Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPreferenceText,
                            onValueChange = { newPreferenceText = it },
                            placeholder = { Text("Add preference...", color = TextGray, fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkBlue,
                                unfocusedBorderColor = TextGray,
                                focusedContainerColor = LightGrayBg,
                                unfocusedContainerColor = LightGrayBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newPreferenceText.isNotBlank()) {
                                    userPreferences.add(newPreferenceText)
                                    newPreferenceText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Save", color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Preferences List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        userPreferences.forEachIndexed { index, pref ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LightGrayBg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pref,
                                    color = DarkBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "❌",
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .clickable { userPreferences.removeAt(index) }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMemoryDialog = false }) {
                    Text("Done", color = DarkBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Month Selector Dialog
    if (showMonthDialog) {
        var tempYear by remember { mutableStateOf(selectedYear) }
        var yearInput by remember { mutableStateOf(selectedYear.toString()) }
        AlertDialog(
            onDismissRequest = { showMonthDialog = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Select Date", color = DarkBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { 
                            tempYear--
                            yearInput = tempYear.toString()
                        }) {
                            Text("<", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { input ->
                                if (input.length <= 4 && input.all { it.isDigit() }) {
                                    yearInput = input
                                    input.toIntOrNull()?.let { parsedYear ->
                                        tempYear = parsedYear
                                    }
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = DarkBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .width(90.dp)
                                .height(52.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkBlue,
                                unfocusedBorderColor = TextGray.copy(alpha = 0.4f),
                                focusedContainerColor = LightGrayBg,
                                unfocusedContainerColor = LightGrayBg
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = { 
                            tempYear++
                            yearInput = tempYear.toString()
                        }) {
                            Text(">", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                }
            },
            text = {
                val months = listOf(
                    listOf("January", "February", "March"),
                    listOf("April", "May", "June"),
                    listOf("July", "August", "September"),
                    listOf("October", "November", "December")
                )
                Column {
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
                                        .background(if (selectedMonth == month && selectedYear == tempYear) LightBlueBg else Color.Transparent)
                                        .clickable {
                                            onMonthSelected(month)
                                            onYearSelected(tempYear)
                                            showMonthDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = month.take(3), // Jan, Feb...
                                        color = if (selectedMonth == month && selectedYear == tempYear) DarkBlue else TextGray,
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
