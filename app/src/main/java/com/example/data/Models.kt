package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["studentClass", "term", "session"])
    ]
)
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String, // e.g., "STU/2026/001"
    val fullName: String,
    val gender: String, // Male, Female
    val studentClass: String, // e.g., Primary 1, JSS1, SS3
    val term: String, // 1st Term, 2nd Term, 3rd Term
    val session: String, // e.g., 2025/2026
    val attendancePresent: Int = 0,
    val attendanceTotal: Int = 0,
    val teacherComment: String = "",
    val principalComment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "academic_results",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["studentId", "term", "session"], unique = true)
    ]
)
data class AcademicResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int, // Foreign key referencing Student.id
    val term: String, // e.g., "1st Term", "2nd Term", "3rd Term"
    val session: String, // e.g., "2025/2026"
    val studentClass: String, // e.g., "JSS 1", "SS 1"
    val totalScore: Double = 0.0,
    val averageScore: Double = 0.0,
    val gpa: Double = 0.0,
    val overallGrade: String = "F",
    val overallRemark: String = "Fail",
    val positionInClass: String = "",
    val subjectsCount: Int = 0,
    val subjectsPassed: Int = 0,
    val teacherComment: String = "",
    val principalComment: String = "",
    val isPublished: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subject_scores")
data class SubjectScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int, // References Student.id
    val subjectName: String, // e.g., Mathematics, English Language
    val caScore: Double, // Continuous Assessment (max 30 or 40)
    val examScore: Double // Examination (max 70 or 60)
) {
    val totalScore: Double get() = caScore + examScore
    
    val grade: String get() = getGrade(totalScore)
    val remark: String get() = getRemark(totalScore)
}

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // Core, Science, Arts, Commercial, Early-Years
    val classLevel: String, // e.g., JSS 1, SS 1, Primary 1, All Classes
    val description: String = "",
    val creditUnits: Int = 1 // Credit weighting for GPA computation
)

@Entity(tableName = "school_classes")
data class SchoolClass(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val className: String
)

@Entity(tableName = "school_settings")
data class SchoolSettings(
    @PrimaryKey val id: Int = 1, // Single row for system settings
    val schoolName: String = "SmartResult International Academy",
    val address: String = "10, Alausa Way, Ikeja, Lagos, Nigeria",
    val contactInfo: String = "info@smartresultng.com | +234 803 123 4567",
    val logoUri: String? = null, // Path to local logo asset or uploaded image
    val resultTemplate: String = "Modern", // Simple, Modern, Detailed
    val isPremium: Boolean = false, // Toggle between Free (limit 20 students) and Premium
    val firestoreApiKey: String = "",
    val firestoreProjectId: String = "",
    val firestoreAppId: String = "",
    val firestoreStatus: String = "Disconnected", // Disconnected, Active Link
    val immersiveMode: Boolean = false, // Immersive branding mode
    val brandColorHex: String = "#013220", // Primary brand color hex (default School Dark Green)
    
    // Visual Template Customization fields
    val logoPlacement: String = "Center", // Left, Center, Right, Hidden
    val termLabel: String = "Term", // Term, Semester, Quarter, Custom
    val sessionLabel: String = "Session", // Session, Academic Year, Year, Custom
    val studentIdLabel: String = "Admission No", // Admission No, Student ID, Reg No, Custom
    val showHeaderAddress: Boolean = true,
    val showHeaderContact: Boolean = true,
    val showSignatureLines: Boolean = true,
    val showTeacherComments: Boolean = true,
    val showPrincipalComments: Boolean = true,
    val showGradeScaleKey: Boolean = true,
    val cardCornerRadius: Int = 8, // in dp
    val reportDensity: String = "Normal" // Compact, Normal, Spacious
)

// Firestore Document Schema Representation
data class FirestoreStudentDocument(
    val studentId: String = "",
    val fullName: String = "",
    val gender: String = "",
    val studentClass: String = "",
    val term: String = "",
    val session: String = "",
    val attendancePresent: Int = 0,
    val attendanceTotal: Int = 0,
    val teacherComment: String = "",
    val principalComment: String = "",
    val scores: List<FirestoreScoreMap> = emptyList(),
    val averageScore: Double = 0.0,
    val totalScore: Double = 0.0,
    val lastSyncedAt: Long = 0L
)

data class FirestoreScoreMap(
    val subjectName: String = "",
    val caScore: Double = 0.0,
    val examScore: Double = 0.0,
    val totalScore: Double = 0.0,
    val grade: String = "",
    val remark: String = ""
)

data class FirestoreSubjectDocument(
    val subjectName: String = "",
    val category: String = "", // Core, Science, Arts, Commercial, Early-Years
    val description: String = "",
    val recommendedClass: String = "All Classes"
)

data class FirestoreGradingScaleDocument(
    val grade: String = "",
    val minScore: Double = 0.0,
    val maxScore: Double = 100.0,
    val remark: String = "",
    val description: String = ""
)

data class StudentWithScores(
    @Embedded val student: Student,
    @Relation(
        parentColumn = "id",
        entityColumn = "studentId"
    )
    val scores: List<SubjectScore>
) {
    val totalScore: Double get() = scores.sumOf { it.totalScore }
    val averageScore: Double get() = if (scores.isNotEmpty()) totalScore / scores.size else 0.0
}

data class StudentWithAcademicResults(
    @Embedded val student: Student,
    @Relation(
        parentColumn = "id",
        entityColumn = "studentId"
    )
    val academicResults: List<AcademicResult>
)

// Nigerian standard grading system
fun getGrade(score: Double): String {
    return when {
        score >= 70.0 -> "A"
        score >= 60.0 -> "B"
        score >= 50.0 -> "C"
        score >= 45.0 -> "D"
        score >= 40.0 -> "E"
        else -> "F"
    }
}

fun getRemark(score: Double): String {
    return when {
        score >= 70.0 -> "Excellent"
        score >= 60.0 -> "Very Good"
        score >= 50.0 -> "Good"
        score >= 45.0 -> "Fair"
        score >= 40.0 -> "Pass"
        else -> "Fail"
    }
}

// -------------------------------------------------------------
// CUSTOM GRADING SCALES & GPA WEIGHTING DATA MODELS
// -------------------------------------------------------------

data class GradeBand(
    val grade: String, // e.g. "A1", "A", "B2", "Emerging"
    val minScore: Double, // e.g. 70.0
    val maxScore: Double, // e.g. 100.0
    val gpaPoints: Double, // e.g. 5.0, 4.0
    val remark: String, // e.g. "Excellent", "Credit", "Outstanding"
    val colorHex: String = "#15803D", // HEX color for visual badges and spectrum bar
    val description: String = ""
)

@Entity(tableName = "grading_scale_configs")
data class GradingScaleConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val schoolLevel: String, // "Senior Secondary (SS)", "Junior Secondary (JSS)", "Primary / Elementary", "Early Years", "Tertiary / College", "General / All Levels"
    val scaleName: String, // e.g. "WAEC / NECO Standard (A1-F9)", "Standard Nigerian 5.0 (A-F)"
    val maxGpa: Double = 5.0, // 5.0, 4.0, 10.0
    val passingGrade: String = "Pass", // Passing boundary label or threshold
    val isDefault: Boolean = false,
    val gradeBandsJson: String = "", // JSON serialized List<GradeBand>
    val subjectWeightingType: String = "Equal", // "Equal" or "CreditUnits"
    val defaultCreditUnits: Int = 1,
    val appliesToClasses: String = "All" // e.g. "SS 1, SS 2, SS 3" or "All"
) {
    // Lazy or parsed list of grade bands
    val gradeBands: List<GradeBand>
        get() = GradingScaleSerializer.fromJson(gradeBandsJson)

    // Lookup matching grade band for a given score
    fun evaluateScore(score: Double): GradeBand {
        val bands = gradeBands
        if (bands.isEmpty()) {
            return GradeBand(
                grade = getGrade(score),
                minScore = 0.0,
                maxScore = 100.0,
                gpaPoints = if (score >= 70) 5.0 else if (score >= 60) 4.0 else if (score >= 50) 3.0 else if (score >= 45) 2.0 else if (score >= 40) 1.0 else 0.0,
                remark = getRemark(score),
                colorHex = if (score >= 70) "#15803D" else if (score >= 50) "#0284C7" else if (score >= 40) "#D97706" else "#DC2626"
            )
        }
        // Match within bounds [minScore, maxScore]
        return bands.firstOrNull { score >= it.minScore && score <= it.maxScore }
            ?: bands.minByOrNull { Math.abs(it.minScore - score) }
            ?: bands.first()
    }

    // Calculate unweighted or weighted GPA for a student's scores
    fun calculateStudentGpa(scores: List<SubjectScore>, subjectCreditMap: Map<String, Int> = emptyMap()): Double {
        if (scores.isEmpty()) return 0.0
        var totalWeightedPoints = 0.0
        var totalUnits = 0.0

        for (score in scores) {
            val band = evaluateScore(score.totalScore)
            val units = if (subjectWeightingType == "CreditUnits") {
                subjectCreditMap[score.subjectName] ?: defaultCreditUnits
            } else {
                1
            }
            totalWeightedPoints += (band.gpaPoints * units)
            totalUnits += units
        }

        return if (totalUnits > 0) {
            val gpa = totalWeightedPoints / totalUnits
            Math.round(gpa * 100.0) / 100.0
        } else {
            0.0
        }
    }
}

object GradingScaleSerializer {
    fun toJson(bands: List<GradeBand>): String {
        val array = org.json.JSONArray()
        for (band in bands) {
            val obj = org.json.JSONObject()
            obj.put("grade", band.grade)
            obj.put("minScore", band.minScore)
            obj.put("maxScore", band.maxScore)
            obj.put("gpaPoints", band.gpaPoints)
            obj.put("remark", band.remark)
            obj.put("colorHex", band.colorHex)
            obj.put("description", band.description)
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String?): List<GradeBand> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<GradeBand>()
        try {
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GradeBand(
                        grade = obj.optString("grade", "F"),
                        minScore = obj.optDouble("minScore", 0.0),
                        maxScore = obj.optDouble("maxScore", 100.0),
                        gpaPoints = obj.optDouble("gpaPoints", 0.0),
                        remark = obj.optString("remark", "Fail"),
                        colorHex = obj.optString("colorHex", "#DC2626"),
                        description = obj.optString("description", "")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }
}

object GradingScalePresets {
    // Senior Secondary: WAEC / NECO 9-Point Scale & 5.0 Max GPA
    val WAEC_NECO_SCALE = listOf(
        GradeBand("A1", 75.0, 100.0, 5.0, "Excellent", "#15803D", "Distinction performance"),
        GradeBand("B2", 70.0, 74.99, 4.5, "Very Good", "#16A34A", "High standard"),
        GradeBand("B3", 65.0, 69.99, 4.0, "Good", "#0D9488", "Above average standard"),
        GradeBand("C4", 60.0, 64.99, 3.5, "Credit", "#0284C7", "Solid credit mark"),
        GradeBand("C5", 55.0, 59.99, 3.0, "Credit", "#2563EB", "Satisfactory credit mark"),
        GradeBand("C6", 50.0, 54.99, 2.5, "Credit", "#4F46E5", "Acceptable credit mark"),
        GradeBand("D7", 45.0, 49.99, 2.0, "Pass", "#D97706", "Marginal pass"),
        GradeBand("E8", 40.0, 44.99, 1.0, "Pass", "#EA580C", "Minimum pass"),
        GradeBand("F9", 0.0, 39.99, 0.0, "Fail", "#DC2626", "Failed / Retake required")
    )

    // Junior Secondary & General: Standard Nigerian 5-Point Scale (A-F)
    val STANDARD_SECONDARY_SCALE = listOf(
        GradeBand("A", 70.0, 100.0, 5.0, "Excellent", "#15803D", "Exemplary understanding"),
        GradeBand("B", 60.0, 69.99, 4.0, "Very Good", "#0D9488", "Above average proficiency"),
        GradeBand("C", 50.0, 59.99, 3.0, "Good", "#2563EB", "Demonstrates core proficiency"),
        GradeBand("D", 45.0, 49.99, 2.0, "Fair", "#D97706", "Fair understanding of concepts"),
        GradeBand("E", 40.0, 44.99, 1.0, "Pass", "#EA580C", "Meets minimum passing grade"),
        GradeBand("F", 0.0, 39.99, 0.0, "Fail", "#DC2626", "Does not meet basic criteria")
    )

    // Primary & Elementary: 4-Tier Assessment Standards & 4.0 GPA
    val PRIMARY_COMPETENCY_SCALE = listOf(
        GradeBand("A", 80.0, 100.0, 4.0, "Exceeding Standards", "#15803D", "Work demonstrates advanced comprehension"),
        GradeBand("B", 65.0, 79.99, 3.0, "Meeting Standards", "#0D9488", "Consistently grasps key grade concepts"),
        GradeBand("C", 50.0, 64.99, 2.0, "Approaching Standards", "#D97706", "Developing foundational competence"),
        GradeBand("D", 0.0, 49.99, 1.0, "Emerging / Needs Support", "#DC2626", "Requires targeted academic intervention")
    )

    // Tertiary / College: Degree Honours Classification (5.0 CGPA)
    val TERTIARY_CGPA_SCALE = listOf(
        GradeBand("A", 70.0, 100.0, 5.0, "First Class Honours", "#15803D", "Cumulative GPA 4.50 - 5.00"),
        GradeBand("B", 60.0, 69.99, 4.0, "Second Class Upper (2:1)", "#0D9488", "Cumulative GPA 3.50 - 4.49"),
        GradeBand("C", 50.0, 59.99, 3.0, "Second Class Lower (2:2)", "#2563EB", "Cumulative GPA 2.40 - 3.49"),
        GradeBand("D", 45.0, 49.99, 2.0, "Third Class", "#D97706", "Cumulative GPA 1.50 - 2.39"),
        GradeBand("E", 40.0, 44.99, 1.0, "Pass Degree", "#EA580C", "Cumulative GPA 1.00 - 1.49"),
        GradeBand("F", 0.0, 39.99, 0.0, "Fail", "#DC2626", "Below 1.00")
    )

    // US / Cambridge International 4.0 Scale
    val INTERNATIONAL_4_SCALE = listOf(
        GradeBand("A", 90.0, 100.0, 4.0, "Excellent", "#15803D", "Superior academic performance"),
        GradeBand("B", 80.0, 89.99, 3.0, "Above Average", "#0D9488", "High academic competence"),
        GradeBand("C", 70.0, 79.99, 2.0, "Average", "#2563EB", "Average mastery of objectives"),
        GradeBand("D", 60.0, 69.99, 1.0, "Below Average", "#D97706", "Passing but below expectation"),
        GradeBand("F", 0.0, 59.99, 0.0, "Failing", "#DC2626", "No credit granted")
    )

    fun getDefaultScales(): List<GradingScaleConfig> {
        return listOf(
            GradingScaleConfig(
                schoolLevel = "Senior Secondary (SS)",
                scaleName = "WAEC / NECO 9-Point Scale",
                maxGpa = 5.0,
                passingGrade = "Credit (C6)",
                isDefault = false,
                gradeBandsJson = GradingScaleSerializer.toJson(WAEC_NECO_SCALE),
                subjectWeightingType = "Equal",
                defaultCreditUnits = 1,
                appliesToClasses = "SS 1, SS 2, SS 3"
            ),
            GradingScaleConfig(
                schoolLevel = "Junior Secondary (JSS)",
                scaleName = "Standard Secondary (A-F)",
                maxGpa = 5.0,
                passingGrade = "Pass (E)",
                isDefault = true,
                gradeBandsJson = GradingScaleSerializer.toJson(STANDARD_SECONDARY_SCALE),
                subjectWeightingType = "Equal",
                defaultCreditUnits = 1,
                appliesToClasses = "JSS 1, JSS 2, JSS 3"
            ),
            GradingScaleConfig(
                schoolLevel = "Primary / Elementary",
                scaleName = "Primary Competency 4-Tier",
                maxGpa = 4.0,
                passingGrade = "Approaching (C)",
                isDefault = false,
                gradeBandsJson = GradingScaleSerializer.toJson(PRIMARY_COMPETENCY_SCALE),
                subjectWeightingType = "Equal",
                defaultCreditUnits = 1,
                appliesToClasses = "Primary 1, Primary 2, Primary 3, Primary 4, Primary 5, Primary 6"
            ),
            GradingScaleConfig(
                schoolLevel = "Tertiary / College",
                scaleName = "Degree Honours (5.0 CGPA)",
                maxGpa = 5.0,
                passingGrade = "Pass (E)",
                isDefault = false,
                gradeBandsJson = GradingScaleSerializer.toJson(TERTIARY_CGPA_SCALE),
                subjectWeightingType = "CreditUnits",
                defaultCreditUnits = 2,
                appliesToClasses = "Tertiary, ND, HND"
            ),
            GradingScaleConfig(
                schoolLevel = "General / All Levels",
                scaleName = "Universal Standard (A-F, 70-100)",
                maxGpa = 5.0,
                passingGrade = "Pass (E)",
                isDefault = false,
                gradeBandsJson = GradingScaleSerializer.toJson(STANDARD_SECONDARY_SCALE),
                subjectWeightingType = "Equal",
                defaultCreditUnits = 1,
                appliesToClasses = "All"
            )
        )
    }

    fun resolveScaleForClass(studentClass: String, scales: List<GradingScaleConfig>): GradingScaleConfig {
        val activeScales = scales.ifEmpty { getDefaultScales() }
        val normClass = studentClass.trim().lowercase()

        val directMatch = activeScales.find { scale ->
            scale.appliesToClasses.split(",").any { it.trim().equals(studentClass, ignoreCase = true) }
        }
        if (directMatch != null) return directMatch

        val keywordMatch = activeScales.find { scale ->
            val level = scale.schoolLevel.lowercase()
            when {
                normClass.contains("ss") || normClass.contains("senior") -> level.contains("senior") || level.contains("ss")
                normClass.contains("jss") || normClass.contains("junior") -> level.contains("junior") || level.contains("jss")
                normClass.contains("primary") || normClass.contains("basic") -> level.contains("primary") || level.contains("elementary")
                normClass.contains("nursery") || normClass.contains("creche") || normClass.contains("kindergarten") -> level.contains("early") || level.contains("nursery")
                normClass.contains("tertiary") || normClass.contains("nd") || normClass.contains("hnd") || normClass.contains("college") -> level.contains("tertiary") || level.contains("college")
                else -> false
            }
        }
        if (keywordMatch != null) return keywordMatch

        return activeScales.firstOrNull { it.isDefault }
            ?: activeScales.firstOrNull { it.schoolLevel.contains("General", ignoreCase = true) }
            ?: activeScales.first()
    }
}

