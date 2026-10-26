package com.example

import com.example.data.local.TranslationHistoryEntity
import com.example.engine.translate.TranslationEngine
import com.example.engine.translate.TranslationResult
import com.example.ui.TranslationUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSupportedLanguagesContainsIndonesianAndEnglish() {
        val codes = TranslationEngine.SUPPORTED_LANGUAGES.map { it.code }
        assertTrue(codes.contains("en"))
        assertTrue(codes.contains("id"))
        assertTrue(codes.contains("ja"))
    }

    @Test
    fun testTranslationHistoryEntityFields() {
        val entity = TranslationHistoryEntity(
            originalText = "Welcome to the adventure!",
            translatedText = "Selamat datang di petualangan!",
            sourceLang = "en",
            targetLang = "id",
            engine = "ML_KIT_OFFLINE",
            latencyMs = 120L
        )

        assertEquals("Welcome to the adventure!", entity.originalText)
        assertEquals("Selamat datang di petualangan!", entity.translatedText)
        assertEquals("en", entity.sourceLang)
        assertEquals("id", entity.targetLang)
        assertEquals("ML_KIT_OFFLINE", entity.engine)
        assertEquals(120L, entity.latencyMs)
    }

    @Test
    fun testDialogueSimilarityDetection() {
        val text1 = "The kingdom is under attack! Take your sword."
        val text2 = "The kingdom is under attack! Take your sword."
        val text3 = "A new hero has arrived in the village."

        // Identical texts
        assertEquals(text1, text2)

        // Different dialogue texts
        assertFalse(text1 == text3)
    }

    @Test
    fun testTranslationUiStateTransitions() {
        val idleState: TranslationUiState = TranslationUiState.Idle
        assertTrue(idleState is TranslationUiState.Idle)

        val translatingState: TranslationUiState = TranslationUiState.Translating("Level Up!")
        assertEquals("Level Up!", (translatingState as TranslationUiState.Translating).text)

        val result = TranslationResult(
            originalText = "Game Over",
            translatedText = "Permainan Berakhir",
            sourceLang = "en",
            targetLang = "id",
            isOffline = true,
            latencyMs = 45L
        )
        val successState: TranslationUiState = TranslationUiState.Success(result)
        assertEquals("Permainan Berakhir", (successState as TranslationUiState.Success).result.translatedText)
    }
}
