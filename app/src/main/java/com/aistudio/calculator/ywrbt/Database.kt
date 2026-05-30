package com.aistudio.calculator.ywrbt

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_profiles")
data class ChatProfile(
    @PrimaryKey val username: String, // e.g., "maluram_official"
    val fullName: String,
    val bio: String,
    val avatarColorHex: String, // Primary color for visual styling
    val avatarUrl: String? = null, // Profile Picture URL
    val isCreatedByLocalUser: Boolean = false, // If created as local switcher or manually searched
    val googleEmail: String = "", // Google Email linking the account to protect the ID from impersonation
    val lastActive: Long = 0L,
    val isPrivate: Boolean = false
)

@Entity(tableName = "user_stories")
data class UserStory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val username: String,
    val fullName: String,
    val text: String,
    val avatarColorHex: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sender: String, // sender username
    val recipient: String, // recipient username
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false,
    val imageUri: String? = null,
    val isViewOnce: Boolean = false,
    val isSaved: Boolean = false,
    val audioBase64: String? = null,
    val audioDurationMs: Int = 0
)

@Dao
interface SecretDao {
    @Query("SELECT * FROM chat_profiles ORDER BY username ASC")
    fun getAllProfiles(): Flow<List<ChatProfile>>

    @Query("SELECT * FROM chat_profiles WHERE username = :username LIMIT 1")
    suspend fun getProfileByUsername(username: String): ChatProfile?

    @Query("SELECT * FROM chat_profiles WHERE username LIKE :query OR fullName LIKE :query")
    fun searchProfiles(query: String): Flow<List<ChatProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ChatProfile)

    @Delete
    suspend fun deleteProfile(profile: ChatProfile)

    @Query("SELECT * FROM user_stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<UserStory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: UserStory)

    @Query("DELETE FROM user_stories WHERE id = :id")
    suspend fun deleteStory(id: Long)

    @Query("DELETE FROM user_stories WHERE timestamp < :cutoff")
    suspend fun deleteExpiredStories(cutoff: Long)

    @Query("SELECT * FROM chat_messages WHERE (sender = :user1 AND recipient = :user2) OR (sender = :user2 AND recipient = :user1) ORDER BY timestamp ASC")
    fun getConversation(user1: String, user2: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE (sender = :user1 AND recipient = :user2) OR (sender = :user2 AND recipient = :user1)")
    suspend fun clearConversation(user1: String, user2: String)

    @Query("DELETE FROM chat_messages WHERE sender = :sender AND recipient = :recipient AND timestamp = :timestamp")
    suspend fun deleteMessageByUnique(sender: String, recipient: String, timestamp: Long)

    @Query("UPDATE chat_messages SET isSaved = :isSaved WHERE sender = :sender AND recipient = :recipient AND timestamp = :timestamp")
    suspend fun updateMessageSavedState(sender: String, recipient: String, timestamp: Long, isSaved: Boolean)

    @Query("DELETE FROM chat_messages WHERE timestamp < :cutoff AND isSaved = 0")
    suspend fun deleteExpiredMessages(cutoff: Long)
}

@Database(
    entities = [ChatProfile::class, ChatMessage::class, UserStory::class],
    version = 11, // Destructive migration will rebuild schema seamlessly
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun secretDao(): SecretDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calculator_vault_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class VaultRepository(private val dao: SecretDao) {
    val allProfiles: Flow<List<ChatProfile>> = dao.getAllProfiles()
    val allMessages: Flow<List<ChatMessage>> = dao.getAllMessages()
    val allStories: Flow<List<UserStory>> = dao.getAllStories()

    fun getConversation(user1: String, user2: String): Flow<List<ChatMessage>> =
        dao.getConversation(user1, user2)

    fun searchProfiles(query: String): Flow<List<ChatProfile>> =
        dao.searchProfiles("%$query%")

    suspend fun getProfile(username: String): ChatProfile? =
        dao.getProfileByUsername(username)

    suspend fun insertProfile(profile: ChatProfile) = dao.insertProfile(profile)
    suspend fun deleteProfile(profile: ChatProfile) = dao.deleteProfile(profile)

    suspend fun insertStory(story: UserStory) = dao.insertStory(story)
    suspend fun deleteStory(id: Long) = dao.deleteStory(id)
    suspend fun deleteExpiredStories(cutoff: Long) = dao.deleteExpiredStories(cutoff)

    suspend fun insertMessage(message: ChatMessage) = dao.insertMessage(message)
    suspend fun clearConversation(user1: String, user2: String) = dao.clearConversation(user1, user2)
    suspend fun deleteMessageByUnique(sender: String, recipient: String, timestamp: Long) = dao.deleteMessageByUnique(sender, recipient, timestamp)
    suspend fun updateMessageSavedState(sender: String, recipient: String, timestamp: Long, isSaved: Boolean) = dao.updateMessageSavedState(sender, recipient, timestamp, isSaved)
    suspend fun deleteExpiredMessages(cutoff: Long) = dao.deleteExpiredMessages(cutoff)
}

data class UserReel(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarColorHex: String = "#38BDF8",
    val avatarUrl: String? = null,
    val caption: String = "",
    val mediaUrl: String? = null,
    val musicTrack: String = "Popular Beats",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val filter: String = "Normal",
    val timestamp: Long = System.currentTimeMillis(),
    val isLikedByMe: Boolean = false
)

data class ReelComment(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarColorHex: String = "#38BDF8",
    val avatarUrl: String? = null,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

