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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import com.example.calendarapp.model.CalendarEvent
import com.example.calendarapp.ui.theme.*
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState

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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CalendarGrid(
    selectedDay: Int,
    selectedDayMonth: String,
    selectedDayYear: Int,
    onDaySelected: (Int, String, Int) -> Unit,
    events: List<CalendarEvent>,
    selectedMonth: String,
    selectedYear: Int,
    onMonthSelected: (String) -> Unit,
    onYearSelected: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val daysOfWeek = listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr")
    val today = remember { LocalDate.now() }
    val monthsList = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Calculate current absolute month index for epoch
    val currentMonthEpoch = selectedYear * 12 + monthNameToIndex(selectedMonth)
    
    val pagerState = rememberPagerState(
        initialPage = currentMonthEpoch,
        pageCount = { 3000 * 12 } // Range of 3000 years!
    )
    
    val coroutineScope = rememberCoroutineScope()
    
    // Sync pagerState -> parentState (when user swipes)
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) return@LaunchedEffect
        delay(300)
        if (pagerState.isScrollInProgress) return@LaunchedEffect
        val targetEpoch = pagerState.currentPage
        val targetYear = targetEpoch / 12
        val targetMonthIndex = targetEpoch % 12
        val targetMonthName = monthsList[targetMonthIndex]
        
        if (targetYear != selectedYear || targetMonthName != selectedMonth) {
            onMonthSelected(targetMonthName)
            onYearSelected(targetYear)
        }
    }
    
    // Sync parentState -> pagerState (when top bar selects a month)
    LaunchedEffect(selectedMonth, selectedYear) {
        val targetEpoch = selectedYear * 12 + monthNameToIndex(selectedMonth)
        if (pagerState.currentPage != targetEpoch) {
            pagerState.scrollToPage(targetEpoch)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Days of week header (Static at the top!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    color = DarkBlue,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Vertical Snapping Pager for Days Grid ( Instagram/TikTok Reel Paging feel!)
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(252.dp)
        ) { page ->
            val pageYear = page / 12
            val pageMonthName = monthsList[page % 12]
            
            CalendarMonthGrid(
                selectedDay = selectedDay,
                selectedDayMonth = selectedDayMonth,
                selectedDayYear = selectedDayYear,
                onDaySelected = onDaySelected,
                events = events,
                selectedMonth = pageMonthName,
                selectedYear = pageYear,
                today = today,
                monthsList = monthsList,
                onPreviousMonth = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                onNextMonth = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            )
        }
    }
}

@Composable
fun CalendarMonthGrid(
    selectedDay: Int,
    selectedDayMonth: String,
    selectedDayYear: Int,
    onDaySelected: (Int, String, Int) -> Unit,
    events: List<CalendarEvent>,
    selectedMonth: String,
    selectedYear: Int,
    today: LocalDate,
    monthsList: List<String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, selectedYear)
        set(java.util.Calendar.MONTH, monthNameToIndex(selectedMonth))
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

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

    val currentMonthIndex = monthsList.indexOf(selectedMonth)
    val nextMonthName = monthsList[(currentMonthIndex + 1) % 12]
    val nextMonthYear = if (selectedMonth == "December") selectedYear + 1 else selectedYear
    val prevMonthName = monthsList[if (currentMonthIndex == 0) 11 else currentMonthIndex - 1]
    val prevMonthYear = if (selectedMonth == "January") selectedYear - 1 else selectedYear

    var currentDay = 1
    var nextMonthDay = 1
    
    // Always render exactly 6 rows to lock the grid height perfectly!
    val numRows = 6

    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until numRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cellIndex < startDayOffset) {
                            val day = previousMonthDays.getOrNull(cellIndex) ?: 30
                            CalendarCell(
                                day = day.toString(),
                                isPast = true,
                                bgColor = Color.Transparent,
                                textColor = Color(0xFFBDBDBD),
                                onClick = {
                                    onPreviousMonth()
                                    onDaySelected(day, prevMonthName, prevMonthYear)
                                }
                            )
                        } else if (currentDay <= daysInMonth) {
                            val dayNum = currentDay
                            val isSelected = selectedDay == dayNum && 
                                             selectedMonth.equals(selectedDayMonth, ignoreCase = true) && 
                                             selectedYear == selectedDayYear
                            
                            val dayEvents = events.filter { it.day == dayNum && it.month.equals(selectedMonth, ignoreCase = true) && it.year == selectedYear }
                            
                            val cellDate = try {
                                LocalDate.of(selectedYear, monthNameToIndex(selectedMonth) + 1, dayNum)
                            } catch (e: Exception) {
                                null
                            }
                            val isCellPast = cellDate != null && cellDate.isBefore(today)
                            val isToday = cellDate != null && cellDate.isEqual(today)

                            val remainingEvents = dayEvents.count { !it.isCompleted }
                            val cellBgColor = when {
                                isCellPast -> Color(0xFFE0E0E0)
                                dayEvents.isEmpty() -> Color.Transparent
                                remainingEvents == 0 -> Color(0xFF81C784)
                                remainingEvents in 1..2 -> Color(0xFFFFF9C4)
                                remainingEvents in 3..5 -> Color(0xFFFFCC80)
                                else -> Color(0xFFEF5350)
                            }

                            val cellTextColor = when {
                                isCellPast -> Color(0xFF757575)
                                isSelected && cellBgColor == Color.Transparent -> White
                                cellBgColor == Color(0xFFEF5350) -> White
                                else -> DarkBlue
                            }
                            
                            CalendarCell(
                                day = dayNum.toString(),
                                isSelected = isSelected,
                                isToday = isToday,
                                bgColor = cellBgColor,
                                textColor = cellTextColor,
                                isPast = isCellPast,
                                onClick = { onDaySelected(dayNum, selectedMonth, selectedYear) }
                            )
                            currentDay++
                        } else {
                            val dayNum = nextMonthDay
                            CalendarCell(
                                day = dayNum.toString(),
                                isSelected = false,
                                bgColor = Color.Transparent,
                                textColor = Color(0xFFBDBDBD),
                                isPast = true,
                                onClick = {
                                    onNextMonth()
                                    onDaySelected(dayNum, nextMonthName, nextMonthYear)
                                }
                            )
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
    isToday: Boolean = false,
    bgColor: Color = Color.Transparent,
    textColor: Color = DarkBlue,
    isPast: Boolean = false,
    onClick: () -> Unit = {}
) {
    val actualBgColor = when {
        isSelected && bgColor == Color.Transparent -> CircleBgSelected
        else -> bgColor
    }
    
    val actualTextColor = when {
        isSelected && bgColor == Color.Transparent -> White
        else -> textColor
    }
    
    val borderModifier = if (isToday) {
        Modifier.border(3.dp, PrimaryBlue, CircleShape)
    } else if (isSelected && bgColor != Color.Transparent) {
        Modifier.border(2.dp, DarkBlue, CircleShape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(actualBgColor)
                .then(borderModifier)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day,
                color = actualTextColor,
                fontWeight = if (isToday || isSelected || !isPast) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            )
        }
    }
}
