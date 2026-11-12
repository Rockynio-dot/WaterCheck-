package app.watercheck

import android.app.Application
import app.watercheck.data.Drink
import app.watercheck.data.DrinkType
import app.watercheck.data.Settings
import app.watercheck.data.SettingsStore
import app.watercheck.data.WaterDb
import app.watercheck.reminders.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class WaterApp : Application() {
    val db by lazy { WaterDb.create(this) }
    val settings by lazy { SettingsStore(this) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Reminders.createChannel(this)
        scope.launch { Reminders.schedule(this@WaterApp, settings.flow.first()) }
    }

    /** Logs a drink and restarts the reminder countdown from now. */
    suspend fun addDrink(amountMl: Int, type: DrinkType) {
        db.drinks().insert(
            Drink(
                timestamp = System.currentTimeMillis(),
                amountMl = amountMl,
                effectiveMl = (amountMl * type.factor).roundToInt(),
                type = type.id,
            )
        )
        Reminders.cancelNotification(this)
        Reminders.schedule(this, settings.flow.first())
    }

    suspend fun updateSettings(change: (Settings) -> Settings) {
        settings.update(change)
        Reminders.schedule(this, settings.flow.first())
    }

    suspend fun todayTotalMl(): Int = db.drinks().totalSince(startOfDay(LocalDate.now()))

    companion object {
        fun startOfDay(date: LocalDate): Long =
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
