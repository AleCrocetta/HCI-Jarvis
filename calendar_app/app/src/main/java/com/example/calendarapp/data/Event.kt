package com.example.calendarapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val startTime: String,
    val endTime: String,
    val dateString: String, // Format "yyyy-MM-dd"
    val type: String // "TASK", "EVENT", "FLIGHT"
)
