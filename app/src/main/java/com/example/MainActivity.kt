package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.Curriculum
import com.example.data.LessonPlan
import com.example.data.SystemLog
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

object Color {
    operator fun invoke(value: Long): ComposeColor {
        return ComposeColor(value) // Standard unaltered dark mode Hex colors
    }

    operator fun invoke(colorValue: Int): ComposeColor = ComposeColor(colorValue)
    operator fun invoke(red: Float, green: Float, blue: Float, alpha: Float = 1f): ComposeColor = ComposeColor(red, green, blue, alpha)
    operator fun invoke(red: Int, green: Int, blue: Int, alpha: Int = 255): ComposeColor = ComposeColor(red, green, blue, alpha)

    val White: ComposeColor get() = ComposeColor.White
    val Black: ComposeColor get() = ComposeColor.Black
    val Transparent: ComposeColor get() = ComposeColor.Transparent
    val Red: ComposeColor get() = ComposeColor.Red
    val Green: ComposeColor get() = ComposeColor.Green
    val Blue: ComposeColor get() = ComposeColor.Blue
    val Yellow: ComposeColor get() = ComposeColor.Yellow
    val Cyan: ComposeColor get() = ComposeColor.Cyan
    val Magenta: ComposeColor get() = ComposeColor.Magenta
    val Gray: ComposeColor get() = ComposeColor.Gray
    val DarkGray: ComposeColor get() = ComposeColor.DarkGray
    val LightGray: ComposeColor get() = ComposeColor.LightGray
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    HypervisorScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    // Beautiful, eye-friendly, soft modern slate-indigo Dark Theme to prevent snowblinding
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF6366F1), // Modern Indigo Accent
        secondary = Color(0xFF10B981), // Emerald/Green
        tertiary = Color(0xFFF59E0B), // Amber/Orange
        background = Color(0xFF0F172A), // Main deep dark background
        surface = Color(0xFF1E293B), // Surface panels / cards
        onPrimary = Color(0xFFFFFFFF),
        onSecondary = Color(0xFFFFFFFF),
        onBackground = Color(0xFFF8FAFC), // High-contrast soft white body
        onSurface = Color(0xFFF8FAFC), // High-contrast soft white body
        surfaceVariant = Color(0xFF334155), // Medium slate borders
        onSurfaceVariant = Color(0xFF94A3B8) // Slate muted gray text
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}

@Composable
fun SubtleAccentDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        val width = size.width
        val height = size.height
        val segmentWidth = 24f
        var x = 0f
        
        // Muted, extremely subtle aesthetic local colors 
        val crimson = Color(0xFFDC2626).copy(alpha = 0.06f) 
        val gold = Color(0xFFF59E0B).copy(alpha = 0.06f)    
        val teal = Color(0xFF14B8A6).copy(alpha = 0.06f)    
        val blue = Color(0xFF3B82F6).copy(alpha = 0.06f)    
        
        val colors = listOf(crimson, gold, teal, blue)
        var i = 0

        while (x < width) {
            val step = segmentWidth
            val c1 = colors[i % colors.size]
            val c2 = colors[(i + 1) % colors.size]
            
            // Draw a subtle nested modern geometric line segment
            val p1 = Path().apply {
                moveTo(x, 0f)
                lineTo(x + step / 2f, height)
                lineTo(x + step, 0f)
                lineTo(x + step - 2f, 0f)
                lineTo(x + step / 2f, height - 0.5f)
                lineTo(x + 2f, 0f)
                close()
            }
            drawPath(path = p1, color = c1)

            val p2 = Path().apply {
                moveTo(x, height)
                lineTo(x + step / 2f, 0f)
                lineTo(x + step, height)
                lineTo(x + step - 2f, height)
                lineTo(x + step / 2f, 0.5f)
                lineTo(x + 2f, height)
                close()
            }
            drawPath(path = p2, color = c2)

            x += step
            i++
        }
    }
}

@Composable
fun HypervisorScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val curriculumItems by viewModel.curriculumItems.collectAsStateWithLifecycle()
    val lessonPlans by viewModel.lessonPlans.collectAsStateWithLifecycle()
    val systemLogs by viewModel.systemLogs.collectAsStateWithLifecycle()

    val currentTermVal by viewModel.currentTerm.collectAsStateWithLifecycle()
    val currentWeekVal by viewModel.currentWeek.collectAsStateWithLifecycle()
    val eieLevelVal by viewModel.eieLevel.collectAsStateWithLifecycle()
    val lockdownActiveVal by viewModel.lockdownActive.collectAsStateWithLifecycle()

    val isAralRemediationRequired by viewModel.isAralRemediationRequired.collectAsStateWithLifecycle()

    val weatherTempShow by viewModel.weatherTemp.collectAsStateWithLifecycle()
    val weatherCondShow by viewModel.weatherCondition.collectAsStateWithLifecycle()
    val localNewsShow by viewModel.localNews.collectAsStateWithLifecycle()
    
    val fontScaleMultiplierVal by viewModel.fontScaleMultiplier.collectAsStateWithLifecycle()

    var activeTabIdx by remember { mutableStateOf(0) }
    var activePrompterPlan by remember { mutableStateOf<com.example.data.LessonPlan?>(null) }

    if (activePrompterPlan != null) {
        com.example.ui.LessonPrompterScreen(
            plan = activePrompterPlan!!,
            viewModel = viewModel,
            onClose = { activePrompterPlan = null }
        )
        return
    }

    val threatColor = when (eieLevelVal) {
        "Hayo" -> Color(0xFF10B981)   // Normal Green
        "Hinay" -> Color(0xFFF59E0B)  // Moderate Caution amber
        "Hinga" -> Color(0xFF3B82F6)  // Module/Home Blue
        "Hinto" -> Color(0xFFEF4444)  // Crisis red
        else -> Color(0xFF10B981)
    }

    val originalDensity = androidx.compose.ui.platform.LocalDensity.current
    val customDensity = androidx.compose.ui.unit.Density(
        density = originalDensity.density,
        fontScale = originalDensity.fontScale * fontScaleMultiplierVal
    )

    CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides customDensity) {
        Column(
            modifier = modifier
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
        // --- 1. Top OS Header Branding Block (Sophisticated and Clean Professional light styled) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 20.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HYPERVISOR PORTAL 01",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(threatColor)
                        )
                        Text(
                            text = "SAFETY PROTOCOL: $eieLevelVal".uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = threatColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "BASKOG ",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "OS",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                }

                Text(
                    text = "BASKOG Localized Educational Engine • DepEd TTY Compliant",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // --- 2. Live Telemetry & News Ticker Block ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Alert logo",
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "REGIONAL ADVISORIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF818CF8),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = localNewsShow,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Atmosphere: $weatherTempShow [$weatherCondShow] • Sensors synchronized",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Normal
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.refreshNewsAndTelemetry() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh variables icon",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        SubtleAccentDivider()

        // --- 3. Lockdown Alarm Warning overlay ---
        AnimatedVisibility(
            visible = lockdownActiveVal,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEF4444))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Lockdown Alert",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CRITICAL LOCKDOWN: ALL IN-PERSON LESSON PREPARATION RESTRICTED.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- 4. Policy Framework Remediation Ribbon (ARAL Warning) ---
        AnimatedVisibility(
            visible = isAralRemediationRequired,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF59E0B).copy(alpha = 0.9f))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ARAL REMEDIATION TRIGGER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WEEK $currentWeekVal OF $currentTermVal BLOCK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Under DepEd TTY policy, Week 5+ of Instructional Term mandates intensive learner catch-up protocols. Ensure 'Ways Forward' sections include remedial scaffolding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. Custom Console Tabs ---
        ScrollableTabRow(
            selectedTabIndex = activeTabIdx,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            val tabLabels = listOf("DASHBOARD", "BUDGET OF WORK", "ILAW BUILDER", "PLANS CATALOG", "CLASS SERVER", "CLASS RECORD", "ATTENDANCE", "SYSTEM CONSOLE")
            tabLabels.forEachIndexed { idx, label ->
                Tab(
                    selected = activeTabIdx == idx,
                    onClick = { activeTabIdx = idx },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (activeTabIdx == idx) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 6. Primary Content Switcher ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (activeTabIdx) {
                0 -> DashboardTab(viewModel, curriculumItems, lessonPlans, onNavigateToTab = { activeTabIdx = it })
                1 -> CurriculumParserTab(viewModel, curriculumItems)
                2 -> IlawCompilerTab(viewModel, curriculumItems, eieLevelVal)
                3 -> PlansCatalogTab(viewModel, lessonPlans, onStartPrompter = { activePrompterPlan = it })
                4 -> ClassServerTab(viewModel)
                5 -> ClassRecordTab(viewModel)
                6 -> AttendanceTab(viewModel)
                7 -> SystemConsoleTab(viewModel, systemLogs, curriculumItems, lessonPlans)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- 7. Static Branding Console Footer ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF070A13))
                .border(1.dp, Color(0xFF1E293B))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BASKOG OPERATIONAL COGNITIVE HYPERVISOR • SECURE CORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aligned with Philippines DepEd Three-Term Year Regulations & Learning Continuity Frameworks",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF334155)
                )
            }
        }
    }
}
}

// ==========================================
// TAB 1: CURRICULUM MELC & BOW PARSER
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurriculumParserTab(viewModel: MainViewModel, curriculumList: List<Curriculum>) {
    val statusMessage by viewModel.parsingStatusMessage.collectAsStateWithLifecycle()
    val isParsing by viewModel.isParsing.collectAsStateWithLifecycle()
    val completedIds by viewModel.completedCurriculumIds.collectAsStateWithLifecycle()

    var itemToDeleteId by remember { mutableStateOf<Int?>(null) }
    var showWipeCurriculumConfirm by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var selectedTermFilter by remember { mutableStateOf("All") }

    var manualGrade by remember { mutableStateOf("KS2") }
    var manualSubject by remember { mutableStateOf("Araling Panlipunan") }
    var manualTerm by remember { mutableStateOf("1st Term") }
    var manualWeek by remember { mutableStateOf("1") }
    var manualMelcCode by remember { mutableStateOf("") }
    var manualSessions by remember { mutableStateOf("4") }
    var manualContentStandard by remember { mutableStateOf("") }
    var manualPerformanceStandard by remember { mutableStateOf("") }
    var manualCompetency by remember { mutableStateOf("") }

    var showManualForm by remember { mutableStateOf(false) }
    var expandedItemIndex by remember { mutableStateOf(-1) }

    // Filtrations
    val filteredList = curriculumList.filter { item ->
        val queryMatches = searchQuery.isBlank() ||
                item.learningCompetency.contains(searchQuery, ignoreCase = true) ||
                item.contentStandard.contains(searchQuery, ignoreCase = true) ||
                item.performanceStandard.contains(searchQuery, ignoreCase = true) ||
                item.melcCode.contains(searchQuery, ignoreCase = true)
        
        val subjectMatches = selectedSubjectFilter == "All" || item.subject.equals(selectedSubjectFilter, ignoreCase = true)
        val termMatches = selectedTermFilter == "All" || item.term.equals(selectedTermFilter, ignoreCase = true)
        
        queryMatches && subjectMatches && termMatches
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        
        if (itemToDeleteId != null) {
            AlertDialog(
                onDismissRequest = { itemToDeleteId = null },
                title = { Text("Confirm Deletion", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Are you sure you want to permanently delete this curriculum item from the local TALA registry?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            itemToDeleteId?.let { viewModel.deleteCurriculumItem(it) }
                            itemToDeleteId = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDeleteId = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        if (showWipeCurriculumConfirm) {
            AlertDialog(
                onDismissRequest = { showWipeCurriculumConfirm = false },
                title = { Text("Confirm Wiping TALA Database", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Are you sure you want to permanently clear the entire local TALA curriculum repository?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearCurriculumItems()
                            showWipeCurriculumConfirm = false
                        }
                    ) {
                        Text("Wipe All", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeCurriculumConfirm = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        // VISUAL CURRICULUM COMPLETION LEDGER CARD
        val lessonPlans by viewModel.lessonPlans.collectAsStateWithLifecycle()
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "VISUAL CURRICULUM COMPLETION LEDGER",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Real-time TALA curriculum alignment tracker",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Calculate progress
                val totalMELCs = curriculumList.size
                val completedMELCs = curriculumList.filter { melc ->
                    completedIds.contains(melc.id) || lessonPlans.any { plan ->
                        plan.subject.equals(melc.subject, ignoreCase = true) &&
                        plan.term.equals(melc.term, ignoreCase = true) &&
                        plan.week == melc.week
                    }
                }
                val percent = if (totalMELCs > 0) (completedMELCs.size.toFloat() / totalMELCs * 100).toInt() else 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$percent% COMPLETED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${completedMELCs.size} of $totalMELCs MELCs",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Linear progress indicator
                        LinearProgressIndicator(
                            progress = { if (totalMELCs > 0) completedMELCs.size.toFloat() / totalMELCs else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Subject Theme Breakdowns:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Breakdown by major subjects
                val subjects = listOf("Araling Panlipunan", "Science", "EPP/TLE")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.forEach { subj ->
                        val totalForSubj = curriculumList.count { it.subject.equals(subj, ignoreCase = true) }
                        val completedForSubj = curriculumList.filter { it.subject.equals(subj, ignoreCase = true) }.count { melc ->
                            lessonPlans.any { plan ->
                                plan.subject.equals(melc.subject, ignoreCase = true) &&
                                plan.term.equals(melc.term, ignoreCase = true) &&
                                plan.week == melc.week
                            }
                        }
                        val subjPercent = if (totalForSubj > 0) (completedForSubj.toFloat() / totalForSubj * 100).toInt() else 0

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = subj,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$completedForSubj / $totalForSubj ($subjPercent%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. GORGEOUS DATABASE STATUS CARD & QUICK CONSOLE
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "TALA COURSEWARE METADATA & BOW",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Browse, filter and index the built-in DepEd Grade 5 three-term learning continuities.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Quick stats pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f), RoundedCornerShape(32.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${filteredList.size} lessons matched",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Action section to seed or reload system database
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = "Database Seed Administration",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Current database contains ${curriculumList.size} active records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isParsing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Button(
                            onClick = { viewModel.resetToBuiltInCurriculum() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload built-in BOW", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RE-INDEX STANDARD BOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 2. SEARCH & DYNAMIC FILTERING CONTROLS CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // Real-time Text Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search competency, rocks, nasyonalismo, internet...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                    prefix = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp).padding(end = 4.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    textStyle = MaterialTheme.typography.bodySmall
                )

                // Subject filter pills
                Column {
                    Text("Filter by Subject Theme:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Araling Panlipunan", "Science", "EPP/TLE").forEach { subject ->
                            val isSelected = selectedSubjectFilter == subject
                            Box(
                                modifier = Modifier
                                    .clickable { selectedSubjectFilter = subject }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = subject,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Term filter pills
                Column {
                    Text("Filter by Term (Three-Term Calendar):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "1st Term", "2nd Term", "3rd Term").forEach { term ->
                            val isSelected = selectedTermFilter == term
                            val label = when (term) {
                                "1st Term" -> "1st Term"
                                "2nd Term" -> "2nd Term"
                                "3rd Term" -> "3rd Term"
                                else -> "Show All"
                            }
                            Box(
                                modifier = Modifier
                                    .clickable { selectedTermFilter = term }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. EXPANDABLE COLLAPSIBLE ACTIVE LISTING
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B2E)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BUILT-IN COURSEWARE SCHEMATIC INDEX",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF818CF8),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (curriculumList.isNotEmpty()) {
                        TextButton(
                            onClick = { showWipeCurriculumConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear database", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WIPE T Tala", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                if (curriculumList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color.White.copy(0.04f), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Warning, contentDescription = "Vacancy warning", tint = Color(0xFFFBBF24), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Curriculum alignment database is vacant.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Click 'RE-INDEX STANDARD BOW' at the top index card to instantly reload Grade 5 Filipino Courseware.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 14.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No courseware entries match your current search queries or subject filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    filteredList.forEachIndexed { index, curr ->
                        val isExpanded = expandedItemIndex == index
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color.White.copy(alpha = if (isExpanded) 0.15f else 0.03f), RoundedCornerShape(14.dp))
                                .clickable { expandedItemIndex = if (isExpanded) -1 else index }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        when (curr.subject) {
                                                            "Araling Panlipunan" -> Color(0xFFF59E0B).copy(0.2f)
                                                            "Science" -> Color(0xFF10B981).copy(0.2f)
                                                            "EPP/TLE" -> Color(0xFF3B82F6).copy(0.2f)
                                                            else -> Color(0xFF6366F1).copy(0.2f)
                                                        }
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = curr.subject,
                                                    color = when (curr.subject) {
                                                        "Araling Panlipunan" -> Color(0xFFF59E0B)
                                                        "Science" -> Color(0xFF34D399)
                                                        "EPP/TLE" -> Color(0xFF60A5FA)
                                                        else -> Color(0xFF818CF8)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            
                                            Text(
                                                text = "Term: ${curr.term} • Week ${curr.week}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            text = "BOW: ${curr.sessionsBudgeted} Class Sessions Allocated",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (curr.melcCode.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF6366F1).copy(0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(curr.melcCode, color = Color(0xFF818CF8), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        
                                        IconButton(
                                            onClick = { itemToDeleteId = curr.id },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete record", tint = Color(0xFFEF4444).copy(0.7f), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = curr.learningCompetency,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color.White.copy(0.06f))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Text("CONTENT STANDARD:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                    Text(curr.contentStandard, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE2E8F0))
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Text("PERFORMANCE STANDARD:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                    Text(curr.performanceStandard, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE2E8F0))
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Direct trigger bridge to pre-configure Compiler parameters instantly!
                                    Button(
                                        onClick = {
                                            viewModel.planSubject.value = curr.subject
                                            viewModel.planGradeLevel.value = curr.gradeLevel
                                            viewModel.currentTerm.value = curr.term
                                            viewModel.currentWeek.value = curr.week
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Build, contentDescription = "Load standard into ILAW compiler", modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("LOAD DIRECTLY INTO ILAW BUILDER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. MANUAL KABAN ACCORDION INSERTION (AT THE BOTTOM FOR EXPERT USERS)
        Row(
            modifier = Modifier
                .clickable { showManualForm = !showManualForm }
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showManualForm) "Hide Manual Entry Form" else "Show Manual Entry Form (Advanced)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF818CF8),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (showManualForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand toggle",
                tint = Color(0xFF818CF8),
                modifier = Modifier.size(16.dp)
            )
        }

        if (showManualForm) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MANUAL KABAN ENTRY FORM",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF818CF8),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Input localized variables directly to generate singular compatible alignment records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grade Target: ", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        listOf("KS1", "KS2", "KS3", "KS4").forEach { level ->
                            val isSel = manualGrade == level
                            Box(
                                modifier = Modifier
                                    .clickable { manualGrade = level }
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Color(0xFF4F46E5) else Color(0xFF0F172A))
                                    .border(1.dp, if (isSel) Color.White.copy(0.2f) else Color.Transparent, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(level, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = manualSubject,
                            onValueChange = { manualSubject = it },
                            label = { Text("Subject", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF818CF8), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1.5f),
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = manualMelcCode,
                            onValueChange = { manualMelcCode = it },
                            label = { Text("MELC Code", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF818CF8), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            placeholder = { Text("e.g. M3NS-Ie-1", color = Color(0xFF475569)) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = manualTerm,
                            onValueChange = { manualTerm = it },
                            label = { Text("Term Block", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF818CF8), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1.2f),
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = manualWeek,
                            onValueChange = { manualWeek = it },
                            label = { Text("Week Target", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF818CF8), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(0.8f),
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = manualSessions,
                            onValueChange = { manualSessions = it },
                            label = { Text("Sessions/BOW", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF818CF8), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(0.8f),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = manualContentStandard,
                        onValueChange = { manualContentStandard = it },
                        label = { Text("Content Standard Description", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF818CF8), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualPerformanceStandard,
                        onValueChange = { manualPerformanceStandard = it },
                        label = { Text("Performance Standard Description", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF818CF8), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualCompetency,
                        onValueChange = { manualCompetency = it },
                        label = { Text("Most Essential Learning Competency (MELC)", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF818CF8), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val parsedWeek = manualWeek.toIntOrNull() ?: 5
                            val parsedSessions = manualSessions.toIntOrNull() ?: 5
                            viewModel.insertCustomCurriculum(
                                gradeLevel = manualGrade,
                                subject = manualSubject,
                                content = manualContentStandard,
                                performance = manualPerformanceStandard,
                                competency = manualCompetency,
                                term = manualTerm,
                                week = parsedWeek,
                                melcCode = manualMelcCode,
                                sessionsBudgeted = parsedSessions
                            )
                            manualContentStandard = ""
                            manualPerformanceStandard = ""
                            manualCompetency = ""
                        },
                        enabled = manualSubject.isNotBlank() && manualContentStandard.isNotBlank() && manualCompetency.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add manual item")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ADD COMPATIBLE MATRIX RECORD", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: ILAW BLUEPRINT COMPILER (GENERATION)
// ==========================================
@Composable
fun CustomDropdownSelector(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun IlawCompilerTab(viewModel: MainViewModel, curriculumList: List<Curriculum>, activeEie: String) {
    val planSubject by viewModel.planSubject.collectAsStateWithLifecycle()
    val planGradeLevel by viewModel.planGradeLevel.collectAsStateWithLifecycle()
    val planSpecificGradeLevel by viewModel.planSpecificGradeLevel.collectAsStateWithLifecycle()
    val planLanguage by viewModel.planLanguage.collectAsStateWithLifecycle()
    val planPpstChecklist by viewModel.planPpstChecklist.collectAsStateWithLifecycle()
    val planStrategy by viewModel.planTeachingStrategy.collectAsStateWithLifecycle()
    val planDuration by viewModel.planDurationMins.collectAsStateWithLifecycle()
    val planPrompt by viewModel.planCustomPrompt.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isPlanGenerating.collectAsStateWithLifecycle()
    val activeJson by viewModel.activeLessonPlanJson.collectAsStateWithLifecycle()

    val currentTermVal by viewModel.currentTerm.collectAsStateWithLifecycle()
    val currentWeekVal by viewModel.currentWeek.collectAsStateWithLifecycle()

    // Professional proficiency & scheduling states
    val teacherProficiency by viewModel.teacherProficiencyLevel.collectAsStateWithLifecycle()
    val deliveryDate by viewModel.lessonDeliveryDate.collectAsStateWithLifecycle()
    val sectionName by viewModel.lessonSection.collectAsStateWithLifecycle()
    val selectedPeriods by viewModel.lessonPeriods.collectAsStateWithLifecycle()

    val planContentStandardText by viewModel.planContentStandardText.collectAsStateWithLifecycle()
    val planLearningCompetencyText by viewModel.planLearningCompetencyText.collectAsStateWithLifecycle()

    val currentCurriculumList by rememberUpdatedState(curriculumList)
    LaunchedEffect(planGradeLevel, planSubject, currentTermVal, currentWeekVal) {
        val curr = currentCurriculumList.find {
            it.gradeLevel == planGradeLevel &&
            it.subject.equals(planSubject, ignoreCase = true) &&
            it.term.equals(currentTermVal, ignoreCase = true) &&
            it.week == currentWeekVal
        }
        if (curr != null) {
            viewModel.planContentStandardText.value = curr.contentStandard
            viewModel.planLearningCompetencyText.value = curr.learningCompetency
        } else {
            val fallback = currentCurriculumList.find {
                it.gradeLevel == planGradeLevel && it.subject.equals(planSubject, ignoreCase = true)
            }
            if (fallback != null) {
                viewModel.planContentStandardText.value = fallback.contentStandard
                viewModel.planLearningCompetencyText.value = fallback.learningCompetency
            } else {
                viewModel.planContentStandardText.value = "Demonstrate core knowledge skills aligned to Masbate localized context."
                viewModel.planLearningCompetencyText.value = "Apply core learnings effectively under constructive evaluation guidance."
            }
        }
    }

    val context = LocalContext.current
    var durationText by remember { mutableStateOf(planDuration.toString()) }

    LaunchedEffect(planDuration) {
        if (planDuration.toString() != durationText) {
            durationText = planDuration.toString()
        }
    }

    // Discover active mapping from KABAN Database
    val matchedCurriculum = curriculumList.find {
        it.gradeLevel == planGradeLevel && 
        it.subject.equals(planSubject, ignoreCase = true) &&
        it.term.equals(currentTermVal, ignoreCase = true) &&
        it.week == currentWeekVal
    } ?: curriculumList.find {
        it.gradeLevel == planGradeLevel && it.subject.equals(planSubject, ignoreCase = true)
    }

    val ppstOptions = listOf(
        "1.1.1: Content knowledge & pedagogy",
        "1.4.1: Use of Mother Tongue/Language",
        "2.1.1: Learner safety and security",
        "3.1.1: Learners with diverse backgrounds",
        "4.1.1: Planning of learning process",
        "5.1.1: Design & selection of assessment"
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ILAW SYSTEM-LEVEL PLAN INTEGRATION",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF818CF8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure parameters to instruct the compiler. Active curriculum standards, planning indicators, and school calendar blocks are automatically compiled and validated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                 // --- CHRONOLOGICAL THREE-TERM CALENDAR SELECTORS ---
                 Text("Chronological Calendar Targets (DepEd Three-Term Block):", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                 Spacer(modifier = Modifier.height(4.dp))
                 
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                 ) {
                     // 1. Term Dropdown Selector
                     var showTermMenu by remember { mutableStateOf(false) }
                     Box(modifier = Modifier.weight(1.3f)) {
                         OutlinedTextField(
                             value = currentTermVal,
                             onValueChange = { },
                             readOnly = true,
                             label = { Text("Block/Term", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                             colors = OutlinedTextFieldDefaults.colors(
                                 focusedBorderColor = Color(0xFF818CF8),
                                 focusedTextColor = Color.White,
                                 unfocusedTextColor = Color.White
                             ),
                             trailingIcon = {
                                 IconButton(onClick = { showTermMenu = true }) {
                                     Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose Term Block", tint = Color.White)
                                 }
                             },
                             modifier = Modifier.fillMaxWidth(),
                             textStyle = MaterialTheme.typography.bodySmall
                         )
                         DropdownMenu(
                             expanded = showTermMenu,
                             onDismissRequest = { showTermMenu = false },
                             modifier = Modifier.background(Color(0xFF1E293B))
                         ) {
                             listOf("1st Term", "2nd Term", "3rd Term").forEach { t ->
                                 DropdownMenuItem(
                                     text = { Text(t, color = Color.White, style = MaterialTheme.typography.bodySmall) },
                                     onClick = {
                                         viewModel.currentTerm.value = t
                                         showTermMenu = false
                                     }
                                 )
                             }
                         }
                     }

                     // 2. Week Dropdown Selector
                     var showWeekMenu by remember { mutableStateOf(false) }
                     Box(modifier = Modifier.weight(1f)) {
                         OutlinedTextField(
                             value = "Week $currentWeekVal",
                             onValueChange = { },
                             readOnly = true,
                             label = { Text("Week Number", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                             colors = OutlinedTextFieldDefaults.colors(
                                 focusedBorderColor = Color(0xFF818CF8),
                                 focusedTextColor = Color.White,
                                 unfocusedTextColor = Color.White
                             ),
                             trailingIcon = {
                                 IconButton(onClick = { showWeekMenu = true }) {
                                     Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose Week Index", tint = Color.White)
                                 }
                             },
                             modifier = Modifier.fillMaxWidth(),
                             textStyle = MaterialTheme.typography.bodySmall
                         )
                         DropdownMenu(
                             expanded = showWeekMenu,
                             onDismissRequest = { showWeekMenu = false },
                             modifier = Modifier.background(Color(0xFF1E293B))
                         ) {
                             (1..10).forEach { w ->
                                 DropdownMenuItem(
                                     text = { Text("Week $w", color = Color.White, style = MaterialTheme.typography.bodySmall) },
                                     onClick = {
                                         viewModel.currentWeek.value = w
                                         showWeekMenu = false
                                     }
                                 )
                             }
                         }
                     }

                     // 3. Day Number Selector (1 to 5)
                     var showDayNumMenu by remember { mutableStateOf(false) }
                     val dayNumDisplay = when (deliveryDate) {
                         "Monday" -> "Day 1 (Mon)"
                         "Tuesday" -> "Day 2 (Tue)"
                         "Wednesday" -> "Day 3 (Wed)"
                         "Thursday" -> "Day 4 (Thu)"
                         "Friday" -> "Day 5 (Fri)"
                         else -> "Day 1 (Mon)"
                     }
                     Box(modifier = Modifier.weight(1.1f)) {
                         OutlinedTextField(
                             value = dayNumDisplay,
                             onValueChange = { },
                             readOnly = true,
                             label = { Text("Day Plan", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                             colors = OutlinedTextFieldDefaults.colors(
                                 focusedBorderColor = Color(0xFF818CF8),
                                 focusedTextColor = Color.White,
                                 unfocusedTextColor = Color.White
                             ),
                             trailingIcon = {
                                 IconButton(onClick = { showDayNumMenu = true }) {
                                     Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose Day Number", tint = Color.White)
                                 }
                             },
                             modifier = Modifier.fillMaxWidth(),
                             textStyle = MaterialTheme.typography.bodySmall
                         )
                         DropdownMenu(
                             expanded = showDayNumMenu,
                             onDismissRequest = { showDayNumMenu = false },
                             modifier = Modifier.background(Color(0xFF1E293B))
                         ) {
                             (1..5).forEach { d ->
                                 val dayName = when (d) {
                                     1 -> "Monday"
                                     2 -> "Tuesday"
                                     3 -> "Wednesday"
                                     4 -> "Thursday"
                                     5 -> "Friday"
                                     else -> "Monday"
                                 }
                                 DropdownMenuItem(
                                     text = { Text("Day $d ($dayName)", color = Color.White, style = MaterialTheme.typography.bodySmall) },
                                     onClick = {
                                         viewModel.lessonDeliveryDate.value = dayName
                                         showDayNumMenu = false
                                     }
                                 )
                             }
                         }
                     }
                 }

                 Spacer(modifier = Modifier.height(16.dp))

                 // --- KABAN DROPDOWN FOR SUBJECT ---
                  // --- MAPPED LESSON FROM CORE INSTRUCTIONAL BLOCK ---
                  val activeTermClean = currentTermVal.lowercase()
                  val alignedStd = curriculumList.find {
                      it.gradeLevel == planGradeLevel && 
                      it.subject.equals(planSubject, ignoreCase = true) &&
                      it.term.equals(currentTermVal, ignoreCase = true) &&
                      it.week == currentWeekVal
                  } ?: curriculumList.find {
                      it.gradeLevel == planGradeLevel && it.subject.equals(planSubject, ignoreCase = true)
                  }

                  val isInstructionalBlock = activeTermClean.contains("2nd") || activeTermClean.contains("instructional")

                  Card(
                      colors = CardDefaults.cardColors(
                          containerColor = if (isInstructionalBlock) Color(0xFF0F172A) else Color(0xFF451A03)
                      ),
                      border = BorderStroke(
                          1.dp,
                          if (isInstructionalBlock) Color(0xFF818CF8).copy(0.3f) else Color(0xFFF97316).copy(0.3f)
                      ),
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier.fillMaxWidth()
                  ) {
                      Column(modifier = Modifier.padding(12.dp)) {
                          Text(
                              text = if (isInstructionalBlock) "📌 ACTIVE LESSON POINTER (INSTRUCTIONAL BLOCK)" else "⚠️ NON-INSTRUCTIONAL BLOCK ALIGNED",
                              style = MaterialTheme.typography.labelSmall,
                              fontWeight = FontWeight.Bold,
                              color = if (isInstructionalBlock) Color(0xFF818CF8) else Color(0xFFF97316),
                              fontFamily = FontFamily.Monospace
                          )
                          Spacer(modifier = Modifier.height(4.dp))
                          
                          if (!isInstructionalBlock) {
                              Text(
                                  text = "Note: Under the DepEd TTY guidelines, formal curriculum teaching occurs exclusively within the Instructional Block (Term 2). Generating a lesson plan in Opening (Term 1) or End-of-Term (Term 3) shifts expectations to diagnostic mapping or transitional evaluation rubrics.",
                                  style = MaterialTheme.typography.bodySmall,
                                  color = Color(0xFFFDBA74)
                              )
                              Spacer(modifier = Modifier.height(8.dp))
                          }
                          
                          if (alignedStd != null) {
                              Text(
                                  text = "TARGET LESSON RANGE: ${alignedStd.subject} - ${alignedStd.gradeLevel} (Week $currentWeekVal)",
                                  style = MaterialTheme.typography.bodySmall,
                                  fontWeight = FontWeight.Bold,
                                  color = Color.White
                              )
                              Spacer(modifier = Modifier.height(2.dp))
                              Text(
                                  text = "Competency: ${alignedStd.learningCompetency}",
                                  style = MaterialTheme.typography.bodySmall,
                                  color = Color(0xFF94A3B8)
                              )
                              Spacer(modifier = Modifier.height(2.dp))
                              Text(
                                  text = "MELC Code: ${if (alignedStd.melcCode.isNotEmpty()) alignedStd.melcCode else "N/A"}",
                                  style = MaterialTheme.typography.bodySmall,
                                  color = Color(0xFF34C759),
                                  fontFamily = FontFamily.Monospace
                              )
                          } else {
                              Text(
                                  text = "No exact curriculum standard matches this combination. Compiler will use a standard fallback competency for $planSubject.",
                                  style = MaterialTheme.typography.bodySmall,
                                  color = Color(0xFF64748B)
                              )
                          }
                      }
                  }

                  Spacer(modifier = Modifier.height(16.dp))

                Text("Subject / Academic Dimension Target:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                
                var showSubjectDropdown by remember { mutableStateOf(false) }
                val subjectsInKaban = curriculumList.map { it.subject }.distinct().sorted()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1.5f)) {
                        CustomDropdownSelector(
                            label = "SUBJECT NAME",
                            value = planSubject,
                            onClick = { showSubjectDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        )
DropdownMenu(
                            expanded = showSubjectDropdown,
                            onDismissRequest = { showSubjectDropdown = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            if (subjectsInKaban.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No subjects in KABAN database", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
                                    onClick = { showSubjectDropdown = false }
                                )
                            } else {
                                subjectsInKaban.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s, color = Color.White, style = MaterialTheme.typography.bodySmall) },
                                        onClick = {
                                            viewModel.planSubject.value = s
                                            showSubjectDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = durationText,
                        onValueChange = {
                            durationText = it
                            val parsed = it.toIntOrNull()
                            if (parsed != null) {
                                viewModel.planDurationMins.value = parsed
                            }
                        },
                        label = { Text("Duration (Mins)", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF818CF8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1.2f),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Target Specific Grade & Language Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var showGradeMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        CustomDropdownSelector(
                            label = "SPECIFIC GRADE",
                            value = planSpecificGradeLevel.ifEmpty { "Grade 5" },
                            onClick = { showGradeMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        )
DropdownMenu(
                            expanded = showGradeMenu,
                            onDismissRequest = { showGradeMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            listOf("Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12").forEach { gradeOption ->
                                DropdownMenuItem(
                                    text = { Text(gradeOption, color = Color.White, style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        viewModel.planSpecificGradeLevel.value = gradeOption
                                        showGradeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    var showLangMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1.5f)) {
                        CustomDropdownSelector(
                            label = "LESSON LANGUAGE",
                            value = planLanguage.ifEmpty { "English" },
                            onClick = { showLangMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        )
DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            listOf("English", "Cebuano", "Tagalog", "Hiligaynon", "Ilokano", "Waray", "Bicolano").forEach { langOption ->
                                DropdownMenuItem(
                                    text = { Text(langOption, color = Color.White, style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        viewModel.planLanguage.value = langOption
                                        showLangMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- 4. NEW: LESSON DELIVERY SCHEDULING CARD ---
                Text("Lesson Delivery Parameters:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(6.dp))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color.White.copy(0.04f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Weekday Delivery drop-down
                            var showWeekdayMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1.2f)) {
                                CustomDropdownSelector(
                                    label = "DAY OF DELIVERY",
                                    value = deliveryDate,
                                    onClick = { showWeekdayMenu = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
DropdownMenu(
                                    expanded = showWeekdayMenu,
                                    onDismissRequest = { showWeekdayMenu = false },
                                    modifier = Modifier.background(Color(0xFF1E293B))
                                ) {
                                    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").forEach { day ->
                                        DropdownMenuItem(
                                            text = { Text(day, color = Color.White, style = MaterialTheme.typography.bodySmall) },
                                            onClick = {
                                                viewModel.lessonDeliveryDate.value = day
                                                showWeekdayMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Section Name Text Input
                            OutlinedTextField(
                                value = sectionName,
                                onValueChange = { viewModel.lessonSection.value = it },
                                label = { Text("Class Section Label", color = Color(0xFF94A3B8)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF818CF8),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Class Periods Multi-selector
                        Text("Active Class Period Blocks (1 to 10 - Choose Multiple):", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..10).forEach { pNum ->
                                val isChecked = selectedPeriods.contains(pNum)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isChecked) Color(0xFF4F46E5) else Color(0xFF1E293B))
                                        .clickable {
                                            val currentSet = selectedPeriods.toMutableSet()
                                            if (isChecked) {
                                                if (currentSet.size > 1) { // Retain at least one Period block
                                                    currentSet.remove(pNum)
                                                }
                                            } else {
                                                currentSet.add(pNum)
                                            }
                                            viewModel.lessonPeriods.value = currentSet
                                        }
                                        .border(1.dp, if (isChecked) Color.White.copy(0.2f) else Color.White.copy(0.02f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$pNum", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Grade Level Target Row
                Text("Select Key Stage Stage Scale:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("KS1", "KS2", "KS3", "KS4").forEach { l ->
                        val isSel = planGradeLevel == l
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF4F46E5) else Color(0xFF0F172A))
                                .clickable { viewModel.planGradeLevel.value = l }
                                .border(1.dp, if (isSel) Color.White.copy(0.15f) else Color.Transparent, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(l, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Teaching Strategy
                Text("Teaching Strategy / Pedagogy Focus:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Collaborative", "Inquiry-Based", "Direct Method", "Constructivist").forEach { s ->
                        val isSel = planStrategy == s
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFF4F46E5) else Color(0xFF0F172A))
                                .clickable { viewModel.planTeachingStrategy.value = s },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(s, style = Modifier.padding(horizontal = 2.dp).let { MaterialTheme.typography.labelSmall }, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- 5. NEW: TEACHER PROFESSIONAL PROFICIENCY LEVEL & PPST SYSTEM PRE-CHECKS ---
                Text("Educator Core Career Stage & PPST Checklist Standards Indicator:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Proficient", "Highly Proficient", "Distinguished").forEach { level ->
                        val isSel = teacherProficiency == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFF10B981) else Color(0xFF0F172A))
                                .clickable {
                                    val levelChecklist = when (level) {
                                        "Proficient" -> setOf("1.1.1: Content knowledge & pedagogy", "1.4.1: Use of Mother Tongue/Language", "2.1.1: Learner safety and security")
                                        "Highly Proficient" -> setOf("3.1.1: Learners with diverse backgrounds", "4.1.1: Planning of learning process")
                                        "Distinguished" -> setOf("4.1.1: Planning of learning process", "5.1.1: Design & selection of assessment", "1.1.1: Content knowledge & pedagogy")
                                        else -> emptySet()
                                    }
                                    viewModel.teacherProficiencyLevel.value = level
                                    viewModel.planPpstChecklist.value = levelChecklist
                                    Toast.makeText(context, "Loaded prechecks for $level level status", Toast.LENGTH_SHORT).show()
                                }
                                .border(1.dp, if (isSel) Color.White.copy(0.2f) else Color.Transparent, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color.White.copy(0.04f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ppstOptions.forEach { indicator ->
                            val isChecked = planPpstChecklist.contains(indicator)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isChecked) Color(0xFF4F46E5).copy(0.12f) else Color.Transparent)
                                    .clickable {
                                        val nextSet = planPpstChecklist.toMutableSet()
                                        if (isChecked) {
                                            nextSet.remove(indicator)
                                        } else {
                                            nextSet.add(indicator)
                                        }
                                        viewModel.planPpstChecklist.value = nextSet
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        val nextSet = planPpstChecklist.toMutableSet()
                                        if (isChecked) {
                                            nextSet.remove(indicator)
                                        } else {
                                            nextSet.add(indicator)
                                        }
                                        viewModel.planPpstChecklist.value = nextSet
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF818CF8),
                                        uncheckedColor = Color(0xFF475569)
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = indicator,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isChecked) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // --- 6. CONTENT STANDARD & GOALS WITH MANUAL OVERRIDES ---
                Text("Active Content Standard (Auto-populated with Override):", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = planContentStandardText,
                    onValueChange = { viewModel.planContentStandardText.value = it },
                    placeholder = { Text("Selected Content Standard...", fontSize = 11.sp, color = Color(0xFF475569)) },
                    modifier = Modifier.fillMaxWidth().height(65.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF818CF8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Active Learning Objective / Competency Target (Auto-populated with Override):", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = planLearningCompetencyText,
                    onValueChange = { viewModel.planLearningCompetencyText.value = it },
                    placeholder = { Text("Selected Learning Objectives...", fontSize = 11.sp, color = Color(0xFF475569)) },
                    modifier = Modifier.fillMaxWidth().height(65.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF818CF8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    var showContentStdsMenu by remember { mutableStateOf(false) }
                    val contentStdsChoices = curriculumList
                        .filter { it.subject.equals(planSubject, ignoreCase = true) }
                        .map { it.contentStandard }
                        .distinct()
                        .sorted()

                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showContentStdsMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("Content Standards (${contentStdsChoices.size})", fontSize = 10.sp, color = Color.White)
                        }
                        DropdownMenu(
                            expanded = showContentStdsMenu,
                            onDismissRequest = { showContentStdsMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B)).widthIn(max = 280.dp)
                        ) {
                            if (contentStdsChoices.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No content standards listed. Ingest first.", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
                                    onClick = { showContentStdsMenu = false }
                                )
                            } else {
                                contentStdsChoices.forEach { std ->
                                    DropdownMenuItem(
                                        text = { Text(std, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2) },
                                        onClick = {
                                            viewModel.planCustomPrompt.value = "Aligned Core Standard: $std. " + viewModel.planCustomPrompt.value
                                            showContentStdsMenu = false
                                            Toast.makeText(context, "Loaded matched Content Standard to Prompt!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    var showCompMenu by remember { mutableStateOf(false) }
                    val compChoices = curriculumList
                        .filter { it.subject.equals(planSubject, ignoreCase = true) }
                        .map { it.learningCompetency }
                        .distinct()
                        .sorted()

                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showCompMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("Learning Objectives (${compChoices.size})", fontSize = 10.sp, color = Color.White)
                        }
                        DropdownMenu(
                            expanded = showCompMenu,
                            onDismissRequest = { showCompMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B)).widthIn(max = 280.dp)
                        ) {
                            if (compChoices.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No criteria objectives listed.", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
                                    onClick = { showCompMenu = false }
                                )
                            } else {
                                compChoices.forEach { comp ->
                                    DropdownMenuItem(
                                        text = { Text(comp, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2) },
                                        onClick = {
                                            viewModel.planCustomPrompt.value = "Focus learning objective details: $comp. " + viewModel.planCustomPrompt.value
                                            showCompMenu = false
                                            Toast.makeText(context, "Integrated Objectives inside Custom Prompt!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom context / educator prompt
                Text("Custom Lesson Context / Educator Focus Instruct (Optional):", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = planPrompt,
                    onValueChange = { viewModel.planCustomPrompt.value = it },
                    placeholder = { Text("e.g. Include Visayan local history tales, drawings, or disaster awareness guidelines...", fontSize = 11.sp, color = Color(0xFF475569)) },
                    modifier = Modifier.fillMaxWidth().height(70.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF818CF8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Anchor Standard Status Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color.White.copy(0.02f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (matchedCurriculum != null) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (matchedCurriculum != null) "KABAN STANDARDS DETECTED & AUTOMAPPED" else "KABAN STANDARDS OUTSIDE RANGE: FALLBACK BLUEPRINT ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (matchedCurriculum != null) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                        if (matchedCurriculum != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("MELC Code: ${matchedCurriculum.melcCode} | Scope competency: ${matchedCurriculum.learningCompetency}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), maxLines = 1)
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No matching Kaban item for Grade $planGradeLevel - $planSubject (Week $currentWeekVal). System falls back to generalized models.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isGenerating) {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF818CF8))
                    }
                } else {
                    Button(
                        onClick = { viewModel.triggerIlawGeneration() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Blueprint generate icon", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GENERATE ILAW LP", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Terminal Output raw JSON representation
        if (activeJson.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMPILER COMPILATION STREAM (JSON RESPONSE)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF818CF8)
                        )
                        
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("ILAW Lesson Plan text", activeJson)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Lesson plan copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Copy block", tint = Color(0xFF34C759), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT", fontSize = 11.sp, color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF070A13))
                            .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = activeJson,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF34C759),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: LESSON PLAN ARCHIVE CATALOG (EXPORT)
// ==========================================
@Composable
fun PlansCatalogTab(viewModel: MainViewModel, plansList: List<LessonPlan>, onStartPrompter: (LessonPlan) -> Unit) {
    var selectedPlanId by remember { mutableStateOf(-1) }
    
    val currentTermVal by viewModel.currentTerm.collectAsStateWithLifecycle()
    val currentWeekVal by viewModel.currentWeek.collectAsStateWithLifecycle()
    
    val currentMonthKey = remember(currentTermVal, currentWeekVal) {
        getPlanMonth(currentTermVal, currentWeekVal)
    }
    
    // Store selected dates as a map of MonthName -> Triple(dayNum, weekIdx, dayName)
    var selectedDates by remember { mutableStateOf(mapOf<String, Triple<Int, Int, String>>()) }
    
    var expandedMonths by remember { mutableStateOf(setOf(currentMonthKey)) }
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "COMPILED ILAW PLAN REPOSITORY",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF818CF8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Every compiled blueprint is written locally to standard SQLite database records. Plans are grouped chronologically inside a calendar month, collapsing by TTY week blocks and daily schedules to prevent pacing confusion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        if (plansList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No compiled lesson plans found. Go to the ILAW BUILDER and click \"GENERATE ILAW LP\". Completed plans are automatically curated here.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
            }
        } else {
            // Group plans by Month first
            val groupedByMonth = plansList.groupBy { p ->
                getPlanMonth(p.term, p.week)
            }

            val academicMonths = listOf("June", "July", "August", "September", "October", "November", "December", "January", "February")
            val sortedMonthKeys = academicMonths.filter { it in groupedByMonth.keys }.reversed()

            sortedMonthKeys.forEach { monthName ->
                val isMonthExpanded = expandedMonths.contains(monthName)
                val plansInMonth = groupedByMonth[monthName] ?: emptyList()

                // Month Card Grouping
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isMonthExpanded) Color(0xFF161B30) else Color(0xFF0F1322)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Month Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedMonths = if (isMonthExpanded) {
                                        expandedMonths - monthName
                                    } else {
                                        expandedMonths + monthName
                                    }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isMonthExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = monthName.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF818CF8).copy(0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${plansInMonth.size} Plans",
                                    color = Color(0xFF818CF8),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isMonthExpanded) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            
                            val currentSelected = selectedDates[monthName]
                            MonthlyCalendarView(
                                monthName = monthName,
                                plansInMonth = plansInMonth,
                                onDateSelected = { dayNum, weekIdx, dayName ->
                                    selectedDates = selectedDates + (monthName to Triple(dayNum, weekIdx, dayName))
                                },
                                selectedCell = currentSelected
                            )

                            if (currentSelected != null) {
                                val (_, weekIdx, dayName) = currentSelected
                                val weekNum = weekIdx + 1
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A).copy(0.4f))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "📅 " + dayName.uppercase() + " (WEEK " + weekNum + ") PERIOD SLOTS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF818CF8),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    
                                    (1..8).forEach { periodNum ->
                                        val planForPeriod = plansInMonth.find { p ->
                                            getWeekOfMonth(p.term, p.week) == weekNum && 
                                            p.deliveryDate.equals(dayName, ignoreCase = true) &&
                                            p.periodNumbers.split(",").map { it.trim() }.contains(periodNum.toString())
                                        }
                                        
                                        val isExpanded = selectedPlanId == planForPeriod?.id
                                        
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = if (planForPeriod != null) Color(0xFF1E293B) else Color(0xFF0F172A).copy(0.3f)),
                                            border = BorderStroke(1.dp, if (planForPeriod != null) Color(0xFF818CF8).copy(0.2f) else Color.White.copy(0.02f)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = planForPeriod != null) {
                                                    selectedPlanId = if (isExpanded) -1 else planForPeriod!!.id
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = periodNum.toString() + ".",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (planForPeriod != null) Color(0xFF818CF8) else Color(0xFF475569),
                                                        modifier = Modifier.width(24.dp)
                                                    )
                                                    
                                                    if (planForPeriod != null) {
                                                        val subjShort = when {
                                                            planForPeriod.subject.lowercase().contains("science") || planForPeriod.subject.lowercase().contains("agham") -> "Sci"
                                                            planForPeriod.subject.lowercase().contains("math") -> "Math"
                                                            planForPeriod.subject.lowercase().contains("araling") || planForPeriod.subject.lowercase().contains("ap") -> "AP"
                                                            planForPeriod.subject.lowercase().contains("reading") || planForPeriod.subject.lowercase().contains("literacy") -> "Read"
                                                            planForPeriod.subject.lowercase().contains("english") -> "Eng"
                                                            else -> planForPeriod.subject.take(4)
                                                        }
                                                        val gradeNum = planForPeriod.specificGradeLevel.filter { it.isDigit() }.ifEmpty { planForPeriod.gradeLevel }
                                                        val prefix = (subjShort + " " + gradeNum).trim()
                                                        val rawTopic = if (planForPeriod.customPrompt.isNotBlank()) planForPeriod.customPrompt else planForPeriod.subject
                                                        val truncatedTopic = if (rawTopic.length > 15) rawTopic.take(15) + "..." else rawTopic
                                                        
                                                        Text(
                                                            text = prefix + " " + truncatedTopic,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color.White
                                                        )
                                                        
                                                        Spacer(modifier = Modifier.weight(1f))
                                                        
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(Color(0xFFF59E0B).copy(0.15f))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("EiE: " + planForPeriod.eieLevel, color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelSmall)
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Icon(
                                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                                contentDescription = null,
                                                                tint = Color(0xFF94A3B8),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Text(
                                                            text = "[Free Period / Empty Slot]",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = Color(0xFF475569),
                                                            fontStyle = FontStyle.Italic
                                                        )
                                                    }
                                                }
                                                
                                                if (planForPeriod != null && isExpanded) {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    QuadrantSection("I - INTENTIONS (OBJECTIVES & CONTENT STANDARD)", planForPeriod.intentions, Color(0xFF818CF8))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    QuadrantSection("L - LEARNING EXPERIENCES (CLASS PROCEDURES)", planForPeriod.learningExperiences, Color(0xFF818CF8))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    QuadrantSection("A - ASSESSMENT (PACE METHOD / DEPED WEIGHTS)", planForPeriod.assessment, Color(0xFF10B981))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    QuadrantSection("W - WAYS FORWARD (REMEDIATION & ENRICHMENT)", planForPeriod.waysForward, Color(0xFFF59E0B))

                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Button(
                                                                onClick = {
                                                                    val textDump = "BASKOG ILAW PLAN\n\nSUBJECT: " + planForPeriod.subject + "\nKEY STAGE: " + planForPeriod.gradeLevel + "\nTERM: " + planForPeriod.term + " Block, Week " + planForPeriod.week + "\n\nI. INTENTIONS\n" + planForPeriod.intentions + "\n\nII. LEARNING EXPERIENCES\n" + planForPeriod.learningExperiences + "\n\nIII. ASSESSMENT\n" + planForPeriod.assessment + "\n\nIV. WAYS FORWARD\n" + planForPeriod.waysForward
                                                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                    val clip = android.content.ClipData.newPlainText("ILAW Lesson Plan text", textDump)
                                                                    clipboard.setPrimaryClip(clip)
                                                                    Toast.makeText(context, "SUCCESS: Lesson Plan text copied to clipboard!", Toast.LENGTH_LONG).show()
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(10.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                            ) {
                                                                Icon(Icons.Default.Share, contentDescription = "Export text plan", tint = Color.White, modifier = Modifier.size(13.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("COPY TXT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }

                                                            Button(
                                                                onClick = {
                                                                    val jsonPayload = org.json.JSONObject().apply {
                                                                        put("subject", planForPeriod.subject)
                                                                        put("gradeLevel", planForPeriod.gradeLevel)
                                                                        put("specificGradeLevel", planForPeriod.specificGradeLevel)
                                                                        put("term", planForPeriod.term)
                                                                        put("week", planForPeriod.week)
                                                                        put("language", planForPeriod.language)
                                                                        put("ppstChecklist", planForPeriod.ppstChecklist)
                                                                        put("intentions", planForPeriod.intentions)
                                                                        put("learningExperiences", planForPeriod.learningExperiences)
                                                                        put("assessment", planForPeriod.assessment)
                                                                        put("waysForward", planForPeriod.waysForward)
                                                                    }.toString(2)
                                                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                    val clip = android.content.ClipData.newPlainText("ILAW Lesson Plan JSON", jsonPayload)
                                                                    clipboard.setPrimaryClip(clip)
                                                                    Toast.makeText(context, "SUCCESS: Full JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(10.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                            ) {
                                                                Icon(Icons.Default.Build, contentDescription = "Export JSON plan", tint = Color.White, modifier = Modifier.size(13.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("COPY JSON", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Button(
                                                                onClick = {
                                                                    com.example.data.LessonPlanExporter.exportToHtml(context, planForPeriod)
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(10.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                            ) {
                                                                Icon(Icons.Default.List, contentDescription = "Export Word document", tint = Color.White, modifier = Modifier.size(13.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("EXPORT WORD/HTML", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }

                                                            Button(
                                                                onClick = {
                                                                    com.example.data.LessonPlanExporter.exportToPdf(context, planForPeriod)
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(10.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                            ) {
                                                                Icon(Icons.Default.PlayArrow, contentDescription = "Export PDF document", tint = Color.White, modifier = Modifier.size(13.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("EXPORT PDF", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }

                                                        Button(
                                                            onClick = {
                                                                onStartPrompter(planForPeriod)
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(10.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                                        ) {
                                                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Active Prompter Deck", tint = Color.Black, modifier = Modifier.size(13.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("START ACTIVE CLASSROOM PROMPTER", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        
                                                        Button(
                                                            onClick = {
                                                                viewModel.deleteLessonPlan(planForPeriod.id)
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(10.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(0.2f)),
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Remove record", tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("DELETE PLAN BLUEPRINT", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                }
            }
        }
    }
}

@Composable
fun MonthlyCalendarView(
    monthName: String,
    plansInMonth: List<LessonPlan>,
    onDateSelected: (Int, Int, String) -> Unit,
    selectedCell: Triple<Int, Int, String>?
) {
    val termWeeks = when (monthName) {
        "June", "September", "December" -> listOf(1, 2, 3, 4)
        "July", "October", "January" -> listOf(5, 6, 7, 8)
        "August", "November", "February" -> listOf(9, 10)
        else -> listOf(1, 2, 3, 4)
    }

    val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    val weekdayAbbrev = listOf("Mon", "Tue", "Wed", "Thu", "Fri")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekdayAbbrev.forEach { abbrev ->
                Text(
                    text = abbrev,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        termWeeks.forEachIndexed { weekIdx, weekNum ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekdays.forEachIndexed { dayIdx, dayName ->
                    val dayNum = (weekIdx * 7) + dayIdx + 1
                    
                    val hasPlan = plansInMonth.any { p ->
                        getWeekOfMonth(p.term, p.week) == weekIdx + 1 && p.deliveryDate.equals(dayName, ignoreCase = true)
                    }

                    val isSelected = selectedCell != null && 
                                     selectedCell.first == dayNum && 
                                     selectedCell.second == weekIdx && 
                                     selectedCell.third == dayName

                    val cellBgColor = when {
                        isSelected -> Color(0xFF4F46E5)
                        hasPlan -> Color(0xFF1D4ED8).copy(alpha = 0.4f)
                        else -> Color(0xFF1E293B).copy(alpha = 0.5f)
                    }

                    val cellBorderColor = when {
                        isSelected -> Color(0xFF818CF8)
                        hasPlan -> Color(0xFF3B82F6).copy(alpha = 0.6f)
                        else -> Color.White.copy(alpha = 0.05f)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cellBgColor),
                        border = BorderStroke(1.dp, cellBorderColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f)
                            .clickable {
                                onDateSelected(dayNum, weekIdx, dayName)
                            }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (hasPlan || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (hasPlan || isSelected) Color.White else Color(0xFF94A3B8)
                                )
                                if (hasPlan && !isSelected) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(Color(0xFF3B82F6))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getWeekOfMonth(term: String, week: Int): Int {
    return when {
        week <= 4 -> week
        week <= 8 -> week - 4
        else -> week - 8
    }
}

private fun getPlanMonth(term: String, week: Int): String {
    val termClean = term.lowercase()
    return when {
        termClean.contains("1st") || termClean.contains("opening") -> {
            when {
                week <= 4 -> "June"
                week <= 8 -> "July"
                else -> "August"
            }
        }
        termClean.contains("2nd") || termClean.contains("instructional") -> {
            when {
                week <= 4 -> "September"
                week <= 8 -> "October"
                else -> "November"
            }
        }
        termClean.contains("3rd") || termClean.contains("end") -> {
            when {
                week <= 4 -> "December"
                week <= 8 -> "January"
                else -> "February"
            }
        }
        else -> "September"
    }
}

@Composable
fun QuadrantSection(title: String, body: String, accentColor: ComposeColor) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F172A))
                .padding(10.dp)
        ) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

// ==========================================
// TAB 4: FORMATIVE CLASS SERVER
// ==========================================
@Composable
fun ClassServerTab(viewModel: MainViewModel) {
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

    var activeSubTab by remember { mutableStateOf(0) } // 0 = Test Generator Pool, 1 = Server Join Setup
    var isCreatingQuestion by remember { mutableStateOf(false) }

    // Custom new question form state
    var newQText by remember { mutableStateOf("") }
    var newQType by remember { mutableStateOf("Multiple Choice") }
    var newQCorrectAnswer by remember { mutableStateOf("") }
    var newQOptionsCsv by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Core Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📡", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "FORMATIVE CLASSROOM QUIZ SERVER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Orchestrate real-time, local Wi-Fi direct interactive telemetry with student desks",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF818CF8)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This module works offline to simulate wireless telemetry desks and logs item score analysis reports inside the local SQLite registry to comply with DepEd TTY PACE assessments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        if (!serverActive) {
            // Configuration mode: Show tab toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { activeSubTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubTab == 0) Color(0xFF312E81) else Color.Transparent,
                        contentColor = if (activeSubTab == 0) Color.White else Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.5f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("📝 TEST GENERATOR POOL (${questionsPool.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { activeSubTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubTab == 1) Color(0xFF312E81) else Color.Transparent,
                        contentColor = if (activeSubTab == 1) Color.White else Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("📡 JOIN DEPLOYMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (activeSubTab == 0) {
                // TAB 0: TEST GENERATOR QUESTION POOL
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TEST POOL CONFIGURATOR",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clickable { viewModel.seedSamplePACEPool() }
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF10B981).copy(0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("SEED 5 PACE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    modifier = Modifier
                                        .clickable { viewModel.clearQuestionPool() }
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFEF4444).copy(0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("CLEAR ALL", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (questionsPool.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0B132B))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No questions configured. Click 'SEED 5 PACE' above or use the custom generator below.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
                            }
                        } else {
                            Text(
                                text = "Select a question below to configure it as the primary launch target:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF71717A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                questionsPool.forEachIndexed { idx, q ->
                                    val isActive = idx == activeIndex
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isActive) Color(0xFF1E1B4B) else Color(0xFF0F172A)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isActive) Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectActiveQuestionFromPool(idx) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Q${idx + 1}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isActive) Color(0xFF818CF8) else Color(0xFF94A3B8)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(
                                                                when (q.type) {
                                                                    "Multiple Choice" -> Color(0xFF6366F1).copy(0.15f)
                                                                    "True/False" -> Color(0xFF10B981).copy(0.15f)
                                                                    else -> Color(0xFF64748B).copy(0.15f)
                                                                }
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = q.type.uppercase(Locale.getDefault()),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 8.sp,
                                                            color = when (q.type) {
                                                                "Multiple Choice" -> Color(0xFF818CF8)
                                                                "True/False" -> Color(0xFF34D399)
                                                                else -> Color(0xFFCBD5E1)
                                                            }
                                                        )
                                                    }

                                                    if (isActive) {
                                                        Text(
                                                            text = "🎯 CONFIGURED",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFF59E0B),
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(q.text, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                                if (q.options.isNotEmpty() && q.type == "Multiple Choice") {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Options: ${q.options.joinToString(" | ")}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Correct: ${q.correctAnswer}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                            }

                                            IconButton(
                                                onClick = { viewModel.removeQuestionFromPool(q.id) }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete question", tint = Color(0xFFEF4444).copy(0.7f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom question generator toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCreatingQuestion = !isCreatingQuestion }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "➕ CREATE CUSTOM QUIZ QUESTION TO POOL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8)
                            )
                            Icon(
                                imageVector = if (isCreatingQuestion) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle creator",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (isCreatingQuestion) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B2E)),
                                border = BorderStroke(1.dp, Color.White.copy(0.04f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = newQText,
                                        onValueChange = { newQText = it },
                                        label = { Text("Question text", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF6366F1),
                                            unfocusedBorderColor = Color.White.copy(0.08f),
                                            unfocusedTextColor = Color.White,
                                            focusedTextColor = Color.White
                                        )
                                    )

                                    // Type select row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Type:", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.width(40.dp))
                                        listOf("Multiple Choice", "True/False", "Free Text").forEach { ty ->
                                            val isSel = newQType == ty
                                            Box(
                                                modifier = Modifier
                                                    .clickable {
                                                        newQType = ty
                                                        if (ty == "True/False") {
                                                            newQCorrectAnswer = "True"
                                                            newQOptionsCsv = "True, False"
                                                        } else if (ty == "Free Text") {
                                                            newQCorrectAnswer = "Write physical response details."
                                                            newQOptionsCsv = ""
                                                        } else {
                                                            newQCorrectAnswer = "A"
                                                            newQOptionsCsv = "A) Friction, B) Gravity, C) Magnetism, D) Inertia"
                                                        }
                                                    }
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSel) Color(0xFF4F46E5) else Color.White.copy(0.06f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(ty, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = newQCorrectAnswer,
                                            onValueChange = { newQCorrectAnswer = it },
                                            label = { Text("Correct Key", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                            modifier = Modifier.weight(1f),
                                            textStyle = MaterialTheme.typography.bodySmall,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF6366F1),
                                                unfocusedBorderColor = Color.White.copy(0.08f),
                                                unfocusedTextColor = Color.White,
                                                focusedTextColor = Color.White
                                            )
                                        )

                                        if (newQType == "Multiple Choice") {
                                            OutlinedTextField(
                                                value = newQOptionsCsv,
                                                onValueChange = { newQOptionsCsv = it },
                                                label = { Text("Choices (comma-sep)", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                                modifier = Modifier.weight(2f),
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF6366F1),
                                                    unfocusedBorderColor = Color.White.copy(0.08f),
                                                    unfocusedTextColor = Color.White,
                                                    focusedTextColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (newQText.isBlank()) {
                                                Toast.makeText(context, "ERROR: Provide question text first.", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val opts = if (newQType == "Multiple Choice") {
                                                newQOptionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                            } else if (newQType == "True/False") {
                                                listOf("True", "False")
                                            } else {
                                                listOf(newQCorrectAnswer)
                                            }
                                            viewModel.addQuestionToPool(newQText, newQType, newQCorrectAnswer, opts)
                                            // Reset custom form values
                                            newQText = ""
                                            newQCorrectAnswer = ""
                                            newQOptionsCsv = ""
                                            Toast.makeText(context, "SUCCESS: Question recorded to standard Test Pool!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Text("RESERVE IN PRE-TEST GENERATOR POOL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: SERVER JOIN DEPLOYMENT SCREEN
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CHOOSE NETWORK JOIN CHANNEL CHANNEL METHOD",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Student connection profile can be distributed through direct visual QR Codes or manual gateway configuration inputs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Join Method Toggle selector (QR vs Manual)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                onClick = { viewModel.serverJoinMethod.value = "QR" },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (joinMethod == "QR") Color(0xFF1E1B4B) else Color(0xFF0F172A)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (joinMethod == "QR") Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("📱", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("QR CODE SIGNAL", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Direct visuals scan-to-join", style = MaterialTheme.typography.labelSmall, color = Color(0xFF71717A), textAlign = TextAlign.Center)
                                }
                            }

                            Card(
                                onClick = { viewModel.serverJoinMethod.value = "Manual" },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (joinMethod == "Manual") Color(0xFF1E1B4B) else Color(0xFF0F172A)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (joinMethod == "Manual") Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("⚙️", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("MANUAL GATEWAY", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Type SSID credentials", style = MaterialTheme.typography.labelSmall, color = Color(0xFF71717A), textAlign = TextAlign.Center)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual Output based on Selection
                        if (joinMethod == "QR") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White)
                                        .border(2.dp, Color(0xFF6366F1), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Custom high-fidelity QR Code block grid representation using nested Canvas drawings
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val squareSize = size.width / 9f
                                        // Draw standard QR Finder Patterns at top-left, top-right, bottom-left
                                        val finderLocs = listOf(
                                            Pair(0f, 0f),
                                            Pair(6f * squareSize, 0f),
                                            Pair(0f, 6f * squareSize)
                                        )
                                        finderLocs.forEach { (xOff, yOff) ->
                                            // Outer finder square
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

                                        // Draw multiple random high-contrast QR data bits
                                        val dataPoints = listOf(
                                            Pair(4f, 1f), Pair(5f, 1f), Pair(4f, 2f), Pair(5f, 3f),
                                            Pair(1f, 4f), Pair(2f, 4f), Pair(3f, 4f), Pair(5f, 4f), Pair(7f, 4f),
                                            Pair(1f, 5f), Pair(3f, 5f), Pair(4f, 5f), Pair(8f, 5f),
                                            Pair(4f, 6f), Pair(5f, 6f), Pair(6f, 6f), Pair(8f, 6f),
                                            Pair(3f, 7f), Pair(4f, 7f), Pair(5f, 7f), Pair(6f, 7f),
                                            Pair(4f, 8f), Pair(6f, 8f), Pair(7f, 8f), Pair(8f, 8f)
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
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "SCAN QR TARGET TO SYNC DESKS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Students scan with built-in camera for fast background handshakes.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        } else {
                            // MANUAL INVENTORY
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, Color.White.copy(0.04f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("MANUAL WIFI DIRECT GATEWAY CREDENTIALS:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)

                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("SSID NETWORK KEY:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                        Text("BASKOG_WIFI_TTY_DIRECT", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }

                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("LOCAL SERVER HOST IP:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                        Text("http://192.168.49.1:8080", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }

                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("PACE ACCESS TOKEN:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                        Text("CLASS-PACE-DEPT-9", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            val clipPayload = "SSID: BASKOG_WIFI_TTY_DIRECT\nHost: http://192.168.49.1:8080\nToken: CLASS-PACE-DEPT-9"
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Manual SSID info", clipPayload)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Handshake credentials copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("COPY WIFI DIRECT CONFIG DETAILS", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ACTIVE QUESTION INFO READY TO TRANSMIT
                        val targetLabel = if (questionsPool.isNotEmpty() && activeIndex in questionsPool.indices) {
                            "Q${activeIndex + 1}: \"${questionsPool[activeIndex].text}\""
                        } else {
                            "Def: \"Verify standard PACE criteria.\""
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, Color.White.copy(0.02f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("LAUNCH BLUEPRINT BROADCAST CONFIGURATION:", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(targetLabel, style = MaterialTheme.typography.bodySmall, color = Color.White, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.launchQuizFromServerPool()
                                Toast.makeText(context, "BROADCAST ONLINE: Live quiz channels activated on Local Host!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("LAUNCH LIVE BROADCAST QUIZ SERVER", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // SERVER IS BROADCASTING LIVE TRANSMISSIONS
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(0.3f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Flashing telemetry status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BROADCAST RUNNING VIA ${joinMethod.uppercase(Locale.getDefault())} CHANNEL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }

                        // Pool position counter or track
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF312E81))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Q ${activeIndex + 1} OF ${questionsPool.size.coerceAtLeast(1)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC7D2FE),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Question box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text("INSTRUCTIONAL QUERY POLLING:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = qText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Correct Key Option: $correctAns | Form: $qType",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // MULTIPLE QUESTIONS NAVIGATION PROVISION inside the active broadcasting server!
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
                                disabledContainerColor = Color(0xFF0F172A).copy(0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
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
                                disabledContainerColor = Color(0xFF0F172A).copy(0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("NEXT QUIZ ▶", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.simulateClassResponses()
                                Toast.makeText(context, "SIMULATING: Wireless telemetry replies received from 30 desks!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f).height(40.dp)
                        ) {
                            Text("SIMULATE DESKS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.stopAndRecordSession()
                                Toast.makeText(context, "SUCCESS: Result saved to Classroom item stats database registry!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f).height(40.dp)
                        ) {
                            Text("STOP & RECORD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.resetCurrentSession()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.7f).height(40.dp)
                        ) {
                            Text("RESET", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 5: CLASS RECORD & ANALYTICS
// ==========================================
@Composable
fun OldClassRecordTab(viewModel: MainViewModel) {
    val history by viewModel.formativeHistory.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Stateful variables for DO 15, s. 2026 Compliance Calculator
    var selectedSubject by remember { mutableStateOf("Filipino") }
    var keyStage by remember { mutableStateOf("KS2-KS4 (Grades 4-12)") } // "KS1 (Kindergarten)", "KS1 (Grades 1-3)", "KS2-KS4 (Grades 4-12)"
    var schoolYear by remember { mutableStateOf("SY 2026-2027 (Transmuted)") } // "SY 2026-2027 (Transmuted)", "SY 2027-2028 (Zero-Based)"
    
    // Component Inputs
    var wwRaw by remember { mutableStateOf("15") }
    var wwMax by remember { mutableStateOf("20") }
    
    var ptRaw by remember { mutableStateOf("45") }
    var ptMax by remember { mutableStateOf("50") }
    
    var st1Raw by remember { mutableStateOf("12") }
    var st1Max by remember { mutableStateOf("15") }
    
    var st2Raw by remember { mutableStateOf("13") }
    var st2Max by remember { mutableStateOf("15") }
    
    var termExamRaw by remember { mutableStateOf("38") }
    var termExamMax by remember { mutableStateOf("50") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Core Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📊", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "CLASS HISTORICAL RECORDS & ANALYTICS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Permanent local ledger registry for TALA curriculum competency diagnostics",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- NEW DEPED ORDER NO. 015, S. 2026 COMPLIANCE CONSOLE ---
        // --- Dynamic Calculations ---
        val wwRawVal = wwRaw.toFloatOrNull() ?: 0f
        val wwMaxVal = wwMax.toFloatOrNull() ?: 1f
        val ptRawVal = ptRaw.toFloatOrNull() ?: 0f
        val ptMaxVal = ptMax.toFloatOrNull() ?: 1f
        
        val st1RawVal = st1Raw.toFloatOrNull() ?: 0f
        val st1MaxVal = st1Max.toFloatOrNull() ?: 1f
        
        val st2RawVal = st2Raw.toFloatOrNull() ?: 0f
        val st2MaxVal = st2Max.toFloatOrNull() ?: 1f
        
        val termExamRawVal = termExamRaw.toFloatOrNull() ?: 0f
        val termExamMaxVal = termExamMax.toFloatOrNull() ?: 1f
        
        // Percentages
        val wwPercent = if (wwMaxVal > 0) (wwRawVal / wwMaxVal * 100f).coerceIn(0f, 100f) else 0f
        val ptPercent = if (ptMaxVal > 0) (ptRawVal / ptMaxVal * 100f).coerceIn(0f, 100f) else 0f
        
        val st1Percent = if (st1MaxVal > 0) (st1RawVal / st1MaxVal * 100f).coerceIn(0f, 100f) else 0f
        val st2Percent = if (st2MaxVal > 0) (st2RawVal / st2MaxVal * 100f).coerceIn(0f, 100f) else 0f
        val termExamPercent = if (termExamMaxVal > 0) (termExamRawVal / termExamMaxVal * 100f).coerceIn(0f, 100f) else 0f
        
        // EX Component Breakdown: ST1 (30%), ST2 (30%), Term Exam (40%)
        val exCompositePercent = (st1Percent * 0.30f) + (st2Percent * 0.30f) + (termExamPercent * 0.40f)
        
        // Weights (KS2-KS4: WW 20%, PT 60%, EX 20%)
        val weightedWW = wwPercent * 0.20f
        val weightedPT = ptPercent * 0.60f
        val weightedEX = exCompositePercent * 0.20f
        
        // Initial Grade (IG)
        val initialGrade = weightedWW + weightedPT + weightedEX
        
        // Helper Transmutation Table function for SY 2026-2027
        fun transmuteSY2026(ig: Float): Int {
            val pct = ig.coerceIn(0f, 100f)
            return when {
                pct >= 100f -> 100
                pct >= 96f -> 99
                pct >= 92f -> 98
                pct >= 88f -> 97
                pct >= 84f -> 96
                pct >= 80f -> 95
                pct >= 76f -> 94
                pct >= 72f -> 93
                pct >= 68f -> 92
                pct >= 64f -> 91
                pct >= 60f -> 90
                pct >= 56f -> 89
                pct >= 52f -> 88
                pct >= 48f -> 87
                pct >= 44f -> 86
                pct >= 40f -> 85
                pct >= 36f -> 84
                pct >= 32f -> 83
                pct >= 28f -> 82
                pct >= 24f -> 81
                pct >= 20f -> 80
                pct >= 16f -> 79
                pct >= 12f -> 78
                pct >= 8f -> 77
                pct >= 4f -> 76
                else -> 75
            }
        }

        // Term Grade (TG) based on School Year Transition Rules
        val isZeroBased = schoolYear.contains("Zero-Based")
        val termGrade = if (isZeroBased) {
            initialGrade.toInt().coerceIn(0, 100)
        } else {
            transmuteSY2026(initialGrade)
        }
        
        // Final Grade (FG) with 75% transitional floor policy applied (if TG < 75 but raw percentage is >= 70%)
        val isFloorApplied = termGrade < 75 && initialGrade >= 70f
        val finalGrade = if (isFloorApplied) 75 else termGrade

        // Section 1: Interactive DO 15 Compliance Electronic Class Record
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Calculator Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DO 015, S. 2026 COMPLIANCE CONSOLE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Selectors Row (Subject, Key Stage, SY)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SUBJECT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    selectedSubject = if (selectedSubject == "Filipino") "English" else "Filipino"
                                }
                                .padding(10.dp)
                        ) {
                            Text(selectedSubject, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("KEY STAGE SCOPE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    keyStage = when (keyStage) {
                                        "KS2-KS4 (Grades 4-12)" -> "KS1 (Kindergarten)"
                                        "KS1 (Kindergarten)" -> "KS1 (Grades 1-3)"
                                        else -> "KS2-KS4 (Grades 4-12)"
                                    }
                                }
                                .padding(10.dp)
                        ) {
                            Text(keyStage, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("POLICY SCHOOL YEAR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    schoolYear = if (schoolYear.contains("Transmuted")) {
                                        "SY 2027-2028 (Zero-Based)"
                                    } else {
                                        "SY 2026-2027 (Transmuted)"
                                    }
                                }
                                .padding(10.dp)
                        ) {
                            Text(schoolYear, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Section (Columns depending on Key Stage)
                if (keyStage.startsWith("KS2-KS4")) {
                    Text(
                        text = "WEIGHTED COMPONENT SCORES (20% WW, 60% PT, 20% EX)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Written Works & Performance Tasks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = wwRaw,
                            onValueChange = { wwRaw = it },
                            label = { Text("WW Raw") },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = wwMax,
                            onValueChange = { wwMax = it },
                            label = { Text("WW Max") },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ptRaw,
                            onValueChange = { ptRaw = it },
                            label = { Text("PT Raw") },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ptMax,
                            onValueChange = { ptMax = it },
                            label = { Text("PT Max") },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Examination Breakdown Component (ST1: 30%, ST2: 30%, Term Exam: 40%)
                    Text(
                        text = "EXAMINATION COMPOSITE BREAKDOWN (30% ST1, 30% ST2, 40% TERM EXAM)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // ST1
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = st1Raw,
                                onValueChange = { st1Raw = it },
                                label = { Text("ST1 Raw") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            OutlinedTextField(
                                value = st1Max,
                                onValueChange = { st1Max = it },
                                label = { Text("ST1 Max") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                        }
                        // ST2
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = st2Raw,
                                onValueChange = { st2Raw = it },
                                label = { Text("ST2 Raw") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            OutlinedTextField(
                                value = st2Max,
                                onValueChange = { st2Max = it },
                                label = { Text("ST2 Max") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                        }
                        // Term Exam
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = termExamRaw,
                                onValueChange = { termExamRaw = it },
                                label = { Text("Term Raw") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            OutlinedTextField(
                                value = termExamMax,
                                onValueChange = { termExamMax = it },
                                label = { Text("Term Max") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                        }
                    }
                } else {
                    // Key Stage 1 Descriptive grading indicators input
                    Text(
                        text = "KEY STAGE 1 (KS1) QUALITATIVE PROFILE INPUTS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = wwRaw,
                            onValueChange = { wwRaw = it },
                            label = { Text("Written Raw") },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = wwMax,
                            onValueChange = { wwMax = it },
                            label = { Text("Written Max") },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ptRaw,
                            onValueChange = { ptRaw = it },
                            label = { Text("Performance Raw") },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ptMax,
                            onValueChange = { ptMax = it },
                            label = { Text("Performance Max") },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- COMPUTATION DISPLAY BLOCK ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CALCULATION LEDGER RESULTS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isZeroBased) "ZERO-BASED" else "TRANSMUTED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        if (keyStage.startsWith("KS2-KS4")) {
                            // Display weighted details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("WW WEIGHTED (20%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${String.format("%.1f", weightedWW)}% / 20.0%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("PT WEIGHTED (60%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${String.format("%.1f", weightedPT)}% / 60.0%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text("EX WEIGHTED (20%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${String.format("%.1f", weightedEX)}% / 20.0%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Terminal official Grades (IG, TG, FG)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Initial Grade
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("INITIAL GRADE (IG)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${String.format("%.1f", initialGrade)}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Term Grade
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TERM GRADE (TG)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$termGrade",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            // Final Grade with Floor logic
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("FINAL GRADE (FG)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (keyStage.startsWith("KS1 (Kinder")) {
                                            // Kindergarten qualitative profile mapping
                                            when {
                                                initialGrade >= 80f -> "CO"
                                                initialGrade >= 50f -> "DV"
                                                else -> "BG"
                                            }
                                        } else if (keyStage.startsWith("KS1 (Grades")) {
                                            // Grades 1-3 descriptive rating mapping
                                            when {
                                                initialGrade >= 90f -> "A"
                                                initialGrade >= 80f -> "B"
                                                initialGrade >= 70f -> "C"
                                                initialGrade >= 60f -> "D"
                                                else -> "E"
                                            }
                                        } else {
                                            "$finalGrade"
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (finalGrade >= 75 || keyStage.startsWith("KS1")) MaterialTheme.colorScheme.primary else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }

                        if (isFloorApplied && keyStage.startsWith("KS2-KS4")) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Floor icon",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TRANSITIONAL FLOOR POLICY APPLIED: Scaled raw weighted score >= 70.0% to 75 in final term records.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Descriptive explanations for Key Stage 1
                        if (keyStage.startsWith("KS1")) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val descriptStr = if (keyStage.contains("Kinder")) {
                                when {
                                    initialGrade >= 80f -> "CO - Consistently Observed: Learner shows consistent competency."
                                    initialGrade >= 50f -> "DV - Developing: Learner exhibits developing competency."
                                    else -> "BG - Beginning: Learner is starting to display core performance competencies."
                                }
                            } else {
                                when {
                                    initialGrade >= 90f -> "A - Outstanding Achievement (Academic Award Eligible)"
                                    initialGrade >= 80f -> "B - Very Satisfactory Achievement"
                                    initialGrade >= 70f -> "C - Satisfactory Performance"
                                    initialGrade >= 60f -> "D - Fairly Satisfactory Performance"
                                    else -> "E - Did Not Meet Target Expectations (Remediation Triggered)"
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = descriptStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        // Append this compliant calculated result to our registry / system logs!
                        viewModel.logSystemEvent(
                            "DO15_GRADING",
                            "Archived DO 15 Grade Profile for $selectedSubject ($keyStage): IG=${String.format("%.1f", initialGrade)}%, TG=$termGrade, FG=$finalGrade"
                        )
                        Toast.makeText(context, "Calculated student grade profile saved to permanent log stream!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Save grade icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ARCHIVE COMPLIANT GRADE PROFILE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Card 3: Historical Registry & Analytics
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "History icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CLASS SCORE REGISTRY & ITEM ANALYTICS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (history.isNotEmpty()) {
                        Button(
                            onClick = {
                                history.firstOrNull()?.let {
                                    com.example.data.LessonPlanExporter.exportClassRecordToCsv(context, it)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Excel Export icon", tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT EXCEL/CSV", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved class sessions recorded yet. Launch a Live server under the CLASS SERVER tab to seed student scoring.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    history.forEach { record ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            val participated = record.studentsList.count { it.hasAnswered }
                            val correctCount = record.studentsList.count { it.isCorrect }
                            val accuracy = if (participated > 0) (correctCount.toFloat() / participated * 100).toInt() else 0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(0.12f))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "ID: #${record.id}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${record.questionType} • ${record.timestamp}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (accuracy >= 75) Color(0xFF10B981).copy(0.12f) else Color(0xFFF59E0B).copy(0.12f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Success: $accuracy%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (accuracy >= 75) Color(0xFF059669) else Color(0xFFD97706)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "\"${record.questionText}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // ITEM ANALYSIS BREAKDOWN STATS
                            Text(
                                text = "ITEM CHOICE ANALYSIS:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            record.itemAnalysis.forEach { (optionLabel, count) ->
                                val pct = if (participated > 0) count.toFloat() / participated else 0f
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = optionLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.width(70.dp),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.08f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(pct)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (optionLabel == record.correctAnswer) Color(0xFF10B981) else Color(0xFF6366F1)
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$count Desks (${(pct * 100).toInt()}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // INDIVIDUAL SCORE REGISTRY SUMMARY (Who answered what)
                            var showStudentRegistry by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showStudentRegistry = !showStudentRegistry }
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (showStudentRegistry) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand info icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showStudentRegistry) "HIDE STUDENT DESK REGISTRY" else "EXPAND STUDENT DESK REGISTRY (SCORE LIST)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (showStudentRegistry) {
                                Spacer(modifier = Modifier.height(8.dp))
                                record.studentsList.forEach { student ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = student.studentName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (student.hasAnswered) "Answer: ${student.answer}" else "DID NOT ANSWER",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (student.hasAnswered) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFDC2626)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (student.isCorrect) Color(0xFF10B981).copy(0.12f) else Color(0xFFEF4444).copy(0.12f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (student.isCorrect) "SCORE: 100" else "SCORE: 0",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (student.isCorrect) Color(0xFF059669) else Color(0xFFDC2626)
                                                )
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
    }
}

// ==========================================
// TAB 6: REAL-TIME CLASS ATTENDANCE & ROSTER
// ==========================================
@Composable
fun OldAttendanceTab(viewModel: MainViewModel) {
    val students by viewModel.formativeSimulatedStudents.collectAsStateWithLifecycle()
    val serverActive by viewModel.formativeServerActive.collectAsStateWithLifecycle()
    val classRosterText by viewModel.classRosterText.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Core Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👥", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "REAL-TIME CLASS ATTENDANCE & LIVE ROSTER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Track online student computing tablets and direct local Wi-Fi telemetry status",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Live Desk Status / Realtime Seating Plan Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = "List logo", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STUDENT LOG-IN DESK MATRIX (ROSTER)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (serverActive) {
                        "Active session is running. Student desks are connected via local Wi-Fi telemetry. Click 'SIMULATE DESKS' under the CLASS SERVER tab to watch responses populate in real-time."
                    } else {
                        "Server is currently offline. Launch the server in the CLASS SERVER tab to start receiving wireless telemetry signals."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (students.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No students initialized in roster. Add student names in the configuration block below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Rendering student boxes
                    val columns = 5
                    val totalRows = (students.size + columns - 1) / columns
                    for (r in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (c in 0 until columns) {
                                val idx = r * columns + c
                                if (idx < students.size) {
                                    val stud = students[idx]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (stud.hasAnswered) {
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                                                } else {
                                                    Color.Black.copy(alpha = 0.04f)
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                if (stud.hasAnswered) MaterialTheme.colorScheme.secondary else Color.Black.copy(alpha = 0.08f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(6.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = stud.studentName.split(" ").firstOrNull() ?: stud.studentName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            if (stud.hasAnswered) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.secondary)
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = stud.answer,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "OFFLINE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 8.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
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
            }
        }

        // CLASS COHORT ROSTER CONFIGURATION Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CLASS COHORT ROSTER CONFIGURATION",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Specify line-separated learner names below to synchronize with standard local student computing tablets. These names are persisted locally inside secure database buffers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = classRosterText,
                    onValueChange = { viewModel.classRosterText.value = it },
                    label = { Text("Student Names (One per line)") },
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total students registered: ${classRosterText.split("\n").filter { it.isNotBlank() }.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ==========================================
// TAB 5: SYSTEM CONSOLE (MONITOR)
// ==========================================
@Composable
fun SystemConsoleTab(
    viewModel: MainViewModel,
    logs: List<SystemLog>,
    currList: List<Curriculum>,
    plansList: List<LessonPlan>
) {
    val term by viewModel.currentTerm.collectAsStateWithLifecycle()
    val week by viewModel.currentWeek.collectAsStateWithLifecycle()
    val eie by viewModel.eieLevel.collectAsStateWithLifecycle()
    val lockdownActive by viewModel.lockdownActive.collectAsStateWithLifecycle()

    val teacherNameVal by viewModel.teacherName.collectAsStateWithLifecycle()
    val teacherDesignationVal by viewModel.teacherDesignation.collectAsStateWithLifecycle()
    val schoolNameVal by viewModel.schoolName.collectAsStateWithLifecycle()
    val schoolDistrictVal by viewModel.schoolDistrict.collectAsStateWithLifecycle()
    val divisionOfficeVal by viewModel.divisionOffice.collectAsStateWithLifecycle()

    var showClearLogsConfirm by remember { mutableStateOf(false) }

    if (showClearLogsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearLogsConfirm = false },
            title = { Text("Confirm Clearing Logs", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to permanently delete all local integrity and security logs from BASCROG OS?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLogs()
                        showClearLogsConfirm = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        
        // Internal Architecture Variables
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "INTERNAL ARCHITECTURE VARIABLES",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF818CF8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Three-Term Block Management
                Text(
                    text = "Governing Three-Term Block Target:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("1st Term", "2nd Term", "3rd Term").forEach { item ->
                        val isSel = term == item
                        Button(
                            onClick = { viewModel.updateTerm(item) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) Color(0xFF4F46E5) else Color(0xFF0F172A),
                                contentColor = if (isSel) Color.White else Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(item, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Week Slider Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Segment Calendar (Block Week): $week",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.updateWeek(week - 1) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.updateWeek(week + 1) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                Spacer(modifier = Modifier.height(14.dp))

                // Learning Continuity Emergency Protocols (EiE)
                Text(
                    text = "Active Learning Continuity Level (EiE Safety Protocol):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Risk factors affect lesson outputs. Manual switches are locked during manual overrides.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Hayo", "Hinay", "Hinga", "Hinto").forEach { protocol ->
                        val isSel = eie == protocol
                        val btnColor = when (protocol) {
                            "Hayo" -> Color(0xFF10B981)
                            "Hinay" -> Color(0xFFF59E0B)
                            "Hinga" -> Color(0xFF3B82F6)
                            "Hinto" -> Color(0xFFEF4444)
                            else -> Color.Gray
                        }
                        Button(
                            onClick = { viewModel.updateEiELevel(protocol) },
                            enabled = !lockdownActive,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) btnColor else Color(0xFF0F172A),
                                contentColor = if (isSel) Color.White else Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(protocol, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                Spacer(modifier = Modifier.height(14.dp))

                // DRRM Critical Lockdown Control Override
                Text(
                    text = "DRRM EMERGENCY SYSTEM OVERRIDE BOARD",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (!lockdownActive) {
                    Button(
                        onClick = { viewModel.triggerLockdownOverride() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Lockdown trigger")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ENGAGE PROTOCOL LOCKDOWN (HINTO)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = { viewModel.liftLockdownOverride() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Lockdown disengage")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DISENGAGE LOCKDOWN AND UNLOCK MATRIX", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Operational Metrics Stats grid
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B2E)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BASKOG OPERATIONAL SYSTEM METRICS",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF818CF8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0F172A)).padding(12.dp)) {
                        Column {
                            Text("SQL MELCs Ingested", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${currList.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0F172A)).padding(12.dp)) {
                        Column {
                            Text("Compiled ILAW Plans", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${plansList.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Educator profile & school settings
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Subtle accent line under banner block
                SubtleAccentDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "EDUCATOR PROFILE & SCHOOL SETTINGS",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF818CF8),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configure default placeholders injected into your generated ILAW lesson plan headings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // --- PROFILE AVATAR PROTOCOLS SECTION ---
                    val activeAvatarVal by viewModel.teacherProfilePicture.collectAsStateWithLifecycle()
                    Text(
                        text = "Educator Profile Picture Local Upload Protocol:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var showLocalFilePicker by remember { mutableStateOf(false) }

                    if (showLocalFilePicker) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showLocalFilePicker = false }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.AccountBox,
                                            contentDescription = "Folder Icon",
                                            tint = Color(0xFF818CF8),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text("Select Profile Pic from Local Storage", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                    var selectedFileIndex by remember { mutableStateOf(-1) }
                                    var isUploadingFile by remember { mutableStateOf(false) }
                                    var uploadProgress by remember { mutableStateOf(0f) }

                                    val simulatedFiles = listOf(
                                        Triple("formal_id_portrait.png", "local_1", "📁 SD Card / Pictures (1.2 MB)"),
                                        Triple("deped_official_avatar.jpg", "local_2", "📁 SD Card / Pictures (840 KB)"),
                                        Triple("outdoor_classroom_profile.jpg", "local_3", "📁 SD Card / Pictures (2.1 MB)"),
                                        Triple("scanned_id_headshot.jpg", "local_4", "📁 SD Card / Downloads (950 KB)"),
                                        Triple("backup_photo_2026.png", "local_5", "📁 SD Card / Downloads (1.5 MB)")
                                    )

                                    LaunchedEffect(selectedFileIndex) {
                                        if (selectedFileIndex != -1) {
                                            isUploadingFile = true
                                            uploadProgress = 0f
                                            while (uploadProgress < 1.0f) {
                                                kotlinx.coroutines.delay(150)
                                                uploadProgress += 0.25f
                                            }
                                            viewModel.saveProfilePicture(simulatedFiles[selectedFileIndex].second)
                                            isUploadingFile = false
                                            selectedFileIndex = -1
                                            showLocalFilePicker = false
                                        }
                                    }

                                    Text(
                                        text = "Directory: /storage/emulated/0/",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )

                                    if (isUploadingFile) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                progress = uploadProgress,
                                                color = Color(0xFF818CF8),
                                                trackColor = Color.White.copy(alpha = 0.1f)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                "Uploading local file: ${(uploadProgress * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            simulatedFiles.forEachIndexed { index, file ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF1E293B))
                                                        .clickable { selectedFileIndex = index }
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = androidx.compose.material.icons.Icons.Default.AccountCircle,
                                                        contentDescription = "File Icon",
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                    Column {
                                                        Text(file.first, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                        Text(file.third, color = Color(0xFF64748B), fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showLocalFilePicker = false }) {
                                            Text("Cancel", color = Color(0xFF94A3B8))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current active profile picture preview
                        TeacherProfileAvatar(
                            picture = activeAvatarVal,
                            size = 64.dp,
                            borderWidth = 3.dp,
                            borderColor = Color(0xFF818CF8)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Select Preset Avatar or Upload Locally:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("preset_1", "preset_2", "preset_3", "preset_4").forEachIndexed { index, preset ->
                                    val isSelected = activeAvatarVal == preset
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .clickable { viewModel.saveProfilePicture(preset) }
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF818CF8) else Color.White.copy(0.2f),
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                    ) {
                                        TeacherProfileAvatar(
                                            picture = preset,
                                            size = 32.dp,
                                            borderWidth = 0.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showLocalFilePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("setting_custom_avatar_url")
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Add,
                            contentDescription = "Upload Icon",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload from Local Directory", color = Color.White, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = teacherNameVal,
                        onValueChange = { viewModel.saveTeacherMetadata(it, teacherDesignationVal, schoolNameVal, schoolDistrictVal, divisionOfficeVal) },
                        label = { Text("Teacher Name", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("setting_teacher_name")
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = teacherDesignationVal,
                        onValueChange = { viewModel.saveTeacherMetadata(teacherNameVal, it, schoolNameVal, schoolDistrictVal, divisionOfficeVal) },
                        label = { Text("Designation (e.g. Teacher III)", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("setting_teacher_designation")
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = schoolNameVal,
                        onValueChange = { viewModel.saveTeacherMetadata(teacherNameVal, teacherDesignationVal, it, schoolDistrictVal, divisionOfficeVal) },
                        label = { Text("School Name", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("setting_school_name")
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = schoolDistrictVal,
                        onValueChange = { viewModel.saveTeacherMetadata(teacherNameVal, teacherDesignationVal, schoolNameVal, it, divisionOfficeVal) },
                        label = { Text("District", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("setting_school_district")
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = divisionOfficeVal,
                        onValueChange = { viewModel.saveTeacherMetadata(teacherNameVal, teacherDesignationVal, schoolNameVal, schoolDistrictVal, it) },
                        label = { Text("Division Office (e.g. Division of Masbate)", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("setting_division_office")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // CLASS ROSTER CONFIGURATION CARD
        val classRosterVal by viewModel.classRosterText.collectAsStateWithLifecycle()
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CLASS COHORT ROSTER CONFIGURATION",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manage and customize your classroom cohort list. Add student names (one name per line) to simulate real-time Class Server formative assessment quizzes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = classRosterVal,
                    onValueChange = { viewModel.classRosterText.value = it },
                    label = { Text("Class Roster List (One student per line)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().height(150.dp).testTag("setting_class_roster")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total students registered: ${classRosterVal.split("\n").filter { it.isNotBlank() }.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Developer Systems Architect & Solidarity Donation Protocols
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DEVELOPER SYSTEM ARCHITECT & DONATIONS",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF818CF8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Developer Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Small circular profile/badge icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF4F46E5).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Developer Profile",
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "JOSEPH DANIEL DURAN",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "BASKOG Lead Developer & Core Architect",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Conceptualized and engineered the BASKOG platform as a localized hypervisor for DepEd's Three-Term Year policy alignment and emergency response workflows.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                Spacer(modifier = Modifier.height(14.dp))

                // Donation Section
                Text(
                    text = "SUPPORT & DONATION PROTOCOLS",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "If BASKOG has streamlined your educational workflows, you can extend your support by sending a donation directly to the developer's GCash ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // GCash QR Block
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(6.dp)
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_gcash_qr_vertical_1782049643917),
                            contentDescription = "GCash Donation QR Code",
                            modifier = Modifier
                                .width(260.dp)
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "SCAN QR PH via GCASH / INSTAPAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // Security Log monitor
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B2E)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INTEGRITY LOG MONITOR",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF818CF8),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (logs.isNotEmpty()) {
                        TextButton(onClick = { showClearLogsConfirm = true }) {
                            Text("CLEAR LOGS", color = Color(0xFFEF4444).copy(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (logs.isEmpty()) {
                    Text("History clean. No active log telemetry generated.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                } else {
                    logs.take(20).forEach { log ->
                        val colorTag = when (log.eventType) {
                            "SECURITY" -> Color(0xFFEF4444)
                            "PARSER" -> Color(0xFF3B82F6)
                            "COMPILER" -> Color(0xFF10B981)
                            else -> Color(0xFF64748B)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colorTag.copy(0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(log.eventType, color = colorTag, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    
                                    val logTimeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                                    Text(logTimeStr, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(log.logMessage, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherProfileAvatar(
    picture: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    borderWidth: androidx.compose.ui.unit.Dp = 2.dp,
    borderColor: ComposeColor = Color(0xFF818CF8)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0xFF1E293B))
            .border(borderWidth, borderColor, androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (picture.startsWith("preset_") || picture.startsWith("local_")) {
            val (bgColor, icon) = when (picture) {
                "preset_1" -> Pair(Color(0xFF4F46E5), androidx.compose.material.icons.Icons.Default.AccountCircle)
                "preset_2" -> Pair(Color(0xFF10B981), androidx.compose.material.icons.Icons.Default.Face)
                "preset_3" -> Pair(Color(0xFFF59E0B), androidx.compose.material.icons.Icons.Default.Star)
                "preset_4" -> Pair(Color(0xFFEC4899), androidx.compose.material.icons.Icons.Default.Person)
                "local_1" -> Pair(Color(0xFF0F172A), androidx.compose.material.icons.Icons.Default.AccountBox)
                "local_2" -> Pair(Color(0xFF047857), androidx.compose.material.icons.Icons.Default.CheckCircle)
                "local_3" -> Pair(Color(0xFFB45309), androidx.compose.material.icons.Icons.Default.Home)
                "local_4" -> Pair(Color(0xFF6D28D9), androidx.compose.material.icons.Icons.Default.Star)
                "local_5" -> Pair(Color(0xFFBE123C), androidx.compose.material.icons.Icons.Default.Favorite)
                else -> Pair(Color(0xFF6366F1), androidx.compose.material.icons.Icons.Default.Person)
            }
            Box(
                modifier = Modifier.fillMaxSize().background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = picture,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        } else if (picture.isNotBlank()) {
            val painter = coil.compose.rememberAsyncImagePainter(
                model = picture,
                error = coil.compose.rememberAsyncImagePainter(model = androidx.compose.material.icons.Icons.Default.Warning)
            )
            androidx.compose.foundation.Image(
                painter = painter,
                contentDescription = "Custom Profile Picture",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF64748B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Person,
                    contentDescription = "Default Profile",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
    }
}
