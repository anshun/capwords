package com.capwords.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert
    suspend fun insert(word: WordEntity): Long

    @Query("SELECT * FROM words ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM words")
    fun observeCount(): Flow<Int>

    @Delete
    suspend fun delete(word: WordEntity)
}
