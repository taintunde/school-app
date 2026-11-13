package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.util.Locale

private val ColorSwatches = listOf(
    "#15803D" to "Forest Green",
    "#16A34A" to "Emerald",
    "#0D9488" to "Teal",
    "#0284C7" to "Sky Blue",
    "#2563EB" to "Royal Blue",
    "#4F46E5" to "Indigo",
    "#7C3AED" to "Purple",
    "#D97706" to "Amber",
    "#EA580C" to "Orange",
    "#DC2626" to "Crimson Red"
)

fun parseHexColor(hex: String, fallback: Color = Color(0xFF15803D)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = if (clean.length == 6) {
            ("FF$clean").toLong(16)
        } else {
            clean.toLong(16)
        }
        Color(colorInt)
    } catch (e: Exception) {
        fallback
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradingScaleScreen(
    viewModel: ResultViewModel,
    settings: SchoolSettings
) {
    val allScales by viewModel.allGradingScales.collectAsStateWithLifecycle()
    val allClasses by viewModel.allClasses.collectAsStateWithLifecycle()

    // Active selected school level scale
    var selectedScaleId by remember { mutableStateOf<Int?>(null) }

    // If no scale is selected yet, pick the first or default scale
    val activeScale = remember(allScales, selectedScaleId) {
        if (allScales.isEmpty()) {
            GradingScalePresets.getDefaultScales().first()
        } else {
            allScales.find { it.id == selectedScaleId }
                ?: allScales.find { it.isDefault }
                ?: allScales.first()
        }
    }

    // Editable state for the active scale
    var scaleNameInput by remember(activeScale) { mutableStateOf(activeScale.scaleName) }
    var schoolLevelInput by remember(activeScale) { mutableStateOf(activeScale.schoolLevel) }
    var maxGpaInput by remember(activeScale) { mutableStateOf(activeScale.maxGpa.toString()) }
    var passingGradeInput by remember(activeScale) { mutableStateOf(activeScale.passingGrade) }
    var subjectWeightingType by remember(activeScale) { mutableStateOf(activeScale.subjectWeightingType) }
    var defaultCreditUnitsInput by remember(activeScale) { mutableStateOf(activeScale.defaultCreditUnits.toString()) }
    var appliesToClassesInput by remember(activeScale) { mutableStateOf(activeScale.appliesToClasses) }
    var gradeBandsState by remember(activeScale) { mutableStateOf(activeScale.gradeBands) }

    // Interactive simulator test score state
    var testSimulatorScore by remember { mutableStateOf(72.5) }

    // Dialog states
    var showEditBandDialog by remember { mutableStateOf(false) }
    var editingBandIndex by remember { mutableStateOf<Int?>(null) }
    var showNewScaleDialog by remember { mutableStateOf(false) }
    var showPresetMenu by remember { mutableStateOf(false) }
    var successNotification by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Sync selectedScaleId when scales change
    LaunchedEffect(allScales) {
        if (selectedScaleId == null && allScales.isNotEmpty()) {
            selectedScaleId = allScales.find { it.isDefault }?.id ?: allScales.first().id
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Grading Scales",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Grading Scales & GPA Weighting",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Customize score brackets (e.g. A=70-100, B=60-69) and GPA weights per school level",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // School Level Navigation Chips & Add Level Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECT SCHOOL LEVEL:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Reset to defaults
                            TextButton(
                                onClick = {
                                    viewModel.resetGradingScalesToDefaults {
                                        successNotification = "Grading scales reset to official national standards!"
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Defaults", fontSize = 12.sp)
                            }

                            // Add new custom level
                            OutlinedButton(
                                onClick = { showNewScaleDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Level", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Level Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        allScales.forEach { scale ->
                            val isSelected = activeScale.id == scale.id
                            Surface(
                                selected = isSelected,
                                onClick = { selectedScaleId = scale.id },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("scale_chip_${scale.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = scale.schoolLevel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (scale.isDefault) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "DEFAULT",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${scale.scaleName} • Max ${String.format(Locale.US, "%.1f", scale.maxGpa)} GPA",
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Scale Overview & Metadata Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scale Configuration: ${activeScale.schoolLevel}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Presets Template Dropdown
                        Box {
                            OutlinedButton(
                                onClick = { showPresetMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Load Preset", fontSize = 12.sp)
                            }

                            DropdownMenu(
                                expanded = showPresetMenu,
                                onDismissRequest = { showPresetMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("WAEC / NECO 9-Point Scale (A1-F9, Max 5.0 GPA)") },
                                    onClick = {
                                        scaleNameInput = "WAEC / NECO 9-Point Scale"
                                        maxGpaInput = "5.0"
                                        passingGradeInput = "Credit (C6)"
                                        gradeBandsState = GradingScalePresets.WAEC_NECO_SCALE
                                        showPresetMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Standard Secondary (A-F, 70-100, Max 5.0 GPA)") },
                                    onClick = {
                                        scaleNameInput = "Standard Secondary (A-F)"
                                        maxGpaInput = "5.0"
                                        passingGradeInput = "Pass (E)"
                                        gradeBandsState = GradingScalePresets.STANDARD_SECONDARY_SCALE
                                        showPresetMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Primary Competency 4-Tier (Max 4.0 GPA)") },
                                    onClick = {
                                        scaleNameInput = "Primary Competency 4-Tier"
                                        maxGpaInput = "4.0"
                                        passingGradeInput = "Approaching (C)"
                                        gradeBandsState = GradingScalePresets.PRIMARY_COMPETENCY_SCALE
                                        showPresetMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Degree Honours Classification (5.0 CGPA)") },
                                    onClick = {
                                        scaleNameInput = "Degree Honours (5.0 CGPA)"
                                        maxGpaInput = "5.0"
                                        passingGradeInput = "Pass (E)"
                                        subjectWeightingType = "CreditUnits"
                                        defaultCreditUnitsInput = "2"
                                        gradeBandsState = GradingScalePresets.TERTIARY_CGPA_SCALE
                                        showPresetMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("International / US 4.0 Scale (A=90-100)") },
                                    onClick = {
                                        scaleNameInput = "US / International 4.0 Scale"
                                        maxGpaInput = "4.0"
                                        passingGradeInput = "Pass (D)"
                                        gradeBandsState = GradingScalePresets.INTERNATIONAL_4_SCALE
                                        showPresetMenu = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Fields Row: Scale Name & Target School Level
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = scaleNameInput,
                            onValueChange = { scaleNameInput = it },
                            label = { Text("Scale Title / Identifier", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("scale_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = schoolLevelInput,
                            onValueChange = { schoolLevelInput = it },
                            label = { Text("School Level", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("school_level_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // GPA Scale Selection & Weighting Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MAX GPA SYSTEM:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("5.0", "4.0", "10.0").forEach { gpaVal ->
                                    val isSelected = maxGpaInput.trim() == gpaVal
                                    Surface(
                                        selected = isSelected,
                                        onClick = { maxGpaInput = gpaVal },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "$gpaVal Scale",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GPA WEIGHTING MODE:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Equal" to "Unweighted", "CreditUnits" to "Credit Units").forEach { (mode, label) ->
                                    val isSelected = subjectWeightingType == mode
                                    Surface(
                                        selected = isSelected,
                                        onClick = { subjectWeightingType = mode },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Passing Grade & Target Class Mapping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = passingGradeInput,
                            onValueChange = { passingGradeInput = it },
                            label = { Text("Passing Benchmark", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("passing_grade_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = appliesToClassesInput,
                            onValueChange = { appliesToClassesInput = it },
                            label = { Text("Assigned Classes (e.g. SS 1, SS 2, SS 3)", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("applies_to_classes_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Card 3: Interactive Visual Grade Spectrum Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Score Range Visual Spectrum (0% - 100%)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${gradeBandsState.size} Grade Boundaries",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    // Stacked Colored Spectrum Bar
                    if (gradeBandsState.isNotEmpty()) {
                        val sortedBands = gradeBandsState.sortedBy { it.minScore }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            sortedBands.forEach { band ->
                                val weight = ((band.maxScore - band.minScore).coerceAtLeast(1.0)).toFloat()
                                Box(
                                    modifier = Modifier
                                        .weight(weight)
                                        .fillMaxHeight()
                                        .background(parseHexColor(band.colorHex))
                                        .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.5f))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = band.grade,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Cutoff Labels Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0%", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            sortedBands.forEach { band ->
                                Text("${band.minScore.toInt()}%", fontSize = 10.sp, color = Color.Gray)
                            }
                            Text("100%", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Card 4: Live Interactive Grade & GPA Simulator Playground
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Grade & GPA Simulator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Score: ${String.format(Locale.US, "%.1f", testSimulatorScore)}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Score Slider
                    Slider(
                        value = testSimulatorScore.toFloat(),
                        onValueChange = { testSimulatorScore = it.toDouble() },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Simulator Result Card
                    val evaluatedBand = remember(testSimulatorScore, gradeBandsState) {
                        gradeBandsState.firstOrNull { testSimulatorScore >= it.minScore && testSimulatorScore <= it.maxScore }
                            ?: gradeBandsState.minByOrNull { Math.abs(it.minScore - testSimulatorScore) }
                            ?: GradeBand("F", 0.0, 39.9, 0.0, "Fail", "#DC2626")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(parseHexColor(evaluatedBand.colorHex), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = evaluatedBand.grade,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = evaluatedBand.remark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Range: ${evaluatedBand.minScore.toInt()} - ${evaluatedBand.maxScore.toInt()}%",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${String.format(Locale.US, "%.2f", evaluatedBand.gpaPoints)} GPA Pts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = parseHexColor(evaluatedBand.colorHex)
                            )
                            val isPass = evaluatedBand.gpaPoints > 0 && !evaluatedBand.remark.contains("Fail", ignoreCase = true)
                            Text(
                                text = if (isPass) "Meets Passing Standard" else "Below Passing Standard",
                                fontSize = 10.sp,
                                color = if (isPass) Color(0xFF15803D) else Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Card 5: Grade Bands List & Detailed Editor
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Grade Brackets & Point Values (${gradeBandsState.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                editingBandIndex = null
                                showEditBandDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_grade_band_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Band", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Table / Card Listing of Grade Bands
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        gradeBandsState.forEachIndexed { index, band ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(parseHexColor(band.colorHex), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = band.grade,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${band.minScore.toInt()} - ${band.maxScore.toInt()}%",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = parseHexColor(band.colorHex).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.2f", band.gpaPoints)} Pts",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = parseHexColor(band.colorHex),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = band.remark + if (band.description.isNotBlank()) " • ${band.description}" else "",
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingBandIndex = index
                                                showEditBandDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Grade",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (gradeBandsState.size > 1) {
                                                    gradeBandsState = gradeBandsState.filterIndexed { i, _ -> i != index }
                                                } else {
                                                    errorMessage = "A grading scale must have at least one grade bracket."
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Grade",
                                                tint = Color.Red.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Save / Apply Button
                    Button(
                        onClick = {
                            val maxGpaDouble = maxGpaInput.toDoubleOrNull() ?: 5.0
                            val defaultUnitsInt = defaultCreditUnitsInput.toIntOrNull() ?: 1
                            val updatedConfig = activeScale.copy(
                                scaleName = scaleNameInput.trim().ifBlank { "Custom Scale" },
                                schoolLevel = schoolLevelInput.trim().ifBlank { "General" },
                                maxGpa = maxGpaDouble,
                                passingGrade = passingGradeInput.trim().ifBlank { "Pass" },
                                subjectWeightingType = subjectWeightingType,
                                defaultCreditUnits = defaultUnitsInt,
                                appliesToClasses = appliesToClassesInput.trim().ifBlank { "All" },
                                gradeBandsJson = GradingScaleSerializer.toJson(gradeBandsState)
                            )

                            viewModel.saveGradingScale(updatedConfig) {
                                successNotification = "Grading scale & GPA weightings successfully saved for ${updatedConfig.schoolLevel}!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_grading_scale_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save & Apply Grading Scale (${activeScale.schoolLevel})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Notification / Success Banner
        if (successNotification != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF15803D).copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, Color(0xFF15803D).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = successNotification ?: "",
                            color = Color(0xFF15803D),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { successNotification = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF15803D))
                        }
                    }
                }
            }
        }

        // Error Banner
        if (errorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add / Edit Grade Band
    if (showEditBandDialog) {
        val existingBand = editingBandIndex?.let { gradeBandsState.getOrNull(it) }
        var dlgGrade by remember { mutableStateOf(existingBand?.grade ?: "") }
        var dlgMinScore by remember { mutableStateOf(existingBand?.minScore?.toString() ?: "70") }
        var dlgMaxScore by remember { mutableStateOf(existingBand?.maxScore?.toString() ?: "100") }
        var dlgGpaPoints by remember { mutableStateOf(existingBand?.gpaPoints?.toString() ?: "5.0") }
        var dlgRemark by remember { mutableStateOf(existingBand?.remark ?: "Excellent") }
        var dlgColorHex by remember { mutableStateOf(existingBand?.colorHex ?: "#15803D") }
        var dlgDescription by remember { mutableStateOf(existingBand?.description ?: "") }
        var dlgError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditBandDialog = false },
            title = {
                Text(
                    text = if (existingBand != null) "Edit Grade Bracket" else "Add New Grade Bracket",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (dlgError != null) {
                        Text(dlgError ?: "", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = dlgGrade,
                        onValueChange = { dlgGrade = it },
                        label = { Text("Grade Letter / Label (e.g. A, A1, B+)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("dialog_grade_label_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dlgMinScore,
                            onValueChange = { dlgMinScore = it },
                            label = { Text("Min Score (%)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("dialog_min_score_input")
                        )

                        OutlinedTextField(
                            value = dlgMaxScore,
                            onValueChange = { dlgMaxScore = it },
                            label = { Text("Max Score (%)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("dialog_max_score_input")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dlgGpaPoints,
                            onValueChange = { dlgGpaPoints = it },
                            label = { Text("GPA Point Weight") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("dialog_gpa_points_input")
                        )

                        OutlinedTextField(
                            value = dlgRemark,
                            onValueChange = { dlgRemark = it },
                            label = { Text("Remark (e.g. Credit)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("dialog_remark_input")
                        )
                    }

                    OutlinedTextField(
                        value = dlgDescription,
                        onValueChange = { dlgDescription = it },
                        label = { Text("Optional Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "SELECT BADGE COLOR:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    // Color Swatches
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorSwatches.forEach { (hex, name) ->
                            val isSel = dlgColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(parseHexColor(hex), CircleShape)
                                    .border(
                                        width = if (isSel) 3.dp else 1.dp,
                                        color = if (isSel) Color.Black else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { dlgColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val min = dlgMinScore.toDoubleOrNull()
                        val max = dlgMaxScore.toDoubleOrNull()
                        val gpa = dlgGpaPoints.toDoubleOrNull()

                        if (dlgGrade.isBlank()) {
                            dlgError = "Grade label cannot be empty."
                            return@Button
                        }
                        if (min == null || max == null || min < 0 || max > 100 || min > max) {
                            dlgError = "Enter valid score boundaries between 0 and 100 (Min <= Max)."
                            return@Button
                        }
                        if (gpa == null || gpa < 0) {
                            dlgError = "Enter valid GPA point weight."
                            return@Button
                        }

                        val newBand = GradeBand(
                            grade = dlgGrade.trim(),
                            minScore = min,
                            maxScore = max,
                            gpaPoints = gpa,
                            remark = dlgRemark.trim().ifBlank { "Pass" },
                            colorHex = dlgColorHex,
                            description = dlgDescription.trim()
                        )

                        val updatedList = gradeBandsState.toMutableList()
                        if (editingBandIndex != null && editingBandIndex!! in updatedList.indices) {
                            updatedList[editingBandIndex!!] = newBand
                        } else {
                            updatedList.add(newBand)
                        }
                        // Sort by minScore descending (e.g. A down to F)
                        gradeBandsState = updatedList.sortedByDescending { it.minScore }
                        showEditBandDialog = false
                    }
                ) {
                    Text("Save Bracket")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBandDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Create New School Level Scale
    if (showNewScaleDialog) {
        var newLevelName by remember { mutableStateOf("") }
        var newScaleTitle by remember { mutableStateOf("") }
        var newPresetTemplate by remember { mutableStateOf("Standard Secondary (A-F)") }
        var newMaxGpa by remember { mutableStateOf("5.0") }
        var newClasses by remember { mutableStateOf("") }
        var newError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showNewScaleDialog = false },
            title = { Text("Define New School Level Scale", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (newError != null) {
                        Text(newError ?: "", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = newLevelName,
                        onValueChange = { newLevelName = it },
                        label = { Text("School Level (e.g. Sixth Form, Pre-Nursery)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_level_name_input")
                    )

                    OutlinedTextField(
                        value = newScaleTitle,
                        onValueChange = { newScaleTitle = it },
                        label = { Text("Scale Name (e.g. Cambridge IGCSE, 4-Tier)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_scale_title_input")
                    )

                    OutlinedTextField(
                        value = newClasses,
                        onValueChange = { newClasses = it },
                        label = { Text("Target School Classes (e.g. Year 10, Grade 1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "STARTING PRESET TEMPLATE:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    listOf(
                        "Standard Secondary (A-F)" to GradingScalePresets.STANDARD_SECONDARY_SCALE,
                        "WAEC / NECO 9-Point Scale" to GradingScalePresets.WAEC_NECO_SCALE,
                        "Primary Competency 4-Tier" to GradingScalePresets.PRIMARY_COMPETENCY_SCALE,
                        "US / International 4.0 Scale" to GradingScalePresets.INTERNATIONAL_4_SCALE
                    ).forEach { (presetName, _) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { newPresetTemplate = presetName }
                        ) {
                            RadioButton(
                                selected = newPresetTemplate == presetName,
                                onClick = { newPresetTemplate = presetName }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(presetName, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newLevelName.isBlank()) {
                            newError = "School level name is required."
                            return@Button
                        }
                        val templateBands = when (newPresetTemplate) {
                            "WAEC / NECO 9-Point Scale" -> GradingScalePresets.WAEC_NECO_SCALE
                            "Primary Competency 4-Tier" -> GradingScalePresets.PRIMARY_COMPETENCY_SCALE
                            "US / International 4.0 Scale" -> GradingScalePresets.INTERNATIONAL_4_SCALE
                            else -> GradingScalePresets.STANDARD_SECONDARY_SCALE
                        }

                        val newScale = GradingScaleConfig(
                            schoolLevel = newLevelName.trim(),
                            scaleName = newScaleTitle.trim().ifBlank { "$newLevelName Grading Scale" },
                            maxGpa = if (newPresetTemplate.contains("4.0")) 4.0 else 5.0,
                            passingGrade = "Pass",
                            isDefault = false,
                            gradeBandsJson = GradingScaleSerializer.toJson(templateBands),
                            subjectWeightingType = "Equal",
                            defaultCreditUnits = 1,
                            appliesToClasses = newClasses.trim().ifBlank { "All" }
                        )

                        viewModel.saveGradingScale(newScale) {
                            successNotification = "Created new grading scale for ${newScale.schoolLevel}!"
                        }
                        showNewScaleDialog = false
                    }
                ) {
                    Text("Create Level Scale")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewScaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
