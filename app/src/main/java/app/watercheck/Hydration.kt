package app.watercheck

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt

/** Pure calculations: daily goal, streak and reminder timing. */
object Hydration {

    /** weight × 33 ml + 12 ml per minute of exercise + 500 ml on hot days, rounded to 50 ml. */
    fun dailyGoalMl(weightKg: Int, activityMin: Int, hotWeather: Boolean): Int {
        val raw = weightKg * 33 + activityMin * 12 + if (hotWeather) 500 else 0
        return ((raw / 50.0).roundToInt() * 50).coerceIn(1000, 6000)
    }

    /**
     * Number of consecutive days with the goal met. Today counts when already met,
     * but an unfinished today does not break the streak.
     */
    fun streak(totals: Map<LocalDate, Int>, goalMl: Int, today: LocalDate): Int {
        var day = if ((totals[today] ?: 0) >= goalMl) today else today.minusDays(1)
        var count = 0
        while ((totals[day] ?: 0) >= goalMl) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** True when [minuteOfDay] lies in the awake window; the window may cross midnight. */
    fun isAwake(minuteOfDay: Int, wakeMin: Int, sleepMin: Int): Boolean =
        if (wakeMin <= sleepMin) minuteOfDay in wakeMin until sleepMin
        else minuteOfDay >= wakeMin || minuteOfDay < sleepMin

    /** Next reminder: one interval from now, or the next wake-up when that falls into sleep. */
    fun nextReminder(now: LocalDateTime, intervalMin: Int, wakeMin: Int, sleepMin: Int): LocalDateTime {
        val candidate = now.plusMinutes(intervalMin.toLong())
        val minute = candidate.hour * 60 + candidate.minute
        if (isAwake(minute, wakeMin, sleepMin)) return candidate
        val wake = LocalTime.of(wakeMin / 60, wakeMin % 60)
        val sameDay = candidate.toLocalDate().atTime(wake)
        return if (sameDay.isAfter(candidate)) sameDay else sameDay.plusDays(1)
    }
}
