package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY fullName ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Transaction
    @Query("SELECT * FROM students ORDER BY fullName ASC")
    fun getAllStudentsWithScores(): Flow<List<StudentWithScores>>

    @Transaction
    @Query("SELECT * FROM students WHERE id = :id")
    fun getStudentWithScoresById(id: Int): Flow<StudentWithScores?>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Int): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()
}

@Dao
interface SubjectScoreDao {
    @Query("SELECT * FROM subject_scores WHERE studentId = :studentId")
    fun getScoresForStudent(studentId: Int): Flow<List<SubjectScore>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: SubjectScore)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: List<SubjectScore>)

    @Query("DELETE FROM subject_scores WHERE studentId = :studentId")
    suspend fun deleteScoresForStudent(studentId: Int)

    @Delete
    suspend fun deleteScore(score: SubjectScore)
}

@Dao
interface SchoolSettingsDao {
    @Query("SELECT * FROM school_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SchoolSettings?>

    @Query("SELECT * FROM school_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SchoolSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SchoolSettings)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjectsFlow(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE classLevel = :classLevel ORDER BY name ASC")
    fun getSubjectsByClassFlow(classLevel: String): Flow<List<Subject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()
}

@Dao
interface SchoolClassDao {
    @Query("SELECT * FROM school_classes ORDER BY id ASC")
    fun getAllClassesFlow(): Flow<List<SchoolClass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(schoolClass: SchoolClass): Long

    @Delete
    suspend fun deleteClass(schoolClass: SchoolClass)

    @Query("DELETE FROM school_classes")
    suspend fun deleteAllClasses()
}

@Dao
interface GradingScaleDao {
    @Query("SELECT * FROM grading_scale_configs ORDER BY id ASC")
    fun getAllScalesFlow(): Flow<List<GradingScaleConfig>>

    @Query("SELECT * FROM grading_scale_configs WHERE schoolLevel = :level LIMIT 1")
    fun getScaleForLevelFlow(level: String): Flow<GradingScaleConfig?>

    @Query("SELECT * FROM grading_scale_configs WHERE isDefault = 1 LIMIT 1")
    fun getDefaultScaleFlow(): Flow<GradingScaleConfig?>

    @Query("SELECT * FROM grading_scale_configs WHERE id = :id LIMIT 1")
    suspend fun getScaleById(id: Int): GradingScaleConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScale(config: GradingScaleConfig): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScales(configs: List<GradingScaleConfig>)

    @Update
    suspend fun updateScale(config: GradingScaleConfig)

    @Delete
    suspend fun deleteScale(config: GradingScaleConfig)

    @Query("DELETE FROM grading_scale_configs")
    suspend fun deleteAllScales()
}

