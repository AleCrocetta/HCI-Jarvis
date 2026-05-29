package com.example.calendarapp.model

import java.util.UUID

data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    val day: Int,
    val month: String = "February",
    val year: Int = 2026,
    val title: String,
    val time: String,
    val priority: String = "Medium",
    val link: String? = null,
    val fileNames: List<String> = emptyList(),
    val showDelete: Boolean = true,
    val isCompleted: Boolean = false
)
