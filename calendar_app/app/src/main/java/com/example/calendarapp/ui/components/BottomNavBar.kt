package com.example.calendarapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendarapp.ui.theme.DarkSlate
import com.example.calendarapp.ui.theme.LightSlate

@Composable
fun BottomNavBar(modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier.height(80.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = LightSlate)
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Outlined.BarChart, contentDescription = "Stats") },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = LightSlate)
        )
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Outlined.Event, contentDescription = "Calendar") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkSlate,
                indicatorColor = com.example.calendarapp.ui.theme.EventBackground
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Outlined.Schedule, contentDescription = "Schedule") },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = LightSlate)
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Outlined.PersonOutline, contentDescription = "Profile") },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = LightSlate)
        )
    }
}
