package com.example.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "daily_tracker")
data class DailyTracker(
    @PrimaryKey val date: String, // format "yyyy-MM-dd"
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    val quran: Boolean = false,
    val charity: Boolean = false,
    val tasbihCount: Int = 0,
    val reading: Boolean = false,
    val istighfar: Boolean = false,
    val parents: Boolean = false
)

@Dao
interface TrackerDao {
    @Query("SELECT * FROM daily_tracker WHERE date = :date")
    suspend fun getTrackerForDate(date: String): DailyTracker?

    @Query("SELECT * FROM daily_tracker WHERE date = :date")
    fun getTrackerFlowForDate(date: String): Flow<DailyTracker?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(tracker: DailyTracker)

    @Query("SELECT * FROM daily_tracker ORDER BY date DESC LIMIT 30")
    fun getAllHistory(): Flow<List<DailyTracker>>
}

@Database(entities = [DailyTracker::class], version = 1, exportSchema = false)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    companion object {
        @Volatile
        private var INSTANCE: TrackerDatabase? = null

        fun getDatabase(context: Context): TrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackerDatabase::class.java,
                    "tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
