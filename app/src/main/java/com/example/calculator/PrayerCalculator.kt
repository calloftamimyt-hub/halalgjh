package com.example.calculator

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

data class PrayerTimes(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    
    // In hours from midnight
    val fajrHours: Double,
    val sunriseHours: Double,
    val dhuhrHours: Double,
    val asrHours: Double,
    val maghribHours: Double,
    val ishaHours: Double
)

class PrayerCalculator {

    companion object {
        private fun dsin(d: Double) = sin(Math.toRadians(d))
        private fun dcos(d: Double) = cos(Math.toRadians(d))
        private fun dtan(d: Double) = tan(Math.toRadians(d))
        private fun darcsin(x: Double) = Math.toDegrees(asin(x))
        private fun darccos(x: Double) = Math.toDegrees(acos(x))
        private fun darctan(x: Double) = Math.toDegrees(atan(x))
        private fun darccot(x: Double) = Math.toDegrees(atan(1.0 / x))

        private fun fixAngle(a: Double): Double {
            var aFixed = a - 360.0 * floor(a / 360.0)
            aFixed = if (aFixed < 0) aFixed + 360.0 else aFixed
            return aFixed
        }

        private fun fixHour(a: Double): Double {
            var aFixed = a - 24.0 * floor(a / 24.0)
            aFixed = if (aFixed < 0) aFixed + 24.0 else aFixed
            return aFixed
        }

        fun calculatePrayerTimes(lat: Double, lng: Double, timeZone: Double, madhab: Int = 1, calendar: Calendar = Calendar.getInstance()): PrayerTimes {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val J = julianDate(year, month, day) - lng / (15.0 * 24.0)

            return calculateTimes(J, lat, lng, timeZone, madhab)
        }

        private fun julianDate(year: Int, month: Int, day: Int): Double {
            var y = year
            var m = month
            if (m <= 2) {
                y -= 1
                m += 12
            }
            val A = floor(y / 100.0)
            val B = 2 - A + floor(A / 4.0)
            return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + B - 1524.5
        }

        private fun equationOfTime(J: Double): Double {
            val d = J - 2451545.0
            val g = fixAngle(357.529 + 0.98560028 * d)
            val q = fixAngle(280.459 + 0.98564736 * d)
            val L = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
            val e = 23.439 - 0.00000036 * d
            
            // Use atan2 to preserve quadrant information
            var RA = Math.toDegrees(kotlin.math.atan2(dcos(e) * dsin(L), dcos(L))) / 15.0
            val RA_fix = fixHour(RA)
            val q_fix = fixHour(q / 15.0)
            var eqT = q_fix - RA_fix
            if (eqT > 12.0) { eqT -= 24.0 }
            if (eqT < -12.0) { eqT += 24.0 }
            return eqT
        }

        private fun sunDeclination(J: Double): Double {
            val d = J - 2451545.0
            val g = fixAngle(357.529 + 0.98560028 * d)
            val q = fixAngle(280.459 + 0.98564736 * d)
            val L = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
            val e = 23.439 - 0.00000036 * d
            return darcsin(dsin(e) * dsin(L))
        }

        private fun timeDiff(angle: Double, declination: Double, lat: Double): Double {
            val term1 = -dsin(angle) - dsin(declination) * dsin(lat)
            val term2 = dcos(declination) * dcos(lat)
            val x = term1 / term2
            if (x > 1.0 || x < -1.0) return 0.0
            return darccos(x) / 15.0
        }

        private fun calculateTimes(J: Double, lat: Double, lng: Double, timeZone: Double, madhab: Int): PrayerTimes {
            val declination = sunDeclination(J)
            val eqT = equationOfTime(J)

            // Dhuhr
            val dhuhr = 12.0 + timeZone - lng / 15.0 - eqT

            // Angles
            val fajrAngle = 18.0
            val ishaAngle = 18.0 // using standard 18 degrees, adapt if needed
            val maghribAngle = 0.833 // consider refraction and sun disc
            val sunriseAngle = 0.833

            val asrHeight = darccot(mathAsrCalc(lat, declination, madhab))
            val tFajr = timeDiff(fajrAngle, declination, lat)
            val tSunrise = timeDiff(sunriseAngle, declination, lat)
            val tAsr = timeDiff(-asrHeight, declination, lat)
            val tMaghrib = timeDiff(maghribAngle, declination, lat)
            val tIsha = timeDiff(ishaAngle, declination, lat)

            var fajr = dhuhr - tFajr
            var sunrise = dhuhr - tSunrise
            var asr = dhuhr + tAsr
            var maghrib = dhuhr + tMaghrib
            var isha = dhuhr + tIsha

            // Some safety max/mins if calculation fails due to extreme latitudes
            if (tFajr == 0.0) fajr = sunrise - 1.5
            if (tIsha == 0.0) isha = maghrib + 1.5

            return PrayerTimes(
                formatTime(fajr),
                formatTime(sunrise),
                formatTime(dhuhr),
                formatTime(asr),
                formatTime(maghrib),
                formatTime(isha),
                fajr, sunrise, dhuhr, asr, maghrib, isha
            )
        }

        private fun mathAsrCalc(lat: Double, declination: Double, madhab: Int): Double {
            val factor = if (madhab == 2) 2.0 else 1.0
            return factor + dtan(Math.abs(lat - declination))
        }

        private fun formatTime(timeVal: Double): String {
            if (timeVal.isNaN()) return "--:--"
            val time = fixHour(timeVal)
            val hours = floor(time).toInt()
            val minutes = floor((time - hours) * 60.0).toInt()
            val p = if (hours >= 12) "PM" else "AM"
            val h = if (hours > 12) hours - 12 else if (hours == 0) 12 else hours
            return String.format("%02d:%02d %s", h, minutes, p)
        }
        
    }
}
