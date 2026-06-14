package com.example.calendarapp.ui.home

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarapp.model.CalendarEvent
import com.example.calendarapp.ui.theme.DarkBlue
import com.example.calendarapp.ui.theme.GrayBorder
import com.example.calendarapp.ui.theme.LightBlueBg
import com.example.calendarapp.ui.theme.LightGrayBg
import com.example.calendarapp.ui.theme.TextGray
import kotlinx.coroutines.delay
import kotlin.math.abs

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

private fun monthIndex(month: String): Int {
    return listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ).indexOfFirst { it.equals(month, ignoreCase = true) }.coerceAtLeast(0)
}

private fun firstDayOffset(month: String, year: Int): Int {
    val dayOfWeek = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, year)
        set(java.util.Calendar.MONTH, monthIndex(month))
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }.get(java.util.Calendar.DAY_OF_WEEK)
    return (dayOfWeek + 5) % 7
}

private fun formatClockTime(totalMinutes: Int): String {
    val normalized = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val hour24 = normalized / 60
    val minute = normalized % 60
    val marker = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "%02d:%02d %s".format(hour12, minute, marker)
}

private fun parseStartMinutes(time: String): Int {
    val match = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""", RegexOption.IGNORE_CASE).find(time) ?: return 10 * 60
    var hour = match.groupValues[1].toIntOrNull() ?: return 10 * 60
    val minute = match.groupValues[2].toIntOrNull() ?: return 10 * 60
    val marker = match.groupValues[3].uppercase()
    if (marker == "PM" && hour != 12) hour += 12
    if (marker == "AM" && hour == 12) hour = 0
    return hour * 60 + minute
}

private fun parseEndMinutes(time: String): Int {
    val matches = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""", RegexOption.IGNORE_CASE).findAll(time).toList()
    val match = matches.getOrNull(1) ?: return parseStartMinutes(time) + 60
    var hour = match.groupValues[1].toIntOrNull() ?: return parseStartMinutes(time) + 60
    val minute = match.groupValues[2].toIntOrNull() ?: return parseStartMinutes(time) + 60
    val marker = match.groupValues[3].uppercase()
    if (marker == "PM" && hour != 12) hour += 12
    if (marker == "AM" && hour == 12) hour = 0
    return hour * 60 + minute
}

private fun formatTimeRange(startMinutes: Int, endMinutes: Int): String {
    return "${formatClockTime(startMinutes)} - ${formatClockTime(endMinutes)}"
}

private fun normalizeWebUri(link: String): Uri? {
    val trimmed = link.trim()
    if (trimmed.isBlank()) return null
    val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    return runCatching { Uri.parse(withScheme) }.getOrNull()
}

private fun getEventFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    result = cursor.getString(nameIndex)
                }
            }
        }
    }
    return result ?: uri.path?.substringAfterLast('/') ?: "Document"
}

private fun supportsTextPreview(fileName: String): Boolean {
    return listOf(".txt", ".md", ".csv", ".json", ".xml", ".log").any { fileName.endsWith(it, ignoreCase = true) }
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TimeWheelColumn(
    label: String,
    values: List<Int>,
    selectedValue: Int,
    modifier: Modifier = Modifier,
    onValueSelected: (Int) -> Unit
) {
    val visibleItems = 5
    val halfVisible = visibleItems / 2
    val itemHeightDp = 44.dp
    val initialIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex
    )
    val snapFlingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
        lazyListState = listState
    )

    // Derive which item is centered
    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
            layoutInfo.visibleItemsInfo
                .filter { it.index in 0 until values.size + halfVisible * 2 }
                .minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index?.minus(halfVisible)?.coerceIn(0, values.size - 1)
                ?: initialIndex
        }
    }

    // Notify parent of selection changes
    LaunchedEffect(centeredIndex) {
        if (centeredIndex in values.indices) {
            onValueSelected(values[centeredIndex])
        }
    }

    val totalHeight = itemHeightDp * visibleItems

    Column(modifier = modifier) {
        Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(LightGrayBg)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                flingBehavior = snapFlingBehavior,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top padding spacers
                items(halfVisible) {
                    Spacer(modifier = Modifier.height(itemHeightDp).fillMaxWidth())
                }

                // Actual value items
                items(values.size) { index ->
                    val isSelected = index == centeredIndex
                    val distanceFromCenter = kotlin.math.abs(index - centeredIndex)
                    val alpha = when (distanceFromCenter) {
                        0 -> 1f
                        1 -> 0.55f
                        else -> 0.3f
                    }

                    Box(
                        modifier = Modifier
                            .height(itemHeightDp)
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) LightBlueBg else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%02d".format(values[index]),
                            color = if (isSelected) DarkBlue else DarkBlue.copy(alpha = alpha),
                            fontSize = if (isSelected) 22.sp else 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Bottom padding spacers
                items(halfVisible) {
                    Spacer(modifier = Modifier.height(itemHeightDp).fillMaxWidth())
                }
            }

            // Top fade overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeightDp * 1.2f)
                    .align(Alignment.TopCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                LightGrayBg,
                                LightGrayBg.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom fade overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeightDp * 1.2f)
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                LightGrayBg.copy(alpha = 0.7f),
                                LightGrayBg
                            )
                        )
                    )
            )

            // Center selection indicator lines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeightDp)
                    .align(Alignment.Center)
                    .padding(horizontal = 4.dp)
            ) {
                Divider(
                    color = DarkBlue.copy(alpha = 0.15f),
                    thickness = 1.5.dp,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                Divider(
                    color = DarkBlue.copy(alpha = 0.15f),
                    thickness = 1.5.dp,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun EventList(
    events: List<CalendarEvent>,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onCompleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    highlightedEventId: String? = null
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
                        onEditEvent = onEditEvent,
                        isHighlighted = event.id == highlightedEventId
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
    onEditEvent: (CalendarEvent) -> Unit,
    isHighlighted: Boolean = false
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
                    onEditEvent = onEditEvent,
                    isHighlighted = isHighlighted
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
    onEditEvent: (CalendarEvent) -> Unit,
    isHighlighted: Boolean = false
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var activePreviewFile by remember { mutableStateOf("") }
    var activePreviewUri by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current

    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(if (isHighlighted) LightBlueBg else if (event.isCompleted) LightGrayBg.copy(alpha = 0.6f) else LightGrayBg)
            .then(if (isHighlighted) Modifier.border(1.5.dp, Color(0xFF4A90E2).copy(alpha = 0.55f), cardShape) else Modifier)
            .clickable { showDetailsDialog = true } // Click card to trigger premium details dialog!
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                                        val uri = normalizeWebUri(event.link.orEmpty())
                                        if (uri == null) {
                                            Toast.makeText(context, "Invalid link", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                            addCategory(Intent.CATEGORY_BROWSABLE)
                                        }
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
                        event.fileNames.forEachIndexed { index, name ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F3F4))
                                    .clickable {
                                        activePreviewFile = name
                                        activePreviewUri = event.fileUris.getOrNull(index)?.takeIf { it.isNotBlank() }
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
            fileUri = activePreviewUri,
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
    var documentUris by remember(event.id) { mutableStateOf(event.fileUris) }
    var priority by remember(event.id) { mutableStateOf(event.priority) }
    var showError by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var startMinutes by remember(event.id) { mutableIntStateOf(parseStartMinutes(event.time)) }
    var endMinutes by remember(event.id) {
        val parsedStart = parseStartMinutes(event.time)
        val parsedEnd = parseEndMinutes(event.time)
        mutableIntStateOf(if (parsedEnd > parsedStart) parsedEnd else parsedStart + 60)
    }
    val priorities = listOf("Low", "Medium", "High")
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val parsedYear = year.toIntOrNull() ?: event.year
    val maxDay = daysInMonth(month, parsedYear)
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
        }
        val selectedNames = uris.map { uri -> getEventFileNameFromUri(context, uri) }
        val currentNames = documents.split(",").map { it.trim() }.filter { it.isNotBlank() }
        documents = (currentNames + selectedNames).distinct().joinToString(", ")
        documentUris = documentUris + uris.map { it.toString() }
    }

    LaunchedEffect(month, parsedYear) {
        if (day > maxDay) {
            day = maxDay
        }
    }

    fun moveEditMonth(delta: Int) {
        val currentIndex = months.indexOf(month)
        val nextIndex = currentIndex + delta
        when {
            nextIndex < 0 -> {
                month = "December"
                year = (parsedYear - 1).toString()
            }
            nextIndex > months.lastIndex -> {
                month = "January"
                year = (parsedYear + 1).toString()
            }
            else -> month = months[nextIndex]
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
                        label = "DATE",
                        value = "$month $day, $parsedYear",
                        modifier = Modifier.weight(1f),
                        onClick = { showDatePicker = true }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Link", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LightGrayBg)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DOCUMENTS", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (documents.isBlank()) "No documents attached" else documents,
                                color = DarkBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Upload", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
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
                    Text("Title and time range must be valid", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val savedYear = year.toIntOrNull()
                    if (title.isBlank() || savedYear == null || endMinutes <= startMinutes) {
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
                                fileUris = documentUris,
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
                        .fillMaxWidth()
                ) {
                    Text("START TIME", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimeWheelColumn(
                            label = "HOUR",
                            values = (0..23).toList(),
                            selectedValue = startMinutes / 60,
                            modifier = Modifier.weight(1f),
                            onValueSelected = { hour ->
                                startMinutes = hour * 60 + (startMinutes % 60)
                            }
                        )
                        TimeWheelColumn(
                            label = "MIN",
                            values = (0..55 step 5).toList(),
                            selectedValue = startMinutes % 60,
                            modifier = Modifier.weight(1f),
                            onValueSelected = { minute ->
                                startMinutes = (startMinutes / 60) * 60 + minute
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("FINISH TIME", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimeWheelColumn(
                            label = "HOUR",
                            values = (0..23).toList(),
                            selectedValue = endMinutes / 60,
                            modifier = Modifier.weight(1f),
                            onValueSelected = { hour ->
                                endMinutes = hour * 60 + (endMinutes % 60)
                            }
                        )
                        TimeWheelColumn(
                            label = "MIN",
                            values = (0..55 step 5).toList(),
                            selectedValue = endMinutes % 60,
                            modifier = Modifier.weight(1f),
                            onValueSelected = { minute ->
                                endMinutes = (endMinutes / 60) * 60 + minute
                            }
                        )
                    }

                    if (endMinutes <= startMinutes) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Finish time must be after start time", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (endMinutes > startMinutes) {
                        time = formatTimeRange(startMinutes, endMinutes)
                        showTimePicker = false
                    }
                }) {
                    Text("OK", color = DarkBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = DarkBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("Select Date", color = DarkBlue, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(month, parsedYear) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = { totalDrag = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                    if (totalDrag > 80f) {
                                        moveEditMonth(-1)
                                        totalDrag = 0f
                                    } else if (totalDrag < -80f) {
                                        moveEditMonth(1)
                                        totalDrag = 0f
                                    }
                                }
                            )
                        }
                ) {
                    Text("$day ${month.take(3)} $parsedYear", color = DarkBlue, fontSize = 38.sp, fontWeight = FontWeight.Light)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$month $parsedYear", color = DarkBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Row {
                            IconButton(onClick = { moveEditMonth(-1) }) {
                                Text("<", color = DarkBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { moveEditMonth(1) }) {
                                Text(">", color = DarkBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                            Text(label, color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val calendarCells = List(firstDayOffset(month, parsedYear)) { null } + (1..maxDay).map { it }
                    calendarCells.chunked(7).forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowDays.forEach { dayNum ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (dayNum != null && day == dayNum) DarkBlue else Color.Transparent)
                                        .clickable {
                                            if (dayNum != null) {
                                                day = dayNum
                                            }
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum?.toString().orEmpty(),
                                        color = if (dayNum != null && day == dayNum) Color.White else DarkBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            repeat(7 - rowDays.size) {
                                Spacer(modifier = Modifier.size(36.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK", color = DarkBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = DarkBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun FilePreviewDialog(
    fileName: String,
    fileUri: String? = null,
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
                    supportsTextPreview(fileName) && fileName.contains("BoardingPass.pdf", ignoreCase = true) -> {
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
                    supportsTextPreview(fileName) && fileName.contains("MeetingAgenda.pdf", ignoreCase = true) -> {
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
                    supportsTextPreview(fileName) && fileName.contains("SprintBacklog.xlsx", ignoreCase = true) -> {
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
                        // Text preview only; binary documents must be opened externally.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF1F3F4))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (supportsTextPreview(fileName)) {
                                Text("Text Preview", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Preview content for $fileName\n\nOpen the file to view the original document.",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            } else {
                                Text("Preview not supported", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "This file type can be opened with an external app if one is installed.",
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
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
                    Toast.makeText(context, "Opening attached file...", Toast.LENGTH_SHORT).show()
                    try {
                        val openUri = fileUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                        if (openUri == null) {
                            Toast.makeText(context, "File reference missing. Please reattach this file.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val mimeType = context.contentResolver.getType(openUri) ?: getMimeType(fileName)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(openUri, mimeType)
                                clipData = ClipData.newUri(context.contentResolver, fileName, openUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open attached file"))
                        } catch (e1: Exception) {
                            try {
                                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(openUri, "*/*")
                                    clipData = ClipData.newUri(context.contentResolver, fileName, openUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(fallbackIntent, "Open attached file"))
                            } catch (e2: Exception) {
                                // Double Fallback: Show location to user directly
                                Toast.makeText(
                                    context, 
                                    "No viewer installed for this file type", 
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
                Text("Open File", color = Color.White, fontWeight = FontWeight.Bold)
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
        fileName.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
        fileName.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        fileName.endsWith(".doc", ignoreCase = true) -> "application/msword"
        fileName.endsWith(".pptx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        fileName.endsWith(".ppt", ignoreCase = true) -> "application/vnd.ms-powerpoint"
        fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
        fileName.endsWith(".md", ignoreCase = true) -> "text/markdown"
        fileName.endsWith(".csv", ignoreCase = true) -> "text/csv"
        fileName.endsWith(".json", ignoreCase = true) -> "application/json"
        fileName.endsWith(".xml", ignoreCase = true) -> "application/xml"
        fileName.endsWith(".png", ignoreCase = true) -> "image/png"
        fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
        fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
        fileName.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
        fileName.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
        fileName.endsWith(".zip", ignoreCase = true) -> "application/zip"
        else -> "*/*"
    }
}
