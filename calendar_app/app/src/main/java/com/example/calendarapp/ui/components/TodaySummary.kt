package com.example.calendarapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calendarapp.ui.theme.DarkSlate
import com.example.calendarapp.ui.theme.LightSlate
import java.time.LocalDate

@Composable
fun TodaySummary(selectedDate: LocalDate, eventCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "TODAY", style = MaterialTheme.typography.labelMedium, color = LightSlate)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = selectedDate.dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = DarkSlate
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${selectedDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightSlate,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$eventCount events and tasks",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkSlate
            )
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSlate,
                onClick = { }
            ) {
                Text(
                    text = "View all",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
