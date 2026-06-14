package com.example.calendarapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarapp.ui.theme.DarkBlue
import com.example.calendarapp.ui.theme.TextGray
import com.example.calendarapp.ui.theme.White

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodaySection(
    selectedDay: Int,
    selectedMonth: String,
    selectedYear: Int,
    eventCount: Int
) {
    val monthIndex = when (selectedMonth.lowercase(Locale.US)) {
        "january" -> 1
        "february" -> 2
        "march" -> 3
        "april" -> 4
        "may" -> 5
        "june" -> 6
        "july" -> 7
        "august" -> 8
        "september" -> 9
        "october" -> 10
        "november" -> 11
        "december" -> 12
        else -> 1
    }

    val selectedDate = try {
        LocalDate.of(selectedYear, monthIndex, selectedDay)
    } catch (e: Exception) {
        null
    }

    val weekday = try {
        selectedDate?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.getDefault()) ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
    val sectionTitle = if (selectedDate == LocalDate.now()) "TODAY" else "SELECTED DAY"

    val eventText = when (eventCount) {
        0 -> "No events"
        1 -> "1 event"
        else -> "$eventCount events"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = sectionTitle,
            color = TextGray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = selectedDay.toString(),
                    color = DarkBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = eventText,
                        color = DarkBlue,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = weekday,
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
