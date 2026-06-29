package com.example

import com.example.ui.MainViewModel
import com.example.ui.StudentGrades

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

// ==========================================
// ELECTRONIC SPREADSHEET MODELS & COMMONS
// ==========================================

data class EditingCell(
    val studentName: String,
    val category: String, // "WW", "PT", "SA", "TE", "MAX_WW", "MAX_PT", "MAX_SA"
    val index: Int,
    val label: String,
    val initialValue: String
)

@Composable
fun SpreadsheetCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    backgroundColor: Color = Color.White,
    textColor: Color = Color.Black,
    fontWeight: FontWeight = FontWeight.Normal,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(width, height)
            .background(backgroundColor)
            .border(0.5.dp, Color(0xFFCCCCCC))
            .clickable(enabled = isClickable, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun MetadataItem(
    label: String,
    value: String,
    isReadOnly: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isReadOnly, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = if (value.isBlank()) "(Click to Edit)" else value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isReadOnly) Color.Black else Color(0xFF0F52BA),
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
        if (!isReadOnly) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "✏️", fontSize = 10.sp)
        }
    }
}

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

// ==========================================
// COMPLIANT DEPED ELECTRONIC CLASS RECORD
// ==========================================
@Composable
fun ClassRecordTab(viewModel: MainViewModel) {
    val scoresMap by viewModel.classRecordScores.collectAsStateWithLifecycle()
    val wwMaxScores by viewModel.wwMaxScores.collectAsStateWithLifecycle()
    val ptMaxScores by viewModel.ptMaxScores.collectAsStateWithLifecycle()
    val saMaxScores by viewModel.saMaxScores.collectAsStateWithLifecycle()
    
    val rosterText by viewModel.classRosterText.collectAsStateWithLifecycle()
    val studentsList = remember(rosterText) {
        rosterText.split("\n").map { it.trim() }.filter { it.isNotBlank() }.sorted()
    }

    // Editable Spreadsheet Metadata
    val teacherName by viewModel.teacherName.collectAsStateWithLifecycle()
    val schoolName by viewModel.schoolName.collectAsStateWithLifecycle()
    val divisionOffice by viewModel.divisionOffice.collectAsStateWithLifecycle()
    val planSubject by viewModel.planSubject.collectAsStateWithLifecycle()
    val lessonSection by viewModel.lessonSection.collectAsStateWithLifecycle()
    val currentTerm by viewModel.currentTerm.collectAsStateWithLifecycle()

    var region by remember { mutableStateOf(viewModel.prefs.getString("deped_region", "Region V (Bicol)") ?: "Region V (Bicol)") }
    var schoolId by remember { mutableStateOf(viewModel.prefs.getString("deped_school_id", "112501") ?: "112501") }
    var schoolYear by remember { mutableStateOf(viewModel.prefs.getString("deped_school_year", "SY 2026-2027") ?: "SY 2026-2027") }
    var gradingPolicy by remember { mutableStateOf(viewModel.prefs.getString("grading_policy_mode", "SY 2026-2027 (Transmuted)") ?: "SY 2026-2027 (Transmuted)") }

    var isMetadataExpanded by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<String?>(null) }

    var showClassSwitchConfirmDialog by remember { mutableStateOf(false) }
    var classSwitchTargetIndex by remember { mutableStateOf(-1) }

    if (showClassSwitchConfirmDialog && classSwitchTargetIndex != -1) {
        val targetName = viewModel.prefs.getString("lesson_section_$classSwitchTargetIndex", viewModel.DEFAULT_CLASS_NAMES[classSwitchTargetIndex]) ?: viewModel.DEFAULT_CLASS_NAMES[classSwitchTargetIndex]
        AlertDialog(
            onDismissRequest = { showClassSwitchConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Verify Roster Transition", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "You are about to switch the active class record sheet to: \"$targetName\".\n\nThis will load the stored roster, grades, and attendance metrics for this class section. Please verify that this is the correct class roster you intend to manage.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.selectClassRecordSheet(classSwitchTargetIndex)
                        showClassSwitchConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Proceed Switch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClassSwitchConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Dialog state for editing sheet cell
    var editingCell by remember { mutableStateOf<EditingCell?>(null) }
    var editingCellValue by remember { mutableStateOf("") }

    // Dialog state for editing metadata
    var editingMetaField by remember { mutableStateOf<String?>(null) }
    var editingMetaValue by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Policy Controls Toolbar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📖 DEPED CLASS RECORD SPREADSHEET",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "DO 15, s. 2026 compliant grading. Click metadata or cells to edit directly.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Policy Selector Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isTransmuted = gradingPolicy == "SY 2026-2027 (Transmuted)"
                    Box(
                        modifier = Modifier
                            .background(
                                if (isTransmuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                gradingPolicy = "SY 2026-2027 (Transmuted)"
                                viewModel.prefs.edit().putString("grading_policy_mode", gradingPolicy).apply()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "SY 26-27 (Transmuted)",
                            color = if (isTransmuted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (!isTransmuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                gradingPolicy = "SY 2027-2028 (Zero-Based)"
                                viewModel.prefs.edit().putString("grading_policy_mode", gradingPolicy).apply()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "SY 27-28 (Zero-Based)",
                            color = if (!isTransmuted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Multi-Class Sheet Dropdown Selector with Verification
            val activeClassIndex by viewModel.selectedClassIndex.collectAsStateWithLifecycle()
            var isDropdownExpanded by remember { mutableStateOf(false) }

            val activeSheetNames = (0..7).map { i ->
                viewModel.prefs.getString("lesson_section_$i", viewModel.DEFAULT_CLASS_NAMES[i]) ?: viewModel.DEFAULT_CLASS_NAMES[i]
            }
            val activeSheetSubjects = (0..7).map { i ->
                viewModel.prefs.getString("plan_subject_$i", viewModel.DEFAULT_CLASS_SUBJECTS[i]) ?: viewModel.DEFAULT_CLASS_SUBJECTS[i]
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(Color(0xFF0F172A), shape = RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Class List Icon",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ACTIVE SPREADSHEET SHEET",
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Sheet ${activeClassIndex + 1}: ${activeSheetNames[activeClassIndex]} (${activeSheetSubjects[activeClassIndex]})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Box {
                    Button(
                        onClick = { isDropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Switch Roster", fontSize = 11.sp, color = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Arrow Down",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B)).border(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        (0..7).forEach { i ->
                            val isSelected = activeClassIndex == i
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "Sheet ${i + 1}: ${activeSheetNames[i]}",
                                            color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Subject: ${activeSheetSubjects[i]}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                onClick = {
                                    isDropdownExpanded = false
                                    if (activeClassIndex != i) {
                                        classSwitchTargetIndex = i
                                        showClassSwitchConfirmDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Collapsible Header Section for Metadata
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isMetadataExpanded = !isMetadataExpanded }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DepEd Spreadsheet Header Metadata (Click to View/Edit)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (isMetadataExpanded) "Collapse ▲" else "Expand ▼",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isMetadataExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBBBBBB), shape = RoundedCornerShape(6.dp))
                        .background(Color(0xFFF9F9F9), shape = RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "OFFICIAL DEPED SPREADSHEET HEADERS (SY 2026-2027)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F2C59),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Strict Vertical list to prevent horizontal clashing or wrapping
                        MetadataItem(label = "REGION", value = region, onClick = { editingMetaField = "REGION"; editingMetaValue = region })
                        MetadataItem(label = "DIVISION OFFICE", value = divisionOffice, onClick = { editingMetaField = "DIVISION"; editingMetaValue = divisionOffice })
                        MetadataItem(label = "SCHOOL NAME", value = schoolName, onClick = { editingMetaField = "SCHOOL_NAME"; editingMetaValue = schoolName })
                        MetadataItem(label = "SCHOOL ID", value = schoolId, onClick = { editingMetaField = "SCHOOL_ID"; editingMetaValue = schoolId })
                        MetadataItem(label = "SCHOOL YEAR", value = schoolYear, onClick = { editingMetaField = "SCHOOL_YEAR"; editingMetaValue = schoolYear })
                        MetadataItem(label = "ACADEMIC TERM", value = currentTerm, isReadOnly = true)
                        MetadataItem(label = "GRADE & SECTION", value = lessonSection, onClick = { editingMetaField = "GRADE_SECTION"; editingMetaValue = lessonSection })
                        MetadataItem(label = "CLASS SUBJECT", value = planSubject, onClick = { editingMetaField = "SUBJECT"; editingMetaValue = planSubject })
                        MetadataItem(label = "ASSIGNED TEACHER", value = teacherName, onClick = { editingMetaField = "TEACHER"; editingMetaValue = teacherName })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // THE MAIN SPREADSHEET SCROLL CONTAINER
            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .border(1.dp, Color(0xFFDDDDDD))
            ) {
                // ==========================================
                // 1. LEFT COLUMN: FROZEN NAMES
                // ==========================================
                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .fillMaxHeight()
                        .verticalScroll(verticalScrollState)
                ) {
                    // Headers matching height of right side
                    SpreadsheetCell(
                        text = "LEARNERS' NAMES",
                        width = 160.dp,
                        height = 32.dp,
                        backgroundColor = Color(0xFF0F2C59),
                        textColor = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    SpreadsheetCell(
                        text = "(Alphabetical Sorted)",
                        width = 160.dp,
                        height = 32.dp,
                        backgroundColor = Color(0xFF0F2C59),
                        textColor = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    SpreadsheetCell(
                        text = "HIGHEST POSSIBLE SCORE",
                        width = 160.dp,
                        height = 40.dp,
                        backgroundColor = Color(0xFFEEEEEE),
                        textColor = Color.Black,
                        fontWeight = FontWeight.Bold
                    )

                    // Students
                    studentsList.forEachIndexed { idx, name ->
                        val activeStudent = selectedStudent ?: studentsList.firstOrNull()
                        val isSelected = activeStudent == name
                        val rowColor = if (isSelected) Color(0xFFDBEAFE) else (if (idx % 2 == 0) Color.White else Color(0xFFF7F9FB))
                        val textColor = if (isSelected) Color(0xFF1E3A8A) else Color.Black
                        SpreadsheetCell(
                            text = "${idx + 1}. $name",
                            width = 160.dp,
                            height = 44.dp,
                            backgroundColor = rowColor,
                            textColor = textColor,
                            fontWeight = FontWeight.Bold,
                            isClickable = true,
                            onClick = { selectedStudent = name }
                        )
                    }
                }

                // ==========================================
                // 2. RIGHT COLUMN: SCROLLABLE DATA CELLS
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(verticalScrollState)
                ) {
                    // Row 1: Category Titles
                    Row {
                        SpreadsheetCell(text = "WRITTEN / ORAL WORKS (20%)", width = (50 * 5 + 55 * 3).dp, height = 32.dp, backgroundColor = Color(0xFFFFF2CC), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "PRODUCT / PERFORMANCE TASKS (50%)", width = (50 * 3 + 55 * 3).dp, height = 32.dp, backgroundColor = Color(0xFFE2EFDA), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "SUMMATIVE TESTS / TERM EXAM (30%)", width = (50 * 3 + 55 * 3).dp, height = 32.dp, backgroundColor = Color(0xFFF2F2F2), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "TERM PERFORMANCE INDEX", width = 240.dp, height = 32.dp, backgroundColor = Color(0xFFDDEBF7), fontWeight = FontWeight.Bold)
                    }

                    // Row 2: Sub-column Labels
                    Row {
                        // WW
                        SpreadsheetCell(text = "WW1", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFFFF9E6))
                        SpreadsheetCell(text = "WW2", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFFFF9E6))
                        SpreadsheetCell(text = "WW3", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFFFF9E6))
                        SpreadsheetCell(text = "WW4", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFFFF9E6))
                        SpreadsheetCell(text = "WW5", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFFFF9E6))
                        SpreadsheetCell(text = "Total", width = 55.dp, height = 32.dp, backgroundColor = Color(0xFFFFE599), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "PS", width = 55.dp, height = 32.dp, backgroundColor = Color(0xFFFFE599), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "WS", width = 55.dp, height = 32.dp, backgroundColor = Color(0xFFFFE599), fontWeight = FontWeight.Bold)

                        // PT
                        SpreadsheetCell(text = "PT1", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFF2F9EC))
                        SpreadsheetCell(text = "PT2", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFF2F9EC))
                        SpreadsheetCell(text = "PT3", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFF2F9EC))
                        SpreadsheetCell(text = "Total", width = 55.dp, height = 32.dp, backgroundColor = Color(0xFFC6E0B4), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "PS", width = 55.dp, height = 32.dp, backgroundColor = Color(0xFFC6E0B4), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "WS", width = 55.dp, height = 32.dp, backgroundColor = Color(0xFFC6E0B4), fontWeight = FontWeight.Bold)

                        // SA
                        SpreadsheetCell(text = "SA1", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFF9F9F9))
                        SpreadsheetCell(text = "SA2", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFF9F9F9))
                        SpreadsheetCell(text = "TE", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFF9F9F9))
                        SpreadsheetCell(text = "Total", width = 55.dp, height = 32.dp, backgroundColor = Color(0xFFD9D9D9), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "PS", width = 55.dp, height = 32.dp, backgroundColor = Color(0xFFD9D9D9), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "WS", width = 55.dp, height = 32.dp, backgroundColor = Color(0xFFD9D9D9), fontWeight = FontWeight.Bold)

                        // Final metrics
                        SpreadsheetCell(text = "Initial Grade", width = 80.dp, height = 32.dp, backgroundColor = Color(0xFFBDD7EE), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "Term Grade", width = 80.dp, height = 32.dp, backgroundColor = Color(0xFF9BC2E6), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "Description", width = 80.dp, height = 32.dp, backgroundColor = Color(0xFF9BC2E6), fontWeight = FontWeight.Bold)
                    }

                    // Row 3: Highest Possible Score (HPS) Cells
                    Row {
                        // WW Max
                        wwMaxScores.forEachIndexed { i, score ->
                            SpreadsheetCell(
                                text = score.toString(),
                                width = 50.dp,
                                height = 40.dp,
                                backgroundColor = Color(0xFFE2E2E2),
                                fontWeight = FontWeight.Bold,
                                isClickable = true,
                                onClick = { editingCell = EditingCell("", "MAX_WW", i, "WW${i+1} Max Score", score.toString()) }
                            )
                        }
                        val wwMaxSum = wwMaxScores.sum().coerceAtLeast(1)
                        SpreadsheetCell(text = wwMaxSum.toString(), width = 55.dp, height = 40.dp, backgroundColor = Color(0xFFD2D2D2), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "100.0", width = 55.dp, height = 40.dp, backgroundColor = Color(0xFFD2D2D2), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "20.0", width = 55.dp, height = 40.dp, backgroundColor = Color(0xFFD2D2D2), fontWeight = FontWeight.Bold)

                        // PT Max
                        ptMaxScores.forEachIndexed { i, score ->
                            SpreadsheetCell(
                                text = score.toString(),
                                width = 50.dp,
                                height = 40.dp,
                                backgroundColor = Color(0xFFD4E6D4),
                                fontWeight = FontWeight.Bold,
                                isClickable = true,
                                onClick = { editingCell = EditingCell("", "MAX_PT", i, "PT${i+1} Max Score", score.toString()) }
                            )
                        }
                        val ptMaxSum = ptMaxScores.sum().coerceAtLeast(1)
                        SpreadsheetCell(text = ptMaxSum.toString(), width = 55.dp, height = 40.dp, backgroundColor = Color(0xFFC0DCC0), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "100.0", width = 55.dp, height = 40.dp, backgroundColor = Color(0xFFC0DCC0), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "50.0", width = 55.dp, height = 40.dp, backgroundColor = Color(0xFFC0DCC0), fontWeight = FontWeight.Bold)

                        // SA Max
                        saMaxScores.forEachIndexed { i, score ->
                            val label = if (i == 2) "TE" else "SA${i+1}"
                            SpreadsheetCell(
                                text = score.toString(),
                                width = 50.dp,
                                height = 40.dp,
                                backgroundColor = Color(0xFFE6D4E6),
                                fontWeight = FontWeight.Bold,
                                isClickable = true,
                                onClick = { editingCell = EditingCell("", "MAX_SA", i, "$label Max Score", score.toString()) }
                            )
                        }
                        val saMaxSum = saMaxScores.sum().coerceAtLeast(1)
                        SpreadsheetCell(text = saMaxSum.toString(), width = 55.dp, height = 40.dp, backgroundColor = Color(0xFFDCC0DC), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "100.0", width = 55.dp, height = 40.dp, backgroundColor = Color(0xFFDCC0DC), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "30.0", width = 55.dp, height = 40.dp, backgroundColor = Color(0xFFDCC0DC), fontWeight = FontWeight.Bold)

                        // Empty end fields for HPS row
                        SpreadsheetCell(text = "100.0", width = 80.dp, height = 40.dp, backgroundColor = Color(0xFFBDD7EE), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "100.0", width = 80.dp, height = 40.dp, backgroundColor = Color(0xFF9BC2E6), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "-", width = 80.dp, height = 40.dp, backgroundColor = Color(0xFF9BC2E6), fontWeight = FontWeight.Bold)
                    }

                    // Student Rows
                    studentsList.forEachIndexed { sIdx, name ->
                        val activeStudent = selectedStudent ?: studentsList.firstOrNull()
                        val isSelected = activeStudent == name
                        val rowColor = if (isSelected) Color(0xFFDBEAFE) else (if (sIdx % 2 == 0) Color.White else Color(0xFFF7F9FB))
                        val cellTextColor = if (isSelected) Color(0xFF1E3A8A) else Color.Black
                        val grades = scoresMap[name] ?: StudentGrades(name, listOf("", "", "", "", ""), listOf("", "", ""), listOf("", ""), "")

                        // Calculations
                        val wwRawValues = grades.wwScores.map { it.toFloatOrNull() }
                        val wwTotalRaw = wwRawValues.filterNotNull().sum()
                        val wwTotalMax = grades.wwScores.mapIndexed { i, v -> if (v.isNotBlank()) wwMaxScores[i] else 0 }.sum().coerceAtLeast(1)
                        val wwPS = if (wwTotalMax > 0) (wwTotalRaw / wwTotalMax * 100f).coerceIn(0f, 100f) else 0f
                        val wwWS = wwPS * 0.20f

                        val ptRawValues = grades.ptScores.map { it.toFloatOrNull() }
                        val ptTotalRaw = ptRawValues.filterNotNull().sum()
                        val ptTotalMax = grades.ptScores.mapIndexed { i, v -> if (v.isNotBlank()) ptMaxScores[i] else 0 }.sum().coerceAtLeast(1)
                        val ptPS = if (ptTotalMax > 0) (ptTotalRaw / ptTotalMax * 100f).coerceIn(0f, 100f) else 0f
                        val ptWS = ptPS * 0.50f

                        val saRawValues = grades.saScores.map { it.toFloatOrNull() }
                        val saTotalRaw = saRawValues.filterNotNull().sum() + (grades.termExamScore.toFloatOrNull() ?: 0f)
                        val saTotalMax = grades.saScores.mapIndexed { i, v -> if (v.isNotBlank()) saMaxScores[i] else 0 }.sum() + (if (grades.termExamScore.isNotBlank()) saMaxScores[2] else 0)
                        val saTotalMaxComp = saTotalMax.coerceAtLeast(1)
                        val saPS = if (saTotalMaxComp > 0) (saTotalRaw / saTotalMaxComp * 100f).coerceIn(0f, 100f) else 0f
                        val saWS = saPS * 0.30f

                        val initialGrade = wwWS + ptWS + saWS
                        val isTransmuted = gradingPolicy == "SY 2026-2027 (Transmuted)"
                        val rawTermGrade = if (isTransmuted) transmuteSY2026(initialGrade) else initialGrade.roundToInt().coerceIn(0, 100)
                        
                        // Transitional Floor Rule
                        val isFloorApplied = (initialGrade >= 70.0f && rawTermGrade < 75)
                        val finalTermGrade = if (isFloorApplied) 75 else rawTermGrade

                        val description = when {
                            finalTermGrade >= 90 -> "Outstanding"
                            finalTermGrade >= 85 -> "Very Satisfactory"
                            finalTermGrade >= 80 -> "Satisfactory"
                            finalTermGrade >= 75 -> "Fairly Sat."
                            else -> "Need Exp."
                        }

                        Row {
                            // WW cells
                            grades.wwScores.forEachIndexed { i, sc ->
                                SpreadsheetCell(
                                    text = sc,
                                    width = 50.dp,
                                    height = 44.dp,
                                    backgroundColor = rowColor,
                                    textColor = cellTextColor,
                                    isClickable = true,
                                    onClick = { editingCell = EditingCell(name, "WW", i, "Written Work ${i+1} score for $name", sc) }
                                )
                            }
                            SpreadsheetCell(text = String.format("%.1f", wwTotalRaw), width = 55.dp, height = 44.dp, backgroundColor = Color(0xFFFFFDF0), fontWeight = FontWeight.Bold, textColor = cellTextColor)
                            SpreadsheetCell(text = String.format("%.1f", wwPS), width = 55.dp, height = 44.dp, backgroundColor = Color(0xFFFFFDF0), textColor = cellTextColor)
                            SpreadsheetCell(text = String.format("%.1f", wwWS), width = 55.dp, height = 44.dp, backgroundColor = Color(0xFFFFF5CC), fontWeight = FontWeight.Bold, textColor = cellTextColor)

                            // PT cells
                            grades.ptScores.forEachIndexed { i, sc ->
                                SpreadsheetCell(
                                    text = sc,
                                    width = 50.dp,
                                    height = 44.dp,
                                    backgroundColor = rowColor,
                                    textColor = cellTextColor,
                                    isClickable = true,
                                    onClick = { editingCell = EditingCell(name, "PT", i, "Performance Task ${i+1} score for $name", sc) }
                                )
                            }
                            SpreadsheetCell(text = String.format("%.1f", ptTotalRaw), width = 55.dp, height = 44.dp, backgroundColor = Color(0xFFF9FBF6), fontWeight = FontWeight.Bold, textColor = cellTextColor)
                            SpreadsheetCell(text = String.format("%.1f", ptPS), width = 55.dp, height = 44.dp, backgroundColor = Color(0xFFF9FBF6), textColor = cellTextColor)
                            SpreadsheetCell(text = String.format("%.1f", ptWS), width = 55.dp, height = 44.dp, backgroundColor = Color(0xFFE2EFDA), fontWeight = FontWeight.Bold, textColor = cellTextColor)

                            // SA cells
                            grades.saScores.forEachIndexed { i, sc ->
                                SpreadsheetCell(
                                    text = sc,
                                    width = 50.dp,
                                    height = 44.dp,
                                    backgroundColor = rowColor,
                                    textColor = cellTextColor,
                                    isClickable = true,
                                    onClick = { editingCell = EditingCell(name, "SA", i, "Summative Test ${i+1} score for $name", sc) }
                                )
                            }
                            SpreadsheetCell(
                                text = grades.termExamScore,
                                width = 50.dp,
                                height = 44.dp,
                                backgroundColor = rowColor,
                                textColor = cellTextColor,
                                isClickable = true,
                                onClick = { editingCell = EditingCell(name, "TE", 0, "Term Exam score for $name", grades.termExamScore) }
                            )
                            SpreadsheetCell(text = String.format("%.1f", saTotalRaw), width = 55.dp, height = 44.dp, backgroundColor = Color(0xFFFAFAFA), fontWeight = FontWeight.Bold, textColor = cellTextColor)
                            SpreadsheetCell(text = String.format("%.1f", saPS), width = 55.dp, height = 44.dp, backgroundColor = Color(0xFFFAFAFA), textColor = cellTextColor)
                            SpreadsheetCell(text = String.format("%.1f", saWS), width = 55.dp, height = 44.dp, backgroundColor = Color(0xFFE8E8E8), fontWeight = FontWeight.Bold, textColor = cellTextColor)

                            // Summary columns
                            SpreadsheetCell(text = String.format("%.2f", initialGrade), width = 80.dp, height = 44.dp, backgroundColor = Color(0xFFD9E1F2), fontWeight = FontWeight.Bold)
                            val gradeColor = when {
                                isFloorApplied -> Color(0xFFFFF2CC)
                                finalTermGrade >= 75 -> Color(0xFFE2EFDA)
                                else -> Color(0xFFFCE4D6)
                            }
                            val floorTextSuffix = if (isFloorApplied) " (Floor)" else ""
                            SpreadsheetCell(text = "$finalTermGrade$floorTextSuffix", width = 80.dp, height = 44.dp, backgroundColor = gradeColor, fontWeight = FontWeight.Bold)
                            SpreadsheetCell(text = description, width = 80.dp, height = 44.dp, backgroundColor = gradeColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    val activeStudentName = selectedStudent ?: studentsList.firstOrNull() ?: ""
    if (activeStudentName.isNotBlank()) {
        SelectedStudentPortfolio(studentName = activeStudentName, viewModel = viewModel)
    }

    // Dialog for editing score cell
    editingCell?.let { cell ->
        AlertDialog(
            onDismissRequest = { editingCell = null },
            title = { Text(cell.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Student Name: ${if (cell.studentName.isBlank()) "ALL (HPS Row)" else cell.studentName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = editingCellValue,
                        onValueChange = { editingCellValue = it },
                        label = { Text("Enter Value") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedVal = editingCellValue.trim()
                        if (cell.category.startsWith("MAX_")) {
                            val intVal = trimmedVal.toIntOrNull() ?: 0
                            val catCode = cell.category.substringAfter("MAX_")
                            viewModel.updateMaxScore(catCode, cell.index, intVal)
                        } else {
                            viewModel.updateStudentGrade(cell.studentName, cell.category, cell.index, trimmedVal)
                        }
                        editingCell = null
                    }
                ) {
                    Text("Apply Score")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCell = null }) {
                    Text("Cancel")
                }
            }
        )
        LaunchedEffect(cell) {
            editingCellValue = cell.initialValue
        }
    }

    // Dialog for editing metadata
    editingMetaField?.let { field ->
        AlertDialog(
            onDismissRequest = { editingMetaField = null },
            title = { Text("Edit $field", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editingMetaValue,
                    onValueChange = { editingMetaValue = it },
                    label = { Text("Enter $field") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalVal = editingMetaValue.trim()
                        when (field) {
                            "REGION" -> {
                                region = finalVal
                                viewModel.prefs.edit().putString("deped_region", finalVal).apply()
                            }
                            "SCHOOL_ID" -> {
                                schoolId = finalVal
                                viewModel.prefs.edit().putString("deped_school_id", finalVal).apply()
                            }
                            "SCHOOL_YEAR" -> {
                                schoolYear = finalVal
                                viewModel.prefs.edit().putString("deped_school_year", finalVal).apply()
                            }
                            "SCHOOL_NAME" -> {
                                viewModel.schoolName.value = finalVal
                                viewModel.prefs.edit().putString("school_name", finalVal).apply()
                            }
                            "DIVISION" -> {
                                viewModel.divisionOffice.value = finalVal
                                viewModel.prefs.edit().putString("division_office", finalVal).apply()
                            }
                            "GRADE_SECTION" -> {
                                viewModel.lessonSection.value = finalVal
                                viewModel.prefs.edit().putString("lesson_section", finalVal).apply()
                            }
                            "SUBJECT" -> {
                                viewModel.planSubject.value = finalVal
                                viewModel.prefs.edit().putString("plan_subject", finalVal).apply()
                            }
                            "TEACHER" -> {
                                viewModel.teacherName.value = finalVal
                                viewModel.prefs.edit().putString("teacher_name", finalVal).apply()
                            }
                        }
                        viewModel.logSystemEvent("POLICY_EVENT", "$field modified in electronic spreadsheet header to $finalVal")
                        editingMetaField = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMetaField = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// REAL-TIME CLASS ATTENDANCE SPREADSHEET (SF-2)
// ==========================================
@Composable
fun AttendanceTab(viewModel: MainViewModel) {
    val attendanceMap by viewModel.attendanceMap.collectAsStateWithLifecycle()
    val rosterText by viewModel.classRosterText.collectAsStateWithLifecycle()
    val studentsList = remember(rosterText) {
        rosterText.split("\n").map { it.trim() }.filter { it.isNotBlank() }.sorted()
    }

    // Metadata
    val teacherName by viewModel.teacherName.collectAsStateWithLifecycle()
    val schoolName by viewModel.schoolName.collectAsStateWithLifecycle()
    val divisionOffice by viewModel.divisionOffice.collectAsStateWithLifecycle()
    val planSubject by viewModel.planSubject.collectAsStateWithLifecycle()
    val lessonSection by viewModel.lessonSection.collectAsStateWithLifecycle()

    var region by remember { mutableStateOf(viewModel.prefs.getString("deped_region", "Region V (Bicol)") ?: "Region V (Bicol)") }
    var schoolId by remember { mutableStateOf(viewModel.prefs.getString("deped_school_id", "112501") ?: "112501") }
    var schoolYear by remember { mutableStateOf(viewModel.prefs.getString("deped_school_year", "SY 2026-2027") ?: "SY 2026-2027") }

    var isAttendanceMetadataExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title Banner
            Text(
                text = "👥 DAILY ATTENDANCE REGISTER (SF-2)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Interactive School Form 2 (SF-2) grid. Tap any cell to cycle status: P (Present ✔) ➔ A (Absent ❌) ➔ L (Late ⚠) ➔ P.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Collapsible Header Section for Attendance Metadata
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAttendanceMetadataExpanded = !isAttendanceMetadataExpanded }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👥", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "School Form 2 Metadata Headers (Click to View)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (isAttendanceMetadataExpanded) "Collapse ▲" else "Expand ▼",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isAttendanceMetadataExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBBBBBB), shape = RoundedCornerShape(6.dp))
                        .background(Color(0xFFFAF9F6), shape = RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MetadataItem(label = "REGION", value = region, isReadOnly = true)
                        MetadataItem(label = "DIVISION OFFICE", value = divisionOffice, isReadOnly = true)
                        MetadataItem(label = "SCHOOL NAME", value = schoolName, isReadOnly = true)
                        MetadataItem(label = "SCHOOL ID", value = schoolId, isReadOnly = true)
                        MetadataItem(label = "SCHOOL YEAR", value = schoolYear, isReadOnly = true)
                        MetadataItem(label = "GRADE & SECTION", value = lessonSection, isReadOnly = true)
                        MetadataItem(label = "SUBJECT / CLASS", value = planSubject, isReadOnly = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // THE SCROLL CONTAINER
            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .border(1.dp, Color(0xFFDDDDDD))
            ) {
                // ==========================================
                // 1. LEFT COLUMN: FROZEN NAMES
                // ==========================================
                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .fillMaxHeight()
                        .verticalScroll(verticalScrollState)
                ) {
                    // Two rows of headers to match right side
                    SpreadsheetCell(
                        text = "LEARNERS' NAMES",
                        width = 160.dp,
                        height = 32.dp,
                        backgroundColor = Color(0xFF4A5568),
                        textColor = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    SpreadsheetCell(
                        text = "(Alphabetical Sorted)",
                        width = 160.dp,
                        height = 32.dp,
                        backgroundColor = Color(0xFF4A5568),
                        textColor = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    // Students list
                    studentsList.forEachIndexed { idx, name ->
                        val rowColor = if (idx % 2 == 0) Color.White else Color(0xFFF7F9FB)
                        SpreadsheetCell(
                            text = "${idx + 1}. $name",
                            width = 160.dp,
                            height = 44.dp,
                            backgroundColor = rowColor,
                            textColor = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ==========================================
                // 2. RIGHT COLUMN: SCROLLABLE DAYS
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(verticalScrollState)
                ) {
                    // Row 1: Session Header spans
                    Row {
                        SpreadsheetCell(text = "DAILY INSTRUCTIONAL DAY SESSIONS", width = (42 * 15).dp, height = 32.dp, backgroundColor = Color(0xFFEDF2F7), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "SUMMARY SCORES (SF-2)", width = 210.dp, height = 32.dp, backgroundColor = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold)
                    }

                    // Row 2: Sub-column Labels
                    Row {
                        for (day in 1..15) {
                            SpreadsheetCell(text = "Day $day", width = 42.dp, height = 32.dp, backgroundColor = Color(0xFFF7FAFC), fontWeight = FontWeight.Bold)
                        }
                        SpreadsheetCell(text = "Present", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFC6E0B4), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "Absent", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFF8CBAD), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "Late", width = 50.dp, height = 32.dp, backgroundColor = Color(0xFFFFD966), fontWeight = FontWeight.Bold)
                        SpreadsheetCell(text = "% Rate", width = 60.dp, height = 32.dp, backgroundColor = Color(0xFFBDD7EE), fontWeight = FontWeight.Bold)
                    }

                    // Student Rows
                    studentsList.forEachIndexed { sIdx, name ->
                        val rowColor = if (sIdx % 2 == 0) Color.White else Color(0xFFF7F9FB)
                        val days = attendanceMap[name] ?: List(15) { "P" }

                        // Calculations
                        val pCount = days.count { it == "P" }
                        val aCount = days.count { it == "A" }
                        val lCount = days.count { it == "L" }
                        val totalSessions = pCount + aCount + lCount
                        val attendanceRate = if (totalSessions > 0) (pCount.toFloat() / totalSessions.toFloat() * 100f) else 100f

                        Row {
                            // Day status cells
                            days.forEachIndexed { dIdx, status ->
                                val (statusText, statusBg, statusFg) = when (status) {
                                    "P" -> Triple("✔", Color(0xFFE2EFDA), Color(0xFF38A169))
                                    "A" -> Triple("❌", Color(0xFFFCE4D6), Color(0xFFE53E3E))
                                    "L" -> Triple("⚠", Color(0xFFFFF2CC), Color(0xFFDD6B20))
                                    else -> Triple("-", rowColor, Color.Gray)
                                }
                                SpreadsheetCell(
                                    text = statusText,
                                    width = 42.dp,
                                    height = 44.dp,
                                    backgroundColor = statusBg,
                                    textColor = statusFg,
                                    fontWeight = FontWeight.Bold,
                                    isClickable = true,
                                    onClick = {
                                        val nextStatus = when (status) {
                                            "P" -> "A"
                                            "A" -> "L"
                                            else -> "P"
                                        }
                                        viewModel.updateAttendance(name, dIdx, nextStatus)
                                    }
                                )
                            }

                            // Summary cells
                            SpreadsheetCell(text = pCount.toString(), width = 50.dp, height = 44.dp, backgroundColor = Color(0xFFE2EFDA), textColor = Color(0xFF276749), fontWeight = FontWeight.Bold)
                            SpreadsheetCell(text = aCount.toString(), width = 50.dp, height = 44.dp, backgroundColor = Color(0xFFFCE4D6), textColor = Color(0xFF9B2C2C), fontWeight = FontWeight.Bold)
                            SpreadsheetCell(text = lCount.toString(), width = 50.dp, height = 44.dp, backgroundColor = Color(0xFFFFF2CC), textColor = Color(0xFF9C4221), fontWeight = FontWeight.Bold)
                            
                            val rateColor = if (attendanceRate >= 90f) Color(0xFFD9E1F2) else Color(0xFFFFF5F5)
                            SpreadsheetCell(text = String.format("%.1f%%", attendanceRate), width = 60.dp, height = 44.dp, backgroundColor = rateColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedStudentPortfolio(
    studentName: String,
    viewModel: MainViewModel
) {
    val scoresMap by viewModel.classRecordScores.collectAsStateWithLifecycle()
    val wwMaxScores by viewModel.wwMaxScores.collectAsStateWithLifecycle()
    val ptMaxScores by viewModel.ptMaxScores.collectAsStateWithLifecycle()
    val saMaxScores by viewModel.saMaxScores.collectAsStateWithLifecycle()
    val anecdotalAll by viewModel.anecdotalRecords.collectAsStateWithLifecycle()
    
    val studentAnecdotals = remember(anecdotalAll, studentName) {
        anecdotalAll.filter { it.studentName == studentName }
    }

    val grades = scoresMap[studentName] ?: StudentGrades(studentName, listOf("", "", "", "", ""), listOf("", "", ""), listOf("", ""), "")

    // Calculations for the selected student to show in the Dashboard
    val wwRawValues = grades.wwScores.map { it.toFloatOrNull() }
    val wwTotalRaw = wwRawValues.filterNotNull().sum()
    val wwTotalMax = grades.wwScores.mapIndexed { i, v -> if (v.isNotBlank()) wwMaxScores[i] else 0 }.sum().coerceAtLeast(1)
    val wwPS = if (wwTotalMax > 0) (wwTotalRaw / wwTotalMax * 100f).coerceIn(0f, 100f) else 0f
    val wwWS = wwPS * 0.20f

    val ptRawValues = grades.ptScores.map { it.toFloatOrNull() }
    val ptTotalRaw = ptRawValues.filterNotNull().sum()
    val ptTotalMax = grades.ptScores.mapIndexed { i, v -> if (v.isNotBlank()) ptMaxScores[i] else 0 }.sum().coerceAtLeast(1)
    val ptPS = if (ptTotalMax > 0) (ptTotalRaw / ptTotalMax * 100f).coerceIn(0f, 100f) else 0f
    val ptWS = ptPS * 0.50f

    val saRawValues = grades.saScores.map { it.toFloatOrNull() }
    val saTotalRaw = saRawValues.filterNotNull().sum() + (grades.termExamScore.toFloatOrNull() ?: 0f)
    val saTotalMax = grades.saScores.mapIndexed { i, v -> if (v.isNotBlank()) saMaxScores[i] else 0 }.sum() + (if (grades.termExamScore.isNotBlank()) saMaxScores[2] else 0)
    val saTotalMaxComp = saTotalMax.coerceAtLeast(1)
    val saPS = if (saTotalMaxComp > 0) (saTotalRaw / saTotalMaxComp * 100f).coerceIn(0f, 100f) else 0f
    val saWS = saPS * 0.30f

    val initialGrade = wwWS + ptWS + saWS
    val gradingPolicy = viewModel.prefs.getString("grading_policy_mode", "SY 2026-2027 (Transmuted)") ?: "SY 2026-2027 (Transmuted)"
    val isTransmuted = gradingPolicy == "SY 2026-2027 (Transmuted)"
    val rawTermGrade = if (isTransmuted) transmuteSY2026(initialGrade) else initialGrade.roundToInt().coerceIn(0, 100)
    
    // Transitional Floor Rule
    val isFloorApplied = (initialGrade >= 70.0f && rawTermGrade < 75)
    val finalTermGrade = if (isFloorApplied) 75 else rawTermGrade

    val description = when {
        finalTermGrade >= 90 -> "Outstanding"
        finalTermGrade >= 85 -> "Very Satisfactory"
        finalTermGrade >= 80 -> "Satisfactory"
        finalTermGrade >= 75 -> "Fairly Sat."
        else -> "Needs Improvement"
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var incidentType by remember { mutableStateOf("Observed Behavior") }
    var actionTaken by remember { mutableStateOf("") }
    var recordNotes by remember { mutableStateOf("") }

    // Media attachments in active adding state
    var selectedPhotoPreset by remember { mutableStateOf<String?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var simulatedAudioSeconds by remember { mutableStateOf(0) }
    var recordedAudioPath by remember { mutableStateOf<String?>(null) }

    // Image Presets for simulated capture
    val photoPresets = listOf(
        "None" to null,
        "Classwork Rubric" to "simulated://photo/rubric",
        "Science Project Draft" to "simulated://photo/science",
        "Group Work Cooperation" to "simulated://photo/cooperation",
        "Exam Paper Scan" to "simulated://photo/exam"
    )

    // Handle simulated recording ticker
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            simulatedAudioSeconds = 0
            while (isRecordingAudio) {
                kotlinx.coroutines.delay(1000)
                simulatedAudioSeconds++
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Responsive Vertical Stack for Header to prevent button squeezing
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎓", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SELECTED LEARNER PORTFOLIO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = studentName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Button(
                    onClick = {
                        incidentType = "Observed Behavior"
                        actionTaken = ""
                        recordNotes = ""
                        selectedPhotoPreset = null
                        recordedAudioPath = null
                        showAddDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("📝", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Anecdotal Note / PACE Log", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stack layout vertically to prevent horizontal clashing and squeezing on mobile screens
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PANEL 1: COMPACT PERFORMANCE DASHBOARD (PACE DO 15 s. 2026 Compliant)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "📊 GRADING WEIGHT ANALYSIS (ADLAW/PACE)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // 1. WW
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Written Works (20%):", fontSize = 11.sp, color = Color.Gray)
                            Text("${String.format("%.1f", wwTotalRaw)}/${wwTotalMax} (${String.format("%.1f", wwPS)}%) ➔ WS: ${String.format("%.1f", wwWS)}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(
                            progress = { wwPS / 100f },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(4.dp),
                            color = Color(0xFFFFC107),
                            trackColor = Color(0xFFFFF2CC)
                        )

                        // 2. PT
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Performance Tasks (50%):", fontSize = 11.sp, color = Color.Gray)
                            Text("${String.format("%.1f", ptTotalRaw)}/${ptTotalMax} (${String.format("%.1f", ptPS)}%) ➔ WS: ${String.format("%.1f", ptWS)}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(
                            progress = { ptPS / 100f },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(4.dp),
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFFE2EFDA)
                        )

                        // 3. SA
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Summative Tests & Exam (30%):", fontSize = 11.sp, color = Color.Gray)
                            Text("${String.format("%.1f", saTotalRaw)}/${saTotalMax} (${String.format("%.1f", saPS)}%) ➔ WS: ${String.format("%.1f", saWS)}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(
                            progress = { saPS / 100f },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(4.dp),
                            color = Color(0xFF9C27B0),
                            trackColor = Color(0xFFF2F2F2)
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Final Grades Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Initial Raw Grade:", fontSize = 11.sp, color = Color.Gray)
                                Text("${String.format("%.2f", initialGrade)}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Final Term Grade:", fontSize = 11.sp, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$finalTermGrade", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    if (isFloorApplied) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("⚠️ (Floor)", fontSize = 9.sp, color = Color(0xFFDD6B20), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (finalTermGrade >= 75) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PACE Standing: $description",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (finalTermGrade >= 75) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // PANEL 2: ANECDOTAL RECORDS CHRONOLOGY (with Multimedia display)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "📚 ANECDOTAL TIMELINE & MULTIMEDIA LOGS (${studentAnecdotals.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (studentAnecdotals.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📭", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("No anecdotal logs recorded yet.", fontSize = 11.sp, color = Color.Gray)
                                    Text("Add an observed behavior or PACE rubric note.", fontSize = 10.sp, color = Color.LightGray)
                                }
                            }
                        } else {
                            // Expand dynamically using standard layout without nested vertical scroll modifiers
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                studentAnecdotals.forEach { item ->
                                    AnecdotalRecordRow(item = item, onDelete = { viewModel.deleteAnecdotalRecord(item.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog to Add Anecdotal Record
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log Anecdotal & Multimedia Record", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Learner: $studentName", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    // Category
                    Text("Incident/Observation Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Observed Behavior", "PACE Evaluation Note", "Emergency/EiE", "Exceptional Work").forEach { cat ->
                            val isSel = incidentType == cat
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { incidentType = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = recordNotes,
                        onValueChange = { recordNotes = it },
                        label = { Text("Observed Narrative / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    // Action Taken
                    OutlinedTextField(
                        value = actionTaken,
                        onValueChange = { actionTaken = it },
                        label = { Text("Action Taken / PACE Way Forward") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // MULTIMEDIA ATTACHMENTS SECTION
                    Text("🖼️ MULTIMEDIA ATTACHMENTS (SIMULATED OR LOCAL)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Image attachment selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Attach Photo Artifact:", fontSize = 10.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(6.dp))
                                    .clickable {
                                        // Cycle preset photos to simulate attaching a file
                                        val curIdx = photoPresets.indexOfFirst { it.second == selectedPhotoPreset }
                                        val nextIdx = (curIdx + 1) % photoPresets.size
                                        selectedPhotoPreset = photoPresets[nextIdx].second
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val label = photoPresets.firstOrNull { it.second == selectedPhotoPreset }?.first ?: "None"
                                Text(
                                    text = "📸 $label",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedPhotoPreset != null) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }

                        // Audio recording attachment simulator
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Record Voice Note:", fontSize = 10.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(
                                        if (isRecordingAudio) Color(0xFFFCE4D6) else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isRecordingAudio) Color(0xFFE53E3E) else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        if (!isRecordingAudio) {
                                            isRecordingAudio = true
                                        } else {
                                            isRecordingAudio = false
                                            recordedAudioPath = "simulated://audio/voicenote_${System.currentTimeMillis()}"
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isRecordingAudio) {
                                        Text("🔴 Rec ($simulatedAudioSeconds s)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9B2C2C))
                                    } else {
                                        val audioLabel = if (recordedAudioPath != null) "Recorded (12s)" else "🎙️ Start Mic"
                                        Text(
                                            text = audioLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (recordedAudioPath != null) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (recordNotes.isNotBlank()) {
                            val finalAudioDur = if (recordedAudioPath != null) 12 else 0
                            viewModel.addAnecdotalRecord(
                                studentName = studentName,
                                incident = incidentType,
                                actionTaken = actionTaken.trim(),
                                notes = recordNotes.trim(),
                                imagePath = selectedPhotoPreset,
                                audioPath = recordedAudioPath,
                                audioDuration = finalAudioDur
                            )
                        }
                        showAddDialog = false
                    }
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AnecdotalRecordRow(item: com.example.ui.AnecdotalRecord, onDelete: () -> Unit) {
    var isPlayingAudio by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableStateOf(0f) }

    // Simulated audio playback progress ticking
    LaunchedEffect(isPlayingAudio) {
        if (isPlayingAudio) {
            audioProgress = 0f
            while (audioProgress < 1f) {
                kotlinx.coroutines.delay(100)
                audioProgress += 0.10f
            }
            isPlayingAudio = false
            audioProgress = 0f
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        border = BorderStroke(0.5.dp, Color(0xFFE5E7EB)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row (Incident Category, Date, Trash button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = when (item.incident) {
                        "PACE Evaluation Note" -> Color(0xFFEBF8FF)
                        "Emergency/EiE" -> Color(0xFFFFF5F5)
                        "Exceptional Work" -> Color(0xFFF0FDF4)
                        else -> Color(0xFFF3F4F6)
                    }
                    val badgeTextColor = when (item.incident) {
                        "PACE Evaluation Note" -> Color(0xFF2B6CB0)
                        "Emergency/EiE" -> Color(0xFFC53030)
                        "Exceptional Work" -> Color(0xFF15803D)
                        else -> Color(0xFF4B5563)
                    }
                    Box(
                        modifier = Modifier
                            .background(badgeColor, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.incident, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeTextColor)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item.date, fontSize = 9.sp, color = Color.Gray)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("🗑️", fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Notes
            Text(text = item.notes, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Medium)

            if (item.actionTaken.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↳ Way Forward: ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3182CE))
                    Text(item.actionTaken, fontSize = 10.sp, color = Color.DarkGray)
                }
            }

            // ATTACHED MEDIA DISPLAY PANELS
            if (item.imagePath != null || item.audioPath != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Attached Photo Panel
                    item.imagePath?.let { path ->
                        val presetTitle = when (path) {
                            "simulated://photo/rubric" -> "Classwork Rubric"
                            "simulated://photo/science" -> "Science Project Draft"
                            "simulated://photo/cooperation" -> "Group Work Cooperation"
                            "simulated://photo/exam" -> "Exam Paper Scan"
                            else -> "Attached Photo"
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(Color(0xFFEDF2F7), shape = RoundedCornerShape(6.dp))
                                .border(0.5.dp, Color(0xFFCBD5E0), shape = RoundedCornerShape(6.dp))
                                .padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFCBD5E0), shape = RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (path) {
                                            "simulated://photo/rubric" -> "📋"
                                            "simulated://photo/science" -> "🔬"
                                            "simulated://photo/cooperation" -> "👥"
                                            "simulated://photo/exam" -> "📝"
                                            else -> "📸"
                                        },
                                        fontSize = 18.sp
                                    )
                                }
                                Column {
                                    Text("IMAGE CAPTURED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(presetTitle, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, maxLines = 1)
                                }
                            }
                        }
                    }

                    // Attached Voice Note Panel
                    item.audioPath?.let { _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(Color(0xFFEDF2F7), shape = RoundedCornerShape(6.dp))
                                .border(0.5.dp, Color(0xFFCBD5E0), shape = RoundedCornerShape(6.dp))
                                .padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { isPlayingAudio = !isPlayingAudio },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isPlayingAudio) Color(0xFFFFF2CC) else Color(0xFFE2EFDA),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Text(if (isPlayingAudio) "⏸" else "▶", fontSize = 12.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("VOICE MEMO (12s)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        if (isPlayingAudio) {
                                            Text("Playing", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38A169))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Linear play timeline slider
                                    LinearProgressIndicator(
                                        progress = { audioProgress },
                                        modifier = Modifier.fillMaxWidth().height(3.dp),
                                        color = Color(0xFF3182CE),
                                        trackColor = Color(0xFFE2E8F0)
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
