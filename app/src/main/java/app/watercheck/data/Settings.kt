package app.watercheck.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.watercheck.Hydration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    val weightKg: Int = 70,
    val activityMin: Int = 0,
    val hotWeather: Boolean = false,
    /** 0 = compute the goal from weight and activity. */
    val customGoalMl: Int = 0,
    val reminders: Boolean = true,
    val intervalMin: Int = 60,
    val wakeMin: Int = 7 * 60,
    val sleepMin: Int = 22 * 60,
    val cupMl: Int = 250,
    val drinkType: String = DrinkType.WATER.id,
) {
    val goalMl: Int
        get() = if (customGoalMl > 0) customGoalMl
        else Hydration.dailyGoalMl(weightKg, activityMin, hotWeather)
}

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private object K {
        val weight = intPreferencesKey("weight")
        val activity = intPreferencesKey("activity")
        val hot = booleanPreferencesKey("hot")
        val customGoal = intPreferencesKey("custom_goal")
        val reminders = booleanPreferencesKey("reminders")
        val interval = intPreferencesKey("interval")
        val wake = intPreferencesKey("wake")
        val sleep = intPreferencesKey("sleep")
        val cup = intPreferencesKey("cup")
        val drink = stringPreferencesKey("drink")
    }

    val flow: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(change: (Settings) -> Settings) {
        context.dataStore.edit { p ->
            val s = change(p.toSettings())
            p[K.weight] = s.weightKg
            p[K.activity] = s.activityMin
            p[K.hot] = s.hotWeather
            p[K.customGoal] = s.customGoalMl
            p[K.reminders] = s.reminders
            p[K.interval] = s.intervalMin
            p[K.wake] = s.wakeMin
            p[K.sleep] = s.sleepMin
            p[K.cup] = s.cupMl
            p[K.drink] = s.drinkType
        }
    }

    private fun Preferences.toSettings(): Settings {
        val d = Settings()
        return Settings(
            weightKg = this[K.weight] ?: d.weightKg,
            activityMin = this[K.activity] ?: d.activityMin,
            hotWeather = this[K.hot] ?: d.hotWeather,
            customGoalMl = this[K.customGoal] ?: d.customGoalMl,
            reminders = this[K.reminders] ?: d.reminders,
            intervalMin = this[K.interval] ?: d.intervalMin,
            wakeMin = this[K.wake] ?: d.wakeMin,
            sleepMin = this[K.sleep] ?: d.sleepMin,
            cupMl = this[K.cup] ?: d.cupMl,
            drinkType = this[K.drink] ?: d.drinkType,
        )
    }
}
