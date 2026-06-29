package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LessonPlan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image

/**
 * Pure digital DSP tone synthesizer for alerts. Generated on-the-fly via AudioTrack.
 */
fun playSyntheticChime(type: Int) {
    Thread {
        try {
            val sampleRate = 44100
            val durationSeconds = if (type == 1) 0.35 else 0.6
            val numSamples = (durationSeconds * sampleRate).toInt()
            val sample = DoubleArray(numSamples)
            val generatedSnd = ByteArray(2 * numSamples)

            if (type == 1) {
                // High frequency sharp alert chime (B5 note transition)
                val freq = 987.77
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = if (t < 0.05) t / 0.05 else if (t > 0.25) (0.35 - t) / 0.1 else 1.0
                    sample[i] = Math.sin(2.0 * Math.PI * freq * t) * envelope
                }
            } else {
                // Harmonic dual tone swipe chime for exact timer transitions
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = if (t < 0.05) t / 0.05 else if (t > 0.5) (0.6 - t) / 0.1 else 1.0
                    val freq = if (t < 0.3) 523.25 else 659.25 // Middle C5 to E5 transition
                    sample[i] = Math.sin(2.0 * Math.PI * freq * t) * envelope
                }
            }

            var idx = 0
            for (dVal in sample) {
                val valShort = (dVal * 32767).toInt().toShort()
                generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
            }

            val audioTrack = android.media.AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.size,
                android.media.AudioTrack.MODE_STATIC
            )
            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
            Thread.sleep((durationSeconds * 1000).toLong() + 100)
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}

fun generateQrCodeBitmap(content: String, sizePix: Int): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            sizePix,
            sizePix
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getCurrentWifiSsid(context: Context): String {
    return try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val info = wifiManager.connectionInfo
        if (info != null && info.ssid != null && info.ssid != "<unknown ssid>") {
            info.ssid.removeSurrounding("\"")
        } else {
            "BASKOG_WIFI_TTY_DIRECT"
        }
    } catch (e: Exception) {
        "BASKOG_WIFI_TTY_DIRECT"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPrompterScreen(
    plan: LessonPlan,
    viewModel: com.example.ui.MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    // Safely initialize TextToSpeech Engine with resource lifecycle handling
    DisposableEffect(Unit) {
        val speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale.US
            }
        }
        tts = speech
        onDispose {
            speech.stop()
            speech.shutdown()
        }
    }

    // Allocate times based on modern lesson pacing weights
    val totalMins = plan.durationMins
    val intentionsMins = maxOf(1, (totalMins * 0.15).toInt())
    val learningMins = maxOf(2, (totalMins * 0.60).toInt())
    val assessmentMins = maxOf(1, (totalMins * 0.15).toInt())
    val waysForwardMins = maxOf(1, totalMins - (intentionsMins + learningMins + assessmentMins))

    val stages = remember(plan) {
        listOf(
            PrompterStageItem("I - INTENTIONS", "Establish Objectives & Focus Points", intentionsMins * 60, plan.intentions, Color(0xFF818CF8)),
            PrompterStageItem("L - LEARNING EXPERIENCES", "Class Procedure, Storytelling & Activities", learningMins * 60, plan.learningExperiences, Color(0xFF60A5FA)),
            PrompterStageItem("A - ASSESSMENT", "PACE Method & Transitional Performance Grading", assessmentMins * 60, plan.assessment, Color(0xFF34D399)),
            PrompterStageItem("W - WAYS FORWARD", "Remediation, Extension & Local Homework", waysForwardMins * 60, plan.waysForward, Color(0xFFFBBF24))
        )
    }

    var activeStageIdx by remember { mutableStateOf(0) }
    var currentSecondsLeft by remember { mutableStateOf(stages[0].durationSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var clockMultiplier by remember { mutableStateOf(1) } // 1x standard, 60x fast-forward simulation

    var bottomActiveTab by remember { mutableStateOf(0) }

    val qText by viewModel.formativeQuestionText.collectAsStateWithLifecycle()
    val qType by viewModel.formativeQuestionType.collectAsStateWithLifecycle()
    val correctAns by viewModel.formativeCorrectAnswer.collectAsStateWithLifecycle()
    val options by viewModel.formativeOptions.collectAsStateWithLifecycle()
    val serverActive by viewModel.formativeServerActive.collectAsStateWithLifecycle()
    val students by viewModel.formativeSimulatedStudents.collectAsStateWithLifecycle()
    val history by viewModel.formativeHistory.collectAsStateWithLifecycle()

    val questionsPool by viewModel.formativeQuestionsList.collectAsStateWithLifecycle()
    val activeIndex by viewModel.formativeActiveQuestionIndex.collectAsStateWithLifecycle()
    val joinMethod by viewModel.serverJoinMethod.collectAsStateWithLifecycle()
    val localIp by viewModel.localIpAddressState.collectAsStateWithLifecycle()

    LaunchedEffect(plan) {
        if (plan.formativeQuestionsJson.isNotEmpty()) {
            try {
                val arr = org.json.JSONArray(plan.formativeQuestionsJson)
                viewModel.clearQuestionPool()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val qTxt = obj.optString("question")
                    val ty = obj.optString("type", "Multiple Choice")
                    val correct = obj.optString("correct_answer")
                    val optsArr = obj.optJSONArray("options")
                    val optionsList = mutableListOf<String>()
                    if (optsArr != null) {
                        for (j in 0 until optsArr.length()) {
                            optionsList.add(optsArr.getString(j))
                        }
                    }
                    viewModel.addQuestionToPool(qTxt, ty, correct, optionsList)
                }
                viewModel.selectActiveQuestionFromPool(0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val activeStage = stages[activeStageIdx]

    // TTS speaker helper with mute checker
    fun speak(phrase: String) {
        if (!isMuted && isTtsReady) {
            tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "prompt_speech")
        }
    }

    // Speech trigger state trackers to avoid repeating a warning
    var warnedAboutTransitionForStage by remember { mutableStateOf(-1) }
    var announcedExactStageCommenced by remember { mutableStateOf(-1) }

    // Master Clock Timer Effect
    LaunchedEffect(isRunning, currentSecondsLeft, clockMultiplier, activeStageIdx) {
        if (isRunning && currentSecondsLeft > 0) {
            val stepDelay = 1000L / clockMultiplier
            delay(stepDelay)
            currentSecondsLeft = maxOf(0, currentSecondsLeft - 1)
        } else if (isRunning && currentSecondsLeft == 0) {
            // Auto transition to next stage if available
            if (activeStageIdx < stages.lastIndex) {
                activeStageIdx++
                currentSecondsLeft = stages[activeStageIdx].durationSeconds
            } else {
                isRunning = false
                speak("Success. All lesson blocks have been carried out. Lesson completion logging starting.")
                Toast.makeText(context, "Lesson plan blueprint fully executed!", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Trigger exact transition speech & synthetic chimes on stage indexes changes
    LaunchedEffect(activeStageIdx) {
        if (announcedExactStageCommenced != activeStageIdx) {
            announcedExactStageCommenced = activeStageIdx
            if (isRunning) {
                playSyntheticChime(2)
                speak("Attention. COMMENCING standard block sequence: ${stages[activeStageIdx].name}.")
            }
        }
    }

    // Pacing Audio Alerts Monitor
    LaunchedEffect(currentSecondsLeft, activeStageIdx) {
        val nextStageName = if (activeStageIdx < stages.lastIndex) stages[activeStageIdx + 1].name else "Dismissal"
        
        // 5 minute warning logic (300 seconds), or 1 min warning (60 seconds) if length of block is too short
        val isShortBlock = activeStage.durationSeconds <= 300
        val warningThreshold = if (isShortBlock) 60 else 300
        val warningMinutes = if (isShortBlock) "one minute" else "five minutes"

        if (currentSecondsLeft == warningThreshold && warnedAboutTransitionForStage != activeStageIdx) {
            warnedAboutTransitionForStage = activeStageIdx
            
            // Execute the custom audio script layout requested:
            // "chime 1", "in 5 minutes, assessment", "chime 1"
            playSyntheticChime(1)
            speak("Incoming lesson part in $warningMinutes: $nextStageName")
            
            // Fire the second chime alert slightly delayed to clear speech buffer
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                delay(2800)
                playSyntheticChime(1)
            }
        }
    }

    // UI Layout Render
    Surface(
        color = Color(0xFF090D1F), // Deep space navy base
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Space header cockpit bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "ACTIVE PACING PROMPTER" else "STANDBY PAUSED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = { onClose() },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(0.05f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit prompter", tint = Color.White)
                }
            }

            // Quick overview panel
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161F38)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF818CF8).copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Metadata", tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(text = plan.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "Stage Target: ${plan.gradeLevel} (${plan.specificGradeLevel}) • L: ${plan.language} • Total Block Mins: ${plan.durationMins}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main stage pipeline stepper cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stages.forEachIndexed { idx, stg ->
                    val isCurrent = idx == activeStageIdx
                    val isPast = idx < activeStageIdx
                    val barColor = if (isCurrent) stg.color else if (isPast) Color(0xFF10B981) else Color.White.copy(0.08f)
                    val alpha = if (isCurrent) 1f else if (isPast) 0.6f else 0.25f

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .border(
                                1.dp,
                                if (isCurrent) stg.color.copy(0.4f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                activeStageIdx = idx
                                currentSecondsLeft = stg.durationSeconds
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stg.name.split(" ")[0], // Display I, L, A, W
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .fillMaxWidth(0.7f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BIG COCKPIT DIGITAL COUNTDOWN CLOCK CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1024)),
                border = BorderStroke(1.dp, activeStage.color.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = activeStage.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = activeStage.color,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Neon glowing time counter
                    Text(
                        text = formatTime(currentSecondsLeft),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White,
                        modifier = Modifier.animateContentSize()
                    )

                    Text(
                        text = "${formatMinutesAndSeconds(currentSecondsLeft)} of ${activeStage.durationSeconds / 60}m Allocated",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable bottom panel container containing Procedures card and Switcher (for Actions or Quiz Server)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Switcher Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(4.dp)
                ) {
                    listOf("📋 SUGGESTED ACTIONS", "📡 COHORT SERVER").forEachIndexed { idx, label ->
                        val isActive = bottomActiveTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) activeStage.color.copy(0.12f) else Color.Transparent)
                                .clickable { bottomActiveTab = idx }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) activeStage.color else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                if (bottomActiveTab == 0) {
                    // SUGGESTED CLASSROOM ACTIONS & STARTERS CARD
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, activeStage.color.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEAB308))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SUGGESTED ACTIONS & DISCUSSION STARTERS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEAB308),
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val actionableItems = when {
                                activeStage.name.contains("I - INTENTIONS", ignoreCase = true) -> listOf(
                                    "🏫 Action Starter" to "Write the day's intent/competency clearly at the top-left section of the blackboard for maximum student visibility.",
                                    "🗣️ Hook Prompt" to "Pose: 'Suguhi inyo kabataan...' (Open with a local Masbate story or a traditional riddle to hook the learners' active interest).",
                                    "🎯 Concept Connect" to "Connect the day's targets to prior agricultural or community knowledge adapted for Key Stage level needs."
                                )
                                activeStage.name.contains("L - LEARNING EXPERIENCES", ignoreCase = true) -> listOf(
                                    "🏫 Action Starter" to "Organize students into traditional small buzz groups ('Hurog-hurog') or story circles using responsive table seat configurations.",
                                    "🗣️ Hook Prompt" to "Ask: 'Based on this story we read, who can share how they have seen this theme applied in their own homes?'",
                                    "🎯 Concept Connect" to "Conduct the primary storytelling or demonstration with expressive language, referencing local cultural motifs."
                                )
                                activeStage.name.contains("A - ASSESSMENT", ignoreCase = true) -> listOf(
                                    "🏫 Action Starter" to "Distribute peer assessment worksheets or checklist rubrics designed for the key competency tracking rules.",
                                    "🗣️ Hook Prompt" to "Say: 'Let us exchange checkbooks with our seatmates and review each other's work with constructive, encouraging remarks.'",
                                    "🎯 Concept Connect" to "Apply the standard 75% transitional grading floor on raw scoring registers to support learning equity."
                                )
                                else -> listOf(
                                    "🏫 Action Starter" to "Initiate a 3-minute quiet reflection and breathing sequence to calm and ground the classroom ('Hinga' protocol).",
                                    "🗣️ Hook Prompt" to "Ask: 'What single word or local phrase describes what we successfully mastered together in class today?'",
                                    "🎯 Concept Connect" to "Distribute homework packets to students requiring extra support, and register remedial parameters."
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                actionableItems.forEach { (category, detail) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF131A33).copy(alpha = 0.5f))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(activeStage.color.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = category.uppercase(Locale.getDefault()),
                                                color = activeStage.color,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp
                                            )
                                        }
                                        Text(
                                            text = detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(0.9f),
                                            lineHeight = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // CLASSROOM QUIZ SERVER & COHORT INTERFACE!
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, activeStage.color.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Section Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("📡", fontSize = 16.sp)
                                    Text(
                                        text = "LOCAL WI-FI COHORT DIRECT TELEMETRY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = activeStage.color,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (serverActive) Color(0xFFEF4444).copy(0.12f) else Color(0xFF818CF8).copy(0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (serverActive) "ONLINE & BROADCASTING" else "STANDBY OFFLINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (serverActive) Color(0xFFFCA5A5) else Color(0xFF818CF8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (!serverActive) {
                                // Design server configuration options
                                Text(
                                    text = "To test standard competency, launch the configured formative assessment pool on the classroom local Direct channels.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Visual network configuration metrics
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Card(
                                        onClick = { viewModel.serverJoinMethod.value = "QR" },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (joinMethod == "QR") Color(0xFF1E1B4B) else Color(0xFF1E293B).copy(0.3f)
                                        ),
                                        border = BorderStroke(1.dp, if (joinMethod == "QR") Color(0xFF6366F1) else Color.White.copy(0.04f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("📱", fontSize = 18.sp)
                                            Text("QR CAPTURE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }

                                    Card(
                                        onClick = { viewModel.serverJoinMethod.value = "Manual" },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (joinMethod == "Manual") Color(0xFF1E1B4B) else Color(0xFF1E293B).copy(0.3f)
                                        ),
                                        border = BorderStroke(1.dp, if (joinMethod == "Manual") Color(0xFF6366F1) else Color.White.copy(0.04f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("⚙️", fontSize = 18.sp)
                                            Text("WIFI KEY DIRECT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Render standard direct visuals depending on network join selection
                                if (joinMethod == "QR") {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(110.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White)
                                                .padding(6.dp)
                                        ) {
                                            val qrBitmap = remember(localIp) {
                                                generateQrCodeBitmap("http://$localIp:8080/join", 256)
                                            }
                                            if (qrBitmap != null) {
                                                Image(
                                                    bitmap = qrBitmap.asImageBitmap(),
                                                    contentDescription = "Real Connection QR Code",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val squareSize = size.width / 9f
                                                    val finderLocs = listOf(
                                                        Pair(0f, 0f),
                                                        Pair(6f * squareSize, 0f),
                                                        Pair(0f, 6f * squareSize)
                                                    )
                                                    finderLocs.forEach { (xOff, yOff) ->
                                                        drawRect(
                                                            color = Color.Black,
                                                            topLeft = androidx.compose.ui.geometry.Offset(xOff, yOff),
                                                            size = androidx.compose.ui.geometry.Size(3f * squareSize, 3f * squareSize)
                                                        )
                                                        drawRect(
                                                            color = Color.White,
                                                            topLeft = androidx.compose.ui.geometry.Offset(xOff + 0.5f * squareSize, yOff + 0.5f * squareSize),
                                                            size = androidx.compose.ui.geometry.Size(2f * squareSize, 2f * squareSize)
                                                        )
                                                        drawRect(
                                                            color = Color.Black,
                                                            topLeft = androidx.compose.ui.geometry.Offset(xOff + 0.8f * squareSize, yOff + 0.8f * squareSize),
                                                            size = androidx.compose.ui.geometry.Size(1.4f * squareSize, 1.4f * squareSize)
                                                        )
                                                    }
                                                    val dataPoints = listOf(
                                                        Pair(4f, 1f), Pair(5f, 1f), Pair(4f, 2f), Pair(5f, 3f),
                                                        Pair(1f, 4f), Pair(2f, 4f), Pair(3f, 4f), Pair(5f, 4f),
                                                        Pair(1f, 5f), Pair(3f, 5f), Pair(4f, 5f), Pair(8f, 5f)
                                                    )
                                                    dataPoints.forEach { (gridX, gridY) ->
                                                        drawRect(
                                                            color = Color.Black,
                                                            topLeft = androidx.compose.ui.geometry.Offset(gridX * squareSize, gridY * squareSize),
                                                            size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val ssid = remember(context) { getCurrentWifiSsid(context) }
                                        Text("SSID: $ssid", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1E293B).copy(0.4f))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("HOST: http://$localIp:8080", style = MaterialTheme.typography.labelSmall, color = Color.White, fontFamily = FontFamily.Monospace)
                                        Text("TOKEN IP: CLASS-PACE-$localIp", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34D399), fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val launchText = if (questionsPool.isNotEmpty() && activeIndex in questionsPool.indices) {
                                    "LAUNCH STAGE FORMATIVE ASSESSMENT: Q${activeIndex + 1}"
                                } else {
                                    "LAUNCH DEFAULT PACE INSTRUCTION COMPLEMENT"
                                }

                                Button(
                                    onClick = {
                                        viewModel.launchQuizFromServerPool()
                                        Toast.makeText(context, "COHORT SERVER BROADCAST ONLINE!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeStage.color),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Text(
                                        text = launchText,
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // ACTIVE BROADCAST FLOW Running
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("BROADCASTING INSTRUCTION TARGET:", style = MaterialTheme.typography.labelSmall, color = activeStage.color, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = qText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Answer Type: $qType | Correct Option: $correctAns",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Active quiz navigation inside the active server pool!
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (activeIndex > 0) {
                                                viewModel.selectActiveQuestionFromPool(activeIndex - 1)
                                            }
                                        },
                                        enabled = activeIndex > 0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1E293B),
                                            contentColor = Color.White,
                                            disabledContainerColor = Color(0xFF0F172A).copy(0.4f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("◀ PREV QUIZ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            if (activeIndex < questionsPool.size - 1) {
                                                viewModel.selectActiveQuestionFromPool(activeIndex + 1)
                                            }
                                        },
                                        enabled = activeIndex < questionsPool.size - 1,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1E293B),
                                            contentColor = Color.White,
                                            disabledContainerColor = Color(0xFF0F172A).copy(0.4f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("NEXT QUIZ ▶", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // SIMULATE DESKS and STOP & RECORD Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.simulateClassResponses()
                                            Toast.makeText(context, "Received telemetry packets from 30 desks!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("SIMULATE DESKS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.stopAndRecordSession()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("STOP & RECORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                // Interactive Student Response Seating Map Matrix
                                if (students.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "STUDENT DESK CONNECTIONS (ROSTER SEATING MATRIX):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Render seating roster cards using clean layouts
                                    val columns = 5
                                    val totalRows = (students.size + columns - 1) / columns
                                    for (r in 0 until totalRows) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            for (c in 0 until columns) {
                                                val sIdx = r * columns + c
                                                if (sIdx < students.size) {
                                                    val stud = students[sIdx]
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(
                                                                if (stud.hasAnswered) {
                                                                    if (stud.isCorrect) Color(0xFF10B981).copy(0.12f) else Color(0xFFEF4444).copy(0.12f)
                                                                } else {
                                                                    Color.White.copy(0.04f)
                                                                }
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (stud.hasAnswered) {
                                                                    if (stud.isCorrect) Color(0xFF10B981).copy(0.3f) else Color(0xFFEF4444).copy(0.3f)
                                                                } else Color.White.copy(0.05f),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .padding(4.dp)
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                                            Text(
                                                                text = stud.studentName.split(" ").firstOrNull() ?: stud.studentName,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                maxLines = 1
                                                            )
                                                            if (stud.hasAnswered) {
                                                                Text(
                                                                    text = stud.answer,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (stud.isCorrect) Color(0xFF34D399) else Color(0xFFFCA5A5)
                                                                )
                                                            } else {
                                                                Text(text = "STANDBY", style = MaterialTheme.typography.labelSmall, fontSize = 7.sp, color = Color(0xFF94A3B8))
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }

                                // DECISION ENGINE RESULTS with force override controls
                                val participated = students.count { it.hasAnswered }
                                val correctCount = students.count { it.isCorrect }
                                val accuracy = if (participated > 0) (correctCount * 100 / participated) else 0

                                Spacer(modifier = Modifier.height(14.dp))

                                var manualDecisionOverride by remember { mutableStateOf<String?>(null) }
                                val activeDecision = manualDecisionOverride ?: if (accuracy >= 75) "PROCEED" else "REMEDIATION"

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (activeDecision == "PROCEED") Color(0xFF065F46) else Color(0xFF991B1B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "ADLAW TELEMETRY RESULT DECISION:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(0.7f),
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (manualDecisionOverride != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color.White.copy(0.2f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("OVERRULED", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (activeDecision == "PROCEED") "🟢 CHANNELS MASTERED (Accuracy: $accuracy%) - PROCEED"
                                                   else "🔴 REMEDIATION REQUISITE (Accuracy: $accuracy%) - REMEDIATION PLAN",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (activeDecision == "PROCEED") "Standard transitional grading target met. The teacher is clear to proceed."
                                                   else "Attention: Standard 75% transitional mastery not met. Remediate or execute ADLAW diagnostic block.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.sp,
                                            color = Color.White.copy(0.9f)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = { manualDecisionOverride = "PROCEED" },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (activeDecision == "PROCEED") Color.White else Color.White.copy(0.12f),
                                                    contentColor = if (activeDecision == "PROCEED") Color.Black else Color.White
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f).height(28.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("OVERRIDE: PROCEED", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = { manualDecisionOverride = "REMEDIATION" },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (activeDecision == "REMEDIATION") Color.White else Color.White.copy(0.12f),
                                                    contentColor = if (activeDecision == "REMEDIATION") Color.Black else Color.White
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1.1f).height(28.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("OVERRIDE: REMEDIATE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (manualDecisionOverride != null) {
                                                Button(
                                                    onClick = { manualDecisionOverride = null },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                                    border = BorderStroke(1.dp, Color.White.copy(0.4f)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.weight(0.7f).height(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("AUTO", fontSize = 9.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // INTERACTIVE SESSION ENGINE CONTROL PANEL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Volume Toggle
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        Toast.makeText(context, if (isMuted) "Voice alerts silenced" else "Voice alerts enabled", Toast.LENGTH_SHORT).show()
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = if (isMuted) Color(0xFFEF4444).copy(0.12f) else Color.White.copy(0.04f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.Close else Icons.Default.Notifications,
                        contentDescription = "Mute audio pacing cues",
                        tint = if (isMuted) Color(0xFFEF4444) else Color(0xFF818CF8)
                    )
                }

                // Simulation speed acceleration multiplier
                Button(
                    onClick = {
                        clockMultiplier = if (clockMultiplier == 1) 60 else if (clockMultiplier == 60) 120 else 1
                        val toastText = when(clockMultiplier) {
                            60 -> "Fast simulation: 60x Speed (1s = 1m)"
                            120 -> "Hyper simulation: 120x Speed (1s = 2m)"
                            else -> "Standard 1x scale real-time clock"
                        }
                        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (clockMultiplier > 1) Color(0xFF10B981).copy(0.15f) else Color.White.copy(0.04f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "${clockMultiplier}X SPEED",
                        color = if (clockMultiplier > 1) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // PLAY/PAUSE BIG PRIMARY BUTTON (Aesthetic accent spacing)
                Button(
                    onClick = {
                        isRunning = !isRunning
                        if (isRunning) {
                            speak("Starting standard classroom prompter pipeline.")
                            playSyntheticChime(1)
                        } else {
                            speak("Prompter paused")
                        }
                    },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeStage.color)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Home else Icons.Default.PlayArrow, // Using Home or PlayArrow dynamically
                        contentDescription = "Play Pause clock trigger",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "PAUSE PROMPTER" else "START LESSON",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // SKIP NEXT BUTTON
                IconButton(
                    onClick = {
                        if (activeStageIdx < stages.lastIndex) {
                            activeStageIdx++
                            currentSecondsLeft = stages[activeStageIdx].durationSeconds
                        } else {
                            Toast.makeText(context, "Last section reached", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(0.04f))
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Skip Forward stage", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Manual simulation testing alerts sandbox bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AcouSTIC DEMO BOARD / TRIGGERS:",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TEST CHIME 1",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF818CF8).copy(0.12f))
                            .clickable { playSyntheticChime(1) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "TEST CHIME 2",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF34D399).copy(0.12f))
                            .clickable { playSyntheticChime(2) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "TEST 5M ALERT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFBBF24).copy(0.12f))
                            .clickable {
                                // Simulate exact transition warning format requested by developer:
                                // 'chime 1', 'in 5 minutes, assessment' , 'chime 1'
                                playSyntheticChime(1)
                                val nextStageTitle = if (activeStageIdx < stages.lastIndex) stages[activeStageIdx + 1].name else "Dismissal"
                                speak("In five minutes, $nextStageTitle")
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    delay(2800)
                                    playSyntheticChime(1)
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

data class PrompterStageItem(
    val name: String,
    val description: String,
    val durationSeconds: Int,
    val contentBody: String,
    val color: Color
)

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun formatMinutesAndSeconds(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins}m ${secs}s"
}
