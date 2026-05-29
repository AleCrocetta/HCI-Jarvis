package com.example.calendarapp.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarapp.model.CalendarEvent
import com.example.calendarapp.ui.theme.DarkBlue
import com.example.calendarapp.ui.theme.GrayBorder
import com.example.calendarapp.ui.theme.LightBlueBg
import com.example.calendarapp.ui.theme.LightGrayBg
import com.example.calendarapp.ui.theme.TextGray
import kotlinx.coroutines.delay

private fun priorityColor(priority: String): Color {
    return when (priority.lowercase()) {
        "high" -> Color(0xFFE57373)
        "low" -> Color(0xFF81C784)
        else -> Color(0xFFFFB74D)
    }
}

private fun daysInMonth(month: String, year: Int): Int {
    val monthIndex = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ).indexOfFirst { it.equals(month, ignoreCase = true) }.coerceAtLeast(0)

    return java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, year)
        set(java.util.Calendar.MONTH, monthIndex)
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
}

@Composable
private fun EditPickerField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = LightGrayBg,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Text(
                text = value,
                color = DarkBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
fun EventList(
    events: List<CalendarEvent>,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onCompleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        if (events.isEmpty()) {
            // High-fidelity empty state placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(LightGrayBg)
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "No events",
                        tint = TextGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No events scheduled",
                        color = DarkBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ask Jarvis in the chat below to add and arrange your tasks",
                        color = TextGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            events.forEachIndexed { index, event ->
                // Key the custom composable to the event.id so states are strictly isolated!
                key(event.id) {
                    SwipeableEventItem(
                        event = event,
                        onDelete = { onDeleteEvent(event) },
                        onComplete = { onCompleteEvent(event) },
                        onEditEvent = onEditEvent
                    )
                }

                if (index < events.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableEventItem(
    event: CalendarEvent,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onEditEvent: (CalendarEvent) -> Unit
) {
    var isDeleted by remember { mutableStateOf(false) }

    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            delay(250) // Wait precisely for shrink exit animation to finish
            onDelete()
        }
    }

    AnimatedVisibility(
        visible = !isDeleted,
        enter = fadeIn(),
        exit = fadeOut() + shrinkVertically(animationSpec = tween(250))
    ) {
        val dismissState = rememberDismissState(
            confirmValueChange = { value ->
                if (value == DismissValue.DismissedToStart) {
                    isDeleted = true
                    true
                } else if (value == DismissValue.DismissedToEnd) {
                    onComplete()
                    false
                } else {
                    false
                }
            }
        )

        SwipeToDismiss(
            state = dismissState,
            directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd), // Swipe left to delete, right to complete!
            background = {
                val direction = dismissState.dismissDirection
                val color = when (direction) {
                    DismissDirection.EndToStart -> Color(0xFFE57373) // Premium warm red
                    DismissDirection.StartToEnd -> Color(0xFF81C784) // Premium soft green
                    else -> Color.Transparent
                }
                val alignment = when (direction) {
                    DismissDirection.EndToStart -> Alignment.CenterEnd
                    DismissDirection.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.Center
                }
                val icon = when (direction) {
                    DismissDirection.EndToStart -> Icons.Outlined.DeleteOutline
                    DismissDirection.StartToEnd -> Icons.Default.Check
                    else -> null
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(color)
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            dismissContent = {
                EventCard(
                    event = event,
                    onDelete = onDelete,
                    onComplete = onComplete,
                    onEditEvent = onEditEvent
                )
            }
        )
    }
}

@Composable
fun EventCard(
    event: CalendarEvent,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onEditEvent: (CalendarEvent) -> Unit
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var activePreviewFile by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (event.isCompleted) LightGrayBg.copy(alpha = 0.6f) else LightGrayBg)
            .clickable { showDetailsDialog = true } // Click card to trigger premium details dialog!
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.time,
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.title,
                    color = if (event.isCompleted) TextGray else DarkBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = priorityColor(event.priority).copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = event.priority.uppercase(),
                        color = priorityColor(event.priority),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            
            // Attachment Indicators on the card itself for added fidelity!
            if (!event.link.isNullOrBlank() || event.fileNames.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!event.link.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F0FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔗", fontSize = 10.sp)
                        }
                    }
                    if (event.fileNames.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F0FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📄", fontSize = 10.sp)
                        }
                    }
                }
            }

            IconButton(
                onClick = onComplete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (event.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = "Complete",
                    tint = if (event.isCompleted) Color(0xFF4CAF50) else TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Event Details",
                        color = DarkBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    IconButton(
                        onClick = {
                            showDetailsDialog = false
                            showEditDialog = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit event",
                            tint = DarkBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Title
                    Text(
                        text = event.title,
                        color = DarkBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = priorityColor(event.priority).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${event.priority.uppercase()} PRIORITY",
                            color = priorityColor(event.priority),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Time info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F0FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = "Time",
                                tint = DarkBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("TIME", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(event.time, color = DarkBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Date info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F0FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = "Calendar Date",
                                tint = DarkBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("DATE", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${event.month} ${event.day}, ${event.year}", color = DarkBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Web Link (If available) - NOW LAUNCHES REAL BROWSER INTENT!
                    if (!event.link.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F0FE))
                                .clickable {
                                    Toast.makeText(context, "Opening in Web Browser...", Toast.LENGTH_SHORT).show()
                                    try {
                                        // Standard real-life Android Intent to open URL!
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.link))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Unable to launch browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Text("🔗", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WEB LINK (LAUNCH BROWSER)", color = TextGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = event.link,
                                    color = Color(0xFF1A73E8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Attached Files (If available) - NOW SUPPORTING MULTIPLE SELECTIONS!
                    if (event.fileNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ATTACHED DOCUMENTS (${event.fileNames.size})",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        event.fileNames.forEach { name ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F3F4))
                                    .clickable {
                                        activePreviewFile = name
                                        showPreviewDialog = true
                                    }
                                    .padding(12.dp)
                            ) {
                                Text("📄", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("CLICK TO PREVIEW", color = TextGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = name,
                                        color = DarkBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDetailsDialog = false
                    showEditDialog = true
                }) {
                    Text("Edit", color = DarkBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close", color = DarkBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showEditDialog) {
        EditEventDialog(
            event = event,
            onDismiss = { showEditDialog = false },
            onSave = { updatedEvent ->
                onEditEvent(updatedEvent)
                showEditDialog = false
            }
        )
    }

    // Gorgeous custom simulated document previews!
    if (showPreviewDialog) {
        FilePreviewDialog(
            fileName = activePreviewFile,
            onDismiss = { showPreviewDialog = false }
        )
    }
}

@Composable
fun EditEventDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit
) {
    var title by remember(event.id) { mutableStateOf(event.title) }
    var time by remember(event.id) { mutableStateOf(event.time) }
    var day by remember(event.id) { mutableIntStateOf(event.day) }
    var month by remember(event.id) { mutableStateOf(event.month) }
    var year by remember(event.id) { mutableStateOf(event.year.toString()) }
    var link by remember(event.id) { mutableStateOf(event.link.orEmpty()) }
    var documents by remember(event.id) { mutableStateOf(event.fileNames.joinToString(", ")) }
    var priority by remember(event.id) { mutableStateOf(event.priority) }
    var showError by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val priorities = listOf("Low", "Medium", "High")
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val timesList = listOf(
        "06:30 AM - 07:00 AM",
        "08:00 AM - 09:20 AM",
        "10:00 AM - 10:30 AM",
        "11:00 AM - 12:00 PM",
        "01:30 PM - 02:30 PM",
        "03:00 PM - 04:30 PM",
        "06:00 PM - 07:00 PM",
        "08:00 PM - 09:00 PM"
    )
    val parsedYear = year.toIntOrNull() ?: event.year
    val maxDay = daysInMonth(month, parsedYear)

    LaunchedEffect(month, parsedYear) {
        if (day > maxDay) {
            day = maxDay
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event", color = DarkBlue, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        showError = false
                    },
                    label = { Text("Title", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && title.isBlank(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                EditPickerField(
                    label = "TIME",
                    value = time,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showTimePicker = true }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditPickerField(
                        label = "DAY",
                        value = day.toString(),
                        modifier = Modifier.weight(0.7f),
                        onClick = { showDayPicker = true }
                    )
                    EditPickerField(
                        label = "MONTH",
                        value = month,
                        modifier = Modifier.weight(1.3f),
                        onClick = { showMonthPicker = true }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Link", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = documents,
                    onValueChange = { documents = it },
                    label = { Text("Documents", color = TextGray) },
                    placeholder = { Text("file.pdf, notes.txt", color = TextGray) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("PRIORITY", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorities.forEach { option ->
                        Surface(
                            color = if (priority.equals(option, ignoreCase = true)) priorityColor(option).copy(alpha = 0.18f) else LightGrayBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { priority = option }
                        ) {
                            Text(
                                text = option,
                                color = if (priority.equals(option, ignoreCase = true)) priorityColor(option) else DarkBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Title, day, and year must be valid", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val savedYear = year.toIntOrNull()
                    if (title.isBlank() || savedYear == null) {
                        showError = true
                    } else {
                        onSave(
                            event.copy(
                                title = title.uppercase(),
                                time = time,
                                day = day,
                                month = month,
                                year = savedYear,
                                link = link.takeIf { it.isNotBlank() },
                                fileNames = documents.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                priority = priority
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DarkBlue, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time", color = DarkBlue, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .height(240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    timesList.forEach { timeSlot ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (time == timeSlot) LightBlueBg else LightGrayBg)
                                .clickable {
                                    time = timeSlot
                                    showTimePicker = false
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = timeSlot,
                                color = DarkBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text("Select Month", color = DarkBlue, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .height(280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    months.forEach { option ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (month == option) LightBlueBg else LightGrayBg)
                                .clickable {
                                    month = option
                                    showMonthPicker = false
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = option,
                                color = DarkBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showDayPicker) {
        AlertDialog(
            onDismissRequest = { showDayPicker = false },
            title = { Text("Select Day", color = DarkBlue, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select a day in $month $parsedYear:", color = TextGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    (1..maxDay).toList().chunked(7).forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowDays.forEach { dayNum ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (day == dayNum) DarkBlue else Color.Transparent)
                                        .clickable {
                                            day = dayNum
                                            showDayPicker = false
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        color = if (day == dayNum) Color.White else DarkBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            },
            confirmButton = {},
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun FilePreviewDialog(
    fileName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📄", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = fileName,
                    color = DarkBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                when {
                    fileName.contains("BoardingPass.pdf", ignoreCase = true) -> {
                        // High-fidelity Boarding Pass preview!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFE8F0FE))
                                .border(1.dp, Color(0xFF1A73E8).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("BOARDING PASS", color = Color(0xFF1A73E8), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    Text("FIRST CLASS", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("PASSENGER", color = TextGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text("ALESSANDRO", color = DarkBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("FLIGHT", color = TextGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text("NYC-778", color = DarkBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("SEAT", color = TextGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text("12A", color = DarkBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("GATE", color = TextGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text("A-14", color = DarkBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("BOARDING", color = TextGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text("01:20 PM", color = Color(0xFF4CAF50), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                // Simulated Barcode layout!
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .background(Color.White)
                                        .padding(horizontal = 24.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(2, 4, 1, 3, 2, 5, 1, 2, 4, 1, 3, 2, 4, 2, 1, 3).forEach { weight ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(weight.dp)
                                                .background(Color.Black)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                }
                            }
                        }
                    }
                    fileName.contains("MeetingAgenda.pdf", ignoreCase = true) -> {
                        // High-fidelity Meeting Agenda preview!
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF8F9FA))
                                .border(1.dp, GrayBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text("MEETING AGENDA", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            listOf(
                                "1. Welcome & Coffee (10m)",
                                "2. Sprint Backlog Alignments (20m)",
                                "3. Interface Design & Navigation (30m)",
                                "4. Question & Answer Session (15m)"
                            ).forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("•", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item, color = DarkBlue, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                    fileName.contains("SprintBacklog.xlsx", ignoreCase = true) -> {
                        // High-fidelity Spreadsheet preview!
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFE8F5E9)) // Light green for spreadsheet feel!
                                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TASK", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text("ASSIGNED", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text("STATUS", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFF4CAF50).copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            val tasks = listOf(
                                Triple("UI Redesign", "Ale", "Done"),
                                Triple("M3 Menu", "Jarvis", "Done"),
                                Triple("Swipe Gestures", "Ale", "In Progress")
                            )
                            tasks.forEach { (task, dev, status) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(task, color = DarkBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(100.dp))
                                    Text(dev, color = TextGray, fontSize = 11.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (status == "Done") Color(0xFFC8E6C9) else Color(0xFFFFE0B2))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = status,
                                            color = if (status == "Done") Color(0xFF2E7D32) else Color(0xFFE65100),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                    else -> {
                        // Standard document details view
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF1F3F4))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📁 Document Viewer Simulation", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Simulating file decryption...", color = TextGray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = DarkBlue,
                                trackColor = Color.LightGray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close Preview", color = DarkBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    Toast.makeText(context, "Opening document in local storage...", Toast.LENGTH_SHORT).show()
                    try {
                        // 1. Create a dummy physical file inside context.cacheDir
                        val file = java.io.File(context.cacheDir, fileName)
                        file.writeText(
                            "This is the mock physical content of the shared document: $fileName.\n\n" +
                            "Under real-world usage, this contains the decrypted secure binary document stream."
                        )
                        
                        // 2. Generate a secure FileProvider URI
                        val fileUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "com.example.calendarapp.fileprovider",
                            file
                        )
                        
                        // 3. Launch an ACTION_VIEW Intent with package query fallback
                        val mimeType = getMimeType(fileName)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(fileUri, mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } catch (e1: Exception) {
                            // Fallback: try opening as text/plain so even empty emulators can preview the mock file content!
                            try {
                                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(fileUri, "text/plain")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(fallbackIntent)
                            } catch (e2: Exception) {
                                // Double Fallback: Show location to user directly
                                Toast.makeText(
                                    context, 
                                    "No viewer installed. File saved to Cache: ${file.absolutePath}", 
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot prepare file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apri File", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

fun getMimeType(fileName: String): String {
    return when {
        fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
        fileName.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        fileName.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        fileName.endsWith(".pptx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        else -> "text/plain"
    }
}
