package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.DailyTracker
import com.example.database.TrackerDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TrackerUiState(
    val selectedDate: Date = Date(),
    val dateStringFormatted: String = "",
    val currentTracker: DailyTracker = DailyTracker(date = ""),
    val history: List<DailyTracker> = emptyList(),
    val totalPrayersCompleted: Int = 0
)

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TrackerDatabase.getDatabase(application)
    private val dao = db.trackerDao()

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMMM, yyyy", Locale("bn", "BD"))

    init {
        updateDate(Date())
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            dao.getAllHistory().collectLatest { list ->
                _uiState.update { it.copy(history = list) }
            }
        }
    }

    fun changeDate(daysOffset: Int) {
        val cal = Calendar.getInstance()
        cal.time = _uiState.value.selectedDate
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        updateDate(cal.time)
    }

    fun jumpToToday() {
        updateDate(Date())
    }

    private fun updateDate(date: Date) {
        val dateKey = apiDateFormat.format(date)
        val formatted = displayDateFormat.format(date)
        
        viewModelScope.launch {
            val existing = dao.getTrackerForDate(dateKey) ?: DailyTracker(date = dateKey)
            _uiState.update { 
                it.copy(
                    selectedDate = date,
                    dateStringFormatted = formatted,
                    currentTracker = existing,
                    totalPrayersCompleted = calculatePrayers(existing)
                )
            }
        }
    }

    private fun calculatePrayers(tracker: DailyTracker): Int {
        var count = 0
        if (tracker.fajr) count++
        if (tracker.dhuhr) count++
        if (tracker.asr) count++
        if (tracker.maghrib) count++
        if (tracker.isha) count++
        return count
    }

    private fun updateCurrentTrackerInDb(updated: DailyTracker) {
        viewModelScope.launch {
            dao.insertOrUpdate(updated)
            _uiState.update { 
                it.copy(
                    currentTracker = updated,
                    totalPrayersCompleted = calculatePrayers(updated)
                )
            }
        }
    }

    fun togglePrayer(prayerName: String) {
        val current = _uiState.value.currentTracker
        val updated = when (prayerName) {
            "Fajr" -> current.copy(fajr = !current.fajr)
            "Dhuhr" -> current.copy(dhuhr = !current.dhuhr)
            "Asr" -> current.copy(asr = !current.asr)
            "Maghrib" -> current.copy(maghrib = !current.maghrib)
            "Isha" -> current.copy(isha = !current.isha)
            else -> current
        }
        updateCurrentTrackerInDb(updated)
    }

    fun toggleHabit(habitName: String) {
        val current = _uiState.value.currentTracker
        val updated = when (habitName) {
            "quran" -> current.copy(quran = !current.quran)
            "charity" -> current.copy(charity = !current.charity)
            "reading" -> current.copy(reading = !current.reading)
            "istighfar" -> current.copy(istighfar = !current.istighfar)
            "parents" -> current.copy(parents = !current.parents)
            else -> current
        }
        updateCurrentTrackerInDb(updated)
    }

    fun incrementTasbih() {
        val current = _uiState.value.currentTracker
        val updated = current.copy(tasbihCount = current.tasbihCount + 1)
        updateCurrentTrackerInDb(updated)
    }

    fun resetTasbih() {
        val current = _uiState.value.currentTracker
        val updated = current.copy(tasbihCount = 0)
        updateCurrentTrackerInDb(updated)
    }
}
