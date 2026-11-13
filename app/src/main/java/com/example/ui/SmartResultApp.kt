package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.rotate
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import com.example.R
import com.example.data.*
import com.example.ui.theme.SchoolGold
import com.example.ui.theme.SchoolGreen
import com.example.ui.theme.SecondaryBlue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.roundToInt
import com.example.ui.theme.BackgroundLight
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import java.util.Locale

@Composable
fun SmartResultApp(viewModel: ResultViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val students by viewModel.allStudents.collectAsStateWithLifecycle()
    val studentsWithScores by viewModel.allStudentsWithScores.collectAsStateWithLifecycle()
    val settings by viewModel.schoolSettings.collectAsStateWithLifecycle()
    val selectedStudentId by viewModel.selectedStudentId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    BackHandler(enabled = currentScreen != AppScreen.Dashboard) {
        viewModel.navigateBack()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isTablet) {
            // Landscape/Tablet Side-by-Side Sidebar Layout
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationSidebar(
                    currentScreen = currentScreen,
                    isPremium = settings.isPremium,
                    onNavigate = { viewModel.navigateTo(it) },
                    settings = settings
                )
                // Reliable divider line
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.LightGray.copy(alpha = 0.5f)))
                Box(modifier = Modifier.weight(1f)) {
                    MainContent(
                        screen = currentScreen,
                        viewModel = viewModel,
                        students = students,
                        studentsWithScores = studentsWithScores,
                        settings = settings,
                        selectedStudentId = selectedStudentId,
                        searchQuery = searchQuery
                    )
                }
            }
        } else {
            // Portrait/Mobile Layout with Hidden Menu in Top-Right Corner
            Scaffold { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    MainContent(
                        screen = currentScreen,
                        viewModel = viewModel,
                        students = students,
                        studentsWithScores = studentsWithScores,
                        settings = settings,
                        selectedStudentId = selectedStudentId,
                        searchQuery = searchQuery
                    )

                    // Hidden Menu Bar / Dropdown at Top-Right Corner
                    var menuExpanded by remember { mutableStateOf(false) }
                    val activeColor = remember(settings.immersiveMode, settings.brandColorHex) {
                        if (settings.immersiveMode) {
                            try {
                                Color(settings.brandColorHex.toColorInt())
                            } catch (e: Exception) {
                                SchoolGreen
                            }
                        } else {
                            SchoolGreen
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp)
                    ) {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.92f),
                                    shape = CircleShape
                                )
                                .border(1.dp, activeColor.copy(alpha = 0.3f), CircleShape)
                                .testTag("floating_menu_trigger_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Menu",
                                tint = activeColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            val menuScreens = listOf(
                                Triple(AppScreen.Dashboard, "Dashboard", Icons.Default.Home),
                                Triple(AppScreen.Export, "Export PDF Center", Icons.Default.Share),
                                Triple(AppScreen.CsvImport, "CSV Data Import", Icons.AutoMirrored.Filled.List),
                                Triple(AppScreen.GradingScaleConfig, "Grading & GPA Scales", Icons.Default.Star),
                                Triple(AppScreen.AddInfo, "Add Info Hub", Icons.Default.Add),
                                Triple(AppScreen.InputScores, "Input Scores", Icons.Default.Edit),
                                Triple(AppScreen.LedgerTable, "Ledger Table", Icons.Default.Menu),
                                Triple(AppScreen.GenerateResults, "Results Ledger", Icons.AutoMirrored.Filled.List),
                                Triple(AppScreen.PerformanceAnalytics, "Performance Analytics", Icons.Default.Star),
                                Triple(AppScreen.CloudSync, "Cloud Sync Hub", Icons.Default.Refresh),
                                Triple(AppScreen.Settings, "School Settings", Icons.Default.Settings)
                            )

                            menuScreens.forEach { (screen, label, icon) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (currentScreen == screen) activeColor else Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = label,
                                                fontWeight = if (currentScreen == screen) FontWeight.Bold else FontWeight.Normal,
                                                color = if (currentScreen == screen) activeColor else Color.Black,
                                                fontSize = 14.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.navigateTo(screen)
                                        menuExpanded = false
                                    },
                                    modifier = Modifier.testTag("menu_item_${screen.name}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationSidebar(
    currentScreen: AppScreen,
    isPremium: Boolean,
    onNavigate: (AppScreen) -> Unit,
    settings: SchoolSettings
) {
    val activeColor = remember(settings.immersiveMode, settings.brandColorHex) {
        if (settings.immersiveMode) {
            try {
                Color(settings.brandColorHex.toColorInt())
            } catch (e: Exception) {
                SchoolGreen
            }
        } else {
            SchoolGreen
        }
    }

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                AcademicShieldLogo(modifier = Modifier.size(52.dp), logoUri = settings.logoUri)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (settings.immersiveMode) {
                        val name = settings.schoolName
                        if (name.length > 13) name.take(11) + ".." else name
                    } else "SmartResult",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = activeColor,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isPremium) "PREMIUM NG" else "FREE TIER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPremium) activeColor else Color.Gray,
                    modifier = Modifier
                        .background(
                            color = if (isPremium) SchoolGold.copy(alpha = 0.25f) else Color.LightGray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        },
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        NavigationRailItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            selected = currentScreen == AppScreen.Dashboard,
            onClick = { onNavigate(AppScreen.Dashboard) },
            modifier = Modifier.testTag("dashboard_nav")
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Add, contentDescription = "Add Info") },
            label = { Text("Add Info") },
            selected = currentScreen == AppScreen.AddInfo,
            onClick = { onNavigate(AppScreen.AddInfo) },
            modifier = Modifier.testTag("add_info_nav")
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Edit, contentDescription = "Input Scores") },
            label = { Text("Input Scores") },
            selected = currentScreen == AppScreen.InputScores,
            onClick = { onNavigate(AppScreen.InputScores) },
            modifier = Modifier.testTag("input_scores_nav")
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Menu, contentDescription = "Ledger Table") },
            label = { Text("Ledger Grid") },
            selected = currentScreen == AppScreen.LedgerTable,
            onClick = { onNavigate(AppScreen.LedgerTable) },
            modifier = Modifier.testTag("ledger_table_nav")
        )
        NavigationRailItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Results") },
            label = { Text("Generate") },
            selected = currentScreen == AppScreen.GenerateResults,
            onClick = { onNavigate(AppScreen.GenerateResults) },
            modifier = Modifier.testTag("generate_results_nav")
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Share, contentDescription = "Export") },
            label = { Text("Export") },
            selected = currentScreen == AppScreen.Export,
            onClick = { onNavigate(AppScreen.Export) },
            modifier = Modifier.testTag("export_nav")
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentScreen == AppScreen.Settings,
            onClick = { onNavigate(AppScreen.Settings) },
            modifier = Modifier.testTag("settings_nav")
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Grading Scales") },
            label = { Text("Grading") },
            selected = currentScreen == AppScreen.GradingScaleConfig,
            onClick = { onNavigate(AppScreen.GradingScaleConfig) },
            modifier = Modifier.testTag("grading_scale_nav")
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Refresh, contentDescription = "Cloud Sync") },
            label = { Text("Cloud Hub") },
            selected = currentScreen == AppScreen.CloudSync,
            onClick = { onNavigate(AppScreen.CloudSync) },
            modifier = Modifier.testTag("cloud_sync_nav")
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Star, contentDescription = "Analytics") },
            label = { Text("Analytics") },
            selected = currentScreen == AppScreen.PerformanceAnalytics,
            onClick = { onNavigate(AppScreen.PerformanceAnalytics) },
            modifier = Modifier.testTag("performance_analytics_nav")
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen == AppScreen.Dashboard,
            onClick = { onNavigate(AppScreen.Dashboard) },
            modifier = Modifier.testTag("dashboard_nav")
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Add, contentDescription = "Add Info") },
            label = { Text("Add Info", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen == AppScreen.AddInfo,
            onClick = { onNavigate(AppScreen.AddInfo) },
            modifier = Modifier.testTag("add_info_nav")
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Edit, contentDescription = "Input Scores") },
            label = { Text("Scores", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen == AppScreen.InputScores,
            onClick = { onNavigate(AppScreen.InputScores) },
            modifier = Modifier.testTag("input_scores_nav")
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Menu, contentDescription = "Ledger Table") },
            label = { Text("Ledger", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen == AppScreen.LedgerTable,
            onClick = { onNavigate(AppScreen.LedgerTable) },
            modifier = Modifier.testTag("ledger_table_nav")
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Results") },
            label = { Text("Results", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen == AppScreen.GenerateResults,
            onClick = { onNavigate(AppScreen.GenerateResults) },
            modifier = Modifier.testTag("generate_results_nav")
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Share, contentDescription = "Export") },
            label = { Text("Export", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen == AppScreen.Export,
            onClick = { onNavigate(AppScreen.Export) },
            modifier = Modifier.testTag("export_nav")
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Star, contentDescription = "Analytics") },
            label = { Text("Analytics", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen == AppScreen.PerformanceAnalytics,
            onClick = { onNavigate(AppScreen.PerformanceAnalytics) },
            modifier = Modifier.testTag("performance_analytics_nav")
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            selected = currentScreen == AppScreen.Settings,
            onClick = { onNavigate(AppScreen.Settings) },
            modifier = Modifier.testTag("settings_nav")
        )
    }
}

@Composable
fun MainContent(
    screen: AppScreen,
    viewModel: ResultViewModel,
    students: List<Student>,
    studentsWithScores: List<StudentWithScores>,
    settings: SchoolSettings,
    selectedStudentId: Int?,
    searchQuery: String
) {
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "ScreenTransition"
    ) { targetScreen ->
        when (targetScreen) {
            AppScreen.Dashboard -> DashboardScreen(viewModel, studentsWithScores, settings)
            AppScreen.AddStudent -> AddStudentScreen(viewModel, settings)
            AppScreen.InputScores -> InputScoresScreen(viewModel, students, studentsWithScores, selectedStudentId)
            AppScreen.LedgerTable -> LedgerTableScreen(viewModel, students, studentsWithScores, settings)
            AppScreen.GenerateResults -> GenerateResultsScreen(viewModel, studentsWithScores, settings, selectedStudentId)
            AppScreen.Settings -> SettingsScreen(viewModel, settings, students.size)
            AppScreen.CloudSync -> CloudSyncScreen(viewModel, students, studentsWithScores, settings)
            AppScreen.ManageSubjects -> ManageSubjectsScreen(viewModel, settings)
            AppScreen.PerformanceAnalytics -> PerformanceAnalyticsScreen(viewModel, studentsWithScores, settings)
            AppScreen.AddInfo -> AddInfoScreen(viewModel, settings, students, studentsWithScores)
            AppScreen.TemplateEditor -> TemplateEditorScreen(viewModel, settings)
            AppScreen.Export -> ExportScreen(viewModel, studentsWithScores, settings)
            AppScreen.CsvImport -> CsvImportScreen(viewModel, settings)
            AppScreen.GradingScaleConfig -> GradingScaleScreen(viewModel, settings)
        }
    }
}

// -------------------------------------------------------------
// LOGO COMPOSABLE
// -------------------------------------------------------------
@Composable
fun AcademicShieldLogo(
    modifier: Modifier = Modifier,
    logoUri: String? = null
) {
    if (logoUri == null || logoUri == "default" || logoUri == "mascot") {
        Image(
            painter = painterResource(id = R.drawable.img_smart_result_logo_1784026965544),
            contentDescription = "SmartResult NG Logo",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else if (logoUri.startsWith("/") || logoUri.startsWith("file:") || logoUri.startsWith("content:")) {
        val context = LocalContext.current
        val imageBitmap = remember(logoUri) {
            try {
                val fileUri = if (logoUri.startsWith("/")) Uri.fromFile(java.io.File(logoUri)) else logoUri.toUri()
                val inputStream = context.contentResolver.openInputStream(fileUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Uploaded School Logo",
                modifier = modifier.clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_smart_result_logo_1784026965544),
                contentDescription = "SmartResult NG Logo",
                modifier = modifier,
                contentScale = ContentScale.Fit
            )
        }
    } else if (logoUri.startsWith("crest:")) {
        val type = logoUri.substringAfter("crest:")
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            
            when (type) {
                "shield_green" -> {
                    // Draw Shield
                    val path = Path().apply {
                        moveTo(w * 0.15f, h * 0.1f)
                        lineTo(w * 0.85f, h * 0.1f)
                        lineTo(w * 0.85f, h * 0.55f)
                        quadraticTo(w * 0.85f, h * 0.85f, w * 0.5f, h * 0.98f)
                        quadraticTo(w * 0.15f, h * 0.85f, w * 0.15f, h * 0.55f)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFF0D5C34))
                    drawPath(path = path, color = Color(0xFFD4AF37), style = Stroke(width = w * 0.05f))
                    
                    // Star symbol inside
                    val starCenter = Offset(w * 0.5f, h * 0.48f)
                    val starRadius = w * 0.15f
                    val starPath = Path().apply {
                        for (i in 0..4) {
                            val angleRad1 = Math.toRadians((18 + i * 72 - 90).toDouble())
                            val angleRad2 = Math.toRadians((54 + i * 72 - 90).toDouble())
                            val p1 = Offset(
                                (starCenter.x + Math.cos(angleRad1) * starRadius).toFloat(),
                                (starCenter.y + Math.sin(angleRad1) * starRadius).toFloat()
                            )
                            val p2 = Offset(
                                (starCenter.x + Math.cos(angleRad2) * (starRadius / 2)).toFloat(),
                                (starCenter.y + Math.sin(angleRad2) * (starRadius / 2)).toFloat()
                            )
                            if (i == 0) moveTo(p1.x, p1.y) else lineTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                        }
                        close()
                    }
                    drawPath(path = starPath, color = Color(0xFFD4AF37))
                }
                "cap_blue" -> {
                    // Circle background
                    drawCircle(color = Color(0xFF1A365D), radius = w * 0.48f, center = center)
                    drawCircle(color = Color(0xFFD4AF37), radius = w * 0.48f, center = center, style = Stroke(width = w * 0.04f))
                    
                    // Mortarboard Cap shape
                    // Diamond top
                    val capPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.28f) // Top
                        lineTo(w * 0.8f, h * 0.43f) // Right
                        lineTo(w * 0.5f, h * 0.58f) // Bottom
                        lineTo(w * 0.2f, h * 0.43f) // Left
                        close()
                    }
                    drawPath(path = capPath, color = Color.White)
                    
                    // Cap skull under
                    val skullPath = Path().apply {
                        moveTo(w * 0.35f, h * 0.5f)
                        lineTo(w * 0.35f, h * 0.65f)
                        quadraticTo(w * 0.5f, h * 0.75f, w * 0.65f, h * 0.65f)
                        lineTo(w * 0.65f, h * 0.55f)
                    }
                    drawPath(path = skullPath, color = Color.White)
                    
                    // Tassel
                    val tasselPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.43f)
                        lineTo(w * 0.25f, h * 0.55f)
                        lineTo(w * 0.25f, h * 0.68f)
                    }
                    drawPath(path = tasselPath, color = Color(0xFFD4AF37), style = Stroke(width = w * 0.03f))
                }
                "book_maroon" -> {
                    // Shield/Hexagon background
                    val bgPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.05f)
                        lineTo(w * 0.9f, h * 0.25f)
                        lineTo(w * 0.9f, h * 0.75f)
                        lineTo(w * 0.5f, h * 0.95f)
                        lineTo(w * 0.1f, h * 0.75f)
                        lineTo(w * 0.1f, h * 0.25f)
                        close()
                    }
                    drawPath(path = bgPath, color = Color(0xFF800020))
                    drawPath(path = bgPath, color = Color.White, style = Stroke(width = w * 0.04f))
                    
                    // Open Book
                    // Left page
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.23f, h * 0.35f),
                        size = Size(w * 0.24f, h * 0.32f)
                    )
                    // Right page
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.53f, h * 0.35f),
                        size = Size(w * 0.24f, h * 0.32f)
                    )
                    // Spine / dividing line
                    drawLine(
                        color = Color(0xFF800020),
                        start = Offset(w * 0.5f, h * 0.32f),
                        end = Offset(w * 0.5f, h * 0.68f),
                        strokeWidth = w * 0.04f
                    )
                }
                "torch_purple" -> {
                    // Octagon or circle
                    drawCircle(color = Color(0xFF4A154B), radius = w * 0.48f, center = center)
                    drawCircle(color = Color(0xFFFFA500), radius = w * 0.45f, center = center, style = Stroke(width = w * 0.03f))
                    
                    // Torch handle
                    val handlePath = Path().apply {
                        moveTo(w * 0.46f, h * 0.75f)
                        lineTo(w * 0.54f, h * 0.75f)
                        lineTo(w * 0.58f, h * 0.45f)
                        lineTo(w * 0.42f, h * 0.45f)
                        close()
                    }
                    drawPath(path = handlePath, color = Color.LightGray)
                    
                    // Flame
                    val flamePath = Path().apply {
                        moveTo(w * 0.42f, h * 0.45f)
                        quadraticTo(w * 0.35f, h * 0.25f, w * 0.5f, h * 0.12f) // center peak
                        quadraticTo(w * 0.65f, h * 0.25f, w * 0.58f, h * 0.45f)
                        close()
                    }
                    drawPath(path = flamePath, color = Color(0xFFFF5722))
                    
                    val innerFlame = Path().apply {
                        moveTo(w * 0.46f, h * 0.45f)
                        quadraticTo(w * 0.42f, h * 0.32f, w * 0.5f, h * 0.22f)
                        quadraticTo(w * 0.58f, h * 0.32f, w * 0.54f, h * 0.45f)
                        close()
                    }
                    drawPath(path = innerFlame, color = Color(0xFFFFEB3B))
                }
                "laurel_teal" -> {
                    drawCircle(color = Color(0xFF319795), radius = w * 0.48f, center = center)
                    
                    // Laurel leaves on left and right
                    val leftLaurel = Path().apply {
                        moveTo(w * 0.5f, h * 0.85f)
                        quadraticTo(w * 0.2f, h * 0.7f, w * 0.22f, h * 0.35f)
                        quadraticTo(w * 0.25f, h * 0.25f, w * 0.32f, h * 0.22f)
                    }
                    drawPath(path = leftLaurel, color = Color.White, style = Stroke(width = w * 0.04f))
                    
                    val rightLaurel = Path().apply {
                        moveTo(w * 0.5f, h * 0.85f)
                        quadraticTo(w * 0.8f, h * 0.7f, w * 0.78f, h * 0.35f)
                        quadraticTo(w * 0.75f, h * 0.25f, w * 0.68f, h * 0.22f)
                    }
                    drawPath(path = rightLaurel, color = Color.White, style = Stroke(width = w * 0.04f))
                    
                    // Little leaves
                    drawCircle(color = Color.White, radius = w * 0.05f, center = Offset(w * 0.26f, h * 0.65f))
                    drawCircle(color = Color.White, radius = w * 0.05f, center = Offset(w * 0.74f, h * 0.65f))
                    drawCircle(color = Color.White, radius = w * 0.05f, center = Offset(w * 0.22f, h * 0.5f))
                    drawCircle(color = Color.White, radius = w * 0.05f, center = Offset(w * 0.78f, h * 0.5f))
                    drawCircle(color = Color.White, radius = w * 0.05f, center = Offset(w * 0.25f, h * 0.35f))
                    drawCircle(color = Color.White, radius = w * 0.05f, center = Offset(w * 0.75f, h * 0.35f))
                    
                    // Star in center
                    val starCenter = Offset(w * 0.5f, h * 0.48f)
                    val starRadius = w * 0.12f
                    val starPath = Path().apply {
                        for (i in 0..4) {
                            val angleRad1 = Math.toRadians((18 + i * 72 - 90).toDouble())
                            val angleRad2 = Math.toRadians((54 + i * 72 - 90).toDouble())
                            val p1 = Offset(
                                (starCenter.x + Math.cos(angleRad1) * starRadius).toFloat(),
                                (starCenter.y + Math.sin(angleRad1) * starRadius).toFloat()
                            )
                            val p2 = Offset(
                                (starCenter.x + Math.cos(angleRad2) * (starRadius / 2)).toFloat(),
                                (starCenter.y + Math.sin(angleRad2) * (starRadius / 2)).toFloat()
                            )
                            if (i == 0) moveTo(p1.x, p1.y) else lineTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                        }
                        close()
                    }
                    drawPath(path = starPath, color = Color(0xFFD4AF37))
                }
            }
        }
    } else if (logoUri.startsWith("custom:")) {
        // Format: custom:INITIALS|SHAPE|COLOR_HEX
        val parts = logoUri.substringAfter("custom:").split("|")
        val initials = parts.getOrNull(0) ?: "SCH"
        val shape = parts.getOrNull(1) ?: "circle"
        val colorHex = parts.getOrNull(2) ?: "#0D5C34"
        val color = try { Color(colorHex.toColorInt()) } catch (e: Exception) { Color(0xFF0D5C34) }
        
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                when (shape) {
                    "circle" -> {
                        drawCircle(color = color, radius = w * 0.48f)
                        drawCircle(color = Color.White, radius = w * 0.44f, style = Stroke(width = w * 0.03f))
                    }
                    "shield" -> {
                        val path = Path().apply {
                            moveTo(w * 0.15f, h * 0.1f)
                            lineTo(w * 0.85f, h * 0.1f)
                            lineTo(w * 0.85f, h * 0.55f)
                            quadraticTo(w * 0.85f, h * 0.85f, w * 0.5f, h * 0.98f)
                            quadraticTo(w * 0.15f, h * 0.85f, w * 0.15f, h * 0.55f)
                            close()
                        }
                        drawPath(path = path, color = color)
                        drawPath(path = path, color = Color.White, style = Stroke(width = w * 0.03f))
                    }
                    "square" -> {
                        val roundPx = w * 0.15f
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(w * 0.05f, h * 0.05f),
                            size = Size(w * 0.9f, h * 0.9f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(roundPx, roundPx)
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(w * 0.08f, h * 0.08f),
                            size = Size(w * 0.84f, h * 0.84f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(roundPx * 0.8f, roundPx * 0.8f),
                            style = Stroke(width = w * 0.03f)
                        )
                    }
                }
            }
            
            Text(
                text = initials.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (initials.length > 2) 11.sp else 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN 1: DASHBOARD
// -------------------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: ResultViewModel,
    records: List<StudentWithScores>,
    settings: SchoolSettings
) {
    val totalStudents = records.size
    val schoolAverage = if (records.isNotEmpty()) {
        val averages = records.map { it.averageScore }
        averages.average()
    } else 0.0

    val passedCount = records.count { it.scores.isNotEmpty() && it.averageScore >= 40.0 }
    val failedCount = records.count { it.scores.isNotEmpty() && it.averageScore < 40.0 }

    val excellentCount = records.count { it.scores.isNotEmpty() && it.averageScore >= 70.0 }
    val veryGoodCount = records.count { it.scores.isNotEmpty() && it.averageScore in 60.0..69.9 }
    val goodCount = records.count { it.scores.isNotEmpty() && it.averageScore in 50.0..59.9 }
    val passCount = records.count { it.scores.isNotEmpty() && it.averageScore in 40.0..49.9 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SmartResult NG",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = settings.schoolName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nigerian Primary & Secondary Schools Grading and Results Ledger System",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    AcademicShieldLogo(modifier = Modifier.size(80.dp), logoUri = settings.logoUri)
                }
            }
        }

        // Cloud Firestore Database Link Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloud Firestore Database Link",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.DarkGray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = when {
                                            settings.firestoreStatus.contains("Connected") -> SchoolGreen
                                            settings.firestoreStatus.contains("Sandbox") -> SecondaryBlue
                                            else -> Color.Gray
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Status: ${settings.firestoreStatus}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.CloudSync) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Hub Console", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Students",
                    value = "$totalStudents",
                    icon = Icons.Default.Person,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "School Average",
                    value = String.format(Locale.US, "%.1f%%", schoolAverage),
                    icon = Icons.Default.Star,
                    color = SchoolGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Free version limit warning banner
        if (!settings.isPremium) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SchoolGold.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, SchoolGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = SchoolGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Free Plan Limit: $totalStudents / 20 Students",
                                fontWeight = FontWeight.Bold,
                                color = SchoolGreen,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Upgrade to Premium to manage unlimited students, unlock Excel/PDF exports, and custom templates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.Settings) },
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Upgrade", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Analytical Performance Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(0.5.dp, Color.LightGray)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Class Performance Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SchoolGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Overall distribution of student final grades in 3rd Term",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (totalStudents == 0 || records.none { it.scores.isNotEmpty() }) {
                        // Empty analytics state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, contentDescription = "No data", tint = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("No scores inputted yet. Results and charts will populate here.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Graphic bar chart
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            BarMetric(label = "Excellent (70-100% / A)", count = excellentCount, total = totalStudents, color = SchoolGreen)
                            BarMetric(label = "Very Good (60-69% / B)", count = veryGoodCount, total = totalStudents, color = SecondaryBlue)
                            BarMetric(label = "Good (50-59% / C)", count = goodCount, total = totalStudents, color = Color(0xFF00ACC1))
                            BarMetric(label = "Fair & Pass (40-49% / D & E)", count = passCount, total = totalStudents, color = SchoolGold)
                            BarMetric(label = "Failed (0-39% / F)", count = failedCount, total = totalStudents, color = Color.Red)
                        }
                    }
                }
            }
        }

        // Beautiful Immersive Hero Illustration Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_academic_hero_1784026274635),
                        contentDescription = "Academic Leadership",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay to keep text legible
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "SMARTRESULT ACADEMIC LEDGER",
                            color = SchoolGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            style = TextStyle(letterSpacing = 1.5.sp)
                        )
                        Text(
                            text = "Interactive Nigeria Grading Suite",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Seamless terminal results, automated position rankings, and elegant A4 PDF reports.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Quick Spreadsheet Shortcut Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecondaryBlue.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, SecondaryBlue.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.LedgerTable) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = null,
                        tint = SecondaryBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Launch Academic Spreadsheet Ledger",
                            fontWeight = FontWeight.Bold,
                            color = SecondaryBlue,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Access React-style tabular spreadsheet to type grades and rename students rapidly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = SecondaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Subject Curriculum Registry Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateToAddInfo(2) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manage Curriculum Subjects",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Add, edit, or delete official registered subjects by class level and synchronize with Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Performance Analytics Dashboard Card
        item {
            val activeColor = remember(settings.immersiveMode, settings.brandColorHex) {
                if (settings.immersiveMode) {
                    try {
                        Color(settings.brandColorHex.toColorInt())
                    } catch (e: Exception) {
                        SchoolGreen
                    }
                } else {
                    SchoolGreen
                }
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = activeColor.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, activeColor.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.PerformanceAnalytics) }
                    .testTag("launch_analytics_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Performance Analytics Dashboard",
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Analyze school-wide grade averages, subject trends, and grade distributions with interactive visual charts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Fast Actions and student quick-list
        item {
            Text(
                text = "Recently Registered Students",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SchoolGreen,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (records.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No students registered yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.navigateToAddInfo(0) },
                                colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Register First Student")
                            }
                        }
                    }
                }
            }
        } else {
            items(records.take(5)) { record ->
                StudentCard(
                    record = record,
                    onInputScores = {
                        viewModel.selectStudent(record.student.id)
                        viewModel.navigateTo(AppScreen.InputScores)
                    },
                    onGenerateReport = {
                        viewModel.selectStudent(record.student.id)
                        viewModel.navigateTo(AppScreen.GenerateResults)
                    },
                    onDelete = {
                        viewModel.deleteStudent(record.student)
                    }
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = SchoolGreen)
        }
    }
}

@Composable
fun BarMetric(label: String, count: Int, total: Int, color: Color) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
            Text("$count student(s) (${String.format(Locale.US, "%.1f%%", fraction * 100)})", fontSize = 11.sp, color = Color.Gray)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(color, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun StudentCard(
    record: StudentWithScores,
    onInputScores: () -> Unit,
    onGenerateReport: () -> Unit,
    onDelete: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete Record") },
            text = { Text("Are you sure you want to delete this record, including all scores entered? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDialog = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student Initials badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(SchoolGreen.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = record.student.fullName.split(" ").map { it.take(1) }.joinToString("").uppercase(),
                    color = SchoolGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.student.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Text(
                    text = "${record.student.studentClass} | ${record.student.studentId}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${record.scores.size} Subject(s) | Avg: ${String.format(Locale.US, "%.1f%%", record.averageScore)}",
                    fontSize = 11.sp,
                    color = if (record.scores.isNotEmpty()) SchoolGreen else Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Quick actions
            IconButton(onClick = onInputScores, modifier = Modifier.testTag("input_score_shortcut")) {
                Icon(Icons.Default.Edit, contentDescription = "Add Scores", tint = SecondaryBlue)
            }
            IconButton(onClick = onGenerateReport, modifier = Modifier.testTag("view_report_shortcut")) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Report Sheet", tint = SchoolGreen)
            }
            IconButton(onClick = { showDialog = true }, modifier = Modifier.testTag("delete_student_shortcut")) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2: ADD STUDENT / REGISTER SCREEN
// -------------------------------------------------------------
@Composable
fun AddStudentScreen(
    viewModel: ResultViewModel,
    settings: SchoolSettings
) {
    val classesFromDb by viewModel.allClasses.collectAsStateWithLifecycle(initialValue = emptyList())
    val classesList = if (classesFromDb.isNotEmpty()) {
        classesFromDb.map { it.className }
    } else {
        listOf(
            "Creche", "Pre-Nursery", "Nursery 1", "Nursery 2",
            "Primary 1", "Primary 2", "Primary 3", "Primary 4", "Primary 5", "Primary 6",
            "JSS 1", "JSS 2", "JSS 3", "SS 1", "SS 2", "SS 3"
        )
    }

    var fullName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var studentClass by remember { mutableStateOf("Creche") }
    var term by remember { mutableStateOf("3rd Term") }
    var session by remember { mutableStateOf("2025/2026") }

    LaunchedEffect(classesList) {
        if (classesList.isNotEmpty() && !classesList.contains(studentClass)) {
            studentClass = classesList.first()
        }
    }

    var bulkInputText by remember { mutableStateOf("") }

    val termsList = listOf("1st Term", "2nd Term", "3rd Term")
    val sessionsList = listOf("2025/2026", "2026/2027", "2027/2028")

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Register Student",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SchoolGreen
            )
            Text(
                text = "Add individual student profiles or bulk upload an Excel/CSV roster",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Individual Student Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SchoolGreen
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { studentId = it },
                        label = { Text("Student Admission/ID Number") },
                        placeholder = { Text("e.g. STU/2026/045") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_id_input"),
                        singleLine = true
                    )

                    // Gender Row
                    Column {
                        Text("Gender", fontSize = 12.sp, color = Color.Gray)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
                                Text("Male")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
                                Text("Female")
                            }
                        }
                    }

                    // Class Selector
                    Column {
                        Text("Class Profile", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Scrollable reliable custom Box-chips grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            classesList.forEach { cls ->
                                val isSelected = studentClass == cls
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) SchoolGreen else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) SchoolGreen else Color.LightGray,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { studentClass = cls }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cls,
                                        color = if (isSelected) Color.White else Color.DarkGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Term & Session Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Term", fontSize = 11.sp, color = Color.Gray)
                            var expandedTerm by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expandedTerm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(term, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                                DropdownMenu(expanded = expandedTerm, onDismissRequest = { expandedTerm = false }) {
                                    termsList.forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t) },
                                            onClick = {
                                                term = t
                                                expandedTerm = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Academic Session", fontSize = 11.sp, color = Color.Gray)
                            var expandedSess by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expandedSess = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(session, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                                DropdownMenu(expanded = expandedSess, onDismissRequest = { expandedSess = false }) {
                                    sessionsList.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s) },
                                            onClick = {
                                                session = s
                                                expandedSess = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.addStudent(
                                fullName = fullName,
                                studentId = studentId,
                                gender = gender,
                                studentClass = studentClass,
                                term = term,
                                session = session,
                                onSuccess = {
                                    Toast.makeText(context, "Student registered successfully!", Toast.LENGTH_SHORT).show()
                                    fullName = ""
                                    studentId = ""
                                    viewModel.navigateTo(AppScreen.Dashboard)
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_student_submit")
                    ) {
                        Text("Add Student Portfolio", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bulk Excel Import Simulator Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Bulk Student Roster Import",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SchoolGreen
                        )
                        if (!settings.isPremium) {
                            Box(
                                modifier = Modifier
                                    .background(SchoolGold, shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("PREMIUM", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paste comma-separated data. Format: Name, ID, Gender, Class. One per line.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bulkInputText,
                        onValueChange = { bulkInputText = it },
                        placeholder = {
                            Text(
                                "e.g.\nTunde Alao, STU/2026/101, Male, SS 2\nObinna Nwosu, STU/2026/102, Male, SS 2\nFatima Bello, STU/2026/103, Female, SS 2"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("bulk_student_input"),
                        enabled = settings.isPremium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.bulkImportStudents(
                                studentsText = bulkInputText,
                                onSuccess = { count ->
                                    Toast.makeText(context, "Successfully imported $count student profiles!", Toast.LENGTH_SHORT).show()
                                    bulkInputText = ""
                                    viewModel.navigateTo(AppScreen.Dashboard)
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_import_submit")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Process Bulk Import", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADD INFO CONTAINER HUB
// -------------------------------------------------------------
@Composable
fun AddInfoScreen(
    viewModel: ResultViewModel,
    settings: SchoolSettings,
    students: List<Student>,
    studentsWithScores: List<StudentWithScores>
) {
    val subTab by viewModel.addInfoSubTab.collectAsStateWithLifecycle()
    val activeBrandColor = remember(settings.immersiveMode, settings.brandColorHex) {
        if (settings.immersiveMode) {
            try {
                Color(settings.brandColorHex.toColorInt())
            } catch (e: Exception) {
                SchoolGreen
            }
        } else {
            SchoolGreen
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(activeBrandColor)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
                        modifier = Modifier.testTag("add_info_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AcademicShieldLogo(modifier = Modifier.size(40.dp), logoUri = settings.logoUri)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add Info Hub",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Register Students, Classes, and Subjects",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = subTab,
                    containerColor = activeBrandColor,
                    contentColor = Color.White
                ) {
                    Tab(
                        selected = subTab == 0,
                        onClick = { viewModel.setAddInfoSubTab(0) },
                        text = { Text("Add Student", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    Tab(
                        selected = subTab == 1,
                        onClick = { viewModel.setAddInfoSubTab(1) },
                        text = { Text("Add Class", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    Tab(
                        selected = subTab == 2,
                        onClick = { viewModel.setAddInfoSubTab(2) },
                        text = { Text("Manage Subjects", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    Tab(
                        selected = subTab == 3,
                        onClick = { viewModel.setAddInfoSubTab(3) },
                        text = { Text("CSV Import", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    Tab(
                        selected = subTab == 4,
                        onClick = { viewModel.setAddInfoSubTab(4) },
                        text = { Text("Grading & GPA", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (subTab) {
                0 -> AddStudentScreen(viewModel, settings)
                1 -> AddClassScreen(viewModel, settings)
                2 -> ManageSubjectsScreen(viewModel, settings, isEmbedded = true)
                3 -> CsvImportScreen(viewModel, settings)
                4 -> GradingScaleScreen(viewModel, settings)
            }
        }
    }
}

// -------------------------------------------------------------
// ADD CLASS SCREEN COMPOSABLE
// -------------------------------------------------------------
@Composable
fun AddClassScreen(
    viewModel: ResultViewModel,
    settings: SchoolSettings
) {
    val context = LocalContext.current
    val classesFromDb by viewModel.allClasses.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var newClassName by remember { mutableStateOf("") }
    
    val activeBrandColor = remember(settings.immersiveMode, settings.brandColorHex) {
        if (settings.immersiveMode) {
            try {
                Color(settings.brandColorHex.toColorInt())
            } catch (e: Exception) {
                SchoolGreen
            }
        } else {
            SchoolGreen
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Class Registry Directory",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = activeBrandColor
            )
            Text(
                text = "Add new classes/grade levels or manage existing academic sections.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Register New Class / Grade Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = activeBrandColor
                    )

                    OutlinedTextField(
                        value = newClassName,
                        onValueChange = { newClassName = it },
                        label = { Text("Class Name (e.g. JSS 1 Science, SS 3 Arts)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_class_name_input"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val trimmed = newClassName.trim()
                            if (trimmed.isEmpty()) {
                                Toast.makeText(context, "Please enter a class name", Toast.LENGTH_SHORT).show()
                            } else if (classesFromDb.any { it.className.equals(trimmed, ignoreCase = true) }) {
                                Toast.makeText(context, "This class is already registered!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addClass(trimmed) {
                                    Toast.makeText(context, "Class '$trimmed' registered successfully!", Toast.LENGTH_SHORT).show()
                                    newClassName = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeBrandColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_class_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Register Class Level", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Active Class Registry Directory (${classesFromDb.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = activeBrandColor
                    )

                    if (classesFromDb.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No registered classes found. Use the form above to add one.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            classesFromDb.forEach { schoolClass ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = activeBrandColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = schoolClass.className,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Black,
                                            fontSize = 14.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteClass(schoolClass) {
                                                Toast.makeText(context, "Class '${schoolClass.className}' deleted from registry.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.testTag("delete_class_${schoolClass.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Class",
                                            tint = Color.Red.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 3: INPUT SUBJECT SCORES
// -------------------------------------------------------------
@Composable
fun InputScoresScreen(
    viewModel: ResultViewModel,
    students: List<Student>,
    records: List<StudentWithScores>,
    selectedStudentId: Int?
) {
    val context = LocalContext.current
    val currentRecord = records.find { it.student.id == selectedStudentId }

    var subjectName by remember { mutableStateOf("") }
    var caScoreText by remember { mutableStateOf("") }
    var examScoreText by remember { mutableStateOf("") }

    val standardSubjects = listOf(
        "Mathematics", "English Language", "Basic Science", "Social Studies",
        "Agricultural Science", "Computer Studies (ICT)", "Civic Education",
        "Biology", "Chemistry", "Physics", "Economics", "Literature-in-English"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Input Subject Scores",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SchoolGreen
            )
            Text(
                text = "Enter continuous assessment and examination scores for registered students",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Student Selector Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Student Portfolio", fontWeight = FontWeight.Bold, color = SchoolGreen)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (students.isEmpty()) {
                        Text("Please register a student first in the registration tab.", color = Color.Red, fontSize = 12.sp)
                    } else {
                        var selectorExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { selectorExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = currentRecord?.student?.fullName ?: "Click here to choose student",
                                    color = if (currentRecord != null) Color.Black else Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = selectorExpanded,
                                onDismissRequest = { selectorExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                students.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.fullName} (${s.studentClass})") },
                                        onClick = {
                                            viewModel.selectStudent(s.id)
                                            selectorExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (currentRecord != null) {
            val s = currentRecord.student
            
            // Core Add Score Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "New Score Entry - ${s.fullName}",
                            fontWeight = FontWeight.Bold,
                            color = SchoolGreen,
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Quick Chips for standard Nigerian subjects
                        Text("Popular Academic Subjects", fontSize = 11.sp, color = Color.Gray)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            standardSubjects.forEach { sub ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.LightGray.copy(alpha = 0.35f), shape = RoundedCornerShape(12.dp))
                                        .clickable { subjectName = sub }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(sub, fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = subjectName,
                                onValueChange = { subjectName = it },
                                label = { Text("Subject Name") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("subject_name_input"),
                                singleLine = true
                            )
                            
                            Button(
                                onClick = {
                                    if (subjectName.trim().isNotEmpty()) {
                                        viewModel.addSubject(
                                            name = subjectName.trim(),
                                            category = "Core Curriculum",
                                            classLevel = "All Classes"
                                        ) {
                                            Toast.makeText(context, "Subject added to Curriculum Registry!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Please enter a subject name first", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen.copy(alpha = 0.1f), contentColor = SchoolGreen),
                                border = BorderStroke(1.dp, SchoolGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(56.dp).testTag("quick_add_subject_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Subject", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Subject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = caScoreText,
                                onValueChange = { caScoreText = it },
                                label = { Text("CA Score (Max 30/40)") },
                                placeholder = { Text("e.g. 24") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ca_score_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = examScoreText,
                                onValueChange = { examScoreText = it },
                                label = { Text("Exam Score (Max 60/70)") },
                                placeholder = { Text("e.g. 52") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("exam_score_input"),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                val ca = caScoreText.toDoubleOrNull() ?: 0.0
                                val exam = examScoreText.toDoubleOrNull() ?: 0.0
                                viewModel.saveSubjectScore(
                                    studentId = s.id,
                                    subjectName = subjectName,
                                    caScore = ca,
                                    examScore = exam,
                                    onSuccess = {
                                        Toast.makeText(context, "Score added successfully!", Toast.LENGTH_SHORT).show()
                                        subjectName = ""
                                        caScoreText = ""
                                        examScoreText = ""
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("add_score_submit")
                        ) {
                            Text("Post Academic Score", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Student Current Scores List Header
            item {
                Text(
                    text = "Current Report Entries (${currentRecord.scores.size})",
                    fontWeight = FontWeight.Bold,
                    color = SchoolGreen,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (currentRecord.scores.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No subject scores recorded for this student yet.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(currentRecord.scores) { score ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, Color.LightGray)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(score.subjectName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("CA: ${score.caScore} | Exam: ${score.examScore}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "${score.totalScore.toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    color = SchoolGreen,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Grade ${score.grade} (${score.remark})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (score.grade == "F") Color.Red else Color.Gray
                                )
                            }
                            IconButton(onClick = { viewModel.deleteScore(score) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: TEMPLATE SELECTOR
// -------------------------------------------------------------
@Composable
fun TemplateSelectorComponent(
    selectedTemplate: String,
    isPremium: Boolean,
    onTemplateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = SchoolGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Select Report Card Template",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SchoolGreen
            )
        }
        Text(
            text = "Choose the layout and style of your final terminal reports. Selected theme applies dynamically to PDF downloads and shares.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val templates = listOf(
                Triple("Simple", "Ink-friendly minimalist layout. Plain margins. Ideal for basic or low-cost printing.", "Free"),
                Triple("Modern", "Vibrant header accents in official green & gold, clean list cells & rounded grids.", "Premium"),
                Triple("Detailed", "Formal executive template featuring detailed double-ruled borders & signatures.", "Premium"),
                Triple("Delma Classic", "Premium high-fidelity design modeled on real school reports with subject statistics and trait matrices.", "Premium")
            )

            templates.forEach { (name, desc, tier) ->
                val isSelected = selectedTemplate == name
                val isUnlocked = tier == "Free" || isPremium

                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .height(200.dp)
                        .clickable {
                            if (!isUnlocked) {
                                Toast.makeText(
                                    context,
                                    "'$name' template is locked in Free Plan. Please upgrade in Settings!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                onTemplateSelected(name)
                            }
                        }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) SchoolGreen else if (!isUnlocked) Color.LightGray.copy(alpha = 0.5f) else Color.LightGray,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) SchoolGreen.copy(alpha = 0.05f) else if (!isUnlocked) Color.LightGray.copy(alpha = 0.1f) else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) SchoolGreen else Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Checkmark or Lock
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = SchoolGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (!isUnlocked) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Premium Lock",
                                    tint = SchoolGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Mini visual representation mock of header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                .padding(2.dp)
                        ) {
                            when (name) {
                                "Simple" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.DarkGray))
                                        Box(modifier = Modifier.width(30.dp).height(2.dp).background(Color.LightGray))
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Box(modifier = Modifier.weight(1f).height(12.dp).background(Color.White).border(0.5.dp, Color.LightGray))
                                            Box(modifier = Modifier.weight(1f).height(12.dp).background(Color.White).border(0.5.dp, Color.LightGray))
                                        }
                                    }
                                }
                                "Modern" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(SchoolGreen))
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Box(modifier = Modifier.width(15.dp).height(3.dp).background(SchoolGold))
                                            Box(modifier = Modifier.width(20.dp).height(3.dp).background(SchoolGold))
                                        }
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SchoolGreen.copy(alpha = 0.3f)))
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Box(modifier = Modifier.weight(1f).height(12.dp).background(SchoolGreen.copy(alpha = 0.05f)).border(0.5.dp, SchoolGreen.copy(alpha = 0.2f)))
                                            Box(modifier = Modifier.weight(1f).height(12.dp).background(SchoolGreen.copy(alpha = 0.05f)).border(0.5.dp, SchoolGreen.copy(alpha = 0.2f)))
                                        }
                                    }
                                }
                                "Detailed" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Transparent))
                                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))
                                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Box(modifier = Modifier.weight(1f).height(12.dp).background(Color.White).border(0.5.dp, Color.Black))
                                            Box(modifier = Modifier.weight(1f).height(12.dp).background(Color.White).border(0.5.dp, Color.Black))
                                        }
                                    }
                                }
                                "Delma Classic" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        // Header with logo & passport mock
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(5.dp).background(SchoolGreen))
                                            Box(modifier = Modifier.width(18.dp).height(3.dp).background(Color.DarkGray))
                                            Box(modifier = Modifier.size(5.dp).background(Color.LightGray))
                                        }
                                        // Subheader banner
                                        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(SchoolGreen))
                                        // Meta grid cells
                                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.fillMaxWidth()) {
                                            Box(modifier = Modifier.weight(1f).height(5.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                                            Box(modifier = Modifier.weight(1f).height(5.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                                        }
                                        // Subject list mock
                                        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(SchoolGreen.copy(alpha = 0.2f)))
                                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.fillMaxWidth()) {
                                            Box(modifier = Modifier.weight(1f).height(8.dp).background(Color.White).border(0.2.dp, Color.LightGray))
                                            Box(modifier = Modifier.weight(1f).height(8.dp).background(Color.White).border(0.2.dp, Color.LightGray))
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = desc,
                            fontSize = 8.5.sp,
                            color = Color.DarkGray,
                            lineHeight = 11.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Tier Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.End)
                                .background(
                                    color = if (tier == "Free") Color.Gray.copy(alpha = 0.15f) else SchoolGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tier,
                                color = if (tier == "Free") Color.DarkGray else Color(0xFF8B6508),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 4: GENERATE RESULTS / REPORT CARDS VIEW
// -------------------------------------------------------------
@Composable
fun GenerateResultsScreen(
    viewModel: ResultViewModel,
    records: List<StudentWithScores>,
    settings: SchoolSettings,
    selectedStudentId: Int?
) {
    val context = LocalContext.current
    val currentRecord = records.find { it.student.id == selectedStudentId }

    var selectedTemplate by remember { mutableStateOf(settings.resultTemplate) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Generate Academic Results",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SchoolGreen
            )
            Text(
                text = "Render and export clean, official progress report cards for Nigeria standard grading",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Student selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Student to View Result Card", fontWeight = FontWeight.Bold, color = SchoolGreen)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (records.isEmpty()) {
                        Text("No students are registered. Register students first.", color = Color.Red, fontSize = 12.sp)
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = currentRecord?.student?.fullName ?: "Choose student profile...",
                                    color = if (currentRecord != null) Color.Black else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                records.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r.student.fullName) },
                                        onClick = {
                                            viewModel.selectStudent(r.student.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (currentRecord != null) {
            val s = currentRecord.student
            val positionString = viewModel.getStudentPosition(s, records)

            // Custom Comments Inputs Expandable Card
            item {
                var teacherComment by remember(s.id) { mutableStateOf(s.teacherComment) }
                var principalComment by remember(s.id) { mutableStateOf(s.principalComment) }
                var showCommentsEditor by remember { mutableStateOf(false) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCommentsEditor = !showCommentsEditor },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Edit Teacher & Principal Remarks",
                                fontWeight = FontWeight.Bold,
                                color = SchoolGreen
                            )
                            Icon(
                                imageVector = if (showCommentsEditor) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }

                        if (showCommentsEditor) {
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Auto Comment Generator assist based on grades
                            TextButton(
                                onClick = {
                                    val avg = currentRecord.averageScore
                                    teacherComment = when {
                                        avg >= 70 -> "An outstanding performance. ${s.fullName} consistently shows exceptional brilliance."
                                        avg >= 50 -> "A highly commendable result. Shows strong aptitude but can push higher."
                                        else -> "${s.fullName} has some challenges in academic engagement and requires home tutor assistance."
                                    }
                                    principalComment = when {
                                        avg >= 70 -> "Excellent achievement! Confident promotion to the next class."
                                        avg >= 40 -> "Good progress. Promoted."
                                        else -> "Failed to meet promotion requirements. Repetition strongly advised."
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = SchoolGreen)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto-Generate Remarks from Scores", fontSize = 12.sp)
                            }

                            OutlinedTextField(
                                value = teacherComment,
                                onValueChange = { teacherComment = it },
                                label = { Text("Class Teacher's Remarks") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = principalComment,
                                onValueChange = { principalComment = it },
                                label = { Text("Principal's Endorsement Remarks") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.updateStudent(
                                        s.copy(
                                            teacherComment = teacherComment,
                                            principalComment = principalComment
                                        )
                                    )
                                    Toast.makeText(context, "Official remarks updated successfully!", Toast.LENGTH_SHORT).show()
                                    showCommentsEditor = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen)
                            ) {
                                Text("Save Comments")
                            }
                        }
                    }
                }
            }

            // Beautiful Template Selector Component
            item {
                TemplateSelectorComponent(
                    selectedTemplate = selectedTemplate,
                    isPremium = settings.isPremium,
                    onTemplateSelected = {
                        selectedTemplate = it
                        viewModel.updateResultTemplate(it)
                    }
                )
            }

            // EXPORT AND ACTION BUTTONS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Native PDF Export button
                    Button(
                        onClick = { viewModel.exportResultAsPdf(context, currentRecord) },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Student PDF", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Bulk PDF Export button
                    Button(
                        onClick = { viewModel.exportAllToPdf(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(44.dp)
                            .testTag("bulk_export_pdf_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bulk Export (All PDFs)", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // PREVIEW TEMPLATE CANVAS CARD
            item {
                val densityPadding = when (settings.reportDensity) {
                    "Compact" -> 8.dp
                    "Normal" -> 16.dp
                    "Spacious" -> 24.dp
                    else -> 16.dp
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Gray, RoundedCornerShape(settings.cardCornerRadius.dp)),
                    shape = RoundedCornerShape(settings.cardCornerRadius.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(densityPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (selectedTemplate == "Delma Classic") {
                            DelmaClassicPreview(s, currentRecord, positionString, settings)
                        } else {
                            val activeHeaderBrandColor = if (settings.immersiveMode) {
                                try { Color(settings.brandColorHex.toColorInt()) } catch (e: Exception) { SchoolGreen }
                            } else {
                                SchoolGreen
                            }

                            // Dynamic Alignment School Header
                            when (settings.logoPlacement) {
                                "Left" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AcademicShieldLogo(modifier = Modifier.size(50.dp), logoUri = settings.logoUri)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = settings.schoolName.uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (selectedTemplate == "Modern") activeHeaderBrandColor else Color.Black
                                            )
                                            if (settings.showHeaderAddress) {
                                                Text(text = settings.address, fontSize = 10.sp, color = Color.DarkGray)
                                            }
                                            if (settings.showHeaderContact) {
                                                Text(text = settings.contactInfo, fontSize = 9.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                                "Right" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = settings.schoolName.uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (selectedTemplate == "Modern") activeHeaderBrandColor else Color.Black
                                            )
                                            if (settings.showHeaderAddress) {
                                                Text(text = settings.address, fontSize = 10.sp, color = Color.DarkGray)
                                            }
                                            if (settings.showHeaderContact) {
                                                Text(text = settings.contactInfo, fontSize = 9.sp, color = Color.Gray)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        AcademicShieldLogo(modifier = Modifier.size(50.dp), logoUri = settings.logoUri)
                                    }
                                }
                                "Center" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AcademicShieldLogo(modifier = Modifier.size(54.dp), logoUri = settings.logoUri)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = settings.schoolName.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (selectedTemplate == "Modern") activeHeaderBrandColor else Color.Black,
                                            textAlign = TextAlign.Center
                                        )
                                        if (settings.showHeaderAddress) {
                                            Text(text = settings.address, fontSize = 10.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
                                        }
                                        if (settings.showHeaderContact) {
                                            Text(text = settings.contactInfo, fontSize = 9.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                                "Hidden" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = settings.schoolName.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (selectedTemplate == "Modern") activeHeaderBrandColor else Color.Black
                                        )
                                        if (settings.showHeaderAddress) {
                                            Text(text = settings.address, fontSize = 10.sp, color = Color.DarkGray)
                                        }
                                        if (settings.showHeaderContact) {
                                            Text(text = settings.contactInfo, fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                            
                            // Reliable Custom Divider Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (selectedTemplate == "Detailed") 3.dp else 1.5.dp)
                                    .background(if (selectedTemplate == "Modern") activeHeaderBrandColor else Color.Black)
                                    .padding(vertical = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Student Meta Info Block
                            val studentGpa = remember(currentRecord) {
                                viewModel.calculateStudentGpa(s.studentClass, currentRecord.scores)
                            }
                            if (selectedTemplate == "Simple") {
                                SimpleMetaBlock(s, positionString, settings)
                            } else {
                                ModernMetaBlock(s, positionString, currentRecord.averageScore, settings, studentGpa)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Results Ledger Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (selectedTemplate == "Modern") SchoolGreen else Color.LightGray.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subject Name", fontWeight = FontWeight.Bold, color = if (selectedTemplate == "Modern") Color.White else Color.Black, fontSize = 11.sp, modifier = Modifier.weight(2f))
                                Text("CA", fontWeight = FontWeight.Bold, color = if (selectedTemplate == "Modern") Color.White else Color.Black, fontSize = 11.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                                Text("Exam", fontWeight = FontWeight.Bold, color = if (selectedTemplate == "Modern") Color.White else Color.Black, fontSize = 11.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                                Text("Total", fontWeight = FontWeight.Bold, color = if (selectedTemplate == "Modern") Color.White else Color.Black, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                Text("Grd", fontWeight = FontWeight.Bold, color = if (selectedTemplate == "Modern") Color.White else Color.Black, fontSize = 11.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                Text("Remark", fontWeight = FontWeight.Bold, color = if (selectedTemplate == "Modern") Color.White else Color.Black, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }

                            // Subject Rows
                            if (currentRecord.scores.isEmpty()) {
                                Box(modifier = Modifier.padding(24.dp)) {
                                    Text("No scores registered yet. Tap scores tab to add.", color = Color.Gray, fontSize = 11.sp)
                                }
                            } else {
                                currentRecord.scores.forEachIndexed { idx, sc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (idx % 2 == 1) Color.LightGray.copy(alpha = 0.15f) else Color.White)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sc.subjectName, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(2f))
                                        Text("${sc.caScore.toInt()}", fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                                        Text("${sc.examScore.toInt()}", fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                                        Text("${sc.totalScore.toInt()}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                        Text(sc.grade, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (sc.grade == "F") Color.Red else Color.Black, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                        Text(sc.remark, fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    }
                                }
                            }

                            // Custom Divider
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray).padding(vertical = 12.dp))

                            // Averages summary row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Cumulative score: ${String.format(Locale.US, "%.1f", currentRecord.totalScore)}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Term average percent: ${String.format(Locale.US, "%.2f%%", currentRecord.averageScore)}",
                                        fontWeight = FontWeight.Bold,
                                        color = SchoolGreen,
                                        fontSize = 12.sp
                                    )
                                }

                                // Dynamic Grade system legend badge
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (currentRecord.averageScore >= 40) SchoolGreen.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = if (currentRecord.averageScore >= 40) "PASSED" else "FAIL/RETAKE",
                                        color = if (currentRecord.averageScore >= 40) SchoolGreen else Color.Red,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Official remarks segment
                            if (settings.showTeacherComments || settings.showPrincipalComments) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (settings.showTeacherComments) {
                                        Column {
                                            Text(
                                                "CLASS TEACHER'S ENDORSEMENT",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = activeHeaderBrandColor
                                            )
                                            Text(
                                                text = s.teacherComment.ifBlank { "A satisfactory term of study. Showing solid progress." },
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                    if (settings.showPrincipalComments) {
                                        Column {
                                            Text(
                                                "PRINCIPAL'S REGULATORY STAMP & COMMENT",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = SecondaryBlue
                                            )
                                            Text(
                                                text = s.principalComment.ifBlank { "Approved result. Good standard maintained." },
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }

                            // Dynamic Grading Scale Key
                            if (settings.showGradeScaleKey) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                        .padding(10.dp)
                                ) {
                                    Text("OFFICIAL SCALING KEY", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("A (>=70): Excellent | B (60-69): Very Good | C (50-59): Good | D (45-49): Pass | E (40-44): Fair | F (<40): Fail", fontSize = 8.sp, color = Color.Gray)
                                }
                            }

                            if (settings.showSignatureLines) {
                                Spacer(modifier = Modifier.height(20.dp))

                                // Signatures
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("_______________", fontSize = 10.sp, color = Color.Gray)
                                        Text("Class Teacher Signature", fontSize = 10.sp, color = Color.DarkGray)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("_______________", fontSize = 10.sp, color = Color.Gray)
                                        Text("Principal Seal", fontSize = 10.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleMetaBlock(s: Student, position: String, settings: SchoolSettings = SchoolSettings()) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("STUDENT: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(1f))
            Text(s.fullName.uppercase(), fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(2f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("${settings.studentIdLabel.uppercase()}: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(1f))
            Text(s.studentId, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(2f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("CLASS: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(1f))
            Text("${s.studentClass} | ${s.gender.uppercase()}", fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(2f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("${settings.termLabel.uppercase()}: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(1f))
            Text("${s.term} (${s.session})", fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(2f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("CLASS POSITION: ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(1f))
            Text(position.uppercase(), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(2f))
        }
    }
}

@Composable
fun ModernMetaBlock(
    s: Student,
    position: String,
    average: Double,
    settings: SchoolSettings = SchoolSettings(),
    gpa: Double? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundLight),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.3f)) {
                Text(s.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SchoolGreen)
                Text("Class: ${s.studentClass} | ${settings.studentIdLabel}: ${s.studentId}", fontSize = 10.sp, color = Color.DarkGray)
                Text("${settings.sessionLabel}: ${s.session} | ${settings.termLabel}: ${s.term}", fontSize = 9.sp, color = Color.Gray)
            }
            // Vertical Divider
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray).padding(horizontal = 8.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rank: ", fontSize = 9.sp, color = Color.Gray)
                    Text(position, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SecondaryBlue)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Avg: ", fontSize = 9.sp, color = Color.Gray)
                    Text(String.format(Locale.US, "%.1f%%", average), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SchoolGreen)
                }
                if (gpa != null && gpa > 0.0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("GPA: ", fontSize = 9.sp, color = Color.Gray)
                        Text(String.format(Locale.US, "%.2f", gpa), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFD97706))
                    }
                }
            }
        }
    }
}

@Composable
fun DelmaClassicPreview(
    s: Student,
    record: StudentWithScores,
    position: String,
    settings: SchoolSettings
) {
    val stableRatings = remember(s.studentId) {
        listOf(
            "Punctuality" to getStableRating(s.studentId, 0),
            "Mental Alertness" to getStableRating(s.studentId, 1),
            "Behaviour" to getStableRating(s.studentId, 2),
            "Reliability" to getStableRating(s.studentId, 3),
            "Respect" to getStableRating(s.studentId, 4),
            "Neatness" to getStableRating(s.studentId, 5),
            "Honesty" to getStableRating(s.studentId, 6),
            "Attitude to School" to getStableRating(s.studentId, 7),
            "Self Control" to getStableRating(s.studentId, 8)
        )
    }

    val stablePsychomotor = remember(s.studentId) {
        listOf(
            "Teamwork" to getStableRating(s.studentId, 10),
            "Initiatives" to getStableRating(s.studentId, 11),
            "Handwriting" to getStableRating(s.studentId, 12),
            "Reading" to getStableRating(s.studentId, 13),
            "Creative Arts" to getStableRating(s.studentId, 14),
            "Physical Ed" to getStableRating(s.studentId, 15)
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Double-ruled border visual accent at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(SchoolGreen)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(SchoolGold)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Header matching uploaded layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AcademicShieldLogo(modifier = Modifier.size(64.dp), logoUri = settings.logoUri)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = settings.schoolName.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SchoolGreen,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = settings.address,
                    fontSize = 8.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp
                )
                Text(
                    text = settings.contactInfo,
                    fontSize = 8.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // Student Portrait Passport photo box
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .border(1.dp, Color.LightGray)
                    .background(Color.LightGray.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Passport Portrait",
                        tint = Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("PASSPORT", fontSize = 6.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Subheader Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SchoolGreen)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${s.term.uppercase()} TERMINAL EVALUATION SHEET — JSS SECTION",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Metadata grid table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.LightGray)
        ) {
            // Row 1
            Row(modifier = Modifier.fillMaxWidth()) {
                MetaGridCell("Session", s.session, Modifier.weight(1f))
                MetaGridCell("Term", s.term, Modifier.weight(1f))
                MetaGridCell("Gender", s.gender.uppercase(), Modifier.weight(1f))
            }
            HorizontalDivider(color = Color.LightGray)
            // Row 2
            Row(modifier = Modifier.fillMaxWidth()) {
                MetaGridCell("Name of student", s.fullName.uppercase(), Modifier.weight(1f))
                MetaGridCell("Reg. No", s.studentId, Modifier.weight(1f))
                MetaGridCell("Term ended", "14-Jul-2026", Modifier.weight(1f))
            }
            HorizontalDivider(color = Color.LightGray)
            // Row 3
            Row(modifier = Modifier.fillMaxWidth()) {
                MetaGridCell("Class", s.studentClass.uppercase(), Modifier.weight(1f))
                MetaGridCell("Next term begins", "14-Sep-2026", Modifier.weight(1f))
                MetaGridCell("Teacher", "Mr. Uche", Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // High-fidelity performance stats grid (from PDF)
        Text(
            text = "CLASSROOM PERFORMANCE METRICS",
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = SchoolGreen,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.LightGray)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MetaGridCell("No. of students in class", "${getStableRating(s.studentId, 40, 20, 45)}", Modifier.weight(1f))
                MetaGridCell("Overall total score", String.format(Locale.US, "%.1f", record.totalScore), Modifier.weight(1f))
                MetaGridCell("No. of days school opened", "118", Modifier.weight(1f))
            }
            HorizontalDivider(color = Color.LightGray)
            Row(modifier = Modifier.fillMaxWidth()) {
                MetaGridCell("Student's average score", String.format(Locale.US, "%.2f%%", record.averageScore), Modifier.weight(1f))
                MetaGridCell("Class average score", "54.82%", Modifier.weight(1f))
                MetaGridCell("No. of days present", "113", Modifier.weight(1f))
            }
            HorizontalDivider(color = Color.LightGray)
            Row(modifier = Modifier.fillMaxWidth()) {
                MetaGridCell("Class position", position.uppercase(), Modifier.weight(1f))
                MetaGridCell("Overall performance", if (record.averageScore >= 40) "PASS" else "FAIL", Modifier.weight(1f), valueColor = if (record.averageScore >= 40) SchoolGreen else Color.Red)
                MetaGridCell("No. of days absent", "5", Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Subject Results Table Header (Beautiful teal-green background)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SchoolGreen)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SUBJECT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 9.sp, modifier = Modifier.weight(2f))
            Text("TEST 1 (20)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
            Text("TEST 2 (20)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
            Text("EXAM (60)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
            Text("TOTAL", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
            Text("GRADE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
            Text("REMARK", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 9.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
        }

        if (record.scores.isEmpty()) {
            Box(modifier = Modifier.padding(24.dp)) {
                Text("No scores registered yet. Tap scores tab to add.", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            record.scores.forEachIndexed { idx, sc ->
                val t1 = String.format(Locale.US, "%.1f", sc.caScore * 0.5)
                val t2 = String.format(Locale.US, "%.1f", sc.caScore * 0.5)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (idx % 2 == 1) Color.LightGray.copy(alpha = 0.15f) else Color.White)
                        .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(sc.subjectName.uppercase(), fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(2f), fontWeight = FontWeight.SemiBold)
                    Text(t1, fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                    Text(t2, fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                    Text("${sc.examScore.toInt()}", fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                    Text("${sc.totalScore.toInt()}", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                    Text(sc.grade, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = if (sc.grade == "F") Color.Red else SchoolGreen, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                    Text(sc.remark, fontSize = 8.5.sp, color = Color.DarkGray, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Side-by-side matrices (Affective Traits & Psychomotor)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Affective Traits Table
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SchoolGreen)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AFFECTIVE TRAITS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                }
                
                Column(modifier = Modifier.border(1.dp, Color.LightGray)) {
                    Row(
                        modifier = Modifier.background(Color.LightGray.copy(alpha = 0.3f)).padding(3.dp)
                    ) {
                        Text("TRAIT", fontWeight = FontWeight.Bold, fontSize = 7.5.sp, modifier = Modifier.weight(1.5f))
                        Text("RATING", fontWeight = FontWeight.Bold, fontSize = 7.5.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                    stableRatings.forEach { (trait, rating) ->
                        HorizontalDivider(color = Color.LightGray)
                        Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 3.dp)) {
                            Text(trait, fontSize = 7.5.sp, modifier = Modifier.weight(1.5f), color = Color.DarkGray)
                            Text("$rating", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = SchoolGreen)
                        }
                    }
                }
            }

            // Psychomotor Skills Table
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SecondaryBlue)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("PSYCHOMOTOR SKILLS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                }
                
                Column(modifier = Modifier.border(1.dp, Color.LightGray)) {
                    Row(
                        modifier = Modifier.background(Color.LightGray.copy(alpha = 0.3f)).padding(3.dp)
                    ) {
                        Text("SKILL", fontWeight = FontWeight.Bold, fontSize = 7.5.sp, modifier = Modifier.weight(1.5f))
                        Text("RATING", fontWeight = FontWeight.Bold, fontSize = 7.5.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                    stablePsychomotor.forEach { (skill, rating) ->
                        HorizontalDivider(color = Color.LightGray)
                        Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 3.dp)) {
                            Text(skill, fontSize = 7.5.sp, modifier = Modifier.weight(1.5f), color = Color.DarkGray)
                            Text("$rating", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = SecondaryBlue)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom endorsements with circular Red APPROVED stamp!
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Text(
                    "CLASS TEACHER'S REPORT & ASSESSMENT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    color = SchoolGreen
                )
                Text(
                    text = s.teacherComment.ifBlank { "Samuel is a hardworking student and always trying his best. Showing solid terminal progress." },
                    fontSize = 9.sp,
                    color = Color.DarkGray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "PRINCIPAL'S REGULATORY ENDORSEMENT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    color = SecondaryBlue
                )
                Text(
                    text = s.principalComment.ifBlank { "Approved result. Satisfactory standard of cognitive development maintained." },
                    fontSize = 9.sp,
                    color = Color.DarkGray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // Red Approved Seal Stamp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-8).dp, y = (8).dp)
                    .size(64.dp)
                    .rotate(-15f)
                    .border(1.5.dp, Color(0xFFC53030).copy(alpha = 0.8f), CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "DELMA SCHOOL",
                        fontSize = 5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC53030).copy(alpha = 0.8f)
                    )
                    Text(
                        "APPROVED",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFC53030).copy(alpha = 0.8f)
                    )
                    Text(
                        "OFFICIAL SEAL",
                        fontSize = 5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC53030).copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Signatures row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("_______________", fontSize = 9.sp, color = Color.Gray)
                Text("Class Teacher Signature", fontSize = 8.sp, color = Color.DarkGray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("_______________", fontSize = 9.sp, color = Color.Gray)
                Text("Principal Seal & Stamp", fontSize = 8.sp, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun MetaGridCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Black
) {
    Column(
        modifier = modifier
            .padding(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun getStableRating(studentId: String, index: Int, min: Int = 3, max: Int = 5): Int {
    val hash = (studentId + index).hashCode()
    return min + Math.abs(hash % (max - min + 1))
}

// -------------------------------------------------------------
// SCREEN 5: SETTINGS & SCHOOL BRANDING CONFIG
// -------------------------------------------------------------
@Composable
fun SettingsScreen(
    viewModel: ResultViewModel,
    settings: SchoolSettings,
    studentsCount: Int
) {
    val context = LocalContext.current

    var schoolName by remember { mutableStateOf(settings.schoolName) }
    var address by remember { mutableStateOf(settings.address) }
    var contactInfo by remember { mutableStateOf(settings.contactInfo) }
    var resultTemplate by remember { mutableStateOf(settings.resultTemplate) }
    var logoUriState by remember { mutableStateOf(settings.logoUri ?: "default") }
    var immersiveMode by remember { mutableStateOf(settings.immersiveMode) }
    var brandColorHexState by remember { mutableStateOf(settings.brandColorHex) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & Customization",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SchoolGreen
            )
            Text(
                text = "Manage school identity details, branding styles, and plan licensing",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Licensing / Premium Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.isPremium) SchoolGreen else Color(0xFF1E2622)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (settings.isPremium) "SmartResult Premium Active" else "SmartResult Free Plan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Icon(
                            imageVector = if (settings.isPremium) Icons.Default.CheckCircle else Icons.Default.Star,
                            contentDescription = null,
                            tint = SchoolGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (settings.isPremium) {
                            "Thank you for upgrading! You have unlocked unlimited students, PDF exports, bulk PDF exporting, bulk excel importing, and premium report designs."
                        } else {
                            "Running in standard trial mode. Maximum 20 students. Simple templates and exports are limited. Toggle premium mode below to try all features."
                        },
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.togglePremium()
                            Toast.makeText(
                                context,
                                if (settings.isPremium) "Reverted to Free Version." else "Upgraded to PREMIUM NG License!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settings.isPremium) Color.White else SchoolGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("premium_upgrade_button")
                    ) {
                        Text(
                            text = if (settings.isPremium) "Downgrade to Free" else "Upgrade / Unlock Premium",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Cloud Firestore Active Database Link
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Cloud Firestore Database Link",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.DarkGray
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Synchronize school-wide student profiles, subject registries, and grading scale parameters with your active Google Firestore cloud database.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Schema Version: v2.0.0",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Link: ${settings.firestoreStatus}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    settings.firestoreStatus.contains("Connected") -> SchoolGreen
                                    settings.firestoreStatus.contains("Sandbox") -> SecondaryBlue
                                    else -> Color.Gray
                                }
                            )
                        }
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.CloudSync) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Database Console", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Report Template Customizer Designer Link
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SchoolGold.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = SchoolGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Visual Report Template Designer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.DarkGray
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Customize report layouts, logo placements, custom header fields, and enable/disable sections (grading key, signature lines, and official comments) dynamically.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.TemplateEditor) },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("open_template_designer_button")
                    ) {
                        Text("Open Visual Template Editor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        // Custom Grading Scales & GPA Weightings Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Custom Grading Scales & GPA Weightings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.DarkGray
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Define custom score brackets (e.g. A=70-100, B=60-69), assign grade points, and configure GPA weighting models across Primary, JSS, and SS levels.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.GradingScaleConfig) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("open_grading_scales_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configure Grading & GPA Scales", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // School Branding Profile
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "School Branding & Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SchoolGreen
                    )

                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("School Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("School Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = contactInfo,
                        onValueChange = { contactInfo = it },
                        label = { Text("Contact Info (Phone/Email)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "School Logo / Emblem Customizer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SchoolGreen
                    )
                    
                    // Selected logo preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AcademicShieldLogo(
                            modifier = Modifier.size(60.dp),
                            logoUri = logoUriState
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Current Logo Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                            val desc = when {
                                logoUriState == "default" || logoUriState == "mascot" -> "Official SmartResult Mascot Badge"
                                logoUriState.startsWith("crest:") -> {
                                    val t = logoUriState.substringAfter("crest:")
                                    t.replace("_", " ").uppercase() + " PRESET"
                                }
                                logoUriState.startsWith("custom:") -> "Custom School Initials Crest"
                                else -> "Default"
                            }
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Selection Mode Tabs
                    var logoCategory by remember { 
                        mutableStateOf(
                            when {
                                logoUriState == "default" || logoUriState == "mascot" -> 0
                                logoUriState.startsWith("crest:") -> 1
                                logoUriState.startsWith("custom:") -> 2
                                else -> 3
                            }
                        ) 
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Default Mascot", "Classic Presets", "Initials Maker", "Upload JPG").forEachIndexed { index, label ->
                            val isSel = logoCategory == index
                            ElevatedButton(
                                onClick = { 
                                    logoCategory = index
                                    if (index == 0) {
                                        logoUriState = "default"
                                    } else if (index == 1) {
                                        logoUriState = "crest:shield_green"
                                    } else if (index == 2) {
                                        logoUriState = "custom:SCH|circle|#0D5C34"
                                    }
                                },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = if (isSel) SchoolGreen else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSel) Color.White else Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    when (logoCategory) {
                        0 -> {
                            Text(
                                text = "Uses the premium high-resolution SmartResult NG logo.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Select an elegant predefined emblem template:",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                                val presets = listOf(
                                    "crest:shield_green" to "Shield",
                                    "crest:cap_blue" to "Classic Cap",
                                    "crest:book_maroon" to "Book",
                                    "crest:torch_purple" to "Torch",
                                    "crest:laurel_teal" to "Laurel"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    presets.forEach { (presetUri, name) ->
                                        val isSel = logoUriState == presetUri
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .border(
                                                    width = if (isSel) 2.dp else 1.dp,
                                                    color = if (isSel) SchoolGreen else Color.LightGray,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .background(if (isSel) SchoolGreen.copy(alpha = 0.08f) else Color.Transparent)
                                                .clickable { logoUriState = presetUri }
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                AcademicShieldLogo(modifier = Modifier.size(24.dp), logoUri = presetUri)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(name, fontSize = 7.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Custom builder form
                            // custom:INITIALS|SHAPE|COLOR_HEX
                            var customInitials by remember { 
                                mutableStateOf(
                                    if (logoUriState.startsWith("custom:")) logoUriState.substringAfter("custom:").split("|").getOrNull(0) ?: "SCH" else "SCH"
                                ) 
                            }
                            var customShape by remember { 
                                mutableStateOf(
                                    if (logoUriState.startsWith("custom:")) logoUriState.substringAfter("custom:").split("|").getOrNull(1) ?: "circle" else "circle"
                                ) 
                            }
                            var customColorHex by remember { 
                                mutableStateOf(
                                    if (logoUriState.startsWith("custom:")) logoUriState.substringAfter("custom:").split("|").getOrNull(2) ?: "#0D5C34" else "#0D5C34"
                                ) 
                            }

                            // Helper function to update state string
                            val updateCustomLogo = { init: String, shp: String, col: String ->
                                logoUriState = "custom:$init|$shp|$col"
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customInitials,
                                    onValueChange = {
                                        if (it.length <= 3) {
                                            customInitials = it
                                            updateCustomLogo(it, customShape, customColorHex)
                                        }
                                    },
                                    label = { Text("School Initials (Max 3 Characters)", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Shape:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                                    listOf("circle", "shield", "square").forEach { shp ->
                                        val isSel = customShape == shp
                                        OutlinedButton(
                                            onClick = {
                                                customShape = shp
                                                updateCustomLogo(customInitials, shp, customColorHex)
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isSel) SchoolGreen.copy(alpha = 0.1f) else Color.Transparent,
                                                contentColor = if (isSel) SchoolGreen else Color.Black
                                            ),
                                            border = BorderStroke(1.dp, if (isSel) SchoolGreen else Color.LightGray),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(shp.uppercase(), fontSize = 9.sp)
                                        }
                                    }
                                }

                                Text("Brand Color Scheme:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                val colors = listOf(
                                    "#0D5C34" to "Green",
                                    "#1A365D" to "Navy",
                                    "#800020" to "Maroon",
                                    "#4A154B" to "Purple",
                                    "#D4AF37" to "Gold",
                                    "#C53030" to "Crimson",
                                    "#319795" to "Teal"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    colors.forEach { (hex, name) ->
                                        val isSel = customColorHex == hex
                                        val parseColor = Color(hex.toColorInt())
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(parseColor)
                                                .border(
                                                    width = if (isSel) 2.dp else 0.dp,
                                                    color = if (isSel) Color.White else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .border(1.dp, Color.Gray, CircleShape)
                                                .clickable {
                                                    customColorHex = hex
                                                    updateCustomLogo(customInitials, customShape, hex)
                                                }
                                        )
                                    }
                                }
                            }
                        }
                        3 -> {
                            val context = LocalContext.current
                            val pickerLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.GetContent()
                            ) { uri ->
                                if (uri != null) {
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val file = java.io.File(context.filesDir, "custom_school_logo.jpg")
                                        val outputStream = java.io.FileOutputStream(file)
                                        inputStream?.copyTo(outputStream)
                                        inputStream?.close()
                                        outputStream.close()
                                        logoUriState = file.absolutePath
                                        Toast.makeText(context, "Logo uploaded successfully!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Upload a custom JPG or PNG school logo image directly from your local storage to use across reports and dashboards.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                
                                Button(
                                    onClick = {
                                        pickerLauncher.launch("image/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Choose Image / JPG", color = Color.White)
                                }
                                
                                if (logoUriState.startsWith("/") || logoUriState.startsWith("file:") || logoUriState.startsWith("content:")) {
                                    Text(
                                        text = "Custom file loaded: ${logoUriState.substringAfterLast("/")}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SchoolGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Immersive School Portal Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SchoolGreen
                            )
                            Text(
                                text = "Transform UI headers, sidebars, cards, and reports with school-branded styling.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = immersiveMode,
                            onCheckedChange = { immersiveMode = it },
                            modifier = Modifier.testTag("immersive_mode_switch")
                        )
                    }

                    if (immersiveMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Primary Brand Color Theme:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SchoolGreen)
                        val brandColors = listOf(
                            "#0F5231" to "Majestic Forest",
                            "#1E3A8A" to "Academic Navy",
                            "#800020" to "Bordeaux Maroon",
                            "#4A154B" to "Scholastic Purple",
                            "#D4AF37" to "Prestige Gold",
                            "#C53030" to "Crimson Red",
                            "#005A9C" to "Royal Blue"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            brandColors.forEach { (hex, name) ->
                                val isSelectedColor = brandColorHexState.equals(hex, ignoreCase = true)
                                val col = Color(hex.toColorInt())
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(
                                            width = if (isSelectedColor) 3.dp else 1.dp,
                                            color = if (isSelectedColor) Color.White else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { brandColorHexState = hex }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.updateSchoolSettings(
                                schoolName = schoolName,
                                address = address,
                                contactInfo = contactInfo,
                                resultTemplate = resultTemplate,
                                logoUri = logoUriState,
                                immersiveMode = immersiveMode,
                                brandColorHex = brandColorHexState
                            )
                            Toast.makeText(context, "School identity profile saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save School Profile & Logo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Database clear action
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Danger Zone", fontWeight = FontWeight.Bold, color = Color.Red, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Deletes all registered students and scores from the offline storage.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var showConfirm by remember { mutableStateOf(false) }
                    if (showConfirm) {
                        AlertDialog(
                            onDismissRequest = { showConfirm = false },
                            title = { Text("Clear All Database Records?") },
                            text = { Text("This will permanently delete all students and subjects. This cannot be undone.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.clearAllStudents()
                                    Toast.makeText(context, "Database cleared successfully.", Toast.LENGTH_SHORT).show()
                                    showConfirm = false
                                }) {
                                    Text("Yes, Clear", color = Color.Red)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfirm = false }) {
                                    Text("No, Cancel")
                                }
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = { showConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Erase Database Ledger")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN: SPREADSHEET LEDGER GRID
// -------------------------------------------------------------
@Composable
fun LedgerTableScreen(
    viewModel: ResultViewModel,
    students: List<Student>,
    studentsWithScores: List<StudentWithScores>,
    settings: SchoolSettings
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var classFilter by remember { mutableStateOf("All Classes") }
    var selectedSubjectFilter by remember { mutableStateOf("All Subjects") }

    val ledgerEntries = remember(studentsWithScores, searchQuery, classFilter, selectedSubjectFilter) {
        val list = mutableListOf<LedgerRowData>()
        studentsWithScores.forEach { record ->
            val s = record.student
            val matchesSearch = s.fullName.contains(searchQuery, ignoreCase = true) || 
                                s.studentId.contains(searchQuery, ignoreCase = true)
            val matchesClass = classFilter == "All Classes" || s.studentClass.equals(classFilter, ignoreCase = true)

            if (matchesSearch && matchesClass) {
                if (record.scores.isEmpty()) {
                    list.add(
                        LedgerRowData(
                            studentId = s.id,
                            studentName = s.fullName,
                            studentIdStr = s.studentId,
                            studentClass = s.studentClass,
                            scoreId = null,
                            subjectName = "",
                            caScore = 0.0,
                            examScore = 0.0,
                            isEmpty = true
                        )
                    )
                } else {
                    record.scores.forEach { score ->
                        val matchesSubject = selectedSubjectFilter == "All Subjects" || 
                                             score.subjectName.equals(selectedSubjectFilter, ignoreCase = true)
                        if (matchesSubject) {
                            list.add(
                                LedgerRowData(
                                    studentId = s.id,
                                    studentName = s.fullName,
                                    studentIdStr = s.studentId,
                                    studentClass = s.studentClass,
                                    scoreId = score.id,
                                    subjectName = score.subjectName,
                                    caScore = score.caScore,
                                    examScore = score.examScore,
                                    isEmpty = false
                                )
                            )
                        }
                    }
                }
            }
        }
        list
    }

    val classesList = listOf("All Classes") + students.map { it.studentClass }.distinct().sorted()
    val subjectsList = listOf("All Subjects") + studentsWithScores.flatMap { it.scores.map { it.subjectName } }.distinct().sorted()

    var showAddStudentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = SchoolGreen.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, SchoolGreen.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Academic Spreadsheet Ledger",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SchoolGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "React-style spreadsheet grid for rapid data input. Click on any cell to edit profiles or post student subject marks instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = SchoolGreen,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name/ID...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            var classExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { classExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(classFilter, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                    classesList.forEach { cls ->
                        DropdownMenuItem(
                            text = { Text(cls, fontSize = 12.sp) },
                            onClick = {
                                classFilter = cls
                                classExpanded = false
                            }
                        )
                    }
                }
            }

            var subjectExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { subjectExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(selectedSubjectFilter, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                    subjectsList.forEach { sub ->
                        DropdownMenuItem(
                            text = { Text(sub, fontSize = 12.sp) },
                            onClick = {
                                selectedSubjectFilter = sub
                                subjectExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddStudentDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Register Student Row", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    if (students.isNotEmpty()) {
                        val firstStudent = students.first()
                        viewModel.saveSubjectScore(
                            studentId = firstStudent.id,
                            subjectName = "Mathematics",
                            caScore = 0.0,
                            examScore = 0.0,
                            onSuccess = {
                                Toast.makeText(context, "Added blank ledger subject row for ${firstStudent.fullName}!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "Register at least one student first!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Blank Score Cell", fontSize = 11.sp)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            if (ledgerEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No matching ledger rows found.", color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add student records or adjust your filters above.", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            } else {
                val horizontalScrollState = rememberScrollState()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    Row(
                        modifier = Modifier
                            .background(SchoolGreen)
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("STUDENT PROFILE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(180.dp).padding(start = 12.dp))
                        Text("CLASS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
                        Text("SUBJECT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(150.dp))
                        Text("CA (40)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
                        Text("EXAM (60)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
                        Text("TOTAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                        Text("GRADE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                        Text("ACTIONS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                    ) {
                        items(ledgerEntries) { rowData ->
                            LedgerItemRow(rowData = rowData, viewModel = viewModel, allStudents = students)
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    if (showAddStudentDialog) {
        var name by remember { mutableStateOf("") }
        var idStr by remember { mutableStateOf("") }
        var gender by remember { mutableStateOf("Male") }
        var selectedClass by remember { mutableStateOf("Creche") }
        val nigerianClasses = listOf(
            "Creche", "Pre-Nursery", "Nursery 1", "Nursery 2",
            "Primary 1", "Primary 2", "Primary 3", "Primary 4", "Primary 5", "Primary 6",
            "JSS 1", "JSS 2", "JSS 3", "SS 1", "SS 2", "SS 3"
        )

        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = { Text("Register New Student") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = idStr,
                        onValueChange = { idStr = it },
                        label = { Text("Student ID / Reg No") },
                        placeholder = { Text("e.g. STU/2026/015") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gender:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
                            Text("Male")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
                            Text("Female")
                        }
                    }

                    var clsExp by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { clsExp = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Class: $selectedClass")
                        }
                        DropdownMenu(expanded = clsExp, onDismissRequest = { clsExp = false }) {
                            nigerianClasses.forEach { cls ->
                                DropdownMenuItem(
                                    text = { Text(cls) },
                                    onClick = {
                                        selectedClass = cls
                                        clsExp = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || idStr.isBlank()) {
                            Toast.makeText(context, "All fields are required!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addStudent(
                            fullName = name,
                            studentId = idStr,
                            gender = gender,
                            studentClass = selectedClass,
                            term = "3rd Term",
                            session = "2025/2026",
                            onSuccess = {
                                Toast.makeText(context, "Student registered successfully in Ledger!", Toast.LENGTH_SHORT).show()
                                showAddStudentDialog = false
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen)
                ) {
                    Text("Register")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class LedgerRowData(
    val studentId: Int,
    val studentName: String,
    val studentIdStr: String,
    val studentClass: String,
    val scoreId: Int?,
    val subjectName: String,
    val caScore: Double,
    val examScore: Double,
    val isEmpty: Boolean
)

@Composable
fun LedgerItemRow(
    rowData: LedgerRowData,
    viewModel: ResultViewModel,
    allStudents: List<Student>
) {
    val context = LocalContext.current
    
    var subjectInput by remember(rowData.scoreId, rowData.studentId) { mutableStateOf(rowData.subjectName) }
    var caInput by remember(rowData.scoreId, rowData.studentId) { mutableStateOf(if (rowData.isEmpty) "" else rowData.caScore.toString()) }
    var examInput by remember(rowData.scoreId, rowData.studentId) { mutableStateOf(if (rowData.isEmpty) "" else rowData.examScore.toString()) }

    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(rowData.studentName) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(180.dp)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            if (isEditingName) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val s = allStudents.find { it.id == rowData.studentId }
                                if (s != null && editedName.isNotBlank()) {
                                    viewModel.updateStudent(s.copy(fullName = editedName.trim()))
                                    isEditingName = false
                                    Toast.makeText(context, "Student profile renamed!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save Name", tint = SchoolGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isEditingName = true }
                ) {
                    Text(
                        text = rowData.studentName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(text = rowData.studentIdStr, fontSize = 10.sp, color = Color.Gray)
            }
        }

        Text(
            text = rowData.studentClass,
            fontSize = 11.sp,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center,
            color = Color.DarkGray
        )

        Box(modifier = Modifier.width(150.dp).padding(end = 8.dp)) {
            OutlinedTextField(
                value = subjectInput,
                onValueChange = { subjectInput = it },
                singleLine = true,
                placeholder = { Text("e.g. Mathematics", fontSize = 11.sp) },
                textStyle = TextStyle(fontSize = 11.sp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            )
        }

        Box(modifier = Modifier.width(70.dp).padding(horizontal = 4.dp)) {
            OutlinedTextField(
                value = caInput,
                onValueChange = { caInput = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            )
        }

        Box(modifier = Modifier.width(80.dp).padding(horizontal = 4.dp)) {
            OutlinedTextField(
                value = examInput,
                onValueChange = { examInput = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            )
        }

        val caVal = caInput.toDoubleOrNull() ?: 0.0
        val examVal = examInput.toDoubleOrNull() ?: 0.0
        val totalScoreVal = caVal + examVal
        val evaluatedBand = viewModel.evaluateScoreForClass(rowData.studentClass, totalScoreVal)
        val currentGrade = evaluatedBand.grade

        Text(
            text = totalScoreVal.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = SchoolGreen,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = currentGrade,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = parseHexColor(evaluatedBand.colorHex),
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.width(100.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (subjectInput.isBlank()) {
                        Toast.makeText(context, "Subject name is required!", Toast.LENGTH_SHORT).show()
                        return@IconButton
                    }
                    viewModel.saveSubjectScore(
                        studentId = rowData.studentId,
                        subjectName = subjectInput,
                        caScore = caVal,
                        examScore = examVal,
                        onSuccess = {
                            Toast.makeText(context, "Posted score successfully!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Post Score", tint = SchoolGreen, modifier = Modifier.size(20.dp))
            }

            IconButton(
                onClick = {
                    if (rowData.scoreId != null) {
                        viewModel.deleteScore(SubjectScore(id = rowData.scoreId, studentId = rowData.studentId, subjectName = rowData.subjectName, caScore = rowData.caScore, examScore = rowData.examScore))
                        Toast.makeText(context, "Score entry deleted from Ledger!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No score saved to delete.", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete score", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// CLOUD FIRESTORE ACTIVE SYNC SCREEN & SCHEMA EXPLORER
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: ResultViewModel,
    students: List<Student>,
    studentsWithScores: List<StudentWithScores>,
    settings: SchoolSettings
) {
    val context = LocalContext.current
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()

    var apiKeyInput by remember { mutableStateOf(settings.firestoreApiKey) }
    var projectIdInput by remember { mutableStateOf(settings.firestoreProjectId) }
    var appIdInput by remember { mutableStateOf(settings.firestoreAppId) }

    var activeTab by remember { mutableStateOf("SCHEMA") } // SCHEMA, CONFIG, TERMINAL
    var schemaTab by remember { mutableStateOf("STUDENTS") } // STUDENTS, SUBJECTS, GRADING

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = SchoolGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cloud Firestore Hub", fontWeight = FontWeight.Bold, color = SchoolGreen)
                    }
                },
                actions = {
                    // Quick Back Button
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Dashboard) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SchoolGreen)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Navigation Tabs
            TabRow(
                selectedTabIndex = when (activeTab) {
                    "SCHEMA" -> 0
                    "CONFIG" -> 1
                    else -> 2
                },
                containerColor = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = activeTab == "SCHEMA",
                    onClick = { activeTab = "SCHEMA" },
                    text = { Text("1. Data Schema", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == "CONFIG",
                    onClick = { activeTab = "CONFIG" },
                    text = { Text("2. Active Link", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == "TERMINAL",
                    onClick = { activeTab = "TERMINAL" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("3. Live Console", fontWeight = FontWeight.Bold)
                            if (syncState == "CONNECTING" || syncState == "UPLOADING") {
                                Spacer(modifier = Modifier.width(6.dp))
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = SchoolGreen)
                            }
                        }
                    }
                )
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    "SCHEMA" -> SchemaExplorerView(schemaTab, onTabChange = { schemaTab = it })
                    "CONFIG" -> ConfigView(
                        apiKey = apiKeyInput,
                        projectId = projectIdInput,
                        appId = appIdInput,
                        onApiKeyChange = { apiKeyInput = it },
                        onProjectIdChange = { projectIdInput = it },
                        onAppIdChange = { appIdInput = it },
                        onSave = {
                            viewModel.updateFirestoreConfig(apiKeyInput, projectIdInput, appIdInput)
                            Toast.makeText(context, "Credentials saved to School Settings!", Toast.LENGTH_SHORT).show()
                        },
                        onConnectReal = {
                            viewModel.runFirestoreSync(context, isSimulation = false)
                            activeTab = "TERMINAL"
                        },
                        onConnectDemo = {
                            viewModel.runFirestoreSync(context, isSimulation = true)
                            activeTab = "TERMINAL"
                        },
                        settings = settings
                    )
                    "TERMINAL" -> TerminalConsoleView(
                        syncState = syncState,
                        logs = syncLogs,
                        onTriggerSync = {
                            val useDemo = settings.firestoreApiKey.isBlank() || settings.firestoreProjectId.isBlank()
                            viewModel.runFirestoreSync(context, isSimulation = useDemo)
                        },
                        onClear = {
                            viewModel.runFirestoreSync(context, isSimulation = true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SchemaExplorerView(activeSchemaTab: String, onTabChange: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color.LightGray),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Firestore Document Model Schemas",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.DarkGray
            )
            Text(
                text = "Explore the hierarchical JSON structures mapped from Room SQL entities to Firestore collections.",
                fontSize = 12.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(14.dp))

            // Schema Sub-tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("STUDENTS" to "students", "SUBJECTS" to "subjects", "GRADING" to "grading_scales").forEach { (tab, label) ->
                    val isSelected = activeSchemaTab == tab
                    Surface(
                        onClick = { onTabChange(tab) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) SchoolGreen.copy(alpha = 0.12f) else Color(0xFFF1F3F4),
                        border = if (isSelected) BorderStroke(1.dp, SchoolGreen) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "/$label",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) SchoolGreen else Color.DarkGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selected Schema View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DOCUMENT STRUCTURE SCHEMA", color = Color(0xFF858585), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("JSON / Map", color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    when (activeSchemaTab) {
                        "STUDENTS" -> {
                            JsonLine("{", 0, Color.White)
                            JsonLine("\"collection\": \"students\",", 1, Color(0xFF6A9955))
                            JsonLine("\"documentId\": \"student_{studentId}\",", 1, Color(0xFF9CDCFE))
                            JsonLine("\"fields\": {", 1, Color.White)
                            JsonLine("\"studentId\": \"STU/2026/001\",", 2, Color(0xFFCE9178))
                            JsonLine("\"fullName\": \"Chinedu Okeke\",", 2, Color(0xFFCE9178))
                            JsonLine("\"gender\": \"Male\",", 2, Color(0xFFCE9178))
                            JsonLine("\"studentClass\": \"JSS 1\",", 2, Color(0xFFCE9178))
                            JsonLine("\"term\": \"3rd Term\",", 2, Color(0xFFCE9178))
                            JsonLine("\"session\": \"2025/2026\",", 2, Color(0xFFCE9178))
                            JsonLine("\"attendancePresent\": 58,", 2, Color(0xFFB5CEA8))
                            JsonLine("\"attendanceTotal\": 60,", 2, Color(0xFFB5CEA8))
                            JsonLine("\"teacherComment\": \"Outstanding brilliant student.\",", 2, Color(0xFFCE9178))
                            JsonLine("\"principalComment\": \"Excellent results kept.\",", 2, Color(0xFFCE9178))
                            JsonLine("\"totalScore\": 375.0,", 2, Color(0xFFB5CEA8))
                            JsonLine("\"averageScore\": 75.0,", 2, Color(0xFFB5CEA8))
                            JsonLine("\"lastSyncedAt\": 1784291040854,", 2, Color(0xFFB5CEA8))
                            JsonLine("\"scores\": [", 2, Color.White)
                            JsonLine("{", 3, Color.White)
                            JsonLine("\"subjectName\": \"Mathematics\",", 4, Color(0xFFCE9178))
                            JsonLine("\"caScore\": 28.5,", 4, Color(0xFFB5CEA8))
                            JsonLine("\"examScore\": 65.0,", 4, Color(0xFFB5CEA8))
                            JsonLine("\"totalScore\": 93.5,", 4, Color(0xFFB5CEA8))
                            JsonLine("\"grade\": \"A\",", 4, Color(0xFFCE9178))
                            JsonLine("\"remark\": \"Excellent\"", 4, Color(0xFFCE9178))
                            JsonLine("}, ...", 3, Color.White)
                            JsonLine("]", 2, Color.White)
                            JsonLine("}", 1, Color.White)
                            JsonLine("}", 0, Color.White)
                        }
                        "SUBJECTS" -> {
                            JsonLine("{", 0, Color.White)
                            JsonLine("\"collection\": \"subjects\",", 1, Color(0xFF6A9955))
                            JsonLine("\"documentId\": \"sub_mathematics\",", 1, Color(0xFF9CDCFE))
                            JsonLine("\"fields\": {", 1, Color.White)
                            JsonLine("\"subjectName\": \"Mathematics\",", 2, Color(0xFFCE9178))
                            JsonLine("\"category\": \"Core Curriculum\",", 2, Color(0xFFCE9178))
                            JsonLine("\"description\": \"Official Nigerian Mathematics syllabus core course.\",", 2, Color(0xFFCE9178))
                            JsonLine("\"recommendedClass\": \"All Classes\"", 2, Color(0xFFCE9178))
                            JsonLine("}", 1, Color.White)
                            JsonLine("}", 0, Color.White)
                        }
                        "GRADING" -> {
                            JsonLine("{", 0, Color.White)
                            JsonLine("\"collection\": \"grading_scales\",", 1, Color(0xFF6A9955))
                            JsonLine("\"documentId\": \"scale_A\",", 1, Color(0xFF9CDCFE))
                            JsonLine("\"fields\": {", 1, Color.White)
                            JsonLine("\"grade\": \"A\",", 2, Color(0xFFCE9178))
                            JsonLine("\"minScore\": 70.0,", 2, Color(0xFFB5CEA8))
                            JsonLine("\"maxScore\": 100.0,", 2, Color(0xFFB5CEA8))
                            JsonLine("\"remark\": \"Excellent\",", 2, Color(0xFFCE9178))
                            JsonLine("\"description\": \"Highest academic standard (Distinction level)\"", 2, Color(0xFFCE9178))
                            JsonLine("}", 1, Color.White)
                            JsonLine("}", 0, Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JsonLine(text: String, indent: Int, valueColor: Color) {
    val indentSpaces = "  ".repeat(indent)
    val keyColor = Color(0xFF9CDCFE)
    
    val parts = text.split(":")
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = indentSpaces,
            style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF858585))
        )
        if (parts.size > 1 && parts[0].startsWith("\"")) {
            Text(
                text = parts[0] + ":",
                color = keyColor,
                style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp)
            )
            Text(
                text = parts.subList(1, parts.size).joinToString(":"),
                color = valueColor,
                style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp)
            )
        } else {
            Text(
                text = text,
                color = valueColor,
                style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp)
            )
        }
    }
}

@Composable
fun ConfigView(
    apiKey: String,
    projectId: String,
    appId: String,
    onApiKeyChange: (String) -> Unit,
    onProjectIdChange: (String) -> Unit,
    onAppIdChange: (String) -> Unit,
    onSave: () -> Unit,
    onConnectReal: () -> Unit,
    onConnectDemo: () -> Unit,
    settings: SchoolSettings
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color.LightGray),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Link Active Database Project",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.DarkGray
            )
            Text(
                text = "Enter your Google Firebase Cloud Firestore configurations below to link this application to your real active production database.",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("Web API Key") },
                placeholder = { Text("AIzaSy...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = projectId,
                onValueChange = onProjectIdChange,
                label = { Text("Project ID") },
                placeholder = { Text("smartresult-97a61") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = appId,
                onValueChange = onAppIdChange,
                label = { Text("Application ID (App ID)") },
                placeholder = { Text("1:901041203:android:9d4b00ca36e522") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Config")
                }

                Button(
                    onClick = onConnectReal,
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolGreen),
                    modifier = Modifier.weight(1.2f),
                    enabled = apiKey.isNotBlank() && projectId.isNotBlank()
                ) {
                    Text("Sync Live DB")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Safe Sandbox Connector
            Card(
                colors = CardDefaults.cardColors(containerColor = SecondaryBlue.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, SecondaryBlue.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Firestore Sandbox (Simulation Link)", fontWeight = FontWeight.Bold, color = SecondaryBlue, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Don't have your own Firebase project credentials? Test our fully operational Firestore Sandbox Mode. It generates the exact collections and documents schemas, runs indexing streams, and compiles live transactions without requiring private credentials.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onConnectDemo,
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect Firebase Sandbox Hub", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalConsoleView(
    syncState: String,
    logs: List<String>,
    onTriggerSync: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color.LightGray),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Firestore Transaction Live Terminal", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
                    Text("Active cloud database streaming logs", fontSize = 11.sp, color = Color.Gray)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear logs", tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dark Terminal Window
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                val scrollState = rememberScrollState()
                
                LaunchedEffect(logs.size) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "system@smartresult:~$ \nTerminal idle. Ready for transaction pipeline link...",
                            color = Color(0xFF94A3B8),
                            style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp)
                        )
                    } else {
                        logs.forEach { log ->
                            val color = when {
                                log.contains("❌") || log.contains("Error") -> Color(0xFFEF4444)
                                log.contains("✔") || log.contains("🎉") || log.contains("Success") -> Color(0xFF10B981)
                                log.contains("⚠") -> Color(0xFFF59E0B)
                                log.contains("  ↳") -> Color(0xFF38BDF8)
                                else -> Color(0xFFE2E8F0)
                            }
                            Text(
                                text = log,
                                color = color,
                                style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        
                        if (syncState == "CONNECTING" || syncState == "UPLOADING") {
                            Text(
                                text = "▋ Running sync pipeline streams...",
                                color = Color(0xFF38BDF8),
                                style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onTriggerSync,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (syncState) {
                        "COMPLETE" -> SchoolGreen
                        "ERROR" -> Color.Red
                        else -> MaterialTheme.colorScheme.primary
                    }
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = syncState != "CONNECTING" && syncState != "UPLOADING"
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (syncState == "COMPLETE") Icons.Default.CheckCircle else Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (syncState) {
                            "CONNECTING" -> "Establishing Link..."
                            "UPLOADING" -> "Streaming Records to Cloud..."
                            "COMPLETE" -> "Re-sync Database"
                            "ERROR" -> "Retry Sync Stream"
                            else -> "Execute Bidirectional Cloud Sync"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CURRICULUM SUBJECTS MANAGEMENT SCREEN
// -------------------------------------------------------------
@Composable
fun ManageSubjectsScreen(
    viewModel: ResultViewModel,
    settings: SchoolSettings,
    isEmbedded: Boolean = false
) {
    val context = LocalContext.current
    val subjects by viewModel.allSubjects.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Dynamic Active Brand Colors from SchoolSettings
    val activeBrandColor = remember(settings.immersiveMode, settings.brandColorHex) {
        if (settings.immersiveMode) {
            try {
                Color(settings.brandColorHex.toColorInt())
            } catch (e: Exception) {
                SchoolGreen
            }
        } else {
            SchoolGreen
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedClassLevelFilter by remember { mutableStateOf("All Classes") }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Subject?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Subject?>(null) }

    // Dialog state fields
    var newSubjName by remember { mutableStateOf("") }
    var newSubjCategory by remember { mutableStateOf("Core Curriculum") }
    var newSubjClassLevel by remember { mutableStateOf("All Classes") }
    var newSubjDescription by remember { mutableStateOf("") }

    val categoriesList = listOf("Core Curriculum", "Science", "Arts", "Commercial", "Early-Years", "Languages")
    val classLevelsList = listOf("All Classes", "Primary 1", "Primary 2", "Primary 3", "Primary 4", "Primary 5", "JSS 1", "JSS 2", "JSS 3", "SS 1", "SS 2", "SS 3")

    // Filtered subjects
    val filteredSubjects = remember(subjects, searchQuery, selectedClassLevelFilter) {
        subjects.filter { sub ->
            val matchQuery = sub.name.contains(searchQuery, ignoreCase = true) || 
                             sub.category.contains(searchQuery, ignoreCase = true)
            val matchClass = selectedClassLevelFilter == "All Classes" || sub.classLevel == "All Classes" || sub.classLevel == selectedClassLevelFilter
            matchQuery && matchClass
        }
    }

    Scaffold(
        topBar = {
            if (!isEmbedded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(activeBrandColor)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
                            modifier = Modifier.testTag("subjects_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        AcademicShieldLogo(modifier = Modifier.size(40.dp), logoUri = settings.logoUri)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Subject Curriculum Registry",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = settings.schoolName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        // Cloud Sync action on top right
                        IconButton(
                            onClick = {
                                viewModel.runFirestoreSync(context, false)
                                Toast.makeText(context, "Initiated cloud sync for curriculum database...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("subjects_cloud_sync_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync to Cloud", tint = Color.White)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newSubjName = ""
                    newSubjCategory = "Core Curriculum"
                    newSubjClassLevel = "All Classes"
                    newSubjDescription = ""
                    showAddDialog = true
                },
                containerColor = activeBrandColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_subject_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Subject")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats & Instructions Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = activeBrandColor.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = activeBrandColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Active Curriculum: ${subjects.size} Registered Subjects",
                                fontWeight = FontWeight.Bold,
                                color = activeBrandColor,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Adding or updating subjects sets the default database registry. Newly inputted scores will sync with this registry to Firestore.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Search Bar & Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search subjects by name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subject_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Class levels horizontal scroll filter
                    Text(
                        text = "Filter by Academic Level:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        classLevelsList.forEach { level ->
                            val isSelected = selectedClassLevelFilter == level
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) activeBrandColor else Color.LightGray.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) activeBrandColor else Color.LightGray,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedClassLevelFilter = level }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = level,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            // Empty State
            if (filteredSubjects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No subjects found matching the filters.", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Click the '+' FAB at the bottom to register a new subject.", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                // List of filtered subjects
                items(filteredSubjects) { subject ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subject_card_${subject.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = subject.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Category Tag
                                        val tagBgColor = when (subject.category) {
                                            "Science" -> Color(0xFFE3F2FD)
                                            "Arts" -> Color(0xFFF3E5F5)
                                            "Commercial" -> Color(0xFFFFF3E0)
                                            "Early-Years" -> Color(0xFFF1F8E9)
                                            else -> Color(0xFFE8F5E9)
                                        }
                                        val tagTextColor = when (subject.category) {
                                            "Science" -> Color(0xFF1E88E5)
                                            "Arts" -> Color(0xFF8E24AA)
                                            "Commercial" -> Color(0xFFF57C00)
                                            "Early-Years" -> Color(0xFF7CB342)
                                            else -> activeBrandColor
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(tagBgColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = subject.category,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tagTextColor
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Level: ${subject.classLevel}",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!subject.description.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = subject.description,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                
                                // Action buttons
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            newSubjName = subject.name
                                            newSubjCategory = subject.category
                                            newSubjClassLevel = subject.classLevel
                                            newSubjDescription = subject.description ?: ""
                                            showEditDialog = subject
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("edit_subject_${subject.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Subject", tint = activeBrandColor, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { showDeleteDialog = subject },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_subject_${subject.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Subject", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Subject Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Subject to Curriculum", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSubjName,
                        onValueChange = { newSubjName = it },
                        label = { Text("Subject Name (e.g. Chemistry)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_subject_name_input"),
                        singleLine = true
                    )

                    // Category Selector
                    Text("Subject Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoriesList.forEach { cat ->
                            val isSel = newSubjCategory == cat
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) activeBrandColor else Color.LightGray.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, if (isSel) activeBrandColor else Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { newSubjCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    // Class Level Selectable buttons
                    Text("Class Level Eligibility:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        classLevelsList.forEach { level ->
                            val isSel = newSubjClassLevel == level
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) activeBrandColor else Color.LightGray.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, if (isSel) activeBrandColor else Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { newSubjClassLevel = level }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = level,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newSubjDescription,
                        onValueChange = { newSubjDescription = it },
                        label = { Text("Short Syllabus Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubjName.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a subject name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addSubject(
                            name = newSubjName.trim(),
                            category = newSubjCategory,
                            classLevel = newSubjClassLevel,
                            description = newSubjDescription.trim()
                        )
                        showAddDialog = false
                        Toast.makeText(context, "Subject added to curriculum!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeBrandColor),
                    modifier = Modifier.testTag("dialog_add_subject_save")
                ) {
                    Text("Add Subject", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Edit Subject Dialog
    showEditDialog?.let { subject ->
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Edit Subject Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSubjName,
                        onValueChange = { newSubjName = it },
                        label = { Text("Subject Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_edit_subject_name_input"),
                        singleLine = true
                    )

                    // Category
                    Text("Subject Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoriesList.forEach { cat ->
                            val isSel = newSubjCategory == cat
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) activeBrandColor else Color.LightGray.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, if (isSel) activeBrandColor else Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { newSubjCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    // Class Level
                    Text("Class Level Eligibility:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        classLevelsList.forEach { level ->
                            val isSel = newSubjClassLevel == level
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) activeBrandColor else Color.LightGray.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, if (isSel) activeBrandColor else Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { newSubjClassLevel = level }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = level,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newSubjDescription,
                        onValueChange = { newSubjDescription = it },
                        label = { Text("Short Syllabus Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubjName.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a subject name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updateSubject(
                            subject.copy(
                                name = newSubjName.trim(),
                                category = newSubjCategory,
                                classLevel = newSubjClassLevel,
                                description = newSubjDescription.trim()
                            )
                        )
                        showEditDialog = null
                        Toast.makeText(context, "Subject updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeBrandColor),
                    modifier = Modifier.testTag("dialog_edit_subject_save")
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Delete Subject Dialog
    showDeleteDialog?.let { subject ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Subject from Ledger?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to permanently delete '${subject.name}' from the academic curriculum? This will remove it from the subject registry database.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(subject)
                        showDeleteDialog = null
                        Toast.makeText(context, "Subject deleted from registry.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.testTag("dialog_delete_subject_confirm")
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// PERFORMANCE ANALYTICS DASHBOARD SCREEN (RECHARTS STYLE)
// -------------------------------------------------------------
data class ChartDataPoint(val label: String, val value: Float)

@Composable
fun PerformanceAnalyticsScreen(
    viewModel: ResultViewModel,
    studentsWithScores: List<StudentWithScores>,
    settings: SchoolSettings
) {
    val context = LocalContext.current
    val pullState by viewModel.pullState.collectAsStateWithLifecycle()
    val pullLogs by viewModel.pullLogs.collectAsStateWithLifecycle()
    
    // Dynamic Active Brand Colors from SchoolSettings
    val activeColor = remember(settings.immersiveMode, settings.brandColorHex) {
        if (settings.immersiveMode) {
            try {
                Color(settings.brandColorHex.toColorInt())
            } catch (e: Exception) {
                SchoolGreen
            }
        } else {
            SchoolGreen
        }
    }

    var selectedClassFilter by remember { mutableStateOf("All Classes") }
    var selectedTermFilter by remember { mutableStateOf("All Terms") }
    var showLogsConsole by remember { mutableStateOf(false) }

    val classLevelsList = listOf("All Classes", "Primary 1", "Primary 2", "Primary 3", "JSS 1", "JSS 2", "JSS 3", "SS 1", "SS 2", "SS 3")
    val termsList = listOf("All Terms", "1st Term", "2nd Term", "3rd Term")

    // Filter students
    val filteredRecords = remember(studentsWithScores, selectedClassFilter, selectedTermFilter) {
        studentsWithScores.filter { rec ->
            val matchClass = selectedClassFilter == "All Classes" || rec.student.studentClass.equals(selectedClassFilter, ignoreCase = true)
            val matchTerm = selectedTermFilter == "All Terms" || rec.student.term.equals(selectedTermFilter, ignoreCase = true)
            matchClass && matchTerm
        }
    }

    // Process analytics metrics
    val totalStudentsCount = filteredRecords.size
    val schoolAverage = remember(filteredRecords) {
        if (filteredRecords.isNotEmpty()) {
            filteredRecords.map { it.averageScore }.average()
        } else 0.0
    }

    val passRate = remember(filteredRecords) {
        if (filteredRecords.isNotEmpty()) {
            val passed = filteredRecords.count { it.averageScore >= 40.0 }
            (passed.toDouble() / filteredRecords.size) * 100.0
        } else 0.0
    }

    // Process subject-wise performance for stacked bar charts
    val subjectStats = remember(filteredRecords) {
        val scoreSums = mutableMapOf<String, Pair<Double, Double>>() // subjectName -> Pair(caTotal, examTotal)
        val scoreCounts = mutableMapOf<String, Int>()
        
        filteredRecords.forEach { rec ->
            rec.scores.forEach { sc ->
                val current = scoreSums[sc.subjectName] ?: Pair(0.0, 0.0)
                scoreSums[sc.subjectName] = Pair(current.first + sc.caScore, current.second + sc.examScore)
                scoreCounts[sc.subjectName] = (scoreCounts[sc.subjectName] ?: 0) + 1
            }
        }
        
        scoreSums.map { (subject, totals) ->
            val count = scoreCounts[subject] ?: 1
            val avgCa = totals.first / count
            val avgExam = totals.second / count
            Triple(subject, avgCa, avgExam)
        }.sortedByDescending { it.second + it.third }
    }

    val topPerformingSubject = remember(subjectStats) {
        if (subjectStats.isNotEmpty()) {
            val top = subjectStats.maxByOrNull { it.second + it.third }
            (top?.first ?: "N/A") to (top?.second?.plus(top.third) ?: 0.0)
        } else "N/A" to 0.0
    }

    // Class average performance trend line data points
    val classTrendPoints = remember(studentsWithScores, selectedTermFilter) {
        val classGroups = studentsWithScores.filter { rec ->
            selectedTermFilter == "All Terms" || rec.student.term.equals(selectedTermFilter, ignoreCase = true)
        }.groupBy { it.student.studentClass }
        
        listOf("Primary 1", "Primary 2", "Primary 3", "JSS 1", "JSS 2", "JSS 3", "SS 1", "SS 2", "SS 3")
            .map { cls ->
                val recs = classGroups[cls] ?: emptyList()
                val avg = if (recs.isNotEmpty()) recs.map { it.averageScore }.average() else 0.0
                ChartDataPoint(cls, avg.toFloat())
            }.filter { it.value > 0f }
    }

    // Grade breakdown spectrum percentages
    val gradeDistribution = remember(filteredRecords) {
        val counts = mutableMapOf("A" to 0, "B" to 0, "C" to 0, "D" to 0, "E" to 0, "F" to 0)
        var totalScoresEvaluated = 0
        filteredRecords.forEach { rec ->
            rec.scores.forEach { sc ->
                val grade = sc.grade
                counts[grade] = (counts[grade] ?: 0) + 1
                totalScoresEvaluated++
            }
        }
        counts.mapValues { if (totalScoresEvaluated > 0) (it.value.toDouble() / totalScoresEvaluated) * 100.0 else 0.0 }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(activeColor)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
                        modifier = Modifier.testTag("analytics_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AcademicShieldLogo(modifier = Modifier.size(40.dp), logoUri = settings.logoUri)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Performance Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${settings.schoolName} • Decision Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // FIRESTORE PULL CONTROL HUB
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (pullState == "COMPLETE") SchoolGreen else if (pullState == "CONNECTING" || pullState == "DOWNLOADING") SchoolGold else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Firestore Sync: ${settings.firestoreStatus}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                            }
                            
                            // Log Toggle
                            if (pullLogs.isNotEmpty()) {
                                TextButton(
                                    onClick = { showLogsConsole = !showLogsConsole }
                                ) {
                                    Text(
                                        text = if (showLogsConsole) "Hide Sync Console" else "Show Logs (${pullLogs.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pull remote grade book files directly from Google Cloud Firestore database to compute aggregate performance visual analytics.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.runFirestorePull(context, isSimulation = false)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pull_real_firestore_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pull Live DB", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.runFirestorePull(context, isSimulation = true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pull_sandbox_btn")
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Run Sandbox Demo", fontSize = 11.sp)
                            }
                        }

                        // EXPANDABLE LOGS TERMINAL
                        if (showLogsConsole && pullLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                LaunchedEffect(pullLogs.size) {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    pullLogs.forEach { log ->
                                        Text(
                                            text = log,
                                            color = if (log.contains("❌")) Color(0xFFEF5350) else if (log.contains("✔") || log.contains("Success")) Color(0xFF66BB6A) else Color(0xFFECEFF1),
                                            fontSize = 10.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // Download progress line
                        if (pullState == "CONNECTING" || pullState == "DOWNLOADING") {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = activeColor,
                                trackColor = activeColor.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // FILTER CAROUSELS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Academic Filter Scope:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    
                    // Class chips carousel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        classLevelsList.forEach { level ->
                            val isSelected = selectedClassFilter == level
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) activeColor else Color.LightGray.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(1.dp, if (isSelected) activeColor else Color.LightGray, RoundedCornerShape(20.dp))
                                    .clickable { selectedClassFilter = level }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = level,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.DarkGray
                                )
                            }
                        }
                    }

                    // Term chips carousel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        termsList.forEach { term ->
                            val isSelected = selectedTermFilter == term
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) activeColor else Color.LightGray.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(1.dp, if (isSelected) activeColor else Color.LightGray, RoundedCornerShape(20.dp))
                                    .clickable { selectedTermFilter = term }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = term,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            // METRICS GRID OVERVIEW
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card 1: School Average
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = activeColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Class Average", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", schoolAverage),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        // Card 2: Top Subject
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Top Subject", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = if (topPerformingSubject.first.length > 12) topPerformingSubject.first.take(10) + ".." else topPerformingSubject.first,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card 3: Pass Rate
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Pass Rate (>=40%)", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", passRate),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        // Card 4: Active Cohort Count
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Cohort Size", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = "$totalStudentsCount Students",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // EMPTY STATE PROMPT
            if (filteredRecords.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = activeColor.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                Text("No students match the current academic filters.", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = "Click 'Run Sandbox Demo' at the top to auto-generate 8 premium student files with scores directly, or sync live Firestore credentials.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                // VISUALIZATION 1: CLASS PERFORMANCE TRENDS (RECHARTS STYLE AREA CHART)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Average Class Performance Trends",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Interactive AreaChart representing score distributions across class cohorts",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(activeColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Recharts Style", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = activeColor)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Native Line/Area Chart Composable
                            BezierAreaChart(
                                dataPoints = classTrendPoints,
                                activeColor = activeColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }
                }

                // VISUALIZATION 2: SUBJECT PERFORMANCE STACKED BAR CHART
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Subject Curriculum Comparison",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                             )
                            Text(
                                text = "Stacked BarChart comparing average Continuous Assessment (CA) and Examination portions",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            SubjectComparisonBarChart(
                                stats = subjectStats.take(6),
                                activeColor = activeColor
                            )
                        }
                    }
                }

                // VISUALIZATION 3: GRADE DISTRIBUTION SPECTRUM (RING MAPPING)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Grade Distribution Analysis",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "Relative percentage of scoring bands (A to F) across all evaluated subjects",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            GradeDistributionBar(distribution = gradeDistribution)
                        }
                    }
                }

                // LEADERBOARD: TOP SCHOLARS SHOWCASE
                item {
                    Text(
                        text = "Outstanding Cohort Scholars",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(filteredRecords.sortedByDescending { it.averageScore }.take(3)) { scholar ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(activeColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = scholar.student.fullName.firstOrNull()?.toString() ?: "S",
                                        fontWeight = FontWeight.Bold,
                                        color = activeColor,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = scholar.student.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "${scholar.student.studentClass} • ${scholar.student.gender}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.2f%%", scholar.averageScore),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = activeColor
                                )
                                Text(
                                    text = "Position: 1st Class",
                                    fontSize = 9.sp,
                                    color = SchoolGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// NATIVE BEZIER AREA CHART (RECHARTS STYLE AREA CHART)
// -------------------------------------------------------------
@Composable
fun BezierAreaChart(
    dataPoints: List<ChartDataPoint>,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    if (dataPoints.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No active trend data to display", color = Color.Gray, fontSize = 12.sp)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = modifier
                .background(Color.White)
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        val widthVal = size.width.toFloat()
                        val stepX = widthVal / (dataPoints.size.coerceAtLeast(2) - 1).toFloat()
                        val index = (offset.x / stepX).roundToInt().coerceIn(0, dataPoints.size - 1)
                        selectedIndex = if (selectedIndex == index) null else index
                    }
                }
        ) {
            val width = size.width.toFloat()
            val height = size.height.toFloat()
            
            val paddingLeft = 60f
            val paddingRight = 30f
            val paddingTop = 30f
            val paddingBottom = 60f
            
            val chartWidth = width - paddingLeft - paddingRight
            val chartHeight = height - paddingTop - paddingBottom

            val maxVal = 100f
            val minVal = 0f

            // 1. Draw horizontal grid lines (dashed)
            val gridLines = listOf(20f, 40f, 60f, 80f, 100f)
            gridLines.forEach { lineVal ->
                val ratio = (lineVal - minVal) / (maxVal - minVal)
                val y = paddingTop + chartHeight * (1f - ratio)
                
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // 2. Draw axes and text labels
            drawLine(
                color = Color.Gray,
                start = Offset(paddingLeft, paddingTop),
                end = Offset(paddingLeft, paddingTop + chartHeight),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.Gray,
                start = Offset(paddingLeft, paddingTop + chartHeight),
                end = Offset(width - paddingRight, paddingTop + chartHeight),
                strokeWidth = 2f
            )

            // Draw Y Ticks Text labels
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 24f
                isAntiAlias = true
            }
            gridLines.forEach { lineVal ->
                val ratio = (lineVal - minVal) / (maxVal - minVal)
                val y = paddingTop + chartHeight * (1f - ratio)
                drawContext.canvas.nativeCanvas.drawText(
                    "${lineVal.toInt()}%",
                    10f,
                    y + 8f,
                    textPaint
                )
            }

            // 3. Compute Coordinates
            val stepX = chartWidth / (dataPoints.size.coerceAtLeast(2) - 1).toFloat()
            val coordinates = dataPoints.mapIndexed { idx, pt ->
                val x = paddingLeft + idx * stepX
                val ratio = (pt.value - minVal) / (maxVal - minVal)
                val y = paddingTop + chartHeight * (1f - ratio)
                Offset(x, y)
            }

            // 4. Draw Area Gradient & Curved Line
            if (coordinates.isNotEmpty()) {
                val linePath = Path()
                val areaPath = Path()
                
                linePath.moveTo(coordinates[0].x, coordinates[0].y)
                areaPath.moveTo(coordinates[0].x, paddingTop + chartHeight)
                areaPath.lineTo(coordinates[0].x, coordinates[0].y)

                for (i in 1 until coordinates.size) {
                    val prev = coordinates[i - 1]
                    val curr = coordinates[i]
                    // smooth connection control points
                    val cp1 = Offset(prev.x + stepX / 2.0f, prev.y)
                    val cp2 = Offset(prev.x + stepX / 2.0f, curr.y)
                    
                    linePath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, curr.x, curr.y)
                    areaPath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, curr.x, curr.y)
                }

                areaPath.lineTo(coordinates.last().x, paddingTop + chartHeight)
                areaPath.close()

                // Draw Area fill
                val fillBrush = Brush.verticalGradient(
                    colors = listOf(activeColor.copy(alpha = 0.35f), activeColor.copy(alpha = 0.005f)),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                )
                drawPath(path = areaPath, brush = fillBrush)

                // Draw line stroke
                drawPath(
                    path = linePath,
                    color = activeColor,
                    style = Stroke(width = 4f)
                )

                // 5. Draw Point Circles & Labels
                coordinates.forEachIndexed { idx, offset ->
                    val isSelected = selectedIndex == idx
                    
                    // Outer accent circle
                    drawCircle(
                        color = if (isSelected) Color.White else activeColor,
                        radius = if (isSelected) 10f else 6f,
                        center = offset
                    )
                    drawCircle(
                        color = activeColor,
                        radius = if (isSelected) 6f else 3f,
                        center = offset,
                        style = if (isSelected) Stroke(width = 3f) else androidx.compose.ui.graphics.drawscope.Fill
                    )

                    // Draw X Labels
                    val label = dataPoints[idx].label
                    val labelPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 24f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        if (label.length > 5) label.take(5) else label,
                        offset.x,
                        paddingTop + chartHeight + 35f,
                        labelPaint
                    )
                }
            }
        }

        // 6. Interactive Tooltip Overlay (mimicking Recharts tooltip)
        selectedIndex?.let { index ->
            if (index in dataPoints.indices) {
                val pt = dataPoints[index]
                Card(
                    colors = CardDefaults.cardColors(containerColor = activeColor.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, activeColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${pt.label} Average:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f%% Score", pt.value),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeColor
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// NATIVE SUBJECT COMPARISON BAR CHART (STACKED CA + EXAM)
// -------------------------------------------------------------
@Composable
fun SubjectComparisonBarChart(
    stats: List<Triple<String, Double, Double>>, // Triple(subjectName, avgCa, avgExam)
    activeColor: Color
) {
    if (stats.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No active curriculum subjects to display.", color = Color.Gray, fontSize = 12.sp)
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        stats.forEach { (subject, avgCa, avgExam) ->
            val total = avgCa + avgExam
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subject,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("CA: ${String.format(Locale.US, "%.1f", avgCa)}", fontSize = 10.sp, color = Color.Gray)
                        Text("Exam: ${String.format(Locale.US, "%.1f", avgExam)}", fontSize = 10.sp, color = Color.Gray)
                        Text("Total: ${String.format(Locale.US, "%.1f%%", total)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = activeColor)
                    }
                }
                
                // Composite Progress bar: CA + Exam
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.fillMaxHeight()) {
                        // CA portion (light color)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (avgCa / 100f).toFloat().coerceIn(0f, 1f))
                                .background(activeColor.copy(alpha = 0.45f))
                        )
                        // Exam portion (main brand color)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (avgExam / (100f - avgCa)).toFloat().coerceIn(0f, 1f))
                                .background(activeColor)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// NATIVE GRADE DISTRIBUTION PROGRESS BAR WIDGET
// -------------------------------------------------------------
@Composable
fun GradeDistributionBar(
    distribution: Map<String, Double> // grade -> percentage
) {
    val grades = listOf("A", "B", "C", "D", "E", "F")
    val gradeColors = listOf(
        Color(0xFF4CAF50), // A - Green
        Color(0xFF2196F3), // B - Blue
        Color(0xFF00BCD4), // C - Teal
        Color(0xFFFFEB3B), // D - Yellow
        Color(0xFFFF9800), // E - Orange
        Color(0xFFF44336)  // F - Red
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Horizontal bar split representation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = 0.2f))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                grades.forEachIndexed { idx, grade ->
                    val percentage = distribution[grade] ?: 0.0
                    if (percentage > 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(percentage.toFloat())
                                .background(gradeColors[idx % gradeColors.size])
                        )
                    }
                }
            }
        }

        // Legend details grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val chunked = grades.chunked(3)
            chunked.forEach { rowGrades ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowGrades.forEach { g ->
                        val index = grades.indexOf(g)
                        val col = gradeColors[index % gradeColors.size]
                        val pct = distribution[g] ?: 0.0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(col)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Grade $g (${String.format(Locale.US, "%.1f%%", pct)})",
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TEMPLATE EDITOR SCREEN COMPOSABLE
// -------------------------------------------------------------
@Composable
fun TemplateEditorScreen(
    viewModel: ResultViewModel,
    settings: SchoolSettings
) {
    val context = LocalContext.current

    // Local states initialized with the settings in Database
    var resultTemplate by remember { mutableStateOf(settings.resultTemplate) }
    var logoPlacement by remember { mutableStateOf(settings.logoPlacement) }
    var termLabel by remember { mutableStateOf(settings.termLabel) }
    var sessionLabel by remember { mutableStateOf(settings.sessionLabel) }
    var studentIdLabel by remember { mutableStateOf(settings.studentIdLabel) }
    var showHeaderAddress by remember { mutableStateOf(settings.showHeaderAddress) }
    var showHeaderContact by remember { mutableStateOf(settings.showHeaderContact) }
    var showSignatureLines by remember { mutableStateOf(settings.showSignatureLines) }
    var showTeacherComments by remember { mutableStateOf(settings.showTeacherComments) }
    var showPrincipalComments by remember { mutableStateOf(settings.showPrincipalComments) }
    var showGradeScaleKey by remember { mutableStateOf(settings.showGradeScaleKey) }
    var cardCornerRadius by remember { mutableStateOf(settings.cardCornerRadius) }
    var reportDensity by remember { mutableStateOf(settings.reportDensity) }

    val activeBrandColor = remember(settings.immersiveMode, settings.brandColorHex) {
        if (settings.immersiveMode) {
            try {
                Color(settings.brandColorHex.toColorInt())
            } catch (e: Exception) {
                SchoolGreen
            }
        } else {
            SchoolGreen
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(activeBrandColor)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.Settings) },
                        modifier = Modifier.testTag("template_editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AcademicShieldLogo(modifier = Modifier.size(40.dp), logoUri = settings.logoUri)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Report Template Designer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Live layout customizer & field editor",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Description Introduction
            item {
                Text(
                    text = "Report Card Customizer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = activeBrandColor
                )
                Text(
                    text = "Visual control center for school term reports. Changes here apply instantly to online screens and exported PDF report cards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Controls Block
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "1. Basic Template Styles & Density",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeBrandColor
                        )

                        // Result template selector
                        Column {
                            Text("Base Layout Template Style", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Modern", "Simple", "Detailed", "Delma Classic").forEach { t ->
                                    val isSel = resultTemplate == t
                                    ElevatedButton(
                                        onClick = { resultTemplate = t },
                                        colors = ButtonDefaults.elevatedButtonColors(
                                            containerColor = if (isSel) activeBrandColor else MaterialTheme.colorScheme.surface,
                                            contentColor = if (isSel) Color.White else Color.DarkGray
                                        ),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(t, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Sizing & Margin Density
                        Column {
                            Text("Layout Padding Density", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Compact", "Normal", "Spacious").forEach { d ->
                                    val isSel = reportDensity == d
                                    OutlinedButton(
                                        onClick = { reportDensity = d },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSel) activeBrandColor.copy(alpha = 0.1f) else Color.Transparent,
                                            contentColor = if (isSel) activeBrandColor else Color.DarkGray
                                        ),
                                        border = BorderStroke(1.dp, if (isSel) activeBrandColor else Color.LightGray),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(d, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Card Corner Rounding Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Card Corner Rounding", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("${cardCornerRadius} dp", fontWeight = FontWeight.Bold, color = activeBrandColor, fontSize = 13.sp)
                            }
                            Slider(
                                value = cardCornerRadius.toFloat(),
                                onValueChange = { cardCornerRadius = it.toInt() },
                                valueRange = 0f..24f,
                                steps = 5,
                                colors = SliderDefaults.colors(
                                    thumbColor = activeBrandColor,
                                    activeTrackColor = activeBrandColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        Text(
                            text = "2. School Logo Alignment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeBrandColor
                        )

                        // Logo Placement choices
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Left", "Center", "Right", "Hidden").forEach { p ->
                                val isSel = logoPlacement == p
                                ElevatedButton(
                                    onClick = { logoPlacement = p },
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = if (isSel) activeBrandColor else MaterialTheme.colorScheme.surface,
                                        contentColor = if (isSel) Color.White else Color.DarkGray
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(p, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        Text(
                            text = "3. Custom Field Label Header Override",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeBrandColor
                        )

                        OutlinedTextField(
                            value = studentIdLabel,
                            onValueChange = { studentIdLabel = it },
                            label = { Text("ID No / Admission Field Label") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_id_label_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = termLabel,
                            onValueChange = { termLabel = it },
                            label = { Text("Academic Term Label") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_term_label_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = sessionLabel,
                            onValueChange = { sessionLabel = it },
                            label = { Text("School Session Label") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_session_label_input"),
                            singleLine = true
                        )

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        Text(
                            text = "4. Header & Footer Component Toggles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeBrandColor
                        )

                        // Switches for toggles
                        ToggleRow(label = "Show Address in School Header", checked = showHeaderAddress, onCheckedChange = { showHeaderAddress = it })
                        ToggleRow(label = "Show Contact Info in School Header", checked = showHeaderContact, onCheckedChange = { showHeaderContact = it })
                        ToggleRow(label = "Include Official Signature Lines", checked = showSignatureLines, onCheckedChange = { showSignatureLines = it })
                        ToggleRow(label = "Show Teacher Assessment Remarks", checked = showTeacherComments, onCheckedChange = { showTeacherComments = it })
                        ToggleRow(label = "Show Principal Endorsement", checked = showPrincipalComments, onCheckedChange = { showPrincipalComments = it })
                        ToggleRow(label = "Show Grading Scheme Scale Key", checked = showGradeScaleKey, onCheckedChange = { showGradeScaleKey = it })

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.updateTemplateSettings(
                                    logoPlacement = logoPlacement,
                                    termLabel = termLabel.trim(),
                                    sessionLabel = sessionLabel.trim(),
                                    studentIdLabel = studentIdLabel.trim(),
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
                                Toast.makeText(context, "Visual Template layout customized successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeBrandColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_template_customization_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply & Save Template Design", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Real-Time Preview Card Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = SchoolGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Real-Time Visual Layout Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
                Text(
                    text = "This preview demonstrates how actual result sheets are formatted with your current selections.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Real-time Mock Preview Card Body!
            item {
                Card(
                    shape = RoundedCornerShape(cardCornerRadius.dp),
                    border = BorderStroke(1.5.dp, activeBrandColor),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Density padding calculation
                    val densityPadding = when (reportDensity) {
                        "Compact" -> 8.dp
                        "Normal" -> 16.dp
                        "Spacious" -> 24.dp
                        else -> 16.dp
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(densityPadding)
                    ) {
                        // School Header
                        when (logoPlacement) {
                            "Left" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AcademicShieldLogo(modifier = Modifier.size(50.dp), logoUri = settings.logoUri)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = settings.schoolName.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (resultTemplate == "Modern") activeBrandColor else Color.Black
                                        )
                                        if (showHeaderAddress) {
                                            Text(text = settings.address, fontSize = 9.sp, color = Color.DarkGray)
                                        }
                                        if (showHeaderContact) {
                                            Text(text = settings.contactInfo, fontSize = 8.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                            "Right" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = settings.schoolName.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (resultTemplate == "Modern") activeBrandColor else Color.Black
                                        )
                                        if (showHeaderAddress) {
                                            Text(text = settings.address, fontSize = 9.sp, color = Color.DarkGray)
                                        }
                                        if (showHeaderContact) {
                                            Text(text = settings.contactInfo, fontSize = 8.sp, color = Color.Gray)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    AcademicShieldLogo(modifier = Modifier.size(50.dp), logoUri = settings.logoUri)
                                }
                            }
                            "Center" -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AcademicShieldLogo(modifier = Modifier.size(54.dp), logoUri = settings.logoUri)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = settings.schoolName.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (resultTemplate == "Modern") activeBrandColor else Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    if (showHeaderAddress) {
                                        Text(text = settings.address, fontSize = 9.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
                                    }
                                    if (showHeaderContact) {
                                        Text(text = settings.contactInfo, fontSize = 8.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                            "Hidden" -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = settings.schoolName.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (resultTemplate == "Modern") activeBrandColor else Color.Black
                                    )
                                    if (showHeaderAddress) {
                                        Text(text = settings.address, fontSize = 9.sp, color = Color.DarkGray)
                                    }
                                    if (showHeaderContact) {
                                        Text(text = settings.contactInfo, fontSize = 8.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (resultTemplate == "Detailed") 3.dp else 1.5.dp)
                                .background(if (resultTemplate == "Modern") activeBrandColor else Color.Black)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Student Meta Info Block with Customized Labels
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Student Name:", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("Tunde Johnson", fontSize = 11.sp, modifier = Modifier.weight(2.0f))
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("$studentIdLabel:", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("AIS-2026-904", fontSize = 11.sp, modifier = Modifier.weight(2.0f))
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Student Class:", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("SS 3 Science", fontSize = 11.sp, modifier = Modifier.weight(2.0f))
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("$termLabel:", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("3rd Term", fontSize = 11.sp, modifier = Modifier.weight(2.0f))
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("$sessionLabel:", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("2025/2026", fontSize = 11.sp, modifier = Modifier.weight(2.0f))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mock Score Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (resultTemplate == "Modern") activeBrandColor else Color.LightGray.copy(alpha = 0.5f))
                                .padding(6.dp)
                        ) {
                            Text("Subject Name", fontWeight = FontWeight.Bold, color = if (resultTemplate == "Modern") Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.weight(2f))
                            Text("CA", fontWeight = FontWeight.Bold, color = if (resultTemplate == "Modern") Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                            Text("Exam", fontWeight = FontWeight.Bold, color = if (resultTemplate == "Modern") Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                            Text("Total", fontWeight = FontWeight.Bold, color = if (resultTemplate == "Modern") Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                            Text("Grd", fontWeight = FontWeight.Bold, color = if (resultTemplate == "Modern") Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                            Text("Remark", fontWeight = FontWeight.Bold, color = if (resultTemplate == "Modern") Color.White else Color.Black, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }

                        // Mock Subject Rows
                        val mockScores = listOf(
                            Triple("Mathematics", 36 to 54, "A" to "Excellent"),
                            Triple("English Language", 31 to 46, "A" to "Excellent"),
                            Triple("Physics", 28 to 41, "B" to "Very Good")
                        )

                        mockScores.forEachIndexed { idx, item ->
                            val (subj, scores, res) = item
                            val (ca, exam) = scores
                            val (grd, rem) = res
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 1) Color.LightGray.copy(alpha = 0.15f) else Color.White)
                                    .padding(horizontal = 6.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(subj, fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(2f))
                                Text("$ca", fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                                Text("$exam", fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                                Text("${ca + exam}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                Text(grd, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                Text(rem, fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Divider
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))

                        Spacer(modifier = Modifier.height(8.dp))

                        // Summary Statistics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Cumulative Score: 236", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                Text("Term Average Percent: 78.67%", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = activeBrandColor)
                            }
                            Box(
                                modifier = Modifier
                                    .background(activeBrandColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("PASSED", color = activeBrandColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Comments Block
                        if (showTeacherComments || showPrincipalComments) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (showTeacherComments) {
                                    Text("CLASS TEACHER'S ENDORSEMENT", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = activeBrandColor)
                                    Text("An excellent student with brilliant analytical capability. Outstanding term!", fontSize = 10.sp, color = Color.DarkGray)
                                }
                                if (showPrincipalComments) {
                                    Text("PRINCIPAL'S REGULATORY STAMP & COMMENT", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = SecondaryBlue)
                                    Text("Highly commendable performance. Keep sustaining this gold standard.", fontSize = 10.sp, color = Color.DarkGray)
                                }
                            }
                        }

                        // Scale Scheme Key
                        if (showGradeScaleKey) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text("OFFICIAL SCALING KEY", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("A (>=70): Excellent  |  B (60-69): Very Good  |  C (50-59): Good  |  D (45-49): Pass  |  E (40-44): Fair  |  F (<40): Fail", fontSize = 8.sp, color = Color.Gray)
                            }
                        }

                        // Signatures Row
                        if (showSignatureLines) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("_______________", fontSize = 9.sp, color = Color.Gray)
                                    Text("Class Teacher Signature", fontSize = 9.sp, color = Color.DarkGray)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("_______________", fontSize = 9.sp, color = Color.Gray)
                                    Text("Principal Seal", fontSize = 9.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// -------------------------------------------------------------
// FILTER CHIP ITEM COMPOSABLE
// -------------------------------------------------------------
@Composable
fun FilterChipItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

// -------------------------------------------------------------
// DEDICATED EXPORT SCREEN (PDF & DATA EXPORT CENTER)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(
    viewModel: ResultViewModel,
    records: List<StudentWithScores>,
    settings: SchoolSettings
) {
    val context = LocalContext.current
    
    // Filter States
    var selectedClass by remember { mutableStateOf("All Classes") }
    var selectedTerm by remember { mutableStateOf("All Terms") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("All Genders") }

    // Dynamic classes extracted from records and standard options
    val availableClasses = remember(records) {
        val fromRecords = records.map { it.student.studentClass }.filter { it.isNotBlank() }
        val defaultClasses = listOf("JSS 1", "JSS 2", "JSS 3", "SS 1", "SS 2", "SS 3")
        (listOf("All Classes") + (fromRecords + defaultClasses).distinct().sorted())
    }

    val availableTerms = listOf("All Terms", "1st Term", "2nd Term", "3rd Term")

    // Filter logic
    val filteredRecords = remember(records, selectedClass, selectedTerm, searchQuery, selectedGender) {
        records.filter { record ->
            val s = record.student
            val matchesClass = selectedClass == "All Classes" || s.studentClass.equals(selectedClass, ignoreCase = true)
            val matchesTerm = selectedTerm == "All Terms" || s.term.equals(selectedTerm, ignoreCase = true)
            val matchesGender = selectedGender == "All Genders" || s.gender.equals(selectedGender, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || 
                s.fullName.contains(searchQuery, ignoreCase = true) || 
                s.studentId.contains(searchQuery, ignoreCase = true)
            matchesClass && matchesTerm && matchesGender && matchesSearch
        }
    }

    // Selection set for batch export
    var selectedStudentIds by remember(filteredRecords) {
        mutableStateOf(filteredRecords.map { it.student.id }.toSet())
    }

    val selectedRecordsForExport = remember(filteredRecords, selectedStudentIds) {
        filteredRecords.filter { selectedStudentIds.contains(it.student.id) }
    }

    val filterSummaryText = remember(selectedClass, selectedTerm) {
        val classLabel = if (selectedClass != "All Classes") selectedClass else "All Classes"
        val termLabel = if (selectedTerm != "All Terms") selectedTerm else "All Terms"
        "$classLabel — $termLabel"
    }

    val isAllSelected = filteredRecords.isNotEmpty() && selectedStudentIds.size == filteredRecords.size

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
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export Results",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Batch PDF Export Center",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Filter and compile student result cards into shareable PDF packages",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter Controls Card
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Filter Student Results",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (selectedClass != "All Classes" || selectedTerm != "All Terms" || searchQuery.isNotEmpty() || selectedGender != "All Genders") {
                            TextButton(
                                onClick = {
                                    selectedClass = "All Classes"
                                    selectedTerm = "All Terms"
                                    searchQuery = ""
                                    selectedGender = "All Genders"
                                },
                                modifier = Modifier.testTag("reset_filters_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Filters", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

                    // Class Filter Chips Row
                    Column {
                        Text(
                            text = "CLASS / GRADE LEVEL:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            availableClasses.forEach { cls ->
                                FilterChipItem(
                                    selected = selectedClass == cls,
                                    onClick = { selectedClass = cls },
                                    label = cls,
                                    modifier = Modifier.testTag("class_filter_$cls")
                                )
                            }
                        }
                    }

                    // Term Filter Chips Row
                    Column {
                        Text(
                            text = "ACADEMIC TERM:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            availableTerms.forEach { term ->
                                FilterChipItem(
                                    selected = selectedTerm == term,
                                    onClick = { selectedTerm = term },
                                    label = term,
                                    modifier = Modifier.testTag("term_filter_${term.replace(" ", "_")}")
                                )
                            }
                        }
                    }

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by student name or ID...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_search_input")
                    )
                }
            }
        }

        // Stats Banner & Primary Export Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("MATCHING RECORDS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(
                                text = "${filteredRecords.size} Students",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column {
                            Text("SELECTED FOR PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(
                                text = "${selectedRecordsForExport.size} Selected",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedRecordsForExport.isNotEmpty()) SchoolGreen else Color.Gray
                            )
                        }

                        Column {
                            Text("BATCH AVERAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            val avg = if (filteredRecords.isNotEmpty()) filteredRecords.map { it.averageScore }.average() else 0.0
                            Text(
                                text = String.format(Locale.US, "%.1f%%", avg),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Primary Export Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportFilteredToPdf(context, selectedRecordsForExport, filterSummaryText)
                            },
                            enabled = selectedRecordsForExport.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("export_filtered_pdf_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Export Filtered PDF (${selectedRecordsForExport.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.exportFilteredToCsv(context, selectedRecordsForExport, filterSummaryText)
                            },
                            enabled = selectedRecordsForExport.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("export_filtered_csv_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Select / Deselect All Toggle Row
                    if (filteredRecords.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        selectedStudentIds = if (isAllSelected) emptySet() else filteredRecords.map { it.student.id }.toSet()
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isAllSelected,
                                    onCheckedChange = { checked ->
                                        selectedStudentIds = if (checked) filteredRecords.map { it.student.id }.toSet() else emptySet()
                                    },
                                    modifier = Modifier.testTag("select_all_checkbox")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAllSelected) "Deselect All (${filteredRecords.size})" else "Select All (${filteredRecords.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                text = "Filter: $filterSummaryText",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        // Student List Header
        item {
            Text(
                text = "Matching Student Reports (${filteredRecords.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Filtered Student Cards List or Empty State
        if (filteredRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Student Results Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No student records match the class '$selectedClass' and term '$selectedTerm' filters.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                selectedClass = "All Classes"
                                selectedTerm = "All Terms"
                                searchQuery = ""
                                selectedGender = "All Genders"
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear All Filters")
                        }
                    }
                }
            }
        } else {
            items(filteredRecords) { record ->
                val student = record.student
                val isSelected = selectedStudentIds.contains(student.id)
                val position = viewModel.getStudentPosition(student, records)
                val overallGrade = com.example.data.getGrade(record.averageScore)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedStudentIds = if (isSelected) {
                                selectedStudentIds - student.id
                            } else {
                                selectedStudentIds + student.id
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedStudentIds = if (checked) {
                                    selectedStudentIds + student.id
                                } else {
                                    selectedStudentIds - student.id
                                }
                            },
                            modifier = Modifier.testTag("student_select_checkbox_${student.id}")
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (overallGrade) {
                                        "A" -> SchoolGreen.copy(alpha = 0.2f)
                                        "B" -> SecondaryBlue.copy(alpha = 0.2f)
                                        "C" -> SchoolGold.copy(alpha = 0.2f)
                                        else -> Color.LightGray.copy(alpha = 0.3f)
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = overallGrade,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = when (overallGrade) {
                                    "A" -> SchoolGreen
                                    "B" -> SecondaryBlue
                                    "C" -> SchoolGold
                                    else -> Color.DarkGray
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = student.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = student.studentClass,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ID: ${student.studentId} • ${student.term} • Pos: $position",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "%.1f%%", record.averageScore),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    viewModel.exportResultAsPdf(context, record)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("export_single_pdf_${student.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export Single PDF",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DEDICATED CSV DATA IMPORT & COLUMN MAPPING WIZARD
// -------------------------------------------------------------
enum class CsvImportCategory(val title: String, val subtitle: String) {
    STUDENTS("Students Roster", "Import student profiles, admission IDs, classes, and terms"),
    SUBJECTS("Subject Catalog", "Import subjects, categories, target class levels, and notes"),
    SCORES("Assessment Scores", "Import student subject scores (CA and Exam marks)")
}

data class CsvTargetField(
    val key: String,
    val label: String,
    val isRequired: Boolean,
    val description: String,
    val keywords: List<String>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CsvImportScreen(
    viewModel: ResultViewModel,
    settings: SchoolSettings
) {
    val context = LocalContext.current
    var activeCategory by remember { mutableStateOf(CsvImportCategory.STUDENTS) }

    // Raw CSV input state
    var rawCsvText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successSummaryMessage by remember { mutableStateOf<String?>(null) }

    // Document Picker Launcher
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val text = stream.bufferedReader().use { reader -> reader.readText() }
                    if (text.isNotBlank()) {
                        rawCsvText = text
                        errorMessage = null
                    } else {
                        errorMessage = "The selected file was empty."
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to read CSV file: ${e.localizedMessage}"
            }
        }
    }

    // Target fields definition based on active category
    val targetFields = remember(activeCategory) {
        when (activeCategory) {
            CsvImportCategory.STUDENTS -> listOf(
                CsvTargetField("fullName", "Full Name", true, "Student's complete name", listOf("name", "full_name", "student_name", "first_name", "student")),
                CsvTargetField("studentId", "Student ID / Reg No", true, "Unique admission or registration number", listOf("id", "student_id", "reg_no", "admission_no", "reg", "code")),
                CsvTargetField("gender", "Gender / Sex", false, "Male or Female", listOf("gender", "sex", "m_f")),
                CsvTargetField("studentClass", "Class / Grade Level", false, "Class assigned (e.g. JSS 1, SS 2)", listOf("class", "grade", "form", "level")),
                CsvTargetField("term", "Academic Term", false, "Term (1st, 2nd, or 3rd Term)", listOf("term", "period", "semester")),
                CsvTargetField("session", "Academic Session", false, "Academic year (e.g. 2025/2026)", listOf("session", "year", "academic_year")),
                CsvTargetField("attendancePresent", "Days Present", false, "Number of school days attended", listOf("present", "days_present", "attendance_present")),
                CsvTargetField("attendanceTotal", "Total Days", false, "Total school days in term", listOf("total_days", "school_days", "attendance_total")),
                CsvTargetField("teacherComment", "Teacher Remarks", false, "Class teacher's end-of-term comment", listOf("teacher", "remarks", "comment"))
            )
            CsvImportCategory.SUBJECTS -> listOf(
                CsvTargetField("name", "Subject Title", true, "Name of the subject (e.g. Mathematics)", listOf("name", "subject", "subject_name", "title", "course")),
                CsvTargetField("category", "Department / Category", false, "Core, Science, Arts, Commercial", listOf("category", "dept", "department", "type")),
                CsvTargetField("classLevel", "Target Class Level", false, "Applicable class or 'All Classes'", listOf("class", "level", "grade")),
                CsvTargetField("description", "Subject Description", false, "Brief outline of course topics", listOf("desc", "description", "notes", "summary"))
            )
            CsvImportCategory.SCORES -> listOf(
                CsvTargetField("studentId", "Student ID / Reg No", false, "Unique student admission code", listOf("id", "student_id", "reg_no", "admission_no")),
                CsvTargetField("fullName", "Student Full Name", false, "Student's full name", listOf("name", "student", "full_name")),
                CsvTargetField("studentClass", "Class / Grade", false, "Student class level", listOf("class", "grade", "form")),
                CsvTargetField("subjectName", "Subject Title", true, "Name of assessed subject", listOf("subject", "subject_name", "course")),
                CsvTargetField("caScore", "CA Mark (max 40)", false, "Continuous assessment test score", listOf("ca", "ca_score", "test", "assessment")),
                CsvTargetField("examScore", "Exam Mark (max 60)", false, "End of term examination mark", listOf("exam", "exam_score", "examination"))
            )
        }
    }

    // CSV Parsing Logic
    val parsedCsvData = remember(rawCsvText) {
        if (rawCsvText.isBlank()) {
            Pair(emptyList<String>(), emptyList<List<String>>())
        } else {
            val lines = rawCsvText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) {
                Pair(emptyList(), emptyList())
            } else {
                val headerLine = lines.first()
                val delimiter = when {
                    headerLine.contains("\t") -> "\t"
                    headerLine.contains(";") -> ";"
                    else -> ","
                }

                fun parseLine(line: String): List<String> {
                    val result = mutableListOf<String>()
                    var current = StringBuilder()
                    var inQuotes = false
                    for (ch in line) {
                        when {
                            ch == '"' -> inQuotes = !inQuotes
                            ch.toString() == delimiter && !inQuotes -> {
                                result.add(current.toString().trim().removeSurrounding("\""))
                                current = StringBuilder()
                            }
                            else -> current.append(ch)
                        }
                    }
                    result.add(current.toString().trim().removeSurrounding("\""))
                    return result
                }

                val headers = parseLine(headerLine)
                val rows = lines.drop(1).map { parseLine(it) }
                Pair(headers, rows)
            }
        }
    }

    val headers = parsedCsvData.first
    val dataRows = parsedCsvData.second

    // Column Mapping State: Map of Target Field Key -> Selected CSV Header Name
    var columnMappings by remember(headers, targetFields) {
        val initialMap = mutableStateOf<Map<String, String>>(emptyMap())
        // Auto match columns on initial parse
        val autoMapped = mutableMapOf<String, String>()
        targetFields.forEach { field ->
            val matched = headers.find { h ->
                val norm = h.lowercase().replace(" ", "_").replace("-", "_")
                field.keywords.any { kw -> norm.contains(kw) }
            }
            if (matched != null) {
                autoMapped[field.key] = matched
            }
        }
        initialMap.value = autoMapped
        initialMap
    }

    // Row Selection state
    var selectedRowIndices by remember(dataRows) {
        mutableStateOf(dataRows.indices.toSet())
    }

    // Construct mapped objects for preview
    val mappedRecords = remember(dataRows, columnMappings, headers) {
        dataRows.map { row ->
            val map = mutableMapOf<String, String>()
            targetFields.forEach { field ->
                val csvHeader = columnMappings[field.key]
                val headerIndex = if (csvHeader != null) headers.indexOf(csvHeader) else -1
                val cellValue = if (headerIndex in row.indices) row[headerIndex] else ""
                map[field.key] = cellValue
            }
            map
        }
    }

    val isAllRowsSelected = dataRows.isNotEmpty() && selectedRowIndices.size == dataRows.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Banner Card
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
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "CSV Import",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "CSV Import & Mapping Wizard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Seamlessly transition existing school rosters, subjects, and marks from spreadsheets",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Import Category Selector Chips
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SELECT DATA CATEGORY TO IMPORT:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CsvImportCategory.values().forEach { category ->
                            val isSelected = activeCategory == category
                            Surface(
                                selected = isSelected,
                                onClick = {
                                    activeCategory = category
                                    rawCsvText = ""
                                    errorMessage = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("import_category_${category.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = when (category) {
                                            CsvImportCategory.STUDENTS -> Icons.Default.Person
                                            CsvImportCategory.SUBJECTS -> Icons.AutoMirrored.Filled.List
                                            CsvImportCategory.SCORES -> Icons.Default.Star
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(category.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            category.subtitle,
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 1: CSV File Input & Sample Data
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
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Step 1: Provide CSV File or Raw Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (rawCsvText.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    rawCsvText = ""
                                    errorMessage = null
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Data", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { csvLauncher.launch("*/*") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("choose_csv_file_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick CSV File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                rawCsvText = when (activeCategory) {
                                    CsvImportCategory.STUDENTS -> """
Reg_No, Student_Full_Name, Sex, Grade_Level, Academic_Term, Academic_Session, Present_Days
STU/2026/101, Chidi Adebayo, Male, JSS 1, 3rd Term, 2025/2026, 58
STU/2026/102, Fatima Abubakar, Female, JSS 1, 3rd Term, 2025/2026, 60
STU/2026/103, Emeka Okonkwo, Male, JSS 2, 3rd Term, 2025/2026, 55
STU/2026/104, Blessing Danjuma, Female, SS 1, 3rd Term, 2025/2026, 59
""".trimIndent()
                                    CsvImportCategory.SUBJECTS -> """
Subject_Title, Subject_Category, Target_Grade_Level, Notes
Mathematics, Core, All Classes, Core STEM subject covering Algebra and Geometry
English Language, Core, All Classes, Grammar, Comprehension, and Essay Writing
Basic Science, Science, JSS 1, General science for junior secondary
Financial Accounting, Commercial, SS 1, Business finance principles
""".trimIndent()
                                    CsvImportCategory.SCORES -> """
Admission_No, Student_Name, Class_Level, Subject_Title, CA_Test_Score, Exam_Mark
STU/2026/101, Chidi Adebayo, JSS 1, Mathematics, 34.5, 52.0
STU/2026/101, Chidi Adebayo, JSS 1, English Language, 31.0, 48.5
STU/2026/102, Fatima Abubakar, JSS 1, Mathematics, 38.0, 58.0
STU/2026/102, Fatima Abubakar, JSS 1, English Language, 36.5, 54.0
""".trimIndent()
                                }
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("load_sample_csv_button")
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load Sample CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Raw CSV Text Field
                    OutlinedTextField(
                        value = rawCsvText,
                        onValueChange = { rawCsvText = it },
                        placeholder = { Text("Or paste raw CSV text here (Header row on line 1, comma or tab separated)...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("csv_raw_text_input"),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    if (rawCsvText.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Detected ${headers.size} Columns & ${dataRows.size} Data Rows",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = "Header row detected on line 1",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        // Card 2: Interactive Column Mapping Interface
        if (headers.isNotEmpty()) {
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
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Step 2: Map System Fields to CSV Columns",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            TextButton(
                                onClick = {
                                    val autoMapped = mutableMapOf<String, String>()
                                    targetFields.forEach { field ->
                                        val matched = headers.find { h ->
                                            val norm = h.lowercase().replace(" ", "_").replace("-", "_")
                                            field.keywords.any { kw -> norm.contains(kw) }
                                        }
                                        if (matched != null) {
                                            autoMapped[field.key] = matched
                                        }
                                    }
                                    columnMappings = autoMapped
                                },
                                modifier = Modifier.testTag("auto_map_columns_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto-Match", fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        Text(
                            text = "Match each SmartResult system field below with the corresponding header from your CSV file:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        // List of Target Fields with Mapping Dropdowns
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            targetFields.forEach { field ->
                                val selectedCsvHeader = columnMappings[field.key]
                                var expandedDropdown by remember { mutableStateOf(false) }

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = field.label,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (field.isRequired) {
                                                    Text(" *", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                            }
                                            Text(
                                                text = field.description,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Mapping Dropdown Box
                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedButton(
                                                onClick = { expandedDropdown = true },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (selectedCsvHeader != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("map_dropdown_${field.key}")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = selectedCsvHeader ?: "[ Unmapped / Skip ]",
                                                        fontSize = 12.sp,
                                                        fontWeight = if (selectedCsvHeader != null) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedCsvHeader != null) MaterialTheme.colorScheme.primary else Color.Gray,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDropDown,
                                                        contentDescription = "Select Column",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = expandedDropdown,
                                                onDismissRequest = { expandedDropdown = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("[ Unmapped / Skip ]", fontSize = 12.sp, color = Color.Gray) },
                                                    onClick = {
                                                        columnMappings = columnMappings - field.key
                                                        expandedDropdown = false
                                                    }
                                                )
                                                HorizontalDivider()
                                                headers.forEach { header ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                if (selectedCsvHeader == header) {
                                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                }
                                                                Text(header, fontSize = 12.sp, fontWeight = if (selectedCsvHeader == header) FontWeight.Bold else FontWeight.Normal)
                                                            }
                                                        },
                                                        onClick = {
                                                            columnMappings = columnMappings + (field.key to header)
                                                            expandedDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 3: Live Data Preview & Validation Table
        if (dataRows.isNotEmpty()) {
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
                                    text = "Step 3: Preview Mapped Records (${mappedRecords.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isAllRowsSelected,
                                    onCheckedChange = { checked ->
                                        selectedRowIndices = if (checked) dataRows.indices.toSet() else emptySet()
                                    },
                                    modifier = Modifier.testTag("select_all_import_rows_checkbox")
                                )
                                Text(
                                    text = if (isAllRowsSelected) "Deselect All" else "Select All",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        // Preview Table Container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            mappedRecords.forEachIndexed { index, record ->
                                val isSelected = selectedRowIndices.contains(index)
                                val missingRequired = targetFields.filter { it.isRequired }.any { record[it.key].isNullOrBlank() }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedRowIndices = if (isSelected) selectedRowIndices - index else selectedRowIndices + index
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedRowIndices = if (checked) selectedRowIndices + index else selectedRowIndices - index
                                            },
                                            modifier = Modifier.testTag("row_checkbox_$index")
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val primaryValue = record[targetFields.firstOrNull()?.key ?: ""] ?: "Row ${index + 1}"
                                                Text(
                                                    text = primaryValue.ifBlank { "Row ${index + 1}" },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                if (missingRequired) {
                                                    Surface(
                                                        color = Color.Red.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Missing Required Field",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Red,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else {
                                                    Surface(
                                                        color = SchoolGreen.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Valid",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SchoolGreen,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = targetFields.drop(1).take(3).mapNotNull { f ->
                                                    val v = record[f.key]
                                                    if (!v.isNullOrBlank()) "${f.label}: $v" else null
                                                }.joinToString(" • "),
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Execute Import Button
                        Button(
                            onClick = {
                                val selectedRecords = mappedRecords.filterIndexed { idx, _ -> selectedRowIndices.contains(idx) }
                                when (activeCategory) {
                                    CsvImportCategory.STUDENTS -> {
                                        viewModel.importMappedStudents(
                                            mappedRows = selectedRecords,
                                            onSuccess = { count, classes ->
                                                successSummaryMessage = "Successfully imported $count student profiles and created $classes new school classes!"
                                            },
                                            onError = { err -> errorMessage = err }
                                        )
                                    }
                                    CsvImportCategory.SUBJECTS -> {
                                        viewModel.importMappedSubjects(
                                            mappedRows = selectedRecords,
                                            onSuccess = { count ->
                                                successSummaryMessage = "Successfully imported $count subject catalog records!"
                                            },
                                            onError = { err -> errorMessage = err }
                                        )
                                    }
                                    CsvImportCategory.SCORES -> {
                                        viewModel.importMappedScores(
                                            mappedRows = selectedRecords,
                                            onSuccess = { students, scores ->
                                                successSummaryMessage = "Successfully recorded $scores subject scores across $students student profiles!"
                                            },
                                            onError = { err -> errorMessage = err }
                                        )
                                    }
                                }
                            },
                            enabled = selectedRowIndices.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("execute_csv_import_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Execute CSV Import (${selectedRowIndices.size} Records)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
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
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
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

    // Success Summary Completion Dialog
    if (successSummaryMessage != null) {
        AlertDialog(
            onDismissRequest = { successSummaryMessage = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(SchoolGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = SchoolGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "CSV Import Completed!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = successSummaryMessage ?: "",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val msg = successSummaryMessage
                        successSummaryMessage = null
                        rawCsvText = ""
                        when {
                            msg?.contains("student") == true -> viewModel.navigateTo(AppScreen.AddInfo)
                            msg?.contains("subject") == true -> viewModel.navigateTo(AppScreen.ManageSubjects)
                            else -> viewModel.navigateTo(AppScreen.LedgerTable)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("dismiss_csv_success_dialog")
                ) {
                    Text("Done & View Records", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

