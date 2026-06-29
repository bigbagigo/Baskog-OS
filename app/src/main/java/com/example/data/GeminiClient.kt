package com.example.data

import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

object GeminiClient {

    var userOverrideApiKey: String = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val TAG = "GeminiClient"

    /**
     * Call the Gemini 3.5 Flash Model to generate a four-part ILAW lesson plan.
     * Incorporates custom Educator instructions, pedagogic strategy, block duration, and active EiE safety protocol.
     */
    suspend fun generateIlawPlan(
        gradeLevel: String,
        subject: String,
        term: String,
        week: Int,
        contentStandard: String,
        performanceStandard: String,
        learningCompetency: String,
        eieLevel: String,
        teachingStrategy: String,
        durationMins: Int,
        customPrompt: String,
        specificGradeLevel: String = "",
        language: String = "English",
        ppstChecklist: String = "",
        teacherName: String = "JOSEPH DANIEL DURAN",
        teacherDesignation: String = "Teacher III",
        schoolName: String = "Tala Elementary School",
        schoolDistrict: String = "District II",
        divisionOffice: String = "Division of Masbate",
        deliveryDate: String = "Monday"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (userOverrideApiKey.isNotBlank()) userOverrideApiKey else BuildConfig.GEMINI_API_KEY
        val dayNum = when (deliveryDate.trim().lowercase()) {
            "monday" -> 1
            "tuesday" -> 2
            "wednesday" -> 3
            "thursday" -> 4
            "friday" -> 5
            "saturday" -> 6
            "sunday" -> 7
            else -> 1
        }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API Key is placeholder. Falling back to local offline generation.")
            return@withContext getOfflineBackupPlan(gradeLevel, subject, term, week, contentStandard, performanceStandard, learningCompetency, eieLevel, teachingStrategy, durationMins, customPrompt, specificGradeLevel, language, ppstChecklist, teacherName, teacherDesignation, schoolName, schoolDistrict, divisionOffice, deliveryDate)
        }

        val prompt = """
            You are the specialized educational hypervisor AI (Cognitive Core of MARAGTASON project) for the BASKOG system under the DepEd Three-Term Year (TTY) policy.
            Your highest objective is to generate an EXTREMELY ROBUST, complete, structurally sound, and ready-to-teach weekly lesson plan of 4 progressive sessions (Session 1 to 4) representing a full week of learning.
            
            [CRITICAL CONSTRAINTS - MUST OBEY OR PENALTY]
            1. NO PLACEHOLDERS: NEVER use any placeholder text, bracketed descriptions like '[insert title]', '[Objective 1]', '# Session 1 Flow', parenthetical draft notations, or template variables like '[Provide distractor]'. Every single sentence must be written out as a fully formed, realistic, rich, and high-quality instruction.
            2. ALL PROGRESSIVE SESSIONS: The 4 sessions must show a progressive, coherent journey without any repetitive copy-pasted sections.
            3. NO SKELETON OUTLINES OR DUMMIES: Do not output '#', empty comments, or generic outlines. Generate complete narrative paragraphs of instruction.
            4. FORMATIVE QUESTIONS QUANTITY: You MUST generate EXACTLY 5 high-quality questions inside the "formative_questions" array. No more and no less.
            5. FORMATIVE QUESTIONS TOPIC COHERENCE: All 5 questions MUST be strictly, 100% focused on testing the ACTUAL learning competency of this specific block ($learningCompetency) under $subject. If the lesson competency is about circuits, then all 5 questions must test circuits. If it is about gravity, all 5 questions must test gravity. NEVER mix unrelated physics, biology, or Chemistry subjects unless explicitly defined in $learningCompetency. No mismatched random questions from outside of this actual competency are permitted!
            6. SUBJECT-AWARE INSTUCTIONAL DESIGN: You MUST generate activities, questions, assessments, and localized materials that are strictly appropriate for the academic domain of $subject. If $subject is Araling Panlipunan, you are STRICTLY FORBIDDEN from including science-lab terms, laboratory equipment, test tubes, solids/liquids properties, measurements of liquids, or particle diagrams. Instead, use historical source readings, timelines, map work, group discussions, comparing and contrasting viewpoints, and short explanatory or reflective essays.
            7. ANTI-COMPETENCY TEXT STUFFING: Do NOT copy and paste the raw competency statement ($learningCompetency) as a lazy noun phrase into template shells (e.g., do NOT write "identify the fundamental elements of $learningCompetency" or "find examples of $learningCompetency in your home kitchen"). You MUST decompose the competency before writing: unpack it into its underlying content concepts, key skills, subtopics, and actual historical/scientific/mathematical details, and use those concrete details to draft objectives and tasks.
            8. SANITY CHECK: Ensure every homework and activity makes complete sense for a Grade $specificGradeLevel student in their actual home or community context. For example, "find examples of early human origin theories in your kitchen" is nonsensical; instead, ask them to interview family elders about traditional narratives, or draw a historical tool.
            
            [GENERAL INFORMATION]
            School: $schoolName
            District/Division: $schoolDistrict, $divisionOffice
            Teacher: $teacherName ($teacherDesignation)
            Grade Level: $specificGradeLevel ($gradeLevel Stage scale)
            Learning Area: $subject
            Quarter/Term: TTY Period $term Block
            Week: Week $week (Weekly 4-Session Block)
            Medium of Instruction/Language Focus: $language

            [CURRICULUM MAPPING]
            Content Standard: $contentStandard
            Performance Standard: $performanceStandard
            Learning Competency (MELC): $learningCompetency

            [PEDAGOGIC CONFIGURATIONS]
            Teaching Strategy Method: $teachingStrategy
            Targeted PPST Standards Indicator Checklist: $ppstChecklist
            Allocated Duration: $durationMins Minutes per Session
            Educator Intentional Directives / Focus Prompts: ${if (customPrompt.isBlank()) "Provide a thorough, highly inclusive instruction plan." else customPrompt}

            [EMERGENCY LEVEL PROTOCOLS (EiE)]
            Active Continuity Level: $eieLevel
            (EiE Guidance Matrix:
             - 'Hayo': Normal active operations. Heavy classroom and collaborative activities.
             - 'Hinay': Moderate caution. Modified group tasks, safe distances, self-paced helpers.
             - 'Hinga': Alternative/Home modes active. Self-directed interactive modules, minor contact.
             - 'Hinto': Critical lockdown. All online/distance activity packets, no physical elements, focus on security and emotional support.)

            You MUST write the response as a single raw JSON object matching this schema exactly. Every string value ('intentions', 'learning_experiences', 'assessment', 'ways_forward') must contain the ACTUAL fully written, comprehensive, ready-to-teach Markdown prose, NOT copy-pasted instruction descriptions:
            {
              "intentions": "A complete, deeply-articulated Markdown document starting with header '## I. Intentions (Layunin)'. Include a prominent '**Target Placement:** Term $term Block, Week $week, Day: $deliveryDate' placeholder block at the very top of Intentions so the educator knows exactly where to place this plan. Explicitly integrate and discuss the content standard ($contentStandard), performance standard ($performanceStandard), and learning competency ($learningCompetency) as a narrative. Write 2-3 detailed paragraphs detailing the 4-session learning progression, and write out 3 specific learning objectives per session representing a logical milestone journey. Use zero placeholders or draft templates.",
              "learning_experiences": "A complete, highly comprehensive Markdown document starting with header '## II. Learning Experiences (Nilalaman)'. Identify and list localized physical resources first. Show details of the lesson procedures across the 4 sessions. Each logging procedure needs to thoroughly map out Elicit, Engage, Explore, Explain, and Elaborate phases. Write out teacher questions and student activity tasks as full prose. Do not leave placeholder sections or brackets.",
              "assessment": "A complete, beautiful Markdown document starting with header '## III. Assessment (Pagtataya)'. Detail 4 daily formative checks (one for each session) assessing student understanding of $learningCompetency. Detail the active evaluation rubrics. If Grade Level is KS1, provide comprehensive qualitative PACE descriptors: Matingkad (Bright/Excellent), Sapat (Sufficient/Satisfactory), Nagsisimula (Beginning). If Grade Level is KS2, KS3, or KS4, detail standard 20% Written Works, 50% Performance Tasks, and 30% Summative Assessment weights, explicitly enforcing the 75% transitional grading floor policy for 70% raw performance outcomes. Specify AI usage rules (prohibited for students).",
              "ways_forward": "A complete, ready-to-use Markdown document starting with header '## IV. Ways Forward (Pagninilay)'. Detail customized localized homework assignments for all 4 sessions. Provide complete narrative paragraphs of instruction for remediation (scored below the 75% floor, utilizing peer-scaffold models) and enrichment paths (advanced diagramming or peer mentoring). No placeholders are permitted.",
              "formative_questions": [
                {
                  "question": "First customized multiple-choice question testing the core subject of $learningCompetency. No placeholders or brackets allowed.",
                  "type": "Multiple Choice",
                  "correct_answer": "A",
                  "options": ["A) First option (write out the complete correct answer text)", "B) Second option (write out realistic distractor text)", "C) Third option (write out realistic distractor text)", "D) Fourth option (write out realistic distractor text)"]
                },
                {
                  "question": "Second customized multiple-choice question testing the core subject of $learningCompetency. No placeholders or brackets allowed.",
                  "type": "Multiple Choice",
                  "correct_answer": "B",
                  "options": ["A) First option (write out realistic distractor text)", "B) Second option (write out the complete correct answer text)", "C) Third option (write out realistic distractor text)", "D) Fourth option (write out realistic distractor text)"]
                },
                {
                  "question": "Third customized multiple-choice question testing the core subject of $learningCompetency. No placeholders or brackets allowed.",
                  "type": "Multiple Choice",
                  "correct_answer": "C",
                  "options": ["A) First option (write out realistic distractor text)", "B) Second option (write out realistic distractor text)", "C) Third option (write out the complete correct answer text)", "D) Fourth option (write out realistic distractor text)"]
                },
                {
                  "question": "Fourth customized multiple-choice question testing the core subject of $learningCompetency. No placeholders or brackets allowed.",
                  "type": "Multiple Choice",
                  "correct_answer": "D",
                  "options": ["A) First option (write out realistic distractor text)", "B) Second option (write out realistic distractor text)", "C) Third option (write out realistic distractor text)", "D) Fourth option (write out the complete correct answer text)"]
                },
                {
                  "question": "Fifth customized multiple-choice question testing the core subject of $learningCompetency. No placeholders or brackets allowed.",
                  "type": "Multiple Choice",
                  "correct_answer": "A",
                  "options": ["A) First option (write out the complete correct answer text)", "B) Second option (write out realistic distractor text)", "C) Third option (write out realistic distractor text)", "D) Fourth option (write out realistic distractor text)"]
                }
              ]
            }

            Rule: Return ONLY standard raw JSON. Do not write any preamble, intro, or markdown tick blocks. Do not use words like 'significant', 'optimize', 'leverage', or 'streamline' in your text content. Ensure all keys and strings are fully escaped JSON.
        """.trimIndent()

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
            
            val textPart = JSONObject().put("text", prompt)
            val partsArray = JSONArray().put(textPart)
            val contentObj = JSONObject().put("parts", partsArray)
            val contentsArray = JSONArray().put(contentObj)
            val payload = JSONObject().put("contents", contentsArray)

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "API failed with code ${response.code}. Using offline backup.")
                return@withContext getOfflineBackupPlan(gradeLevel, subject, term, week, contentStandard, performanceStandard, learningCompetency, eieLevel, teachingStrategy, durationMins, customPrompt, specificGradeLevel, language, ppstChecklist, teacherName, teacherDesignation, schoolName, schoolDistrict, divisionOffice, deliveryDate)
            }

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val rawText = parts.getJSONObject(0).getString("text")

            return@withContext cleanJsonResponse(rawText)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content: ${e.message}", e)
            return@withContext getOfflineBackupPlan(gradeLevel, subject, term, week, contentStandard, performanceStandard, learningCompetency, eieLevel, teachingStrategy, durationMins, customPrompt, specificGradeLevel, language, ppstChecklist, teacherName, teacherDesignation, schoolName, schoolDistrict, divisionOffice, deliveryDate)
        }
    }

    private fun cleanJsonResponse(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        clean = clean.trim()
        
        try {
            JSONObject(clean)
            return clean
        } catch (e: Exception) {
            Log.e(TAG, "Cleaning JSON failed: ${e.message}. Packaging block.", e)
            val backup = JSONObject()
            backup.put("intentions", "Introduce foundational lessons aligned with standard competencies.")
            backup.put("learning_experiences", "Step-by-step reading activities built using collaborative strategies.")
            backup.put("assessment", "Verify results according to stage specifications (Qualitative or Quantitative scales).")
            backup.put("ways_forward", "Individual tutorial and home support modules as needed.")
            return backup.toString()
        }
    }

    /**
     * Extracts text from low-level page Bitmaps using the Gemini 3.5 Flash multimodal stream.
     */
    suspend fun performGeminiOcr(bitmap: android.graphics.Bitmap): String = withContext(Dispatchers.IO) {
        val apiKey = if (userOverrideApiKey.isNotBlank()) userOverrideApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "[Error: Gemini API Key is missing or default. Cannot run AI-powered OCR fallback. Please insert your API key in the Settings section.]"
        }

        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
        val base64Image = android.util.Base64.encodeToString(byteArrayOutputStream.toByteArray(), android.util.Base64.NO_WRAP)

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

            val textPart = JSONObject().put("text", "Extract all visible text from this document page image with absolute high fidelity. Do not include introductory notes, chat filler, or comments—return only the transcribed text exactly as written.")
            val inlineDataObj = JSONObject()
                .put("mimeType", "image/jpeg")
                .put("data", base64Image)
            val imagePart = JSONObject().put("inlineData", inlineDataObj)

            val partsArray = JSONArray().put(textPart).put(imagePart)
            val contentObj = JSONObject().put("parts", partsArray)
            val contentsArray = JSONArray().put(contentObj)
            val payload = JSONObject().put("contents", contentsArray)

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "[Error: OCR generation failed with HTTP response code ${response.code}]"
            }

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            return@withContext parts.getJSONObject(0).getString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Gemini OCR execution failed", e)
            val msg = e.message ?: ""
            if (e is java.net.UnknownHostException || msg.contains("resolve", ignoreCase = true) || msg.contains("host", ignoreCase = true)) {
                return@withContext "[Error: Gemini OCR is offline. Please check your internet connection or secure key configuration.]"
            }
            return@withContext "[Error: OCR extraction threw exception: ${e.message}]"
        }
    }

    /**
     * Call the Gemini 3.5 Flash Model to sort, clean, and classify unstructured
     * curriculum/MELC/BOW/OCR text into a clean JSON array structure.
     */
    suspend fun structureCurriculumViaGemini(rawText: String): String = withContext(Dispatchers.IO) {
        val apiKey = if (userOverrideApiKey.isNotBlank()) userOverrideApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API Key is missing for preprocessing. Falling back to local parser.")
            return@withContext ""
        }

        val prompt = """
            You are the specialized educational hypervisor AI for the BASKOG/MARAGTASON project under the DepEd Three-Term Year (TTY) policy.
            You have received raw, unstructured, copy-pasted, or OCR-transcribed curriculum/MELC/BOW text.
            Your task is to analyze, clean, classify, and sort this data before ingesting it into our KABAN Curriculum database.

            Restructure the provided text into a clean, complete, and standard JSON array of curriculum alignment objects:
            - gradeLevel: Must be EXACTLY "KS1" (K to Gr3), "KS2" (Gr4 to Gr6), "KS3" (Gr7 to Gr10), or "KS4" (Gr11 to Gr12). Ensure you map numerical grade levels to their corresponding Key Stage.
            - subject: Clear, standardized, properly capitalized academic subject name (e.g. "Reading Literacy", "Mathematics", "Science", "English", "Filipino", "Araling Panlipunan", "MAPEH", "EPP/TLE"). It must be a standard subject name, NEVER a random word, and NEVER an entire action/standard sentence. Resolve all spelling mistakes or mismatched words.
            - term: Must be EXCLUSIVELY "1st Term", "2nd Term", or "3rd Term".
            - week: Integer index from 1 to 10.
            - melcCode: Alphanumeric standard MELC code if visible (e.g. "M1NS-Ia-1.1"). If missing or corrupted, generate a logical clean code like "MELC-MATH-W3".
            - sessionsBudgeted: Integer (default 5).
            - contentStandard: The standard outlining core understanding or concepts. If missing or incomplete, extrapolate a brief, grammatically complete, relevant standard.
            - performanceStandard: The standard outlining expected application. If missing or incomplete, extrapolate a brief, grammatically complete, relevant standard.
            - learningCompetency: The specific learning competency or competency objective statement.

            Rule: Return ONLY standard raw JSON array. Do not write any preamble, intro, or markdown tick blocks. Do not use words like 'significant', 'optimize', 'leverage', or 'streamline' in your text content.

            Raw data to parse/classify:
            ---------------------
            $rawText
            ---------------------
        """.trimIndent()

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

            val textPart = JSONObject().put("text", prompt)
            val partsArray = JSONArray().put(textPart)
            val contentObj = JSONObject().put("parts", partsArray)
            val contentsArray = JSONArray().put(contentObj)
            val payload = JSONObject().put("contents", contentsArray)

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Curriculum preprocessing failed with status code ${response.code}")
                return@withContext ""
            }

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val rawTextResult = parts.getJSONObject(0).getString("text")

            return@withContext cleanJsonResponse(rawTextResult)
        } catch (e: Exception) {
            Log.e(TAG, "Error in curriculum preprocessing via Gemini: ${e.message}", e)
            return@withContext ""
        }
    }

    /**
     * Local fallback lesson plan generator integrating all customized configuration fields.
     */
    fun getOfflineBackupPlan(
        gradeLevel: String,
        subject: String,
        term: String,
        week: Int,
        contentStandard: String,
        performanceStandard: String,
        learningCompetency: String,
        eieLevel: String,
        teachingStrategy: String,
        durationMins: Int,
        customPrompt: String,
        specificGradeLevel: String = "",
        language: String = "English",
        ppstChecklist: String = "",
        teacherName: String = "JOSEPH DANIEL DURAN",
        teacherDesignation: String = "Teacher III",
        schoolName: String = "Tala Elementary School",
        schoolDistrict: String = "District II",
        divisionOffice: String = "Division of Masbate",
        deliveryDate: String = "Monday"
    ): String {
        val root = JSONObject()
        val dayNum = when (deliveryDate.trim().lowercase()) {
            "monday" -> 1
            "tuesday" -> 2
            "wednesday" -> 3
            "thursday" -> 4
            "friday" -> 5
            "saturday" -> 6
            "sunday" -> 7
            else -> 1
        }
        
        // Anti-competency text stuffing cleaning
        fun getCleanTopic(competency: String): String {
            var comp = competency.trim()
            val prefixes = listOf(
                "nasusuri ang", "naipaliliwanag ang", "natutukoy ang", "nailalarawan ang", "napahahalagahan ang",
                "nakapagsasagawa ng", "naipakikita ang", "nakasusulat ng", "nasusuri", "naipaliliwanag",
                "natutukoy", "nailalarawan", "napahahalagahan", "identify", "explain", "describe", "analyze",
                "demonstrate", "understand"
            )
            for (prefix in prefixes) {
                if (comp.lowercase().startsWith(prefix)) {
                    comp = comp.substring(prefix.length).trim()
                    break
                }
            }
            if (comp.endsWith(".")) comp = comp.removeSuffix(".")
            return comp.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        val cleanTopic = getCleanTopic(learningCompetency).ifBlank { "the core concepts aligned to the DepEd learning competencies" }
        val cleanSubject = subject.ifBlank { "General Education" }
        val cleanCS = contentStandard.ifBlank { "the designated thematic content metrics of the DepEd budget of work" }
        val cleanPS = performanceStandard.ifBlank { "the application of core conceptual mastery to everyday situations" }

        val subjectLower = cleanSubject.lowercase()
        val isAralingPanlipunan = subjectLower.contains("araling panlipunan") || subjectLower.contains("ap") || subjectLower.contains("social") || subjectLower.contains("history") || subjectLower.contains("kasaysayan")
        val isScience = subjectLower.contains("science") || subjectLower.contains("agham")
        val isMath = subjectLower.contains("math") || subjectLower.contains("mathematics") || subjectLower.contains("aritmetika")
        val isEnglishOrReading = subjectLower.contains("english") || subjectLower.contains("reading") || subjectLower.contains("literacy") || subjectLower.contains("wika") || subjectLower.contains("mother tongue")

        val intentionsMarkdown: String
        val experiencesMarkdown: String
        val assessmentMarkdown: String
        val waysForwardMarkdown: String
        val qArray = JSONArray()

        if (isAralingPanlipunan) {
            // Araling Panlipunan (AP) Offline Backup
            intentionsMarkdown = """
## I. Intentions (Layunin)

### Kurikulum at Pamantayan (Curriculum & Standards)
Ang linggong ito ay nakatuon sa malalim na pagsusuri at pag-unawa sa paksa ukol sa $cleanTopic. Layunin ng araling ito na hubugin ang kamalayan ng mga mag-aaral sa kanilang kasaysayan at kultura sa pamamagitan ng pagsusuri ng iba't ibang batayan at teorya.

- **Profile ng Guro**: $teacherName | Designation: $teacherDesignation
- **Paaralan**: $schoolName | Distrito/Dibisyon: $schoolDistrict, $divisionOffice
- **Kuwarter at Termino**: Term Block $term, Linggo $week
- **Baitang at Sakop**: Grade $specificGradeLevel ($gradeLevel Key Stage)
- **Pamamaraan ng Pagtuturo**: $teachingStrategy, umaayon sa $eieLevel safety guidelines.

- **Pamantayang Pangnilalaman (Content Standard)**: $cleanCS.
- **Pamantayan sa Pagganap (Performance Standard)**: $cleanPS.

### Mga Tiyak na Layunin kada Sesyon (Specific Session Objectives)
1. **Sesyon 1 (Oryentasyon sa Teorya)**: Mailahad at matukoy ang mga pangunahing teorya ukol sa $cleanTopic, na nagbibigay-pansin sa mga unang pananaw ng mga mananaliksik.
2. **Sesyon 2 (Siyentipiko at Arkeolohikal na Pagsusuri)**: Masuri ang mga arkeolohikal na ebidensya at siyentipikong pag-aaral na nagpapatunay sa mga teoryang tinalakay sa Sesyon 1.
3. **Sesyon 3 (Kaalamang Bayan at Tradisyon)**: Maipaghambing ang siyentipikong teorya sa mga lokal kuwentong-bayan at mitolohiya tungkol sa pinagmulan ng tao.
4. **Sesyon 4 (Pagbubuod at Repleksyon)**: Makabuo ng sariling maikling sanaysay o visual na timeline na nagpapakita ng pinag-isang pag-unawa sa kasaysayan ng $cleanTopic.
            """.trimIndent()

            experiencesMarkdown = """
## II. Learning Experiences (Nilalaman)

### Mga Kagamitang Pampagtuturo (Localized Resources)
- **Lokal na Sanggunian**: Mga sipi mula sa lokal na kasaysayan, mga lumang mapa, mga kuwentong-bayan ng Masbate, at mga activity sheets.
- **Kagamitang Biswal**: Mga larawan ng Manunggul Jar, fossils, at timeline charts ng pagdating ng mga unang tao.
- **Gabay sa Kurikulum**: DepEd Division Learning Materials at Araling Panlipunan Curriculum Guides.

### Lingguhang Proseso ng Pagtuturo (Weekly Instructional Procedures)

#### Sesyon 1: Pagpapakilala sa mga Teorya (45 Minuto)
- **Elicit (5 Minuto)**: Simulan sa pagpapakita ng larawan ng bansa at itanong kung paano nabuo ang unang komunidad ng mga Pilipino.
- **Engage (10 Minuto)**: Ibahagi ang maikling salaysay tungkol sa paglalakbay ng mga sinaunang tao sa pamamagitan ng tulay na lupa o paglalayag.
- **Explore (15 Minuto)**: Sa tulong ng timeline chart, talakayin ng mga mag-aaral ang pagkakaiba ng Teoryang Wave of Migration at Out-of-Taiwan sa ilalim ng gabay para sa $eieLevel.
- **Explain (10 Minuto)**: Ipaliwanag ang kahulugan ng bawat teorya at ang mga ebidensyang sumusuporta rito.
- **Elaborate (5 Minuto)**: Iugnay ang mga teorya sa pagkakaroon ng iba't ibang wika sa bansa.

#### Sesyon 2: Arkeolohiya at Siyensya (45 Minuto)
- **Elicit (5 Minuto)**: Balikan ang mga teoryang tinalakay sa unang sesyon.
- **Engage (5 Minuto)**: Magpakita ng larawan ng kuweba (tulad ng Tabon o Callao) at tanungin kung ano ang maaaring matagpuan sa loob nito.
- **Explore (20 Minuto)**: Suriin ang mga sipi tungkol sa pagkakatuklas ng mga fossil o artifacts na nagpapatunay sa presensya ng sinaunang tao.
- **Explain (10 Minuto)**: Ipaliwanag ang papel ng siyensya at arkeolohiya sa pagtatakda ng edad ng mga artifacts.
- **Elaborate (5 Minuto)**: Talakayin kung paano binabago ng bagong tuklas ang mga nakasulat na sa ating mga aklat ng kasaysayan.

#### Sesyon 3: Pagsusuri ng Alamat at Kaalamang Bayan (45 Minuto)
- **Elicit (5 Minuto)**: Itanong sa klase kung sino ang pamilyar sa alamat nina Malakas at Maganda.
- **Engage (5 Minuto)**: Ilahad ang maikling kuwento ukol sa paglitaw ng tao mula sa kawayan o iba pang lokal na mitolohiya.
- **Explore (20 Minuto)**: Pangkatin ang klase (o isagawa nang isahan kung $eieLevel ay mahigpit) upang paghambingin ang Alamat (Kaalamang Bayan) at Arkeolohiya (Siyensya) gamit ang Venn Diagram.
- **Explain (10 Minuto)**: Talakayin na ang kaalamang bayan ay sumasalamin sa kultura, sining, at pagpapahalaga ng ating mga ninuno, habang ang agham ay naghahanap ng pisikal na patunay.
- **Elaborate (5 Minuto)**: Ipaliwanag ang kahalagahan ng pagrespeto sa parehong aspeto ng ating pagkakakilanlan.

#### Sesyon 4: Pagbubuod at Pagsasapraktika (45 Minuto)
- **Elicit (5 Minuto)**: Mabilisang pagsusuri ng Venn Diagram na ginawa noong nakaraang sesyon.
- **Engage (5 Minuto)**: Hamunin ang mga mag-aaral na mag-isip kung paano nila maipagmamalaki ang kanilang lahi sa kasalukuyang panahon.
- **Explore (15 Minuto)**: Pagsulat ng sariling maikling paglalahad o pagguhit ng timeline na nagpapakita ng pinagmulan ng tao batay sa natutunan.
- **Explain (10 Minuto)**: Pagbabahagi ng ilang gawa ng mag-aaral sa klase at pagbibigay ng positibong puna ng guro.
- **Elaborate (10 Minuto)**: Magdaos ng maikling pagpapahalaga sa pagiging bahagi ng mayamang kultura ng Pilipinas bago magtapos ang klase.
            """.trimIndent()

            assessmentMarkdown = when (gradeLevel) {
                "KS1" -> """
## III. Assessment (Pagtataya)

### Lingguhang Pagtataya sa Bawat Sesyon (Formative Assessments)
- **Sesyon 1**: Pagsagot sa maikling talahanayan ng mga teorya ng pinagmulan.
- **Sesyon 2**: Checklist ng pagsusuri sa kahalagahan ng mga arkeolohikal na labi.
- **Sesyon 3**: Pagpuno sa Venn Diagram (Agham vs. Kaalamang Bayan).
- **Sesyon 4**: Pagsulat ng 3-pangungusap na repleksyon ukol sa pinagmulan ng sinaunang tao.

### Patakaran sa Pagtatasa (PACE Scale Compliance)
- **Para sa Baitang sa ilalim ng Key Stage 1 (KS1)**: Ang pagtatasa ay nakatuon sa husay at pag-unlad ng mag-aaral gamit ang PACE indicators. Walang numerical grades na ilalagay. Ang antas ay itatala bilang:
  - **Matingkad** (Nagpapakita ng malalim at sariling pagsisikap sa pag-unawa).
  - **Sapat** (Nagpapakita ng tamang antas ng pag-unawa sa tulong ng guro).
  - **Nagsisimula** (Nangangailangan pa ng karagdagang gabay at pagsasanay).

### AI Ethics Policy
- **Kategorya 1 (Mahigpit na Ipinagbabawal)**: Bawal gumamit ang mga mag-aaral ng AI tools sa kanilang mga takdang-aralin o pagsusulit.
- **Kategorya 2 (Limitadong Gabay sa Guro)**: Ang AI ay gagamitin lamang bilang katulong ng guro sa pagbuo ng disenyo ng aralin.
                """.trimIndent()
                else -> """
## III. Assessment (Pagtataya)

### Lingguhang Pagtataya sa Bawat Sesyon (Formative Assessments)
- **Sesyon 1**: Pagsagot sa maikling talahanayan ng mga teorya ng pinagmulan.
- **Sesyon 2**: Checklist ng pagsusuri sa kahalagahan ng mga arkeolohikal na labi.
- **Sesyon 3**: Pagpuno sa Venn Diagram (Agham vs. Kaalamang Bayan).
- **Sesyon 4**: Pagsulat ng 3-pangungusap na repleksyon ukol sa pinagmulan ng sinaunang tao.

### Patakaran sa Pagtatasa (DepEd Grading Weights)
- **Para sa Baitang sa ilalim ng Key Stage 2 hanggang 4 (KS2-KS4)**: Ang grado ay kinokompyut gamit ang opisyal na timbang ng DepEd: **20% Written Works**, **50% Performance Tasks**, at **30% Summative Assessment**.
- **Transitional Grading Floor**: Mahigpit na ipinapatupad ang 75% grading floor kung saan ang raw score na 70% ay awtomatikong isasalin sa 75% upang hikayatin ang mga mag-aaral na magpatuloy sa pag-aaral.

### AI Ethics Policy
- **Kategorya 1 (Mahigpit na Ipinagbabawal)**: Bawal gumamit ang mga mag-aaral ng AI tools sa kanilang mga takdang-aralin o pagsusulit.
- **Kategorya 2 (Limitadong Gabay sa Guro)**: Ang AI ay gagamitim lamang bilang katulong ng guro sa pagbuo ng disenyo ng aralin.
                """.trimIndent()
            }

            waysForwardMarkdown = """
## IV. Ways Forward (Pagninilay)

### Mga Takdang-Aralin (Homework per Session)
- **Takdang-Aralin 1**: Magtanong sa magulang o nakatatanda ng isang lokal na alamat o kuwento tungkol sa pinagmulan ng inyong komunidad o bayan.
- **Takdang-Aralin 2**: Gumuhit o mag-paste ng larawan ng isang kilalang artifact tulad ng Manunggul Jar o Tabon cave sa inyong kuwaderno.
- **Takdang-Aralin 3**: Isulat kung paano mo pahahalagahan ang mga tradisyon at alamat na ikinukuwento ng iyong pamilya.
- **Takdang-Aralin 4**: Sumulat ng maikling talata na nagpapahayag ng pasasalamat sa mga arkeologo at historyador na naghahanap ng ating kasaysayan.

### Remediation at Enrichment Plans
- **Plano sa Remediation (Para sa mag-aaral na nakakuha ng mababa sa 75%):** Pagbibigay ng simpleng babasahin na may malalaking larawan at paggamit ng peer tutoring o gabay mula sa kamag-aral upang mas madaling maunawaan ang mga konsepto.
- **Plano sa Enrichment (Para sa mga mabilis matuto):** Paggawa ng mas malalim na pagsusuri sa iba pang teorya o pagsulat ng isang kathang-isip ngunit makasaysayang talaarawan (diary) ng isang sinaunang Pilipino.
            """.trimIndent()

            // Generate 5 AP questions
            val q1 = JSONObject()
            q1.put("question", "Aling teorya ang nagpapaliwanag na ang mga sinaunang tao sa Pilipinas ay nagmula sa Timog Tsina at Taiwan at naglakbay gamit ang mga bangka?")
            q1.put("type", "Multiple Choice")
            q1.put("correct_answer", "A")
            val opts1 = JSONArray()
            opts1.put("A) Teoryang Out-of-Taiwan ni Peter Bellwood")
            opts1.put("B) Wave of Migration Theory ni H. Otley Beyer")
            opts1.put("C) Core Population Theory ni Felipe Landa Jocano")
            opts1.put("D) Solheim's Island Origin Theory")
            q1.put("options", opts1)
            qArray.put(q1)

            val q2 = JSONObject()
            q2.put("question", "Anong arkeolohikal na ebidensya ang natuklasan sa Palawan na nagpapatunay na may nanirahan nang sinaunang tao sa Pilipinas libu-libong taon na ang nakalipas?")
            q2.put("type", "Multiple Choice")
            q2.put("correct_answer", "B")
            val opts2 = JSONArray()
            opts2.put("A) Ang Ginto ng Surigao")
            opts2.put("B) Ang Labi ng Tabon Man")
            opts2.put("C) Ang Banga ng Manunggul")
            opts2.put("D) Ang Callao Man Fossil")
            q2.put("options", opts2)
            qArray.put(q2)

            val q3 = JSONObject()
            q3.put("question", "Paano nakatutulong ang mga kaalamang bayan tulad ng alamat at mitolohiya sa pag-unawa sa kasaysayan ng ating mga ninuno?")
            q3.put("type", "Multiple Choice")
            q3.put("correct_answer", "C")
            val opts3 = JSONArray()
            opts3.put("A) Sila lamang ang tanging maaasahang batayan ng eksaktong petsa ng kasaysayan.")
            opts3.put("B) Pinatutunayan nito na walang halaga ang mga siyentipikong pag-aaral.")
            opts3.put("C) Ipinapakita nito ang kultura, paniniwala, at pamumuhay ng ating mga ninuno sa paraang pasalita.")
            opts3.put("D) Sila ay nagbibigay ng mga modernong formula sa pag-aaral ng pisika.")
            q3.put("options", opts3)
            qArray.put(q3)

            val q4 = JSONObject()
            q4.put("question", "Bakit mahalagang gamitin ang parehong agham (arkeolohiya) at kaalamang bayan sa pag-aaral ng pinagmulan ng sinaunang tao?")
            q4.put("type", "Multiple Choice")
            q4.put("correct_answer", "D")
            val opts4 = JSONArray()
            opts4.put("A) Upang mapatunayan na laging mali ang mga kuwentong-bayan.")
            opts4.put("B) Upang maiwasan ang pagbabasa ng mahihirap na aklat sa paaralan.")
            opts4.put("C) Upang hindi na maging mahalaga ang mga museo sa ating bansa.")
            opts4.put("D) Upang magkaroon ng balanseng pananaw sa pisikal na ebidensya at kultural na kasaysayan.")
            q4.put("options", opts4)
            qArray.put(q4)

            val q5 = JSONObject()
            q5.put("question", "Sino ang tinutukoy na pinakabagong species ng sinaunang tao na natuklasan sa Kawayan Cave sa Cagayan na nabuhay higit 50,000 taon na ang nakalipas batay sa agham?")
            q5.put("type", "Multiple Choice")
            q5.put("correct_answer", "A")
            val opts5 = JSONArray()
            opts5.put("A) Homo luzonensis (Callao Man)")
            opts5.put("B) Tabon Man")
            opts5.put("C) Homo erectus")
            opts5.put("D) Homo sapiens")
            q5.put("options", opts5)
            qArray.put(q5)

        } else if (isScience) {
            // Science Offline Backup
            intentionsMarkdown = """
## I. Intentions (Layunin)

### Curriculum Integration & Framework
The intentional core of this instructional week centers on establishing a profound cognitive and skill-based connection with standard competencies. Under the official DepEd guidelines, this standard outlines that learners will actively examine, observe, and investigate the fundamental components of $cleanTopic.

- **Teacher Profile**: $teacherName | Designation: $teacherDesignation
- **School Location**: $schoolName under the $schoolDistrict, $divisionOffice
- **Quarter & Term Reference**: Term Block $term, Week $week
- **Grade & Target Scope**: Grade $specificGradeLevel ($gradeLevel Key Stage)
- **Pedagogical Alignment**: Designed using the $teachingStrategy method to accommodate active $eieLevel safety guidelines.

- **Content Standard Integration**: $cleanCS.
- **Performance Standard Target**: $cleanPS.

### Specific Session Objectives:
1. **Session 1 (Inquiry & Observation)**: Identify and describe the primary visible properties of $cleanTopic through direct, safe observation.
2. **Session 2 (Inquiry & Practical Modeling)**: Group or model representations of $cleanTopic using localized materials to clarify its physical properties.
3. **Session 3 (Comparative Measurement)**: Measure, compare, and record variations of $cleanTopic under specified conditions.
4. **Session 4 (Synthesis & Scientific Review)**: Synthesize findings to explain how $cleanTopic behaves or functions in the local environment.
            """.trimIndent()

            experiencesMarkdown = """
## II. Learning Experiences (Nilalaman)

### Localized Instructional Resources
- **Localized Physical Artifacts**: Safe local specimens, leaves, stones, containers, and household objects of Masbate to illustrate concepts of $cleanTopic.
- **Support Material Tools**: Observation sheets, activity logs, drawing instruments, and simple measuring guides.
- **Educational References**: DepEd Division Learning Materials, Regional Budget of Work guides, and localized curriculum notes.

### Instructional Procedure & Weekly Progression Flow (Daily Sessions)

#### Session 1: Exploration & Cognitive Anchoring (45 Mins duration)
- **Elicit (5 Mins)**: The teacher asks students to look around the schoolyard and recall any natural objects representing $cleanTopic.
- **Engage (10 Mins)**: Show a simple specimen of localized matter and ask students to write down their first observations.
- **Explore (15 Mins)**: Students observe samples safely in small teams, documenting visible characteristics under active $eieLevel guidelines.
- **Explain (10 Mins)**: Clarify vocabulary terms and establish a cohesive description of these characteristics.
- **Elaborate (5 Mins)**: Relate observations to objects found in students' immediate homes.

#### Session 2: Practical Scientific Modeling (45 Mins duration)
- **Elicit (5 Mins)**: Review the observation lists compiled during Session 1.
- **Engage (5 Mins)**: The teacher models a simple demonstration using localized objects of different shapes and configurations.
- **Explore (20 Mins)**: Students construct simple representations or physical groups to demonstrate the properties of $cleanTopic.
- **Explain (10 Mins)**: Representatives summarize their models and what they represent.
- **Elaborate (5 Mins)**: Discuss how these models help us understand natural behaviors around us.

#### Session 3: Data Measurement & Comparative Logs (45 Mins duration)
- **Elicit (5 Mins)**: Review the physical models created on the previous day.
- **Engage (5 Mins)**: Present a puzzle challenge showing different scales or configurations.
- **Explore (20 Mins)**: Students work on customized observation sheets to measure, weigh, or log variations of $cleanTopic items.
- **Explain (10 Mins)**: Formal explanation of scientific measurement variables and logging techniques.
- **Elaborate (5 Mins)**: Relate measurements to regional farming, weather, or transportation practices.

#### Session 4: Scientific Synthesis & Reflection (45 Mins duration)
- **Elicit (5 Mins)**: Recap the logs completed during Session 3.
- **Engage (5 Mins)**: Challenge students to draw a simple concept map on paper showing what they learned.
- **Explore (15 Mins)**: Students work on a final review diagram illustrating how the concepts of $cleanTopic fit together.
- **Explain (10 Mins)**: Guided peer reviews inspect drawings against clear scientific criteria.
- **Elaborate (10 Mins)**: Conduct a collective physical relaxation stretch (Hinga protocol) to conclude the week's learning.
            """.trimIndent()

            assessmentMarkdown = when (gradeLevel) {
                "KS1" -> """
## III. Assessment (Pagtataya)

### Integrated Daily Formative Metrics
- **Session 1 Assessment**: Quick-response exit tickets assessing observational accuracy.
- **Session 2 Assessment**: Systematic observation checklists of student modeling tasks.
- **Session 3 Assessment**: Collection of completed data measurement sheets.
- **Session 4 Assessment**: Quality inspection of drawn concept diagrams or peer reviews.

### Evaluation Policy & Scale Compliance
- **For Grade Levels under Key Stage 1 (KS1)**: Assessment centers entirely on PACE qualitative indicators to identify growth over time. Performance tiers are recorded as **Matingkad** (showing exceptional self-directed mastery), **Sapat** (showing sufficient target alignment), or **Nagsisimula** (currently developing core indicators with guided scaffolding). Raw numerical grade scales are omitted.

### AI Ethics & Academic Integrity Policy
- **Category 1 (Strict Prohibition)**: Students are strictly banned from utilising generative AI tools for writing assignments or quizzes.
- **Category 2 (Limited Educator Assistance)**: AI is restricted to aiding professional instructional designers and classroom teachers in lesson structure formulation.
                """.trimIndent()
                else -> """
## III. Assessment (Pagtataya)

### Integrated Daily Formative Metrics
- **Session 1 Assessment**: Quick-response exit tickets assessing observational accuracy.
- **Session 2 Assessment**: Systematic observation checklists of student modeling tasks.
- **Session 3 Assessment**: Collection of completed data measurement sheets.
- **Session 4 Assessment**: Quality inspection of drawn concept diagrams or peer reviews.

### Evaluation Policy & Scale Compliance
- **For Grade Levels under Key Stage 2 to 4 (KS2-KS4)**: Learner achievement uses standard DepEd composite grading weights comprising 20% Written Works quizzes, 50% Active Performance Tasks, and 30% End-of-Term Summative Assessments. Calculations strictly apply the 75% transitional grading floor policy where an earned raw score of 70% automatically scales to 75% in final quarterly logs to prevent premature academic discouragement.

### AI Ethics & Academic Integrity Policy
- **Category 1 (Strict Prohibition)**: Students are strictly banned from utilising generative AI tools for writing assignments or quizzes.
- **Category 2 (Limited Educator Assistance)**: AI is restricted to aiding professional instructional designers and classroom teachers in lesson structure formulation.
                """.trimIndent()
            }

            waysForwardMarkdown = """
## IV. Ways Forward (Pagninilay)

### Localized Homework Assignments (Takdang-Aralin)
- **Session 1 Homework**: Identify and write down 3 examples of the concept of $cleanTopic found in your home kitchen.
- **Session 2 Homework**: Discuss with parents or siblings how $cleanTopic relates to daily chores like cleaning or gardening.
- **Session 3 Homework**: Complete the daily localized diagram illustrating the measurement exercises.
- **Session 4 Homework**: Write a 3-sentence personal journal entry summarizing your favorite scientific activity of the week.

### Adaptive Remediation & Enrichment Plans
- **Comprehensive Remediation Strategy**: Students scoring below the 75% transitional grade floor receive immediate contextualized support, utilizing self-paced physical worksheets with larger illustrations and cooperative student-buddy reading exercises.
- **Active Enrichment Strategy**: Rapid learners who achieve prompt mastery will engage in drawing multi-perspective structural diagrams or act as peer mentors for small study circles.
            """.trimIndent()

            // Generate 5 Science questions
            val q1 = JSONObject()
            q1.put("question", "Which of the following is the most objective way for a scientist to observe the characteristics of $cleanTopic?")
            q1.put("type", "Multiple Choice")
            q1.put("correct_answer", "A")
            val opts1 = JSONArray()
            opts1.put("A) By measuring and recording physical properties using standard instruments.")
            opts1.put("B) By guessing the properties without checking the specimen.")
            opts1.put("C) By relying on opinions from social media posts.")
            opts1.put("D) By changing the data values to match a pre-written theory.")
            q1.put("options", opts1)
            qArray.put(q1)

            val q2 = JSONObject()
            q2.put("question", "Why do science classes construct physical models when investigating complex concepts like $cleanTopic?")
            q2.put("type", "Multiple Choice")
            q2.put("correct_answer", "B")
            val opts2 = JSONArray()
            opts2.put("A) To make sure the class finishes early without writing logs.")
            opts2.put("B) To visualize and safely investigate interactions that are difficult to see directly.")
            opts2.put("C) To completely replace the need for any scientific observation.")
            opts2.put("D) To show that physical materials do not obey natural laws.")
            q2.put("options", opts2)
            qArray.put(q2)

            val q3 = JSONObject()
            q3.put("question", "What safety rule must always be observed when exploring localized materials in the classroom?")
            q3.put("type", "Multiple Choice")
            q3.put("correct_answer", "C")
            val opts3 = JSONArray()
            opts3.put("A) Taste all unknown liquid specimens immediately.")
            opts3.put("B) Handle materials roughly without reading the activity steps.")
            opts3.put("C) Follow instructions carefully, handle objects gently, and wash hands afterward.")
            opts3.put("D) Mix unrelated chemicals or items together without the teacher's approval.")
            q3.put("options", opts3)
            qArray.put(q3)

            val q4 = JSONObject()
            q4.put("question", "When collecting data logs during a scientific experiment, how should variations in properties be recorded?")
            q4.put("type", "Multiple Choice")
            q4.put("correct_answer", "D")
            val opts4 = JSONArray()
            opts4.put("A) By ignoring any results that do not match what the student expected.")
            opts4.put("B) By waiting until the next day and guessing the measurements.")
            opts4.put("C) By writing down only the results that are pleasant to look at.")
            opts4.put("D) By documenting all actual measurements and observations honestly in the log.")
            q4.put("options", opts4)
            qArray.put(q4)

            val q5 = JSONObject()
            q5.put("question", "Which statement is true regarding the role of content standards in our daily science logs?")
            q5.put("type", "Multiple Choice")
            q5.put("correct_answer", "A")
            val opts5 = JSONArray()
            opts5.put("A) Standards define the core knowledge and skills that every lesson must build toward.")
            opts5.put("B) Standards are completely unrelated to the physical objects used in activities.")
            opts5.put("C) Standards require students to copy sentences verbatim without understanding.")
            opts5.put("D) Standards change randomly every day depending on the weather.")
            q5.put("options", opts5)
            qArray.put(q5)

        } else if (isMath) {
            // Mathematics Offline Backup
            intentionsMarkdown = """
## I. Intentions (Layunin)

### Curriculum Integration & Framework
The intentional core of this instructional week centers on establishing a profound cognitive and skill-based connection with standard competencies. Under the official DepEd guidelines, this standard outlines that learners will actively calculate, solve, and analyze mathematical structures of $cleanTopic.

- **Teacher Profile**: $teacherName | Designation: $teacherDesignation
- **School Location**: $schoolName under the $schoolDistrict, $divisionOffice
- **Quarter & Term Reference**: Term Block $term, Week $week
- **Grade & Target Scope**: Grade $specificGradeLevel ($gradeLevel Key Stage)
- **Pedagogical Alignment**: Designed using the $teachingStrategy method to accommodate active $eieLevel safety guidelines.

- **Content Standard Integration**: $cleanCS.
- **Performance Standard Target**: $cleanPS.

### Specific Session Objectives:
1. **Session 1 (Conceptual Definition)**: Define and identify the key numeric values and shapes representing $cleanTopic in standard math problems.
2. **Session 2 (Inquiry & Visual Modeling)**: Create visual models or diagrams representing mathematical relationships under safety guidelines.
3. **Session 3 (Algorithmic Calculations)**: Compute, calculate, and solve standard equations representing $cleanTopic steps.
4. **Session 4 (Synthesis & Practical Application)**: Construct mathematical solutions to real-world word problems related to localized contexts of $cleanTopic.
            """.trimIndent()

            experiencesMarkdown = """
## II. Learning Experiences (Nilalaman)

### Localized Instructional Resources
- **Localized Physical Counter Artifacts**: Smooth pebbles, seeds, local Masbate shells, grids, and ruler rulers.
- **Support Material Tools**: Multi-step word problem sheets, graph sheets, calculations journals, and pencils.
- **Educational References**: DepEd Mathematics Budget of Work, regional primary textbooks, and math standard manuals.

### Instructional Procedure & Weekly Progression Flow (Daily Sessions)

#### Session 1: Exploration & Numerical Orientation (45 Mins duration)
- **Elicit (5 Mins)**: Start by asking students to count specific features around the classroom.
- **Engage (10 Mins)**: Present a visual math puzzle representing $cleanTopic in a simplified format.
- **Explore (15 Mins)**: Students analyze the puzzle in safe pairs, organizing counters to represent mathematical values under $eieLevel.
- **Explain (10 Mins)**: Formally explain the math concept, the symbols used, and basic counting laws.
- **Elaborate (5 Mins)**: Apply the numbering rule to actual objects in the room.

#### Session 2: Guided Visual Modeling (45 Mins duration)
- **Elicit (5 Mins)**: Quick review of the counting numbers tinalakay in Session 1.
- **Engage (5 Mins)**: Show a grid or geometric chart representing mathematical proportions.
- **Explore (20 Mins)**: Students construct drawings or arrange local counters to model the fractional or proportional values of $cleanTopic.
- **Explain (10 Mins)**: Explain how visual models translate into algebraic equations or numeric forms.
- **Elaborate (5 Mins)**: Connect models to local farming distributions or household portion sizes.

#### Session 3: Controlled Calculations & Practice (45 Mins duration)
- **Elicit (5 Mins)**: Review the drawings and visual models constructed in Session 2.
- **Engage (5 Mins)**: Present a math puzzle on the blackboard with a missing variable.
- **Explore (20 Mins)**: Students practice calculations in their journals, solving sample equations of $cleanTopic with guided feedback.
- **Explain (10 Mins)**: Detail the exact order of operations and algorithmic rules.
- **Elaborate (5 Mins)**: Relay equations to simple measuring tasks done in regional markets.

#### Session 4: Mathematical Synthesis & Problem Solving (45 Mins duration)
- **Elicit (5 Mins)**: Re-cap standard order of operations discussed in Session 3.
- **Engage (5 Mins)**: Challenge the class to solve a real-life word problem on the board.
- **Explore (15 Mins)**: Students create their own multi-step mathematical problems representing $cleanTopic scenarios.
- **Explain (10 Mins)**: Selected students present their solutions, explaining each logical step clearly.
- **Elaborate (10 Mins)**: Conduct a relaxing breathing stretch (Hinga protocol) before completing final self-paced assessments.
            """.trimIndent()

            assessmentMarkdown = when (gradeLevel) {
                "KS1" -> """
## III. Assessment (Pagtataya)

### Integrated Daily Formative Metrics
- **Session 1 Assessment**: Checklists of simple counting or grouping accuracy.
- **Session 2 Assessment**: Quality of drawn math models or fraction charts.
- **Session 3 Assessment**: Accuracy of completed calculation practice sheets.
- **Session 4 Assessment**: Performance during math problem-solving challenges.

### Evaluation Policy & Scale Compliance
- **For Grade Levels under Key Stage 1 (KS1)**: Assessment centers entirely on PACE qualitative indicators to identify growth over time. Performance tiers are recorded as **Matingkad** (showing exceptional self-directed mastery), **Sapat** (showing sufficient target alignment), or **Nagsisimula** (currently developing core indicators with guided scaffolding). Raw numerical grade scales are omitted.

### AI Ethics & Academic Integrity Policy
- **Category 1 (Strict Prohibition)**: Students are strictly banned from utilising generative AI tools for writing assignments or quizzes.
- **Category 2 (Limited Educator Assistance)**: AI is restricted to aiding professional instructional designers and classroom teachers in lesson structure formulation.
                """.trimIndent()
                else -> """
## III. Assessment (Pagtataya)

### Integrated Daily Formative Metrics
- **Session 1 Assessment**: Checklists of simple counting or grouping accuracy.
- **Session 2 Assessment**: Quality of drawn math models or fraction charts.
- **Session 3 Assessment**: Accuracy of completed calculation practice sheets.
- **Session 4 Assessment**: Performance during math problem-solving challenges.

### Evaluation Policy & Scale Compliance
- **For Grade Levels under Key Stage 2 to 4 (KS2-KS4)**: Learner achievement uses standard DepEd composite grading weights comprising 20% Written Works quizzes, 50% Active Performance Tasks, and 30% End-of-Term Summative Assessments. Calculations strictly apply the 75% transitional grading floor policy where an earned raw score of 70% automatically scales to 75% in final quarterly logs to prevent premature academic discouragement.

### AI Ethics & Academic Integrity Policy
- **Category 1 (Strict Prohibition)**: Students are strictly banned from utilising generative AI tools for writing assignments or quizzes.
- **Category 2 (Limited Educator Assistance)**: AI is restricted to aiding professional instructional designers and classroom teachers in lesson structure formulation.
                """.trimIndent()
            }

            waysForwardMarkdown = """
## IV. Ways Forward (Pagninilay)

### Localized Homework Assignments (Takdang-Aralin)
- **Session 1 Homework**: Count 5 household items and organize them into mathematical sets of values.
- **Session 2 Homework**: Draw a grid map showing the proportion of different crops or spaces in your garden.
- **Session 3 Homework**: Complete 5 practice calculation exercises in your math notebook.
- **Session 4 Homework**: Write a short 3-sentence word problem based on buying items in a local bakery or store.

### Adaptive Remediation & Enrichment Plans
- **Comprehensive Remediation Strategy**: Students scoring below the 75% transactional grade floor receive immediate contextualized support, utilizing self-paced physical worksheets with larger illustrations and cooperative student-buddy reading exercises.
- **Active Enrichment Strategy**: Rapid learners who achieve prompt mastery will engage in drawing multi-perspective structural diagrams or act as peer mentors for small study circles.
            """.trimIndent()

            // Generate 5 Math questions
            val q1 = JSONObject()
            q1.put("question", "In solving a complex word problem regarding $cleanTopic, what is the most logical first step?")
            q1.put("type", "Multiple Choice")
            q1.put("correct_answer", "A")
            val opts1 = JSONArray()
            opts1.put("A) Read the problem carefully and identify the given quantities and what is being asked.")
            opts1.put("B) Write down a random number and hope it is correct.")
            opts1.put("C) Add all numbers together immediately without checking their units.")
            opts1.put("D) Copy the problem into a separate notebook without writing any calculations.")
            q1.put("options", opts1)
            qArray.put(q1)

            val q2 = JSONObject()
            q2.put("question", "How do visual diagrams and fraction charts help us solve mathematical calculations of $cleanTopic?")
            q2.put("type", "Multiple Choice")
            q2.put("correct_answer", "B")
            val opts2 = JSONArray()
            opts2.put("A) They completely replace the need for writing down numeric equations.")
            opts2.put("B) They help us visualize proportions and spatial relations clearly before computing.")
            opts2.put("C) They prove that mathematical formulas do not follow logical patterns.")
            opts2.put("D) They allow students to skip peer reviews and teachers' assessments.")
            q2.put("options", opts2)
            qArray.put(q2)

            val q3 = JSONObject()
            q3.put("question", "When partitioning a collection of localized counters into equal sets, which mathematical operation is being modeled?")
            q3.put("type", "Multiple Choice")
            q3.put("correct_answer", "C")
            val opts3 = JSONArray()
            opts3.put("A) Subtraction")
            opts3.put("B) Addition")
            opts3.put("C) Division")
            opts3.put("D) Square roots")
            q3.put("options", opts3)
            qArray.put(q3)

            val q4 = JSONObject()
            q4.put("question", "Which equation correctly models the sum of two fractional parts representing equivalent portions of localized agricultural spaces?")
            q4.put("type", "Multiple Choice")
            q4.put("correct_answer", "D")
            val opts4 = JSONArray()
            opts4.put("A) 1/2 + 1/2 = 1/4")
            opts4.put("B) 1/3 + 1/3 = 2/6")
            opts4.put("C) 1/4 + 1/4 = 1/8")
            opts4.put("D) 1/4 + 1/4 = 2/4 (or 1/2)")
            q4.put("options", opts4)
            qArray.put(q4)

            val q5 = JSONObject()
            q5.put("question", "Under the official DepEd guidelines, how are student raw grades processed if they achieve a 70% average?")
            q5.put("type", "Multiple Choice")
            q5.put("correct_answer", "A")
            val opts5 = JSONArray()
            opts5.put("A) The raw 70% is scaled to a 75% transitional grading floor in quarterly records.")
            opts5.put("B) The score remains 70% without any transitional adjustment.")
            opts5.put("C) The score is automatically reduced to a failing grade of 60%.")
            opts5.put("D) The grade is completely deleted from the school database.")
            q5.put("options", opts5)
            qArray.put(q5)

        } else if (isEnglishOrReading) {
            // English or Reading Offline Backup
            intentionsMarkdown = """
## I. Intentions (Layunin)

### Curriculum Integration & Framework
The intentional core of this instructional week centers on establishing a profound cognitive and skill-based connection with standard competencies. Under the official DepEd guidelines, this standard outlines that learners will actively read, comprehend, and analyze passages about $cleanTopic.

- **Teacher Profile**: $teacherName | Designation: $teacherDesignation
- **School Location**: $schoolName under the $schoolDistrict, $divisionOffice
- **Quarter & Term Reference**: Term Block $term, Week $week
- **Grade & Target Scope**: Grade $specificGradeLevel ($gradeLevel Key Stage)
- **Pedagogical Alignment**: Designed using the $teachingStrategy method to accommodate active $eieLevel safety guidelines.

- **Content Standard Integration**: $cleanCS.
- **Performance Standard Target**: $cleanPS.

### Specific Session Objectives:
1. **Session 1 (Vocabulary Building)**: Unlock and define unfamiliar words in the reading selection of $cleanTopic using context clues.
2. **Session 2 (Reading Comprehension)**: Read and analyze the main idea of a localized passage, identifying the narrative flow.
3. **Session 3 (Critical Text Analysis)**: Compare and contrast different viewpoints or characters in the narrative.
4. **Session 4 (Synthesis & Summarization)**: Write a coherent, well-structured 3-sentence summary of the weekly passage.
            """.trimIndent()

            experiencesMarkdown = """
## II. Learning Experiences (Nilalaman)

### Localized Instructional Resources
- **Localized Reading Materials**: Printed short passages, sentence strips, vocabulary cards, and story charts containing cultural themes.
- **Support Material Tools**: Reading logs, character maps, exit tickets, and student markers.
- **Educational References**: DepEd English/Language Curriculum Guides, localized Masbate short stories, and vocabulary lists.

### Instructional Procedure & Weekly Progression Flow (Daily Sessions)

#### Session 1: Vocabulary Unlocking & Context Clues (45 Mins duration)
- **Elicit (5 Mins)**: Show a single localized word on the board and ask students to guess its meaning.
- **Engage (10 Mins)**: Read aloud a short teaser sentence containing the weekly vocabulary.
- **Explore (15 Mins)**: Students analyze context clues in safe pairs under active $eieLevel guidelines, logging definitions in their journals.
- **Explain (10 Mins)**: Define grammatical rules and parts of speech of these vocabulary words.
- **Elaborate (5 Mins)**: Construct simple original sentences using these words.

#### Session 2: Guided Silent Reading & Analysis (45 Mins duration)
- **Elicit (5 Mins)**: Quick review of the vocabulary words unlocked in Session 1.
- **Engage (5 Mins)**: Show an illustration representing the central character or theme of the weekly reading.
- **Explore (20 Mins)**: Students read the designated passage silently (or with reading buddies under safe $eieLevel steps) and map out the character traits.
- **Explain (10 Mins)**: Discuss the main idea and key supporting details of the text.
- **Elaborate (5 Mins)**: Relate the character's values to students' daily family responsibilities.

#### Session 3: Comparative Text Analysis (45 Mins duration)
- **Elicit (5 Mins)**: Quick recap of the characters tinalakay in Session 2.
- **Engage (5 Mins)**: Pose a predictive question regarding the character's next decision.
- **Explore (20 Mins)**: Students complete a compare-and-contrast matrix, analyzing different actions or settings.
- **Explain (10 Mins)**: Discuss the difference between literal and figurative statements in the passage.
- **Elaborate (5 Mins)**: Discuss how the reading reflects localized regional traditions.

#### Session 4: Collaborative Synthesis & Summarization (45 Mins duration)
- **Elicit (5 Mins)**: Re-visit the character maps completed in prior sessions.
- **Engage (5 Mins)**: Challenge the class to write a single headline summarizing the story.
- **Explore (15 Mins)**: Students write individual, well-structured summaries of the reading selection.
- **Explain (10 Mins)**: Review peer summaries against standard paragraph rubrics.
- **Elaborate (10 Mins)**: Conclude the week with a reflective reading-circle sharing segment before final assessments.
            """.trimIndent()

            assessmentMarkdown = when (gradeLevel) {
                "KS1" -> """
## III. Assessment (Pagtataya)

### Integrated Daily Formative Metrics
- **Session 1 Assessment**: Sentence construction checks utilizing vocabulary words.
- **Session 2 Assessment**: Systematic checklists of reading comprehension sheets.
- **Session 3 Assessment**: Accuracy of completed character comparison matrices.
- **Session 4 Assessment**: Review of the 3-sentence written summaries.

### Evaluation Policy & Scale Compliance
- **For Grade Levels under Key Stage 1 (KS1)**: Assessment centers entirely on PACE qualitative indicators to identify growth over time. Performance tiers are recorded as **Matingkad** (showing exceptional self-directed mastery), **Sapat** (showing sufficient target alignment), or **Nagsisimula** (currently developing core indicators with guided scaffolding). Raw numerical grade scales are omitted.

### AI Ethics & Academic Integrity Policy
- **Category 1 (Strict Prohibition)**: Students are strictly banned from utilising generative AI tools for writing assignments or quizzes.
- **Category 2 (Limited Educator Assistance)**: AI is restricted to aiding professional instructional designers and classroom teachers in lesson structure formulation.
                """.trimIndent()
                else -> """
## III. Assessment (Pagtataya)

### Integrated Daily Formative Metrics
- **Session 1 Assessment**: Sentence construction checks utilizing vocabulary words.
- **Session 2 Assessment**: Systematic checklists of reading comprehension sheets.
- **Session 3 Assessment**: Accuracy of completed character comparison matrices.
- **Session 4 Assessment**: Review of the 3-sentence written summaries.

### Evaluation Policy & Scale Compliance
- **For Grade Levels under Key Stage 2 to 4 (KS2-KS4)**: Learner achievement uses standard DepEd composite grading weights comprising 20% Written Works quizzes, 50% Active Performance Tasks, and 30% End-of-Term Summative Assessments. Calculations strictly apply the 75% transitional grading floor policy where an earned raw score of 70% automatically scales to 75% in final quarterly logs to prevent premature academic discouragement.

### AI Ethics & Academic Integrity Policy
- **Category 1 (Strict Prohibition)**: Students are strictly banned from utilising generative AI tools for writing assignments or quizzes.
- **Category 2 (Limited Educator Assistance)**: AI is restricted to aiding professional instructional designers and classroom teachers in lesson structure formulation.
                """.trimIndent()
            }

            waysForwardMarkdown = """
## IV. Ways Forward (Pagninilay)

### Localized Homework Assignments (Takdang-Aralin)
- **Session 1 Homework**: Find 2 unfamiliar words in a home magazine or newspaper and ask your family what they mean.
- **Session 2 Homework**: Read the weekly story aloud to your parents or siblings.
- **Session 3 Homework**: Write down 2 sentences describing your favorite character.
- **Session 4 Homework**: Draw a simple scene representing the main conflict of the weekly story.

### Adaptive Remediation & Enrichment Plans
- **Comprehensive Remediation Strategy**: Students scoring below the 75% transitional grade floor receive immediate contextualized support, utilizing self-paced physical worksheets with larger illustrations and cooperative student-buddy reading exercises.
- **Active Enrichment Strategy**: Rapid learners who achieve prompt mastery will engage in drawing multi-perspective structural diagrams or act as peer mentors for small study circles.
            """.trimIndent()

            // Generate 5 Reading questions
            val q1 = JSONObject()
            q1.put("question", "What is the most effective way to identify the main idea of a reading selection about $cleanTopic?")
            q1.put("type", "Multiple Choice")
            q1.put("correct_answer", "A")
            val opts1 = JSONArray()
            opts1.put("A) By looking for the recurring theme and the supporting details in the opening and closing sentences.")
            opts1.put("B) By counting the total number of letters or punctuation marks in the passage.")
            opts1.put("C) By skipping the text and choosing the longest options in the quiz sheet.")
            opts1.put("D) By changing the words of the text to represent an entirely different story.")
            q1.put("options", opts1)
            qArray.put(q1)

            val q2 = JSONObject()
            q2.put("question", "How can context clues assist a reader in unlocking unfamiliar localized words?")
            q2.put("type", "Multiple Choice")
            q2.put("correct_answer", "B")
            val opts2 = JSONArray()
            opts2.put("A) By instructing the reader to memorize the entire dictionary before reading.")
            opts2.put("B) By providing hints within the surrounding sentences that clarify the word's meaning.")
            opts2.put("C) By proving that localized words do not have logical definitions.")
            opts2.put("D) By forcing the student to write down unrelated chemistry terminology.")
            q2.put("options", opts2)
            qArray.put(q2)

            val q3 = JSONObject()
            q3.put("question", "Which of the following describes a proper, well-structured 3-sentence summary?")
            q3.put("type", "Multiple Choice")
            q3.put("correct_answer", "C")
            val opts3 = JSONArray()
            opts3.put("A) A paragraph consisting of 50 unconnected words copied from random pages.")
            opts3.put("B) A blank writing sheet with empty bullet points.")
            opts3.put("C) A brief passage stating the main character, the central action, and the final resolution.")
            opts3.put("D) A long list of unrelated science lab steps and measurements.")
            q3.put("options", opts3)
            qArray.put(q3)

            val q4 = JSONObject()
            q4.put("question", "In reading a traditional localized narrative, what is the purpose of the central conflict?")
            q4.put("type", "Multiple Choice")
            q4.put("correct_answer", "D")
            val opts4 = JSONArray()
            opts4.put("A) To make sure that students do not understand the story's outcome.")
            opts4.put("B) To replace standard reading practices with heavy mathematical equations.")
            opts4.put("C) To prove that characters do not experience challenges in their settings.")
            opts4.put("D) To drive the plot and highlight the core lessons or values of the story.")
            q4.put("options", opts4)
            qArray.put(q4)

            val q5 = JSONObject()
            q5.put("question", "Which part of speech is utilized to describe or modify a localized noun representing the topic?")
            q5.put("type", "Multiple Choice")
            q5.put("correct_answer", "A")
            val opts5 = JSONArray()
            opts5.put("A) Adjective")
            opts5.put("B) Conjunction")
            opts5.put("C) Preposition")
            opts5.put("D) Verb")
            q5.put("options", opts5)
            qArray.put(q5)

        } else {
            // General Fallback
            intentionsMarkdown = """
## I. Intentions (Layunin)

### Curriculum Integration & Framework
The intentional core of this instructional week centers on establishing a profound cognitive and skill-based connection with standard competencies. Under the official DepEd guidelines, this standard outlines that learners will actively comprehend, practice, and apply the core components of $cleanTopic.

- **Teacher Profile**: $teacherName | Designation: $teacherDesignation
- **School Location**: $schoolName under the $schoolDistrict, $divisionOffice
- **Quarter & Term Reference**: Term Block $term, Week $week
- **Grade & Target Scope**: Grade $specificGradeLevel ($gradeLevel Key Stage)
- **Pedagogical Alignment**: Designed using the $teachingStrategy method to accommodate active $eieLevel safety guidelines.

- **Content Standard Integration**: $cleanCS.
- **Performance Standard Target**: $cleanPS.

### Specific Session Objectives:
1. **Session 1 (Conceptual Orientation)**: Define and identify the fundamental elements of $cleanTopic, clarifying baseline terminology and local occurrences in everyday settings.
2. **Session 2 (Inquiry & Practical Modeling)**: Utilize localized tools or visual representations to examine and organize representations of $cleanTopic.
3. **Session 3 (Interactive Practice)**: Perform structured exercises or comparative logs, documenting observations under safe guidelines.
4. **Session 4 (Synthesis & Reflection)**: Coordinate active findings to present explanations of $cleanTopic, proposing cooperative real-world applications.
            """.trimIndent()

            experiencesMarkdown = """
## II. Learning Experiences (Nilalaman)

### Localized Instructional Resources
- **Localized Support Artifacts**: Physical guides, local references, maps, activity booklets, and drawing instruments.
- **Support Material Tools**: Discussion worksheets, observation sheets, peer-coaching checklists, and markers.
- **Educational References**: DepEd Division Curriculum guides, regional teacher logs, and localized study guides.

### Instructional Procedure & Weekly Progression Flow (Daily Sessions)

#### Session 1: Exploration & Cognitive Anchoring (45 Mins duration)
- **Elicit (5 Mins)**: The teacher begins by flashing reference cards or asking students to recount objects around their neighborhood that represent $cleanTopic.
- **Engage (10 Mins)**: Introduce a direct challenge or interesting scenario where students share their initial thoughts.
- **Explore (15 Mins)**: Students write down early findings on charts, following $eieLevel guidelines for safe, spaced interactions.
- **Explain (10 Mins)**: Clarify common vocabulary terms and establish a cohesive conceptual framework.
- **Elaborate (5 Mins)**: Connect findings in quick dialogue segments.

#### Session 2: Guided Structural Investigation (45 Mins duration)
- **Elicit (5 Mins)**: Review findings from Session 1 on the blackboard.
- **Engage (5 Mins)**: The teacher models a simple demonstration using localized diagrams or physical models.
- **Explore (20 Mins)**: Students group up (or study independently under restrictive Hinga/Hinto steps) to analyze the core parts.
- **Explain (10 Mins)**: Representatives from each session group summarize how they organized elements.
- **Elaborate (5 Mins)**: Relay observations to simple daily tasks at home.

#### Session 3: Practical Application & Data Logging (45 Mins duration)
- **Elicit (5 Mins)**: Quick review of the group models compiled on the prior day.
- **Engage (5 Mins)**: Present an active puzzle challenge where two different items appear equivalent.
- **Explore (20 Mins)**: Students work on customized activity sheets to classify or sequence $cleanTopic items.
- **Explain (10 Mins)**: Formal explanation of procedural steps and classification rules.
- **Elaborate (5 Mins)**: Relate exact findings to regional transport, farming, or cultural structures.

#### Session 4: Collaborative Synthesis & Core Peer Reflection (45 Mins duration)
- **Elicit (5 Mins)**: Recap of the data log trends completed during Session 3.
- **Engage (5 Mins)**: Challenge the class to draw simple visual models on drawing sheets.
- **Explore (15 Mins)**: Students create individual conceptual diagrams representing the concepts of $cleanTopic.
- **Explain (10 Mins)**: Guided peer reviews inspect drawings against clear criteria.
- **Elaborate (10 Mins)**: Conduct a collective physical relaxation stretch (Hinga protocol) to conclude the week's learning.
            """.trimIndent()

            assessmentMarkdown = when (gradeLevel) {
                "KS1" -> """
## III. Assessment (Pagtataya)

### Integrated Daily Formative Metrics
- **Session 1 Assessment**: Quick-response exit tickets assessing conceptual awareness.
- **Session 2 Assessment**: Systematic observation checklists of student exploration.
- **Session 3 Assessment**: Review and collection of completed activity sheets.
- **Session 4 Assessment**: Quality inspection of drawn conceptual diagrams or peer evaluations.

### Evaluation Policy & Scale Compliance
- **For Grade Levels under Key Stage 1 (KS1)**: Assessment centers entirely on PACE qualitative indicators to identify growth over time. Performance tiers are recorded as **Matingkad** (showing exceptional self-directed mastery), **Sapat** (showing sufficient target alignment), or **Nagsisimula** (currently developing core indicators with guided scaffolding). Raw numerical grade scales are omitted.

### AI Ethics & Academic Integrity Policy
- **Category 1 (Strict Prohibition)**: Students are strictly banned from utilising generative AI tools for writing assignments or quizzes.
- **Category 2 (Limited Educator Assistance)**: AI is restricted to aiding professional instructional designers and classroom teachers in lesson structure formulation.
                """.trimIndent()
                else -> """
## III. Assessment (Pagtataya)

### Integrated Daily Formative Metrics
- **Session 1 Assessment**: Quick-response exit tickets assessing conceptual awareness.
- **Session 2 Assessment**: Systematic observation checklists of student exploration.
- **Session 3 Assessment**: Review and collection of completed activity sheets.
- **Session 4 Assessment**: Quality inspection of drawn conceptual diagrams or peer evaluations.

### Evaluation Policy & Scale Compliance
- **For Grade Levels under Key Stage 2 to 4 (KS2-KS4)**: Learner achievement uses standard DepEd composite grading weights comprising 20% Written Works quizzes, 50% Active Performance Tasks, and 30% End-of-Term Summative Assessments. Calculations strictly apply the 75% transitional grading floor policy where an earned raw score of 70% automatically scales to 75% in final quarterly logs to prevent premature academic discouragement.

### AI Ethics & Academic Integrity Policy
- **Category 1 (Strict Prohibition)**: Students are strictly banned from utilising generative AI tools for writing assignments or quizzes.
- **Category 2 (Limited Educator Assistance)**: AI is restricted to aiding professional instructional designers and classroom teachers in lesson structure formulation.
                """.trimIndent()
            }

            waysForwardMarkdown = """
## IV. Ways Forward (Pagninilay)

### Localized Homework Assignments (Takdang-Aralin)
- **Session 1 Homework**: Identify and list 3 examples of the concept of $cleanTopic found in your home setting.
- **Session 2 Homework**: Discuss with parents or siblings how $cleanTopic relates to daily family chores.
- **Session 3 Homework**: Complete the daily localized diagram illustrating the classification exercises.
- **Session 4 Homework**: Write a 3-sentence personal journal entry summarizing your favorite activity.

### Adaptive Remediation & Enrichment Plans
- **Comprehensive Remediation Strategy**: Students scoring below the 75% transitional grade floor receive immediate contextualized support, utilizing self-paced physical worksheets with larger illustrations and cooperative student-buddy reading exercises.
- **Active Enrichment Strategy**: Rapid learners who achieve prompt mastery will engage in drawing multi-perspective structural diagrams or act as peer mentors for small study circles.
            """.trimIndent()

            // Generate 5 Fallback questions
            val q1 = JSONObject()
            q1.put("question", "Which of the following is the most objective way to analyze the characteristics of $cleanTopic?")
            q1.put("type", "Multiple Choice")
            q1.put("correct_answer", "A")
            val opts1 = JSONArray()
            opts1.put("A) By examining and documenting core properties using verified materials.")
            opts1.put("B) By guessing the properties without checking any sources.")
            opts1.put("C) By relying on unverified rumors.")
            opts1.put("D) By changing the records to fit an unapproved template.")
            q1.put("options", opts1)
            qArray.put(q1)

            val q2 = JSONObject()
            q2.put("question", "How do visual diagrams help us understand complex concepts like $cleanTopic?")
            q2.put("type", "Multiple Choice")
            q2.put("correct_answer", "B")
            val opts2 = JSONArray()
            opts2.put("A) They completely replace the need for writing down notes.")
            opts2.put("B) They organize core information visually, highlighting underlying connections.")
            opts2.put("C) They prove that educational concepts do not have logical patterns.")
            opts2.put("D) They allow students to skip peer reviews and teachers' assessments.")
            q2.put("options", opts2)
            qArray.put(q2)

            val q3 = JSONObject()
            q3.put("question", "What safety rule must always be observed when exploring localized materials in the classroom?")
            q3.put("type", "Multiple Choice")
            q3.put("correct_answer", "C")
            val opts3 = JSONArray()
            opts3.put("A) Taste all unknown liquid specimens immediately.")
            opts3.put("B) Handle materials roughly without reading the activity steps.")
            opts3.put("C) Follow instructions carefully, handle objects gently, and wash hands afterward.")
            opts3.put("D) Mix unrelated items together without the teacher's approval.")
            q3.put("options", opts3)
            qArray.put(q3)

            val q4 = JSONObject()
            q4.put("question", "When collecting data logs during a class activity, how should variations be recorded?")
            q4.put("type", "Multiple Choice")
            q4.put("correct_answer", "D")
            val opts4 = JSONArray()
            opts4.put("A) By ignoring any results that do not match what the student expected.")
            opts4.put("B) By waiting until the next day and guessing the measurements.")
            opts4.put("C) By writing down only the results that are pleasant to look at.")
            opts4.put("D) By documenting all actual findings and observations honestly in the log.")
            q4.put("options", opts4)
            qArray.put(q4)

            val q5 = JSONObject()
            q5.put("question", "Which statement is true regarding the role of content standards in our daily lesson logs?")
            q5.put("type", "Multiple Choice")
            q5.put("correct_answer", "A")
            val opts5 = JSONArray()
            opts5.put("A) Standards define the core knowledge and skills that every lesson must build toward.")
            opts5.put("B) Standards are completely unrelated to the physical objects used in activities.")
            opts5.put("C) Standards require students to copy sentences verbatim without understanding.")
            opts5.put("D) Standards change randomly every day depending on the weather.")
            q5.put("options", opts5)
            qArray.put(q5)
        }

        root.put("intentions", intentionsMarkdown)
        root.put("learning_experiences", experiencesMarkdown)
        root.put("assessment", assessmentMarkdown)
        root.put("ways_forward", waysForwardMarkdown)
        root.put("formative_questions", qArray)

        return root.toString()
    }
}
