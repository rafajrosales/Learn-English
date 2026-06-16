package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(val text: String)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(val contents: List<Content>)

@JsonClass(generateAdapter = true)
data class PartResponse(val text: String?)

@JsonClass(generateAdapter = true)
data class ContentResponse(val parts: List<PartResponse>)

@JsonClass(generateAdapter = true)
data class Candidate(val content: ContentResponse?)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

object GeminiHelper {
    suspend fun fetchWordDetails(word: String, defaultLevelName: String): WordCard {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("API key is missing. Please configure GEMINI_API_KEY in the Secrets panel.")
        }

        val prompt = """
            Provide the Spanish translation, IPA phonetic pronunciation guide, learner level (one of: Beginner, Intermediate, Advanced), an English example sentence, and its Spanish translation for the English word: "$word".
            Output EXACTLY with the following labels and nothing else:
            Translation: <translation>
            Phonetic: <ipa_phonetic_pronunciation_guide_wrapped_in_slashes>
            Level: <level>
            Example: <english_example_sentence>
            ExampleTranslation: <spanish_translation_of_example_sentence>
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from Gemini API")
            
            return parseGeminiResponse(rawText, word, defaultLevelName)
        } catch (e: Exception) {
            throw Exception("Gemini suggestions failed: ${e.message}")
        }
    }

    private fun parseGeminiResponse(rawText: String, originalWord: String, defaultLevel: String): WordCard {
        var translation = ""
        var phonetic = ""
        var level = defaultLevel
        var example = ""
        var exampleTranslation = ""

        val lines = rawText.split("\n")
        for (line in lines) {
            val cleanLine = line.trim()
            when {
                cleanLine.startsWith("Translation:", ignoreCase = true) -> {
                    translation = cleanLine.substringAfter("Translation:").trim()
                }
                cleanLine.startsWith("Phonetic:", ignoreCase = true) -> {
                    phonetic = cleanLine.substringAfter("Phonetic:").trim()
                }
                cleanLine.startsWith("Level:", ignoreCase = true) -> {
                    val parsedLevel = cleanLine.substringAfter("Level:").trim()
                    if (parsedLevel.equals("Beginner", ignoreCase = true) ||
                        parsedLevel.equals("Intermediate", ignoreCase = true) ||
                        parsedLevel.equals("Advanced", ignoreCase = true)) {
                        level = parsedLevel
                    }
                }
                cleanLine.startsWith("Example:", ignoreCase = true) -> {
                    example = cleanLine.substringAfter("Example:").trim()
                }
                cleanLine.startsWith("ExampleTranslation:", ignoreCase = true) -> {
                    exampleTranslation = cleanLine.substringAfter("ExampleTranslation:").trim()
                }
            }
        }

        // Fallbacks if parsed patterns were empty or missing
        if (translation.isEmpty()) translation = ""
        if (phonetic.isEmpty()) phonetic = "/${originalWord.lowercase()}/"
        if (example.isEmpty()) example = "The word is $originalWord."
        if (exampleTranslation.isEmpty()) exampleTranslation = "La palabra es $originalWord."

        return WordCard(
            word = originalWord,
            translation = translation,
            pronunciationGuide = phonetic,
            levelName = level,
            exampleSentence = example,
            exampleTranslation = exampleTranslation,
            isCustom = true
        )
    }
}
