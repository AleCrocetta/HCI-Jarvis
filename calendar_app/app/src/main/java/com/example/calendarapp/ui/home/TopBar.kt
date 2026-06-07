package com.example.calendarapp.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarapp.ui.theme.DarkBlue
import com.example.calendarapp.ui.theme.LightBlueBg
import com.example.calendarapp.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    selectedMonth: String,
    selectedYear: Int,
    onMonthClick: () -> Unit,
    onTodayClick: () -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    val todayDay = remember { java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSearchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search events...", color = TextGray, fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                trailingIcon = {
                    IconButton(onClick = { 
                        isSearchExpanded = false
                        onSearchQueryChanged("") // Clear search on collapse
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close search", tint = DarkBlue)
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkBlue,
                    unfocusedBorderColor = TextGray,
                    focusedContainerColor = LightBlueBg,
                    unfocusedContainerColor = LightBlueBg
                ),
                shape = CircleShape
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .border(1.dp, DarkBlue.copy(alpha = 0.12f), CircleShape)
                            .clickable { onMonthClick() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Calendar",
                                tint = TextGray,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$selectedMonth $selectedYear",
                                color = DarkBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .border(1.dp, DarkBlue.copy(alpha = 0.12f), CircleShape)
                            .clickable { onTodayClick() }
                    ) {
                        Text(
                            text = "Today",
                            color = DarkBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            // Icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isSearchExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextGray
                    )
                }
            }
        }
    }
}
