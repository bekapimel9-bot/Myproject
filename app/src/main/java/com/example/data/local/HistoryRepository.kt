package com.example.data.local

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<TranslationHistoryEntity>> = historyDao.getAllHistory()

    fun getRecentHistory(limit: Int = 10): Flow<List<TranslationHistoryEntity>> {
        return historyDao.getRecentHistory(limit)
    }

    fun search(query: String): Flow<List<TranslationHistoryEntity>> {
        return historyDao.searchHistory(query)
    }

    suspend fun insert(
        originalText: String,
        translatedText: String,
        sourceLang: String,
        targetLang: String,
        engine: String = "ML_KIT_OFFLINE",
        latencyMs: Long = 0
    ): Long {
        if (originalText.isBlank() || translatedText.isBlank()) return -1L
        val entity = TranslationHistoryEntity(
            originalText = originalText.trim(),
            translatedText = translatedText.trim(),
            sourceLang = sourceLang,
            targetLang = targetLang,
            engine = engine,
            latencyMs = latencyMs
        )
        return historyDao.insert(entity)
    }

    suspend fun deleteById(id: Long) {
        historyDao.deleteById(id)
    }

    suspend fun clearAll() {
        historyDao.clearAll()
    }
}
