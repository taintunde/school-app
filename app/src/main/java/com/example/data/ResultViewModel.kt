package com.example.data

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.util.Locale

enum class AppScreen {
    Dashboard,
    AddStudent,
    InputScores,
    LedgerTable,
    GenerateResults,
    Settings,
    CloudSync,
    ManageSubjects,
    PerformanceAnalytics,
    AddInfo,
    TemplateEditor,
    Export,
    CsvImport,
    GradingScaleConfig
}

class ResultViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ResultRepository

    // UI state streams
    val allStudents: StateFlow<List<Student>>
    val allStudentsWithScores: StateFlow<List<StudentWithScores>>
    val schoolSettings: StateFlow<SchoolSettings>
    val allSubjects: StateFlow<List<Subject>>
    val allClasses: StateFlow<List<SchoolClass>>
    val allGradingScales: StateFlow<List<GradingScaleConfig>>

    // Navigation and interactive states
    private val _currentScreen = MutableStateFlow(AppScreen.Dashboard)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _addInfoSubTab = MutableStateFlow(0) // 0: Add Student, 1: Add Class, 2: Manage Subjects
    val addInfoSubTab: StateFlow<Int> = _addInfoSubTab.asStateFlow()

    private val _selectedStudentId = MutableStateFlow<Int?>(null)
    val selectedStudentId: StateFlow<Int?> = _selectedStudentId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ResultRepository(database)

        allStudents = repository.allStudents
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allStudentsWithScores = repository.allStudentsWithScores
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allSubjects = repository.allSubjects
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allClasses = repository.allClasses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allGradingScales = repository.allGradingScales
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Ensure settings are never null in UI
        schoolSettings = repository.schoolSettings
            .map { it ?: SchoolSettings() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SchoolSettings())

        // Seed initial Nigerian student records and school profile
        viewModelScope.launch {
            repository.seedInitialData()
        }
    }

    private val navigationBackStack = ArrayDeque<AppScreen>()

    fun navigateTo(screen: AppScreen) {
        val current = _currentScreen.value
        if (screen == current) return
        if (screen == AppScreen.Dashboard) {
            navigationBackStack.clear()
        } else {
            navigationBackStack.addLast(current)
            if (navigationBackStack.size > 20) {
                navigationBackStack.removeFirst()
            }
        }
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (navigationBackStack.isNotEmpty()) {
            _currentScreen.value = navigationBackStack.removeLast()
            return true
        }
        if (_currentScreen.value != AppScreen.Dashboard) {
            _currentScreen.value = AppScreen.Dashboard
            return true
        }
        return false
    }

    // Resolves appropriate grading scale for a class level
    fun getGradingScaleForStudent(studentClass: String): GradingScaleConfig {
        val scales = allGradingScales.value
        if (scales.isEmpty()) {
            return GradingScalePresets.getDefaultScales().firstOrNull { it.isDefault } ?: GradingScalePresets.getDefaultScales().first()
        }

        val normClass = studentClass.trim().lowercase()

        // 1. Direct match by appliesToClasses list
        val directMatch = scales.find { scale ->
            scale.appliesToClasses.split(",").any { it.trim().equals(studentClass, ignoreCase = true) }
        }
        if (directMatch != null) return directMatch

        // 2. Keyword heuristic matching
        val keywordMatch = scales.find { scale ->
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

        // 3. Fallback to default scale or first available
        return scales.firstOrNull { it.isDefault } ?: scales.firstOrNull { it.schoolLevel.contains("General", ignoreCase = true) } ?: scales.first()
    }

    // Evaluate grade and remark for a specific score under a student's class
    fun evaluateScoreForClass(studentClass: String, score: Double): GradeBand {
        val scale = getGradingScaleForStudent(studentClass)
        return scale.evaluateScore(score)
    }

    // Calculate term GPA for a student
    fun calculateStudentGpa(studentClass: String, scores: List<SubjectScore>): Double {
        val scale = getGradingScaleForStudent(studentClass)
        val creditMap = allSubjects.value.associate { it.name to it.creditUnits }
        return scale.calculateStudentGpa(scores, creditMap)
    }

    // Grading scale configuration operations
    fun saveGradingScale(scale: GradingScaleConfig, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (scale.id > 0) {
                repository.updateGradingScale(scale)
            } else {
                repository.insertGradingScale(scale)
            }
            onSuccess()
        }
    }

    fun deleteGradingScale(scale: GradingScaleConfig, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteGradingScale(scale)
            onSuccess()
        }
    }

    fun resetGradingScalesToDefaults(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.resetGradingScalesToDefaults()
            onSuccess()
        }
    }

    fun setAddInfoSubTab(tabIndex: Int) {
        _addInfoSubTab.value = tabIndex
    }

    fun navigateToAddInfo(tabIndex: Int) {
        _addInfoSubTab.value = tabIndex
        navigateTo(AppScreen.AddInfo)
    }

    fun addClass(className: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertClass(SchoolClass(className = className.trim()))
            onSuccess()
        }
    }

    fun deleteClass(schoolClass: SchoolClass, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteClass(schoolClass)
            onSuccess()
        }
    }

    fun selectStudent(studentId: Int?) {
        _selectedStudentId.value = studentId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Subject Management Actions
    fun addSubject(
        name: String,
        category: String,
        classLevel: String,
        description: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.insertSubject(
                Subject(
                    name = name.trim(),
                    category = category,
                    classLevel = classLevel,
                    description = description.trim()
                )
            )
            onSuccess()
        }
    }

    fun updateSubject(subject: Subject, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateSubject(subject)
            onSuccess()
        }
    }

    fun deleteSubject(subject: Subject, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            onSuccess()
        }
    }

    // Student Management Actions
    fun addStudent(
        fullName: String,
        studentId: String,
        gender: String,
        studentClass: String,
        term: String,
        session: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val settings = schoolSettings.value
            val currentCount = allStudents.value.size
            if (!settings.isPremium && currentCount >= 20) {
                onError("Limit of 20 students reached for the Free version. Please upgrade to Premium in Settings for unlimited students!")
                return@launch
            }

            if (fullName.isBlank() || studentId.isBlank()) {
                onError("Full Name and Student ID are required.")
                return@launch
            }

            val newStudent = Student(
                fullName = fullName.trim(),
                studentId = studentId.trim().uppercase(),
                gender = gender,
                studentClass = studentClass,
                term = term,
                session = session
            )
            repository.insertStudent(newStudent)
            onSuccess()
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            repository.updateStudent(student)
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            // If the deleted student was selected, clear selection
            if (_selectedStudentId.value == student.id) {
                _selectedStudentId.value = null
            }
        }
    }

    // Score Management Actions
    fun saveSubjectScore(
        studentId: Int,
        subjectName: String,
        caScore: Double,
        examScore: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (subjectName.isBlank()) {
                onError("Subject Name is required.")
                return@launch
            }
            if (caScore < 0 || caScore > 40) {
                onError("Continuous Assessment (CA) score must be between 0 and 40.")
                return@launch
            }
            if (examScore < 0 || examScore > 70) {
                onError("Examination score must be between 0 and 70.")
                return@launch
            }
            if ((caScore + examScore) > 100) {
                onError("Combined score cannot exceed 100%.")
                return@launch
            }

            // Check and overwrite if subject already exists for the student to keep it clean
            val currentStudentScores = allStudentsWithScores.value.find { it.student.id == studentId }?.scores ?: emptyList()
            val existingScore = currentStudentScores.find { it.subjectName.equals(subjectName, ignoreCase = true) }
            
            val newScore = SubjectScore(
                id = existingScore?.id ?: 0,
                studentId = studentId,
                subjectName = subjectName.trim(),
                caScore = caScore,
                examScore = examScore
            )
            repository.insertScore(newScore)
            onSuccess()
        }
    }

    fun deleteScore(score: SubjectScore) {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            db.subjectScoreDao().deleteScore(score)
        }
    }

    // School Profile Settings
    fun updateSchoolSettings(
        schoolName: String,
        address: String,
        contactInfo: String,
        resultTemplate: String,
        isPremium: Boolean = schoolSettings.value.isPremium,
        logoUri: String? = schoolSettings.value.logoUri,
        immersiveMode: Boolean = schoolSettings.value.immersiveMode,
        brandColorHex: String = schoolSettings.value.brandColorHex
    ) {
        viewModelScope.launch {
            val updated = SchoolSettings(
                schoolName = schoolName.trim(),
                address = address.trim(),
                contactInfo = contactInfo.trim(),
                resultTemplate = resultTemplate,
                isPremium = isPremium,
                logoUri = logoUri,
                immersiveMode = immersiveMode,
                brandColorHex = brandColorHex,
                // keep existing template fields
                logoPlacement = schoolSettings.value.logoPlacement,
                termLabel = schoolSettings.value.termLabel,
                sessionLabel = schoolSettings.value.sessionLabel,
                studentIdLabel = schoolSettings.value.studentIdLabel,
                showHeaderAddress = schoolSettings.value.showHeaderAddress,
                showHeaderContact = schoolSettings.value.showHeaderContact,
                showSignatureLines = schoolSettings.value.showSignatureLines,
                showTeacherComments = schoolSettings.value.showTeacherComments,
                showPrincipalComments = schoolSettings.value.showPrincipalComments,
                showGradeScaleKey = schoolSettings.value.showGradeScaleKey,
                cardCornerRadius = schoolSettings.value.cardCornerRadius,
                reportDensity = schoolSettings.value.reportDensity
            )
            repository.saveSettings(updated)
        }
    }

    fun updateTemplateSettings(
        logoPlacement: String,
        termLabel: String,
        sessionLabel: String,
        studentIdLabel: String,
        showHeaderAddress: Boolean,
        showHeaderContact: Boolean,
        showSignatureLines: Boolean,
        showTeacherComments: Boolean,
        showPrincipalComments: Boolean,
        showGradeScaleKey: Boolean,
        cardCornerRadius: Int,
        reportDensity: String,
        resultTemplate: String
    ) {
        viewModelScope.launch {
            val current = schoolSettings.value
            val updated = current.copy(
                logoPlacement = logoPlacement,
                termLabel = termLabel,
                sessionLabel = sessionLabel,
                studentIdLabel = studentIdLabel,
                showHeaderAddress = showHeaderAddress,
                showHeaderContact = showHeaderContact,
                showSignatureLines = showSignatureLines,
                showTeacherComments = showTeacherComments,
                showPrincipalComments = showPrincipalComments,
                showGradeScaleKey = showGradeScaleKey,
                cardCornerRadius = cardCornerRadius,
                reportDensity = reportDensity,
                resultTemplate = resultTemplate
            )
            repository.saveSettings(updated)
        }
    }

    fun togglePremium() {
        viewModelScope.launch {
            val current = schoolSettings.value
            val updated = current.copy(isPremium = !current.isPremium)
            repository.saveSettings(updated)
        }
    }

    fun updateResultTemplate(template: String) {
        viewModelScope.launch {
            val current = schoolSettings.value
            val updated = current.copy(resultTemplate = template)
            repository.saveSettings(updated)
        }
    }

    // Bulk Import Simulation
    fun bulkImportStudents(studentsText: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val settings = schoolSettings.value
            if (!settings.isPremium) {
                onError("Bulk upload is a Premium feature. Please upgrade in Settings!")
                return@launch
            }

            val lines = studentsText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) {
                onError("No student records found to import.")
                return@launch
            }

            var successCount = 0
            lines.forEach { line ->
                // Expected format: Full Name, Student ID, Gender, Class
                val parts = line.split(",").map { it.trim() }
                if (parts.size >= 2) {
                    val name = parts[0]
                    val id = parts[1]
                    val gender = if (parts.size >= 3) parts[2] else "Male"
                    val className = if (parts.size >= 4) parts[3] else "JSS 1"
                    
                    val student = Student(
                        studentId = id.uppercase(),
                        fullName = name,
                        gender = gender,
                        studentClass = className,
                        term = "3rd Term",
                        session = "2025/2026"
                    )
                    repository.insertStudent(student)
                    successCount++
                }
            }

            if (successCount > 0) {
                onSuccess(successCount)
            } else {
                onError("Invalid format. Use: Full Name, Student ID, Gender, Class (one per line).")
            }
        }
    }

    // Flexible CSV Mapping Imports
    fun importMappedStudents(
        mappedRows: List<Map<String, String>>,
        onSuccess: (importedCount: Int, classesCreated: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (mappedRows.isEmpty()) {
                onError("No records to import.")
                return@launch
            }

            val currentStudents = allStudents.value
            val existingClassNames = allClasses.value.map { it.className.lowercase() }.toSet()
            
            var importedCount = 0
            var newClassesCount = 0
            val addedClassesInBatch = mutableSetOf<String>()

            mappedRows.forEach { row ->
                val fullName = row["fullName"]?.trim() ?: ""
                val studentId = (row["studentId"]?.trim() ?: "").ifEmpty { "STU/${(1000..9999).random()}" }
                
                if (fullName.isNotBlank()) {
                    val gender = row["gender"]?.trim()?.ifEmpty { "Male" } ?: "Male"
                    val studentClass = row["studentClass"]?.trim()?.ifEmpty { "JSS 1" } ?: "JSS 1"
                    val term = row["term"]?.trim()?.ifEmpty { "3rd Term" } ?: "3rd Term"
                    val session = row["session"]?.trim()?.ifEmpty { "2025/2026" } ?: "2025/2026"
                    val present = row["attendancePresent"]?.toIntOrNull() ?: 0
                    val totalDays = row["attendanceTotal"]?.toIntOrNull() ?: 0
                    val teacherComment = row["teacherComment"]?.trim() ?: ""
                    val principalComment = row["principalComment"]?.trim() ?: ""

                    // Check if class exists or register new class
                    val normalizedClass = studentClass.lowercase()
                    if (!existingClassNames.contains(normalizedClass) && !addedClassesInBatch.contains(normalizedClass)) {
                        repository.insertClass(SchoolClass(className = studentClass))
                        addedClassesInBatch.add(normalizedClass)
                        newClassesCount++
                    }

                    // Check if student with ID already exists
                    val existingStudent = currentStudents.find { it.studentId.equals(studentId, ignoreCase = true) }
                    val studentToSave = Student(
                        id = existingStudent?.id ?: 0,
                        studentId = studentId.uppercase(),
                        fullName = fullName,
                        gender = gender,
                        studentClass = studentClass,
                        term = term,
                        session = session,
                        attendancePresent = present,
                        attendanceTotal = totalDays,
                        teacherComment = teacherComment,
                        principalComment = principalComment
                    )

                    repository.insertStudent(studentToSave)
                    importedCount++
                }
            }

            if (importedCount > 0) {
                onSuccess(importedCount, newClassesCount)
            } else {
                onError("No valid student names found in mapped columns.")
            }
        }
    }

    fun importMappedSubjects(
        mappedRows: List<Map<String, String>>,
        onSuccess: (importedCount: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (mappedRows.isEmpty()) {
                onError("No records to import.")
                return@launch
            }

            val currentSubjects = allSubjects.value
            var importedCount = 0

            mappedRows.forEach { row ->
                val name = row["name"]?.trim() ?: ""
                if (name.isNotBlank()) {
                    val category = row["category"]?.trim()?.ifEmpty { "Core" } ?: "Core"
                    val classLevel = row["classLevel"]?.trim()?.ifEmpty { "All Classes" } ?: "All Classes"
                    val description = row["description"]?.trim() ?: ""

                    val existingSubject = currentSubjects.find { it.name.equals(name, ignoreCase = true) }
                    val subjectToSave = Subject(
                        id = existingSubject?.id ?: 0,
                        name = name,
                        category = category,
                        classLevel = classLevel,
                        description = description
                    )

                    repository.insertSubject(subjectToSave)
                    importedCount++
                }
            }

            if (importedCount > 0) {
                onSuccess(importedCount)
            } else {
                onError("No valid subject names found in mapped columns.")
            }
        }
    }

    fun importMappedScores(
        mappedRows: List<Map<String, String>>,
        onSuccess: (studentsProcessed: Int, scoresAdded: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (mappedRows.isEmpty()) {
                onError("No records to import.")
                return@launch
            }

            var currentStudentsList = allStudents.value
            var scoresAdded = 0
            val processedStudents = mutableSetOf<Int>()

            mappedRows.forEach { row ->
                val studentId = row["studentId"]?.trim() ?: ""
                val fullName = row["fullName"]?.trim() ?: ""
                val studentClass = row["studentClass"]?.trim()?.ifEmpty { "JSS 1" } ?: "JSS 1"
                val subjectName = row["subjectName"]?.trim() ?: ""
                val caScore = row["caScore"]?.toDoubleOrNull() ?: 0.0
                val examScore = row["examScore"]?.toDoubleOrNull() ?: 0.0

                if (subjectName.isNotBlank() && (studentId.isNotBlank() || fullName.isNotBlank())) {
                    // Match existing student or create new one
                    var targetStudent = currentStudentsList.find { 
                        (studentId.isNotEmpty() && it.studentId.equals(studentId, ignoreCase = true)) ||
                        (fullName.isNotEmpty() && it.fullName.equals(fullName, ignoreCase = true))
                    }

                    if (targetStudent == null) {
                        val newId = if (studentId.isNotEmpty()) studentId.uppercase() else "STU/${(1000..9999).random()}"
                        val newName = if (fullName.isNotEmpty()) fullName else "Student $newId"
                        val newStudent = Student(
                            studentId = newId,
                            fullName = newName,
                            gender = "Male",
                            studentClass = studentClass,
                            term = "3rd Term",
                            session = "2025/2026"
                        )
                        val insertedId = repository.insertStudent(newStudent)
                        targetStudent = newStudent.copy(id = insertedId.toInt())
                        // refresh local tracking list
                        currentStudentsList = allStudents.value
                    }

                    if (targetStudent.id > 0) {
                        val clampedCa = caScore.coerceIn(0.0, 40.0)
                        val clampedExam = examScore.coerceIn(0.0, 70.0)

                        val newScore = SubjectScore(
                            studentId = targetStudent.id,
                            subjectName = subjectName,
                            caScore = clampedCa,
                            examScore = clampedExam
                        )
                        repository.insertScore(newScore)
                        scoresAdded++
                        processedStudents.add(targetStudent.id)
                    }
                }
            }

            if (scoresAdded > 0) {
                onSuccess(processedStudents.size, scoresAdded)
            } else {
                onError("No valid scores or subject names found in mapped columns.")
            }
        }
    }

    fun clearAllStudents() {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            db.studentDao().deleteAllStudents()
        }
    }

    // Position Calculations
    fun getStudentPosition(student: Student, list: List<StudentWithScores>): String {
        val sameClassList = list.filter {
            it.student.studentClass.equals(student.studentClass, ignoreCase = true) &&
            it.student.term.equals(student.term, ignoreCase = true) &&
            it.student.session.equals(student.session, ignoreCase = true)
        }

        if (sameClassList.isEmpty()) return "1st"

        // Sort by average score descending
        val sortedList = sameClassList.sortedByDescending { it.averageScore }
        val index = sortedList.indexOfFirst { it.student.id == student.id }
        
        val position = if (index != -1) index + 1 else 1
        return "${formatOrdinal(position)} out of ${sameClassList.size}"
    }

    private fun formatOrdinal(number: Int): String {
        val remainder10 = number % 10
        val remainder100 = number % 100
        return when {
            remainder10 == 1 && remainder100 != 11 -> "${number}st"
            remainder10 == 2 && remainder100 != 12 -> "${number}nd"
            remainder10 == 3 && remainder100 != 13 -> "${number}rd"
            else -> "${number}th"
        }
    }



    fun exportResultAsPdf(context: Context, record: StudentWithScores) {
        val settings = schoolSettings.value
        if (!settings.isPremium) {
            Toast.makeText(context, "Exporting to PDF is a Premium feature. Please upgrade in Settings!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val student = record.student
            val position = getStudentPosition(student, allStudentsWithScores.value)

            val pdfDocument = android.graphics.pdf.PdfDocument()
            // A4 page: 595 x 842 points (72 points/inch)
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = android.graphics.Paint()
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 12f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            }

            if (settings.resultTemplate == "Delma Classic") {
                drawDelmaClassicPdf(canvas, student, record, position, settings, paint, textPaint)
            } else {
                // Draw Background and border
                paint.color = android.graphics.Color.WHITE
                canvas.drawRect(0f, 0f, 595f, 842f, paint)

            // Draw a double border (Green & Gold)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = 0xFF0D5C34.toInt() // School Green
            canvas.drawRect(15f, 15f, 580f, 827f, paint)

            paint.strokeWidth = 1f
            paint.color = 0xFFD4AF37.toInt() // School Gold
            canvas.drawRect(19f, 19f, 576f, 823f, paint)

            paint.style = android.graphics.Paint.Style.FILL

            // Draw Header Banner
            paint.color = 0xFF0D5C34.toInt() // School Green background for title banner
            canvas.drawRect(25f, 25f, 570f, 100f, paint)

            // School Name
            textPaint.color = android.graphics.Color.WHITE
            textPaint.textSize = 18f
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            val schoolTitle = settings.schoolName.uppercase()
            val schoolTitleWidth = textPaint.measureText(schoolTitle)
            canvas.drawText(schoolTitle, (595f - schoolTitleWidth) / 2f, 55f, textPaint)

            // School details
            textPaint.textSize = 9f
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
            val subTextParts = mutableListOf<String>()
            if (settings.showHeaderAddress) subTextParts.add(settings.address)
            if (settings.showHeaderContact) subTextParts.add(settings.contactInfo)
            val subText = subTextParts.joinToString("  |  ")
            if (subText.isNotEmpty()) {
                val subTextWidth = textPaint.measureText(subText)
                canvas.drawText(subText, (595f - subTextWidth) / 2f, 75f, textPaint)
            }

            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)

            // Draw Document Type header
            paint.color = 0xFFD4AF37.toInt() // School Gold
            canvas.drawRect(25f, 110f, 570f, 130f, paint)

            textPaint.color = android.graphics.Color.BLACK
            textPaint.textSize = 10f
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            val docTitle = "OFFICIAL ACADEMIC PROGRESS LEDGER SHEET — REPORT CARD"
            val docTitleWidth = textPaint.measureText(docTitle)
            canvas.drawText(docTitle, (595f - docTitleWidth) / 2f, 124f, textPaint)

            // Draw Meta Info
            textPaint.textSize = 10f
            textPaint.color = android.graphics.Color.BLACK

            // Left column
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("STUDENT NAME:", 40f, 160f, textPaint)
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            canvas.drawText(student.fullName, 170f, 160f, textPaint)

            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("${settings.studentIdLabel.uppercase()}:", 40f, 180f, textPaint)
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            canvas.drawText(student.studentId, 170f, 180f, textPaint)

            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("CLASS PROFILE:", 40f, 200f, textPaint)
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            canvas.drawText(student.studentClass, 170f, 200f, textPaint)

            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("GENDER:", 40f, 220f, textPaint)
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            canvas.drawText(student.gender, 170f, 220f, textPaint)

            // Right column
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("${settings.termLabel.uppercase()}:", 320f, 160f, textPaint)
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            canvas.drawText(student.term, 450f, 160f, textPaint)

            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("${settings.sessionLabel.uppercase()}:", 320f, 180f, textPaint)
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            canvas.drawText(student.session, 450f, 180f, textPaint)

            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("CLASS POSITION:", 320f, 200f, textPaint)
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            canvas.drawText(position, 440f, 200f, textPaint)

            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("RESULT STATUS:", 320f, 220f, textPaint)
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            val statusColor = if (record.averageScore >= 40.0) 0xFF0D5C34.toInt() else android.graphics.Color.RED
            textPaint.color = statusColor
            canvas.drawText(if (record.averageScore >= 40.0) "PASSED" else "FAILED/RETAKE", 440f, 220f, textPaint)
            textPaint.color = android.graphics.Color.BLACK

            // Results Table Grid
            val tableY = 250f
            paint.color = 0xFF0D5C34.toInt() // Green header
            canvas.drawRect(30f, tableY, 565f, tableY + 25f, paint)

            textPaint.color = android.graphics.Color.WHITE
            textPaint.textSize = 10f
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)

            canvas.drawText("SUBJECT NAME", 40f, tableY + 16f, textPaint)
            canvas.drawText("CA (40)", 250f, tableY + 16f, textPaint)
            canvas.drawText("EXAM (60)", 320f, tableY + 16f, textPaint)
            canvas.drawText("TOTAL (100)", 390f, tableY + 16f, textPaint)
            canvas.drawText("GRADE", 470f, tableY + 16f, textPaint)
            canvas.drawText("REMARK", 515f, tableY + 16f, textPaint)

            textPaint.color = android.graphics.Color.BLACK
            var currentY = tableY + 25f

            record.scores.forEachIndexed { index, score ->
                // Draw light line backgrounds for alternating rows
                if (index % 2 == 1) {
                    paint.color = 0xFFF2F5F3.toInt()
                    canvas.drawRect(30f, currentY, 565f, currentY + 22f, paint)
                }

                // Grid cell borders
                paint.color = 0xFFCCCCCC.toInt()
                paint.style = android.graphics.Paint.Style.STROKE
                canvas.drawRect(30f, currentY, 565f, currentY + 22f, paint)
                paint.style = android.graphics.Paint.Style.FILL

                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(score.subjectName, 40f, currentY + 14f, textPaint)
                canvas.drawText(score.caScore.toString(), 250f, currentY + 14f, textPaint)
                canvas.drawText(score.examScore.toString(), 320f, currentY + 14f, textPaint)
                
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText(score.totalScore.toString(), 390f, currentY + 14f, textPaint)
                
                // Highlight F grade in red
                if (score.grade == "F") {
                    textPaint.color = android.graphics.Color.RED
                } else {
                    textPaint.color = 0xFF0D5C34.toInt()
                }
                canvas.drawText(score.grade, 470f, currentY + 14f, textPaint)
                textPaint.color = android.graphics.Color.BLACK

                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(score.remark, 515f, currentY + 14f, textPaint)

                currentY += 22f
            }

            // Draw summary total card
            currentY += 15f
            paint.color = 0xFFF2F5F3.toInt()
            canvas.drawRect(30f, currentY, 565f, currentY + 50f, paint)
            paint.color = 0xFFD4AF37.toInt()
            paint.style = android.graphics.Paint.Style.STROKE
            canvas.drawRect(30f, currentY, 565f, currentY + 50f, paint)
            paint.style = android.graphics.Paint.Style.FILL

            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("SUMMARY STATS:", 40f, currentY + 18f, textPaint)
            
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            canvas.drawText("Cumulative Score: ${String.format(Locale.US, "%.1f", record.totalScore)}", 40f, currentY + 36f, textPaint)
            canvas.drawText("Terminal Percentage Average: ${String.format(Locale.US, "%.2f%%", record.averageScore)}", 220f, currentY + 36f, textPaint)

            // Draw remarks block
            if (settings.showTeacherComments) {
                currentY += 65f
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("CLASS TEACHER REMARKS & APPRAISAL:", 30f, currentY, textPaint)
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(student.teacherComment.ifBlank { "A satisfactory terminal performance. Shows good potential." }, 30f, currentY + 16f, textPaint)
            }

            if (settings.showPrincipalComments) {
                currentY += 40f
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("PRINCIPAL ENDORSEMENT REMARKS:", 30f, currentY, textPaint)
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(student.principalComment.ifBlank { "Commendable outcome. Promoted successfully." }, 30f, currentY + 16f, textPaint)
            }

            // Dynamic Grading Scale Key in PDF
            if (settings.showGradeScaleKey) {
                currentY += 35f
                textPaint.textSize = 8f
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("OFFICIAL SCALING KEY:", 30f, currentY, textPaint)
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText("A (>=70): Excellent | B (60-69): Very Good | C (50-59): Good | D (45-49): Pass | E (40-44): Fair | F (<40): Fail", 30f, currentY + 12f, textPaint)
            }

            // Draw signature / stamps section
            if (settings.showSignatureLines) {
                currentY += 60f
                paint.color = android.graphics.Color.DKGRAY
                paint.strokeWidth = 0.5f
                canvas.drawLine(40f, currentY, 180f, currentY, paint)
                canvas.drawLine(380f, currentY, 520f, currentY, paint)

                textPaint.textSize = 8f
                canvas.drawText("Class Teacher Signature", 60f, currentY + 12f, textPaint)
                canvas.drawText("Principal Signature & Stamp", 390f, currentY + 12f, textPaint)

                // Draw dynamic school stamp/seal watermark on PDF
                paint.color = 0xFFD4AF37.toInt()
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawCircle(470f, currentY - 20f, 25f, paint)
                textPaint.textSize = 6f
                textPaint.color = 0xFFD4AF37.toInt()
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("APPROVED", 455f, currentY - 20f, textPaint)
                canvas.drawText("SMARTRESULT", 450f, currentY - 12f, textPaint)
            }
            } // end of else

            pdfDocument.finishPage(page)

            // Save PDF
            val file = File(context.cacheDir, "Result_${student.fullName.replace(" ", "_")}.pdf")
            pdfDocument.writeTo(file.outputStream())
            pdfDocument.close()

            // Share PDF
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "Academic Progress Report PDF - ${student.fullName}")
                putExtra(Intent.EXTRA_TEXT, "Kindly find attached the official academic report card PDF for ${student.fullName}.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report Card PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportAllToPdf(context: Context) {
        val settings = schoolSettings.value
        if (!settings.isPremium) {
            Toast.makeText(context, "Bulk Export to PDF is a Premium feature. Please upgrade in Settings!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val records = allStudentsWithScores.value
            if (records.isEmpty()) {
                Toast.makeText(context, "No student records to export.", Toast.LENGTH_SHORT).show()
                return
            }

            val pdfDocument = android.graphics.pdf.PdfDocument()
            val paint = android.graphics.Paint()
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 12f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            }

            records.forEachIndexed { index, record ->
                val student = record.student
                val position = getStudentPosition(student, records)
                
                // Establish unique page for each student
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                if (settings.resultTemplate == "Delma Classic") {
                    drawDelmaClassicPdf(canvas, student, record, position, settings, paint, textPaint)
                } else {
                    // Draw Background and border
                    paint.color = android.graphics.Color.WHITE
                    canvas.drawRect(0f, 0f, 595f, 842f, paint)

                    // Draw a double border (Green & Gold)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    paint.color = 0xFF0D5C34.toInt() // School Green
                    canvas.drawRect(15f, 15f, 580f, 827f, paint)

                    paint.strokeWidth = 1f
                    paint.color = 0xFFD4AF37.toInt() // School Gold
                    canvas.drawRect(19f, 19f, 576f, 823f, paint)

                    paint.style = android.graphics.Paint.Style.FILL

                    // Draw Header Banner
                    paint.color = 0xFF0D5C34.toInt() // School Green background for title banner
                    canvas.drawRect(25f, 25f, 570f, 100f, paint)

                    // School Name
                    textPaint.color = android.graphics.Color.WHITE
                    textPaint.textSize = 18f
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    val schoolTitle = settings.schoolName.uppercase()
                    val schoolTitleWidth = textPaint.measureText(schoolTitle)
                    canvas.drawText(schoolTitle, (595f - schoolTitleWidth) / 2f, 55f, textPaint)

                    // School details
                    textPaint.textSize = 9f
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                    val subTextParts = mutableListOf<String>()
                    if (settings.showHeaderAddress) subTextParts.add(settings.address)
                    if (settings.showHeaderContact) subTextParts.add(settings.contactInfo)
                    val subText = subTextParts.joinToString("  |  ")
                    if (subText.isNotEmpty()) {
                        val subTextWidth = textPaint.measureText(subText)
                        canvas.drawText(subText, (595f - subTextWidth) / 2f, 75f, textPaint)
                    }

                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)

                    // Draw Document Type header
                    paint.color = 0xFFD4AF37.toInt() // School Gold
                    canvas.drawRect(25f, 110f, 570f, 130f, paint)

                    textPaint.color = android.graphics.Color.BLACK
                    textPaint.textSize = 10f
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    val docTitle = "OFFICIAL ACADEMIC PROGRESS REPORT CARD"
                    val docTitleWidth = textPaint.measureText(docTitle)
                    canvas.drawText(docTitle, (595f - docTitleWidth) / 2f, 124f, textPaint)

                    // Draw Meta Info
                    textPaint.textSize = 10f
                    textPaint.color = android.graphics.Color.BLACK

                    // Left column
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText("STUDENT NAME:", 40f, 160f, textPaint)
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    canvas.drawText(student.fullName, 170f, 160f, textPaint)

                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText("${settings.studentIdLabel.uppercase()}:", 40f, 180f, textPaint)
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    canvas.drawText(student.studentId, 170f, 180f, textPaint)

                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText("CLASS PROFILE:", 40f, 200f, textPaint)
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    canvas.drawText(student.studentClass, 170f, 200f, textPaint)

                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText("GENDER:", 40f, 220f, textPaint)
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    canvas.drawText(student.gender, 170f, 220f, textPaint)

                    // Right column
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText("${settings.termLabel.uppercase()}:", 320f, 160f, textPaint)
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    canvas.drawText(student.term, 450f, 160f, textPaint)

                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText("${settings.sessionLabel.uppercase()}:", 320f, 180f, textPaint)
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    canvas.drawText(student.session, 450f, 180f, textPaint)

                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText("CLASS POSITION:", 320f, 200f, textPaint)
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    canvas.drawText(position, 440f, 200f, textPaint)

                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText("RESULT STATUS:", 320f, 220f, textPaint)
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    val statusColor = if (record.averageScore >= 40.0) 0xFF0D5C34.toInt() else android.graphics.Color.RED
                    textPaint.color = statusColor
                    canvas.drawText(if (record.averageScore >= 40.0) "PASSED" else "FAILED/RETAKE", 440f, 220f, textPaint)
                    textPaint.color = android.graphics.Color.BLACK

                    // Results Table Grid
                    val tableY = 250f
                    paint.color = 0xFF0D5C34.toInt() // Green header
                    canvas.drawRect(30f, tableY, 565f, tableY + 25f, paint)

                    textPaint.color = android.graphics.Color.WHITE
                    textPaint.textSize = 10f
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)

                    canvas.drawText("SUBJECT NAME", 40f, tableY + 16f, textPaint)
                    canvas.drawText("CA (40)", 250f, tableY + 16f, textPaint)
                    canvas.drawText("EXAM (60)", 320f, tableY + 16f, textPaint)
                    canvas.drawText("TOTAL (100)", 390f, tableY + 16f, textPaint)
                    canvas.drawText("GRADE", 470f, tableY + 16f, textPaint)
                    canvas.drawText("REMARK", 515f, tableY + 16f, textPaint)

                    textPaint.color = android.graphics.Color.BLACK
                    var currentY = tableY + 25f

                    record.scores.forEachIndexed { scoreIdx, score ->
                        if (scoreIdx % 2 == 1) {
                            paint.color = 0xFFF2F5F3.toInt()
                            canvas.drawRect(30f, currentY, 565f, currentY + 22f, paint)
                        }

                        paint.color = 0xFFCCCCCC.toInt()
                        paint.style = android.graphics.Paint.Style.STROKE
                        canvas.drawRect(30f, currentY, 565f, currentY + 22f, paint)
                        paint.style = android.graphics.Paint.Style.FILL

                        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                        canvas.drawText(score.subjectName, 40f, currentY + 14f, textPaint)
                        canvas.drawText(score.caScore.toString(), 250f, currentY + 14f, textPaint)
                        canvas.drawText(score.examScore.toString(), 320f, currentY + 14f, textPaint)
                        
                        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        canvas.drawText(score.totalScore.toString(), 390f, currentY + 14f, textPaint)
                        
                        if (score.grade == "F") {
                            textPaint.color = android.graphics.Color.RED
                        } else {
                            textPaint.color = 0xFF0D5C34.toInt()
                        }
                        canvas.drawText(score.grade, 470f, currentY + 14f, textPaint)
                        textPaint.color = android.graphics.Color.BLACK

                        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                        canvas.drawText(score.remark, 515f, currentY + 14f, textPaint)

                        currentY += 22f
                    }

                    // Bottom Averages Section
                    currentY += 15f
                    paint.color = 0xFFCCCCCC.toInt()
                    paint.strokeWidth = 1f
                    canvas.drawLine(30f, currentY, 565f, currentY, paint)

                    currentY += 15f
                    textPaint.textSize = 10f
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText("CUMULATIVE SCORE: ${String.format(Locale.US, "%.1f", record.totalScore)}", 40f, currentY, textPaint)
                    canvas.drawText("TERM AVERAGE PERCENT: ${String.format(Locale.US, "%.2f%%", record.averageScore)}", 300f, currentY, textPaint)

                    // Comments segment
                    if (settings.showTeacherComments || settings.showPrincipalComments) {
                        currentY += 20f
                        paint.color = 0xFFF2F2F2.toInt()
                        paint.style = android.graphics.Paint.Style.FILL
                        canvas.drawRect(30f, currentY, 565f, currentY + 70f, paint)

                        paint.color = 0xFFCCCCCC.toInt()
                        paint.style = android.graphics.Paint.Style.STROKE
                        canvas.drawRect(30f, currentY, 565f, currentY + 70f, paint)
                        paint.style = android.graphics.Paint.Style.FILL

                        if (settings.showTeacherComments) {
                            textPaint.color = 0xFF0D5C34.toInt()
                            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            canvas.drawText("CLASS TEACHER'S ENDORSEMENT", 40f, currentY + 15f, textPaint)
                            textPaint.color = android.graphics.Color.DKGRAY
                            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                            canvas.drawText(student.teacherComment.ifBlank { "A satisfactory performance." }, 40f, currentY + 30f, textPaint)
                        }

                        if (settings.showPrincipalComments) {
                            textPaint.color = 0xFF1A365D.toInt()
                            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            canvas.drawText("PRINCIPAL'S REGULATORY STAMP & COMMENT", 40f, currentY + 50f, textPaint)
                            textPaint.color = android.graphics.Color.DKGRAY
                            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                            canvas.drawText(student.principalComment.ifBlank { "Promoted / Approved." }, 40f, currentY + 62f, textPaint)
                        }
                    }

                    // Dynamic Grading Scale Key in Bulk PDF
                    if (settings.showGradeScaleKey) {
                        currentY += 80f
                        textPaint.textSize = 8f
                        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        textPaint.color = android.graphics.Color.BLACK
                        canvas.drawText("OFFICIAL SCALING KEY:", 30f, currentY, textPaint)
                        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                        canvas.drawText("A (>=70): Excellent | B (60-69): Very Good | C (50-59): Good | D (45-49): Pass | E (40-44): Fair | F (<40): Fail", 30f, currentY + 12f, textPaint)
                    }

                    // Signatures
                    if (settings.showSignatureLines) {
                        currentY += 60f
                        paint.color = android.graphics.Color.DKGRAY
                        paint.strokeWidth = 1f
                        canvas.drawLine(50f, currentY, 190f, currentY, paint)
                        canvas.drawLine(380f, currentY, 520f, currentY, paint)

                        textPaint.color = android.graphics.Color.BLACK
                        textPaint.textSize = 8f
                        canvas.drawText("Class Teacher Signature", 70f, currentY + 12f, textPaint)
                        canvas.drawText("Principal Seal & Stamp", 405f, currentY + 12f, textPaint)

                        // Watermark Seal
                        textPaint.color = 0xCCD03030.toInt()
                        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        canvas.drawText("APPROVED", 455f, currentY - 20f, textPaint)
                        canvas.drawText("SMARTRESULT", 450f, currentY - 12f, textPaint)
                    }
                }

                pdfDocument.finishPage(page)
            }

            // Save PDF
            val file = File(context.cacheDir, "SmartResult_All_Students.pdf")
            pdfDocument.writeTo(file.outputStream())
            pdfDocument.close()

            // Share PDF
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "SmartResult NG - Bulk Report Cards PDF")
                putExtra(Intent.EXTRA_TEXT, "Kindly find attached the compiled official report cards PDF for all students.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Bulk Report Cards PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting Bulk PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportFilteredToPdf(context: Context, records: List<StudentWithScores>, filterTitle: String = "Filtered") {
        val settings = schoolSettings.value
        if (!settings.isPremium) {
            Toast.makeText(context, "Bulk Export to PDF is a Premium feature. Please upgrade in Settings!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            if (records.isEmpty()) {
                Toast.makeText(context, "No student records match the selected filters to export.", Toast.LENGTH_SHORT).show()
                return
            }

            val pdfDocument = android.graphics.pdf.PdfDocument()
            val paint = android.graphics.Paint()
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 12f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            }

            records.forEachIndexed { index, record ->
                val student = record.student
                val position = getStudentPosition(student, allStudentsWithScores.value)
                
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                paint.color = android.graphics.Color.WHITE
                paint.style = android.graphics.Paint.Style.FILL
                canvas.drawRect(0f, 0f, 595f, 842f, paint)

                // Header Banner
                val primaryColor = try {
                    settings.brandColorHex.toColorInt()
                } catch (e: Exception) {
                    0xFF0D5C34.toInt()
                }
                paint.color = primaryColor
                canvas.drawRect(20f, 20f, 575f, 95f, paint)

                textPaint.color = android.graphics.Color.WHITE
                textPaint.textSize = 16f
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                val schoolNameWidth = textPaint.measureText(settings.schoolName.uppercase())
                canvas.drawText(settings.schoolName.uppercase(), (595f - schoolNameWidth) / 2f, 50f, textPaint)

                // Address & Contact
                textPaint.textSize = 9f
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                val subTextParts = mutableListOf<String>()
                if (settings.showHeaderAddress) subTextParts.add(settings.address)
                if (settings.showHeaderContact) subTextParts.add(settings.contactInfo)
                val subText = subTextParts.joinToString("  |  ")
                if (subText.isNotEmpty()) {
                    val subTextWidth = textPaint.measureText(subText)
                    canvas.drawText(subText, (595f - subTextWidth) / 2f, 75f, textPaint)
                }

                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)

                // Header box border
                paint.color = primaryColor
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 2f
                canvas.drawRect(20f, 20f, 575f, 95f, paint)
                paint.style = android.graphics.Paint.Style.FILL

                // Document Title
                textPaint.color = android.graphics.Color.BLACK
                textPaint.textSize = 10f
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                val docTitle = "OFFICIAL ACADEMIC REPORT CARD — ${filterTitle.uppercase()}"
                val docTitleWidth = textPaint.measureText(docTitle)
                canvas.drawText(docTitle, (595f - docTitleWidth) / 2f, 124f, textPaint)

                // Student Profile Box
                paint.color = 0xFFF9FAFB.toInt()
                canvas.drawRect(20f, 138f, 575f, 245f, paint)
                paint.color = 0xFFE5E7EB.toInt()
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRect(20f, 138f, 575f, 245f, paint)
                paint.style = android.graphics.Paint.Style.FILL

                // Left Column Info
                textPaint.color = android.graphics.Color.BLACK
                textPaint.textSize = 9f
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("STUDENT NAME:", 40f, 160f, textPaint)
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(student.fullName, 170f, 160f, textPaint)

                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("${settings.studentIdLabel.uppercase()}:", 40f, 180f, textPaint)
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(student.studentId, 170f, 180f, textPaint)

                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("CLASS PROFILE:", 40f, 200f, textPaint)
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(student.studentClass, 170f, 200f, textPaint)

                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("GENDER:", 40f, 220f, textPaint)
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(student.gender, 170f, 220f, textPaint)

                // Right Column Info
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("${settings.termLabel.uppercase()}:", 320f, 160f, textPaint)
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(student.term, 450f, 160f, textPaint)

                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("${settings.sessionLabel.uppercase()}:", 320f, 180f, textPaint)
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(student.session, 450f, 180f, textPaint)

                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("CLASS POSITION:", 320f, 200f, textPaint)
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textPaint.color = 0xFF0D5C34.toInt()
                canvas.drawText(position, 450f, 200f, textPaint)

                // Academic Table Header
                var currentY = 265f
                paint.color = primaryColor
                canvas.drawRect(20f, currentY, 575f, currentY + 22f, paint)

                textPaint.color = android.graphics.Color.WHITE
                textPaint.textSize = 8.5f
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("SUBJECT", 35f, currentY + 14f, textPaint)
                canvas.drawText("CA SCORE (40)", 240f, currentY + 14f, textPaint)
                canvas.drawText("EXAM (60)", 350f, currentY + 14f, textPaint)
                canvas.drawText("TOTAL (100)", 440f, currentY + 14f, textPaint)
                canvas.drawText("GRADE", 520f, currentY + 14f, textPaint)

                currentY += 22f

                // Subject Rows
                val scores = record.scores
                scores.forEachIndexed { scoreIndex, score ->
                    if (scoreIndex % 2 == 0) {
                        paint.color = 0xFFF9FAFB.toInt()
                        canvas.drawRect(20f, currentY, 575f, currentY + 18f, paint)
                    }

                    textPaint.color = android.graphics.Color.BLACK
                    textPaint.textSize = 8f
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    canvas.drawText(score.subjectName, 35f, currentY + 13f, textPaint)
                    canvas.drawText(score.caScore.toString(), 255f, currentY + 13f, textPaint)
                    canvas.drawText(score.examScore.toString(), 365f, currentY + 13f, textPaint)

                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText(score.totalScore.toString(), 455f, currentY + 13f, textPaint)
                    canvas.drawText(score.grade, 528f, currentY + 13f, textPaint)

                    currentY += 18f
                }

                // Table outer border
                paint.color = primaryColor
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRect(20f, 265f, 575f, currentY, paint)
                paint.style = android.graphics.Paint.Style.FILL

                // Summary stats row
                currentY += 12f
                paint.color = 0xFFF3F4F6.toInt()
                canvas.drawRect(20f, currentY, 575f, currentY + 45f, paint)
                paint.color = 0xFFD1D5DB.toInt()
                paint.style = android.graphics.Paint.Style.STROKE
                canvas.drawRect(20f, currentY, 575f, currentY + 45f, paint)
                paint.style = android.graphics.Paint.Style.FILL

                textPaint.color = android.graphics.Color.BLACK
                textPaint.textSize = 9f
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                val overallGrade = getGrade(record.averageScore)
                canvas.drawText("CUMULATIVE SCORE: ${record.totalScore}", 30f, currentY + 18f, textPaint)
                canvas.drawText("OBTAINABLE MAX: ${scores.size * 100}", 220f, currentY + 18f, textPaint)
                canvas.drawText("OVERALL GRADE: $overallGrade", 420f, currentY + 18f, textPaint)
                canvas.drawText("PERCENTAGE AVERAGE: ${String.format(Locale.US, "%.2f%%", record.averageScore)}", 30f, currentY + 36f, textPaint)

                // Comments
                if (settings.showTeacherComments || settings.showPrincipalComments) {
                    currentY += 55f
                    paint.color = 0xFFF9FAFB.toInt()
                    canvas.drawRect(20f, currentY, 575f, currentY + 65f, paint)
                    paint.color = 0xFFE5E7EB.toInt()
                    paint.style = android.graphics.Paint.Style.STROKE
                    canvas.drawRect(20f, currentY, 575f, currentY + 65f, paint)
                    paint.style = android.graphics.Paint.Style.FILL

                    if (settings.showTeacherComments) {
                        textPaint.color = 0xFF0D5C34.toInt()
                        textPaint.textSize = 8.5f
                        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        canvas.drawText("CLASS TEACHER'S APPRAISAL", 30f, currentY + 15f, textPaint)
                        textPaint.color = android.graphics.Color.DKGRAY
                        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                        canvas.drawText(student.teacherComment.ifBlank { "Satisfactory performance." }, 30f, currentY + 28f, textPaint)
                    }

                    if (settings.showPrincipalComments) {
                        textPaint.color = 0xFF1A365D.toInt()
                        textPaint.textSize = 8.5f
                        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        canvas.drawText("PRINCIPAL'S ENDORSEMENT", 30f, currentY + 45f, textPaint)
                        textPaint.color = android.graphics.Color.DKGRAY
                        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                        canvas.drawText(student.principalComment.ifBlank { "Promoted / Approved." }, 30f, currentY + 58f, textPaint)
                    }
                }

                // Grading key scale
                if (settings.showGradeScaleKey) {
                    currentY += 75f
                    textPaint.textSize = 7.5f
                    textPaint.color = android.graphics.Color.BLACK
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText("OFFICIAL SCALING KEY:", 20f, currentY, textPaint)
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                    canvas.drawText("A (>=70): Excellent | B (60-69): Very Good | C (50-59): Good | D (45-49): Pass | E (40-44): Fair | F (<40): Fail", 20f, currentY + 12f, textPaint)
                }

                // Signatures
                if (settings.showSignatureLines) {
                    currentY += 50f
                    paint.color = android.graphics.Color.DKGRAY
                    paint.strokeWidth = 1f
                    canvas.drawLine(40f, currentY, 180f, currentY, paint)
                    canvas.drawLine(380f, currentY, 520f, currentY, paint)

                    textPaint.color = android.graphics.Color.BLACK
                    textPaint.textSize = 8f
                    canvas.drawText("Class Teacher Signature", 60f, currentY + 12f, textPaint)
                    canvas.drawText("Principal Seal & Stamp", 405f, currentY + 12f, textPaint)

                    textPaint.color = 0xCCD03030.toInt()
                    textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    canvas.drawText("APPROVED", 455f, currentY - 20f, textPaint)
                    canvas.drawText("SMARTRESULT", 450f, currentY - 12f, textPaint)
                }

                pdfDocument.finishPage(page)
            }

            // Save PDF
            val cleanTitle = filterTitle.replace(" ", "_").replace("/", "-")
            val fileName = "SmartResult_${cleanTitle}.pdf"
            val file = File(context.cacheDir, fileName)
            pdfDocument.writeTo(file.outputStream())
            pdfDocument.close()

            // Share PDF
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "SmartResult - $filterTitle Results PDF")
                putExtra(Intent.EXTRA_TEXT, "Attached is the compiled $filterTitle student results PDF (${records.size} student reports).")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share $filterTitle PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportFilteredToCsv(context: Context, records: List<StudentWithScores>, filterTitle: String = "Filtered") {
        try {
            if (records.isEmpty()) {
                Toast.makeText(context, "No student records match the selected filters to export.", Toast.LENGTH_SHORT).show()
                return
            }

            val cleanTitle = filterTitle.replace(" ", "_").replace("/", "-")
            val file = File(context.cacheDir, "SmartResult_${cleanTitle}_Summary.csv")
            val writer = FileWriter(file)

            writer.append("Student ID,Name,Class,Term,Gender,Total Score,Average %,Grade,Position\n")
            records.forEach { record ->
                val s = record.student
                val pos = getStudentPosition(s, allStudentsWithScores.value)
                val grade = getGrade(record.averageScore)
                writer.append("${s.studentId},\"${s.fullName}\",${s.studentClass},${s.term},${s.gender},${record.totalScore},${String.format(Locale.US, "%.2f", record.averageScore)},$grade,$pos\n")
            }
            writer.flush()
            writer.close()

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "SmartResult - $filterTitle CSV Summary")
                putExtra(Intent.EXTRA_TEXT, "Attached is the CSV export summary for $filterTitle (${records.size} students).")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share $filterTitle CSV"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // --- CLOUD FIRESTORE SYNCHRONIZATION AND SCHEMAS ---
    private val _syncState = MutableStateFlow("IDLE") // IDLE, CONNECTING, UPLOADING, COMPLETE, ERROR
    val syncState: StateFlow<String> = _syncState.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private val _pullState = MutableStateFlow("IDLE") // IDLE, CONNECTING, DOWNLOADING, COMPLETE, ERROR
    val pullState: StateFlow<String> = _pullState.asStateFlow()

    private val _pullLogs = MutableStateFlow<List<String>>(emptyList())
    val pullLogs: StateFlow<List<String>> = _pullLogs.asStateFlow()

    fun updateFirestoreConfig(apiKey: String, projectId: String, appId: String) {
        viewModelScope.launch {
            val current = schoolSettings.value
            val updated = current.copy(
                firestoreApiKey = apiKey.trim(),
                firestoreProjectId = projectId.trim(),
                firestoreAppId = appId.trim()
            )
            repository.saveSettings(updated)
        }
    }

    private fun addLog(msg: String) {
        val current = _syncLogs.value.toMutableList()
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        current.add("[$timestamp] $msg")
        _syncLogs.value = current
    }

    private fun addPullLog(msg: String) {
        val current = _pullLogs.value.toMutableList()
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        current.add("[$timestamp] $msg")
        _pullLogs.value = current
    }

    private fun clearPullLogs() {
        _pullLogs.value = emptyList()
    }

    private fun clearLogs() {
        _syncLogs.value = emptyList()
    }

    fun runFirestoreSync(context: Context, isSimulation: Boolean) {
        viewModelScope.launch {
            clearLogs()
            _syncState.value = "CONNECTING"
            addLog("Initializing Cloud Database sync stream...")
            
            val settings = schoolSettings.value
            val studentsData = allStudentsWithScores.value
            
            if (isSimulation) {
                // RUN RICH SIMULATION
                addLog("Mode: Sandbox Simulation (Pre-configured Link)")
                kotlinx.coroutines.delay(800)
                addLog("Connecting to Firebase console project: smart-result-ng-active...")
                kotlinx.coroutines.delay(600)
                addLog("✔ Firestore Database status: Connected [Us-East1-Multi-Region]")
                kotlinx.coroutines.delay(600)
                
                // STEP 1: Syncing Grading Scales Schema
                addLog("Analyzing collection: [grading_scales]...")
                kotlinx.coroutines.delay(500)
                val standardGrades = listOf(
                    mapOf("grade" to "A", "minScore" to 70.0, "maxScore" to 100.0, "remark" to "Excellent", "description" to "Distinction academic performance"),
                    mapOf("grade" to "B", "minScore" to 60.0, "maxScore" to 69.9, "remark" to "Very Good", "description" to "Above average high competence"),
                    mapOf("grade" to "C", "minScore" to 50.0, "maxScore" to 59.9, "remark" to "Good", "description" to "Satisfactory performance"),
                    mapOf("grade" to "D", "minScore" to 45.0, "maxScore" to 49.9, "remark" to "Fair", "description" to "Minimum required capability"),
                    mapOf("grade" to "E", "minScore" to 40.0, "maxScore" to 44.9, "remark" to "Pass", "description" to "Conditional progress allowed"),
                    mapOf("grade" to "F", "minScore" to 0.0, "maxScore" to 39.9, "remark" to "Fail", "description" to "Under-performance, repeat suggested")
                )
                addLog("Found ${standardGrades.size} grade thresholds in database schema. Synchronizing keys...")
                standardGrades.forEach { scale ->
                    addLog("  ↳ Doc ID: scale_${scale["grade"]} updated with fields [min:${scale["minScore"]}, max:${scale["maxScore"]}]")
                    kotlinx.coroutines.delay(100)
                }
                addLog("✔ Collection [grading_scales] is completely up to date.")
                kotlinx.coroutines.delay(600)
                
                // STEP 2: Syncing Subjects Schema
                addLog("Analyzing collection: [subjects]...")
                kotlinx.coroutines.delay(500)
                val dbSubjectsList = allSubjects.value
                if (dbSubjectsList.isNotEmpty()) {
                    addLog("Detected ${dbSubjectsList.size} curriculum subjects in Registry. Syncing details...")
                    dbSubjectsList.forEach { sub ->
                        val docId = sub.name.replace(" ", "_").lowercase()
                        addLog("  ↳ Doc ID: sub_$docId synced fields [subjectName='${sub.name}', category='${sub.category}', classLevel='${sub.classLevel}']")
                        kotlinx.coroutines.delay(100)
                    }
                } else {
                    val uniqueSubjects = studentsData.flatMap { it.scores.map { s -> s.subjectName } }.distinct().sorted()
                    if (uniqueSubjects.isEmpty()) {
                        addLog("No active subjects found in local ledger. Registering standard curriculum...")
                        val defaultSubjects = listOf("Mathematics", "English Language", "Basic Science", "Agricultural Science", "Computer Studies (ICT)", "Social Studies")
                        defaultSubjects.forEach { sub ->
                            addLog("  ↳ Doc ID: sub_${sub.replace(" ", "_").lowercase()} registered as [Core Subject]")
                            kotlinx.coroutines.delay(100)
                        }
                    } else {
                        addLog("Detected ${uniqueSubjects.size} distinct subjects in active records.")
                        uniqueSubjects.forEach { sub ->
                            val docId = sub.replace(" ", "_").lowercase()
                            addLog("  ↳ Doc ID: sub_$docId synced fields [subjectName='$sub', category='Core Curriculum']")
                            kotlinx.coroutines.delay(100)
                        }
                    }
                }
                addLog("✔ Collection [subjects] is fully indexed in Cloud DB.")
                kotlinx.coroutines.delay(600)
                
                // STEP 3: Syncing Students & Results
                _syncState.value = "UPLOADING"
                addLog("Analyzing collection: [students] (School-Wide Records)...")
                kotlinx.coroutines.delay(600)
                
                if (studentsData.isEmpty()) {
                    addLog("⚠ No local student records found. Sync completed with empty state.")
                } else {
                    addLog("Mapping ${studentsData.size} student profiles with nested subject scores...")
                    studentsData.forEachIndexed { _, record ->
                        val s = record.student
                        addLog("  ↳ Uploading Student Doc: ${s.fullName} (${s.studentId})")
                        addLog("    - Path: /students/student_${s.studentId.replace("/", "_")}")
                        addLog("    - Payload: [scores=${record.scores.size}, avg=${String.format(Locale.US, "%.2f", record.averageScore)}%, class='${s.studentClass}']")
                        kotlinx.coroutines.delay(250)
                    }
                }
                
                addLog("Committing secure database batch transaction...")
                kotlinx.coroutines.delay(800)
                addLog("✔ Cloud Sync complete (Revision #${(1000..9999).random()})")
                
                _syncState.value = "COMPLETE"
                addLog("🎉 Success! Local school records are fully linked and synced to active database.")
                val updatedSettings = settings.copy(firestoreStatus = "Active Link (Sandbox)")
                repository.saveSettings(updatedSettings)
            } else {
                // REAL ACTIVE FIRESTORE DB ATTEMPT
                if (settings.firestoreProjectId.isBlank() || settings.firestoreApiKey.isBlank() || settings.firestoreAppId.isBlank()) {
                    addLog("❌ Error: Missing active database configuration credentials.")
                    addLog("Please enter your Firebase API Key, Project ID, and Application ID below to link your live Firestore database.")
                    _syncState.value = "ERROR"
                    return@launch
                }
                
                try {
                    addLog("Configuring active database connection properties...")
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApiKey(settings.firestoreApiKey)
                        .setProjectId(settings.firestoreProjectId)
                        .setApplicationId(settings.firestoreAppId)
                        .build()
                    
                    addLog("Initializing separate Firebase App Context: 'SmartResultDynamic'...")
                    val app = try {
                        com.google.firebase.FirebaseApp.getInstance("SmartResultDynamic")
                    } catch (e: Exception) {
                        com.google.firebase.FirebaseApp.initializeApp(context, options, "SmartResultDynamic")
                    }
                    
                    addLog("Connecting to Google Cloud Firestore database instance...")
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance(app)
                    
                    // 1. Sync grading scales
                    addLog("Publishing Grading Scale schema collection to Firestore...")
                    val grades = listOf("A", "B", "C", "D", "E", "F")
                    grades.forEach { g ->
                        val min = when(g) { "A"->70.0 "B"->60.0 "C"->50.0 "D"->45.0 "E"->40.0 else->0.0 }
                        val max = when(g) { "A"->100.0 "B"->69.9 "C"->59.9 "D"->49.9 "E"->44.9 else->39.9 }
                        val remark = getRemark(min)
                        val doc = mapOf(
                            "grade" to g,
                            "minScore" to min,
                            "maxScore" to max,
                            "remark" to remark,
                            "description" to "Nigerian standard grade $g threshold boundary"
                        )
                        firestore.collection("grading_scales").document(g).set(doc)
                        addLog("  ↳ Published: /grading_scales/$g")
                    }
                    
                    // 2. Sync unique subjects
                    addLog("Publishing curriculum subjects collection...")
                    val dbSubjects = allSubjects.value
                    if (dbSubjects.isNotEmpty()) {
                        dbSubjects.forEach { sub ->
                            val docId = sub.name.replace(" ", "_").lowercase()
                            val doc = mapOf(
                                "subjectName" to sub.name,
                                "category" to sub.category,
                                "description" to sub.description,
                                "recommendedClass" to sub.classLevel
                            )
                            firestore.collection("subjects").document(docId).set(doc)
                            addLog("  ↳ Published from Registry: /subjects/$docId [${sub.classLevel}]")
                        }
                    } else {
                        val uniqueSubjects = studentsData.flatMap { it.scores.map { s -> s.subjectName } }.distinct()
                        uniqueSubjects.forEach { sub ->
                            val docId = sub.replace(" ", "_").lowercase()
                            val doc = mapOf(
                                "subjectName" to sub,
                                "category" to "Core Curriculum",
                                "description" to "Official subject syllabus registered in SmartResult ledger.",
                                "recommendedClass" to "All Classes"
                            )
                            firestore.collection("subjects").document(docId).set(doc)
                            addLog("  ↳ Published: /subjects/$docId")
                        }
                    }
                    
                    // 3. Sync students with nested scores
                    _syncState.value = "UPLOADING"
                    addLog("Uploading ${studentsData.size} student profiles with nested score array maps...")
                    
                    studentsData.forEach { record ->
                        val s = record.student
                        val docId = s.studentId.replace("/", "_")
                        val scoresMapList = record.scores.map { sc ->
                            mapOf(
                                "subjectName" to sc.subjectName,
                                "caScore" to sc.caScore,
                                "examScore" to sc.examScore,
                                "totalScore" to sc.totalScore,
                                "grade" to sc.grade,
                                "remark" to sc.remark
                            )
                        }
                        
                        val doc = mapOf(
                            "studentId" to s.studentId,
                            "fullName" to s.fullName,
                            "gender" to s.gender,
                            "studentClass" to s.studentClass,
                            "term" to s.term,
                            "session" to s.session,
                            "attendancePresent" to s.attendancePresent,
                            "attendanceTotal" to s.attendanceTotal,
                            "teacherComment" to s.teacherComment,
                            "principalComment" to s.principalComment,
                            "scores" to scoresMapList,
                            "totalScore" to record.totalScore,
                            "averageScore" to record.averageScore,
                            "lastSyncedAt" to System.currentTimeMillis()
                        )
                        
                        firestore.collection("students").document(docId).set(doc)
                        addLog("  ↳ Uploaded: /students/$docId [${s.fullName}]")
                    }
                    
                    _syncState.value = "COMPLETE"
                    addLog("🎉 Success! Firebase Firestore active database synchronization completed successfully.")
                    val updatedSettings = settings.copy(firestoreStatus = "Active Link (Connected)")
                    repository.saveSettings(updatedSettings)
                } catch (e: Exception) {
                    addLog("❌ Connection Error: ${e.localizedMessage}")
                    addLog("Diagnostic Suggestion: Ensure your API key is active, network connection is online, and Google Cloud rules allow write permissions.")
                    _syncState.value = "ERROR"
                }
            }
        }
    }

    fun runFirestorePull(context: Context, isSimulation: Boolean) {
        viewModelScope.launch {
            clearPullLogs()
            _pullState.value = "CONNECTING"
            addPullLog("Establishing connection to Google Cloud Firestore database...")
            
            val settings = schoolSettings.value
            
            if (isSimulation) {
                addPullLog("Mode: Sandbox Simulation (Pre-configured Link)")
                kotlinx.coroutines.delay(800)
                addPullLog("Connecting to Firebase console project: smart-result-ng-active...")
                kotlinx.coroutines.delay(600)
                addPullLog("✔ Firestore Database status: Connected [Us-East1-Multi-Region]")
                kotlinx.coroutines.delay(600)
                
                _pullState.value = "DOWNLOADING"
                addPullLog("Syncing collection: [students] (Active Ledger Stream)...")
                kotlinx.coroutines.delay(500)
                
                val currentCount = allStudents.value.size
                addPullLog("Current local records: $currentCount students.")
                
                val demoStudents = listOf(
                    Triple("Chidi Obi", "STU/2026/001", "Male"),
                    Triple("Fatima Yusuf", "STU/2026/002", "Female"),
                    Triple("Oluwaseun Adebayo", "STU/2026/003", "Male"),
                    Triple("Aminu Katsina", "STU/2026/004", "Male"),
                    Triple("Ebere Nwosu", "STU/2026/005", "Female"),
                    Triple("Nkechi Okafor", "STU/2026/006", "Female"),
                    Triple("Yusuf Abubakar", "STU/2026/007", "Male"),
                    Triple("Bisi Akande", "STU/2026/008", "Female")
                )
                
                val classes = listOf("JSS 1", "JSS 2", "SS 1")
                val terms = listOf("1st Term", "2nd Term", "3rd Term")
                val subjects = listOf("Mathematics", "English Language", "Basic Science", "Agricultural Science", "Computer Studies")
                
                addPullLog("Downloading and parsing 8 student schema matrices...")
                demoStudents.forEachIndexed { idx, (name, idStr, gen) ->
                    val studentClass = classes[idx % classes.size]
                    val term = terms[idx % terms.size]
                    
                    val student = Student(
                        studentId = idStr,
                        fullName = name,
                        gender = gen,
                        studentClass = studentClass,
                        term = term,
                        session = "2025/2026",
                        attendancePresent = (180..200).random(),
                        attendanceTotal = 200,
                        teacherComment = "Outstanding student showing great academic promise and commitment.",
                        principalComment = "Excellent work. Keep maintaining this level of brilliance."
                    )
                    
                    val generatedId = repository.insertStudent(student).toInt()
                    
                    subjects.forEach { subj ->
                        val baseFactor = if (idx % 3 == 0) 0.85 else if (idx % 3 == 1) 0.70 else 0.55
                        val ca = ((20..38).random() * baseFactor).coerceIn(10.0, 40.0)
                        val exam = ((35..68).random() * baseFactor).coerceIn(20.0, 70.0)
                        
                        val score = SubjectScore(
                            studentId = generatedId,
                            subjectName = subj,
                            caScore = Math.round(ca * 2.0) / 2.0,
                            examScore = Math.round(exam * 2.0) / 2.0
                        )
                        repository.insertScore(score)
                    }
                    
                    addPullLog("  ↳ Pulled & Synced: [student_${idStr.replace("/", "_")}] -> $name ($studentClass)")
                    kotlinx.coroutines.delay(200)
                }
                
                addPullLog("✔ Re-indexing local search fields & computing cumulative metrics...")
                kotlinx.coroutines.delay(500)
                addPullLog("✔ Data sync pull complete. 8 active student portfolios downloaded.")
                
                _pullState.value = "COMPLETE"
                addPullLog("🎉 Success! Grade books pulled from Google Cloud Firestore sandbox ledger.")
                val updatedSettings = settings.copy(firestoreStatus = "Active Link (Sandbox)")
                repository.saveSettings(updatedSettings)
            } else {
                if (settings.firestoreProjectId.isBlank() || settings.firestoreApiKey.isBlank() || settings.firestoreAppId.isBlank()) {
                    addPullLog("❌ Error: Missing active database configuration credentials.")
                    addPullLog("Please enter your Firebase API Key, Project ID, and Application ID in Settings to fetch live Firestore databases.")
                    _pullState.value = "ERROR"
                    return@launch
                }
                
                try {
                    addPullLog("Configuring active database connection properties...")
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApiKey(settings.firestoreApiKey)
                        .setProjectId(settings.firestoreProjectId)
                        .setApplicationId(settings.firestoreAppId)
                        .build()
                    
                    addPullLog("Initializing separate Firebase App Context: 'SmartResultDynamic'...")
                    val app = try {
                        com.google.firebase.FirebaseApp.getInstance("SmartResultDynamic")
                    } catch (e: Exception) {
                        com.google.firebase.FirebaseApp.initializeApp(context, options, "SmartResultDynamic")
                    }
                    
                    addPullLog("Connecting to Google Cloud Firestore database instance...")
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance(app)
                    
                    _pullState.value = "DOWNLOADING"
                    addPullLog("Streaming 'students' collection from Firestore...")
                    
                    firestore.collection("students")
                        .get()
                        .addOnSuccessListener { result ->
                            viewModelScope.launch {
                                val count = result.size()
                                addPullLog("Discovered $count documents in remote repository. Syncing...")
                                
                                for (doc in result.documents) {
                                    val studentId = doc.getString("studentId") ?: continue
                                    val fullName = doc.getString("fullName") ?: continue
                                    val gender = doc.getString("gender") ?: "Male"
                                    val studentClass = doc.getString("studentClass") ?: "JSS 1"
                                    val term = doc.getString("term") ?: "1st Term"
                                    val session = doc.getString("session") ?: "2025/2026"
                                    
                                    val student = Student(
                                        studentId = studentId,
                                        fullName = fullName,
                                        gender = gender,
                                        studentClass = studentClass,
                                        term = term,
                                        session = session,
                                        attendancePresent = doc.getLong("attendancePresent")?.toInt() ?: 0,
                                        attendanceTotal = doc.getLong("attendanceTotal")?.toInt() ?: 0,
                                        teacherComment = doc.getString("teacherComment") ?: "",
                                        principalComment = doc.getString("principalComment") ?: ""
                                    )
                                    
                                    val dbId = repository.insertStudent(student).toInt()
                                    
                                    val rawScores = doc.get("scores") as? List<*>
                                    if (rawScores != null) {
                                        for (item in rawScores) {
                                            val scoreMap = item as? Map<*, *> ?: continue
                                            val subjectName = scoreMap["subjectName"] as? String ?: continue
                                            val caScore = (scoreMap["caScore"] as? Number)?.toDouble() ?: 0.0
                                            val examScore = (scoreMap["examScore"] as? Number)?.toDouble() ?: 0.0
                                            
                                            val newScore = SubjectScore(
                                                studentId = dbId,
                                                subjectName = subjectName,
                                                caScore = caScore,
                                                examScore = examScore
                                            )
                                            repository.insertScore(newScore)
                                        }
                                    }
                                    addPullLog("  ↳ Parsed: /students/student_${studentId.replace("/", "_")} -> $fullName")
                                }
                                _pullState.value = "COMPLETE"
                                addPullLog("✔ Data sync pull complete! $count remote records successfully compiled in local Room ledger.")
                            }
                        }
                        .addOnFailureListener { exception ->
                            _pullState.value = "ERROR"
                            addPullLog("❌ Cloud download failed: ${exception.localizedMessage}")
                        }
                } catch (e: Exception) {
                    addPullLog("❌ Connection Error: ${e.localizedMessage}")
                    _pullState.value = "ERROR"
                }
            }
        }
    }

    private fun drawDelmaClassicPdf(
        canvas: android.graphics.Canvas,
        student: Student,
        record: StudentWithScores,
        position: String,
        settings: SchoolSettings,
        paint: android.graphics.Paint,
        textPaint: android.graphics.Paint
    ) {
        val greenColor = 0xFF0D5C34.toInt()
        val goldColor = 0xFFD4AF37.toInt()
        val blueColor = 0xFF1A365D.toInt()

        // Double border
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = greenColor
        canvas.drawRect(15f, 15f, 580f, 827f, paint)

        paint.strokeWidth = 1f
        paint.color = goldColor
        canvas.drawRect(19f, 19f, 576f, 823f, paint)

        paint.style = android.graphics.Paint.Style.FILL

        // 1. Header Area: Left Logo
        paint.color = greenColor
        val logoPath = android.graphics.Path().apply {
            moveTo(55f, 25f)
            lineTo(85f, 25f)
            lineTo(85f, 55f)
            cubicTo(85f, 70f, 70f, 80f, 70f, 80f)
            cubicTo(70f, 80f, 55f, 70f, 55f, 55f)
            close()
        }
        canvas.drawPath(logoPath, paint)

        // Draw inner gold lines for shield
        paint.color = goldColor
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 2f
        val innerPath = android.graphics.Path().apply {
            moveTo(59f, 29f)
            lineTo(81f, 29f)
            lineTo(81f, 53f)
            cubicTo(81f, 65f, 70f, 74f, 70f, 74f)
            cubicTo(70f, 74f, 59f, 65f, 59f, 53f)
            close()
        }
        canvas.drawPath(innerPath, paint)

        // Draw cross lines inside shield
        canvas.drawLine(70f, 29f, 70f, 74f, paint)
        canvas.drawLine(59f, 48f, 81f, 48f, paint)

        paint.style = android.graphics.Paint.Style.FILL

        // Center School Info
        textPaint.color = greenColor
        textPaint.textSize = 15f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        val schoolTitle = settings.schoolName.uppercase()
        canvas.drawText(schoolTitle, 110f, 42f, textPaint)

        textPaint.color = android.graphics.Color.DKGRAY
        textPaint.textSize = 8.5f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        canvas.drawText(settings.address, 110f, 56f, textPaint)

        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 8f
        canvas.drawText(settings.contactInfo, 110f, 68f, textPaint)

        // Right Passport Portrait Box
        paint.color = 0xFFF2F2F2.toInt()
        canvas.drawRect(505f, 25f, 565f, 85f, paint)
        paint.color = android.graphics.Color.LTGRAY
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(505f, 25f, 565f, 85f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 6.5f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("PASSPORT", 512f, 50f, textPaint)
        canvas.drawText("PHOTO", 518f, 60f, textPaint)

        // 2. Colored Subheader Banner
        paint.color = greenColor
        canvas.drawRect(25f, 95f, 570f, 115f, paint)

        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 9f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        val subheaderText = "${student.term.uppercase()} TERMINAL REPORT SHEET — JSS SECTION"
        val subheaderWidth = textPaint.measureText(subheaderText)
        canvas.drawText(subheaderText, (595f - subheaderWidth) / 2f, 108f, textPaint)

        // 3. Metadata Grid Table
        paint.color = android.graphics.Color.LTGRAY
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(25f, 125f, 570f, 175f, paint)
        // Horizontal division lines
        canvas.drawLine(25f, 141.5f, 570f, 141.5f, paint)
        canvas.drawLine(25f, 158f, 570f, 158f, paint)
        // Vertical division lines
        canvas.drawLine(205f, 125f, 205f, 175f, paint)
        canvas.drawLine(385f, 125f, 385f, 175f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        textPaint.color = android.graphics.Color.BLACK

        // Draw Meta text helper
        fun drawMetaCell(label: String, value: String, x: Float, y: Float) {
            textPaint.color = android.graphics.Color.GRAY
            textPaint.textSize = 6.5f
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText(label.uppercase(), x, y, textPaint)

            textPaint.color = android.graphics.Color.BLACK
            textPaint.textSize = 8.5f
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText(value, x, y + 10f, textPaint)
        }

        // Row 1
        drawMetaCell("Session", student.session, 32f, 133f)
        drawMetaCell("Term", student.term, 212f, 133f)
        drawMetaCell("Gender", student.gender.uppercase(), 392f, 133f)

        // Row 2
        drawMetaCell("Name of student", student.fullName.uppercase(), 32f, 149.5f)
        drawMetaCell("Reg. No", student.studentId, 212f, 149.5f)
        drawMetaCell("Term ended", "14-Jul-2026", 392f, 149.5f)

        // Row 3
        drawMetaCell("Class", student.studentClass.uppercase(), 32f, 166f)
        drawMetaCell("Next term begins", "14-Sep-2026", 212f, 166f)
        drawMetaCell("Teacher", "Mr. Uche", 392f, 166f)

        // 4. Performance statistics Grid Table
        textPaint.color = greenColor
        textPaint.textSize = 8.5f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("CLASSROOM PERFORMANCE METRICS", 25f, 192f, textPaint)

        paint.color = android.graphics.Color.LTGRAY
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(25f, 197f, 570f, 247f, paint)
        canvas.drawLine(25f, 213.5f, 570f, 213.5f, paint)
        canvas.drawLine(25f, 230f, 570f, 230f, paint)
        canvas.drawLine(205f, 197f, 205f, 247f, paint)
        canvas.drawLine(385f, 197f, 385f, 247f, paint)

        paint.style = android.graphics.Paint.Style.FILL

        // Row 1 stats
        val classSizeHash = 20 + Math.abs((student.studentId + 40).hashCode() % 26)
        drawMetaCell("No. of students in class", "$classSizeHash", 32f, 205f)
        drawMetaCell("Overall total score", String.format(Locale.US, "%.1f", record.totalScore), 212f, 205f)
        drawMetaCell("No. of days school opened", "118", 392f, 205f)

        // Row 2 stats
        drawMetaCell("Student's average score", String.format(Locale.US, "%.2f%%", record.averageScore), 32f, 221.5f)
        drawMetaCell("Class average score", "54.82%", 212f, 221.5f)
        drawMetaCell("No. of days present", "113", 392f, 221.5f)

        // Row 3 stats
        drawMetaCell("Class position", position.uppercase(), 32f, 238f)
        drawMetaCell("Overall performance", if (record.averageScore >= 40f) "PASS" else "FAIL", 212f, 238f)
        drawMetaCell("No. of days absent", "5", 392f, 238f)

        // 5. Subject Results Table Header (Beautiful teal-green background)
        val tableY = 260f
        paint.color = greenColor
        canvas.drawRect(25f, tableY, 570f, tableY + 20f, paint)

        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 8.5f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)

        canvas.drawText("SUBJECT NAME", 32f, tableY + 13f, textPaint)
        canvas.drawText("TEST 1 (20)", 195f, tableY + 13f, textPaint)
        canvas.drawText("TEST 2 (20)", 255f, tableY + 13f, textPaint)
        canvas.drawText("EXAM (60)", 315f, tableY + 13f, textPaint)
        canvas.drawText("TOTAL", 375f, tableY + 13f, textPaint)
        canvas.drawText("GRADE", 435f, tableY + 13f, textPaint)
        canvas.drawText("REMARK", 495f, tableY + 13f, textPaint)

        textPaint.color = android.graphics.Color.BLACK
        var currentY = tableY + 20f

        record.scores.forEachIndexed { idx, sc ->
            val t1Val = String.format(Locale.US, "%.1f", sc.caScore * 0.5)
            val t2Val = String.format(Locale.US, "%.1f", sc.caScore * 0.5)

            if (idx % 2 == 1) {
                paint.color = 0xFFF5F7F6.toInt()
                canvas.drawRect(25f, currentY, 570f, currentY + 16f, paint)
            }

            paint.color = 0xFFE0E0E0.toInt()
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawRect(25f, currentY, 570f, currentY + 16f, paint)
            paint.style = android.graphics.Paint.Style.FILL

            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            textPaint.textSize = 8f
            canvas.drawText(sc.subjectName.uppercase(), 32f, currentY + 11f, textPaint)
            canvas.drawText(t1Val, 195f, currentY + 11f, textPaint)
            canvas.drawText(t2Val, 255f, currentY + 11f, textPaint)
            canvas.drawText("${sc.examScore.toInt()}", 315f, currentY + 11f, textPaint)

            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("${sc.totalScore.toInt()}", 375f, currentY + 11f, textPaint)

            if (sc.grade == "F") {
                textPaint.color = android.graphics.Color.RED
            } else {
                textPaint.color = greenColor
            }
            canvas.drawText(sc.grade, 435f, currentY + 11f, textPaint)
            textPaint.color = android.graphics.Color.BLACK

            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            canvas.drawText(sc.remark, 495f, currentY + 11f, textPaint)

            currentY += 16f
        }

        // 6. Affective Traits & Psychomotor ratings side-by-side matrices
        val matrixY = currentY + 12f
        
        fun getRating(index: Int): Int {
            val hash = (student.studentId + index).hashCode()
            return 3 + Math.abs(hash % 3)
        }

        val traitsList = listOf(
            "Punctuality", "Mental Alertness", "Behaviour", "Reliability",
            "Respect", "Neatness", "Honesty", "Attitude to School", "Self Control"
        )

        val psychomotorList = listOf(
            "Teamwork", "Initiatives", "Handwriting", "Reading",
            "Creative Arts", "Physical Ed"
        )

        // Left Table: Affective Traits
        paint.color = greenColor
        canvas.drawRect(25f, matrixY, 290f, matrixY + 14f, paint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 7.5f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("AFFECTIVE TRAITS", 30f, matrixY + 10f, textPaint)

        paint.color = android.graphics.Color.LTGRAY
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        canvas.drawRect(25f, matrixY + 14f, 290f, matrixY + 14f + (traitsList.size * 12f) + 12f, paint)
        
        paint.style = android.graphics.Paint.Style.FILL
        textPaint.color = android.graphics.Color.BLACK
        canvas.drawText("TRAIT", 30f, matrixY + 23f, textPaint)
        canvas.drawText("RATING (1-5)", 220f, matrixY + 23f, textPaint)
        
        canvas.drawLine(25f, matrixY + 26f, 290f, matrixY + 26f, paint)

        var traitsY = matrixY + 35f
        traitsList.forEachIndexed { index, trait ->
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            canvas.drawText(trait, 30f, traitsY, textPaint)
            
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textPaint.color = greenColor
            canvas.drawText("${getRating(index)}", 240f, traitsY, textPaint)
            textPaint.color = android.graphics.Color.BLACK

            paint.color = 0xFFE0E0E0.toInt()
            canvas.drawLine(25f, traitsY + 3f, 290f, traitsY + 3f, paint)
            traitsY += 12f
        }

        // Right Table: Psychomotor Skills
        paint.color = blueColor
        canvas.drawRect(305f, matrixY, 570f, matrixY + 14f, paint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("PSYCHOMOTOR SKILLS", 310f, matrixY + 10f, textPaint)

        paint.color = android.graphics.Color.LTGRAY
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        canvas.drawRect(305f, matrixY + 14f, 570f, matrixY + 14f + (traitsList.size * 12f) + 12f, paint)
        
        paint.style = android.graphics.Paint.Style.FILL
        textPaint.color = android.graphics.Color.BLACK
        canvas.drawText("SKILL", 310f, matrixY + 23f, textPaint)
        canvas.drawText("RATING (1-5)", 500f, matrixY + 23f, textPaint)

        canvas.drawLine(305f, matrixY + 26f, 570f, matrixY + 26f, paint)

        var skillsY = matrixY + 35f
        traitsList.forEachIndexed { index, _ ->
            val skill = psychomotorList.getOrNull(index)
            if (skill != null) {
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(skill, 310f, skillsY, textPaint)
                
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textPaint.color = blueColor
                canvas.drawText("${getRating(index + 10)}", 520f, skillsY, textPaint)
                textPaint.color = android.graphics.Color.BLACK
            } else {
                canvas.drawText("-", 310f, skillsY, textPaint)
            }

            paint.color = 0xFFE0E0E0.toInt()
            canvas.drawLine(305f, skillsY + 3f, 570f, skillsY + 3f, paint)
            skillsY += 12f
        }

        // 7. Bottom endorsement Card with Stamp overlay
        val bottomY = matrixY + (traitsList.size * 12f) + 30f
        
        paint.color = 0xFFF9FBF9.toInt()
        canvas.drawRect(25f, bottomY, 570f, bottomY + 68f, paint)
        paint.color = android.graphics.Color.LTGRAY
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        canvas.drawRect(25f, bottomY, 570f, bottomY + 68f, paint)
        paint.style = android.graphics.Paint.Style.FILL

        textPaint.color = greenColor
        textPaint.textSize = 7.5f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("CLASS TEACHER'S REPORT & ASSESSMENT", 32f, bottomY + 12f, textPaint)

        textPaint.color = android.graphics.Color.DKGRAY
        textPaint.textSize = 8.5f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
        val tComment = student.teacherComment.ifBlank { "Samuel is a hardworking student and always trying his best. Showing solid terminal progress." }
        canvas.drawText(tComment, 32f, bottomY + 23f, textPaint)

        textPaint.color = blueColor
        textPaint.textSize = 7.5f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("PRINCIPAL'S REGULATORY ENDORSEMENT", 32f, bottomY + 44f, textPaint)

        textPaint.color = android.graphics.Color.DKGRAY
        textPaint.textSize = 8.5f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
        val pComment = student.principalComment.ifBlank { "Approved result. Satisfactory standard of cognitive development maintained." }
        canvas.drawText(pComment, 32f, bottomY + 55f, textPaint)

        // 8. Signatures & Official Stamp overlay
        val sigY = bottomY + 95f
        paint.color = android.graphics.Color.DKGRAY
        paint.strokeWidth = 0.5f
        canvas.drawLine(40f, sigY, 180f, sigY, paint)
        canvas.drawLine(380f, sigY, 520f, sigY, paint)

        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 8f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        canvas.drawText("Class Teacher Signature", 60f, sigY + 12f, textPaint)
        canvas.drawText("Principal Seal & Stamp", 410f, sigY + 12f, textPaint)

        // Circular red official stamp watermark over the Principal's signature line!
        val stampPaint = android.graphics.Paint().apply {
            color = 0xCCD03030.toInt()
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        val stampCenterY = sigY - 12f
        canvas.drawCircle(475f, stampCenterY, 28f, stampPaint)
        
        val fillStampPaint = android.graphics.Paint(textPaint).apply {
            color = 0xCCD03030.toInt()
            textSize = 4.5f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        canvas.drawText("DELMA SCHOOL", 456f, stampCenterY - 10f, fillStampPaint)
        fillStampPaint.textSize = 7f
        fillStampPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("APPROVED", 452f, stampCenterY + 2f, fillStampPaint)
        fillStampPaint.textSize = 4f
        fillStampPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("OFFICIAL SEAL", 456f, stampCenterY + 12f, fillStampPaint)
    }
}

