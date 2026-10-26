package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.GameLingoApp
import com.example.data.local.AppSettings
import com.example.data.local.HistoryRepository
import com.example.data.local.SettingsRepository
import com.example.data.local.TranslationHistoryEntity
import com.example.engine.translate.LanguageModelInfo
import com.example.engine.translate.TranslationEngine
import com.example.engine.translate.TranslationResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TranslationUiState {
    data object Idle : TranslationUiState
    data class Translating(val text: String) : TranslationUiState
    data class Success(val result: TranslationResult) : TranslationUiState
    data class Error(val message: String) : TranslationUiState
}

class TranslationViewModel(
    private val translationEngine: TranslationEngine,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    // ML Kit Downloaded & Downloading Language Pack States
    val downloadedModels: StateFlow<Set<String>> = translationEngine.downloadedModels
    val downloadingCodes: StateFlow<Set<String>> = translationEngine.downloadingCodes

    // App Preferences State
    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    // History States
    val recentHistory: StateFlow<List<TranslationHistoryEntity>> = historyRepository.getRecentHistory(10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allHistory: StateFlow<List<TranslationHistoryEntity>> = historyRepository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active Offline Readiness indicator
    val isOfflineReady: StateFlow<Boolean> = combine(
        settings,
        downloadedModels
    ) { currentSettings, models ->
        models.contains(currentSettings.sourceLang) && models.contains(currentSettings.targetLang)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _translationUiState = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
    val translationUiState: StateFlow<TranslationUiState> = _translationUiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    init {
        // Initialize the ML Kit Translation manager by loading all currently installed offline language packs
        refreshLanguagePacks()
    }

    /**
     * Queries Google ML Kit's RemoteModelManager to sync all downloaded on-device models.
     */
    fun refreshLanguagePacks() {
        viewModelScope.launch {
            translationEngine.refreshDownloadedModels()
        }
    }

    /**
     * Downloads an offline language pack for on-device machine translation via ML Kit.
     */
    fun downloadLanguagePack(langCode: String) {
        viewModelScope.launch {
            _toastEvents.emit("Memulai unduhan model bahasa ($langCode)...")
            val success = translationEngine.downloadLanguageModel(langCode)
            if (success) {
                _toastEvents.emit("Model bahasa ($langCode) berhasil diunduh untuk penggunaan offline!")
            } else {
                _toastEvents.emit("Gagal mengunduh model ($langCode). Pastikan koneksi internet tersedia.")
            }
        }
    }

    /**
     * Deletes an installed offline language pack to free device storage.
     */
    fun deleteLanguagePack(langCode: String) {
        viewModelScope.launch {
            val success = translationEngine.deleteLanguageModel(langCode)
            if (success) {
                _toastEvents.emit("Model bahasa ($langCode) telah dihapus dari perangkat.")
            } else {
                _toastEvents.emit("Gagal menghapus model bahasa ($langCode).")
            }
        }
    }

    /**
     * Performs on-device translation of text using the ML Kit Translation engine.
     */
    fun translateText(
        text: String,
        sourceLang: String? = null,
        targetLang: String? = null
    ) {
        val currentSettings = settings.value
        val src = sourceLang ?: currentSettings.sourceLang
        val tgt = targetLang ?: currentSettings.targetLang

        viewModelScope.launch {
            _translationUiState.value = TranslationUiState.Translating(text)
            val result = translationEngine.translate(
                text = text,
                sourceLang = src,
                targetLang = tgt,
                strictOffline = currentSettings.strictOffline
            )

            if (result.error != null) {
                _translationUiState.value = TranslationUiState.Error(result.error)
                _toastEvents.emit(result.error)
            } else {
                _translationUiState.value = TranslationUiState.Success(result)
                if (currentSettings.saveHistory && result.translatedText.isNotBlank()) {
                    historyRepository.insert(
                        originalText = result.originalText,
                        translatedText = result.translatedText,
                        sourceLang = result.sourceLang,
                        targetLang = result.targetLang,
                        engine = if (result.isOffline) "ML_KIT_OFFLINE" else "ONLINE",
                        latencyMs = result.latencyMs
                    )
                }
            }
        }
    }

    fun updateLanguages(source: String, target: String) {
        viewModelScope.launch {
            settingsRepository.updateLanguages(source, target)
        }
    }

    fun setOverlayDisplayMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setOverlayDisplayMode(mode)
        }
    }

    fun setAutoTranslate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoTranslate(enabled)
        }
    }

    fun setAutoIntervalMs(interval: Long) {
        viewModelScope.launch {
            settingsRepository.setAutoIntervalMs(interval)
        }
    }

    fun setStrictOffline(strict: Boolean) {
        viewModelScope.launch {
            settingsRepository.setStrictOffline(strict)
        }
    }

    fun setShowOriginalText(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowOriginalText(show)
        }
    }

    fun setSaveHistory(save: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSaveHistory(save)
        }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = GameLingoApp.instance
                return TranslationViewModel(
                    translationEngine = app.translationEngine,
                    settingsRepository = app.settingsRepository,
                    historyRepository = app.historyRepository
                ) as T
            }
        }
    }
}
