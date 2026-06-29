package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * KABAN Curriculum alignment database representing parsed MELC (Most Essential Learning Competencies)
 * and BOW (Budget of Work) variables.
 */
@Entity(tableName = "curriculum")
data class Curriculum(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val gradeLevel: String, // KS1, KS2, KS3, KS4
    val subject: String,
    val term: String, // 1st Term, 2nd Term, 3rd Term
    val week: Int,
    val melcCode: String = "", // Standard code, e.g. "M3NS-Ie-1"
    val sessionsBudgeted: Int = 5, // Budget of Work allocated class sessions
    val contentStandard: String,
    val performanceStandard: String,
    val learningCompetency: String // MELC competency
)

/**
 * Lesson plans stored inside the local storage, generated under the four-part ILAW format
 * and matching regional environmental hazard awareness parameters.
 */
@Entity(tableName = "lesson_plans")
data class LessonPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val gradeLevel: String,
    val subject: String,
    val term: String,
    val week: Int,
    val intentions: String, // Part 1: Intentions
    val learningExperiences: String, // Part 2: Learning Experiences
    val assessment: String, // Part 3: Assessment
    val waysForward: String, // Part 4: Ways Forward
    val eieLevel: String = "Hayo", // Security/DRRM continuity level at generation
    val teachingStrategy: String = "Collaborative", // Interactive strategy used
    val durationMins: Int = 60, // Estimated block time
    val customPrompt: String = "", // Custom educator configuration prompt used
    val specificGradeLevel: String = "",
    val language: String = "English",
    val ppstChecklist: String = "",
    val deliveryDate: String = "", // e.g. "Monday", "Tuesday", etc.
    val sectionName: String = "Section A", // e.g. "Aguinaldo"
    val periodNumbers: String = "1", // comma-separated, e.g. "1, 2"
    val formativeQuestionsJson: String = ""
)

/**
 * Log for storing operating events, parser updates, or compilation overrides.
 */
@Entity(tableName = "system_logs")
data class SystemLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventType: String, // PARSER, COMPILER, POLICY_EVENT, SECURITY
    val logMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)
