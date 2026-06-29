package com.example

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun normalizeLineEndings() {
    val paths = listOf(
      "app/src/main/java/com/example/MainActivity.kt",
      "app/src/main/java/com/example/ui/MainViewModel.kt"
    )
    for (p in paths) {
      val file = File(p)
      if (file.exists()) {
        val content = file.readText()
        val normalized = content.replace("\r\n", "\n")
        file.writeText(normalized)
        println("Normalized line endings for: $p")
      } else {
        println("File not found: $p")
      }
    }
  }

  @Test
  fun runReplacements() {
    var file = File("app/src/main/java/com/example/MainActivity.kt")
    if (!file.exists()) {
      file = File("src/main/java/com/example/MainActivity.kt")
    }
    if (!file.exists()) {
      file = File("../app/src/main/java/com/example/MainActivity.kt")
    }
    if (file.exists()) {
      var content = file.readText()

      // 1. Replace PlansCatalogTab block
      val startTag = "fun PlansCatalogTab("
      val endTag = "private fun getPlanMonth("
      val startIndex = content.indexOf(startTag)
      val endIndex = content.indexOf(endTag)
      if (startIndex != -1 && endIndex != -1) {
          val before = content.substring(0, startIndex)
          val after = content.substring(endIndex)
          val middle = """fun PlansCatalogTab(viewModel: MainViewModel, plansList: List<LessonPlan>, onStartPrompter: (LessonPlan) -> Unit) {
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
                                    text = "${'$'}{plansInMonth.size} Plans",
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

"""
          content = before + middle + after
          println("Replaced PlansCatalogTab successfully!")
      }

      // 2. Replace Subject dropdown
      val landmarkSubjectStart = "val subjectsInKaban = curriculumList.map { it.subject }.distinct().sorted()"
      val landmarkSubjectEnd = "DropdownMenu(\n                            expanded = showSubjectDropdown,"
      val idxSubjectStart = content.indexOf(landmarkSubjectStart)
      if (idxSubjectStart != -1) {
          val idxSubjectEnd = content.indexOf(landmarkSubjectEnd, idxSubjectStart)
          if (idxSubjectEnd != -1) {
              val before = content.substring(0, idxSubjectStart + landmarkSubjectStart.length)
              val after = content.substring(idxSubjectEnd)
              val replacement = """

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1.5f)) {
                        CustomDropdownSelector(
                            label = "SUBJECT NAME",
                            value = planSubject,
                            onClick = { showSubjectDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        )
"""
              content = before + replacement + after
              println("Replaced Subject dropdown successfully!")
          }
      }

      // 3. Replace Specific Grade dropdown
      val landmarkGradeStart = "Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {"
      val idxGradeRowStart = content.indexOf(landmarkGradeStart, content.indexOf("// Target Specific Grade & Language Selection"))
      if (idxGradeRowStart != -1) {
          val landmarkGradeEnd = "DropdownMenu(\n                            expanded = showGradeMenu,"
          val idxGradeEnd = content.indexOf(landmarkGradeEnd, idxGradeRowStart)
          if (idxGradeEnd != -1) {
              val before = content.substring(0, idxGradeRowStart + landmarkGradeStart.length)
              val after = content.substring(idxGradeEnd)
              val replacement = """
                    var showGradeMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        CustomDropdownSelector(
                            label = "SPECIFIC GRADE",
                            value = planSpecificGradeLevel.ifEmpty { "Grade 5" },
                            onClick = { showGradeMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        )
"""
              content = before + replacement + after
              println("Replaced Specific Grade dropdown successfully!")
          }
      }

      // 4. Replace Lesson Language dropdown
      val landmarkLangStart = "var showLangMenu by remember { mutableStateOf(false) }"
      val idxLangStart = content.indexOf(landmarkLangStart)
      if (idxLangStart != -1) {
          val landmarkLangEnd = "DropdownMenu(\n                            expanded = showLangMenu,"
          val idxLangEnd = content.indexOf(landmarkLangEnd, idxLangStart)
          if (idxLangEnd != -1) {
              val before = content.substring(0, idxLangStart)
              val after = content.substring(idxLangEnd)
              val replacement = """var showLangMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1.5f)) {
                        CustomDropdownSelector(
                            label = "LESSON LANGUAGE",
                            value = planLanguage.ifEmpty { "English" },
                            onClick = { showLangMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        )
"""
              content = before + replacement + after
              println("Replaced Lesson Language dropdown successfully!")
          }
      }

      // 5. Replace Day of Delivery dropdown
      val landmarkWeekdayStart = "var showWeekdayMenu by remember { mutableStateOf(false) }"
      val idxWeekdayStart = content.indexOf(landmarkWeekdayStart)
      if (idxWeekdayStart != -1) {
          val landmarkWeekdayEnd = "DropdownMenu(\n                                    expanded = showWeekdayMenu,"
          val idxWeekdayEnd = content.indexOf(landmarkWeekdayEnd, idxWeekdayStart)
          if (idxWeekdayEnd != -1) {
              val before = content.substring(0, idxWeekdayStart)
              val after = content.substring(idxWeekdayEnd)
              val replacement = """var showWeekdayMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1.2f)) {
                                CustomDropdownSelector(
                                    label = "DAY OF DELIVERY",
                                    value = deliveryDate,
                                    onClick = { showWeekdayMenu = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
"""
              content = before + replacement + after
              println("Replaced Day of Delivery dropdown successfully!")
          }
      }

      file.writeText(content)
      println("Successfully applied all programmatic modifications!")
    } else {
      fail("MainActivity.kt not found!")
    }
  }
}
