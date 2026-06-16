package com.example.data

import kotlinx.coroutines.flow.Flow

class WordRepository(private val wordDao: WordDao) {
    val allWords: Flow<List<WordCard>> = wordDao.getAllWords()

    fun getWordsByLevel(levelName: String): Flow<List<WordCard>> {
        return wordDao.getWordsByLevel(levelName)
    }

    suspend fun insertWord(word: WordCard) {
        wordDao.insertWord(word)
    }

    suspend fun insertWords(words: List<WordCard>) {
        wordDao.insertWords(words)
    }

    suspend fun updateWord(word: WordCard) {
        wordDao.updateWord(word)
    }

    suspend fun deleteWordById(id: Int) {
        wordDao.deleteWordById(id)
    }

    suspend fun getWordCount(): Int {
        return wordDao.getWordCount()
    }
}
