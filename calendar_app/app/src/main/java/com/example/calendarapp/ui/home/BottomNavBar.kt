package com.example.calendarapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.calendarapp.ui.theme.DarkBlue
import com.example.calendarapp.ui.theme.LightBlueBg
import com.example.calendarapp.ui.theme.TextGray
import com.example.calendarapp.ui.theme.White

@Composable
fun BottomNavBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            tabName = "home",
            icon = Icons.Default.Home,
            contentDescription = "Home",
            isActive = activeTab == "home",
            onClick = { onTabSelected("home") }
        )
        
        BottomNavItem(
            tabName = "chart",
            icon = Icons.Default.BarChart,
            contentDescription = "Chart",
            isActive = activeTab == "chart",
            onClick = { onTabSelected("chart") }
        )
        
        BottomNavItem(
            tabName = "calendar",
            icon = Icons.Outlined.Event,
            contentDescription = "Calendar",
            isActive = activeTab == "calendar",
            onClick = { onTabSelected("calendar") }
        )
        
        BottomNavItem(
            tabName = "time",
            icon = Icons.Outlined.AccessTime,
            contentDescription = "Time",
            isActive = activeTab == "time",
            onClick = { onTabSelected("time") }
        )
        
        BottomNavItem(
            tabName = "profile",
            icon = Icons.Default.Person,
            contentDescription = "Profile",
            isActive = activeTab == "profile",
            onClick = { onTabSelected("profile") }
        )
    }
}

@Composable
fun BottomNavItem(
    tabName: String,
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    if (isActive) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LightBlueBg),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick) {
                Icon(icon, contentDescription = contentDescription, tint = DarkBlue)
            }
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = TextGray)
        }
    }
}
