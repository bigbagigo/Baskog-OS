package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class AppRepository(private val appDao: AppDao) {

    val curriculumItems: Flow<List<Curriculum>> = appDao.getAllCurriculum()
    val lessonPlans: Flow<List<LessonPlan>> = appDao.getAllLessonPlans()
    val systemLogs: Flow<List<SystemLog>> = appDao.getAllSystemLogs()

    // Curriculum operations
    suspend fun saveCurriculum(curriculum: Curriculum) {
        appDao.insertCurriculum(curriculum)
    }

    suspend fun removeCurriculum(id: Int) {
        appDao.deleteCurriculumById(id)
    }

    suspend fun clearAllCurriculum() {
        appDao.clearCurriculum()
        logSystemEvent("PARSER", "Cleared all items from Curriculum catalog.")
    }

    // Lesson Plan operations
    suspend fun saveLessonPlan(plan: LessonPlan) {
        appDao.insertLessonPlan(plan)
    }

    suspend fun removeLessonPlan(id: Int) {
        appDao.deleteLessonPlanById(id)
        logSystemEvent("COMPILER", "Deleted lesson plan with ID: $id.")
    }

    // System Logging
    suspend fun logSystemEvent(type: String, message: String) {
        appDao.insertSystemLog(SystemLog(eventType = type, logMessage = message))
    }

    suspend fun clearLogs() {
        appDao.clearSystemLogs()
    }

    /**
     * Parse unstructured or structured copy-pasted text representing MELC/BOW data.
     * Supports Tab-separated, CSV, Pipe-separated lines, or full standard JSON Array format.
     * Ingested unstructured text is run through Gemini classifier first to sort and normalize fields.
     */
    suspend fun parseAndSaveCurriculum(inputText: String): Pair<Int, String> {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) {
            return Pair(0, "Input is empty. Provide some curriculum standards.")
        }

        var importedCount = 0
        var errorFeedback = ""
        var jsonToParse = ""
        var isAiProcessed = false

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            jsonToParse = trimmed
        } else {
            // Attempt to classify and structure via Gemini first!
            logSystemEvent("PARSER", "Unstructured curriculum text detected. Processing with Gemini classification & sorting model...")
            val aiStructured = GeminiClient.structureCurriculumViaGemini(trimmed)
            if (aiStructured.isNotBlank() && aiStructured.startsWith("[") && aiStructured.endsWith("]")) {
                jsonToParse = aiStructured
                isAiProcessed = true
                logSystemEvent("PARSER", "Gemini hypervisor successfully sorted, classified, and structured curriculum entries.")
            }
        }

        // Case A: If we have a JSON string (either supplied directly or processed via Gemini)
        if (jsonToParse.isNotEmpty()) {
            try {
                val array = JSONArray(jsonToParse)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val rawSubject = obj.optString("subject", "General").trim()
                    val cleanSubject = sanitizeSubject(rawSubject)
                    val gradeVal = obj.optString("gradeLevel", "KS1").uppercase(Locale.getDefault()).trim()
                    
                    val curr = Curriculum(
                        gradeLevel = if (gradeVal in listOf("KS1", "KS2", "KS3", "KS4")) gradeVal else "KS1",
                        subject = cleanSubject,
                        term = sanitizeTerm(obj.optString("term", "Instructional")),
                        week = obj.optInt("week", 1),
                        melcCode = obj.optString("melcCode", ""),
                        sessionsBudgeted = obj.optInt("sessionsBudgeted", 5),
                        contentStandard = obj.optString("contentStandard", "General knowledge understanding."),
                        performanceStandard = obj.optString("performanceStandard", "General application of standard."),
                        learningCompetency = obj.optString("learningCompetency", "Core competency expectation.")
                    )
                    appDao.insertCurriculum(curr)
                    importedCount++
                }
                val parserType = if (isAiProcessed) "AI-Sourced & Sorted Ingest" else "KABAN JSON matrix"
                logSystemEvent("PARSER", "Successfully parsed $importedCount curriculum alignment entries via $parserType.")
                return Pair(importedCount, "Successfully imported $importedCount curriculum records.")
            } catch (e: Exception) {
                errorFeedback = "JSON Parse Failure: ${e.message}"
                logSystemEvent("PARSER", "JSON curriculum parsing failed: ${e.message}")
            }
        }

        // Case B: Unstructured CSV/Tab/Pipe separated tabular mapping (Offline Fallback)
        try {
            val lines = trimmed.split("\n")
            for (line in lines) {
                val cleanLine = line.trim()
                if (cleanLine.isEmpty()) continue
                
                // Skip headers if detected
                if (cleanLine.contains("contentStandard", ignoreCase = true) || 
                    cleanLine.contains("Learning Competency", ignoreCase = true) ||
                    cleanLine.contains("melcCode", ignoreCase = true) ||
                    cleanLine.startsWith("GradeLevel", ignoreCase = true)) {
                    continue
                }

                // Detect delimiter
                val delimiter = when {
                    cleanLine.contains("\t") -> "\t"
                    cleanLine.contains("|") -> "|"
                    cleanLine.contains(";") -> ";"
                    else -> ","
                }

                val tokens = cleanLine.split(delimiter).map { it.trim() }
                if (tokens.size >= 5) {
                    val gradeVal = tokens.getOrNull(0)?.uppercase(Locale.getDefault()) ?: ""
                    
                    // Offline Fallback Defenser: If it doesn't start with a valid Key Stage ID,
                    // do not split this line. It's likely a normal narrative sentence with commas.
                    if (gradeVal !in listOf("KS1", "KS2", "KS3", "KS4")) {
                        continue
                    }

                    val subjVal = tokens.getOrNull(1) ?: "Language"
                    val termVal = tokens.getOrNull(2) ?: "Instructional"
                    val weekVal = tokens.getOrNull(3)?.toIntOrNull() ?: 1
                    
                    // Remainder tokens
                    val contentStd = tokens.getOrNull(4) ?: "Not defined"
                    val perfStd = tokens.getOrNull(5) ?: "Not defined"
                    val compVal = tokens.getOrNull(6) ?: "Not defined"
                    val codeVal = tokens.getOrNull(7) ?: ""
                    val sessionsVal = tokens.getOrNull(8)?.toIntOrNull() ?: 5

                    val curr = Curriculum(
                        gradeLevel = gradeVal,
                        subject = sanitizeSubject(subjVal),
                        term = sanitizeTerm(termVal),
                        week = weekVal,
                        contentStandard = contentStd,
                        performanceStandard = perfStd,
                        learningCompetency = compVal,
                        melcCode = codeVal,
                        sessionsBudgeted = sessionsVal
                    )
                    appDao.insertCurriculum(curr)
                    importedCount++
                }
            }

            if (importedCount > 0) {
                logSystemEvent("PARSER", "parsed $importedCount rows from custom tabular copy-paste string.")
                return Pair(importedCount, "Parsed and imported $importedCount curriculum lines.")
            } else {
                if (errorFeedback.isEmpty()) {
                    errorFeedback = "No valid lines parsed. Make sure they have at least 5 columns (Grade, Subject, Term, Week, Content Standard, Performance, Competency)."
                }
            }
        } catch (e: Exception) {
            errorFeedback = e.message ?: "General line-by-line parsing failure."
            logSystemEvent("PARSER", "Tabular parsing error: ${e.message}")
        }

        return Pair(importedCount, errorFeedback)
    }

    /**
     * Sanitizes raw subject names to standardize them in the system.
     * Prevents sentences or random comma tokens from posing as main subjects in the dropdown.
     */
    private fun sanitizeSubject(rawSubject: String): String {
        val lower = rawSubject.lowercase(Locale.getDefault()).trim()
        return when {
            lower.contains("reading") || lower.contains("literacy") -> "Reading Literacy"
            lower.contains("math") || lower.contains("mathematics") -> "Mathematics"
            lower.contains("science") -> "Science"
            lower.contains("english") -> "English"
            lower.contains("filipino") -> "Filipino"
            lower.contains("araling panlipunan") || lower.contains("ap ") || lower == "ap" -> "Araling Panlipunan"
            lower.contains("mapeh") -> "MAPEH"
            lower.contains("epp") || lower.contains("tle") -> "EPP/TLE"
            lower.contains("mother tongue") -> "Reading Literacy"
            lower.isBlank() -> "General"
            
            // If the classified/parsed subject name is too long, we truncate
            rawSubject.length > 40 || rawSubject.split(" ").size > 4 -> {
                val words = rawSubject.split(" ").take(2).joinToString(" ")
                if (words.length > 25) "General" else words
            }
            else -> rawSubject.trim()
        }
    }

    private fun sanitizeTerm(rawTerm: String): String {
        val clean = rawTerm.lowercase(Locale.getDefault()).trim()
        return when {
            clean.contains("1st") || clean.contains("opening") || clean == "1" -> "1st Term"
            clean.contains("2nd") || clean.contains("instructional") || clean == "2" -> "2nd Term"
            clean.contains("3rd") || clean.contains("end") || clean == "3" -> "3rd Term"
            else -> "2nd Term"
        }
    }

    /**
     * Seeds curriculum standard structures based on actual DepEd MELCs and BOW.
     */
    suspend fun seedDatabase() {
        val currentCurr = curriculumItems.first()
        if (currentCurr.isEmpty()) {
            val listToSeed = mutableListOf<Curriculum>()

            // 1. Grade 5 Araling Panlipunan (KS2)
            val apT1 = listOf(
                1 to "• Naipaliliwanag ang pag-aaral ng kasaysayan at mga batayan nito.\n• Natatalakay ang mga pamamaraan at pananaw ng kasaysayan.",
                2 to "• Natutukoy ang pinagmulan ng Pilipinas batay sa agham, kaalamang bayan, at relihiyon.",
                3 to "• Nasusuri ang pinagmulan ng sinaunang tao sa Pilipinas batay sa agham at kaalamang bayan.",
                4 to "• Nasusuri ang kaugnayan ng lokasyon sa paghubog ng kasaysayan.",
                5 to "• Nasusuri ang mga sinaunang bayang Pilipino batay sa organisasyong panlipunan, pang-ekonomiya, at pampolitika.",
                6 to "• Napahahalagahan ang ginampanan ng kababaihan sa pagbuo ng kalinangan ng sinaunang Pilipino.\n• Naipaliliwanag ang konsepto ng kalinangan at uri nito.",
                7 to "• Nasusuri ang mga paniniwala, relihiyon, at tradisyon ng sinaunang bayang Pilipino.",
                8 to "• Natatalakay ang iba't ibang uri ng sinaunang sining, pagpapalamuti at arkitektura ng mga Pilipino.",
                9 to "• Natatalakay ang pagdating at paglaganap ng Islam.\n• Nasusuri ang mga katuruan ng Islam at pamahalaang sultanato.",
                10 to "• Napahahalagahan ang ugnayan ng sinaunang bayang Pilipino sa ilang piling bansa sa Asya."
            )
            apT1.forEach { (wk, comp) ->
                listToSeed.add(
                    Curriculum(
                        gradeLevel = "KS2",
                        subject = "Araling Panlipunan",
                        term = "1st Term",
                        week = wk,
                        melcCode = "AP5-PLP-W$wk",
                        sessionsBudgeted = 4,
                        contentStandard = "Naipamamalas ang pag-unawa at pagpapahalaga sa pinagmulan ng Pilipinas at sa nabuong kalinangan ng sinaunang bayang Pilipino, at ugnayan sa ilang piling bansa sa Asya.",
                        performanceStandard = "Nakalilikha ng presentasyon at nakapagsasagawa ng gawaing nagpapahalaga sa nabuong kalinangang bayan at ugnayan sa ilang piling bansa sa Asya ng mga sinaunang Pilipino.",
                        learningCompetency = comp
                    )
                )
            }

            val apT2 = listOf(
                1 to "• Natutukoy ang kahulugan at mga dahilan ng kolonisasyon.",
                2 to "• Nasusuri ang mga paraan sa pagsasailalim sa lipunang Pilipino.",
                3 to "• Nasusuri ang mga patakarang kolonyal na ipinatupad ng Espanya sa bansa.",
                4 to "• Nasusuri ang mga paraan ng pag-aangkop at pagtugon ng mga Pilipino sa kolonyalismong Espanyol (Paghahambing).",
                5 to "• Nasusuri ang mga paraan ng pag-aangkop at pagtugon ng mga Pilipino sa kolonyalismong Espanyol (Kilusang Rebolusyonaryo).",
                6 to "• Nasusuri ang mga paraan ng pag-aangkop at pagtugon ng mga Pilipino sa kolonyalismong Espanyol.",
                7 to "• Nasusuri ang mga paraan ng pag-aangkop at pagtugon ng mga Pilipino sa kolonyalismong Espanyol (Pagtutol at Pakikipaglaban).",
                8 to "• Natatalakay ang katayuan ng kababaihan sa panahon ng kolonyalismong Espanyol.",
                9 to "• Nasusuri ang mga pagpupunyagi, kinahinatnan, at implikasyon ng pagtatanggol ng mga katutubong pangkat, kababaihan, at iba pang sektor na mapanatili ang kalayaan sa kolonyalismo.",
                10 to "• Napahahalagahan ang magiting na pagtatanggol ng mga Pilipino laban sa pang-aapi ng mga kolonisador."
            )
            apT2.forEach { (wk, comp) ->
                listToSeed.add(
                    Curriculum(
                        gradeLevel = "KS2",
                        subject = "Araling Panlipunan",
                        term = "2nd Term",
                        week = wk,
                        melcCode = "AP5-KOL-W$wk",
                        sessionsBudgeted = 4,
                        contentStandard = "Naipamamalas ang pag-unawa at pagpapahalaga sa pagtugon at pagpupunyagi ng mga Pilipino sa panahon ng kolonyalismong Espanyol.",
                        performanceStandard = "Nakapagsasagawa ng presentasyon ukol sa pagpapahalaga at pagmamalaki sa pagpupunyagi ng mga Pilipino sa pagpapanatili ng kasarinlan.",
                        learningCompetency = comp
                    )
                )
            }

            val apT3 = listOf(
                1 to "• Nasusuri ang konsepto ng nasyonalismo.",
                2 to "• Nasusuri ang pagbabagong dulot ng kaisipang liberal sa pag-usbong ng nasyonalismo.",
                3 to "• Nasusuri ang pagbabagong dulot ng kaisipang liberal sa pag-usbong ng nasyonalismo (Cont.).",
                4 to "• Nasusuri ang ambag ng panggitnang uri sa pag-usbong ng nasyonalismong Pilipino.",
                5 to "• Nasusuri ang ambag ng panggitnang uri sa pag-usbong ng nasyonalismong Pilipino (Kilusang Propaganda).",
                6 to "• Naipaliliwanag ang kilusan para sa sekularisasyon at Pilipinisasyon ng mga parokya.",
                7 to "• Nasusuri ang mga idinulot ng Cavite Mutiny at paggarote sa GOMBURZA sa pag-usbong ng nasyonalismong Pilipino.",
                8 to "• Naiisa-isa ang mga layunin ng pagkakatatag ng Kilusang Propaganda.",
                9 to "• Nasusuri ang papel na ginampanan ng Kilusang Propaganda sa pag-usbong ng nasyonalismo.",
                10 to "• Nasusuri ang papel na ginampanan ng Kilusang Propaganda sa pag-usbong ng nasyonalismo (Ebalwasyon)."
            )
            apT3.forEach { (wk, comp) ->
                listToSeed.add(
                    Curriculum(
                        gradeLevel = "KS2",
                        subject = "Araling Panlipunan",
                        term = "3rd Term",
                        week = wk,
                        melcCode = "AP5-NAS-W$wk",
                        sessionsBudgeted = 4,
                        contentStandard = "Naipamamalas ang pag-unawa at pagpapahalaga sa mga salik na nagbigay-daan sa pag-usbong ng nasyonalismong Pilipino at Kilusang Propaganda.",
                        performanceStandard = "Nakapagsasagawa ng adbokasiya na naglalayong pahalagahan ang pag-usbong ng nasyonalismong Pilipino.",
                        learningCompetency = comp
                    )
                )
            }

            // 2. Grade 5 Science (KS2)
            val sciT1 = listOf(
                1 to "• Describe matter as mass and space. Identify three states: solid, liquid, gas.",
                2 to "• Identify and demonstrate properties of Solids (definite shape/volume), Liquids (no definite shape, definite volume), Gases (none).",
                3 to "• Measuring volume (mL, L) using graduated cylinders and beakers.",
                4 to "• Investigate temperature effects on changes of state in everyday situations.",
                5 to "• Plan and construct simple scientific investigation steps: Problem, Materials, Procedure, Findings.",
                6 to "• Perform measurement of matter using units: mg, g, kg, °C.",
                7 to "• Plan a scientific investigation task to answer questions: 'Do gases have mass?'.",
                8 to "• Plants, Animals & Microorganisms Classification: Plants (flowering/non-flowering), Animals (mammals, reptiles, etc.), Microorganisms (fungi, bacteria).",
                9 to "• Identify specialized plant structures: Rhizomes, tubers, thorns, bulbs, aerial roots.",
                10 to "• Examine plant adaptations to environments and localized ecosystems."
            )
            sciT1.forEach { (wk, comp) ->
                listToSeed.add(
                    Curriculum(
                        gradeLevel = "KS2",
                        subject = "Science",
                        term = "1st Term",
                        week = wk,
                        melcCode = "S5-MT-W$wk",
                        sessionsBudgeted = 4,
                        contentStandard = "Demonstrates understanding of properties of matter, temperature effects, simple scientific investigations, and classification/structures of plants, animals, and microorganisms.",
                        performanceStandard = "Plan and carry out simple scientific investigations about matter and its changes, and organize tables to classify living things.",
                        learningCompetency = comp
                    )
                )
            }

            val sciT2 = listOf(
                1 to "• Body Systems in Animals: Digestive (Mouth, gullet, stomach, intestines).",
                2 to "• Body Systems in Animals: Respiratory (Nose, windpipe, lungs).",
                3 to "• Reproductive Organs and Functions: Male (prostate, testis, penis) & Female (ovaries, uterus, vagina).",
                4 to "• Analyze animal reproduction: Live birth (mammals) vs. Eggs (birds, reptiles).",
                5 to "• Describe ecological adaptations of animals: Mimicry and camouflage.",
                6 to "• Compare life cycles: Mammals, birds, and plants from seed to maturity.",
                7 to "• Conduct investigations on how contact forces cause motion in the direction of the force.",
                8 to "• Investigate friction: Effect of different surfaces (rough vs. smooth) on friction.",
                9 to "• Describe friction producing heat and explore ways to increase or reduce friction.",
                10 to "• Investigate gravity as a non-contact force and predict falling speeds of heavy vs. light objects."
            )
            sciT2.forEach { (wk, comp) ->
                listToSeed.add(
                    Curriculum(
                        gradeLevel = "KS2",
                        subject = "Science",
                        term = "2nd Term",
                        week = wk,
                        melcCode = "S5-LIF-W$wk",
                        sessionsBudgeted = 4,
                        contentStandard = "Demonstrates understanding of animal systems for growth and reproduction, comparing life cycles of living things, and how contact/non-contact forces affect motion.",
                        performanceStandard = "Use diagrams and models to explain animal body systems and compare life cycles. Plan and carry out fair tests to investigate friction/gravity.",
                        learningCompetency = comp
                    )
                )
            }

            val sciT3 = listOf(
                1 to "• Investigate static electricity effects from rubbing materials (comb, balloons).",
                2 to "• Construct simple parallel or series electric circuits with battery, wires, switch, bulb, and identify conductors vs. insulators.",
                3 to "• Examine local landforms (mountains, valleys) and their influence on weather and environment.",
                4 to "• Identify rock features (texture, color, grain) and describe soil formation.",
                5 to "• Perform rock classification using a simple dichotomous key.",
                6 to "• Weathering and Erosion: Demonstrating how water and wind erosion transports Earth materials.",
                7 to "• Describe the role of the water cycle and construct basic models of rain formation.",
                8 to "• Examine weather disturbances in the Philippines and identify PAGASA signals.",
                9 to "• Outline tropical cyclone conditions (before, during, and after) to propose emergency disaster solutions.",
                10 to "• Identify major celestial objects and features in the Solar System, and explain Earth-Moon-Sun motion."
            )
            sciT3.forEach { (wk, comp) ->
                listToSeed.add(
                    Curriculum(
                        gradeLevel = "KS2",
                        subject = "Science",
                        term = "3rd Term",
                        week = wk,
                        melcCode = "S5-PSC-W$wk",
                        sessionsBudgeted = 4,
                        contentStandard = "Demonstrates understanding of electric current/circuits, landforms, rocks and soil formation, weathering/erosion, the water cycle, weather disturbances, and the solar system.",
                        performanceStandard = "Investigates static electricity, builds responsive circuits, classifies soil/landforms/weather disturbances, and models Moon phases and the solar system.",
                        learningCompetency = comp
                    )
                )
            }

            // 3. Grade 5 EPP/TLE (KS2)
            val eppT1 = listOf(
                1 to "• Netiquette: Natatalakay ang mga panuntunan ng netiquette sa paggamit ng internet.",
                2 to "• Web Browsers: Nakikilala ang mga uri ng web browser at mga bahagi nito.",
                3 to "• Search Engines: Nagagamit ang search engine sa ligtas at responsableng pamamaraan ng pagsasaliksik.",
                4 to "• Digital Detectives: Pagsasaliksik gamit ang search engine para sa pag-verify ng impormasyon.",
                5 to "• Anatomiya ng Browser: Pag-label at pagkilala sa mga bahagi ng isang web browser.",
                6 to "• Netiquette Poster Pitch: Paglikha ng poster para sa responsableng paggamit ng internet.",
                7 to "• E-mail: Nakapagpapadala ng mensahe gamit ang E-mail sa ligtas at responsableng pamamaraan.",
                8 to "• Email Writing: Paggawa ng email na may tamang address, subject line, at nilalaman.",
                9 to "• E-mail Attachments: Paggamit ng modernong email para sa pagpapadala ng mga dokumento.",
                10 to "• EDR-ICT Synthesis: Praktikal na ebalwasyon sa netiquette, search engine, at e-mail."
            )
            eppT1.forEach { (wk, comp) ->
                listToSeed.add(
                    Curriculum(
                        gradeLevel = "KS2",
                        subject = "EPP/TLE",
                        term = "1st Term",
                        week = wk,
                        melcCode = "EPP5-ICT-W$wk",
                        sessionsBudgeted = 4,
                        contentStandard = "Naipamamalas ang pag-unawa sa mga panuntunan ng netiquette, at sa paggamit ng web browser, search engine, at E-mail.",
                        performanceStandard = "Ang mga mag-aaral ay nakagagawa ng web browser, search engine, at E-mail sa ligtas at responsableng pamamaraan.",
                        learningCompetency = comp
                    )
                )
            }

            val eppT2 = listOf(
                1 to "• Word Processing: Nakagagawa ng word document na may images, shapes, smartarts, tables at page background.",
                2 to "• Digital Poster Layouts: Paggawa ng poster gamit ang word processing tools.",
                3 to "• Advanced Word Formatting: Pagsasaayos ng margin, indent, at visual tables sa isang dokumento.",
                4 to "• Presentation Software: Nakagagawa ng slide presentation document.",
                5 to "• Slide Presentation Design: Paglikha ng presentation tungkol sa sarili o paksang pang-akademiko.",
                6 to "• EPP Presentation Assembly: Paggamit ng animation, disenyo, at slide transition sa mga pagtatanghal.",
                7 to "• Desktop Publishing: Nakagagawa ng desktop publishing document.",
                8 to "• Brochure/Flyers Creation: Paggawa ng marketing materials para sa isang nais na lokal na negosyo.",
                9 to "• Local business layout designs using professional templates in publisher software.",
                10 to "• Productivity Tools Synthesis: Presentasyon ng lahat ng nagawang flyer, slide, at dokumento."
            )
            eppT2.forEach { (wk, comp) ->
                listToSeed.add(
                    Curriculum(
                        gradeLevel = "KS2",
                        subject = "EPP/TLE",
                        term = "2nd Term",
                        week = wk,
                        melcCode = "EPP5-PROD-W$wk",
                        sessionsBudgeted = 4,
                        contentStandard = "Naipapamalas ang pag-unawa sa paggamit ng productivity software.",
                        performanceStandard = "Ang mga mag-aaral ay nakagagawa ng iba’t ibang dokumento gamit ang computing devices at productivity tools.",
                        learningCompetency = comp
                    )
                )
            }

            val eppT3 = listOf(
                1 to "• Spreadsheets: Nakagagawa ng spreadsheet na may basic functions at numerical formulas.",
                2 to "• Pagtutuos ng Kita: Paggamit ng spreadsheet para sa listahan ng paninda at pagkalkula ng kita.",
                3 to "• Advanced Spreadsheet Charts: Paggawa ng mga bar/line charts batay sa ledger ng benta.",
                4 to "• Spreadsheets formulas such as SUM, AVERAGE, and standard mathematics cells.",
                5 to "• Introduction to Block Coding: Nakikilala ang user-interface ng Block Coding (Scratch, Code.org, etc.).",
                6 to "• Pagkilala ng Interface: Paggalugad sa mga tool ng block coding at paggawa ng gumagalaw na karakter (Sprite).",
                7 to "• Advanced Block Coding: Nagagamit ang events, motion, sound, looks, at backdrops block codes.",
                8 to "• Move-it Animation: Paglikha ng animation (Sprite) na may motion, sound, at background changes.",
                9 to "• Interactive gaming blocks, logical loops, and action trigger responses.",
                10 to "• Block Coding Evaluation: Mini-game presentation showing animation, sprites, and loops."
            )
            eppT3.forEach { (wk, comp) ->
                listToSeed.add(
                    Curriculum(
                        gradeLevel = "KS2",
                        subject = "EPP/TLE",
                        term = "3rd Term",
                        week = wk,
                        melcCode = "EPP5-CODE-W$wk",
                        sessionsBudgeted = 4,
                        contentStandard = "Naipamamalas ang pag-unawa sa paggamit ng spreadsheets at block code structures.",
                        performanceStandard = "Ang mga mag-aaral ay nakagagawa ng animations o games gamit ang Block Coding.",
                        learningCompetency = comp
                    )
                )
            }

            // Seed into Room DB!
            for (curr in listToSeed) {
                appDao.insertCurriculum(curr)
            }

            // Seed an initial ILAW lesson plan draft
            val samplePlan = LessonPlan(
                gradeLevel = "KS2",
                subject = "Science",
                term = "1st Term",
                week = 5,
                intentions = "{\"aim\": \"Configure simple scientific investigation steps correctly and plan inquiry processes.\", \"duration\": \"60 mins\"}",
                learningExperiences = "{\"activities\": [\"Group brainstorming on physical inquiry designs.\", \"Mock setup testing using local beakers and materials.\", \"Formulating hypotheses for state changes.\"]}",
                assessment = "{\"rubric\": \"Qualitative PACE checklist monitoring inquiry understanding (Problem formulation, materials, and findings evaluation).\"}",
                waysForward = "{\"remediation\": \"Assisted visual prompt worksheets for slower learners.\", \"enrichment\": \"Designing advanced variables cards for expanded inquiry challenges.\"}",
                eieLevel = "Hayo",
                specificGradeLevel = "Grade 5",
                teachingStrategy = "Inquiry-Based Learning",
                durationMins = 60,
                customPrompt = "Incorporate local scientific variables for thermal transitions."
            )
            appDao.insertLessonPlan(samplePlan)

            logSystemEvent("SYSTEM_INIT", "Database seed finished: Standard built-in DepEd Grade 5 Budget of Work (TALA) catalog seeded successfully.")
        }
    }
}
