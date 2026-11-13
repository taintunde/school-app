package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class ResultRepository(private val database: AppDatabase) {
    private val studentDao = database.studentDao()
    private val scoreDao = database.subjectScoreDao()
    private val settingsDao = database.schoolSettingsDao()
    private val subjectDao = database.subjectDao()
    private val schoolClassDao = database.schoolClassDao()
    private val gradingScaleDao = database.gradingScaleDao()

    val allStudents: Flow<List<Student>> = studentDao.getAllStudents()
    val allStudentsWithScores: Flow<List<StudentWithScores>> = studentDao.getAllStudentsWithScores()
    val schoolSettings: Flow<SchoolSettings?> = settingsDao.getSettingsFlow()
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjectsFlow()
    val allClasses: Flow<List<SchoolClass>> = schoolClassDao.getAllClassesFlow()
    val allGradingScales: Flow<List<GradingScaleConfig>> = gradingScaleDao.getAllScalesFlow()

    fun getGradingScaleForLevel(level: String): Flow<GradingScaleConfig?> {
        return gradingScaleDao.getScaleForLevelFlow(level)
    }

    suspend fun insertGradingScale(scale: GradingScaleConfig): Long {
        return gradingScaleDao.insertScale(scale)
    }

    suspend fun updateGradingScale(scale: GradingScaleConfig) {
        gradingScaleDao.updateScale(scale)
    }

    suspend fun deleteGradingScale(scale: GradingScaleConfig) {
        gradingScaleDao.deleteScale(scale)
    }

    suspend fun resetGradingScalesToDefaults() {
        gradingScaleDao.deleteAllScales()
        gradingScaleDao.insertScales(GradingScalePresets.getDefaultScales())
    }

    fun getSubjectsByClass(classLevel: String): Flow<List<Subject>> {
        return subjectDao.getSubjectsByClassFlow(classLevel)
    }

    suspend fun insertSubject(subject: Subject): Long {
        return subjectDao.insertSubject(subject)
    }

    suspend fun updateSubject(subject: Subject) {
        subjectDao.updateSubject(subject)
    }

    suspend fun deleteSubject(subject: Subject) {
        subjectDao.deleteSubject(subject)
    }

    suspend fun insertClass(schoolClass: SchoolClass): Long {
        return schoolClassDao.insertClass(schoolClass)
    }

    suspend fun deleteClass(schoolClass: SchoolClass) {
        schoolClassDao.deleteClass(schoolClass)
    }

    fun getStudentWithScoresById(id: Int): Flow<StudentWithScores?> {
        return studentDao.getStudentWithScoresById(id)
    }

    suspend fun getStudentById(id: Int): Student? {
        return studentDao.getStudentById(id)
    }

    suspend fun insertStudent(student: Student): Long {
        return studentDao.insertStudent(student)
    }

    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.deleteStudent(student)
    }

    suspend fun insertScore(score: SubjectScore) {
        scoreDao.insertScore(score)
    }

    suspend fun insertScores(scores: List<SubjectScore>) {
        scoreDao.insertScores(scores)
    }

    suspend fun deleteScoresForStudent(studentId: Int) {
        scoreDao.deleteScoresForStudent(studentId)
    }

    suspend fun saveSettings(settings: SchoolSettings) {
        settingsDao.insertOrUpdateSettings(settings)
    }

    suspend fun seedInitialData() {
        // Check if settings exist, if not, create default settings
        val currentSettings = settingsDao.getSettings()
        if (currentSettings == null) {
            settingsDao.insertOrUpdateSettings(SchoolSettings())
        }

        // Check if database is empty of students, if so, seed a few students with scores
        val currentStudents = studentDao.getAllStudents().firstOrNull() ?: emptyList()
        if (currentStudents.isEmpty()) {
            val s1Id = studentDao.insertStudent(
                Student(
                    studentId = "STU/2026/001",
                    fullName = "Chinedu Okeke",
                    gender = "Male",
                    studentClass = "JSS 1",
                    term = "3rd Term",
                    session = "2025/2026",
                    attendancePresent = 58,
                    attendanceTotal = 60,
                    teacherComment = "Chinedu is an exceptionally brilliant student who shows great passion for mathematics.",
                    principalComment = "Outstanding performance. Keep maintaining this high standard."
                )
            ).toInt()

            val s2Id = studentDao.insertStudent(
                Student(
                    studentId = "STU/2026/002",
                    fullName = "Amina Bello",
                    gender = "Female",
                    studentClass = "SS 1",
                    term = "3rd Term",
                    session = "2025/2026",
                    attendancePresent = 55,
                    attendanceTotal = 60,
                    teacherComment = "Amina is highly diligent and has made outstanding progress in the sciences.",
                    principalComment = "Excellent results. Impressive focus and work ethic."
                )
            ).toInt()

            val s3Id = studentDao.insertStudent(
                Student(
                    studentId = "STU/2026/003",
                    fullName = "Olumide Adebayo",
                    gender = "Male",
                    studentClass = "Primary 5",
                    term = "3rd Term",
                    session = "2025/2026",
                    attendancePresent = 60,
                    attendanceTotal = 60,
                    teacherComment = "Olumide is active and participates enthusiastically in class projects.",
                    principalComment = "A very good result. Keep up the high level of curiosity."
                )
            ).toInt()

            val s4Id = studentDao.insertStudent(
                Student(
                    studentId = "STU/2026/004",
                    fullName = "Chioma Nnaji",
                    gender = "Female",
                    studentClass = "JSS 1",
                    term = "3rd Term",
                    session = "2025/2026",
                    attendancePresent = 52,
                    attendanceTotal = 60,
                    teacherComment = "Chioma is polite and attentive, but needs more focus in quantitative reasoning.",
                    principalComment = "Good results. With extra effort, she can achieve excellence."
                )
            ).toInt()

            // Seed scores for Chinedu Okeke
            scoreDao.insertScores(
                listOf(
                    SubjectScore(studentId = s1Id, subjectName = "Mathematics", caScore = 28.5, examScore = 65.0),
                    SubjectScore(studentId = s1Id, subjectName = "English Language", caScore = 24.0, examScore = 58.0),
                    SubjectScore(studentId = s1Id, subjectName = "Basic Science", caScore = 27.0, examScore = 61.5),
                    SubjectScore(studentId = s1Id, subjectName = "Agricultural Science", caScore = 25.0, examScore = 54.0),
                    SubjectScore(studentId = s1Id, subjectName = "Computer Studies (ICT)", caScore = 29.0, examScore = 68.0)
                )
            )

            // Seed scores for Amina Bello
            scoreDao.insertScores(
                listOf(
                    SubjectScore(studentId = s2Id, subjectName = "Mathematics", caScore = 25.0, examScore = 55.0),
                    SubjectScore(studentId = s2Id, subjectName = "English Language", caScore = 27.0, examScore = 63.5),
                    SubjectScore(studentId = s2Id, subjectName = "Biology", caScore = 26.0, examScore = 60.0),
                    SubjectScore(studentId = s2Id, subjectName = "Chemistry", caScore = 28.0, examScore = 64.0),
                    SubjectScore(studentId = s2Id, subjectName = "Physics", caScore = 24.5, examScore = 58.0)
                )
            )

            // Seed scores for Olumide Adebayo
            scoreDao.insertScores(
                listOf(
                    SubjectScore(studentId = s3Id, subjectName = "Mathematics", caScore = 22.0, examScore = 48.0),
                    SubjectScore(studentId = s3Id, subjectName = "English Language", caScore = 25.0, examScore = 53.0),
                    SubjectScore(studentId = s3Id, subjectName = "Basic Science & Tech", caScore = 21.0, examScore = 50.0),
                    SubjectScore(studentId = s3Id, subjectName = "Social Studies", caScore = 26.0, examScore = 58.0),
                    SubjectScore(studentId = s3Id, subjectName = "Home Economics", caScore = 24.0, examScore = 62.0)
                )
            )

            // Seed scores for Chioma Nnaji
            scoreDao.insertScores(
                listOf(
                    SubjectScore(studentId = s4Id, subjectName = "Mathematics", caScore = 15.0, examScore = 32.0), // Total 47 - D
                    SubjectScore(studentId = s4Id, subjectName = "English Language", caScore = 22.0, examScore = 48.0), // Total 70 - A
                    SubjectScore(studentId = s4Id, subjectName = "Basic Science", caScore = 18.0, examScore = 38.0), // Total 56 - C
                    SubjectScore(studentId = s4Id, subjectName = "Agricultural Science", caScore = 19.5, examScore = 41.0), // Total 60.5 - B
                    SubjectScore(studentId = s4Id, subjectName = "Computer Studies (ICT)", caScore = 20.0, examScore = 44.0) // Total 64 - B
                )
            )
        }

        // Seed subjects if empty
        val currentSubjects = subjectDao.getAllSubjectsFlow().firstOrNull() ?: emptyList()
        if (currentSubjects.isEmpty()) {
            val defaultSubjects = listOf(
                Subject(name = "Mathematics", category = "Core", classLevel = "All Classes", description = "Core logical and quantitative operations"),
                Subject(name = "English Language", category = "Core", classLevel = "All Classes", description = "Grammar, comprehension, and expressive writing"),
                Subject(name = "Basic Science", category = "Science", classLevel = "JSS 1", description = "Foundational scientific principles and exploration"),
                Subject(name = "Agricultural Science", category = "Science", classLevel = "JSS 1", description = "Crops, livestock, and modern farming methodologies"),
                Subject(name = "Computer Studies (ICT)", category = "Science", classLevel = "JSS 1", description = "Foundational computing concepts and practical skills"),
                Subject(name = "Biology", category = "Science", classLevel = "SS 1", description = "Study of living organisms, cells, and ecosystems"),
                Subject(name = "Chemistry", category = "Science", classLevel = "SS 1", description = "Matter, properties, reactions, and laboratory safety"),
                Subject(name = "Physics", category = "Science", classLevel = "SS 1", description = "Mechanics, energy, waves, and fundamental physical laws"),
                Subject(name = "Social Studies", category = "Core", classLevel = "Primary 5", description = "Civics, culture, community development, and leadership"),
                Subject(name = "Home Economics", category = "Core", classLevel = "Primary 5", description = "Nutrition, food preparation, and sewing fundamentals"),
                Subject(name = "Basic Science & Tech", category = "Science", classLevel = "Primary 5", description = "Introduction to science and engineering modules")
            )
            defaultSubjects.forEach {
                subjectDao.insertSubject(it)
            }
        }

        // Seed school classes if empty
        val currentClasses = schoolClassDao.getAllClassesFlow().firstOrNull() ?: emptyList()
        if (currentClasses.isEmpty()) {
            val defaultClasses = listOf(
                "Creche", "Pre-Nursery", "Nursery 1", "Nursery 2",
                "Primary 1", "Primary 2", "Primary 3", "Primary 4", "Primary 5", "Primary 6",
                "JSS 1", "JSS 2", "JSS 3", "SS 1", "SS 2", "SS 3"
            )
            defaultClasses.forEach {
                schoolClassDao.insertClass(SchoolClass(className = it))
            }
        }

        // Seed default grading scales & GPA weighting configs if empty
        val currentScales = gradingScaleDao.getAllScalesFlow().firstOrNull() ?: emptyList()
        if (currentScales.isEmpty()) {
            gradingScaleDao.insertScales(GradingScalePresets.getDefaultScales())
        }
    }
}
