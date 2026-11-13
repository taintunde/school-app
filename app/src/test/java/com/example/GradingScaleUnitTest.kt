package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GradingScaleUnitTest {

    @Test
    fun testSerializationRoundTrip() {
        val bands = listOf(
            GradeBand("A", 70.0, 100.0, 5.0, "Excellent", "#15803D"),
            GradeBand("B", 60.0, 69.99, 4.0, "Very Good", "#0D9488"),
            GradeBand("C", 50.0, 59.99, 3.0, "Good", "#2563EB"),
            GradeBand("D", 45.0, 49.99, 2.0, "Fair", "#D97706"),
            GradeBand("E", 40.0, 44.99, 1.0, "Pass", "#EA580C"),
            GradeBand("F", 0.0, 39.99, 0.0, "Fail", "#DC2626")
        )

        val json = GradingScaleSerializer.toJson(bands)
        assertNotNull(json)
        assertTrue(json.contains("\"grade\":\"A\""))

        val deserialized = GradingScaleSerializer.fromJson(json)
        assertEquals(bands.size, deserialized.size)
        assertEquals("A", deserialized[0].grade)
        assertEquals(70.0, deserialized[0].minScore, 0.01)
        assertEquals(100.0, deserialized[0].maxScore, 0.01)
        assertEquals(5.0, deserialized[0].gpaPoints, 0.01)
        assertEquals("Fail", deserialized.last().remark)
    }

    @Test
    fun testWaecNecoScoreEvaluation() {
        val waecScale = GradingScaleConfig(
            schoolLevel = "Senior Secondary (SS)",
            scaleName = "WAEC / NECO 9-Point Scale",
            maxGpa = 5.0,
            gradeBandsJson = GradingScaleSerializer.toJson(GradingScalePresets.WAEC_NECO_SCALE)
        )

        // 82 -> A1
        assertEquals("A1", waecScale.evaluateScore(82.0).grade)
        assertEquals(5.0, waecScale.evaluateScore(82.0).gpaPoints, 0.01)

        // 72 -> B2
        assertEquals("B2", waecScale.evaluateScore(72.0).grade)
        assertEquals(4.5, waecScale.evaluateScore(72.0).gpaPoints, 0.01)

        // 62 -> C4
        assertEquals("C4", waecScale.evaluateScore(62.0).grade)
        assertEquals(3.5, waecScale.evaluateScore(62.0).gpaPoints, 0.01)

        // 35 -> F9
        assertEquals("F9", waecScale.evaluateScore(35.0).grade)
        assertEquals(0.0, waecScale.evaluateScore(35.0).gpaPoints, 0.01)
    }

    @Test
    fun testCustomScoreBracketsAndGpaCalculation() {
        // User custom brackets: A=70-100 (5 pts), B=60-69 (4 pts), C=50-59 (3 pts), D=40-49 (2 pts), F=0-39 (0 pts)
        val customBands = listOf(
            GradeBand("A", 70.0, 100.0, 5.0, "Excellent", "#15803D"),
            GradeBand("B", 60.0, 69.9, 4.0, "Good", "#0D9488"),
            GradeBand("C", 50.0, 59.9, 3.0, "Average", "#2563EB"),
            GradeBand("D", 40.0, 49.9, 2.0, "Pass", "#D97706"),
            GradeBand("F", 0.0, 39.9, 0.0, "Fail", "#DC2626")
        )

        val customScale = GradingScaleConfig(
            schoolLevel = "Junior Secondary (JSS)",
            scaleName = "Custom JSS Scale",
            maxGpa = 5.0,
            subjectWeightingType = "Equal",
            gradeBandsJson = GradingScaleSerializer.toJson(customBands)
        )

        val scores = listOf(
            SubjectScore(studentId = 1, subjectName = "Mathematics", caScore = 30.0, examScore = 45.0), // 75 -> A (5.0)
            SubjectScore(studentId = 1, subjectName = "English Language", caScore = 25.0, examScore = 40.0), // 65 -> B (4.0)
            SubjectScore(studentId = 1, subjectName = "Basic Science", caScore = 20.0, examScore = 35.0) // 55 -> C (3.0)
        )

        // Unweighted GPA: (5.0 + 4.0 + 3.0) / 3 = 4.00
        val gpa = customScale.calculateStudentGpa(scores)
        assertEquals(4.00, gpa, 0.01)
    }

    @Test
    fun testWeightedGpaCalculationWithCreditUnits() {
        val bands = listOf(
            GradeBand("A", 70.0, 100.0, 5.0, "First Class", "#15803D"),
            GradeBand("B", 60.0, 69.9, 4.0, "Second Class Upper", "#0D9488"),
            GradeBand("C", 50.0, 59.9, 3.0, "Second Class Lower", "#2563EB")
        )

        val weightedScale = GradingScaleConfig(
            schoolLevel = "Tertiary / College",
            scaleName = "Weighted Degree Scale",
            maxGpa = 5.0,
            subjectWeightingType = "CreditUnits",
            defaultCreditUnits = 1,
            gradeBandsJson = GradingScaleSerializer.toJson(bands)
        )

        val scores = listOf(
            SubjectScore(studentId = 1, subjectName = "Mathematics", caScore = 35.0, examScore = 45.0), // 80 -> A (5.0 pts)
            SubjectScore(studentId = 1, subjectName = "English Language", caScore = 25.0, examScore = 38.0) // 63 -> B (4.0 pts)
        )

        // Math has 4 credits, English has 2 credits
        val creditMap = mapOf("Mathematics" to 4, "English Language" to 2)
        // Weighted: (5.0 * 4 + 4.0 * 2) / (4 + 2) = (20 + 8) / 6 = 28 / 6 = 4.67
        val gpa = weightedScale.calculateStudentGpa(scores, creditMap)
        assertEquals(4.67, gpa, 0.01)
    }
}
