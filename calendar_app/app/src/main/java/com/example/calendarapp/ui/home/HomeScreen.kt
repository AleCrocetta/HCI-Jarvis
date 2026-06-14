package com.example.calendarapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType

private data class MemoryEntry(
    val id: Int,
    val name: String,
    val value: String
)

private enum class AiFeedbackState(val label: String) {
    Success("Success"),
    MemoryUpdate("Memory update"),
    MissingInfo("Missing info"),
    Collision("Conflict")
}

private fun classifyAiFeedback(message: String): AiFeedbackState {
    val normalized = message.lowercase()
    return when {
        normalized.startsWith("success:") ->
            AiFeedbackState.Success
        listOf("collision", "collides", "overlap", "conflict", "sovrappone", "conflitto").any { it in normalized } ->
            AiFeedbackState.Collision
        listOf("memory updated", "memory update").any { it in normalized } ->
            AiFeedbackState.MemoryUpdate
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val handleTodayClick = {
        val today = java.util.Calendar.getInstance()
        val todayDay = today.get(java.util.Calendar.DAY_OF_MONTH)
        val todayMonth = today.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.ENGLISH) ?: selectedMonth
        val todayYear = today.get(java.util.Calendar.YEAR)
        onDaySelected(todayDay, todayMonth, todayYear)
        onMonthSelected(todayMonth)
        onYearSelected(todayYear)
        onViewAllChanged(false)
    }

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
        showUndoSnackbar = true
        undoTimerJob?.cancel()
        undoTimerJob = coroutineScope.launch {
            delay(4000)
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
    var showJarvisChat by remember { mutableStateOf(false) }
    var jarvisChatText by remember { mutableStateOf("") }
    var submittedFromJarvisChat by remember { mutableStateOf(false) }
    var focusedEventIdBeforeJarvisSubmit by remember { mutableStateOf<String?>(null) }
    var latestMessageIdWhenJarvisOpened by remember { mutableStateOf<String?>(null) }
    
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

    LaunchedEffect(highlightedEventId, latestAiMessage?.id) {
        val latestMessage = latestAiMessage ?: return@LaunchedEffect
        if (!showJarvisChat || !submittedFromJarvisChat) return@LaunchedEffect
        val focusedNewEvent = highlightedEventId != null && highlightedEventId != focusedEventIdBeforeJarvisSubmit
        val sameEventUpdated = highlightedEventId != null &&
            latestMessage.id != latestMessageIdWhenJarvisOpened &&
            listOf("created event", "updated event").any { it in latestMessage.text.lowercase() }
        if (focusedNewEvent || sameEventUpdated) {
            showJarvisChat = false
            submittedFromJarvisChat = false
            latestMessageIdWhenJarvisOpened = chatHistory.lastOrNull()?.id
        }
    }

    Scaffold(
        bottomBar = {
            val secondaryButtonColor = Color(0xFFF4F8FF)
            val secondaryButtonBorder = Color(0xFF2979FF).copy(alpha = 0.22f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(84.dp)
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                        .size(48.dp)
                        .shadow(6.dp, CircleShape, spotColor = DarkBlue.copy(alpha = 0.08f))
                        .clip(CircleShape)
                        .background(secondaryButtonColor)
                        .border(1.dp, secondaryButtonBorder, CircleShape)
                        .clickable { showMemoryDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = "Memory Preferences",
                        tint = Color(0xFF2979FF),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(68.dp)
                        .shadow(12.dp, CircleShape, spotColor = DarkBlue.copy(alpha = 0.18f))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2979FF),
                                    Color(0xFF00E5FF)
                                )
                            )
                        )
                        .clickable {
                            if (latestMessageIdWhenJarvisOpened == null) {
                                latestMessageIdWhenJarvisOpened = chatHistory.lastOrNull()?.id
                            }
                            submittedFromJarvisChat = false
                            showJarvisChat = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ask Jarvis",
                        tint = White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(48.dp)
                        .shadow(6.dp, CircleShape, spotColor = DarkBlue.copy(alpha = 0.08f))
                        .clip(CircleShape)
                        .background(secondaryButtonColor)
                        .border(1.dp, secondaryButtonBorder, CircleShape)
                        .clickable {
                            if (latestMessageIdWhenJarvisOpened == null) {
                                latestMessageIdWhenJarvisOpened = chatHistory.lastOrNull()?.id
                            }
                            focusedEventIdBeforeJarvisSubmit = highlightedEventId
                            submittedFromJarvisChat = true
                            showJarvisChat = true
                            onMicClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = Color(0xFF2979FF),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
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
                            selectedDay = selectedDay,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            onMonthClick = { showMonthDialog = true },
                            onTodayClick = handleTodayClick,
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
                            },
                            onEditEvent = { event ->
                                onEditEvent(event)
                            },
                            highlightedEventId = highlightedEventId
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            val feedbackMessage = currentFeedbackMessage
            LaunchedEffect(feedbackMessage?.id) {
                val message = feedbackMessage ?: return@LaunchedEffect
                if (classifyAiFeedback(message.text) != AiFeedbackState.Collision) {
                    delay(6000)
                    dismissedFeedbackId = message.id
                }
            }

            if (feedbackMessage != null && dismissedFeedbackId != feedbackMessage.id) {
                val feedbackState = classifyAiFeedback(feedbackMessage.text)
                fun handleFeedbackClick() {
                    when (feedbackState) {
                        AiFeedbackState.MissingInfo -> {
                            showJarvisChat = true
                            dismissedFeedbackId = feedbackMessage.id
                        }
                        AiFeedbackState.Success -> {
                            showJarvisChat = false
                            onSearchQueryChanged("")
                            onViewAllChanged(false)
                            dismissedFeedbackId = feedbackMessage.id
                        }
                        else -> Unit
                    }
                }
                Surface(
                    color = White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = if (showUndoSnackbar) 88.dp else 16.dp)
                        .clickable(enabled = feedbackState == AiFeedbackState.MissingInfo || feedbackState == AiFeedbackState.Success) {
                            handleFeedbackClick()
                        }
                        .pointerInput(feedbackMessage.id) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -20f) {
                                    dismissedFeedbackId = feedbackMessage.id
                                }
                            }
                        }
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
                            if (feedbackState == AiFeedbackState.Collision && pendingCollisionNewEvent != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onRescheduleExistingEvent,
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Reschedule", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = onKeepBothEvents,
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Same hour", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
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
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF81C784))
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

    if (showJarvisChat) {
        val visibleMessages = latestMessageIdWhenJarvisOpened?.let { markerId ->
            val markerIndex = chatHistory.indexOfFirst { it.id == markerId }
            if (markerIndex == -1) emptyList() else chatHistory.drop(markerIndex + 1)
        } ?: chatHistory
        val recentMessages = visibleMessages.takeLast(8)
        val jarvisChatListState = androidx.compose.foundation.lazy.rememberLazyListState()
        val isJarvisThinking = submittedFromJarvisChat && recentMessages.lastOrNull()?.isUser == true
        val ledTransition = rememberInfiniteTransition(label = "jarvis_input_led")
        val ledProgress by ledTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = LinearEasing)
            ),
            label = "jarvis_input_led_progress"
        )

        LaunchedEffect(recentMessages.lastOrNull()?.id) {
            if (recentMessages.isNotEmpty()) {
                jarvisChatListState.animateScrollToItem(recentMessages.lastIndex)
            }
        }

        fun submitJarvisPrompt() {
            val prompt = jarvisChatText.trim()
            if (prompt.isBlank()) return
            focusedEventIdBeforeJarvisSubmit = highlightedEventId
            submittedFromJarvisChat = true
            onSendClick(prompt)
            jarvisChatText = ""
        }

        ModalBottomSheet(
            onDismissRequest = {
                showJarvisChat = false
                submittedFromJarvisChat = false
            },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Ask Jarvis",
                    color = DarkBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ask Jarvis to add an event, save a routine, or arrange your tasks.",
                    color = TextGray,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 60.dp, max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    state = jarvisChatListState
                ) {
                    if (recentMessages.isNotEmpty()) {
                        items(recentMessages.size) { index ->
                            val message = recentMessages[index]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (message.isUser) DarkBlue else LightGrayBg,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.widthIn(max = 320.dp)
                                ) {
                                    Text(
                                        text = message.text,
                                        color = if (message.isUser) White else DarkBlue,
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .drawBehind {
                                if (isJarvisThinking) {
                                    val radius = 22.dp.toPx()
                                    val perimeter = 2f * (size.width + size.height - 4f * radius) + (2f * Math.PI.toFloat() * radius)
                                    val dashLength = perimeter * 0.18f
                                    val dashEffect = PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(dashLength, perimeter),
                                        phase = -ledProgress * perimeter
                                    )
                                    drawRoundRect(
                                        color = Color(0xFF2979FF).copy(alpha = 0.22f),
                                        cornerRadius = CornerRadius(radius, radius),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                    drawRoundRect(
                                        color = Color(0xFF00E5FF).copy(alpha = 0.42f),
                                        cornerRadius = CornerRadius(radius, radius),
                                        style = Stroke(width = 9.dp.toPx(), pathEffect = dashEffect)
                                    )
                                    drawRoundRect(
                                        color = Color(0xFF00E5FF),
                                        cornerRadius = CornerRadius(radius, radius),
                                        style = Stroke(width = 4.dp.toPx(), pathEffect = dashEffect)
                                    )
                                }
                            }
                    ) {
                        OutlinedTextField(
                            value = jarvisChatText,
                            onValueChange = { jarvisChatText = it },
                            placeholder = {
                                Text(
                                    text = "Ask Jarvis...",
                                    color = TextGray,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isJarvisThinking) Color.Transparent else DarkBlue.copy(alpha = 0.4f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = LightGrayBg,
                                unfocusedContainerColor = LightGrayBg
                            ),
                            shape = RoundedCornerShape(22.dp),
                            singleLine = true
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF2979FF),
                                        Color(0xFF00E5FF)
                                    )
                                )
                            )
                            .clickable {
                                focusedEventIdBeforeJarvisSubmit = highlightedEventId
                                submittedFromJarvisChat = true
                                onMicClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (jarvisChatText.isNotBlank()) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF2979FF),
                                            Color(0xFF00E5FF)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            LightGrayBg,
                                            LightGrayBg
                                        )
                                    )
                                }
                            )
                            .clickable(enabled = jarvisChatText.isNotBlank()) { submitJarvisPrompt() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Text",
                            tint = if (jarvisChatText.isNotBlank()) White else TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

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
                                .height(56.dp),
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
