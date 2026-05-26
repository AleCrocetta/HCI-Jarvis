package com.example.calendarapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.example.calendarapp.model.CalendarEvent
import com.example.calendarapp.ui.theme.*

private fun monthNameToIndex(monthName: String): Int {
    return when (monthName.lowercase(java.util.Locale.US)) {
        "january" -> 0
        "february" -> 1
        "march" -> 2
        "april" -> 3
        "may" -> 4
        "june" -> 5
        "july" -> 6
        "august" -> 7
        "september" -> 8
        "october" -> 9
        "november" -> 10
        "december" -> 11
        else -> 0
    }
}

@Composable
fun CalendarGrid(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    events: List<CalendarEvent>,
    selectedMonth: String,
    selectedYear: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val daysOfWeek = listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr")
    
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, selectedYear)
        set(java.util.Calendar.MONTH, monthNameToIndex(selectedMonth))
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

    // Sunday = 1, Monday = 2, Tuesday = 3, Wednesday = 4, Thursday = 5, Friday = 6, Saturday = 7.
    // Starting on Saturday:
    // Saturday (7) -> 0
    // Sunday (1) -> 1
    // ...
    // Friday (6) -> 6
    // Formula: dayOfWeek % 7
    val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
    val startDayOffset = dayOfWeek % 7

    val prevCal = (cal.clone() as java.util.Calendar).apply {
        add(java.util.Calendar.MONTH, -1)
    }
    val daysInPrevMonth = prevCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    
    val previousMonthDays = if (startDayOffset > 0) {
        ((daysInPrevMonth - startDayOffset + 1)..daysInPrevMonth).toList()
    } else {
        emptyList()
    }
    
    var offsetX by remember { mutableStateOf(0f) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pointerInput(selectedMonth, selectedYear) {
                detectHorizontalDragGestures(
                    onDragStart = { offsetX = 0f },
                    onDragEnd = {
                        if (offsetX > 150f) {
                            onPreviousMonth()
                        } else if (offsetX < -150f) {
                            onNextMonth()
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
    ) {
        // Days of week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    color = DarkBlue,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Calendar Grid
        val totalDaysToShow = daysInMonth + startDayOffset
        val numRows = if (totalDaysToShow > 35) 6 else 5
        var currentDay = 1
        var nextMonthDay = 1
        
        for (row in 0 until numRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cellIndex < startDayOffset) {
                            // Previous month days
                            val day = previousMonthDays.getOrNull(cellIndex) ?: 30
                            CalendarCell(day = day.toString(), isPast = true)
                        } else if (currentDay <= daysInMonth) {
                            // Current month days
                            val dayNum = currentDay
                            val isSelected = selectedDay == dayNum
                            
                            // Check events for indicators dynamically matching BOTH day, selectedMonth AND selectedYear!
                            val dayEvents = events.filter { it.day == dayNum && it.month.equals(selectedMonth, ignoreCase = true) && it.year == selectedYear }
                            val hasFlight = dayEvents.any { it.title.contains("FLIGHT", ignoreCase = true) || it.title.contains("TRIP", ignoreCase = true) || it.title.contains("PLANE", ignoreCase = true) }
                            val hasBorder = dayEvents.isNotEmpty() && !hasFlight
                            
                            CalendarCell(
                                day = dayNum.toString(),
                                isSelected = isSelected,
                                hasBorder = hasBorder,
                                hasIcon = hasFlight,
                                onClick = { onDaySelected(dayNum) }
                            )
                            currentDay++
                        } else {
                            // Next month days
                            CalendarCell(day = nextMonthDay.toString(), isPast = true)
                            nextMonthDay++
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarCell(
    day: String,
    isSelected: Boolean = false,
    isPast: Boolean = false,
    hasBorder: Boolean = false,
    hasIcon: Boolean = false,
    onClick: () -> Unit = {}
) {
    val bgColor = when {
        isSelected -> CircleBgSelected
        isPast -> Color.Transparent
        hasBorder -> Color.White
        else -> CircleBgNormal
    }
    
    val textColor = when {
        isSelected -> White
        isPast -> Color(0xFFD0D0D0) // Light gray for past days
        else -> DarkBlue
    }
    
    val borderModifier = if (hasBorder) {
        Modifier.border(1.dp, GrayBorder, CircleShape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bgColor)
                .then(borderModifier)
                .clickable(enabled = !isPast) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day,
                color = textColor,
                fontWeight = if (isSelected || !isPast) FontWeight.Medium else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
        
        if (hasIcon) {
            Icon(
                imageVector = Icons.Default.AirplanemodeActive,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = 4.dp)
            )
        }
    }
}
