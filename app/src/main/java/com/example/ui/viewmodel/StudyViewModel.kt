package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.calculator.CalculatorEvaluator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Simple custom stroke structures for Scribble Pad
data class DrawPoint(val x: Float, val y: Float)
data class DrawStroke(val points: List<DrawPoint>, val colorHex: String, val width: Float)

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val studyDao = AppDatabase.getDatabase(application).studyDao()
    private val repository = StudyRepository(studyDao)

    // === Global UI State ===
    val isDarkMode = MutableStateFlow(true) // Start with premium dark mode
    val currentTab = MutableStateFlow("CHECKLIST") // TAB_CHECKLIST, TAB_SCRIBBLE, TAB_NOTES, TAB_POMODORO, TAB_CALCULATOR

    // === Checklist State ===
    val checklistItems: StateFlow<List<ChecklistItem>> = repository.allChecklistItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addChecklistItem(title: String, priority: String, category: String) {
        viewModelScope.launch {
            repository.insertChecklistItem(
                ChecklistItem(
                    title = title.trim(),
                    priority = priority,
                    category = category
                )
            )
        }
    }

    fun toggleChecklistItem(item: ChecklistItem) {
        viewModelScope.launch {
            repository.updateChecklistItem(item.copy(isCompleted = !item.isCompleted))
        }
    }

    fun deleteChecklistItem(id: Int) {
        viewModelScope.launch {
            repository.deleteChecklistItem(id)
        }
    }

    // === Scribble Pad State ===
    val scribbles: StateFlow<List<ScribbleItem>> = repository.allScribbles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStrokes = MutableStateFlow<List<DrawStroke>>(emptyList())
    val currentBrushColorHex = MutableStateFlow("#FF00E5FF") // High contrast cyan
    val currentBrushWidth = MutableStateFlow(8f)
    val scribbleTitle = MutableStateFlow("My Study Mindmap")

    fun serializeStrokes(strokes: List<DrawStroke>): String {
        return strokes.joinToString(";") { stroke ->
            val pointsStr = stroke.points.joinToString("|") { "${it.x},${it.y}" }
            "${stroke.colorHex}:${stroke.width}:$pointsStr"
        }
    }

    fun deserializeStrokes(data: String): List<DrawStroke> {
        if (data.isBlank()) return emptyList()
        return try {
            data.split(";").mapNotNull { strokeStr ->
                if (strokeStr.isBlank()) return@mapNotNull null
                val parts = strokeStr.split(":")
                if (parts.size < 3) return@mapNotNull null
                val colorHex = parts[0]
                val width = parts[1].toFloatOrNull() ?: 8f
                val pointsStr = parts[2]
                val points = pointsStr.split("|").mapNotNull { ptStr ->
                    val coords = ptStr.split(",")
                    if (coords.size == 2) {
                        val x = coords[0].toFloatOrNull() ?: return@mapNotNull null
                        val y = coords[1].toFloatOrNull() ?: return@mapNotNull null
                        DrawPoint(x, y)
                    } else null
                }
                DrawStroke(points, colorHex, width)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveScribble(title: String) {
        viewModelScope.launch {
            val serialized = serializeStrokes(activeStrokes.value)
            repository.insertScribble(
                ScribbleItem(
                    title = if (title.isBlank()) "Untitled Sketch" else title,
                    strokeData = serialized
                )
            )
        }
    }

    fun loadScribble(item: ScribbleItem) {
        scribbleTitle.value = item.title
        activeStrokes.value = deserializeStrokes(item.strokeData)
    }

    fun deleteScribble(id: Int) {
        viewModelScope.launch {
            repository.deleteScribble(id)
        }
    }

    fun clearScribbleCanvas() {
        activeStrokes.value = emptyList()
    }

    // === Multimedia Notes State ===
    val notes: StateFlow<List<NoteItem>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addNote(title: String, content: String, type: String, uri: String?) {
        viewModelScope.launch {
            repository.insertNote(
                NoteItem(
                    title = if (title.isBlank()) "Quick ${type.lowercase().replaceFirstChar { it.uppercase() }} Note" else title,
                    content = content,
                    noteType = type,
                    mediaUri = uri
                )
            )
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    // === Pomodoro Timer State ===
    private var timerJob: Job? = null
    val pomodoroTimeLeft = MutableStateFlow(25 * 60) // Default 25 min
    val isTimerRunning = MutableStateFlow(false)
    val currentTimerType = MutableStateFlow("STUDY") // STUDY, SHORT_BREAK, LONG_BREAK
    val sessionCount = MutableStateFlow(0)
    val customDurationMinutes = MutableStateFlow(25)

    // === Focus Shield State ===
    val isFocusShieldEnabled = MutableStateFlow(true)
    val customStudyAppName = MutableStateFlow("Duolingo")
    val customStudyAppPackage = MutableStateFlow("com.duolingo")
    val isForbiddenAppActive = MutableStateFlow(false)
    val forbiddenAppName = MutableStateFlow("")

    fun isUsageStatsPermissionGranted(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getForegroundPackageName(context: Context): String? {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 10000 // Last 10 seconds
            val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            if (usageStats.isNullOrEmpty()) return null
            
            // Find the one with latest lastTimeUsed
            val sorted = usageStats.sortedByDescending { it.lastTimeUsed }
            sorted.firstOrNull()?.packageName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun checkFocusShieldViolation() {
        try {
            if (!isFocusShieldEnabled.value || currentTimerType.value != "STUDY" || !isTimerRunning.value) {
                isForbiddenAppActive.value = false
                return
            }
            
            val context = getApplication<Application>()
            if (isUsageStatsPermissionGranted(context)) {
                val foregroundPackage = getForegroundPackageName(context)
                if (foregroundPackage != null) {
                    val allowedPackages = listOf(
                        context.packageName,
                        "com.google.android.googlequicksearchbox", // Google App
                        "com.google.android.youtube",             // YouTube
                        "com.whatsapp",                            // WhatsApp
                        "com.google.android.apps.classroom",       // Google Classroom
                        customStudyAppPackage.value.trim()         // User's custom allowed app
                    )
                    
                    val isSystemOrHome = foregroundPackage.contains("launcher") || 
                                         foregroundPackage.contains("home") || 
                                         foregroundPackage.contains("car") ||
                                         foregroundPackage == "com.android.systemui" ||
                                         foregroundPackage == "android"
                    
                    if (foregroundPackage !in allowedPackages && !isSystemOrHome) {
                        val pm = context.packageManager
                        val appName = try {
                            val appInfo = pm.getApplicationInfo(foregroundPackage, 0)
                            pm.getApplicationLabel(appInfo).toString()
                        } catch (e: Exception) {
                            foregroundPackage.substringAfterLast('.')
                        }
                        forbiddenAppName.value = appName
                        isForbiddenAppActive.value = true
                    } else {
                        isForbiddenAppActive.value = false
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isForbiddenAppActive.value = false
        }
    }

    fun setTimerType(type: String) {
        stopTimer()
        currentTimerType.value = type
        val mins = when (type) {
            "STUDY" -> customDurationMinutes.value
            "SHORT_BREAK" -> 5
            "LONG_BREAK" -> 15
            else -> 25
        }
        pomodoroTimeLeft.value = mins * 60
    }

    fun adjustStudyDuration(deltaMin: Int) {
        val nextVal = (customDurationMinutes.value + deltaMin).coerceIn(1, 180)
        customDurationMinutes.value = nextVal
        if (currentTimerType.value == "STUDY") {
            setTimerType("STUDY")
        }
    }

    fun startTimer() {
        if (isTimerRunning.value) return
        isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (isTimerRunning.value && pomodoroTimeLeft.value > 0) {
                delay(1000)
                pomodoroTimeLeft.value -= 1
                if (currentTimerType.value == "STUDY" && isFocusShieldEnabled.value) {
                    checkFocusShieldViolation()
                }
            }
            if (pomodoroTimeLeft.value == 0) {
                isTimerRunning.value = false
                playFocusDoneSound()
                if (currentTimerType.value == "STUDY") {
                    sessionCount.value += 1
                    setTimerType("SHORT_BREAK")
                } else {
                    setTimerType("STUDY")
                }
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun stopTimer() {
        isTimerRunning.value = false
        timerJob?.cancel()
        val mins = when (currentTimerType.value) {
            "STUDY" -> customDurationMinutes.value
            "SHORT_BREAK" -> 5
            "LONG_BREAK" -> 15
            else -> 25
        }
        pomodoroTimeLeft.value = mins * 60
    }

    private fun playFocusDoneSound() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // === Scientific Calculator State ===
    val calcExpression = MutableStateFlow("")
    val calcResult = MutableStateFlow("")

    fun onCalculatorKeyPressed(key: String) {
        when (key) {
            "C" -> {
                calcExpression.value = ""
                calcResult.value = ""
            }
            "⌫" -> {
                val current = calcExpression.value
                if (current.isNotEmpty()) {
                    calcExpression.value = current.dropLast(1)
                }
            }
            "=" -> {
                try {
                    val result = CalculatorEvaluator.evaluate(calcExpression.value)
                    // Format output
                    calcResult.value = if (result.isNaN()) "Error"
                                      else if (result == Double.POSITIVE_INFINITY || result == Double.NEGATIVE_INFINITY) "Overflow"
                                      else if (result % 1.0 == 0.0) result.toLong().toString()
                                      else String.format("%.6f", result).trimEnd('0').trimEnd('.')
                } catch (e: Exception) {
                    calcResult.value = "Error"
                }
            }
            "sin", "cos", "tan", "log", "ln" -> {
                calcExpression.value += "$key("
            }
            "√" -> {
                calcExpression.value += "√("
            }
            else -> {
                calcExpression.value += key
            }
        }
    }
}
