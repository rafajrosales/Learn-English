package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_cards")
data class WordCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val translation: String,
    val pronunciationGuide: String, // Phonetic representation like /heˈləʊ/
    val levelName: String,         // "Beginner", "Intermediate", "Advanced"
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val isCustom: Boolean = false,
    val isLearned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
