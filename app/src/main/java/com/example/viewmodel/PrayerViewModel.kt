package com.example.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.PrayerCalculator
import com.example.calculator.PrayerTimes
import com.example.receiver.AlarmHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

object GlobalLanguage {
    var isEnglish: Boolean = false
}

fun String.toBengali(): String {
    if (GlobalLanguage.isEnglish) {
        val ben = listOf("০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "এএম", "পিএম")
        val eng = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "AM", "PM")
        var res = this
        ben.forEachIndexed { index, s ->
            res = res.replace(s, eng[index])
        }
        return res
    }
    val eng = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "AM", "PM", "am", "pm")
    val ben = listOf("০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "এএম", "পিএম", "এএম", "পিএম")
    var res = this
    eng.forEachIndexed { index, s ->
        res = res.replace(s, ben[index])
    }
    return res
}

data class ViewState(
    val isLoading: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val isAutoLocation: Boolean = true,
    val locationName: String = "ঢাকা",
    val selectedCountry: String = "বাংলাদেশ",
    val selectedDistrict: String = "ঢাকা",
    val currentDate: String = "",
    val prayerTimes: PrayerTimes? = null,
    val nextPrayerName: String = "Loading...",
    val nextPrayerNameBen: String = "...",
    val nextPrayerRemaining: String = "০০:০০:০০",
    val timerProgress: Float = 0f,
    val specialCountdownLabel: String = "সাহরির বাকি",
    val specialCountdownTime: String = "০০:০০:০০",
    val specialCountdownProgress: Float = 0f,
    val alarms: Map<String, Boolean> = mapOf(
        "Fajr" to true,
        "Sunrise" to false,
        "Dhuhr" to true,
        "Asr" to true,
        "Maghrib" to true,
        "Isha" to true
    ),
    val error: String? = null
)

class PrayerViewModel : ViewModel() {
    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var timerJob: Job? = null

    private var lastLat = 23.8103
    private var lastLng = 90.4125
    private var lastOffset = 6.0
    private var hasLocationData = true

    init {
        refreshState()
    }

    private fun refreshState() {
        val dateFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.US)
        val defaultTimes = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset)
        _state.update {
            it.copy(
                isLoading = false,
                currentDate = dateFormat.format(Date()).toBengali(),
                prayerTimes = defaultTimes
            )
        }
        updateNextPrayer(defaultTimes)
    }

    fun setLocationManually(districtName: String, lat: Double, lng: Double) {
        lastLat = lat
        lastLng = lng
        lastOffset = 6.0 // Bangladesh Standard Time
        hasLocationData = true
        _state.update { 
            it.copy(
                isAutoLocation = false,
                locationName = districtName,
                selectedDistrict = districtName
            ) 
        }
        refreshState()
    }

    fun setAutoLocation(context: Context) {
        _state.update { it.copy(isAutoLocation = true) }
        startLocationUpdates(context)
    }

    fun toggleAlarm(context: Context, prayerName: String) {
        _state.update { current ->
            val newAlarms = current.alarms.toMutableMap()
            newAlarms[prayerName] = !(newAlarms[prayerName] ?: true)
            current.copy(alarms = newAlarms)
        }
        if (hasLocationData) {
            AlarmHelper.scheduleNextPrayer(context, lastLat, lastLng, lastOffset, _state.value.alarms)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context) {
        if (!_state.value.isAutoLocation) return

        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }

        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
            locationCallback = null
        }

        _state.update { it.copy(hasLocationPermission = true) }

        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                if (location != null && _state.value.isAutoLocation) {
                    val timeZoneOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (1000.0 * 60.0 * 60.0)
                    lastLat = location.latitude
                    lastLng = location.longitude
                    lastOffset = timeZoneOffset
                    hasLocationData = true

                    val times = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset)
                    AlarmHelper.scheduleNextPrayer(context, lastLat, lastLng, lastOffset, _state.value.alarms)

                    _state.update { it.copy(prayerTimes = times, locationName = "আমার অবস্থান") }
                    updateNextPrayer(times)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 600000).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (!_state.value.isAutoLocation) return
                result.lastLocation?.let { location ->
                    val timeZoneOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (1000.0 * 60.0 * 60.0)
                    lastLat = location.latitude
                    lastLng = location.longitude
                    lastOffset = timeZoneOffset
                    hasLocationData = true

                    val times = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset)
                    AlarmHelper.scheduleNextPrayer(context, lastLat, lastLng, lastOffset, _state.value.alarms)

                    _state.update { it.copy(prayerTimes = times, locationName = "আমার অবস্থান") }
                    updateNextPrayer(times)
                }
            }
        }
        try {
            fusedLocationClient?.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    fun setPermissionDenied() {
        _state.update { it.copy(hasLocationPermission = false, isLoading = false, error = "Permission Required") }
    }

    private fun updateNextPrayer(times: PrayerTimes) {
        val calendar = Calendar.getInstance()
        val currentHourDecimal = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0 + calendar.get(Calendar.SECOND) / 3600.0
        
        val prayerList = if (GlobalLanguage.isEnglish) {
            listOf(
                Triple("Fajr", "Fajr", times.fajrHours),
                Triple("Sunrise", "Sunrise", times.sunriseHours),
                Triple("Dhuhr", "Dhuhr", times.dhuhrHours),
                Triple("Asr", "Asr", times.asrHours),
                Triple("Maghrib", "Maghrib", times.maghribHours),
                Triple("Isha", "Isha", times.ishaHours)
            )
        } else {
            listOf(
                Triple("Fajr", "ফজর", times.fajrHours),
                Triple("Sunrise", "সূর্যোদয়", times.sunriseHours),
                Triple("Dhuhr", "যোহর", times.dhuhrHours),
                Triple("Asr", "আসর", times.asrHours),
                Triple("Maghrib", "মাগরিব", times.maghribHours),
                Triple("Isha", "এশা", times.ishaHours)
            )
        }

        var nextName = ""
        var nextNameBen = ""
        var nextTime = -1.0
        var prevTime = 0.0

        for (i in prayerList.indices) {
            if (prayerList[i].third > currentHourDecimal) {
                nextName = prayerList[i].first
                nextNameBen = prayerList[i].second
                nextTime = prayerList[i].third
                prevTime = if (i > 0) prayerList[i-1].third else {
                    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    val y = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset, yesterday)
                    y.ishaHours - 24.0
                }
                break
            }
        }

        if (nextName.isEmpty()) {
            nextName = "Fajr"
            nextNameBen = if (GlobalLanguage.isEnglish) "Fajr" else "ফজর"
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val t = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset, tomorrow)
            nextTime = t.fajrHours + 24.0
            prevTime = times.ishaHours
        }

        _state.update { it.copy(nextPrayerName = nextName, nextPrayerNameBen = nextNameBen) }
        startCountdownTimer(nextTime, prevTime, times)
    }

    private fun startCountdownTimer(nextPrayerHour: Double, prevPrayerHour: Double, todayTimes: PrayerTimes) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while(true) {
                val cal = Calendar.getInstance()
                val currentHourDec = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE)/60.0 + cal.get(Calendar.SECOND)/3600.0
                
                // Normal Prayer Countdown
                var diff = nextPrayerHour - currentHourDec
                if (diff < 0) diff += 24.0
                
                val totalDuration = nextPrayerHour - prevPrayerHour
                val progress = ( (currentHourDec - prevPrayerHour) / totalDuration ).coerceIn(0.0, 1.0).toFloat()
                
                val h = Math.floor(diff).toInt()
                val m = Math.floor((diff - h)*60).toInt()
                val s = Math.floor(((diff - h)*60 - m)*60).toInt()
                
                val timeStr = String.format("%02d:%02d:%02d", h, m, s).toBengali()
                
                // Sehri / Iftar Countdown
                // Sehri ends at Fajr. Iftar starts at Maghrib.
                val fajr = todayTimes.fajrHours
                val maghrib = todayTimes.maghribHours
                
                var specialLabel = if (GlobalLanguage.isEnglish) "Sehri Remaining" else "সাহরির বাকি"
                var targetHour = fajr
                var startHour = maghrib - 24.0 // yesterday's maghrib as fallback

                if (currentHourDec > fajr && currentHourDec < maghrib) {
                    // It's daytime, count down to Iftar
                    specialLabel = if (GlobalLanguage.isEnglish) "Iftar Remaining" else "ইফতারের বাকি"
                    targetHour = maghrib
                    startHour = fajr
                } else {
                    // It's nighttime, count down to Sehri
                    specialLabel = if (GlobalLanguage.isEnglish) "Sehri Remaining" else "সাহরির বাকি"
                    targetHour = if (currentHourDec > maghrib) fajr + 24.0 else fajr
                    startHour = if (currentHourDec > maghrib) maghrib else maghrib - 24.0
                }

                var specDiff = targetHour - currentHourDec
                val specTotal = targetHour - startHour
                val specProgress = ((currentHourDec - startHour) / specTotal).coerceIn(0.0, 1.0).toFloat()
                
                val sh = Math.floor(specDiff).toInt()
                val sm = Math.floor((specDiff - sh)*60).toInt()
                val ss = Math.floor(((specDiff - sh)*60 - sm)*60).toInt()
                val specTimeStr = String.format("%02d:%02d:%02d", sh, sm, ss).toBengali()

                _state.update { it.copy(
                    nextPrayerRemaining = timeStr,
                    timerProgress = progress,
                    specialCountdownLabel = specialLabel,
                    specialCountdownTime = specTimeStr,
                    specialCountdownProgress = specProgress
                ) }
                
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
        timerJob?.cancel()
    }
}
