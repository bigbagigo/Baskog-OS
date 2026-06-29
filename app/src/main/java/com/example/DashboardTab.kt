package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Curriculum
import com.example.data.LessonPlan
import com.example.ui.MainViewModel

@Composable
fun DashboardTab(
    viewModel: MainViewModel,
    curriculumList: List<Curriculum>,
    plansList: List<LessonPlan>,
    onNavigateToTab: (Int) -> Unit
) {
    val teacherName by viewModel.teacherName.collectAsStateWithLifecycle()
    val teacherDesignation by viewModel.teacherDesignation.collectAsStateWithLifecycle()
    val schoolName by viewModel.schoolName.collectAsStateWithLifecycle()
    val schoolDistrict by viewModel.schoolDistrict.collectAsStateWithLifecycle()
    val divisionOffice by viewModel.divisionOffice.collectAsStateWithLifecycle()
    val profilePic by viewModel.teacherProfilePicture.collectAsStateWithLifecycle()
    val currentTerm by viewModel.currentTerm.collectAsStateWithLifecycle()
    val currentWeek by viewModel.currentWeek.collectAsStateWithLifecycle()
    val eieLevel by viewModel.eieLevel.collectAsStateWithLifecycle()
    val classRoster by viewModel.classRosterText.collectAsStateWithLifecycle()
    val attendanceData by viewModel.attendanceMap.collectAsStateWithLifecycle()

    val registeredStudents = classRoster.split("\n").filter { it.isNotBlank() }
    val totalStudentsCount = registeredStudents.size

    val activeSectionLabel by viewModel.lessonSection.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // --- 1. HERO EDUCATOR PROFILE BANNER ---
        Card(
            colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF818CF8).copy(0.2f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Large profile picture display
                TeacherProfileAvatar(
                    picture = profilePic,
                    size = 80.dp,
                    borderWidth = 3.dp,
                    borderColor = Color(0xFF818CF8)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WELCOME BACK, EDUCATOR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = teacherName.ifBlank { "JOSEPH DANIEL DURAN" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ComposeColor.White
                    )
                    Text(
                        text = (teacherDesignation.ifBlank { "Teacher III" }) + " • " + (activeSectionLabel.ifBlank { "Section A" }),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ComposeColor(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Home,
                            contentDescription = "School",
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = schoolName.ifBlank { "Tala Elementary School" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF34C759),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // --- 2. SYSTEM STATUS INFOGRAPHICS ---
        Text(
            text = "SYSTEM STATUS INFOGRAPHICS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = ComposeColor(0xFF64748B),
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )

        // Row 1 of Infographics: TTY block and Emergency level
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // TTY School Year Progress
            Card(
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0F172A)),
                border = BorderStroke(1.dp, ComposeColor.White.copy(0.05f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.DateRange,
                        contentDescription = "TTY Progress",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "DEPED TTY CALENDAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = ComposeColor(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentTerm,
                        style = MaterialTheme.typography.titleMedium,
                        color = ComposeColor.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Week $currentWeek of TTY Block",
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor(0xFF94A3B8)
                    )
                }
            }

            // EiE Protocol Level
            val eieColor = when (eieLevel) {
                "Hayo" -> Color(0xFF10B981)
                "Hinay" -> Color(0xFF3B82F6)
                "Hinga" -> Color(0xFFF59E0B)
                "Hinto" -> Color(0xFFEF4444)
                else -> Color(0xFF10B981)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0F172A)),
                border = BorderStroke(1.dp, eieColor.copy(0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                            contentDescription = "EiE Level",
                            tint = eieColor,
                            modifier = Modifier.size(24.dp)
                        )
                        // Pulsing status dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(eieColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ACTIVE EIE PROTOCOL",
                        style = MaterialTheme.typography.labelSmall,
                        color = ComposeColor(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = eieLevel.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = eieColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (eieLevel) {
                            "Hayo" -> "Normal active operations."
                            "Hinay" -> "Caution active safety."
                            "Hinga" -> "Alternative Home modules."
                            "Hinto" -> "Emergency lockdown active."
                            else -> "Normal active operations."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor(0xFF94A3B8),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Row 2 of Infographics: Curriculum coverage & Generated lesson plans
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // KABAN Curriculum Coverage
            Card(
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0F172A)),
                border = BorderStroke(1.dp, ComposeColor.White.copy(0.05f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.List,
                        contentDescription = "KABAN database",
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "KABAN CURRICULUM",
                        style = MaterialTheme.typography.labelSmall,
                        color = ComposeColor(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${curriculumList.size} Standards",
                        style = MaterialTheme.typography.titleMedium,
                        color = ComposeColor.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MELC alignment records loaded",
                        color = ComposeColor(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Generated Lesson Plans Catalog
            Card(
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0F172A)),
                border = BorderStroke(1.dp, ComposeColor.White.copy(0.05f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                        contentDescription = "Compiled Plans",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ILAW COMPILER PLANS",
                        style = MaterialTheme.typography.labelSmall,
                        color = ComposeColor(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${plansList.size} Plans",
                        style = MaterialTheme.typography.titleMedium,
                        color = ComposeColor.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Local catalog database size",
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor(0xFF94A3B8)
                    )
                }
            }
        }

        // Row 3 of Infographics: Class roster & Attendance summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Class Cohort Roster size
            Card(
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0F172A)),
                border = BorderStroke(1.dp, ComposeColor.White.copy(0.05f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.AccountBox,
                        contentDescription = "Class roster",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "CLASS COHORT SIZE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ComposeColor(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalStudentsCount Students",
                        style = MaterialTheme.typography.titleMedium,
                        color = ComposeColor.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Registered in active cohort",
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor(0xFF94A3B8)
                    )
                }
            }

            // Attendance Analytics
            var totalSessionsCount = 0
            var totalPresentCount = 0
            attendanceData.forEach { (_, daysList) ->
                daysList.forEach { status ->
                    if (status == "P" || status == "✔") {
                        totalPresentCount++
                    }
                    if (status == "P" || status == "✔" || status == "A" || status == "❌" || status == "L" || status == "⚠") {
                        totalSessionsCount++
                    }
                }
            }
            val attendancePercent = if (totalSessionsCount > 0) (totalPresentCount * 100) / totalSessionsCount else 100

            Card(
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0F172A)),
                border = BorderStroke(1.dp, ComposeColor.White.copy(0.05f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Check,
                        contentDescription = "Attendance rate",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "COHORT ATTENDANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ComposeColor(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$attendancePercent% Rate",
                        style = MaterialTheme.typography.titleMedium,
                        color = ComposeColor.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cumulative class attendance",
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor(0xFF94A3B8),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        // --- 3. QUICK ACTION PROTOCOLS ---
        Text(
            text = "QUICK ACTION PROTOCOLS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = ComposeColor(0xFF64748B),
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF1E293B)),
            border = BorderStroke(1.dp, ComposeColor.White.copy(0.03f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Launch ILAW Builder Button (Tab 2)
                Button(
                    onClick = { onNavigateToTab(2) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = null, tint = ComposeColor.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LAUNCH ILAW COMPILER BUILDER", fontWeight = FontWeight.Bold, color = ComposeColor.White)
                }

                // Quick View Plans Catalog (Tab 3)
                OutlinedButton(
                    onClick = { onNavigateToTab(3) },
                    border = BorderStroke(1.dp, Color(0xFF818CF8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Menu, contentDescription = null, tint = Color(0xFF818CF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VIEW LESSON BLUEPRINTS CATALOG", fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                }
            }
        }
    }
}
