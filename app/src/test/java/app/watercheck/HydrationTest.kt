package app.watercheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HydrationTest {
    @Test
    fun goalFromWeightActivityAndHeat() {
        assertEquals(2300, Hydration.dailyGoalMl(70, 0, false))   // 2310 → 2300
        assertEquals(2650, Hydration.dailyGoalMl(70, 30, false))  // 2670 → 2650
        assertEquals(2800, Hydration.dailyGoalMl(70, 0, true))    // 2810 → 2800
        assertEquals(1000, Hydration.dailyGoalMl(10, 0, false))   // clamped
    }

    @Test
    fun streakIgnoresUnfinishedToday() {
        val today = LocalDate.of(2026, 11, 12)
        val totals = mapOf(
            today to 500,
            today.minusDays(1) to 2300,
            today.minusDays(2) to 2500,
            today.minusDays(3) to 100,
            today.minusDays(4) to 3000,
        )
        assertEquals(2, Hydration.streak(totals, 2300, today))
        assertEquals(3, Hydration.streak(totals + (today to 2300), 2300, today))
        assertEquals(0, Hydration.streak(emptyMap(), 2300, today))
    }

    @Test
    fun awakeWindow() {
        assertTrue(Hydration.isAwake(12 * 60, 7 * 60, 22 * 60))
        assertFalse(Hydration.isAwake(23 * 60, 7 * 60, 22 * 60))
        // night owl: awake 10:00 → 02:00
        assertTrue(Hydration.isAwake(60, 10 * 60, 2 * 60))
        assertFalse(Hydration.isAwake(5 * 60, 10 * 60, 2 * 60))
    }

    @Test
    fun nextReminderSkipsSleep() {
        val noon = LocalDateTime.of(2026, 11, 12, 12, 0)
        assertEquals(noon.plusHours(1), Hydration.nextReminder(noon, 60, 7 * 60, 22 * 60))

        val late = LocalDateTime.of(2026, 11, 12, 21, 30)
        assertEquals(
            LocalDateTime.of(2026, 11, 13, 7, 0),
            Hydration.nextReminder(late, 60, 7 * 60, 22 * 60),
        )

        val early = LocalDateTime.of(2026, 11, 12, 4, 0)
        assertEquals(
            LocalDateTime.of(2026, 11, 12, 7, 0),
            Hydration.nextReminder(early, 60, 7 * 60, 22 * 60),
        )
    }
}
