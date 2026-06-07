package com.immichframe.app

import java.util.Calendar

object SleepSchedule {

    fun parseMinutes(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun nowMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun isAsleep(now: Int, off: Int, on: Int): Boolean {
        if (off == on) return false
        return if (off < on) now in off until on else now >= off || now < on
    }

    fun isAsleepNow(settings: ImmichSettings): Boolean {
        if (!settings.sleepEnabled) return false
        val off = parseMinutes(settings.sleepOffTime) ?: return false
        val on = parseMinutes(settings.sleepOnTime) ?: return false
        return isAsleep(nowMinutes(), off, on)
    }

    fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }
}
