package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.FloatingTranslatorService
import com.example.ui.TranslationViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.models.ModelManagementScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.CyberObsidian
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.GameLingoTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GameLingoTheme {
                MainContent()
            }
        }
    }
}

enum class AppTab(val title: String) {
    DASHBOARD("Dasbor"),
    MODELS("Model"),
    HISTORY("Riwayat"),
    SETTINGS("Pengaturan")
}

@Composable
fun MainContent(
    viewModel: TranslationViewModel = viewModel(factory = TranslationViewModel.Factory)
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var isServiceRunning by remember { mutableStateOf(false) }

    // ViewModel states backed by ML Kit & repositories
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val downloadedModels by viewModel.downloadedModels.collectAsStateWithLifecycle()
    val downloadingCodes by viewModel.downloadingCodes.collectAsStateWithLifecycle()

    // Observe ViewModel toast events
    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Overlay permission state
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    // MediaProjection screen capture launcher
    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val projection = mediaProjectionManager.getMediaProjection(result.resultCode, result.data!!)
            FloatingTranslatorService.activeMediaProjection = projection

            val intent = Intent(context, FloatingTranslatorService::class.java).apply {
                action = FloatingTranslatorService.ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
            isServiceRunning = true
            Toast.makeText(context, "Penerjemah game aktif! Silakan buka game Anda.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Izin tangkapan layar dibatalkan.", Toast.LENGTH_SHORT).show()
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(CyberObsidian),
        containerColor = CyberObsidian,
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.DASHBOARD,
                    onClick = { currentTab = AppTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dasbor") },
                    label = { Text("Dasbor") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = NeonCyan.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.MODELS,
                    onClick = { currentTab = AppTab.MODELS },
                    icon = { Icon(Icons.Default.Download, contentDescription = "Model") },
                    label = { Text("Model") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = NeonCyan.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORY,
                    onClick = { currentTab = AppTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = "Riwayat") },
                    label = { Text("Riwayat") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = NeonCyan.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = { currentTab = AppTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Pengaturan") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = NeonCyan.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    isServiceRunning = isServiceRunning,
                    hasOverlayPermission = hasOverlayPermission,
                    settings = settings,
                    downloadedModels = downloadedModels,
                    recentHistory = recentHistory,
                    onRequestOverlayPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayPermissionLauncher.launch(intent)
                        }
                    },
                    onStartService = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            Toast.makeText(
                                context,
                                "Harap izinkan jendela mengambang terlebih dahulu.",
                                Toast.LENGTH_LONG
                            ).show()
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayPermissionLauncher.launch(intent)
                        } else {
                            screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                        }
                    },
                    onStopService = {
                        val intent = Intent(context, FloatingTranslatorService::class.java).apply {
                            action = FloatingTranslatorService.ACTION_STOP
                        }
                        context.stopService(intent)
                        isServiceRunning = false
                        Toast.makeText(context, "Layanan GameLingo dihentikan.", Toast.LENGTH_SHORT).show()
                    },
                    onUpdateLanguages = { src, tgt ->
                        viewModel.updateLanguages(src, tgt)
                    },
                    onSetOverlayMode = { mode ->
                        viewModel.setOverlayDisplayMode(mode)
                    },
                    onToggleAutoTranslate = { enabled ->
                        viewModel.setAutoTranslate(enabled)
                    },
                    onNavigateToModels = { currentTab = AppTab.MODELS },
                    onNavigateToHistory = { currentTab = AppTab.HISTORY }
                )

                AppTab.MODELS -> ModelManagementScreen(
                    downloadedCodes = downloadedModels,
                    downloadingCodes = downloadingCodes,
                    onDownloadModel = { code ->
                        viewModel.downloadLanguagePack(code)
                    },
                    onDeleteModel = { code ->
                        viewModel.deleteLanguagePack(code)
                    },
                    onRefresh = {
                        viewModel.refreshLanguagePacks()
                    }
                )

                AppTab.HISTORY -> HistoryScreen(
                    historyList = allHistory,
                    onDeleteEntry = { id ->
                        viewModel.deleteHistoryEntry(id)
                    },
                    onClearAll = {
                        viewModel.clearAllHistory()
                    }
                )

                AppTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onUpdateInterval = { interval ->
                        viewModel.setAutoIntervalMs(interval)
                    },
                    onToggleStrictOffline = { strict ->
                        viewModel.setStrictOffline(strict)
                    },
                    onToggleShowOriginal = { show ->
                        viewModel.setShowOriginalText(show)
                    },
                    onToggleSaveHistory = { save ->
                        viewModel.setSaveHistory(save)
                    }
                )
            }
        }
    }
}
