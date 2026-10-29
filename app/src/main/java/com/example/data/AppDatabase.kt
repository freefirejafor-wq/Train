package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_trains")
data class FavoriteTrainEntity(
    @PrimaryKey val trainNumber: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trainNumber: String,
    val trainNameBn: String,
    val senderName: String,
    val messageBn: String,
    val currentStationTag: String,
    val reportedDelayMin: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "lost_found_posts")
data class LostFoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postType: String, // "LOST" or "FOUND"
    val itemTitleBn: String,
    val trainNameBn: String,
    val stationNameBn: String,
    val coachSerialBn: String,
    val contactPhone: String,
    val detailsBn: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "station_alarms")
data class StationAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trainNumber: String,
    val trainNameBn: String,
    val stationCode: String,
    val stationNameBn: String,
    val alertBeforeKm: Int = 5,
    val isEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "passenger_complaints")
data class ComplaintEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryBn: String,
    val trainOrStationBn: String,
    val coachNumberBn: String,
    val descriptionBn: String,
    val statusBn: String = "তদন্তাধীন (Submitted)",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface RailPulseDao {
    @Query("SELECT * FROM favorite_trains ORDER BY addedTimestamp DESC")
    fun getFavoriteTrains(): Flow<List<FavoriteTrainEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(fav: FavoriteTrainEntity)

    @Query("DELETE FROM favorite_trains WHERE trainNumber = :trainNumber")
    suspend fun removeFavorite(trainNumber: String)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(msg: ChatMessageEntity)

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getChatCount(): Int

    @Query("SELECT * FROM lost_found_posts ORDER BY timestamp DESC")
    fun getAllLostFoundPosts(): Flow<List<LostFoundEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLostFoundPost(post: LostFoundEntity)

    @Query("DELETE FROM lost_found_posts WHERE id = :id")
    suspend fun deleteLostFoundPost(id: Int)

    @Query("SELECT COUNT(*) FROM lost_found_posts")
    suspend fun getLostFoundCount(): Int

    @Query("SELECT * FROM station_alarms ORDER BY timestamp DESC")
    fun getAllAlarms(): Flow<List<StationAlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: StationAlarmEntity)

    @Query("DELETE FROM station_alarms WHERE id = :id")
    suspend fun deleteAlarm(id: Int)

    @Query("SELECT * FROM passenger_complaints ORDER BY timestamp DESC")
    fun getAllComplaints(): Flow<List<ComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity)
}

@Database(
    entities = [
        FavoriteTrainEntity::class,
        ChatMessageEntity::class,
        LostFoundEntity::class,
        StationAlarmEntity::class,
        ComplaintEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun railPulseDao(): RailPulseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "railpulse_bd.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class RailPulseRepository(private val dao: RailPulseDao) {
    val favoriteTrains: Flow<List<FavoriteTrainEntity>> = dao.getFavoriteTrains()
    val chatMessages: Flow<List<ChatMessageEntity>> = dao.getAllChatMessages()
    val lostFoundPosts: Flow<List<LostFoundEntity>> = dao.getAllLostFoundPosts()
    val stationAlarms: Flow<List<StationAlarmEntity>> = dao.getAllAlarms()
    val complaints: Flow<List<ComplaintEntity>> = dao.getAllComplaints()

    suspend fun toggleFavorite(trainNumber: String, isCurrentlyFav: Boolean) {
        if (isCurrentlyFav) {
            dao.removeFavorite(trainNumber)
        } else {
            dao.insertFavorite(FavoriteTrainEntity(trainNumber))
        }
    }

    suspend fun postChatMessage(msg: ChatMessageEntity) {
        dao.insertChatMessage(msg)
    }

    suspend fun postLostFound(post: LostFoundEntity) {
        dao.insertLostFoundPost(post)
    }

    suspend fun deleteLostFound(id: Int) {
        dao.deleteLostFoundPost(id)
    }

    suspend fun addStationAlarm(alarm: StationAlarmEntity) {
        dao.insertAlarm(alarm)
    }

    suspend fun removeStationAlarm(id: Int) {
        dao.deleteAlarm(id)
    }

    suspend fun submitComplaint(complaint: ComplaintEntity) {
        dao.insertComplaint(complaint)
    }

    suspend fun seedInitialCommunityDataIfEmpty() {
        if (dao.getChatCount() == 0) {
            val now = System.currentTimeMillis()
            dao.insertFavorite(FavoriteTrainEntity("701", now - 600_000))
            dao.insertFavorite(FavoriteTrainEntity("813", now - 300_000))

            dao.insertChatMessage(
                ChatMessageEntity(
                    trainNumber = "701",
                    trainNameBn = "সুবর্ণ এক্সপ্রেস (৭০১)",
                    senderName = "তানভীর আহমেদ (বগি-গ)",
                    messageBn = "ভৈরব মেঘনা ব্রিজ পার হচ্ছে এখন! স্পিড ৯৪ কিমি/ঘণ্টা, মাত্র ৪ মিনিট লেট।",
                    currentStationTag = "ভৈরব বাজার জংশন",
                    reportedDelayMin = 4,
                    timestamp = now - 180_000
                )
            )
            dao.insertChatMessage(
                ChatMessageEntity(
                    trainNumber = "813",
                    trainNameBn = "কক্সবাজার এক্সপ্রেস (৮১৩)",
                    senderName = "রাফি চৌধুরী (বগি-ক)",
                    messageBn = "ফেনী জংশন ক্রস করল, এসি কোচের সার্ভিস আজকে অনেক ভালো।",
                    currentStationTag = "ফেনী জংশন",
                    reportedDelayMin = 6,
                    timestamp = now - 420_000
                )
            )
            dao.insertChatMessage(
                ChatMessageEntity(
                    trainNumber = "709",
                    trainNameBn = "পারাবত এক্সপ্রেস (৭০৯)",
                    senderName = "সাদিয়া ইসলাম (বগি-ঙ)",
                    messageBn = "শ্রীমঙ্গল স্টেশনে ৫ মিনিট বিরতি দিচ্ছে। আবহাওয়া চমৎকার!",
                    currentStationTag = "শ্রীমঙ্গল",
                    reportedDelayMin = 10,
                    timestamp = now - 900_000
                )
            )
            dao.insertChatMessage(
                ChatMessageEntity(
                    trainNumber = "791",
                    trainNameBn = "বনলতা এক্সপ্রেস (৭৯১)",
                    senderName = "মাহমুদ হাসান (বগি-ছ)",
                    messageBn = "যমুনা রেল সেতু পার হয়ে টাঙ্গাইল ঢুকছে। একদম অন-টাইম!",
                    currentStationTag = "বঙ্গবন্ধু সেতু পূর্ব",
                    reportedDelayMin = 2,
                    timestamp = now - 1_200_000
                )
            )
        }

        if (dao.getLostFoundCount() == 0) {
            val now = System.currentTimeMillis()
            dao.insertLostFoundPost(
                LostFoundEntity(
                    postType = "LOST",
                    itemTitleBn = "কালো ল্যাপটপ ব্যাগ (Dell Laptop)",
                    trainNameBn = "সুবর্ণ এক্সপ্রেস (৭০১)",
                    stationNameBn = "বিমানবন্দর স্টেশন",
                    coachSerialBn = "বগি - ঘ (সিট ২৪)",
                    contactPhone = "01711-009988",
                    detailsBn = "বিমানবন্দর স্টেশনে নামার সময় উপরের ল্যাগেজ র‍্যাকে কালো রঙের ডেল ল্যাপটপ ব্যাগ ফেলে এসেছি। ভেতরে জরুরি কাগজপত্র আছে।",
                    timestamp = now - 3_600_000
                )
            )
            dao.insertLostFoundPost(
                LostFoundEntity(
                    postType = "FOUND",
                    itemTitleBn = "জাতীয় পরিচয়পত্র ও মানিব্যাগ",
                    trainNameBn = "সোনার বাংলা এক্সপ্রেস (৭৮৭)",
                    stationNameBn = "কমলাপুর স্টেশন",
                    coachSerialBn = "বগি - ছ (সিট ১২)",
                    contactPhone = "01819-223344",
                    detailsBn = "সিটের পাশে একটি বাদামী মানিব্যাগ ও মো: কামরুল হাসানের এনআইডি কার্ড পাওয়া গেছে। উপযুক্ত প্রমাণ দিয়ে সংগ্রহ করুন।",
                    timestamp = now - 7_200_000
                )
            )
            dao.insertLostFoundPost(
                LostFoundEntity(
                    postType = "LOST",
                    itemTitleBn = "নীল রঙের ট্রাভেল ট্রলি ব্যাগ",
                    trainNameBn = "কক্সবাজার এক্সপ্রেস (৮১৩)",
                    stationNameBn = "চট্টগ্রাম স্টেশন",
                    coachSerialBn = "বগি - গ",
                    contactPhone = "01912-556677",
                    detailsBn = "চট্টগ্রাম স্টেশনে বগি গ-এর দরজার কাছ থেকে ভুলবশত অন্য কোনো যাত্রী নীল ট্রলি ব্যাগটি নিয়ে নেমে গেছেন।",
                    timestamp = now - 14_400_000
                )
            )
        }
    }
}
