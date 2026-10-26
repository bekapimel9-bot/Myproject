package com.example.engine.translate

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class LanguageModelInfo(
    val code: String,
    val displayName: String,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val estimatedSizeMb: Int = 30
)

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val isOffline: Boolean = true,
    val latencyMs: Long = 0,
    val error: String? = null
)

class TranslationEngine {

    private val modelManager = RemoteModelManager.getInstance()
    private val translators = ConcurrentHashMap<String, Translator>()

    private val _downloadedModels = MutableStateFlow<Set<String>>(emptySet())
    val downloadedModels: StateFlow<Set<String>> = _downloadedModels.asStateFlow()

    private val _downloadingCodes = MutableStateFlow<Set<String>>(emptySet())
    val downloadingCodes: StateFlow<Set<String>> = _downloadingCodes.asStateFlow()

    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            LanguageModelInfo("en", "Inggris (English)"),
            LanguageModelInfo("id", "Indonesia (Bahasa Indonesia)"),
            LanguageModelInfo("ja", "Jepang (Japanese)"),
            LanguageModelInfo("zh", "Mandarin (Chinese)"),
            LanguageModelInfo("ko", "Korea (Korean)"),
            LanguageModelInfo("es", "Spanyol (Spanish)"),
            LanguageModelInfo("fr", "Prancis (French)"),
            LanguageModelInfo("de", "Jerman (German)"),
            LanguageModelInfo("ru", "Rusia (Russian)")
        )
    }

    suspend fun refreshDownloadedModels(): Set<String> = withContext(Dispatchers.IO) {
        try {
            val models = Tasks.await(modelManager.getDownloadedModels(TranslateRemoteModel::class.java))
            val codes = models.map { it.language }.toSet()
            _downloadedModels.value = codes
            codes
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun isLanguageDownloaded(langCode: String): Boolean {
        return _downloadedModels.value.contains(langCode)
    }

    suspend fun downloadLanguageModel(langCode: String): Boolean = withContext(Dispatchers.IO) {
        val model = TranslateRemoteModel.Builder(langCode).build()
        val conditions = DownloadConditions.Builder().build()
        _downloadingCodes.value = _downloadingCodes.value + langCode
        try {
            Tasks.await(modelManager.download(model, conditions))
            refreshDownloadedModels()
            true
        } catch (e: Exception) {
            false
        } finally {
            _downloadingCodes.value = _downloadingCodes.value - langCode
        }
    }

    suspend fun deleteLanguageModel(langCode: String): Boolean = withContext(Dispatchers.IO) {
        val model = TranslateRemoteModel.Builder(langCode).build()
        try {
            Tasks.await(modelManager.deleteDownloadedModel(model))
            // Remove cached translators using this language
            val keysToRemove = translators.keys.filter { it.startsWith(langCode) || it.endsWith(langCode) }
            keysToRemove.forEach { key ->
                translators.remove(key)?.close()
            }
            refreshDownloadedModels()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
        strictOffline: Boolean = true
    ): TranslationResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext TranslationResult(
                originalText = text,
                translatedText = "",
                sourceLang = sourceLang,
                targetLang = targetLang,
                isOffline = true,
                latencyMs = 0
            )
        }

        // Map language code to ML Kit translate constants if needed
        val srcTag = TranslateLanguage.fromLanguageTag(sourceLang) ?: sourceLang
        val tgtTag = TranslateLanguage.fromLanguageTag(targetLang) ?: targetLang

        val cacheKey = "$srcTag->$tgtTag"
        var translator = translators[cacheKey]

        if (translator == null) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(srcTag)
                .setTargetLanguage(tgtTag)
                .build()
            translator = Translation.getClient(options)
            translators[cacheKey] = translator
        }

        try {
            // Check if model is downloaded locally
            val conditions = DownloadConditions.Builder().build()
            // If strictOffline is true, we test if the model is already downloaded without fetching
            if (strictOffline && (!_downloadedModels.value.contains(srcTag) || !_downloadedModels.value.contains(tgtTag))) {
                // If model download list is not yet cached, attempt to verify
                val downloaded = refreshDownloadedModels()
                if (!downloaded.contains(srcTag) || !downloaded.contains(tgtTag)) {
                    val missing = when {
                        !downloaded.contains(srcTag) && !downloaded.contains(tgtTag) -> "$srcTag & $tgtTag"
                        !downloaded.contains(srcTag) -> srcTag
                        else -> tgtTag
                    }
                    return@withContext TranslationResult(
                        originalText = text,
                        translatedText = "",
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        isOffline = true,
                        latencyMs = System.currentTimeMillis() - startTime,
                        error = "Model bahasa ($missing) belum diunduh untuk mode offline."
                    )
                }
            } else if (!strictOffline) {
                // Download if necessary when strict offline is off
                Tasks.await(translator.downloadModelIfNeeded(conditions))
                refreshDownloadedModels()
            }

            val translated = Tasks.await(translator.translate(trimmed))
            val latency = System.currentTimeMillis() - startTime
            TranslationResult(
                originalText = text,
                translatedText = translated,
                sourceLang = sourceLang,
                targetLang = targetLang,
                isOffline = true,
                latencyMs = latency
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            TranslationResult(
                originalText = text,
                translatedText = "",
                sourceLang = sourceLang,
                targetLang = targetLang,
                isOffline = true,
                latencyMs = latency,
                error = e.localizedMessage ?: "Gagal menerjemahkan teks."
            )
        }
    }

    fun close() {
        translators.values.forEach {
            try {
                it.close()
            } catch (_: Exception) {}
        }
        translators.clear()
    }
}
