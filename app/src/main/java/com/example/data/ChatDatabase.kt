package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String, // Storing plain or simple hash for local dev is fine
    val isCreator: Boolean = false
)

@Entity(
    tableName = "chat_sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val userId: Long,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Long,
    val text: String,
    val isUser: Boolean,
    val hasImage: Boolean = false,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("UPDATE users SET isCreator = 1 WHERE id = :userId")
    suspend fun setCreatorFlag(userId: Long)

    @Query("SELECT * FROM chat_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllSessionsForUser(userId: Long): Flow<List<ChatSessionEntity>>

    @Insert
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Query("DELETE FROM chat_sessions WHERE userId = :userId")
    suspend fun deleteAllSessionsForUser(userId: Long)

    @Query("DELETE FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: Long)
    
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity)
    
    @Query("UPDATE chat_sessions SET title = :newTitle WHERE sessionId = :sessionId")
    suspend fun updateSessionTitle(sessionId: Long, newTitle: String)
}

@Database(entities = [UserEntity::class, MessageEntity::class, ChatSessionEntity::class], version = 4, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
