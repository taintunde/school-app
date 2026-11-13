package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AcademicResultRoomTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ResultRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ResultRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertStudentAndSyncAcademicResult_persistsStructuredRecord() = runTest {
        database.gradingScaleDao().insertScales(GradingScalePresets.getDefaultScales())

        val studentId = repository.insertStudent(
            Student(
                studentId = "STU/2026/101",
                fullName = "Tunde Bakare",
                gender = "Male",
                studentClass = "JSS 1",
                term = "3rd Term",
                session = "2025/2026",
                teacherComment = "Excellent focus in class.",
                principalComment = "Promoted with distinction."
            )
        ).toInt()

        repository.insertScores(
            listOf(
                SubjectScore(studentId = studentId, subjectName = "Mathematics", caScore = 30.0, examScore = 50.0), // 80 (A)
                SubjectScore(studentId = studentId, subjectName = "English Language", caScore = 25.0, examScore = 40.0) // 65 (B)
            )
        )

        repository.syncAcademicResultForStudent(studentId)

        val results = repository.getAcademicResultsForStudent(studentId).first()
        assertEquals(1, results.size)

        val academicResult = results.first()
        assertEquals(studentId, academicResult.studentId)
        assertEquals("3rd Term", academicResult.term)
        assertEquals("2025/2026", academicResult.session)
        assertEquals("JSS 1", academicResult.studentClass)
        assertEquals(145.0, academicResult.totalScore, 0.01)
        assertEquals(72.5, academicResult.averageScore, 0.01)
        assertEquals(4.5, academicResult.gpa, 0.01)
        assertEquals("A", academicResult.overallGrade)
        assertEquals("Excellent", academicResult.overallRemark)
        assertEquals(2, academicResult.subjectsCount)
        assertEquals(2, academicResult.subjectsPassed)
        assertEquals("1st out of 1", academicResult.positionInClass)
        assertEquals("Excellent focus in class.", academicResult.teacherComment)
        assertEquals("Promoted with distinction.", academicResult.principalComment)
    }

    @Test
    fun studentWithAcademicResultsRelation_andCascadeDeleteWorkCorrectly() = runTest {
        val sId = repository.insertStudent(
            Student(
                studentId = "STU/2026/102",
                fullName = "Zainab Musa",
                gender = "Female",
                studentClass = "SS 1",
                term = "1st Term",
                session = "2025/2026"
            )
        ).toInt()

        repository.insertAcademicResult(
            AcademicResult(
                studentId = sId,
                term = "1st Term",
                session = "2025/2026",
                studentClass = "SS 1",
                totalScore = 410.0,
                averageScore = 82.0,
                gpa = 5.0,
                overallGrade = "A1",
                overallRemark = "Excellent",
                positionInClass = "1st out of 12",
                subjectsCount = 5,
                subjectsPassed = 5,
                isPublished = true
            )
        )

        // Verify @Relation returns Student paired with AcademicResult list
        val studentWithResults = repository.getStudentWithAcademicResultsById(sId).first()
        assertNotNull(studentWithResults)
        assertEquals("Zainab Musa", studentWithResults!!.student.fullName)
        assertEquals(1, studentWithResults.academicResults.size)
        assertTrue(studentWithResults.academicResults.first().isPublished)

        // Updating score/sync preserves unique (studentId, term, session) without duplicating rows
        repository.insertScore(
            SubjectScore(studentId = sId, subjectName = "Biology", caScore = 30.0, examScore = 55.0)
        )
        repository.syncAcademicResultForStudent(sId)
        val afterSync = repository.getAcademicResultsForStudent(sId).first()
        assertEquals(1, afterSync.size)
        assertEquals(85.0, afterSync.first().averageScore, 0.01)

        // Deleting parent Student must cascade-delete child AcademicResult rows
        repository.deleteStudent(studentWithResults.student)
        val remainingResults = repository.allAcademicResults.first()
        assertTrue(remainingResults.isEmpty())
    }
}
