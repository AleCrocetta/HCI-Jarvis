package com.example.calendarapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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

private data class MemoryEntry(
    val id: Int,
    val name: String,
    val value: String
)

private enum class AiFeedbackState(val label: String) {
    Success("Success"),
    MissingInfo("Missing info"),
    Collision("Collision")
}

private fun classifyAiFeedback(message: String): AiFeedbackState {
    val normalized = message.lowercase()
    return when {
        listOf("collision", "collides", "overlap", "conflict", "sovrappone", "conflitto").any { it in normalized } ->
            AiFeedbackState.Collision
        listOf("missing info", "missing", "need", "provide", "details", "date", "time", "manca", "serve", "specifica", "quando", "data", "giorno", "orario", "dettagli").any { it in normalized } ->
            AiFeedbackState.MissingInfo
        else -> AiFeedbackState.Success
    }
}

@Composable
private fun SearchResultsPanel(
    events: List<CalendarEvent>,
    onEventSelected: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (events.isEmpty()) {
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
            events.forEachIndexed { index, event ->
                Surface(
                    color = LightBlueBg,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEventSelected(event) }
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

                if (index < events.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

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
    onEditEvent: (CalendarEvent) -> Unit,
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
    chatHistory: List<ChatMessage> = emptyList(),
    highlightedEventId: String? = null,
    userPreferences: List<String>,
    onSavePreferences: (List<String>) -> Unit,
    showFirstRunPreferences: Boolean,
    onFirstRunPreferencesDone: (List<String>) -> Unit,
    pendingCollisionNewEvent: CalendarEvent? = null,
    pendingCollisionExistingEvent: CalendarEvent? = null,
    onCancelNewEvent: () -> Unit = {},
    onKeepBothEvents: () -> Unit = {},
    onReplaceExistingEvent: () -> Unit = {},
    onRescheduleExistingEvent: () -> Unit = {}
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
    val focusManager = LocalFocusManager.current

    val coroutineScope = rememberCoroutineScope()
    val recentlyDeletedEvents = remember { mutableStateListOf<CalendarEvent>() }
    var showUndoSnackbar by remember { mutableStateOf(false) }
    var undoTimerJob by remember { mutableStateOf<Job?>(null) }
    var dismissedFeedbackId by remember { mutableStateOf<String?>(null) }
    var currentFeedbackMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val latestAiMessage = chatHistory.lastOrNull { !it.isUser }

    LaunchedEffect(latestAiMessage?.id) {
        if (latestAiMessage != null) {
            currentFeedbackMessage = latestAiMessage
            dismissedFeedbackId = null
        }
    }

    fun showFeedback(message: String) {
        currentFeedbackMessage = ChatMessage(text = message, isUser = false)
        dismissedFeedbackId = null
    }

    val handleEventDeletion: (CalendarEvent) -> Unit = { event ->
        recentlyDeletedEvents.add(event)
        onDeleteEvent(event)
        showFeedback("Success: deleted ${event.title}.")
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
        showFeedback("Success: restored ${recentlyDeletedEvents.size} deleted event(s).")
        recentlyDeletedEvents.clear()
        showUndoSnackbar = false
        undoTimerJob?.cancel()
    }

    var showMemoryDialog by remember { mutableStateOf(false) }
    
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
                        if (isSearching) {
                            SearchResultsPanel(
                                events = filteredEvents,
                                onEventSelected = { event ->
                                    focusManager.clearFocus()
                                    onDaySelected(event.day, event.month, event.year)
                                    onMonthSelected(event.month)
                                    onYearSelected(event.year)
                                    onSearchQueryChanged("")
                                },
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .heightIn(max = 220.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
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
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                
                // Bottom Section with Events (scrollable, locked to proportion!)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!isSearching) {
                        TodaySection(
                            selectedDay = selectedDay,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            eventCount = filteredEvents.size
                        )
                        
                        EventList(
                            events = filteredEvents,
                            onDeleteEvent = handleEventDeletion,
                            onCompleteEvent = { event ->
                                onCompleteEvent(event)
                                showFeedback("Success: updated ${event.title}.")
                            },
                            onEditEvent = { event ->
                                onEditEvent(event)
                                showFeedback("Success: updated ${event.title}.")
                            },
                            highlightedEventId = highlightedEventId
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp)) // Padding at bottom of event list
                }
            }

            val feedbackMessage = currentFeedbackMessage
            if (feedbackMessage != null && dismissedFeedbackId != feedbackMessage.id) {
                val feedbackState = classifyAiFeedback(feedbackMessage.text)
                Surface(
                    color = White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = feedbackState.label,
                                color = DarkBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = feedbackMessage.text,
                                color = TextGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Text(
                            text = "Dismiss",
                            color = DarkBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { dismissedFeedbackId = feedbackMessage.id }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
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
        val memoryEntries = remember(userPreferences) {
            mutableStateListOf<MemoryEntry>().apply {
                userPreferences.forEachIndexed { index, preference ->
                    val hasName = preference.contains(":")
                    val name = if (hasName) preference.substringBefore(":").trim() else "Memory ${index + 1}"
                    val value = if (hasName) preference.substringAfter(":").trim() else preference.trim()
                    if (value.isNotBlank() && !value.equals("Not specified", ignoreCase = true)) {
                        add(MemoryEntry(index, name.ifBlank { "Memory ${index + 1}" }, value))
                    }
                }
            }
        }
        var expandedMemoryPool by remember(userPreferences) {
            mutableStateOf(memoryEntries.firstOrNull()?.id)
        }

        fun saveMemoryEntries() {
            onSavePreferences(
                memoryEntries
                    .filter { it.name.isNotBlank() && it.value.isNotBlank() }
                    .map { "${it.name.trim()}: ${it.value.trim()}" }
            )
            showFeedback("Success: memory updated.")
        }

        fun memoryFolder(entry: MemoryEntry): String {
            val text = "${entry.name} ${entry.value}".lowercase()
            return when {
                Regex("""\b(football|gym|sport|training|run|running|yoga|basket|tennis|swim)\b""").containsMatchIn(text) -> "Sport"
                Regex("""\b(study|exam|homework|lesson|revision)\b""").containsMatchIn(text) -> "Study"
                Regex("""\b(work|class|course|lecture|meeting|job|shift)\b""").containsMatchIn(text) -> "Work / Classes"
                Regex("""\b(hard|harder|difficult|morning|afternoon|planning|preference)\b""").containsMatchIn(text) -> "Preferences"
                Regex("""\b(sleep|wake|lunch|dinner|breakfast|meal|commute|travel|break|rest)\b""").containsMatchIn(text) -> "Daily Life"
                else -> "Other"
            }
        }

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
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    Surface(
                        color = LightBlueBg,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Saved memory fields are grouped automatically and sync regular routines into the calendar.",
                            color = DarkBlue,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (memoryEntries.isEmpty()) {
                        Surface(
                            color = LightGrayBg,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No saved memory yet. Add a regular task or preference from the Jarvis bar.",
                                color = TextGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                            )
                        }
                    }

                    memoryEntries.groupBy { memoryFolder(it) }.forEach { (folder, entries) ->
                        Text(
                            text = folder,
                            color = DarkBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                        )
                        entries.forEach { entry ->
                        val index = memoryEntries.indexOfFirst { it.id == entry.id }
                        if (index != -1) {
                        val isExpanded = expandedMemoryPool == entry.id
                        Surface(
                            color = LightGrayBg,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedMemoryPool = if (isExpanded) null else entry.id
                                }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.name,
                                            color = DarkBlue,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = entry.value.ifBlank { "No saved instruction" },
                                            color = TextGray,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = if (isExpanded) "Hide" else "Edit",
                                        color = DarkBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Delete",
                                        color = Color(0xFFD32F2F),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                memoryEntries.removeAt(index)
                                                saveMemoryEntries()
                                                if (expandedMemoryPool == entry.id) {
                                                    expandedMemoryPool = null
                                                }
                                            }
                                            .padding(horizontal = 4.dp, vertical = 6.dp)
                                    )
                                }
                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = entry.name,
                                        onValueChange = {
                                            memoryEntries[index] = entry.copy(name = it)
                                            saveMemoryEntries()
                                        },
                                        label = { Text("Name", color = TextGray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DarkBlue.copy(alpha = 0.45f),
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = White,
                                            unfocusedContainerColor = White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = entry.value,
                                        onValueChange = {
                                            memoryEntries[index] = entry.copy(value = it)
                                            saveMemoryEntries()
                                        },
                                        label = { Text("Saved text", color = TextGray) },
                                        placeholder = { Text("Routine or preference...", color = TextGray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DarkBlue.copy(alpha = 0.45f),
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = White,
                                            unfocusedContainerColor = White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveMemoryEntries()
                        showMemoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Memory", color = White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (pendingCollisionNewEvent != null && pendingCollisionExistingEvent != null) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "Collision",
                    color = DarkBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "${pendingCollisionNewEvent.title} (${pendingCollisionNewEvent.time}) overlaps with ${pendingCollisionExistingEvent.title} (${pendingCollisionExistingEvent.time}) on ${pendingCollisionNewEvent.month} ${pendingCollisionNewEvent.day}, ${pendingCollisionNewEvent.year}.",
                        color = TextGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose how Jarvis should resolve this collision.",
                        color = DarkBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Column {
                    TextButton(onClick = onCancelNewEvent) {
                        Text("Cancel the new event", color = DarkBlue, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onKeepBothEvents) {
                        Text("Keep both", color = DarkBlue, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onReplaceExistingEvent) {
                        Text("Replace the existing event", color = DarkBlue, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onRescheduleExistingEvent) {
                        Text("Reschedule the existing event", color = DarkBlue, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showFirstRunPreferences) {
        var occupiedDays by remember { mutableStateOf("") }
        var occupiedTimes by remember { mutableStateOf("") }
        var studyTime by remember { mutableStateOf("2 hours") }
        var wantsBreaks by remember { mutableStateOf(true) }
        var breakLength by remember { mutableStateOf("10 minutes") }
        var studyManagement by remember { mutableStateOf("Balanced study plan") }

        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "Study Preferences",
                    color = DarkBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Jarvis will use this memory when planning tasks and avoiding overlaps.",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = occupiedDays,
                        onValueChange = { occupiedDays = it },
                        label = { Text("Occupied days", color = TextGray) },
                        placeholder = { Text("Monday, Wednesday...", color = TextGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = occupiedTimes,
                        onValueChange = { occupiedTimes = it },
                        label = { Text("Occupied time", color = TextGray) },
                        placeholder = { Text("09:00 AM - 01:00 PM", color = TextGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = studyTime,
                        onValueChange = { studyTime = it },
                        label = { Text("Daily study time", color = TextGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Breaks during study", color = DarkBlue, fontWeight = FontWeight.SemiBold)
                        Switch(checked = wantsBreaks, onCheckedChange = { wantsBreaks = it })
                    }
                    if (wantsBreaks) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = breakLength,
                            onValueChange = { breakLength = it },
                            label = { Text("Break length", color = TextGray) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = studyManagement,
                        onValueChange = { studyManagement = it },
                        label = { Text("Study management style", color = TextGray) },
                        placeholder = { Text("Intensive, balanced, spaced repetition...", color = TextGray) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFirstRunPreferencesDone(
                            listOf(
                                "Occupied days: ${occupiedDays.ifBlank { "Not specified" }}",
                                "Occupied time: ${occupiedTimes.ifBlank { "Not specified" }}",
                                "Daily study time: ${studyTime.ifBlank { "Not specified" }}",
                                "Study breaks: ${if (wantsBreaks) "Yes, $breakLength" else "No"}",
                                "Study management style: ${studyManagement.ifBlank { "Not specified" }}"
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Preferences", color = White, fontWeight = FontWeight.Bold)
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
