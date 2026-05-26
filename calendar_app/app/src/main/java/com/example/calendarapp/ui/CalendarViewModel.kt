package com.example.calendarapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.data.Event
import com.example.calendarapp.data.EventDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarViewModel(private val eventDao: EventDao) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _eventsForSelectedDate = MutableStateFlow<List<Event>>(emptyList())
    val eventsForSelectedDate: StateFlow<List<Event>> = _eventsForSelectedDate.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        // Fetch initial data
        fetchEventsForDate(_selectedDate.value)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        fetchEventsForDate(date)
    }

    private fun fetchEventsForDate(date: LocalDate) {
        viewModelScope.launch {
            val dateString = date.format(dateFormatter)
            eventDao.getEventsForDate(dateString).collectLatest { events ->
                _eventsForSelectedDate.value = events
            }
        }
    }

    fun addDummyEvent() {
        viewModelScope.launch {
            val dateString = _selectedDate.value.format(dateFormatter)
            val newEvent = Event(
                title = "NEW EVENT",
                startTime = "12:00PM",
                endTime = "01:00PM",
                dateString = dateString,
                type = "EVENT"
            )
            eventDao.insertEvent(newEvent)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            eventDao.deleteEvent(event)
        }
    }
}

class CalendarViewModelFactory(private val eventDao: EventDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(eventDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
