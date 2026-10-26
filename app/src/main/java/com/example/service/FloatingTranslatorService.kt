package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.GameLingoApp
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppSettings
import com.example.engine.capture.ScreenCaptureManager
import com.example.engine.ocr.DetectedTextBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingTranslatorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null

    // Screen capture
    private var captureManager: ScreenCaptureManager? = null

    // Overlays
    private var floatingBubbleView: View? = null
    private var floatingMenuOverlay: View? = null
    private var subtitleOverlayContainer: FrameLayout? = null
    private var cardOverlayContainer: View? = null
    private var regionSelectorOverlay: View? = null

    // State
    private var isMenuExpanded = false
    private var isAutoTranslating = false
    private var autoTranslateJob: Job? = null
    private var lastRecognizedText = ""
    private var isTranslating = false

    companion object {
        const val ACTION_START = "com.example.service.START"
        const val ACTION_STOP = "com.example.service.STOP"
        const val NOTIFICATION_ID = 1001

        // Static holder for MediaProjection created in Activity
        var activeMediaProjection: MediaProjection? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundServiceWithNotification("GameLingo Siap")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                initMediaProjection()
                showFloatingBubble()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification(statusText: String) {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingTranslatorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, GameLingoApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, "Hentikan", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initMediaProjection() {
        val mp = activeMediaProjection
        if (mp != null) {
            captureManager = ScreenCaptureManager(this, mp)
        }
    }

    // ==========================================
    // FLOATING BUBBLE
    // ==========================================
    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingBubble() {
        if (floatingBubbleView != null) return

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 400
        }

        // Programmatic cyber-styled floating orb
        val bubbleSize = (56 * resources.displayMetrics.density).toInt()
        val orb = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#141722"))
                setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#00E5FF"))
            }
            background = bg
            elevation = 16f
        }

        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter(Color.parseColor("#00E5FF"))
            val pad = (14 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        orb.addView(icon, FrameLayout.LayoutParams(bubbleSize, bubbleSize))

        // Dragging & Click Handling
        orb.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        windowManager?.updateViewLayout(orb, layoutParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            toggleFloatingMenu(layoutParams.x, layoutParams.y)
                        }
                        return true
                    }
                }
                return false
            }
        })

        floatingBubbleView = orb
        try {
            windowManager?.addView(orb, layoutParams)
        } catch (_: Exception) {}
    }

    // ==========================================
    // QUICK ACTION EXPANDED MENU
    // ==========================================
    private fun toggleFloatingMenu(bubbleX: Int, bubbleY: Int) {
        if (isMenuExpanded) {
            dismissFloatingMenu()
        } else {
            showFloatingMenu(bubbleX, bubbleY)
        }
    }

    private fun showFloatingMenu(bubbleX: Int, bubbleY: Int) {
        dismissFloatingMenu()
        isMenuExpanded = true

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (bubbleX + 70 * resources.displayMetrics.density).toInt().coerceAtLeast(20)
            y = (bubbleY - 40 * resources.displayMetrics.density).toInt().coerceAtLeast(100)
        }

        val menuLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * resources.displayMetrics.density
                setColor(Color.parseColor("#F010121A"))
                setStroke((1.5f * resources.displayMetrics.density).toInt(), Color.parseColor("#282E46"))
            }
            background = bg
            val p = (12 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
            elevation = 20f
        }

        // Action 1: Terjemahkan Sekarang
        menuLayout.addView(createMenuItem("⚡ Terjemahkan Sekarang", Color.parseColor("#00E5FF")) {
            dismissFloatingMenu()
            performManualTranslation()
        })

        // Action 2: Terjemahan Otomatis (Toggle)
        val autoLabel = if (isAutoTranslating) "⏸️ Jeda Otomatis" else "🔄 Terjemahan Otomatis"
        menuLayout.addView(createMenuItem(autoLabel, Color.parseColor("#8B5CF6")) {
            dismissFloatingMenu()
            toggleAutoTranslation()
        })

        // Action 3: Pilih Area Layar
        menuLayout.addView(createMenuItem("🎯 Pilih Area Layar", Color.parseColor("#00E676")) {
            dismissFloatingMenu()
            showRegionSelector()
        })

        // Action 4: Bersihkan Overlay
        menuLayout.addView(createMenuItem("🧹 Bersihkan Subtitle", Color.parseColor("#94A3B8")) {
            dismissFloatingMenu()
            clearSubtitles()
        })

        // Action 5: Tutup / Hentikan Layanan
        menuLayout.addView(createMenuItem("❌ Hentikan Layanan", Color.parseColor("#FF3366")) {
            dismissFloatingMenu()
            stopSelf()
        })

        floatingMenuOverlay = menuLayout
        try {
            windowManager?.addView(menuLayout, params)
        } catch (_: Exception) {}
    }

    private fun dismissFloatingMenu() {
        floatingMenuOverlay?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        floatingMenuOverlay = null
        isMenuExpanded = false
    }

    private fun createMenuItem(text: String, textColor: Int, onClick: () -> Unit): View {
        return TextView(this).apply {
            this.text = text
            this.setTextColor(textColor)
            this.textSize = 14f
            val padH = (14 * resources.displayMetrics.density).toInt()
            val padV = (10 * resources.displayMetrics.density).toInt()
            setPadding(padH, padV, padH, padV)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    // ==========================================
    // TRANSLATION PIPELINE
    // ==========================================
    private fun performManualTranslation() {
        if (isTranslating) {
            Toast.makeText(this, "Sedang memproses terjemahan...", Toast.LENGTH_SHORT).show()
            return
        }
        serviceScope.launch {
            processScreenCaptureAndTranslate(isAuto = false)
        }
    }

    private fun toggleAutoTranslation() {
        if (isAutoTranslating) {
            isAutoTranslating = false
            autoTranslateJob?.cancel()
            autoTranslateJob = null
            startForegroundServiceWithNotification("GameLingo Siap")
            Toast.makeText(this, "Terjemahan otomatis dihentikan", Toast.LENGTH_SHORT).show()
        } else {
            isAutoTranslating = true
            startForegroundServiceWithNotification("GameLingo: Mode Otomatis Aktif")
            Toast.makeText(this, "Terjemahan otomatis dimulai", Toast.LENGTH_SHORT).show()
            startAutoTranslationLoop()
        }
    }

    private fun startAutoTranslationLoop() {
        autoTranslateJob?.cancel()
        autoTranslateJob = serviceScope.launch {
            val app = GameLingoApp.instance
            while (isActive && isAutoTranslating) {
                val settings = app.settingsRepository.settingsFlow.first()
                processScreenCaptureAndTranslate(isAuto = true)
                delay(settings.autoIntervalMs.coerceAtLeast(1500L))
            }
        }
    }

    private suspend fun processScreenCaptureAndTranslate(isAuto: Boolean) {
        val capture = captureManager
        if (capture == null) {
            if (!isAuto) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FloatingTranslatorService,
                        "Izin tangkapan layar diperlukan. Silakan buka aplikasi GameLingo.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            return
        }

        isTranslating = true
        try {
            val app = GameLingoApp.instance
            val settings = app.settingsRepository.settingsFlow.first()

            val bitmap: Bitmap? = capture.captureFrame()
            if (bitmap == null) {
                isTranslating = false
                return
            }

            val regionRectF = RectF(
                settings.regionLeft,
                settings.regionTop,
                settings.regionRight,
                settings.regionBottom
            )

            // Step 1: Run ML Kit OCR
            val ocrResult = app.ocrEngine.recognizeText(bitmap, regionRectF)
            val detectedText = ocrResult.fullText.trim()

            // If empty text or identical to last text, skip duplicate translation
            if (detectedText.isEmpty()) {
                isTranslating = false
                return
            }

            if (isAuto && isSimilarText(detectedText, lastRecognizedText)) {
                // Screen dialogue unchanged, do not spam translation
                isTranslating = false
                return
            }

            lastRecognizedText = detectedText

            // Step 2: Run Offline Machine Translation
            val translation = app.translationEngine.translate(
                text = detectedText,
                sourceLang = settings.sourceLang,
                targetLang = settings.targetLang,
                strictOffline = settings.strictOffline
            )

            if (translation.error != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FloatingTranslatorService,
                        translation.error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                isTranslating = false
                return
            }

            // Step 3: Save to local History if enabled
            if (settings.saveHistory && translation.translatedText.isNotEmpty()) {
                app.historyRepository.insert(
                    originalText = detectedText,
                    translatedText = translation.translatedText,
                    sourceLang = settings.sourceLang,
                    targetLang = settings.targetLang,
                    engine = if (translation.isOffline) "ML_KIT_OFFLINE" else "ONLINE",
                    latencyMs = translation.latencyMs + ocrResult.executionTimeMs
                )
            }

            // Step 4: Display on Screen Overlay
            withContext(Dispatchers.Main) {
                if (settings.overlayDisplayMode == "IN_PLACE" && ocrResult.blocks.isNotEmpty()) {
                    showInPlaceSubtitles(ocrResult.blocks, translation.translatedText, ocrResult.frameWidth, ocrResult.frameHeight)
                } else {
                    showFloatingTranslationCard(detectedText, translation.translatedText, settings)
                }
            }
        } catch (e: Exception) {
            // Safe catch
        } finally {
            isTranslating = false
        }
    }

    private fun isSimilarText(newText: String, oldText: String): Boolean {
        if (newText == oldText) return true
        val cleanNew = newText.replace("\\s+".toRegex(), " ")
        val cleanOld = oldText.replace("\\s+".toRegex(), " ")
        if (cleanNew == cleanOld) return true
        // Levenshtein approximation for small differences
        val diffRatio = Math.abs(cleanNew.length - cleanOld.length).toFloat() / maxOf(cleanNew.length, 1)
        return diffRatio < 0.10f && cleanNew.startsWith(cleanOld.take(15))
    }

    // ==========================================
    // IN-PLACE DIALOGUE SUBTITLES
    // ==========================================
    private fun showInPlaceSubtitles(
        blocks: List<DetectedTextBlock>,
        fullTranslatedText: String,
        frameWidth: Int,
        frameHeight: Int
    ) {
        clearSubtitles()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val container = FrameLayout(this)
        subtitleOverlayContainer = container

        // Map translated text over detected blocks
        val firstBlock = blocks.firstOrNull() ?: return
        val box = firstBlock.bounds

        // Scale box to screen size
        val displayMetrics = resources.displayMetrics
        val scaleX = displayMetrics.widthPixels.toFloat() / frameWidth.coerceAtLeast(1)
        val scaleY = displayMetrics.heightPixels.toFloat() / frameHeight.coerceAtLeast(1)

        val overlayLeft = (box.left * scaleX).toInt()
        val overlayTop = (box.top * scaleY).toInt()
        val overlayWidth = ((box.width() * scaleX).toInt()).coerceAtLeast((220 * displayMetrics.density).toInt())

        val subtitleCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * displayMetrics.density
                setColor(Color.parseColor("#E60C0E14"))
                setStroke((1.5f * displayMetrics.density).toInt(), Color.parseColor("#00E5FF"))
            }
            background = bg
            val p = (10 * displayMetrics.density).toInt()
            setPadding(p, p, p, p)
            elevation = 18f
        }

        val textLabel = TextView(this).apply {
            text = fullTranslatedText
            setTextColor(Color.WHITE)
            textSize = 15f
            setLineSpacing(4f, 1.15f)
        }
        subtitleCard.addView(textLabel)

        // Close icon in subtitle card
        val closeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        val closeBtn = TextView(this).apply {
            text = "✕ Tutup"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            setPadding(12, 6, 12, 6)
            isClickable = true
            setOnClickListener { clearSubtitles() }
        }
        closeRow.addView(closeBtn)
        subtitleCard.addView(closeRow)

        val cardParams = FrameLayout.LayoutParams(
            overlayWidth.coerceAtMost((displayMetrics.widthPixels * 0.90f).toInt()),
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = overlayLeft.coerceIn(20, (displayMetrics.widthPixels * 0.15f).toInt())
            topMargin = overlayTop.coerceIn(100, (displayMetrics.heightPixels * 0.85f).toInt())
        }

        container.addView(subtitleCard, cardParams)

        try {
            windowManager?.addView(container, params)
        } catch (_: Exception) {}
    }

    private fun clearSubtitles() {
        subtitleOverlayContainer?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        subtitleOverlayContainer = null

        cardOverlayContainer?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        cardOverlayContainer = null
    }

    // ==========================================
    // FLOATING TRANSLATION CARD (ALTERNATIVE MODE)
    // ==========================================
    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingTranslationCard(
        originalText: String,
        translatedText: String,
        settings: AppSettings
    ) {
        clearSubtitles()

        val displayMetrics = resources.displayMetrics
        val params = WindowManager.LayoutParams(
            (displayMetrics.widthPixels * 0.88f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (80 * displayMetrics.density).toInt()
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18 * displayMetrics.density
                setColor(Color.parseColor("#F510121A"))
                setStroke((1.5f * displayMetrics.density).toInt(), Color.parseColor("#7C4DFF"))
            }
            background = bg
            val p = (14 * displayMetrics.density).toInt()
            setPadding(p, p, p, p)
            elevation = 20f
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "GameLingo Offline"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            setPadding(16, 4, 16, 4)
            isClickable = true
            setOnClickListener { clearSubtitles() }
        }
        header.addView(title)
        header.addView(closeBtn)
        card.addView(header)

        // Translated Text
        val transView = TextView(this).apply {
            text = translatedText
            setTextColor(Color.WHITE)
            textSize = 16f
            setLineSpacing(4f, 1.15f)
            val padTop = (8 * displayMetrics.density).toInt()
            setPadding(0, padTop, 0, padTop)
        }
        card.addView(transView)

        // Original Text (if enabled)
        if (settings.showOriginalText && originalText.isNotEmpty()) {
            val origView = TextView(this).apply {
                text = originalText
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 12f
                val padTop = (4 * displayMetrics.density).toInt()
                setPadding(0, padTop, 0, 0)
            }
            card.addView(origView)
        }

        cardOverlayContainer = card
        try {
            windowManager?.addView(card, params)
        } catch (_: Exception) {}
    }

    // ==========================================
    // SCREEN REGION SELECTION OVERLAY
    // ==========================================
    @SuppressLint("ClickableViewAccessibility")
    private fun showRegionSelector() {
        dismissRegionSelector()

        val displayMetrics = resources.displayMetrics
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val overlay = RegionSelectorView(this, displayMetrics) { leftRatio, topRatio, rightRatio, bottomRatio ->
            serviceScope.launch {
                GameLingoApp.instance.settingsRepository.updateRegion(
                    leftRatio,
                    topRatio,
                    rightRatio,
                    bottomRatio
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FloatingTranslatorService,
                        "Area layar berhasil disimpan!",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismissRegionSelector()
                }
            }
        }

        regionSelectorOverlay = overlay
        try {
            windowManager?.addView(overlay, params)
        } catch (_: Exception) {}
    }

    private fun dismissRegionSelector() {
        regionSelectorOverlay?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        regionSelectorOverlay = null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        dismissFloatingMenu()
        clearSubtitles()
        dismissRegionSelector()
        floatingBubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        captureManager?.release()
        captureManager = null
    }
}
