package app.watercheck.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.watercheck.Hydration
import app.watercheck.WaterApp
import app.watercheck.data.Drink
import app.watercheck.data.DrinkType
import app.watercheck.data.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class UiState(
    val today: LocalDate,
    val settings: Settings,
    val todayDrinks: List<Drink>,
    val todayMl: Int,
    /** Effective millilitres per day for the last year. */
    val totals: Map<LocalDate, Int>,
    val streak: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
class WaterViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WaterApp

    // Re-emits when the date changes so the screen rolls over at midnight.
    private val today = flow {
        while (true) {
            emit(LocalDate.now())
            delay(30_000)
        }
    }.distinctUntilChanged()

    /** Null until the first load finishes. */
    val state: StateFlow<UiState?> = today.flatMapLatest { date ->
        combine(
            app.db.drinks().since(WaterApp.startOfDay(date.minusDays(365))),
            app.settings.flow,
        ) { drinks, settings -> build(date, drinks, settings) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun build(date: LocalDate, drinks: List<Drink>, settings: Settings): UiState {
        val zone = ZoneId.systemDefault()
        val byDay = drinks.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        val totals = byDay.mapValues { (_, list) -> list.sumOf { it.effectiveMl } }
        return UiState(
            today = date,
            settings = settings,
            todayDrinks = byDay[date].orEmpty(),
            todayMl = totals[date] ?: 0,
            totals = totals,
            streak = Hydration.streak(totals, settings.goalMl, date),
        )
    }

    fun addDrink(amountMl: Int, type: DrinkType) {
        viewModelScope.launch { app.addDrink(amountMl, type) }
    }

    fun deleteDrink(id: Long) {
        viewModelScope.launch { app.db.drinks().delete(id) }
    }

    fun update(change: (Settings) -> Settings) {
        viewModelScope.launch { app.updateSettings(change) }
    }
}
