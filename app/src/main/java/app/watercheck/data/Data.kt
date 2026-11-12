package app.watercheck.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

enum class DrinkType(val id: String, val label: String, val emoji: String, val factor: Double) {
    WATER("water", "Voda", "💧", 1.0),
    TEA("tea", "Čaj", "🍵", 0.9),
    COFFEE("coffee", "Káva", "☕", 0.8),
    MILK("milk", "Mléko", "🥛", 0.9),
    JUICE("juice", "Džus", "🧃", 0.85);

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: WATER
    }
}

@Entity(tableName = "drinks")
data class Drink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val amountMl: Int,
    /** amountMl × hydration factor of the drink type. */
    val effectiveMl: Int,
    val type: String,
)

@Dao
interface DrinkDao {
    @Insert
    suspend fun insert(drink: Drink)

    @Query("DELETE FROM drinks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM drinks WHERE timestamp >= :from ORDER BY timestamp DESC")
    fun since(from: Long): Flow<List<Drink>>

    @Query("SELECT COALESCE(SUM(effectiveMl), 0) FROM drinks WHERE timestamp >= :from")
    suspend fun totalSince(from: Long): Int
}

@Database(entities = [Drink::class], version = 1, exportSchema = false)
abstract class WaterDb : RoomDatabase() {
    abstract fun drinks(): DrinkDao

    companion object {
        fun create(context: Context): WaterDb =
            Room.databaseBuilder(context, WaterDb::class.java, "water.db").build()
    }
}
