package com.reps.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.reps.app.core.data.entity.AIChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIChatMessageDao {
    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    fun getAll(): Flow<List<AIChatMessageEntity>>

    @Insert
    suspend fun insert(entity: AIChatMessageEntity): Long

    @Query("DELETE FROM ai_chat_messages")
    suspend fun clearAll()

    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<AIChatMessageEntity>
}
