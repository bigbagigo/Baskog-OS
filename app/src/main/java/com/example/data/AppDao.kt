package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Curriculum (MELC & BOW Archive)
    @Query("SELECT * FROM curriculum ORDER BY subject ASC, CASE term WHEN '1st Term' THEN 1 WHEN '2nd Term' THEN 2 WHEN '3rd Term' THEN 3 ELSE 4 END ASC, week ASC")
    fun getAllCurriculum(): Flow<List<Curriculum>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurriculum(curriculum: Curriculum)

    @Query("DELETE FROM curriculum WHERE id = :id")
    suspend fun deleteCurriculumById(id: Int)

    @Query("DELETE FROM curriculum")
    suspend fun clearCurriculum()

    // Lesson Plans
    @Query("SELECT * FROM lesson_plans ORDER BY id DESC")
    fun getAllLessonPlans(): Flow<List<LessonPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonPlan(plan: LessonPlan)

    @Query("DELETE FROM lesson_plans WHERE id = :id")
    suspend fun deleteLessonPlanById(id: Int)

    // System Logs
    @Query("SELECT * FROM system_logs ORDER BY timestamp DESC")
    fun getAllSystemLogs(): Flow<List<SystemLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSystemLog(log: SystemLog)

    @Query("DELETE FROM system_logs")
    suspend fun clearSystemLogs()
}
