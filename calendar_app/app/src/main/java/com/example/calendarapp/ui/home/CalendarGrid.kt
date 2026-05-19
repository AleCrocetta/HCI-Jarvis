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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarapp.model.CalendarEvent
import com.example.calendarapp.ui.theme.*

@Composable
fun CalendarGrid(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    events: List<CalendarEvent>,
    selectedMonth: String
) {
    val daysOfWeek = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    
    val daysInMonth = when (selectedMonth) {
        "January", "March", "May", "July", "August", "October", "December" -> 31
        "April", "June", "September", "November" -> 30
        "February" -> 28
        else -> 28
    }

    val startDayOffset = when (selectedMonth) {
        "January" -> 2 // Wednesday
        "February" -> 5 // Saturday
        "March" -> 5 // Saturday
        "April" -> 1 // Tuesday
        "May" -> 3 // Thursday
        "June" -> 6 // Sunday
        "July" -> 1 // Tuesday
        "August" -> 4 // Friday
        "September" -> 0 // Monday
        "October" -> 2 // Wednesday
        "November" -> 5 // Saturday
        "December" -> 0 // Monday
        else -> 5
    }

    val previousMonthDays = when (selectedMonth) {
        "January" -> listOf(30, 31)
        "February" -> listOf(27, 28, 29, 30, 31)
        "March" -> listOf(24, 25, 26, 27, 28)
        "April" -> listOf(31)
        "May" -> listOf(28, 29, 30)
        "June" -> listOf(26, 27, 28, 29, 30, 31)
        "July" -> listOf(30)
        "August" -> listOf(28, 29, 30, 31)
        "September" -> emptyList()
        "October" -> listOf(29, 30)
        "November" -> listOf(27, 28, 29, 30, 31)
        "December" -> emptyList()
        else -> listOf(27, 28, 29, 30, 31)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
                            
                            // Check events for indicators dynamically matching BOTH day AND selectedMonth!
                            val dayEvents = events.filter { it.day == dayNum && it.month.equals(selectedMonth, ignoreCase = true) }
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
