package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GeminiHelper
import com.example.data.WordCard
import com.example.data.WordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class EnglishViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: WordRepository
    private var tts: TextToSpeech? = null

    // UI States
    private val _allWords = MutableStateFlow<List<WordCard>>(emptyList())
    val allWords: StateFlow<List<WordCard>> = _allWords.asStateFlow()

    private val _activeLevel = MutableStateFlow("Beginner")
    val activeLevel: StateFlow<String> = _activeLevel.asStateFlow()

    private val _isGeneratingSuggest = MutableStateFlow(false)
    val isGeneratingSuggest: StateFlow<Boolean> = _isGeneratingSuggest.asStateFlow()

    private val _suggestError = MutableStateFlow<String?>(null)
    val suggestError: StateFlow<String?> = _suggestError.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    // Quiz States
    private val _quizQuestions = MutableStateFlow<List<WordCard>>(emptyList())
    val quizQuestions: StateFlow<List<WordCard>> = _quizQuestions.asStateFlow()

    private val _quizCurrentIndex = MutableStateFlow(0)
    val quizCurrentIndex: StateFlow<Int> = _quizCurrentIndex.asStateFlow()

    private val _quizOptions = MutableStateFlow<List<String>>(emptyList())
    val quizOptions: StateFlow<List<String>> = _quizOptions.asStateFlow()

    private val _quizCorrectAnswer = MutableStateFlow("")
    val quizCorrectAnswer: StateFlow<String> = _quizCorrectAnswer.asStateFlow()

    private val _selectedAnswerIndex = MutableStateFlow<Int?>(null)
    val selectedAnswerIndex: StateFlow<Int?> = _selectedAnswerIndex.asStateFlow()

    private val _quizCorrectCount = MutableStateFlow(0)
    val quizCorrectCount: StateFlow<Int> = _quizCorrectCount.asStateFlow()

    private val _quizScreenState = MutableStateFlow<QuizState>(QuizState.NOT_STARTED)
    val quizScreenState: StateFlow<QuizState> = _quizScreenState.asStateFlow()

    init {
        val wordDao = AppDatabase.getDatabase(application).wordDao
        repository = WordRepository(wordDao)

        // Initialize Text To Speech
        tts = TextToSpeech(application, this)

        // Observe and pre-populate if needed
        viewModelScope.launch {
            repository.allWords.collectLatest { list ->
                if (list.isEmpty()) {
                    prepopulateDatabase()
                } else {
                    _allWords.value = list
                }
            }
        }
    }

    private suspend fun prepopulateDatabase() {
        val preloaded = listOf(
            // Beginner (A1-A2)
            WordCard(word = "Welcome", translation = "Bienvenido", pronunciationGuide = "/ˈwel.kəm/", levelName = "Beginner", exampleSentence = "Welcome to our new home!", exampleTranslation = "¡Bienvenido a nuestro nuevo hogar!", isCustom = false),
            WordCard(word = "Journey", translation = "Viaje", pronunciationGuide = "/ˈdʒɜː.ni/", levelName = "Beginner", exampleSentence = "Life is a beautiful journey.", exampleTranslation = "La vida es un hermoso viaje.", isCustom = false),
            WordCard(word = "Understand", translation = "Entender / Comprender", pronunciationGuide = "/ˌʌn.dəˈstænd/", levelName = "Beginner", exampleSentence = "Do you understand the English lesson?", exampleTranslation = "¿Entiendes la lección de inglés?", isCustom = false),
            WordCard(word = "Knowledge", translation = "Conocimiento", pronunciationGuide = "/ˈnɒl.ɪdʒ/", levelName = "Beginner", exampleSentence = "Reading books gives you deep knowledge.", exampleTranslation = "Leer libros te da un conocimiento profundo.", isCustom = false),
            WordCard(word = "Practice", translation = "Práctica / Practicar", pronunciationGuide = "/ˈpræk.tɪs/", levelName = "Beginner", exampleSentence = "You need to practice English every day.", exampleTranslation = "Necesitas practicar inglés todos los días.", isCustom = false),
            WordCard(word = "Morning", translation = "Mañana (día)", pronunciationGuide = "/ˈmɔː.nɪŋ/", levelName = "Beginner", exampleSentence = "I love the fresh morning breeze.", exampleTranslation = "Me encanta la brisa fresca de la mañana.", isCustom = false),
            WordCard(word = "Family", translation = "Familia", pronunciationGuide = "/ˈfæm.əl.i/", levelName = "Beginner", exampleSentence = "We spend weekends with our family.", exampleTranslation = "Pasamos los fines de semana con nuestra familia.", isCustom = false),

            // Intermediate (B1-B2)
            WordCard(word = "Challenge", translation = "Desafío", pronunciationGuide = "/ˈtʃæl.ɪndʒ/", levelName = "Intermediate", exampleSentence = "Learning a language is an amazing challenge.", exampleTranslation = "Aprender un idioma es un desafío increíble.", isCustom = false),
            WordCard(word = "Improvement", translation = "Mejora", pronunciationGuide = "/ɪmˈpruːv.mənt/", levelName = "Intermediate", exampleSentence = "We notice a big improvement in your pronunciation.", exampleTranslation = "Notamos una gran mejora en tu pronunciación.", isCustom = false),
            WordCard(word = "Success", translation = "Éxito", pronunciationGuide = "/səkˈses/", levelName = "Intermediate", exampleSentence = "Hard work is the key to success.", exampleTranslation = "El trabajo duro es la clave del éxito.", isCustom = false),
            WordCard(word = "Purpose", translation = "Propósito / Objetivo", pronunciationGuide = "/ˈpɜː.pəs/", levelName = "Intermediate", exampleSentence = "What is the purpose of this mini-app?", exampleTranslation = "¿Cuál es el propósito de esta mini-aplicación?", isCustom = false),
            WordCard(word = "Confident", translation = "Seguro / Confiado", pronunciationGuide = "/ˈkɒn.fɪ.dənt/", levelName = "Intermediate", exampleSentence = "Be confident when speaking English.", exampleTranslation = "Ten confianza al hablar inglés.", isCustom = false),
            WordCard(word = "Schedule", translation = "Horario / Programar", pronunciationGuide = "/ˈʃed.juːl/", levelName = "Intermediate", exampleSentence = "Let me check my daily class schedule.", exampleTranslation = "Déjame revisar mi horario diario de clases.", isCustom = false),
            WordCard(word = "Focus", translation = "Enfoque / Enfocarse", pronunciationGuide = "/ˈfəʊ.kəs/", levelName = "Intermediate", exampleSentence = "You should focus on vocabulary first.", exampleTranslation = "Deberías enfocarte primero en el vocabulario.", isCustom = false),

            // Advanced (C1-C2)
            WordCard(word = "Ubiquitous", translation = "Ubicuo / Omnipresente", pronunciationGuide = "/juːˈbɪk.wɪ.təs/", levelName = "Advanced", exampleSentence = "English is ubiquitous in the tech industry.", exampleTranslation = "El inglés es omnipresente en la industria de la tecnología.", isCustom = false),
            WordCard(word = "Resilient", translation = "Resiliente / Resistente", pronunciationGuide = "/rɪˈzɪl.i.ənt/", levelName = "Advanced", exampleSentence = "She is extremely resilient despite many failures.", exampleTranslation = "Ella es extremadamente fuerte ante las dificultades.", isCustom = false),
            WordCard(word = "Eloquent", translation = "Elocuente", pronunciationGuide = "/ˈel.ə.kwənt/", levelName = "Advanced", exampleSentence = "The speaker gave an eloquent speech on education.", exampleTranslation = "El orador dio un discurso elocuente sobre educación.", isCustom = false),
            WordCard(word = "Serendipity", translation = "Serendipia / Casualidad", pronunciationGuide = "/ˌser.ənˈdɪp.ə.ti/", levelName = "Advanced", exampleSentence = "Finding this English app was pure serendipity.", exampleTranslation = "Encontrar esta aplicación de inglés fue pura casualidad.", isCustom = false),
            WordCard(word = "Flourish", translation = "Florecer / Prosperar", pronunciationGuide = "/ˈflʌr.ɪʃ/", levelName = "Advanced", exampleSentence = "Her English skills flourished in just three months.", exampleTranslation = "Sus habilidades en inglés florecieron en solo tres meses.", isCustom = false),
            WordCard(word = "Astounding", translation = "Sorprendente / Asombroso", pronunciationGuide = "/əˈstaʊn.dɪŋ/", levelName = "Advanced", exampleSentence = "Your learning speed is astounding!", exampleTranslation = "¡Tu velocidad de aprendizaje es asombrosa!", isCustom = false),
            WordCard(word = "Superfluous", translation = "Superfluo / Innecesario", pronunciationGuide = "/suːˈpɜː.flu.əs/", levelName = "Advanced", exampleSentence = "Avoid adding superfluous words to your sentences.", exampleTranslation = "Evita añadir palabras superfluas a tus oraciones.", isCustom = false)
        )
        repository.insertWords(preloaded)
    }

    // TTS implementation
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "English language in TTS is not supported or missing data")
                _isTtsReady.value = false
            } else {
                _isTtsReady.value = true
            }
        } else {
            Log.e("TTS", "TTS Initialization failed")
            _isTtsReady.value = false
        }
    }

    fun pronounceWord(word: String) {
        if (_isTtsReady.value) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun changeActiveLevel(level: String) {
        _activeLevel.value = level
    }

    // Add word with automatic Gemini-suggested metadata
    fun generateAndAddWord(wordText: String, level: String) {
        viewModelScope.launch {
            _isGeneratingSuggest.value = true
            _suggestError.value = null
            try {
                val filledWord = GeminiHelper.fetchWordDetails(wordText.trim(), level)
                repository.insertWord(filledWord)
            } catch (e: Exception) {
                _suggestError.value = e.message ?: "An unknown error occurred"
                // Fallback: Add directly with simple details if Gemini API has any issues
                val simpleWord = WordCard(
                    word = wordText.trim(),
                    translation = "Manual Tap to Translate",
                    pronunciationGuide = "/${wordText.trim().lowercase()}/",
                    levelName = level,
                    exampleSentence = "Enter your custom sentence.",
                    exampleTranslation = "Introduce tu traducción aquí.",
                    isCustom = true
                )
                repository.insertWord(simpleWord)
            } finally {
                _isGeneratingSuggest.value = false
            }
        }
    }

    // Add custom word manually
    fun addWordManually(word: String, translation: String, phonetic: String, level: String, sentence: String, sentenceTranslation: String) {
        viewModelScope.launch {
            val wordCard = WordCard(
                word = word.trim(),
                translation = translation.trim(),
                pronunciationGuide = if (phonetic.trim().startsWith("/")) phonetic.trim() else "/${phonetic.trim()}/",
                levelName = level,
                exampleSentence = sentence.trim(),
                exampleTranslation = sentenceTranslation.trim(),
                isCustom = true
            )
            repository.insertWord(wordCard)
        }
    }

    fun deleteWord(wordCard: WordCard) {
        viewModelScope.launch {
            repository.deleteWordById(wordCard.id)
        }
    }

    fun clearSuggestError() {
        _suggestError.value = null
    }

    // --- QUIZ GAME ENGINE ---
    fun startQuiz(levelName: String) {
        viewModelScope.launch {
            val wordsOfLevel = _allWords.value.filter { it.levelName.equals(levelName, ignoreCase = true) }
            if (wordsOfLevel.size < 2) {
                _quizScreenState.value = QuizState.NOT_ENOUGH_WORDS
                return@launch
            }

            // Shuffle and pick up to 5 words for this session
            val quizSessionQuestions = wordsOfLevel.shuffled().take(5)
            _quizQuestions.value = quizSessionQuestions
            _quizCurrentIndex.value = 0
            _quizCorrectCount.value = 0
            _quizScreenState.value = QuizState.IN_PROGRESS
            setupQuestion(quizSessionQuestions[0])
        }
    }

    private fun setupQuestion(questionWord: WordCard) {
        _selectedAnswerIndex.value = null
        _quizCorrectAnswer.value = questionWord.translation

        // Generate decoys from all other words in the DB
        val decoyPool = _allWords.value
            .filter { it.translation != questionWord.translation }
            .map { it.translation }
            .distinct()
            .shuffled()
            .take(3)

        // Combine and shuffle choices
        val choices = (decoyPool + questionWord.translation).shuffled()
        _quizOptions.value = choices
    }

    fun submitAnswer(optionIndex: Int) {
        if (_selectedAnswerIndex.value != null) return // Already answered
        _selectedAnswerIndex.value = optionIndex

        val selectedOption = _quizOptions.value[optionIndex]
        if (selectedOption == _quizCorrectAnswer.value) {
            _quizCorrectCount.value++
        }
    }

    fun nextQuestion() {
        val nextIndex = _quizCurrentIndex.value + 1
        if (nextIndex < _quizQuestions.value.size) {
            _quizCurrentIndex.value = nextIndex
            setupQuestion(_quizQuestions.value[nextIndex])
        } else {
            _quizScreenState.value = QuizState.FINISHED
        }
    }

    fun exitQuiz() {
        _quizScreenState.value = QuizState.NOT_STARTED
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}

enum class QuizState {
    NOT_STARTED,
    IN_PROGRESS,
    FINISHED,
    NOT_ENOUGH_WORDS
}
