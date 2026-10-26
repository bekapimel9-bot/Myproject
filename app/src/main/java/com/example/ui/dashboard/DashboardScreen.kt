package com.example.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettings
import com.example.data.local.TranslationHistoryEntity
import com.example.engine.translate.TranslationEngine
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberObsidian
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RubyStop
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun DashboardScreen(
    isServiceRunning: Boolean,
    hasOverlayPermission: Boolean,
    settings: AppSettings,
    downloadedModels: Set<String>,
    recentHistory: List<TranslationHistoryEntity>,
    onRequestOverlayPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onUpdateLanguages: (source: String, target: String) -> Unit,
    onSetOverlayMode: (mode: String) -> Unit,
    onToggleAutoTranslate: (enabled: Boolean) -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val isSourceDownloaded = downloadedModels.contains(settings.sourceLang)
    val isTargetDownloaded = downloadedModels.contains(settings.targetLang)
    val isOfflineReady = isSourceDownloaded && isTargetDownloaded

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberObsidian)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header & Offline Readiness Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth().testTag("status_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GameLingo Offline",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Penerjemah Layar Game On-Device",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        // Readiness Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isOfflineReady) EmeraldReady.copy(alpha = 0.15f)
                                    else Color(0xFFFFB300).copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (isOfflineReady) EmeraldReady.copy(alpha = 0.6f)
                                    else Color(0xFFFFB300).copy(alpha = 0.6f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isOfflineReady) EmeraldReady else Color(0xFFFFB300))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOfflineReady) "Siap Offline" else "Perlu Unduh Model",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOfflineReady) EmeraldReady else Color(0xFFFFB300)
                                )
                            }
                        }
                    }

                    if (!isOfflineReady) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberSurfaceVariant)
                                .clickable { onNavigateToModels() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Model bahasa (${settings.sourceLang.uppercase()} → ${settings.targetLang.uppercase()}) belum diunduh. Ketuk untuk mengunduh.",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Overlay Permission Warning (if missing)
        if (!hasOverlayPermission) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF231B15)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF8A00).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("permission_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF8A00)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Izin Jendela Mengambang Diperlukan",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "GameLingo memerlukan izin untuk menampilkan gelembung dan subtitle di atas layar game Anda.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestOverlayPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A00)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("grant_overlay_button")
                        ) {
                            Text("Buka Pengaturan Izin", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Main Service Action Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Kontrol Penerjemah Game",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isServiceRunning)
                            "Layanan sedang berjalan. Buka game Anda dan ketuk gelembung GameLingo di layar."
                        else
                            "Mulai layanan untuk mengaktifkan gelembung terjemahan mengambang di atas game.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    if (isServiceRunning) {
                        Button(
                            onClick = onStopService,
                            colors = ButtonDefaults.buttonColors(containerColor = RubyStop),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("stop_service_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hentikan Penerjemah Layar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = onStartService,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_service_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0C0E14))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mulai Penerjemah Layar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0C0E14))
                        }
                    }
                }
            }
        }

        // Language Selection & Overlay Mode Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Konfigurasi Terjemahan",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Language Pickers Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LanguageDropdown(
                            label = "Bahasa Sumber (Game)",
                            selectedCode = settings.sourceLang,
                            modifier = Modifier.weight(1f)
                        ) { newSource ->
                            onUpdateLanguages(newSource, settings.targetLang)
                        }

                        LanguageDropdown(
                            label = "Bahasa Terjemahan",
                            selectedCode = settings.targetLang,
                            modifier = Modifier.weight(1f)
                        ) { newTarget ->
                            onUpdateLanguages(settings.sourceLang, newTarget)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Overlay Mode Selection
                    Text(
                        text = "Gaya Tampilan Overlay",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isDirect = settings.overlayDisplayMode == "IN_PLACE"
                        OverlayModeButton(
                            title = "Subtitle Langsung",
                            subtitle = "Tepat di atas dialog game",
                            isSelected = isDirect,
                            modifier = Modifier.weight(1f)
                        ) {
                            onSetOverlayMode("IN_PLACE")
                        }

                        OverlayModeButton(
                            title = "Kartu Mengambang",
                            subtitle = "Panel teks bawah",
                            isSelected = !isDirect,
                            modifier = Modifier.weight(1f)
                        ) {
                            onSetOverlayMode("CARD")
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Auto-Translate Quick Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Terjemahan Otomatis Berkala",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Mendeteksi perubahan dialog game otomatis",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = settings.autoTranslateEnabled,
                            onCheckedChange = { onToggleAutoTranslate(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = Color(0xFF004F58)
                            )
                        )
                    }
                }
            }
        }

        // Recent History Preview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Riwayat Terjemahan Terakhir",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Lihat Semua",
                            fontSize = 13.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onNavigateToHistory() }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (recentHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada dialog yang diterjemahkan.\nMulai layanan dan tangkap layar game Anda.",
                                fontSize = 13.sp,
                                color = TextTertiary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        recentHistory.take(3).forEach { item ->
                            RecentHistoryRow(item = item) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("GameLingo", item.translatedText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Teks disalin ke papan klip", Toast.LENGTH_SHORT).show()
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageDropdown(
    label: String,
    selectedCode: String,
    modifier: Modifier = Modifier,
    onSelect: (code: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedInfo = TranslationEngine.SUPPORTED_LANGUAGES.find { it.code == selectedCode }
        ?: TranslationEngine.SUPPORTED_LANGUAGES.first()

    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurfaceVariant)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedInfo.displayName.substringBefore(" ("),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(CyberSurface)
            ) {
                TranslationEngine.SUPPORTED_LANGUAGES.forEach { lang ->
                    DropdownMenuItem(
                        text = {
                            Text(text = lang.displayName, fontSize = 13.sp, color = TextPrimary)
                        },
                        onClick = {
                            onSelect(lang.code)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OverlayModeButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.12f) else CyberSurfaceVariant)
            .border(
                1.5.dp,
                if (isSelected) NeonCyan else CyberCardBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) NeonCyan else TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun RecentHistoryRow(
    item: TranslationHistoryEntity,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.translatedText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.originalText,
                fontSize = 11.sp,
                color = TextTertiary,
                maxLines = 1
            )
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Salin",
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
