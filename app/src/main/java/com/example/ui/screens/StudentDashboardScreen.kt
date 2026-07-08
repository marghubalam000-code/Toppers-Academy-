package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.Student
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.theme.ToppersTeal
import com.example.ui.components.bounceClick
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.ui.viewmodel.ExamSchedule
import com.example.ui.viewmodel.HomeworkItem
import com.example.ui.viewmodel.ClassworkItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val student by viewModel.currentStudent.collectAsState()
    val appLogoUri by viewModel.appLogoUri.collectAsState()
    
    // Safety check
    val activeStudent = student ?: return

    var calendarMonth by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    // Collect all attendance logs for this student
    val attendanceRecords by viewModel.getAttendanceForStudent(activeStudent.studentId).collectAsState(initial = emptyList())
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    val sharedPrefs = remember { context.getSharedPreferences("toppers_student_prefs", android.content.Context.MODE_PRIVATE) }
    var showFeesDialog by remember { mutableStateOf(false) }
    var showExamsDialog by remember { mutableStateOf(false) }
    var showClassworkDialog by remember { mutableStateOf(false) }
    var showHomeworkDialog by remember { mutableStateOf(false) }
    var showLiveClassDialog by remember { mutableStateOf(false) }
    var showTimetableDialog by remember { mutableStateOf(false) }
    var showFitnessDialog by remember { mutableStateOf(false) }
    var showStoreDialog by remember { mutableStateOf(false) }
    var showGalleryDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    var showPhotoEditDialog by remember { mutableStateOf(false) }
    var showPresetPicker by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val b64 = uriToBase64(context, it)
            if (b64 != null) {
                viewModel.updateCurrentStudentPhoto(b64)
                Toast.makeText(context, "Profile photo updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var previewPhotoPath by remember { mutableStateOf<String?>(null) }
    var previewPdfPath by remember { mutableStateOf<String?>(null) }
    var previewPdfName by remember { mutableStateOf("") }
    var previewHomeworkTitle by remember { mutableStateOf("") }
    var previewHomeworkDesc by remember { mutableStateOf("") }

    val homeworkList by viewModel.homeworkList.collectAsState()
    val classworkList by viewModel.classworkList.collectAsState()
    val examsList by viewModel.examsList.collectAsState()
    val announcementsList by viewModel.announcementsList.collectAsState()
    val communityMessages by viewModel.communityMessages.collectAsState()

    val myHomework = remember(homeworkList, activeStudent) {
        homeworkList.filter {
            it.studentClass.equals(activeStudent.studentClass, ignoreCase = true) &&
            (it.section.equals("ALL", ignoreCase = true) || it.section.equals(activeStudent.section, ignoreCase = true))
        }
    }
    val myClasswork = remember(classworkList, activeStudent) {
        classworkList.filter {
            it.studentClass.equals(activeStudent.studentClass, ignoreCase = true) &&
            (it.section.equals("ALL", ignoreCase = true) || it.section.equals(activeStudent.section, ignoreCase = true))
        }
    }
    val myExams = remember(examsList, activeStudent) {
        examsList.filter {
            it.studentClass.equals(activeStudent.studentClass, ignoreCase = true)
        }
    }
    val myAnnouncements = remember(announcementsList, activeStudent) {
        announcementsList.filter {
            it.targetClass.equals("ALL", ignoreCase = true) ||
            it.targetClass.equals(activeStudent.studentClass, ignoreCase = true)
        }
    }

    val isDark = MaterialTheme.colorScheme.background == com.example.ui.theme.DarkBackground
    val calendarCardBg = if (isDark) Color(0xFF1E2129) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val calendarTextPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val calendarTextSecondary = if (isDark) Color(0xFF8F9CAE) else MaterialTheme.colorScheme.onSurfaceVariant
    val dayEmptyColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)

    // Metrics calculations
    val totalDays = attendanceRecords.size
    val presentCount = attendanceRecords.count { it.status.equals("Present", ignoreCase = true) }
    val absentCount = attendanceRecords.count { it.status.equals("Absent", ignoreCase = true) }
    val lateCount = attendanceRecords.count { it.status.equals("Late", ignoreCase = true) }
    
    val attendancePercentage = if (totalDays > 0) {
        ((presentCount + lateCount).toFloat() / totalDays.toFloat() * 100f).toInt()
    } else {
        100
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (appLogoUri.isNotEmpty()) {
                                AsyncImage(
                                    model = appLogoUri,
                                    contentDescription = "App Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_icon_foreground),
                                    contentDescription = "Default School Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Text(
                            text = "Toppers Student Portal",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    val themePref by viewModel.themePreference.collectAsState()
                    val themeIcon = when (themePref) {
                        "light" -> Icons.Default.LightMode
                        "dark" -> Icons.Default.DarkMode
                        else -> Icons.Default.Settings
                    }
                    var showThemeDialog by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.testTag("student_theme_btn")
                    ) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = "Change Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (showThemeDialog) {
                        ThemeSettingsDialog(
                            viewModel = viewModel,
                            onDismiss = { showThemeDialog = false }
                        )
                    }

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Reloading student data...", Toast.LENGTH_SHORT).show()
                            viewModel.reloadStudentPortalData(activeStudent.studentId) { success ->
                                if (success) {
                                    Toast.makeText(context, "Student data updated!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to refresh data", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("student_reload_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            showLogoutConfirmDialog = true
                        },
                        modifier = Modifier.testTag("student_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Log Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Student Welcome Profile Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Image or Initials
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable { showPhotoEditDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            StudentPhoto(
                                photoData = activeStudent.photo,
                                modifier = Modifier.fillMaxSize(),
                                placeholderIcon = Icons.Default.School,
                                tintColor = MaterialTheme.colorScheme.primary
                            )
                            // Elegant semi-transparent edit overlay with camera icon
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .align(Alignment.BottomCenter),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Edit Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Student Meta Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Namaste,",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeStudent.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ID: ${activeStudent.studentId}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Class: ${activeStudent.studentClass} | Section: ${activeStudent.section}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Attendance Summary Section (Interactive Calendar Card)
            item {
                val selectedYear = calendarMonth.get(Calendar.YEAR)
                val selectedMonth = calendarMonth.get(Calendar.MONTH)

                // Calculate calendar grids dynamically
                val tempCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val firstDayOfWeekVal = tempCal.get(Calendar.DAY_OF_WEEK)
                val daysInMonthVal = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                // Monday-first indexing: 0 = Mon, 6 = Sun
                val firstDayOfWeekIndexVal = (firstDayOfWeekVal + 5) % 7

                val calendarCells = remember(selectedYear, selectedMonth) {
                    val list = mutableListOf<Int?>()
                    for (i in 0 until firstDayOfWeekIndexVal) {
                        list.add(null)
                    }
                    for (d in 1..daysInMonthVal) {
                        list.add(d)
                    }
                    while (list.size % 7 != 0) {
                        list.add(null)
                    }
                    list
                }

                // Compute monthly counts based on selected calendar month
                val monthlyPresentCount = remember(attendanceRecords, selectedYear, selectedMonth) {
                    attendanceRecords.count { record ->
                        val parts = record.date.split("-")
                        if (parts.size == 3) {
                            val rYear = parts[0].toIntOrNull()
                            val rMonth = parts[1].toIntOrNull()
                            rYear == selectedYear && rMonth == (selectedMonth + 1) && record.status.equals("Present", ignoreCase = true)
                        } else false
                    }
                }

                val monthlyAbsentCount = remember(attendanceRecords, selectedYear, selectedMonth) {
                    attendanceRecords.count { record ->
                        val parts = record.date.split("-")
                        if (parts.size == 3) {
                            val rYear = parts[0].toIntOrNull()
                            val rMonth = parts[1].toIntOrNull()
                            rYear == selectedYear && rMonth == (selectedMonth + 1) && record.status.equals("Absent", ignoreCase = true)
                        } else false
                    }
                }

                val monthlyLateCount = remember(attendanceRecords, selectedYear, selectedMonth) {
                    attendanceRecords.count { record ->
                        val parts = record.date.split("-")
                        if (parts.size == 3) {
                            val rYear = parts[0].toIntOrNull()
                            val rMonth = parts[1].toIntOrNull()
                            rYear == selectedYear && rMonth == (selectedMonth + 1) && 
                            (record.status.equals("Late", ignoreCase = true) || record.status.equals("Half Day", ignoreCase = true) || record.status.contains("Half", ignoreCase = true))
                        } else false
                    }
                }

                val monthlyLeaveCount = remember(attendanceRecords, selectedYear, selectedMonth) {
                    attendanceRecords.count { record ->
                        val parts = record.date.split("-")
                        if (parts.size == 3) {
                            val rYear = parts[0].toIntOrNull()
                            val rMonth = parts[1].toIntOrNull()
                            rYear == selectedYear && rMonth == (selectedMonth + 1) && 
                            (record.status.equals("Leave", ignoreCase = true) || record.status.equals("Excused", ignoreCase = true))
                        } else false
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attendance_calendar_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = calendarCardBg), // Adaptive theme background
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Attendance Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = calendarTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val classText = if (activeStudent.studentClass.isNotBlank()) {
                                    "Class ${activeStudent.studentClass} (${activeStudent.section.ifBlank { "2025-27" }})"
                                } else {
                                    "Class 12th (2025-27)"
                                }
                                Text(
                                    text = classText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = calendarTextSecondary
                                )
                            }

                            // Month/Year navigation
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        calendarMonth = (calendarMonth.clone() as Calendar).apply {
                                            add(Calendar.MONTH, -1)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowLeft,
                                        contentDescription = "Previous Month",
                                        tint = ToppersTeal, // Elegant brand tint
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                                val monthStr = monthNames[selectedMonth]
                                Text(
                                    text = "$monthStr-$selectedYear",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = calendarTextPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = {
                                        calendarMonth = (calendarMonth.clone() as Calendar).apply {
                                            add(Calendar.MONTH, 1)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Next Month",
                                        tint = ToppersTeal,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Weekday names
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            weekdays.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = calendarTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Calendar numbers grid
                        val chunkedRows = calendarCells.chunked(7)
                        chunkedRows.forEach { weekRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                weekRow.forEach { day ->
                                    if (day == null) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val dateString = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, day)
                                        val recordOnDay = attendanceRecords.find { it.date == dateString }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (recordOnDay != null) {
                                                val highlightColor = when {
                                                    recordOnDay.status.equals("Present", ignoreCase = true) -> Color(0xFF2E7D32)
                                                    recordOnDay.status.equals("Absent", ignoreCase = true) -> Color(0xFFC62828)
                                                    recordOnDay.status.equals("Late", ignoreCase = true) || recordOnDay.status.equals("Half Day", ignoreCase = true) || recordOnDay.status.contains("Half", ignoreCase = true) -> Color(0xFFD97706)
                                                    recordOnDay.status.equals("Leave", ignoreCase = true) || recordOnDay.status.equals("Excused", ignoreCase = true) -> Color(0xFFFFC107) // Yellow for Leave
                                                    else -> Color(0xFF1565C0) // Other / Holiday
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(highlightColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val dayStr = if (day < 10) "0$day" else "$day"
                                                    Text(
                                                        text = dayStr,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 13.sp,
                                                        color = Color.White
                                                    )
                                                }
                                            } else {
                                                val dayStr = if (day < 10) "0$day" else "$day"
                                                Text(
                                                    text = dayStr,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = dayEmptyColor,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Footer Row of Status Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                symbol = "✓",
                                count = monthlyPresentCount,
                                color = Color(0xFF4CAF50), // Vibrant Green
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(
                                symbol = "✕",
                                count = monthlyAbsentCount,
                                color = Color(0xFFEF5350), // Vibrant Red
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(
                                symbol = "—",
                                count = monthlyLateCount,
                                color = Color(0xFFFFB300), // Vibrant Amber/Yellow
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(
                                symbol = "🏃",
                                count = monthlyLeaveCount,
                                color = Color(0xFFFFC107), // Vibrant Yellow (🏃 Runner/Leave status)
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // School Updates Section: Redesigned into beautiful Premium Icon Grid + Announcements & Messages Noticeboard
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "My School Updates & Work",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )

                        // 10-item premium icon grid exactly matching user's image
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                DashboardIcon(
                                    icon = Icons.Default.Payments,
                                    label = "Fees",
                                    containerColor = if (isDark) Color(0xFF1B3D22) else Color(0xFFE8F5E9),
                                    iconColor = Color(0xFF4CAF50),
                                    onClick = { showFeesDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardIcon(
                                    icon = Icons.Default.Assignment,
                                    label = "Exams",
                                    containerColor = if (isDark) Color(0xFF1B2D4A) else Color(0xFFE3F2FD),
                                    iconColor = Color(0xFF2196F3),
                                    onClick = { showExamsDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardIcon(
                                    icon = Icons.Default.Computer,
                                    label = "Classwork",
                                    containerColor = if (isDark) Color(0xFF3E3A24) else Color(0xFFFFFDE7),
                                    iconColor = Color(0xFFFFB300),
                                    onClick = { showClassworkDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardIcon(
                                    icon = Icons.Default.Book,
                                    label = "Homework",
                                    containerColor = if (isDark) Color(0xFF421E1C) else Color(0xFFFFEBEE),
                                    iconColor = Color(0xFFEF5350),
                                    onClick = { showHomeworkDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Row 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                DashboardIcon(
                                    icon = Icons.Default.Timer,
                                    label = "Live Class",
                                    containerColor = if (isDark) Color(0xFF3E2D1E) else Color(0xFFFFF3E0),
                                    iconColor = Color(0xFFFF9800),
                                    onClick = { showLiveClassDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardIcon(
                                    icon = Icons.Default.DateRange,
                                    label = "Timetable",
                                    containerColor = if (isDark) Color(0xFF3E341F) else Color(0xFFFFF9C4),
                                    iconColor = Color(0xFFFBC02D),
                                    onClick = { showTimetableDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardIcon(
                                    icon = Icons.Default.TrendingUp,
                                    label = "Fitness",
                                    containerColor = if (isDark) Color(0xFF1C2D3D) else Color(0xFFE0F7FA),
                                    iconColor = Color(0xFF00BCD4),
                                    onClick = { showFitnessDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardIcon(
                                    icon = Icons.Default.ShoppingBag,
                                    label = "Store",
                                    containerColor = if (isDark) Color(0xFF1E353A) else Color(0xFFE0F2F1),
                                    iconColor = Color(0xFF009688),
                                    onClick = { showStoreDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Row 3
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                DashboardIcon(
                                    icon = Icons.Default.Image,
                                    label = "Gallery",
                                    containerColor = if (isDark) Color(0xFF3E361B) else Color(0xFFFFF8E1),
                                    iconColor = Color(0xFFFFC107),
                                    onClick = { showGalleryDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardIcon(
                                    icon = Icons.Default.Remove,
                                    label = "Leave",
                                    containerColor = if (isDark) Color(0xFF2B2D31) else Color(0xFFECEFF1),
                                    iconColor = Color(0xFF78909C),
                                    onClick = { showLeaveDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Noticeboard Section (Announcements & Community Chat)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Campus Bulletin & Discussion Board",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )

                        var noticeboardTab by remember { mutableStateOf(0) }
                        ScrollableTabRow(
                            selectedTabIndex = noticeboardTab,
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = noticeboardTab == 0,
                                onClick = { noticeboardTab = 0 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Announcements", fontWeight = if (noticeboardTab == 0) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp)
                                        if (myAnnouncements.isNotEmpty()) {
                                            Badge { Text(myAnnouncements.size.toString()) }
                                        }
                                    }
                                }
                            )
                            Tab(
                                selected = noticeboardTab == 1,
                                onClick = { noticeboardTab = 1 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Live Discussion Chat", fontWeight = if (noticeboardTab == 1) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp)
                                        if (communityMessages.isNotEmpty()) {
                                            Badge { Text(communityMessages.size.toString()) }
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (noticeboardTab == 0) {
                            if (myAnnouncements.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("No announcements posted", fontSize = 13.sp, color = Color.Gray)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    myAnnouncements.forEach { item ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {
                                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(item.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                                            Text(dateStr, fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Community messages
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                var newMessage by remember { mutableStateOf("") }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(6.dp)
                                ) {
                                    if (communityMessages.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No community messages yet", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    } else {
                                        val sortedMessages = remember(communityMessages) { communityMessages.sortedBy { it.timestamp } }
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            items(sortedMessages) { msg ->
                                                val isMe = msg.sender == activeStudent.name
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 4.dp),
                                                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .widthIn(max = 240.dp)
                                                            .background(
                                                                if (isMe) MaterialTheme.colorScheme.primaryContainer
                                                                else MaterialTheme.colorScheme.surface,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("${msg.sender} (${msg.role})", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                                            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                                                            Text(timeStr, fontSize = 8.sp, color = Color.Gray)
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(msg.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newMessage,
                                        onValueChange = { newMessage = it },
                                        placeholder = { Text("Write message...", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                    )
                                    IconButton(
                                        onClick = {
                                            if (newMessage.trim().isNotEmpty()) {
                                                viewModel.sendCommunityMessage(activeStudent.name, "Student", newMessage.trim())
                                                newMessage = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Student General Bio Information Details
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "My Profile Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        BioRow(icon = Icons.Default.Person, label = "Father Name", value = activeStudent.fatherName)
                        if (activeStudent.fatherMobile.isNotEmpty()) {
                            BioRow(icon = Icons.Default.Phone, label = "Father Mobile", value = activeStudent.fatherMobile)
                        }
                        BioRow(icon = Icons.Default.Person, label = "Mother Name", value = activeStudent.motherName)
                        BioRow(icon = Icons.Default.Phone, label = "Mobile", value = activeStudent.mobile)
                        if (activeStudent.email.isNotEmpty()) {
                            BioRow(icon = Icons.Default.Email, label = "Email", value = activeStudent.email)
                        }
                        BioRow(icon = Icons.Default.Cake, label = "Date of Birth", value = activeStudent.dob)
                        BioRow(icon = Icons.Default.Home, label = "Address", value = activeStudent.address)
                    }
                }
            }

            // Recent Attendance History List Section
            item {
                Text(
                    text = "My Attendance Logs",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            if (attendanceRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "No Logs",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No registered attendance records found.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Sort attendance records by date descending
                val sortedRecords = attendanceRecords.sortedByDescending { it.date }
                items(sortedRecords) { record ->
                    AttendanceHistoryRow(record = record)
                }
            }

            // Footer Credit Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    MarghubSignatureBadge(
                        onClick = { showDeveloperDialog = true }
                    )
                }
            }
        }
    }

    if (showDeveloperDialog) {
        MarghubProfileDialog(
            onDismiss = { showDeveloperDialog = false }
        )
    }

    // Fullscreen Photo Viewer Dialog
    previewPhotoPath?.let { path ->
        AlertDialog(
            onDismissRequest = { previewPhotoPath = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Image Attachment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val fileName = "Homework_Photo_${System.currentTimeMillis()}.jpg"
                            val downloadedPath = downloadFileToPublicDownloads(context, path, fileName)
                            if (downloadedPath != null) {
                                Toast.makeText(context, "Downloaded photo: $downloadedPath", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to download", Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.bounceClick()) {
                            Icon(Icons.Default.Download, contentDescription = "Download Photo", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { previewPhotoPath = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = java.io.File(path),
                        contentDescription = "Full Attachment Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { previewPhotoPath = null }) {
                    Text("Go Back")
                }
            }
        )
    }

    // Modern High-Fidelity Interactive PDF Viewer Dialog
    previewPdfPath?.let { path ->
        AlertDialog(
            onDismissRequest = { previewPdfPath = null },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(22.dp))
                        Text(
                            text = previewPdfName.takeIf { it.isNotEmpty() } ?: "Document.pdf",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val fileName = if (previewPdfName.isNotEmpty()) previewPdfName else "Homework_${System.currentTimeMillis()}.pdf"
                            val downloadedPath = downloadFileToPublicDownloads(context, path, fileName)
                            if (downloadedPath != null) {
                                Toast.makeText(context, "Downloaded PDF: $downloadedPath", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to download", Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.bounceClick()) {
                            Icon(Icons.Default.Download, contentDescription = "Download PDF", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { previewPdfPath = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Toolbar Simulation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Page 1 of 1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Default.Print, contentDescription = "Print Document", modifier = Modifier.size(16.dp), tint = Color.Gray)
                        }
                    }

                    // Simulated Page Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Header banner
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("TOPPERS ACADEMY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                                Text("ASSIGNMENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                            
                            Divider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

                            Text(
                                text = previewHomeworkTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )

                            Text(
                                text = "Instructions & Assignment Tasks:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )

                            Text(
                                text = previewHomeworkDesc,
                                fontSize = 12.sp,
                                color = Color.Black,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            Divider(color = Color.LightGray, thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Teacher Signature", fontSize = 9.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("M. Rahman", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Grade / Remarks", fontSize = 9.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Pending", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { previewPdfPath = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close Document")
                }
            }
        )
    }

    if (showPhotoEditDialog) {
        StudentPhotoEditDialog(
            onDismiss = { showPhotoEditDialog = false },
            onLaunchGallery = { galleryLauncher.launch("image/*") },
            onOpenPresetPicker = { showPresetPicker = true },
            onRemovePhoto = { viewModel.updateCurrentStudentPhoto("") },
            currentPhoto = activeStudent.photo
        )
    }

    if (showPresetPicker) {
        StudentPresetPickerDialog(
            onDismiss = { showPresetPicker = false },
            onSelectPreset = { presetKey ->
                viewModel.updateCurrentStudentPhoto(presetKey)
                showPhotoEditDialog = false
            }
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Logout Confirm",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to log out from this ID?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logoutStudent()
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirmDialog = false }
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    if (showFeesDialog) {
        StudentFeesDialog(
            onDismiss = { showFeesDialog = false },
            viewModel = viewModel,
            studentId = activeStudent.studentId
        )
    }

    if (showExamsDialog) {
        StudentExamsDialog(
            onDismiss = { showExamsDialog = false },
            myExams = myExams
        )
    }

    if (showClassworkDialog) {
        StudentClassworkDialog(
            onDismiss = { showClassworkDialog = false },
            myClasswork = myClasswork
        )
    }

    if (showHomeworkDialog) {
        StudentHomeworkDialog(
            onDismiss = { showHomeworkDialog = false },
            myHomework = myHomework,
            onPreviewPhoto = { previewPhotoPath = it },
            onPreviewPdf = { path, name, title, desc ->
                previewPdfPath = path
                previewPdfName = name
                previewHomeworkTitle = title
                previewHomeworkDesc = desc
            }
        )
    }

    if (showLiveClassDialog) {
        StudentLiveClassDialog(
            onDismiss = { showLiveClassDialog = false },
            studentName = activeStudent.name
        )
    }

    if (showTimetableDialog) {
        StudentTimetableDialog(
            onDismiss = { showTimetableDialog = false }
        )
    }

    if (showFitnessDialog) {
        StudentFitnessDialog(
            onDismiss = { showFitnessDialog = false },
            sharedPrefs = sharedPrefs,
            studentId = activeStudent.studentId
        )
    }

    if (showStoreDialog) {
        StudentStoreDialog(
            onDismiss = { showStoreDialog = false }
        )
    }

    if (showGalleryDialog) {
        StudentGalleryDialog(
            onDismiss = { showGalleryDialog = false }
        )
    }

    if (showLeaveDialog) {
        StudentLeaveDialog(
            onDismiss = { showLeaveDialog = false },
            viewModel = viewModel,
            studentId = activeStudent.studentId
        )
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BioRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AttendanceHistoryRow(record: AttendanceRecord) {
    val isPresent = record.status.equals("Present", ignoreCase = true)
    val isLate = record.status.equals("Late", ignoreCase = true)
    
    val badgeColor = when {
        isPresent -> Color(0xFFE6F4EA) // Light green
        isLate -> Color(0xFFFFF7E6) // Light yellow
        else -> Color(0xFFFCE8E6) // Light red
    }
    
    val textColor = when {
        isPresent -> Color(0xFF137333)
        isLate -> Color(0xFFB06000)
        else -> Color(0xFFC5221F)
    }

    // Date formatting for a cleaner display
    val formattedDate = try {
        val originalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateObj = originalFormat.parse(record.date)
        val cleanFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        if (dateObj != null) cleanFormat.format(dateObj) else record.date
    } catch (e: Exception) {
        record.date
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDate,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = record.status.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    symbol: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(50.dp),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
        ) {
            Text(
                text = symbol,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = count.toString(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun DashboardIcon(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(4.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(containerColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StudentFeesDialog(
    onDismiss: () -> Unit,
    viewModel: com.example.ui.viewmodel.AttendanceViewModel,
    studentId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allFees by viewModel.allFeesList.collectAsState()
    val studentFees = remember(allFees, studentId) {
        allFees.filter { it.studentId == studentId }
    }

    val upiId by viewModel.upiId.collectAsState()
    val upiName by viewModel.upiName.collectAsState()
    val upiQrUrl by viewModel.upiQrUrl.collectAsState()

    var activePaymentFee by remember { mutableStateOf<com.example.data.model.FeeRecord?>(null) }
    var transactionIdInput by remember { mutableStateOf("") }
    var screenshotUriState by remember { mutableStateOf<Uri?>(null) }
    var isUploadingReceipt by remember { mutableStateOf(false) }
    var receiptProgress by remember { mutableStateOf(0f) }

    val screenshotPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            screenshotUriState = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text(
                        text = if (activePaymentFee != null) "Submit Payment" else "My Academic Fees",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                IconButton(onClick = {
                    if (activePaymentFee != null) {
                        activePaymentFee = null
                    } else {
                        onDismiss()
                    }
                }) {
                    Icon(if (activePaymentFee != null) Icons.Default.ArrowBack else Icons.Default.Close, contentDescription = "Back")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activePaymentFee == null) {
                    // 1. Show all fee demands list
                    if (studentFees.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No fee demands issued for you yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val unpaidFees = studentFees.filter { it.status == "Unpaid" || it.status == "Rejected" }
                        val pendingFees = studentFees.filter { it.status == "Pending" }
                        val paidFees = studentFees.filter { it.status == "Paid" }

                        if (unpaidFees.isNotEmpty()) {
                            Text("Unpaid Fees:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            unpaidFees.forEach { fee ->
                                FeeDemandCard(fee = fee, onPay = { activePaymentFee = fee })
                            }
                        }

                        if (pendingFees.isNotEmpty()) {
                            Text("Pending Verification:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFF59E0B))
                            pendingFees.forEach { fee ->
                                FeeDemandCard(fee = fee, onPay = {})
                            }
                        }

                        if (paidFees.isNotEmpty()) {
                            Text("Cleared Fees:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF10B981))
                            paidFees.forEach { fee ->
                                FeeDemandCard(fee = fee, onPay = {})
                            }
                        }
                    }
                } else {
                    // 2. Submit payment screenshot / Transaction ID for activePaymentFee
                    val targetFee = activePaymentFee!!
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Paying For: ${targetFee.title}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Amount Due: ₹${targetFee.amount}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text(text = "Scan QR Code or Use UPI details below:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    // QR scanner box
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (upiQrUrl.isNotEmpty()) {
                            AsyncImage(
                                model = upiQrUrl,
                                contentDescription = "Admin QR Scanner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No QR scanner uploaded", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // UPI details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("UPI ID:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(upiId.ifEmpty { "Not configured" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payee Name:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(upiName.ifEmpty { "Not configured" }, fontSize = 12.sp)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text(text = "Enter payment proof details:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = transactionIdInput,
                        onValueChange = { transactionIdInput = it },
                        label = { Text("Transaction ID / UTR Reference") },
                        modifier = Modifier.fillMaxWidth().testTag("student_fee_txid"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Screenshot Upload Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { screenshotPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (screenshotUriState != null) {
                            AsyncImage(
                                model = screenshotUriState,
                                contentDescription = "Screenshot Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Upload Payment Screenshot", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Tap to choose file", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (isUploadingReceipt) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { receiptProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                            Text("Uploading Receipt... ${(receiptProgress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (transactionIdInput.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter your Transaction ID", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (screenshotUriState == null) {
                                Toast.makeText(context, "Please upload payment screenshot", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.submitFeePayment(
                                feeId = targetFee.feeId,
                                transactionId = transactionIdInput,
                                screenshotUri = screenshotUriState,
                                upiUsed = upiId
                            )
                            Toast.makeText(context, "Payment proof submitted successfully!", Toast.LENGTH_SHORT).show()
                            activePaymentFee = null
                            transactionIdInput = ""
                            screenshotUriState = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Payment for Verification", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun FeeDemandCard(
    fee: com.example.data.model.FeeRecord,
    onPay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(fee.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Due: ${fee.dueDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("₹${fee.amount}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            }

            val statusColor = when (fee.status) {
                "Paid" -> Color(0xFF10B981)
                "Pending" -> Color(0xFFF59E0B)
                "Rejected" -> Color(0xFFEF4444)
                else -> MaterialTheme.colorScheme.error
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    contentColor = statusColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = fee.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (fee.status == "Unpaid" || fee.status == "Rejected") {
                    Button(
                        onClick = onPay,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (fee.status == "Rejected") "Retry Pay" else "Pay Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (fee.status == "Rejected" && fee.rejectionReason.isNotEmpty()) {
                Text(
                    text = "Rejection Reason: ${fee.rejectionReason}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun StudentExamsDialog(
    onDismiss: () -> Unit,
    myExams: List<ExamSchedule>
) {
    var selectedExamTab by remember { mutableStateOf(0) } // 0: Datesheet, 1: Syllabus

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(24.dp))
                    Text("Exams & Performance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabRow(selectedTabIndex = selectedExamTab, modifier = Modifier.fillMaxWidth()) {
                    Tab(selected = selectedExamTab == 0, onClick = { selectedExamTab = 0 }) {
                        Text("Datesheet", fontSize = 12.sp, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
                    }
                    Tab(selected = selectedExamTab == 1, onClick = { selectedExamTab = 1 }) {
                        Text("Syllabus", fontSize = 12.sp, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedExamTab == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Assigned Examinations:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            if (myExams.isEmpty()) {
                                ExamScheduleCard("Mathematics Prep Exam", "15-Jul-2026", "10:00 AM - 01:00 PM", "Max 100 Marks")
                                ExamScheduleCard("General Science Test", "17-Jul-2026", "10:00 AM - 01:00 PM", "Max 100 Marks")
                                ExamScheduleCard("English Literature", "19-Jul-2026", "10:00 AM - 01:00 PM", "Max 80 Marks")
                            } else {
                                myExams.forEach { exam ->
                                    ExamScheduleCard(exam.subject, exam.date, exam.time, "Max ${exam.maxMarks} Marks")
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Exam Syllabi for Term-1:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            SyllabusAccordion("Mathematics", listOf("Unit 1: Real Numbers & Linear Equations", "Unit 2: Quadratic Equations & Progressions", "Unit 3: Trigonometry Identities"))
                            SyllabusAccordion("General Science", listOf("Unit 1: Chemical Reactions & Acids", "Unit 2: Carbon and its Compounds", "Unit 3: Light Reflection & Human Eye"))
                            SyllabusAccordion("English Literature", listOf("Prose: Merchant of Venice - Acts I & II", "Poetry: Daffodils, The Patriot", "Grammar: Direct & Indirect Speech, Voice"))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ExamScheduleCard(subject: String, date: String, time: String, marks: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(subject, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Text(marks, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Date: $date", fontSize = 11.sp, color = Color.Gray)
                Text("Time: $time", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SyllabusAccordion(subject: String, chapters: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(subject, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    chapters.forEach { chapter ->
                        Text("• $chapter", fontSize = 11.sp, color = Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun StudentClassworkDialog(
    onDismiss: () -> Unit,
    myClasswork: List<ClassworkItem>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Computer, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(24.dp))
                    Text("Daily Classwork Tracker", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Assigned Class Activities:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (myClasswork.isEmpty()) {
                            ClassworkCard("Introduction to Electromagnetic Waves", "03-Jul-2026", "Physics - Classwork #06", "Watch video lecture, note down wave categories, speed of light formulas, and equations in lab file.")
                            ClassworkCard("Types of Chemical Reactions", "02-Jul-2026", "Chemistry - Classwork #04", "Draw synthesis, decomposition and double displacement diagrams in organic science journals.")
                            ClassworkCard("Grammar: Active and Passive Voice", "01-Jul-2026", "English - Classwork #03", "Complete exercises 1 to 10 on page 44 of English Practice Workbook.")
                        } else {
                            myClasswork.forEach { item ->
                                ClassworkCard(item.title, item.date, "Classwork", item.description)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Back to Dashboard")
            }
        }
    )
}

@Composable
fun ClassworkCard(title: String, date: String, tag: String, description: String) {
    var completed by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (completed) Color(0xFF81C784) else Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (completed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary)
                Text(date, fontSize = 10.sp, color = Color.Gray)
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
            Text(description, fontSize = 11.sp, color = Color.DarkGray)
            
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (completed) "✓ Completed" else "Status: Incomplete",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) Color(0xFF2E7D32) else Color.Red
                )
                TextButton(
                    onClick = { completed = !completed },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (completed) "Mark Pending" else "Mark Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StudentHomeworkDialog(
    onDismiss: () -> Unit,
    myHomework: List<HomeworkItem>,
    onPreviewPhoto: (String) -> Unit,
    onPreviewPdf: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(24.dp))
                    Text("Homework & Submissions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Homework Assigned:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (myHomework.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No homework assigned", fontSize = 13.sp, color = Color.Gray)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            myHomework.forEach { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Due: ${item.dueDate}", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.description, fontSize = 12.sp, color = Color.DarkGray)
                                    
                                    if (item.photoPath.isNotEmpty() || item.pdfPath.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (item.photoPath.isNotEmpty()) {
                                                val localFile = java.io.File(item.photoPath)
                                                if (localFile.exists()) {
                                                    Card(
                                                        onClick = { 
                                                            onPreviewPhoto(item.photoPath)
                                                        },
                                                        modifier = Modifier.weight(1f).height(50.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                AsyncImage(
                                                                    model = localFile,
                                                                    contentDescription = "Th",
                                                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(4.dp)),
                                                                    contentScale = ContentScale.Crop
                                                                )
                                                                Text("Photo", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    val fileName = "Homework_Photo_${System.currentTimeMillis()}.jpg"
                                                                    val downloadedPath = downloadFileToPublicDownloads(context, item.photoPath, fileName)
                                                                    if (downloadedPath != null) {
                                                                        Toast.makeText(context, "Downloaded photo: $downloadedPath", Toast.LENGTH_LONG).show()
                                                                    } else {
                                                                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                },
                                                                modifier = Modifier.size(32.dp).bounceClick()
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Download,
                                                                    contentDescription = "Download Photo",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            if (item.pdfPath.isNotEmpty()) {
                                                Card(
                                                    onClick = { 
                                                        onPreviewPdf(item.pdfPath, item.pdfName, item.title, item.description)
                                                    },
                                                    modifier = Modifier.weight(1f).height(50.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                                                            Text(
                                                                text = if (item.pdfName.isNotEmpty()) item.pdfName else "Document.pdf",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val fileName = if (item.pdfName.isNotEmpty()) item.pdfName else "Homework_${System.currentTimeMillis()}.pdf"
                                                                val downloadedPath = downloadFileToPublicDownloads(context, item.pdfPath, fileName)
                                                                if (downloadedPath != null) {
                                                                    Toast.makeText(context, "Downloaded PDF: $downloadedPath", Toast.LENGTH_LONG).show()
                                                                } else {
                                                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                                                }
                                                            },
                                                            modifier = Modifier.size(32.dp).bounceClick()
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Download,
                                                                contentDescription = "Download PDF",
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
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun StudentLiveClassDialog(
    onDismiss: () -> Unit,
    studentName: String
) {
    var inLiveRoom by remember { mutableStateOf(false) }
    var inputMsg by remember { mutableStateOf("") }
    
    val liveChatMessages = remember {
        mutableStateListOf(
            "Rahul Kumar: Good morning, Sir!" to "Rahul Kumar",
            "Simran Shah: Sir, will this coordinate system topic come in prep test?" to "Simran Shah",
            "Amit Patel: Formula clear, thank you sir!" to "Amit Patel"
        )
    }

    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
                    Text("Toppers Virtual Classroom", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!inLiveRoom) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
                                Text("LIVE NOW", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                            Text("Mathematics: Quadrilaterals & Geometry", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                            Text("Teacher: Prof. M. Rahman", fontSize = 11.sp, color = Color.DarkGray)
                            Text("Time: 09:30 AM - 10:45 AM (15 mins remaining)", fontSize = 11.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { inLiveRoom = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                            ) {
                                Text("Join Live Stream Lecture", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Upcoming Live Lectures:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    UpcomingLiveRow("Physics: Electric Field", "Today, 02:00 PM", "Dr. S. K. Roy")
                    UpcomingLiveRow("Chemistry: Periodic Table", "Tomorrow, 11:30 AM", "Ms. Anjali Sen")
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(Color.Black, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Podcasts, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("STREAM ACTIVE: Prof. M. Rahman", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Topic: Solving Area under Curves", color = Color.LightGray, fontSize = 9.sp)
                            }
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).background(Color.Red, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("Live Room Discussion Chat:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(liveChatMessages) { (text, sender) ->
                                    val isMe = sender == studentName
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isMe) Color(0xFFFFE0B2) else MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(6.dp)
                                        ) {
                                            Text(text, fontSize = 10.sp, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputMsg,
                                onValueChange = { inputMsg = it },
                                placeholder = { Text("Ask a query...", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (inputMsg.trim().isNotEmpty()) {
                                        val typed = inputMsg.trim()
                                        liveChatMessages.add("$studentName: $typed" to studentName)
                                        inputMsg = ""
                                        
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(1500)
                                            val teacherReplies = listOf(
                                                "Prof. M. Rahman: Wonderful query @$studentName, we will address this in precisely 5 minutes!",
                                                "Prof. M. Rahman: Spot on @$studentName! That represents the fundamental theorem.",
                                                "Prof. M. Rahman: Please note down that statement class. Excellent question @$studentName."
                                            )
                                            liveChatMessages.add(teacherReplies.random() to "Prof. M. Rahman")
                                        }
                                    }
                                },
                                modifier = Modifier.background(Color(0xFFFF9800), CircleShape).size(36.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }

                        OutlinedButton(
                            onClick = { inLiveRoom = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Exit Virtual Classroom", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun UpcomingLiveRow(subject: String, time: String, teacher: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(subject, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("By $teacher", fontSize = 10.sp, color = Color.Gray)
            }
            Text(time, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        }
    }
}

@Composable
fun StudentTimetableDialog(onDismiss: () -> Unit) {
    var selectedDay by remember { mutableStateOf("Mon") }
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(24.dp))
                    Text("Class Weekly Schedule", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEach { day ->
                        Surface(
                            onClick = { selectedDay = day },
                            color = if (selectedDay == day) Color(0xFFFFF59D) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.width(44.dp).height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(day, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedDay == day) Color.Black else Color.Gray)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (selectedDay) {
                            "Mon" -> {
                                TimetableRow("Period 1", "08:30 AM", "English Lit.", "Room 102", "Ms. A. Sharma")
                                TimetableRow("Period 2", "09:15 AM", "Algebra", "Room 102", "Mr. S. K. Singh")
                                TimetableRow("Period 3", "10:15 AM", "Physics", "Science Lab A", "Dr. S. K. Roy")
                                TimetableRow("Period 4", "11:30 AM", "Geography", "Room 104", "Mr. B. Kundu")
                                TimetableRow("Period 5", "12:15 PM", "Moral Sci.", "Room 102", "Ms. P. Sen")
                            }
                            "Tue" -> {
                                TimetableRow("Period 1", "08:30 AM", "Chemistry", "Chemistry Lab", "Ms. A. Roy")
                                TimetableRow("Period 2", "09:15 AM", "Geometry", "Room 102", "Mr. S. K. Singh")
                                TimetableRow("Period 3", "10:15 AM", "Hindi Lang.", "Room 102", "Mr. S. Pathak")
                                TimetableRow("Period 4", "11:30 AM", "History", "Room 102", "Mr. B. Kundu")
                                TimetableRow("Period 5", "12:15 PM", "Sports Play", "Playground", "Mr. R. Prasad")
                            }
                            "Wed" -> {
                                TimetableRow("Period 1", "08:30 AM", "Biology", "Biology Lab", "Ms. N. Das")
                                TimetableRow("Period 2", "09:15 AM", "English Lang.", "Room 102", "Ms. A. Sharma")
                                TimetableRow("Period 3", "10:15 AM", "Civics", "Room 102", "Mr. B. Kundu")
                                TimetableRow("Period 4", "11:30 AM", "Computer Sci.", "Comp. Lab B", "Mr. A. Pal")
                                TimetableRow("Period 5", "12:15 PM", "Art & Craft", "Art Room", "Ms. S. Chanda")
                            }
                            "Thu" -> {
                                TimetableRow("Period 1", "08:30 AM", "English Lit.", "Room 102", "Ms. A. Sharma")
                                TimetableRow("Period 2", "09:15 AM", "Trigonometry", "Room 102", "Mr. S. K. Singh")
                                TimetableRow("Period 3", "10:15 AM", "Chemistry", "Room 102", "Ms. A. Roy")
                                TimetableRow("Period 4", "11:30 AM", "Geography", "Room 104", "Mr. B. Kundu")
                                TimetableRow("Period 5", "12:15 PM", "Library Hour", "Main Library", "Mrs. S. Paul")
                            }
                            "Fri" -> {
                                TimetableRow("Period 1", "08:30 AM", "Algebra", "Room 102", "Mr. S. K. Singh")
                                TimetableRow("Period 2", "09:15 AM", "Physics Test", "Room 102", "Dr. S. K. Roy")
                                TimetableRow("Period 3", "10:15 AM", "Hindi Lit.", "Room 102", "Mr. S. Pathak")
                                TimetableRow("Period 4", "11:30 AM", "EVS", "Room 102", "Ms. N. Das")
                                TimetableRow("Period 5", "12:15 PM", "Weekly Quiz", "Seminar Hall", "Principal")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TimetableRow(period: String, time: String, subject: String, room: String, teacher: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("$period • $time", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(subject, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(room, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(teacher, fontSize = 11.sp, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun StudentFitnessDialog(
    onDismiss: () -> Unit,
    sharedPrefs: android.content.SharedPreferences,
    studentId: String
) {
    var stepsCount by remember { mutableStateOf(sharedPrefs.getInt("fitness_steps_$studentId", 6420)) }
    var waterGlasses by remember { mutableStateOf(sharedPrefs.getInt("fitness_water_$studentId", 5)) }
    
    var heightStr by remember { mutableStateOf("165") }
    var weightStr by remember { mutableStateOf("58") }
    var calculatedBmi by remember { mutableStateOf(0f) }
    var bmiCategory by remember { mutableStateOf("") }
    var bmiColor by remember { mutableStateOf(Color.Gray) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF00BCD4), modifier = Modifier.size(24.dp))
                    Text("FitStudent Health Center", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DAILY STEP TRACKER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00838F))
                            Text("$stepsCount / 10,000 steps", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                            LinearProgressIndicator(
                                progress = { (stepsCount / 10000f).coerceIn(0f, 1f) },
                                color = Color(0xFF00BCD4),
                                trackColor = Color.LightGray.copy(alpha = 0.5f),
                                modifier = Modifier.width(130.dp).padding(top = 6.dp)
                            )
                        }
                        Button(
                            onClick = {
                                stepsCount += 500
                                sharedPrefs.edit().putInt("fitness_steps_$studentId", stepsCount).apply()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
                        ) {
                            Text("+500", fontSize = 11.sp)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("WATER LOG DRANK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                            Text("$waterGlasses / 8 Glasses today", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                        }
                        Button(
                            onClick = {
                                if (waterGlasses < 12) {
                                    waterGlasses += 1
                                    sharedPrefs.edit().putInt("fitness_water_$studentId", waterGlasses).apply()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                        ) {
                            Text("Drink Glass", fontSize = 11.sp)
                        }
                    }
                }

                Text("Campus BMI Calculator", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = heightStr,
                            onValueChange = { heightStr = it },
                            label = { Text("Height (cm)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = { weightStr = it },
                            label = { Text("Weight (kg)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val h = heightStr.toFloatOrNull() ?: 1f
                            val w = weightStr.toFloatOrNull() ?: 0f
                            if (h > 10) {
                                val bmi = w / ((h/100f) * (h/100f))
                                calculatedBmi = String.format(Locale.US, "%.1f", bmi).toFloat()
                                if (bmi < 18.5f) {
                                    bmiCategory = "Underweight"
                                    bmiColor = Color.Blue
                                } else if (bmi in 18.5f..24.9f) {
                                    bmiCategory = "Normal weight (Healthy)"
                                    bmiColor = Color(0xFF4CAF50)
                                } else if (bmi in 25f..29.9f) {
                                    bmiCategory = "Overweight"
                                    bmiColor = Color(0xFFFF9800)
                                } else {
                                    bmiCategory = "Obese"
                                    bmiColor = Color.Red
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                    ) {
                        Text("Calculate BMI", fontSize = 12.sp)
                    }

                    if (calculatedBmi > 0f) {
                        HorizontalDivider(color = Color.LightGray)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Text("Calculated BMI Value: $calculatedBmi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(bmiCategory, fontWeight = FontWeight.Black, fontSize = 13.sp, color = bmiColor)
                            Text("Healthy School range: 18.5 - 24.9", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close Settings")
            }
        }
    )
}

@Composable
fun StudentStoreDialog(onDismiss: () -> Unit) {
    var cartCount by remember { mutableStateOf(0) }
    var viewCartMode by remember { mutableStateOf(false) }
    
    val itemsList = remember {
        listOf(
            StoreItem("TOP_01", "Toppers Academy Blazer", 1200, "Official winter uniform standard wool blazer with school crest."),
            StoreItem("TOP_02", "Annual Syllabus Book Pack", 480, "Class 10 CBSE comprehensive notebooks, papers & study pack."),
            StoreItem("TOP_03", "Geometry and Lab Instruments Box", 190, "Camlin metal premium geometry kit with precision scale & math tools."),
            StoreItem("TOP_04", "School Waterproof Backpack", 650, "Heavy duty 3-compartment campus bag with bottle pockets and laptop sleeve.")
        )
    }
    
    val cartQuantities = remember { mutableStateMapOf<String, Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color(0xFF009688), modifier = Modifier.size(24.dp))
                    Text("Toppers Campus Store", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                IconButton(onClick = { viewCartMode = !viewCartMode }) {
                    BadgedBox(
                        badge = {
                            if (cartCount > 0) {
                                Badge { Text(cartCount.toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color(0xFF009688))
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!viewCartMode) {
                    Text("Available Student Supplies:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsList.forEach { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("₹${item.price}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF009688))
                                        }
                                        Text(item.description, fontSize = 10.sp, color = Color.DarkGray)
                                        
                                        Button(
                                            onClick = {
                                                cartQuantities[item.id] = (cartQuantities[item.id] ?: 0) + 1
                                                cartCount += 1
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
                                            modifier = Modifier.fillMaxWidth().height(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Add to Cart", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("My Selected Items Cart:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    var totalBill = 0
                    itemsList.forEach { item ->
                        val qty = cartQuantities[item.id] ?: 0
                        totalBill += (qty * item.price)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (cartCount == 0) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Shopping cart is empty.", color = Color.Gray, fontSize = 11.sp)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsList.forEach { item ->
                                    val qty = cartQuantities[item.id] ?: 0
                                    if (qty > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(item.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text("Price: ₹${item.price} • Qty: $qty", fontSize = 10.sp, color = Color.DarkGray)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = {
                                                    cartQuantities[item.id] = qty - 1
                                                    cartCount -= 1
                                                }) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Reduce", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                                Text(qty.toString(), fontSize = 12.sp)
                                                IconButton(onClick = {
                                                    cartQuantities[item.id] = qty + 1
                                                    cartCount += 1
                                                }) {
                                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Green, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Bill Summary:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("₹$totalBill", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF009688))
                    }

                    if (cartCount > 0) {
                        Button(
                            onClick = {
                                cartCount = 0
                                cartQuantities.clear()
                                viewCartMode = false
                                Toast.makeText(onDismiss().let { null }, "Order Success! Collect items from school reception inside 48 hrs.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                        ) {
                            Text("Place Reservation Order", fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { viewCartMode = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to Catalogue")
                    }
                }
            }
        },
        confirmButton = {
            if (!viewCartMode) {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

data class StoreItem(val id: String, val name: String, val price: Int, val description: String)

@Composable
fun StudentGalleryDialog(onDismiss: () -> Unit) {
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }
    
    val photos = listOf(
        GalleryPhoto("Sports Day 2026", "Gold medalist runner finishing race.", "https://images.unsplash.com/photo-1576402187878-974f70c890a5?w=500&auto=format&fit=crop"),
        GalleryPhoto("Science Exhibition", "Class 10 students demonstrating robotic arm.", "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=500&auto=format&fit=crop"),
        GalleryPhoto("Independence Day", "Flag hoisting ceremony and drill parade.", "https://images.unsplash.com/photo-1589308078059-be1415eab4c3?w=500&auto=format&fit=crop")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Collections, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(24.dp))
                    Text("Toppers Photo Gallery", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("School Event Moments:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        photos.forEach { photo ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))
                                                )
                                            )
                                            .clickable { selectedPhotoUrl = photo.title },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Collections, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("View Event Photo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(photo.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(photo.description, fontSize = 10.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    selectedPhotoUrl?.let { title ->
        AlertDialog(
            onDismissRequest = { selectedPhotoUrl = null },
            title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("High-res album image view simulation.", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPhotoUrl = null }) {
                    Text("Go Back")
                }
            }
        )
    }
}

data class GalleryPhoto(val title: String, val description: String, val url: String)

@Composable
fun StudentLeaveDialog(
    onDismiss: () -> Unit,
    viewModel: AttendanceViewModel,
    studentId: String
) {
    val activeStudent = viewModel.currentStudent.value
    val studentName = activeStudent?.name ?: "Student"
    var leaveType by remember { mutableStateOf("Sick Leave") }
    var startDate by remember { mutableStateOf("15-Jul-2026") }
    var endDate by remember { mutableStateOf("17-Jul-2026") }
    var reasonText by remember { mutableStateOf("") }
    
    val leaveRequests by viewModel.leaveRequests.collectAsState()
    val myLeaves = remember(leaveRequests, studentId) {
        leaveRequests.filter { it.studentId == studentId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = Color(0xFF78909C), modifier = Modifier.size(24.dp))
                    Text("Request Leave Absence", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Submit Application Form:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select Leave Category", fontSize = 10.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Sick Leave", "Casual", "Family").forEach { type ->
                            Surface(
                                onClick = { leaveType = type },
                                color = if (leaveType == type) Color(0xFFECEFF1) else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (leaveType == type) Color(0xFF78909C) else Color.LightGray),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(type, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (leaveType == type) Color.Black else Color.Gray)
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End Date", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("Reason for Leave Details") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )

                val context = LocalContext.current
                Button(
                    onClick = {
                        if (reasonText.trim().isNotEmpty()) {
                            viewModel.submitLeaveRequest(
                                studentId = studentId,
                                studentName = studentName,
                                type = leaveType,
                                dates = "$startDate to $endDate",
                                reason = reasonText
                            )
                            reasonText = ""
                            Toast.makeText(context, "Leave request registered successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please describe the reason for your leave.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78909C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Submit Application", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider()

                Text("Leave Submission Logs:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                if (myLeaves.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                        Text("No past leave requests found.", fontSize = 10.sp, color = Color.Gray)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        myLeaves.forEach { leave ->
                            val statusColor = when (leave.status) {
                                "Approved" -> Color(0xFF2E7D32)
                                "Rejected" -> Color(0xFFC62828)
                                else -> Color(0xFFFFB300)
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("${leave.type} Request", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Box(
                                            modifier = Modifier.background(statusColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(leave.status.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        }
                                    }
                                    Text("Dates: ${leave.dates}", fontSize = 10.sp, color = Color.Gray)
                                    Text("Reason: ${leave.reason}", fontSize = 10.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun StudentPhotoEditDialog(
    onDismiss: () -> Unit,
    onLaunchGallery: () -> Unit,
    onOpenPresetPicker: () -> Unit,
    onRemovePhoto: () -> Unit,
    currentPhoto: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text("Update Profile Photo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show current photo preview
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    StudentPhoto(
                        photoData = currentPhoto,
                        modifier = Modifier.fillMaxSize(),
                        placeholderIcon = Icons.Default.School,
                        tintColor = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option: Choose from Gallery
                Surface(
                    onClick = {
                        onLaunchGallery()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Choose from Gallery", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Select an image from your device storage", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // Option: Select Preset Avatar
                Surface(
                    onClick = {
                        onOpenPresetPicker()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.Face, contentDescription = "Presets", tint = MaterialTheme.colorScheme.secondary)
                        Column {
                            Text("Choose Avatar Preset", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Choose from our colorful styled presets", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // Option: Remove photo if current is not empty
                if (currentPhoto.isNotEmpty()) {
                    Surface(
                        onClick = {
                            onRemovePhoto()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            Column {
                                Text("Remove Photo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                                Text("Delete current picture and use default icon", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun StudentPresetPickerDialog(
    onDismiss: () -> Unit,
    onSelectPreset: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text("Select Avatar Preset", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select a beautifully styled avatar preset for your profile:",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                // Grid layout with 3 items per row
                val presets = avatarPresets
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val chunked = presets.chunked(3)
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { preset ->
                                Card(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .aspectRatio(1f)
                                        .clickable {
                                            onSelectPreset(preset.key)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(preset.startColor, preset.endColor)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = preset.icon,
                                                contentDescription = preset.label,
                                                tint = preset.tint,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = preset.label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = preset.tint,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                            // Add placeholders to keep spacing consistent if the last row is not complete
                            if (rowItems.size < 3) {
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1.5f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

fun downloadFileToPublicDownloads(context: android.content.Context, filePath: String, displayName: String): String? {
    val srcFile = java.io.File(filePath)
    if (!srcFile.exists()) {
        return null
    }
    
    // On Android Q and above, we can use MediaStore to save files directly to Downloads
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            val mimeType = if (filePath.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outStream ->
                    srcFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                return "Downloads/$displayName"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Fallback or older APIs: standard Environment.getExternalStoragePublicDirectory
    try {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val destFile = java.io.File(downloadsDir, displayName)
        srcFile.inputStream().use { inStream ->
            destFile.outputStream().use { outStream ->
                inStream.copyTo(outStream)
            }
        }
        return destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    // Fallback to app external files dir if everything else fails
    try {
        val externalFilesDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (externalFilesDir != null) {
            val destFile = java.io.File(externalFilesDir, displayName)
            srcFile.inputStream().use { inStream ->
                destFile.outputStream().use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            return destFile.absolutePath
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return null
}
