package com.example.calendarapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendarapp.ui.theme.LightSlate
import java.time.LocalDate

@Composable
fun CalendarGrid(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysOfWeek = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    // Start from the month of the selected date
    val currentMonth = selectedDate.withDayOfMonth(1)
    val startDayOfWeek = currentMonth.dayOfWeek.value
    val daysInMonth = currentMonth.lengthOfMonth()
    
    val previousMonth = currentMonth.minusMonths(1)
    val daysInPrevMonth = previousMonth.lengthOfMonth()
    
    val totalCells = 35
    val cells = mutableListOf<LocalDate>()
    
    for (i in startDayOfWeek - 1 downTo 1) {
        cells.add(previousMonth.withDayOfMonth(daysInPrevMonth - i + 1))
    }
    for (i in 1..daysInMonth) {
        cells.add(currentMonth.withDayOfMonth(i))
    }
    var nextMonthDay = 1
    while (cells.size < totalCells) {
        cells.add(currentMonth.plusMonths(1).withDayOfMonth(nextMonthDay++))
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightSlate,
                    modifier = Modifier.width(40.dp).wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        for (row in 0 until 5) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until 7) {
                    val date = cells[row * 7 + col]
                    val isSelected = date == selectedDate
                    val isCurrentMonth = date.month == currentMonth.month
                    val hasEvent = (date.dayOfMonth == 4 || date.dayOfMonth == 13 || date.dayOfMonth == 27) && isCurrentMonth 
                    
                    DayCell(
                        date = date,
                        isSelected = isSelected,
                        isCurrentMonth = isCurrentMonth,
                        hasEvent = hasEvent,
                        onClick = { onDateSelected(date) }
                    )
                }
            }
        }
    }
}
