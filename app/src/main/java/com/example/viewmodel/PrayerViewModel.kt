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
    val forbiddenSunrise: String = "০০:০০",
    val forbiddenSunriseEnd: String = "০০:০০",
    val forbiddenNoon: String = "০০:০০",
    val forbiddenNoonEnd: String = "০০:০০",
    val forbiddenSunset: String = "০০:০০",
    val forbiddenSunsetEnd: String = "০০:০০",
    val currentPrayerName: String = "",
    val currentPrayerNameBen: String = "",
    val rotatingNames: List<String> = emptyList(),
    val madhab: Int = 2, // 1 for Shafi, 2 for Hanafi
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
    private var lastMadhab = 2
    private var hasLocationData = true

    init {
        // We will receive context later to load from prefs if needed, 
        // initially use defaults
        refreshState()
    }
    
    fun setMadhab(context: Context, m: Int) {
        lastMadhab = m
        context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE).edit().putInt("madhab", m).apply()
        _state.update { it.copy(madhab = m) }
        refreshState()
    }
    
    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE)
        lastMadhab = prefs.getInt("madhab", 2)
        _state.update { it.copy(madhab = lastMadhab) }
        refreshState()
    }

    private fun refreshState() {
        val dateFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.US)
        val defaultTimes = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset, lastMadhab)
        _state.update {
            it.copy(
                isLoading = false,
                currentDate = dateFormat.format(Date()).toBengali(),
                prayerTimes = defaultTimes
            )
        }
        calculateForbiddenTimes(defaultTimes)
        updateNextPrayer(defaultTimes)
    }

    private fun calculateForbiddenTimes(times: com.example.calculator.PrayerTimes) {
        val format = { h: Double ->
            val totalMin = (h * 60).toInt()
            val hour = (totalMin / 60) % 24
            val min = totalMin % 60
            val p = if (hour >= 12) "পিএম" else "এএম"
            val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
            String.format("%02d:%02d %s", displayHour, min, p).toBengali()
        }

        _state.update {
            it.copy(
                forbiddenSunrise = format(times.sunriseHours),
                forbiddenSunriseEnd = format(times.sunriseHours + 15.0 / 60.0),
                forbiddenNoon = format(times.dhuhrHours - 15.0 / 60.0),
                forbiddenNoonEnd = format(times.dhuhrHours),
                forbiddenSunset = format(times.maghribHours - 15.0 / 60.0),
                forbiddenSunsetEnd = format(times.maghribHours)
            )
        }
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

        val fineLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocationPermission != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            coarseLocationPermission != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            _state.update { it.copy(hasLocationPermission = false, isLoading = false, error = "Permission Required") }
            return
        }

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

                    val times = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset, lastMadhab)
                    calculateForbiddenTimes(times)
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
                    calculateForbiddenTimes(times)
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
        
        val sunriseHours = times.sunriseHours
        val dhuhrHours = times.dhuhrHours
        val asrHours = times.asrHours
        val maghribHours = times.maghribHours
        val ishaHours = times.ishaHours
        val fajrHours = times.fajrHours

        // Check if we are in the Nafl/Chasht period (Sunrise to Dhuhr)
        val isNaflPeriod = currentHourDecimal >= sunriseHours && currentHourDecimal < dhuhrHours

        var currentName = ""
        var currentNameBen = ""
        var currentStartTime = 0.0
        var currentEndTime = 0.0

        var nextName = ""
        var nextNameBen = ""

        if (isNaflPeriod) {
            // It's Nafl period! Between Sunrise and Dhuhr.
            currentName = "Duha" // triggers rotating names in center of circular timer
            currentNameBen = if (GlobalLanguage.isEnglish) "Chasht" else "চাশত"
            currentStartTime = sunriseHours
            currentEndTime = dhuhrHours

            nextName = "Dhuhr"
            nextNameBen = if (GlobalLanguage.isEnglish) "Dhuhr" else "যোহর"
        } else {
            // Find current and next among the 5 daily prayers
            // Fajr period: starts at fajrHours, ends at sunriseHours
            if (currentHourDecimal >= fajrHours && currentHourDecimal < sunriseHours) {
                currentName = "Fajr"
                currentNameBen = if (GlobalLanguage.isEnglish) "Fajr" else "ফজর"
                currentStartTime = fajrHours
                currentEndTime = sunriseHours

                nextName = "Dhuhr"
                nextNameBen = if (GlobalLanguage.isEnglish) "Dhuhr" else "যোহর"
            } 
            // Dhuhr period: dhuhrHours to asrHours
            else if (currentHourDecimal >= dhuhrHours && currentHourDecimal < asrHours) {
                currentName = "Dhuhr"
                currentNameBen = if (GlobalLanguage.isEnglish) "Dhuhr" else "যোহর"
                currentStartTime = dhuhrHours
                currentEndTime = asrHours

                nextName = "Asr"
                nextNameBen = if (GlobalLanguage.isEnglish) "Asr" else "আসর"
            }
            // Asr period: asrHours to maghribHours
            else if (currentHourDecimal >= asrHours && currentHourDecimal < maghribHours) {
                currentName = "Asr"
                currentNameBen = if (GlobalLanguage.isEnglish) "Asr" else "আসর"
                currentStartTime = asrHours
                currentEndTime = maghribHours

                nextName = "Maghrib"
                nextNameBen = if (GlobalLanguage.isEnglish) "Maghrib" else "মাগরিব"
            }
            // Maghrib period: maghribHours to ishaHours
            else if (currentHourDecimal >= maghribHours && currentHourDecimal < ishaHours) {
                currentName = "Maghrib"
                currentNameBen = if (GlobalLanguage.isEnglish) "Maghrib" else "মাগরিব"
                currentStartTime = maghribHours
                currentEndTime = ishaHours

                nextName = "Isha"
                nextNameBen = if (GlobalLanguage.isEnglish) "Isha" else "এশা"
            }
            // Isha period: ishaHours to next Fajr (with midnight rollover check)
            else {
                currentName = "Isha"
                currentNameBen = if (GlobalLanguage.isEnglish) "Isha" else "এশা"
                
                if (currentHourDecimal >= ishaHours) {
                    currentStartTime = ishaHours
                    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    val tomorrowTimes = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset, lastMadhab, tomorrow)
                    currentEndTime = tomorrowTimes.fajrHours + 24.0
                } else {
                    // past midnight but before Fajr
                    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    val yesterdayTimes = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset, lastMadhab, yesterday)
                    currentStartTime = yesterdayTimes.ishaHours - 24.0
                    currentEndTime = fajrHours
                }

                nextName = "Fajr"
                nextNameBen = if (GlobalLanguage.isEnglish) "Fajr" else "ফজর"
            }
        }

        _state.update { 
            it.copy(
                currentPrayerName = currentName, 
                currentPrayerNameBen = currentNameBen,
                nextPrayerName = nextName,
                nextPrayerNameBen = nextNameBen
            ) 
        }
        
        // Handle rotating names for Nafl period (Between Sunrise and Dhuhr)
        val rotating = if (isNaflPeriod) {
            if (GlobalLanguage.isEnglish) listOf("Ishraq", "Chasht", "Duha") 
            else listOf("ইশরাক", "চাশত", "দোহা")
        } else {
            emptyList()
        }
        _state.update { it.copy(rotatingNames = rotating) }

        startCountdownTimer(currentEndTime, currentStartTime, times)
    }

    private fun startCountdownTimer(targetHour: Double, startHour: Double, todayTimes: PrayerTimes) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while(true) {
                val cal = Calendar.getInstance()
                val currentHourDec = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE)/60.0 + cal.get(Calendar.SECOND)/3600.0
                
                // Current Prayer Countdown (How much time left for current prayer to end)
                var diff = targetHour - currentHourDec
                if (diff < 0) diff += 24.0
                
                val totalDuration = targetHour - startHour
                val progress = ((currentHourDec - startHour) / totalDuration).coerceIn(0.0, 1.0).toFloat()
                
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
