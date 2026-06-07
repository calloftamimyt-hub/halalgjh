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

@Entity(tableName = "saved_posts")
data class SavedPost(
    @PrimaryKey val docId: String,
    val author: String,
    val description: String,
    val category: String,
    val status: String,
    val userId: String,
    val telegramFileId: String,
    val viewsCount: Long,
    val sharesCount: Long,
    val title: String,
    val videoUri: String,
    val url: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val type: String, // "LIKE", "SHARE", "VIDEO", "REACTION", "COMMENT", "GENERAL", "FOLLOW", "UNFOLLOW"
    val actorName: String,
    val actorAvatar: String = "",
    val itemType: String = "", // "reel", "photo", "post" etc.
    val itemTitle: String = "", // text of reel/photo/post
    val remoteId: String = "" // Firestore doc id
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

@Dao
interface SavedPostDao {
    @Query("SELECT * FROM saved_posts")
    fun getAllSavedPosts(): Flow<List<SavedPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePost(post: SavedPost)

    @Delete
    suspend fun deletePost(post: SavedPost)
    
    @Query("SELECT * FROM saved_posts WHERE docId = :docId")
    suspend fun getPostById(docId: String): SavedPost?
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Update
    suspend fun updateNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Int)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    @Query("SELECT COUNT(*) FROM notifications WHERE remoteId = :remoteId")
    suspend fun countByRemoteId(remoteId: String): Int
}

@Entity(tableName = "user_alarms")
data class UserAlarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val amPm: String, // "AM" or "PM"
    val days: String, // comma separated days e.g. "Mon,Tue" or "Once"
    val deleteAfterRinging: Boolean = false,
    val sound: String = "Default",
    val ringtoneUri: String = "",
    val label: String = "",
    val snooze: String = "10 min, 3 times",
    val vibrate: Boolean = true,
    val isEnabled: Boolean = true
)

@Dao
interface UserAlarmDao {
    @Query("SELECT * FROM user_alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<UserAlarm>>

    @Query("SELECT * FROM user_alarms")
    suspend fun getAllAlarmsDirect(): List<UserAlarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: UserAlarm): Long

    @Update
    suspend fun updateAlarm(alarm: UserAlarm)

    @Delete
    suspend fun deleteAlarm(alarm: UserAlarm)

    @Query("DELETE FROM user_alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Int)
}

@Database(entities = [DailyTracker::class, SavedPost::class, NotificationEntity::class, UserAlarm::class], version = 4, exportSchema = false)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao
    abstract fun savedPostDao(): SavedPostDao
    abstract fun notificationDao(): NotificationDao
    abstract fun alarmDao(): UserAlarmDao

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
