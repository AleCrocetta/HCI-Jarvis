package com.example.calendarapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.calendarapp.ui.theme.DarkSlate
import com.example.calendarapp.ui.theme.LightSlate
import com.example.calendarapp.ui.theme.OutlineColor
import java.time.LocalDate

@Composable
fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    hasEvent: Boolean, // Kept for compatibility, though currently unused for UI
    onClick: () -> Unit
) {
    val isPast = date.isBefore(LocalDate.now())
    val isToday = date == LocalDate.now()

    val textColor = when {
        isSelected -> Color.White
        !isCurrentMonth -> OutlineColor.copy(alpha = 0.3f)
        isPast -> LightSlate
        else -> DarkSlate
    }

    val backgroundColor = when {
        isSelected -> DarkSlate
        else -> Color.Transparent
    }

    val borderModifier = if (isToday && !isSelected) {
        Modifier.border(1.dp, DarkSlate, CircleShape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(borderModifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}
