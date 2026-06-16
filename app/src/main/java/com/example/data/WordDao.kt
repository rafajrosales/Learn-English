package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM word_cards ORDER BY timestamp DESC")
    fun getAllWords(): Flow<List<WordCard>>

    @Query("SELECT * FROM word_cards WHERE levelName = :levelName ORDER BY timestamp DESC")
    fun getWordsByLevel(levelName: String): Flow<List<WordCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordCard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordCard>)

    @Update
    suspend fun updateWord(word: WordCard)

    @Query("DELETE FROM word_cards WHERE id = :id")
    suspend fun deleteWordById(id: Int)

    @Query("SELECT COUNT(*) FROM word_cards")
    suspend fun getWordCount(): Int
}
