package com.mplayerx.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class PlaybackHistory(
    @PrimaryKey val uri: String,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long = System.currentTimeMillis(),
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun recent(limit: Int = 20): Flow<List<PlaybackHistory>>

    @Query("SELECT * FROM history WHERE uri = :uri LIMIT 1")
    suspend fun byUri(uri: String): PlaybackHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlaybackHistory)

    @Query("DELETE FROM history WHERE uri = :uri")
    suspend fun delete(uri: String)
}
