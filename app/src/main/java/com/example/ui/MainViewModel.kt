package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database.appDao())

    // --- State Ingestion Database Flows ---
    val curriculumItems: StateFlow<List<Curriculum>> = repository.curriculumItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lessonPlans: StateFlow<List<LessonPlan>> = repository.lessonPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemLogs: StateFlow<List<SystemLog>> = repository.systemLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Operational Controls & UI Transient States ---
    val currentTerm = MutableStateFlow("2nd Term") // 1st Term, 2nd Term, 3rd Term
    val currentWeek = MutableStateFlow(5) // 1 to 10

    val eieLevel = MutableStateFlow("Hayo") // Hayo, Hinay, Hinga, Hinto
    val lockdownActive = MutableStateFlow(false) // DRRM Lockdown active flag

    // Accessibility scale & API variables
    val fontScaleMultiplier = MutableStateFlow(1.0f)
    val customApiKey = MutableStateFlow("")
    val automaticBlockCalendar = MutableStateFlow(true)

    // Educator specifics for precheck & scheduling
    val teacherProficiencyLevel = MutableStateFlow("Proficient") // Proficient, Highly Proficient, Distinguished
    val lessonDeliveryDate = MutableStateFlow("Monday") // Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
    val lessonSection = MutableStateFlow("Section A")
    val lessonPeriods = MutableStateFlow(setOf(1)) // integers e.g. 1, 2, 3

    // User PROFILE Metadata settings
    val teacherName = MutableStateFlow("JOSEPH DANIEL DURAN")
    val teacherProfilePicture = MutableStateFlow("preset_1")
    val teacherDesignation = MutableStateFlow("Teacher III")
    val schoolName = MutableStateFlow("Tala Elementary School")
    val schoolDistrict = MutableStateFlow("District II")
    val divisionOffice = MutableStateFlow("Division of Masbate")

    // Weather and News state telemetry
    val weatherTemp = MutableStateFlow("31.5°C")
    val weatherCondition = MutableStateFlow("Light Rain showers")
    val localNews = MutableStateFlow("DepEd Region VI issues continuity advisory aligned to TTY protocol.")

    // Generated Lesson Plan (ILAW format) Output block - strict raw JSON format
    val activeLessonPlanJson = MutableStateFlow("")
    val planSubject = MutableStateFlow("Reading Literacy")
    val planGradeLevel = MutableStateFlow("KS1")
    val planSpecificGradeLevel = MutableStateFlow("Grade 1")
    val planLanguage = MutableStateFlow("English")
    val planPpstChecklist = MutableStateFlow(setOf("1.1.1: Content knowledge & pedagogy", "1.4.1: Use of Mother Tongue/Language"))
    val planTeachingStrategy = MutableStateFlow("Collaborative Learning") // Collaborative, Inquiry, Direct, Constructivist
    val planDurationMins = MutableStateFlow(60)
    val planCustomPrompt = MutableStateFlow("")
    val planContentStandardText = MutableStateFlow("")
    val planLearningCompetencyText = MutableStateFlow("")
    val isPlanGenerating = MutableStateFlow(false)

    // Formative Server state variables
    val classRosterText = MutableStateFlow(
        "Juan dela Cruz\nMaria Clara Santos\nJose Bantog\nAna Mae Rosas\nBaste Magbanua\nTrixie Alcala\nRamil Legaspi\nCynthia Ramos\nMated Duran\nLeonila Gomez\nNicanor Abelardo\nSalvador Laurel\nTeresa Magbanua\nAgapito Flores\nLualhati Bautista\nEfren Peñaflorida\nGilda Cordero\nBienvenido Lumbera\nNick Joaquin\nJose Rizal\nAndres Bonifacio\nEmilio Jacinto\nApolinario Mabini\nMelchora Aquino\nGabriela Silang\nFrancisco Balagtas\nGraciano Lopez Jaena\nMacario Sakay\nGregorio del Pilar\nMarcelo H. del Pilar"
    )
    val formativeQuestionType = MutableStateFlow("Multiple Choice") // Multiple Choice, True/False, Free Text
    val formativeQuestionText = MutableStateFlow("")
    val formativeCorrectAnswer = MutableStateFlow("A") // e.g. A, B, C, D, True, False
    val formativeOptions = MutableStateFlow(listOf("A", "B", "C", "D"))
    val formativeServerActive = MutableStateFlow(false)
    val formativeSimulatedStudents = MutableStateFlow<List<StudentResponse>>(emptyList())
    val formativeHistory = MutableStateFlow<List<FormativeQuizResult>>(emptyList())
    val completedCurriculumIds = MutableStateFlow<Set<Int>>(emptySet())

    // Standard pre-configured Test Generator state variables
    val formativeQuestionsList = MutableStateFlow<List<FormativeQuestion>>(
        listOf(
            FormativeQuestion(
                text = "Which of the following is an example of a Contact Force?",
                type = "Multiple Choice",
                correctAnswer = "A",
                options = listOf("A) Friction", "B) Gravity", "C) Magnetism", "D) Static Electricity")
            ),
            FormativeQuestion(
                text = "What state of matter has a definite volume but no definite shape?",
                type = "Multiple Choice",
                correctAnswer = "Liquid",
                options = listOf("Solid", "Liquid", "Gas", "Plasma")
            ),
            FormativeQuestion(
                text = "A Series Circuit has only one path for the flow of electric current.",
                type = "True/False",
                correctAnswer = "True",
                options = listOf("True", "False")
            )
        )
    )
    val formativeActiveQuestionIndex = MutableStateFlow(0)
    val serverJoinMethod = MutableStateFlow("QR") // "QR" or "Manual"
    val localIpAddressState = MutableStateFlow("127.0.0.1")

    // Curriculum Parser variables
    val parserInputText = MutableStateFlow("")
    val parsingStatusMessage = MutableStateFlow("")
    val isParsing = MutableStateFlow(false)

    // Derived states
    val isAralRemediationRequired: StateFlow<Boolean> = combine(currentTerm, currentWeek) { term, week ->
        term == "2nd Term" && week >= 5
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // --- Dynamic Class Record and Attendance Spreadsheet States ---
    val classRecordScores = MutableStateFlow<Map<String, StudentGrades>>(emptyMap())
    val attendanceMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val wwMaxScores = MutableStateFlow(listOf(20, 20, 20, 20, 20))
    val ptMaxScores = MutableStateFlow(listOf(50, 50, 50))
    val saMaxScores = MutableStateFlow(listOf(30, 30, 50)) // SA1, SA2, TE
    val anecdotalRecords = MutableStateFlow<List<AnecdotalRecord>>(emptyList())

    // --- MULTIPLE CLASS RECORDS SETUP (8 SHEETS) ---
    val selectedClassIndex = MutableStateFlow(0) // 0 to 7

    val DEFAULT_CLASS_NAMES = listOf(
        "Grade 4 - Science",
        "Grade 5 - Mathematics",
        "Grade 6 - English",
        "Grade 4 - Araling Panlipunan",
        "Grade 5 - MAPEH",
        "Grade 6 - Filipino",
        "Grade 4 - Edukasyon sa Pagpapakatao",
        "Grade 5 - EPP"
    )

    val DEFAULT_CLASS_SUBJECTS = listOf(
        "Science",
        "Mathematics",
        "English",
        "Araling Panlipunan",
        "MAPEH",
        "Filipino",
        "Edukasyon sa Pagpapakatao",
        "EPP"
    )

    val DEFAULT_CLASS_ROSTERS = listOf(
        "ALCANTARA, BENJAMIN\nBELMONTE, CRISPIN\nCASTILLO, DANICA\nDIMAANO, EDUARDO\nESPINOSA, FLORENCIA\nFRANCISCO, GREGORIO\nGUTIERREZ, HAROLD\nIGNACIO, IMELDA\nJAVIER, JOVITO\nLAGMAN, KRISTINA\nJuan dela Cruz\nMaria Clara Santos\nJose Bantog\nAna Mae Rosas\nBaste Magbanua\nTrixie Alcala\nRamil Legaspi\nCynthia Ramos\nMated Duran\nLeonila Gomez\nNicanor Abelardo\nSalvador Laurel\nTeresa Magbanua\nAgapito Flores\nLualhati Bautista\nEfren Peñaflorida\nGilda Cordero\nBienvenido Lumbera\nNick Joaquin\nJose Rizal\nAndres Bonifacio\nEmilio Jacinto\nApolinario Mabini\nMelchora Aquino\nGabriela Silang\nFrancisco Balagtas\nGraciano Lopez Jaena\nMacario Sakay\nGregorio del Pilar\nMarcelo H. del Pilar",
        "ALABAN, CARLO\nBARBACENA, CHRIS\nCRUZ, ANGELA\nDELA CRUZ, JUAN\nESPAÑOLA, REY\nFLORES, LINA\nGARCIA, DANICA\nHERNANDEZ, MARIO\nILAGAN, NOEL\nMACALALAD, RITA\nOCAMPO, REMY\nSANTOS, CARMINA",
        "BALAT, ELENA\nCONCEPCION, JONAS\nESTEBAN, PATRICIA\nGOMEZ, JOSE\nLOPEZ, ALICIA\nMANALILI, PAOLO\nSANTOS, CARMINA\nTAN, REGINALD\nVALENCIA, PILAR\nZOSA, MARK",
        "AGUINALDO, EMILIO\nBONIFACIO, ANDRES\nDEL PILAR, GREGORIO\nMABINI, APOLINARIO\nRIZAL, JOSE\nSILANG, GABRIELA\nTECSON, TRINIDAD\nJACINTO, EMILIO\nRECTO, CLARO\nSOLIMAN, RAJAH",
        "ALONZO, LIZA\nBUENDIA, ELY\nDIMALANTA, KARL\nSANTIAGO, RANDY\nVALERA, CHITO\nREGALA, OGIE\nVELASCO, JAYA\nVALDEZ, REGINE\nSANTOS, LEA\nNIEVERA, MARTIN",
        "BALAGTAS, FRANCISCO\nQUEZON, MANUEL\nLUNA, JUAN\nRECTO, CLARO\nSANTOS, LOPE\nHENERAL, LUNA\nDELA REYNA, REINA\nMAALALAHANIN, MARA\nHERNANDEZ, AMADO\nAGUILA, CORAZON",
        "AMOROSO, AMY\nBUENAVENTURA, BEN\nCABRAL, CORA\nDIMAGIBA, DINO\nESTRADA, ERNIE\nFUENTES, FELY\nGUZMAN, GARY\nHERRERA, HILDA\nIBARRA, CRISPIN\nJOAQUIN, NICK",
        "AQUINO, MELCHORA\nCASTRO, JOSEFA\nKASILAG, LUCRECIA\nLIM, PILAR\nPECSOM, GERONIMA\nREYES, ALICE\nSILANG, GABRIELA\nAQUINO, CORAZON\nLORENZO, DIOSDADO\nABELLANA, JOVITO"
    )

    val prefs = application.getSharedPreferences("maragtason_prefs", android.content.Context.MODE_PRIVATE)

    init {
        // Load custom API key
        val savedKey = prefs.getString("custom_api_key", "") ?: ""
        customApiKey.value = savedKey
        com.example.data.GeminiClient.userOverrideApiKey = savedKey

        // Load font scale
        val savedFontScale = prefs.getFloat("font_scale_multiplier", 1.0f)
        fontScaleMultiplier.value = savedFontScale

        // Load automatic block calendar setting
        val savedAutoBlock = prefs.getBoolean("automatic_block_calendar", true)
        automaticBlockCalendar.value = savedAutoBlock

        // Load teacher profile metadata settings
        teacherName.value = prefs.getString("teacher_name", "JOSEPH DANIEL DURAN") ?: "JOSEPH DANIEL DURAN"
        teacherProfilePicture.value = prefs.getString("teacher_profile_picture", "preset_1") ?: "preset_1"
        teacherDesignation.value = prefs.getString("teacher_designation", "Teacher III") ?: "Teacher III"
        schoolName.value = prefs.getString("school_name", "Tala Elementary School") ?: "Tala Elementary School"
        schoolDistrict.value = prefs.getString("school_district", "District II") ?: "District II"
        divisionOffice.value = prefs.getString("division_office", "Division of Masbate") ?: "Division of Masbate"
        // Load Selected Class Record Index and Sheet values
        val savedIndex = prefs.getInt("selected_class_record_index", 0)
        selectedClassIndex.value = savedIndex
        loadSheetFromPrefs(savedIndex)

        // Automatic syncing of Student Record arrays if roster elements change
        viewModelScope.launch {
            classRosterText.collect { roster ->
                val names = roster.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                
                // Grades auto-syncing
                val currentScores = classRecordScores.value.toMutableMap()
                var updatedScores = false
                val missingScores = names.filter { !currentScores.containsKey(it) }
                if (missingScores.isNotEmpty()) {
                    for (name in missingScores) {
                        val hash = name.hashCode()
                        val wwList = listOf(
                            ((hash % 6) + 14).toString(),
                            ((hash % 5) + 15).toString(),
                            ((hash % 4) + 16).toString(),
                            ((hash % 5) + 15).toString(),
                            ((hash % 6) + 14).toString()
                        )
                        val ptList = listOf(
                            ((hash % 10) + 40).toString(),
                            ((hash % 8) + 41).toString(),
                            ((hash % 9) + 40).toString()
                        )
                        val saList = listOf(
                            ((hash % 6) + 22).toString(),
                            ((hash % 7) + 23).toString()
                        )
                        val te = ((hash % 12) + 38).toString()
                        currentScores[name] = StudentGrades(name, wwList, ptList, saList, te)
                    }
                    updatedScores = true
                }
                // cleanup removed students
                val obsoleteScores = currentScores.keys.filter { !names.contains(it) }
                if (obsoleteScores.isNotEmpty()) {
                    for (name in obsoleteScores) {
                        currentScores.remove(name)
                    }
                    updatedScores = true
                }
                if (updatedScores) {
                    classRecordScores.value = currentScores
                    saveClassRecordToPrefs(currentScores)
                }

                // Attendance auto-syncing
                val currentAttendance = attendanceMap.value.toMutableMap()
                var updatedAttendance = false
                val missingAttendance = names.filter { !currentAttendance.containsKey(it) }
                if (missingAttendance.isNotEmpty()) {
                    for (name in missingAttendance) {
                        val hash = name.hashCode()
                        val dayList = MutableList(15) { "P" }
                        if (hash % 7 == 0) dayList[3] = "A"
                        if (hash % 11 == 0) dayList[8] = "L"
                        if (hash % 13 == 0) dayList[12] = "A"
                        currentAttendance[name] = dayList
                    }
                    updatedAttendance = true
                }
                // cleanup removed students
                val obsoleteAttendance = currentAttendance.keys.filter { !names.contains(it) }
                if (obsoleteAttendance.isNotEmpty()) {
                    for (name in obsoleteAttendance) {
                        currentAttendance.remove(name)
                    }
                    updatedAttendance = true
                }
                if (updatedAttendance) {
                    attendanceMap.value = currentAttendance
                    saveAttendanceToPrefs(currentAttendance)
                }
            }
        }

        val savedCompleted = prefs.getString("completed_curriculum_ids", "") ?: ""
        completedCurriculumIds.value = savedCompleted.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

        // Persistent configurations loading
        currentTerm.value = prefs.getString("current_term", "2nd Term") ?: "2nd Term"
        currentWeek.value = prefs.getInt("current_week", 5)
        eieLevel.value = prefs.getString("eie_level", "Hayo") ?: "Hayo"
        lockdownActive.value = prefs.getBoolean("lockdown_active", false)
        teacherProficiencyLevel.value = prefs.getString("teacher_proficiency_level", "Proficient") ?: "Proficient"
        lessonDeliveryDate.value = prefs.getString("lesson_delivery_date", "Monday") ?: "Monday"
        lessonSection.value = prefs.getString("lesson_section", "Section A") ?: "Section A"
        val savedPeriodsString = prefs.getString("lesson_periods", "1") ?: "1"
        lessonPeriods.value = savedPeriodsString.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

        // Persistent ILAW Builder configuration parameters
        planSubject.value = prefs.getString("plan_subject", "Reading Literacy") ?: "Reading Literacy"
        planGradeLevel.value = prefs.getString("plan_grade_level", "KS1") ?: "KS1"
        planSpecificGradeLevel.value = prefs.getString("plan_specific_grade_level", "Grade 1") ?: "Grade 1"
        planLanguage.value = prefs.getString("plan_language", "English") ?: "English"
        planTeachingStrategy.value = prefs.getString("plan_teaching_strategy", "Collaborative Learning") ?: "Collaborative Learning"
        planDurationMins.value = prefs.getInt("plan_duration_mins", 60)
        planCustomPrompt.value = prefs.getString("plan_custom_prompt", "") ?: ""
        planContentStandardText.value = prefs.getString("plan_content_standard_text", "") ?: ""
        planLearningCompetencyText.value = prefs.getString("plan_learning_competency_text", "") ?: ""

        // Auto-persist setup reactively
        viewModelScope.launch {
            classRosterText.collect { roster ->
                val idx = selectedClassIndex.value
                prefs.edit().putString("class_roster_text_$idx", roster).apply()
                if (idx == 0) {
                    prefs.edit().putString("class_roster_text", roster).apply()
                }
            }
        }
        viewModelScope.launch {
            currentTerm.collect { prefs.edit().putString("current_term", it).apply() }
        }
        viewModelScope.launch {
            currentWeek.collect { prefs.edit().putInt("current_week", it).apply() }
        }
        viewModelScope.launch {
            eieLevel.collect { prefs.edit().putString("eie_level", it).apply() }
        }
        viewModelScope.launch {
            lockdownActive.collect { prefs.edit().putBoolean("lockdown_active", it).apply() }
        }
        viewModelScope.launch {
            teacherProficiencyLevel.collect { prefs.edit().putString("teacher_proficiency_level", it).apply() }
        }
        viewModelScope.launch {
            lessonDeliveryDate.collect { prefs.edit().putString("lesson_delivery_date", it).apply() }
        }
        viewModelScope.launch {
            lessonSection.collect { section ->
                val idx = selectedClassIndex.value
                prefs.edit().putString("lesson_section_$idx", section).apply()
                if (idx == 0) {
                    prefs.edit().putString("lesson_section", section).apply()
                }
            }
        }
        viewModelScope.launch {
            lessonPeriods.collect { set -> 
                prefs.edit().putString("lesson_periods", set.joinToString(",")).apply()
            }
        }
        viewModelScope.launch {
            planSubject.collect { subject ->
                val idx = selectedClassIndex.value
                prefs.edit().putString("plan_subject_$idx", subject).apply()
                if (idx == 0) {
                    prefs.edit().putString("plan_subject", subject).apply()
                }
            }
        }
        viewModelScope.launch {
            planGradeLevel.collect { prefs.edit().putString("plan_grade_level", it).apply() }
        }
        viewModelScope.launch {
            planSpecificGradeLevel.collect { prefs.edit().putString("plan_specific_grade_level", it).apply() }
        }
        viewModelScope.launch {
            planLanguage.collect { prefs.edit().putString("plan_language", it).apply() }
        }
        viewModelScope.launch {
            planTeachingStrategy.collect { prefs.edit().putString("plan_teaching_strategy", it).apply() }
        }
        viewModelScope.launch {
            planDurationMins.collect { prefs.edit().putInt("plan_duration_mins", it).apply() }
        }
        viewModelScope.launch {
            planCustomPrompt.collect { prefs.edit().putString("plan_custom_prompt", it).apply() }
        }
        viewModelScope.launch {
            planContentStandardText.collect { prefs.edit().putString("plan_content_standard_text", it).apply() }
        }
        viewModelScope.launch {
            planLearningCompetencyText.collect { prefs.edit().putString("plan_learning_competency_text", it).apply() }
        }

        // Initialize database seeds
        viewModelScope.launch {
            repository.seedDatabase()
            refreshNewsAndTelemetry()
            checkAndApplyAutoWeek()
            resolveLocalWifiIpAddress()
            loadAnecdotalRecords()
        }
    }

    fun saveTeacherMetadata(
        name: String,
        designation: String,
        school: String,
        district: String,
        division: String
    ) {
        teacherName.value = name
        teacherDesignation.value = designation
        schoolName.value = school
        schoolDistrict.value = district
        divisionOffice.value = division
        prefs.edit().apply {
            putString("teacher_name", name)
            putString("teacher_designation", designation)
            putString("school_name", school)
            putString("school_district", district)
            putString("division_office", division)
        }.apply()
    }

    fun saveProfilePicture(picture: String) {
        teacherProfilePicture.value = picture
        prefs.edit().putString("teacher_profile_picture", picture).apply()
    }

    fun saveCustomApiKey(key: String) {
        customApiKey.value = key
        com.example.data.GeminiClient.userOverrideApiKey = key
        prefs.edit().putString("custom_api_key", key).apply()
        viewModelScope.launch {
            repository.logSystemEvent("SECURITY", "Custom API credential index updated securely.")
        }
    }

    fun logSystemEvent(type: String, message: String) {
        viewModelScope.launch {
            repository.logSystemEvent(type, message)
        }
    }

    fun saveFontScaleMultiplier(scale: Float) {
        fontScaleMultiplier.value = scale
        prefs.edit().putFloat("font_scale_multiplier", scale).apply()
    }

    fun saveAutomaticBlockCalendar(enabled: Boolean) {
        automaticBlockCalendar.value = enabled
        prefs.edit().putBoolean("automatic_block_calendar", enabled).apply()
        if (enabled) {
            checkAndApplyAutoWeek()
        }
    }

    // --- Spreadsheet Interactive Update Callbacks ---
    fun updateStudentGrade(
        studentName: String,
        category: String, // "WW", "PT", "SA", "TE"
        index: Int, // 0-based index
        newValue: String
    ) {
        val current = classRecordScores.value.toMutableMap()
        val grades = current[studentName] ?: StudentGrades(
            studentName,
            listOf("", "", "", "", ""),
            listOf("", "", ""),
            listOf("", ""),
            ""
        )
        
        val updatedGrades = when (category) {
            "WW" -> {
                val newList = grades.wwScores.toMutableList()
                if (index in newList.indices) newList[index] = newValue
                grades.copy(wwScores = newList)
            }
            "PT" -> {
                val newList = grades.ptScores.toMutableList()
                if (index in newList.indices) newList[index] = newValue
                grades.copy(ptScores = newList)
            }
            "SA" -> {
                val newList = grades.saScores.toMutableList()
                if (index in newList.indices) newList[index] = newValue
                grades.copy(saScores = newList)
            }
            "TE" -> grades.copy(termExamScore = newValue)
            else -> grades
        }
        
        current[studentName] = updatedGrades
        classRecordScores.value = current
        saveClassRecordToPrefs(current)
    }

    fun updateAttendance(studentName: String, dayIndex: Int, newStatus: String) {
        val current = attendanceMap.value.toMutableMap()
        val list = current[studentName]?.toMutableList() ?: MutableList(15) { "P" }
        if (dayIndex in list.indices) {
            list[dayIndex] = newStatus
        }
        current[studentName] = list
        attendanceMap.value = current
        saveAttendanceToPrefs(current)
    }

    fun updateMaxScore(category: String, index: Int, newValue: Int) {
        val idx = selectedClassIndex.value
        when (category) {
            "WW" -> {
                val list = wwMaxScores.value.toMutableList()
                if (index in list.indices) list[index] = newValue
                wwMaxScores.value = list
                prefs.edit().putString("ww_max_scores_$idx", list.joinToString(",")).apply()
                if (idx == 0) prefs.edit().putString("ww_max_scores", list.joinToString(",")).apply()
            }
            "PT" -> {
                val list = ptMaxScores.value.toMutableList()
                if (index in list.indices) list[index] = newValue
                ptMaxScores.value = list
                prefs.edit().putString("pt_max_scores_$idx", list.joinToString(",")).apply()
                if (idx == 0) prefs.edit().putString("pt_max_scores", list.joinToString(",")).apply()
            }
            "SA" -> {
                val list = saMaxScores.value.toMutableList()
                if (index in list.indices) list[index] = newValue
                saMaxScores.value = list
                prefs.edit().putString("sa_max_scores_$idx", list.joinToString(",")).apply()
                if (idx == 0) prefs.edit().putString("sa_max_scores", list.joinToString(",")).apply()
            }
        }
    }

    fun saveActiveSheetToPrefs() {
        val idx = selectedClassIndex.value
        prefs.edit().putString("class_roster_text_$idx", classRosterText.value).apply()
        prefs.edit().putString("lesson_section_$idx", lessonSection.value).apply()
        prefs.edit().putString("plan_subject_$idx", planSubject.value).apply()

        val gradesMap = classRecordScores.value
        val serializedGrades = gradesMap.entries.joinToString(separator = ";") { entry ->
            "${entry.key}|${entry.value.wwScores.joinToString(",")}|${entry.value.ptScores.joinToString(",")}|${entry.value.saScores.joinToString(",")}|${entry.value.termExamScore}"
        }
        prefs.edit().putString("class_record_data_v2_$idx", serializedGrades).apply()
        if (idx == 0) {
            prefs.edit().putString("class_record_data_v2", serializedGrades).apply()
        }

        val attMap = attendanceMap.value
        val serializedAtt = attMap.entries.joinToString(separator = ";") { entry ->
            "${entry.key}|${entry.value.joinToString(",")}"
        }
        prefs.edit().putString("attendance_data_v2_$idx", serializedAtt).apply()
        if (idx == 0) {
            prefs.edit().putString("attendance_data_v2", serializedAtt).apply()
        }

        prefs.edit().putString("ww_max_scores_$idx", wwMaxScores.value.joinToString(",")).apply()
        prefs.edit().putString("pt_max_scores_$idx", ptMaxScores.value.joinToString(",")).apply()
        prefs.edit().putString("sa_max_scores_$idx", saMaxScores.value.joinToString(",")).apply()
    }

    fun loadSheetFromPrefs(idx: Int) {
        val defaultRoster = DEFAULT_CLASS_ROSTERS.getOrElse(idx) { DEFAULT_CLASS_ROSTERS[0] }
        val defaultSection = DEFAULT_CLASS_NAMES.getOrElse(idx) { DEFAULT_CLASS_NAMES[0] }
        val defaultSubject = DEFAULT_CLASS_SUBJECTS.getOrElse(idx) { DEFAULT_CLASS_SUBJECTS[0] }

        val roster = prefs.getString("class_roster_text_$idx", if (idx == 0) prefs.getString("class_roster_text", defaultRoster) else defaultRoster) ?: defaultRoster
        classRosterText.value = roster

        val section = prefs.getString("lesson_section_$idx", if (idx == 0) prefs.getString("lesson_section", defaultSection) else defaultSection) ?: defaultSection
        lessonSection.value = section

        val subject = prefs.getString("plan_subject_$idx", if (idx == 0) prefs.getString("plan_subject", defaultSubject) else defaultSubject) ?: defaultSubject
        planSubject.value = subject

        val defaultGradesString = ""
        val savedRecord = prefs.getString("class_record_data_v2_$idx", if (idx == 0) prefs.getString("class_record_data_v2", defaultGradesString) else defaultGradesString) ?: defaultGradesString
        val map = mutableMapOf<String, StudentGrades>()
        if (savedRecord.isNotBlank()) {
            val parts = savedRecord.split(";")
            for (part in parts) {
                if (part.isBlank()) continue
                val subParts = part.split("|")
                if (subParts.size >= 5) {
                    val name = subParts[0]
                    val ww = subParts[1].split(",")
                    val pt = subParts[2].split(",")
                    val sa = subParts[3].split(",")
                    val te = subParts[4]
                    map[name] = StudentGrades(name, ww, pt, sa, te)
                }
            }
        }
        classRecordScores.value = map

        val defaultAttendanceString = ""
        val savedAttendance = prefs.getString("attendance_data_v2_$idx", if (idx == 0) prefs.getString("attendance_data_v2", defaultAttendanceString) else defaultAttendanceString) ?: defaultAttendanceString
        val attMap = mutableMapOf<String, List<String>>()
        if (savedAttendance.isNotBlank()) {
            val parts = savedAttendance.split(";")
            for (part in parts) {
                if (part.isBlank()) continue
                val subParts = part.split("|")
                if (subParts.size >= 2) {
                    val name = subParts[0]
                    val days = subParts[1].split(",")
                    attMap[name] = days
                }
            }
        }
        attendanceMap.value = attMap

        val savedWwMax = prefs.getString("ww_max_scores_$idx", if (idx == 0) prefs.getString("ww_max_scores", "20,20,20,20,20") else "20,20,20,20,20") ?: "20,20,20,20,20"
        wwMaxScores.value = savedWwMax.split(",").mapNotNull { it.toIntOrNull() }

        val savedPtMax = prefs.getString("pt_max_scores_$idx", if (idx == 0) prefs.getString("pt_max_scores", "50,50,50") else "50,50,50") ?: "50,50,50"
        ptMaxScores.value = savedPtMax.split(",").mapNotNull { it.toIntOrNull() }

        val savedSaMax = prefs.getString("sa_max_scores_$idx", if (idx == 0) prefs.getString("sa_max_scores", "30,30,50") else "30,30,50") ?: "30,30,50"
        saMaxScores.value = savedSaMax.split(",").mapNotNull { it.toIntOrNull() }
    }

    fun selectClassRecordSheet(index: Int) {
        saveActiveSheetToPrefs()
        selectedClassIndex.value = index
        prefs.edit().putInt("selected_class_record_index", index).apply()
        loadSheetFromPrefs(index)
    }

    private fun saveClassRecordToPrefs(map: Map<String, StudentGrades>) {
        val idx = selectedClassIndex.value
        val serialized = map.entries.joinToString(separator = ";") { entry ->
            "${entry.key}|${entry.value.wwScores.joinToString(",")}|${entry.value.ptScores.joinToString(",")}|${entry.value.saScores.joinToString(",")}|${entry.value.termExamScore}"
        }
        prefs.edit().putString("class_record_data_v2_$idx", serialized).apply()
        if (idx == 0) {
            prefs.edit().putString("class_record_data_v2", serialized).apply()
        }
    }

    private fun saveAttendanceToPrefs(map: Map<String, List<String>>) {
        val idx = selectedClassIndex.value
        val serialized = map.entries.joinToString(separator = ";") { entry ->
            "${entry.key}|${entry.value.joinToString(",")}"
        }
        prefs.edit().putString("attendance_data_v2_$idx", serialized).apply()
        if (idx == 0) {
            prefs.edit().putString("attendance_data_v2", serialized).apply()
        }
    }

    fun calculateAutoWeek(): Int {
        val cal = java.util.Calendar.getInstance()
        val month = cal.get(java.util.Calendar.MONTH) // 0-indexed (June is 5, July is 6)
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return if (month == java.util.Calendar.JUNE) {
            ((day - 1) / 7) + 1
        } else if (month == java.util.Calendar.JULY) {
            (((day - 1 + 30) / 7) + 1).coerceAtMost(10)
        } else {
            5 // Standard mid-term segment fallback
        }
    }

    fun checkAndApplyAutoWeek() {
        if (automaticBlockCalendar.value) {
            val autoWeek = calculateAutoWeek()
            currentWeek.value = autoWeek
        }
    }

    // --- Operational Controls logic ---

    fun updateTerm(term: String) {
        currentTerm.value = term
        viewModelScope.launch {
            repository.logSystemEvent("POLICY_EVENT", "DepEd TTY Block changed manually to: $term")
        }
    }

    fun updateWeek(week: Int) {
        // If manual change occurs, turn off automatic block calendar
        if (automaticBlockCalendar.value) {
            saveAutomaticBlockCalendar(false)
        }
        currentWeek.value = week.coerceIn(1, 10)
        viewModelScope.launch {
            repository.logSystemEvent("POLICY_EVENT", "Current week index updated to: ${currentWeek.value} (Manual override active)")
        }
    }

    fun updateEiELevel(level: String) {
        if (!lockdownActive.value) {
            eieLevel.value = level
            viewModelScope.launch {
                repository.logSystemEvent("SECURITY", "Continuity level transitioned to $level.")
            }
        }
    }

    /**
     * Emergency DRRM lockdown button. Forces threat level to Hinto, triggers system logs, and prevents typical operations.
     */
    fun triggerLockdownOverride() {
        lockdownActive.value = true
        eieLevel.value = "Hinto"
        viewModelScope.launch {
            repository.logSystemEvent(
                "SECURITY",
                "CRITICAL DRRM OVERRIDE: Lockdown engaged. Threat level forced to Hinto. Plan compilation processes restricted."
            )
        }
    }

    fun liftLockdownOverride() {
        lockdownActive.value = false
        eieLevel.value = "Hinga" // Transitional standard relief state
        viewModelScope.launch {
            repository.logSystemEvent(
                "SECURITY",
                "Lockdown protocol disengaged. Transitions safely back to Hinga status."
            )
        }
    }

    fun refreshNewsAndTelemetry() {
        val temps = listOf("32.1°C", "28.4°C", "34.0°C", "23.5°C")
        val conditions = listOf("Heavy Thunderstorm alert", "Sunny and Clear", "Intermittent Drizzle", "Cloudy gusty winds")
        val newsHeadlines = listOf(
            "DepEd Regional Headquarters announces hybrid learning guidance under Hinay protocols.",
            "Local DRRM warnings indicate active monsoon currents. Prepare home learning modules.",
            "Visual aid lesson plan templates distributed for opening term segments.",
            "In-person curriculum evaluations resumed for KS2 learners in local districts."
        )

        weatherTemp.value = temps.random()
        weatherCondition.value = conditions.random()
        localNews.value = newsHeadlines.random()

        viewModelScope.launch {
            repository.logSystemEvent(
                "POLICY_EVENT",
                "Telemetry refresh: Temperature state is ${weatherTemp.value} [${weatherCondition.value}]."
            )
        }
    }

    // --- Parser Operations ---

    fun insertCustomCurriculum(
        gradeLevel: String,
        subject: String,
        content: String,
        performance: String,
        competency: String,
        term: String,
        week: Int,
        melcCode: String = "",
        sessionsBudgeted: Int = 5
    ) {
        viewModelScope.launch {
            val item = Curriculum(
                gradeLevel = gradeLevel,
                subject = subject,
                term = term,
                week = week,
                contentStandard = content,
                performanceStandard = performance,
                learningCompetency = competency,
                melcCode = melcCode,
                sessionsBudgeted = sessionsBudgeted
            )
            repository.saveCurriculum(item)
            repository.logSystemEvent(
                "PARSER",
                "Curriculum item manually injected into alignment sqlite table for $gradeLevel - $subject."
            )
        }
    }

    fun triggerCurriculumParsing() {
        val input = parserInputText.value
        if (input.isBlank()) {
            parsingStatusMessage.value = "Please insert curriculum data to parse."
            return
        }

        isParsing.value = true
        viewModelScope.launch {
            repository.logSystemEvent("PARSER", "Initiating raw curriculum parser.")
            
            val (count, message) = repository.parseAndSaveCurriculum(input)
            
            parsingStatusMessage.value = message
            isParsing.value = false
            
            if (count > 0) {
                // Clear the input area upon success
                parserInputText.value = ""
            }
        }
    }

    fun deleteCurriculumItem(id: Int) {
        viewModelScope.launch {
            repository.removeCurriculum(id)
            repository.logSystemEvent("PARSER", "Removed curriculum item ID: $id.")
            
            // Also clean up from completed set if present
            if (completedCurriculumIds.value.contains(id)) {
                val newSet = completedCurriculumIds.value - id
                completedCurriculumIds.value = newSet
                prefs.edit().putString("completed_curriculum_ids", newSet.joinToString(",")).apply()
            }
        }
    }

    fun toggleCurriculumCompletion(id: Int) {
        val currentSet = completedCurriculumIds.value
        val newSet = if (currentSet.contains(id)) {
            currentSet - id
        } else {
            currentSet + id
        }
        completedCurriculumIds.value = newSet
        prefs.edit().putString("completed_curriculum_ids", newSet.joinToString(",")).apply()
        
        viewModelScope.launch {
            repository.logSystemEvent("POLICY_EVENT", "Curriculum item ID $id completion toggled: ${newSet.contains(id)}")
        }
    }

    fun clearCurriculumItems() {
        viewModelScope.launch {
            repository.clearAllCurriculum()
        }
    }

    fun resetToBuiltInCurriculum() {
        viewModelScope.launch {
            isParsing.value = true
            parsingStatusMessage.value = "Clearing current active curriculum list..."
            repository.clearAllCurriculum()
            parsingStatusMessage.value = "Seeding complete Grade 5 TTY Budget of Work..."
            repository.seedDatabase()
            parsingStatusMessage.value = "Success! Standard Grade 5 AP, Science, and EPP-ICT Budget of Work loaded."
            isParsing.value = false
        }
    }

    // --- ILAW Lesson Plan Compiler & Generator ---

    fun triggerIlawGeneration() {
        if (lockdownActive.value) {
            viewModelScope.launch {
                repository.logSystemEvent(
                    "SECURITY",
                    "Compilation Blurry: Generation aborted due to active lockdown protocols."
                )
            }
            activeLessonPlanJson.value = "{\"error\": \"Active Lockdown in operations. Physical compilation suspended.\"}"
            return
        }

        isPlanGenerating.value = true
        viewModelScope.launch {
            val currentWeekVal = currentWeek.value
            val currentTermVal = currentTerm.value
            val gradeVal = planGradeLevel.value
            val subjectVal = planSubject.value
            val strategyVal = planTeachingStrategy.value
            val durationVal = planDurationMins.value
            val customPromptVal = planCustomPrompt.value
            val currentEiELevel = eieLevel.value
            val specificGradeVal = planSpecificGradeLevel.value
            val languageVal = planLanguage.value
            val ppstChecklistVal = planPpstChecklist.value.joinToString(", ")

            // Collect active standards from curriculum database to supply to key generator
            val curriculumList = curriculumItems.value
            val alignedStd = curriculumList.find {
                it.gradeLevel == gradeVal && 
                it.subject.equals(subjectVal, ignoreCase = true) &&
                it.term.equals(currentTermVal, ignoreCase = true) &&
                it.week == currentWeekVal
            } ?: curriculumList.find {
                // Fallback secondary matching ignoring term and week to be flexible
                it.gradeLevel == gradeVal && it.subject.equals(subjectVal, ignoreCase = true)
            }

            val contentStd = if (planContentStandardText.value.isNotEmpty()) planContentStandardText.value else (alignedStd?.contentStandard ?: "Demonstrate generic vocabulary acquisition and letter mapping skills.")
            val perfStd = alignedStd?.performanceStandard ?: "Produce standard language tokens or write simple descriptive sentences."
            val compStd = if (planLearningCompetencyText.value.isNotEmpty()) planLearningCompetencyText.value else (alignedStd?.learningCompetency ?: "Understand and construct vocabulary components from lessons.")

            repository.logSystemEvent(
                "COMPILER",
                "Analyzing learning standard anchors for grade $gradeVal ($subjectVal, Week $currentWeekVal). Connecting generator..."
            )

            val rawPlanJson = GeminiClient.generateIlawPlan(
                gradeLevel = gradeVal,
                subject = subjectVal,
                term = currentTermVal,
                week = currentWeekVal,
                contentStandard = contentStd,
                performanceStandard = perfStd,
                learningCompetency = compStd,
                eieLevel = currentEiELevel,
                teachingStrategy = strategyVal,
                durationMins = durationVal,
                customPrompt = customPromptVal,
                specificGradeLevel = specificGradeVal,
                language = languageVal,
                ppstChecklist = ppstChecklistVal,
                teacherName = teacherName.value,
                teacherDesignation = teacherDesignation.value,
                schoolName = schoolName.value,
                schoolDistrict = schoolDistrict.value,
                divisionOffice = divisionOffice.value,
                deliveryDate = lessonDeliveryDate.value
            )

            activeLessonPlanJson.value = rawPlanJson

            // Save plan to local Room database
            try {
                val parsed = JSONObject(rawPlanJson)
                val formativeQuestionsArr = parsed.optJSONArray("formative_questions")
                val formativeQuestionsStr = formativeQuestionsArr?.toString() ?: ""
                
                val newPlan = LessonPlan(
                    gradeLevel = gradeVal,
                    subject = subjectVal,
                    term = currentTermVal,
                    week = currentWeekVal,
                    intentions = parsed.optString("intentions", ""),
                    learningExperiences = parsed.optString("learning_experiences", ""),
                    assessment = parsed.optString("assessment", ""),
                    waysForward = parsed.optString("ways_forward", ""),
                    eieLevel = currentEiELevel,
                    teachingStrategy = strategyVal,
                    durationMins = durationVal,
                    customPrompt = customPromptVal,
                    specificGradeLevel = specificGradeVal,
                    language = languageVal,
                    ppstChecklist = ppstChecklistVal,
                    deliveryDate = lessonDeliveryDate.value,
                    sectionName = lessonSection.value,
                    periodNumbers = lessonPeriods.value.sorted().joinToString(", "),
                    formativeQuestionsJson = formativeQuestionsStr
                )
                repository.saveLessonPlan(newPlan)
                repository.logSystemEvent("COMPILER", "ILAW Lesson Plan compiled and catalogued successfully for $subjectVal.")
            } catch (e: Exception) {
                // Save fallback plain text wrapper if json parse failed completely
                val fallbackPlan = LessonPlan(
                    gradeLevel = gradeVal,
                    subject = subjectVal,
                    term = currentTermVal,
                    week = currentWeekVal,
                    intentions = rawPlanJson,
                    learningExperiences = "Direct fallback content.",
                    assessment = "Continuous feedback.",
                    waysForward = "Individual check-in.",
                    eieLevel = currentEiELevel,
                    teachingStrategy = strategyVal,
                    durationMins = durationVal,
                    customPrompt = customPromptVal,
                    specificGradeLevel = specificGradeVal,
                    language = languageVal,
                    ppstChecklist = ppstChecklistVal,
                    deliveryDate = lessonDeliveryDate.value,
                    sectionName = lessonSection.value,
                    periodNumbers = lessonPeriods.value.sorted().joinToString(", ")
                )
                repository.saveLessonPlan(fallbackPlan)
                repository.logSystemEvent("COMPILER", "Generated raw text saved in database.")
            } finally {
                isPlanGenerating.value = false
            }
        }
    }

    fun deleteLessonPlan(id: Int) {
        viewModelScope.launch {
            repository.removeLessonPlan(id)
        }
    }
    
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // --- FORMATIVE ASSESSMENT QUIZ SERVER SYSTEM ---
    fun startNewQuizSession(question: String, type: String, correct: String, activeOptions: List<String>) {
        formativeQuestionText.value = question
        formativeQuestionType.value = type
        formativeCorrectAnswer.value = correct
        formativeOptions.value = activeOptions
        formativeServerActive.value = true
        
        // Populate custom or standard student localized classroom cohort roster
        val roster = classRosterText.value.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty {
                listOf(
                    "Juan dela Cruz", "Maria Clara Santos", "Jose Bantog", "Ana Mae Rosas",
                    "Baste Magbanua", "Trixie Alcala", "Ramil Legaspi", "Cynthia Ramos",
                    "Mateo Duran", "Leonila Gomez", "Nicanor Abelardo", "Salvador Laurel",
                    "Teresa Magbanua", "Agapito Flores", "Lualhati Bautista", "Efren Peñaflorida",
                    "Gilda Cordero", "Bienvenido Lumbera", "Nick Joaquin", "Jose Rizal",
                    "Andres Bonifacio", "Emilio Jacinto", "Apolinario Mabini", "Melchora Aquino",
                    "Gabriela Silang", "Francisco Balagtas", "Graciano Lopez Jaena", "Macario Sakay",
                    "Gregorio del Pilar", "Marcelo H. del Pilar"
                )
            }
        formativeSimulatedStudents.value = roster.map { name ->
            StudentResponse(
                studentName = name,
                hasAnswered = false,
                answer = "",
                isCorrect = false,
                timestamp = "",
                score = 0
            )
        }
        viewModelScope.launch {
            repository.logSystemEvent("FORMATIVE_SERVER", "Active formative assessment polling initialized. Class Server is ONLINE.")
        }
    }

    fun simulateClassResponses() {
        if (!formativeServerActive.value) return
        
        val type = formativeQuestionType.value
        val correct = formativeCorrectAnswer.value
        val opts = formativeOptions.value
        val random = java.util.Random()
        
        val updated = formativeSimulatedStudents.value.map { currentStudent ->
            val participated = random.nextDouble() < 0.94
            if (participated) {
                val isAnswerCorrect: Boolean
                val ans: String
                
                when (type) {
                    "Multiple Choice" -> {
                        val chooseCorrect = random.nextDouble() < 0.72
                        ans = if (chooseCorrect) correct else {
                            val altOpts = opts.filter { it != correct }
                            if (altOpts.isNotEmpty()) altOpts[random.nextInt(altOpts.size)] else correct
                        }
                        isAnswerCorrect = (ans == correct)
                    }
                    "True/False" -> {
                        val chooseCorrect = random.nextDouble() < 0.82
                        ans = if (chooseCorrect) correct else {
                            if (correct.equals("True", ignoreCase = true)) "False" else "True"
                        }
                        isAnswerCorrect = ans.equals(correct, ignoreCase = true)
                    }
                    else -> { // Free text
                        val chooseCorrect = random.nextDouble() < 0.68
                        ans = if (chooseCorrect) correct else "Identified alternative community solutions."
                        isAnswerCorrect = ans.trim().equals(correct.trim(), ignoreCase = true)
                    }
                }
                
                StudentResponse(
                    studentName = currentStudent.studentName,
                    hasAnswered = true,
                    answer = ans,
                    isCorrect = isAnswerCorrect,
                    timestamp = "S: ${random.nextInt(12) + 2}",
                    score = if (isAnswerCorrect) 100 else 0
                )
            } else {
                StudentResponse(
                    studentName = currentStudent.studentName,
                    hasAnswered = false,
                    answer = "No reply",
                    isCorrect = false,
                    timestamp = "",
                    score = 0
                )
            }
        }
        formativeSimulatedStudents.value = updated
        viewModelScope.launch {
            repository.logSystemEvent("FORMATIVE_SERVER", "Received response telemetry packets from ${updated.count { it.hasAnswered }} student desks.")
        }
    }

    fun stopAndRecordSession() {
        if (!formativeServerActive.value) return
        
        val type = formativeQuestionType.value
        val correct = formativeCorrectAnswer.value
        val qText = formativeQuestionText.value
        val students = formativeSimulatedStudents.value
        
        val analysis = mutableMapOf<String, Int>()
        for (st in students) {
            val key = if (st.hasAnswered) st.answer else "No reply"
            analysis[key] = (analysis[key] ?: 0) + 1
        }
        
        val newRecord = FormativeQuizResult(
            id = (formativeHistory.value.size + 1),
            questionText = if (qText.isNotEmpty()) qText else "Formative assessment checkpoint",
            questionType = type,
            correctAnswer = correct,
            itemAnalysis = analysis,
            studentsList = students,
            timestamp = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        )
        
        formativeHistory.value = listOf(newRecord) + formativeHistory.value
        formativeServerActive.value = false
        viewModelScope.launch {
            repository.logSystemEvent("FORMATIVE_SERVER", "Class polling recorded successfully inside Local Registry DB.")
        }
    }
    
    fun resetCurrentSession() {
        formativeServerActive.value = false
        formativeSimulatedStudents.value = emptyList()
    }

    // --- TEST GENERATOR POOL ACTIONS ---
    fun addQuestionToPool(text: String, type: String, correct: String, options: List<String>) {
        val newQ = FormativeQuestion(text = text, type = type, correctAnswer = correct, options = options)
        formativeQuestionsList.value = formativeQuestionsList.value + newQ
        viewModelScope.launch {
            repository.logSystemEvent("FORMATIVE_SERVER", "Added question to standard pre-configured Test Generator: \"$text\"")
        }
    }

    fun removeQuestionFromPool(id: String) {
        val list = formativeQuestionsList.value.filter { it.id != id }
        formativeQuestionsList.value = list
        if (formativeActiveQuestionIndex.value >= list.size) {
            formativeActiveQuestionIndex.value = (list.size - 1).coerceAtLeast(0)
        }
        viewModelScope.launch {
            repository.logSystemEvent("FORMATIVE_SERVER", "Removed question from Test Generator.")
        }
    }

    fun clearQuestionPool() {
        formativeQuestionsList.value = emptyList()
        formativeActiveQuestionIndex.value = 0
        viewModelScope.launch {
            repository.logSystemEvent("FORMATIVE_SERVER", "Cleared entire Test Generator question pool.")
        }
    }

    fun selectActiveQuestionFromPool(index: Int) {
        val pool = formativeQuestionsList.value
        if (index in pool.indices) {
            formativeActiveQuestionIndex.value = index
            val q = pool[index]
            formativeQuestionText.value = q.text
            formativeQuestionType.value = q.type
            formativeCorrectAnswer.value = q.correctAnswer
            formativeOptions.value = q.options
        }
    }

    fun launchQuizFromServerPool() {
        val pool = formativeQuestionsList.value
        val index = formativeActiveQuestionIndex.value
        if (pool.isNotEmpty() && index in pool.indices) {
            val q = pool[index]
            startNewQuizSession(q.text, q.type, q.correctAnswer, q.options)
        } else {
            startNewQuizSession(
                "Verify standard PACE criteria.",
                "Multiple Choice",
                "A",
                listOf("A) Standard Met", "B) Practice Required")
            )
        }
    }

    fun seedSamplePACEPool() {
        formativeQuestionsList.value = listOf(
            FormativeQuestion(
                text = "Which of the following is an example of a Contact Force?",
                type = "Multiple Choice",
                correctAnswer = "A",
                options = listOf("A) Friction", "B) Gravity", "C) Magnetism", "D) Static Electricity")
            ),
            FormativeQuestion(
                text = "What state of matter has a definite volume but no definite shape?",
                type = "Multiple Choice",
                correctAnswer = "Liquid",
                options = listOf("Solid", "Liquid", "Gas", "Plasma")
            ),
            FormativeQuestion(
                text = "A Series Circuit has only one path for the flow of electric current.",
                type = "True/False",
                correctAnswer = "True",
                options = listOf("True", "False")
            ),
            FormativeQuestion(
                text = "What force attracts all objects towards the center of the Earth?",
                type = "Multiple Choice",
                correctAnswer = "Gravity",
                options = listOf("Gravity", "Friction", "Magnetism", "Air Resistance")
            ),
            FormativeQuestion(
                text = "What happens when two solid objects rub against each other?",
                type = "Free Text",
                correctAnswer = "Friction creates heat and opposes motion",
                options = listOf("Friction creates heat and opposes motion")
            )
        )
        formativeActiveQuestionIndex.value = 0
        viewModelScope.launch {
            repository.logSystemEvent("FORMATIVE_SERVER", "Test Generator seeded with 5 Standard PACE TTY Curriculum items.")
        }
    }

    fun resolveLocalWifiIpAddress() {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (networkInterface in java.util.Collections.list(interfaces)) {
                val inetAddresses = networkInterface.inetAddresses
                for (inetAddress in java.util.Collections.list(inetAddresses)) {
                    if (!inetAddress.isLoopbackAddress) {
                        val hostAddress = inetAddress.hostAddress
                        val isIPv4 = hostAddress.indexOf(':') < 0
                        if (isIPv4) {
                            localIpAddressState.value = hostAddress
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        localIpAddressState.value = "192.168.49.1" // Fallback Gateway for standard Direct Hotspots
    }

    fun loadAnecdotalRecords() {
        val jsonStr = prefs.getString("anecdotal_records_v1", "") ?: ""
        if (jsonStr.isNotBlank()) {
            try {
                val list = mutableListOf<AnecdotalRecord>()
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        AnecdotalRecord(
                            id = obj.getString("id"),
                            studentName = obj.getString("studentName"),
                            date = obj.getString("date"),
                            incident = obj.getString("incident"),
                            actionTaken = obj.getString("actionTaken"),
                            notes = obj.getString("notes"),
                            imagePath = if (obj.isNull("imagePath") || obj.getString("imagePath") == "null") null else obj.getString("imagePath"),
                            audioPath = if (obj.isNull("audioPath") || obj.getString("audioPath") == "null") null else obj.getString("audioPath"),
                            audioDuration = obj.optInt("audioDuration", 0)
                        )
                    )
                }
                anecdotalRecords.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveAnecdotalRecords(list: List<AnecdotalRecord>) {
        try {
            val arr = org.json.JSONArray()
            for (rec in list) {
                val obj = org.json.JSONObject()
                obj.put("id", rec.id)
                obj.put("studentName", rec.studentName)
                obj.put("date", rec.date)
                obj.put("incident", rec.incident)
                obj.put("actionTaken", rec.actionTaken)
                obj.put("notes", rec.notes)
                obj.put("imagePath", rec.imagePath)
                obj.put("audioPath", rec.audioPath)
                obj.put("audioDuration", rec.audioDuration)
                arr.put(obj)
            }
            prefs.edit().putString("anecdotal_records_v1", arr.toString()).apply()
            anecdotalRecords.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addAnecdotalRecord(
        studentName: String,
        incident: String,
        actionTaken: String,
        notes: String,
        imagePath: String?,
        audioPath: String?,
        audioDuration: Int
    ) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val currentDate = sdf.format(java.util.Date())
        val newRecord = AnecdotalRecord(
            id = java.util.UUID.randomUUID().toString(),
            studentName = studentName,
            date = currentDate,
            incident = incident,
            actionTaken = actionTaken,
            notes = notes,
            imagePath = imagePath,
            audioPath = audioPath,
            audioDuration = audioDuration
        )
        val updated = anecdotalRecords.value + newRecord
        saveAnecdotalRecords(updated)
        logSystemEvent("ANECDOTAL_EVENT", "Added anecdotal record for $studentName: $incident")
    }

    fun deleteAnecdotalRecord(id: String) {
        val updated = anecdotalRecords.value.filter { it.id != id }
        saveAnecdotalRecords(updated)
        logSystemEvent("ANECDOTAL_EVENT", "Deleted anecdotal record ID $id")
    }
}

// Data models representing Classroom response structures
data class FormativeQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val type: String, // Multiple Choice, True/False, Free Text
    val correctAnswer: String,
    val options: List<String>
)

data class StudentResponse(
    val studentName: String,
    val hasAnswered: Boolean,
    val answer: String,
    val isCorrect: Boolean,
    val timestamp: String,
    val score: Int
)

data class FormativeQuizResult(
    val id: Int,
    val questionText: String,
    val questionType: String,
    val correctAnswer: String,
    val itemAnalysis: Map<String, Int>,
    val studentsList: List<StudentResponse>,
    val timestamp: String
)

data class StudentGrades(
    val studentName: String,
    val wwScores: List<String>,
    val ptScores: List<String>,
    val saScores: List<String>,
    val termExamScore: String
)

data class AnecdotalRecord(
    val id: String,
    val studentName: String,
    val date: String,
    val incident: String,
    val actionTaken: String,
    val notes: String,
    val imagePath: String? = null,
    val audioPath: String? = null,
    val audioDuration: Int = 0
)
