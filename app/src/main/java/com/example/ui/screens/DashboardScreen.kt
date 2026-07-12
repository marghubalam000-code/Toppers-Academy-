package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.data.model.AttendanceRecord
import com.example.ui.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AttendanceViewModel,
    onNavigateToAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val allAttendance by viewModel.allAttendanceRecords.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allChats by viewModel.allChatMessages.collectAsState()
    val unreadChatCount = remember(allChats) {
        allChats.count { it.sender == "Parent" && !it.isRead }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedStudentProfile by remember { mutableStateOf<Student?>(null) }

    // Calendar states for interactive summaries
    var attendanceCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var monthlySummaryCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val monthYearFormat = remember { SimpleDateFormat("MMM-yyyy", Locale.US) }

    var showBatchesDialog by remember { mutableStateOf(false) }
    var showStaffDialog by remember { mutableStateOf(false) }
    var showExamsDialog by remember { mutableStateOf(false) }
    var showAnnouncementsDialog by remember { mutableStateOf(false) }
    var showMessagesDialog by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showHomeworkDialog by remember { mutableStateOf(false) }
    var showClassworkDialog by remember { mutableStateOf(false) }
    var showLeavesDialog by remember { mutableStateOf(false) }
    var showFeeStructureDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == com.example.ui.theme.DarkBackground
    val cardBgColor = if (isDark) Color(0xFF1E2130) else Color.White
    val textPrimaryColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDark) Color.LightGray else Color(0xFF475569)

    // Computations
    val totalStudentsCount = students.size
    val activeStudents = students.filter { it.status == "Active" }
    
    // Attendance status for selectedDate
    val todayAttendance = allAttendance.filter { it.date == selectedDate }
    
    val presentToday = todayAttendance.count { it.status == "Present" }
    val absentToday = todayAttendance.count { it.status == "Absent" }
    val leaveToday = todayAttendance.count { it.status == "Leave" }
    val holidayToday = todayAttendance.count { it.status == "Holiday" }
    
    val totalMarked = todayAttendance.size
    val attendancePercentage = if (totalMarked > 0) {
        val presentOrLeaveOrHoliday = presentToday + leaveToday + holidayToday
        (presentOrLeaveOrHoliday * 100) / totalMarked
    } else {
        100
    }

    // Search matches
    val searchResults = remember(searchQuery, students) {
        if (searchQuery.isBlank()) emptyList()
        else students.filter { student ->
            student.name.contains(searchQuery, ignoreCase = true) ||
            student.rollNumber.contains(searchQuery) ||
            student.studentId.contains(searchQuery, ignoreCase = true) ||
            student.mobile.contains(searchQuery)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background), // Responsive background
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profile / Account Header Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        // Initials Avatar
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF104E5B) else MaterialTheme.colorScheme.primaryContainer), // Adaptive avatar background
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TA",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp
                            )
                        }
                        Column {
                            Text(
                                text = "Toppers Academy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = textPrimaryColor // Adaptive text color
                            )
                            Text(
                                text = "Administrator",
                                fontSize = 12.sp,
                                color = textSecondaryColor // Adaptive text color
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Reloading data from Supabase...", Toast.LENGTH_SHORT).show()
                                viewModel.fetchFromSupabase()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = textPrimaryColor, // Adaptive tint
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                showLogoutConfirmDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Log Out",
                                tint = textPrimaryColor, // Adaptive tint
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 2. Search Box for Admin convenience
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, roll, mobile, ID...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E2130),
                        unfocusedContainerColor = Color(0xFF1E2130),
                        focusedBorderColor = Color(0xFF0F4D4A),
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_search"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Search Results List overlay
            if (searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Matching Students (${searchResults.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
                itemsIndexed(searchResults) { index, student ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        onClick = { selectedStudentProfile = student }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${index + 1}. ${student.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimaryColor)
                                Text("ID: ${student.studentId} | Class: ${student.studentClass} | Roll: ${student.rollNumber}", fontSize = 11.sp, color = textSecondaryColor)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = "View Profile", tint = textSecondaryColor)
                        }
                    }
                }
            }

            // 3. Three Summary Cards (Students, Batches, Staff)
            item {
                val batchesList by viewModel.batchesList.collectAsState()
                val staffList by viewModel.staffList.collectAsState()

                val studentsBg = if (isDark) Color(0xFF00332B) else Color(0xFFE8F5E9)
                val studentsText = if (isDark) Color.White else Color(0xFF1B5E20)
                
                val batchesBg = if (isDark) Color(0xFF1B3D14) else Color(0xFFF1F8E9)
                val batchesText = if (isDark) Color.White else Color(0xFF33691E)
                
                val staffBg = if (isDark) Color(0xFF4C3600) else Color(0xFFFFF8E1)
                val staffText = if (isDark) Color.White else Color(0xFFE65100)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Students Card
                    SummaryCard(
                        count = "$totalStudentsCount",
                        label = "Students",
                        subtext = "Active",
                        icon = Icons.Default.Groups,
                        backgroundColor = studentsBg,
                        textColor = studentsText,
                        modifier = Modifier.weight(1f)
                    )

                    // Batches Card
                    SummaryCard(
                        count = "${batchesList.size}",
                        label = "Batches",
                        subtext = "Manage",
                        icon = Icons.Default.Hub,
                        backgroundColor = batchesBg,
                        textColor = batchesText,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showBatchesDialog = true }
                    )

                    // Staff Card
                    SummaryCard(
                        count = "${staffList.size}",
                        label = "Staff",
                        subtext = "Manage",
                        icon = Icons.Default.Badge,
                        backgroundColor = staffBg,
                        textColor = staffText,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStaffDialog = true }
                    )
                }
            }

            // 4. Attendance Summary Card with Line Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title and Month Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Attendance Summary",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF59E0B), // Golden yellow
                                fontSize = 15.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        attendanceCalendar = (attendanceCalendar.clone() as Calendar).apply {
                                            add(Calendar.MONTH, -1)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = monthYearFormat.format(attendanceCalendar.time),
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimaryColor,
                                    fontSize = 13.sp
                                )
                                IconButton(
                                    onClick = {
                                        attendanceCalendar = (attendanceCalendar.clone() as Calendar).apply {
                                            add(Calendar.MONTH, 1)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Curve Graph Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height - 20.dp.toPx()
                                
                                // Draw horizontal grid lines
                                for (i in 0..4) {
                                    val y = (height / 4) * i
                                    drawLine(
                                        color = Color.Gray.copy(alpha = 0.15f),
                                        start = Offset(0f, y),
                                        end = Offset(width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // 11 Points on X Axis
                                val pointsCount = 11
                                val xStep = width / (pointsCount - 1)
                                
                                // Draw vertical grid lines
                                for (i in 0 until pointsCount) {
                                    val x = i * xStep
                                    drawLine(
                                        color = Color.Gray.copy(alpha = 0.1f),
                                        start = Offset(x, 0f),
                                        end = Offset(x, height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Coords: Day 2 rises up to peak, then drops down to 0 for the rest
                                val coords = List(pointsCount) { index ->
                                    val x = index * xStep
                                    val y = when (index) {
                                        0 -> height * 0.95f
                                        1 -> height * 0.12f // peak
                                        2 -> height * 0.95f // drop
                                        else -> height * 0.95f // flat
                                    }
                                    Offset(x, y)
                                }

                                val path = androidx.compose.ui.graphics.Path()
                                val fillPath = androidx.compose.ui.graphics.Path()

                                path.moveTo(coords[0].x, coords[0].y)
                                fillPath.moveTo(coords[0].x, height)
                                fillPath.lineTo(coords[0].x, coords[0].y)

                                for (i in 1 until pointsCount) {
                                    val prev = coords[i - 1]
                                    val curr = coords[i]
                                    val cp1 = Offset(prev.x + (curr.x - prev.x) / 2f, prev.y)
                                    val cp2 = Offset(prev.x + (curr.x - prev.x) / 2f, curr.y)
                                    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, curr.x, curr.y)
                                    fillPath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, curr.x, curr.y)
                                }

                                fillPath.lineTo(width, height)
                                fillPath.close()

                                // Translucent Blue underfill
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.25f), Color.Transparent)
                                    )
                                )

                                // Blue Stroke Line
                                drawPath(
                                    path = path,
                                    color = Color(0xFF3B82F6),
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Point peak dot
                                drawCircle(
                                    color = Color(0xFF3B82F6),
                                    radius = 4.dp.toPx(),
                                    center = coords[1]
                                )
                            }

                            // Day Labels 1 to 11
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (d in 1..11) {
                                    Text("$d", color = Color.Gray, fontSize = 9.sp)
                                }
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.15f))

                        // Custom Horizontal Legend Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendItem(label = "Present", color = Color(0xFF10B981))
                            LegendItem(label = "Absent", color = Color(0xFFEF4444))
                            LegendItem(label = "Leave", color = Color(0xFFF59E0B))
                            LegendItem(label = "Holiday", color = Color(0xFF3B82F6))
                        }
                    }
                }
            }

            // 5. Student & Staff Marking Attendance Progress Row
            item {
                val staffList by viewModel.staffList.collectAsState()
                
                val totalStudents = students.size
                val markedStudents = todayAttendance.size
                val studentMarkedRatio = "$markedStudents/$totalStudents"
                val studentMarkedPercent = if (totalStudents > 0) "${(markedStudents * 100) / totalStudents}%" else "0%"
                val studentMarkedProgress = if (totalStudents > 0) markedStudents.toFloat() / totalStudents else 0.0f

                val totalStaff = staffList.size
                val staffMarkedRatio = "$totalStaff/$totalStaff"
                val staffMarkedPercent = if (totalStaff > 0) "100%" else "0%"
                val staffMarkedProgress = if (totalStaff > 0) 1.0f else 0.0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MarkingAttendanceCard(
                        title = "Student",
                        percentage = studentMarkedPercent,
                        ratio = studentMarkedRatio,
                        progress = studentMarkedProgress,
                        icon = Icons.Default.Groups,
                        progressColor = Color(0xFFF59E0B), // Golden gold
                        modifier = Modifier.weight(1f)
                    )

                    MarkingAttendanceCard(
                        title = "Staff",
                        percentage = staffMarkedPercent,
                        ratio = staffMarkedRatio,
                        progress = staffMarkedProgress,
                        icon = Icons.Default.Person,
                        progressColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 6. Due Fees Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2130))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "Due Fees",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                              )
                            Text(
                                text = "Due Fees",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "(0) 0", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                                Text(text = "Active", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "(0) 0", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                Text(text = "Close", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 7. Monthly Summary Section with selector, vertical bar graph and details table
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2130))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Summary",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF59E0B),
                                fontSize = 15.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        monthlySummaryCalendar = (monthlySummaryCalendar.clone() as Calendar).apply {
                                            add(Calendar.MONTH, -1)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = monthYearFormat.format(monthlySummaryCalendar.time),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                IconButton(
                                    onClick = {
                                        monthlySummaryCalendar = (monthlySummaryCalendar.clone() as Calendar).apply {
                                            add(Calendar.MONTH, 1)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Bar Graph Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height - 16.dp.toPx()
                                val barWidth = 8.dp.toPx()
                                val cols = 12
                                val xStep = width / (cols + 1)

                                // Horizontal background grid lines
                                for (i in 0..4) {
                                    val y = (height / 4) * i
                                    drawLine(
                                        color = Color.Gray.copy(alpha = 0.1f),
                                        start = Offset(0f, y),
                                        end = Offset(width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // 12 columns representing 12 months
                                for (i in 1..cols) {
                                    val x = i * xStep
                                    val hInc = if (i == 7) height * 0.4f else 0f // Month 7 (Jul) mock bar
                                    val hExp = if (i == 7) height * 0.2f else 0f

                                    if (hInc > 0) {
                                        drawRect(
                                            color = Color(0xFF10B981), // Green (Incomes)
                                            topLeft = Offset(x - barWidth, height - hInc),
                                            size = androidx.compose.ui.geometry.Size(barWidth, hInc)
                                        )
                                    }
                                    if (hExp > 0) {
                                        drawRect(
                                            color = Color(0xFFEF4444), // Expense (Red/Pink)
                                            topLeft = Offset(x, height - hExp),
                                            size = androidx.compose.ui.geometry.Size(barWidth, hExp)
                                        )
                                    }
                                }
                            }

                            // Horizontal labels 1 to 12
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (m in 1..12) {
                                    Text("$m", color = Color.Gray, fontSize = 9.sp)
                                }
                            }
                        }

                        // Legend Row (Incomes, Expenses)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendItem(label = "Incomes", color = Color(0xFF10B981))
                            LegendItem(label = "Expenses", color = Color(0xFFEF4444))
                        }

                        // Breakdown Table Setup (Incomes vs Expenses vs Balance)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Table Header Row
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(text = "", modifier = Modifier.weight(1.2f))
                                Text(text = "Today", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text(text = "Monthly", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text(text = "Total", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.15f))

                            // Incomes Row
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Incomes", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                                Text(text = "0", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text(text = "0", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text(text = "0", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            }

                            // Expenses Row
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Expenses", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                                Text(text = "0", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text(text = "0", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text(text = "0", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            }

                            // Balance / Net profit Row
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Net Profit", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                                Text(text = "0", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text(text = "0", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text(text = "0", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 8. FEATURES SECTION Grid
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FEATURES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Divider(modifier = Modifier.weight(1f), color = Color.Gray.copy(alpha = 0.15f))
                    }

                    // Row 1 of Features (Exams, Birthdays, Home works, Class works)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FeatureButton(
                            icon = Icons.Default.Timer,
                            label = "Exams",
                            backgroundColor = Color(0xFF5C2B15),
                            onClick = { showExamsDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        FeatureButton(
                            icon = Icons.Default.Cake,
                            label = "Birthdays",
                            backgroundColor = Color(0xFF6E5606),
                            onClick = { Toast.makeText(context, "Birthdays List", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f)
                        )
                        FeatureButton(
                            icon = Icons.Default.ListAlt,
                            label = "Home works",
                            backgroundColor = Color(0xFF0C3C44),
                            onClick = { showHomeworkDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        FeatureButton(
                            icon = Icons.Default.MenuBook,
                            label = "Class works",
                            backgroundColor = Color(0xFF144D1E),
                            onClick = { showClassworkDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2 of Features (Staff Logs, Announcements, Messages)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeatureButton(
                            icon = Icons.Default.HistoryEdu,
                            label = "Staff Logs",
                            backgroundColor = Color(0xFF1B424C),
                            onClick = { Toast.makeText(context, "Staff Activity Logs", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f)
                        )
                        FeatureButton(
                            icon = Icons.Default.Notifications,
                            label = "Announcements",
                            backgroundColor = Color(0xFF502500),
                            onClick = { showAnnouncementsDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        FeatureButton(
                            icon = Icons.Default.Chat,
                            label = "Messages",
                            backgroundColor = Color(0xFF123C32),
                            onClick = { showMessagesDialog = true },
                            modifier = Modifier.weight(1f),
                            badgeCount = unreadChatCount
                        )
                        FeatureButton(
                            icon = Icons.Default.Remove,
                            label = "Leaves",
                            backgroundColor = Color(0xFF3F4E5A),
                            onClick = { showLeavesDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3 of Features (Fees Structure)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeatureButton(
                            icon = Icons.Default.Payments,
                            label = "Fees Structure",
                            backgroundColor = Color(0xFF2E7D32),
                            onClick = { showFeeStructureDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1.5f))
                        Spacer(modifier = Modifier.weight(1.5f))
                    }
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

        // 9. Yellow Floating Action Button "+ Management"
        FloatingActionButton(
            onClick = {
                onNavigateToAttendance()
                Toast.makeText(context, "Navigating to Student Attendance Sheet...", Toast.LENGTH_SHORT).show()
            },
            containerColor = Color(0xFFF59E0B),
            contentColor = Color.Black,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("dashboard_management_fab")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Management",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Management",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }

    // View Profile Dialog
    if (selectedStudentProfile != null) {
        StudentProfileDialog(
            student = selectedStudentProfile!!,
            allAttendance = allAttendance,
            viewModel = viewModel,
            onDismiss = { selectedStudentProfile = null }
        )
    }

    if (showBatchesDialog) {
        BatchesManagementDialog(
            viewModel = viewModel,
            onDismiss = { showBatchesDialog = false }
        )
    }

    if (showStaffDialog) {
        StaffManagementDialog(
            viewModel = viewModel,
            onDismiss = { showStaffDialog = false }
        )
    }

    if (showExamsDialog) {
        ScheduleExamDialog(
            viewModel = viewModel,
            onDismiss = { showExamsDialog = false }
        )
    }

    if (showAnnouncementsDialog) {
        AnnouncementsDialog(
            viewModel = viewModel,
            onDismiss = { showAnnouncementsDialog = false }
        )
    }

    if (showMessagesDialog) {
        CommunityMessagesDialog(
            viewModel = viewModel,
            onDismiss = { showMessagesDialog = false }
        )
    }

    if (showDeveloperDialog) {
        MarghubProfileDialog(
            onDismiss = { showDeveloperDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemeSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showHomeworkDialog) {
        HomeworkManagementDialog(
            viewModel = viewModel,
            onDismiss = { showHomeworkDialog = false }
        )
    }

    if (showClassworkDialog) {
        ClassworkManagementDialog(
            viewModel = viewModel,
            onDismiss = { showClassworkDialog = false }
        )
    }

    if (showLeavesDialog) {
        StudentLeaveRequestsDialog(
            viewModel = viewModel,
            onDismiss = { showLeavesDialog = false }
        )
    }

    if (showFeeStructureDialog) {
        FeeStructureManagementDialog(
            viewModel = viewModel,
            onDismiss = { showFeeStructureDialog = false }
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
                    text = "Are you sure you want to log out from the Admin portal?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchesManagementDialog(
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val batchesList by viewModel.batchesList.collectAsState()
    var newBatchName by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFFF59E0B))
                Text("Manage Academy Batches", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Active School Batches (${batchesList.size})", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                
                // Scrollable List of Batches
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    if (batchesList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No batches registered", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(batchesList) { batch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text(batch, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (batch in listOf("Class 10", "Class 11", "Class 12")) {
                                                Toast.makeText(context, "Default classes cannot be deleted", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.deleteBatch(batch)
                                                Toast.makeText(context, "$batch deleted", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Input Form
                Text("Create New Batch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = newBatchName,
                    onValueChange = { newBatchName = it },
                    placeholder = { Text("e.g. Class 9, Batch B") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        val name = newBatchName.trim()
                        if (name.isNotEmpty()) {
                            viewModel.addBatch(name)
                            newBatchName = ""
                            Toast.makeText(context, "Batch '$name' added successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Batch name cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Batch", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementDialog(
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val staffList by viewModel.staffList.collectAsState()
    val teachersList by viewModel.allTeachers.collectAsState()
    val batchesList by viewModel.batchesList.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0: Support Staff, 1: Class Teachers

    // State for Staff
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }

    // State for Teacher
    var teacherId by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var teacherClass by remember { mutableStateOf("") }
    var teacherSection by remember { mutableStateOf("A") }
    var teacherMobile by remember { mutableStateOf("") }
    var teacherEmail by remember { mutableStateOf("") }
    var teacherPassword by remember { mutableStateOf("") }
    var teacherSubject by remember { mutableStateOf("Mathematics") }
    var customSubject by remember { mutableStateOf("") }

    // Dropdown States
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    // Photo picker state
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        pickedPhotoUri = uri
    }

    // List of standard subjects
    val subjectsList = listOf("Mathematics", "Science", "English", "History", "Physics", "Chemistry", "Biology", "Computer Science", "Other")

    // List of batches
    val classesToChoose = remember(batchesList) {
        if (batchesList.isNotEmpty()) batchesList else listOf("Class 10", "Class 9", "Class 8", "Class 7")
    }

    // Pre-populate automatic ID & password & default class on open
    LaunchedEffect(Unit) {
        if (teacherId.isEmpty()) {
            teacherId = "T-${(101..999).random()}"
        }
        if (teacherPassword.isEmpty()) {
            teacherPassword = (100000..999999).random().toString()
        }
    }

    LaunchedEffect(classesToChoose) {
        if (teacherClass.isEmpty() || (teacherClass == "Class 10" && !classesToChoose.contains("Class 10"))) {
            teacherClass = classesToChoose.firstOrNull() ?: "Class 10"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFFF59E0B))
                Text("Staff & Teacher Directory", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab Selection
                TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    ) {
                        Text(
                            text = "Staff (${staffList.size})",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    ) {
                        Text(
                            text = "Teachers (${teachersList.size})",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (selectedTab == 0) {
                    // Support Staff List & Registration
                    Text("Active Staff Members", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        if (staffList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No staff registered", fontSize = 12.sp, color = Color.Gray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(staffList) { staff ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(staff.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text(staff.role, fontSize = 11.sp, color = Color.Gray)
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteStaff(staff.name, staff.role)
                                                Toast.makeText(context, "${staff.name} removed", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Text("Register New Staff", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Staff Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        placeholder = { Text("Role (e.g. Clerk, Guard, Peon)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            val n = name.trim()
                            val r = role.trim()
                            if (n.isNotEmpty() && r.isNotEmpty()) {
                                viewModel.addStaff(n, r)
                                name = ""
                                role = ""
                                Toast.makeText(context, "Staff '$n' registered", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Register Staff", fontWeight = FontWeight.Bold)
                    }

                } else {
                    // Class Teachers List & Registration
                    Text("Active Classroom Teachers", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        if (teachersList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No class teachers registered", fontSize = 12.sp, color = Color.Gray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(teachersList) { teacher ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Circular Photo or Placeholder
                                        val photoUrl = if (teacher.photoPath.isNotEmpty()) Uri.parse(teacher.photoPath) else null
                                        AsyncImage(
                                            model = photoUrl ?: "https://images.unsplash.com/photo-1544717305-2782549b5136?w=150&auto=format&fit=crop",
                                            contentDescription = "Teacher Photo",
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, Color(0xFFF59E0B), CircleShape),
                                            contentScale = ContentScale.Crop
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(teacher.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("ID: ${teacher.teacherId} | Class: ${teacher.assignedClass}-${teacher.assignedSection} | Subject: ${teacher.subject.ifBlank { "All" }}", fontSize = 11.sp, color = Color.Gray)
                                            Text("Email: ${teacher.email} | PIN: ${teacher.password}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteTeacher(teacher)
                                                Toast.makeText(context, "Removed teacher: ${teacher.name}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Text("Register Class Teacher", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Teacher Photo Picker Profile Area
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(2.dp, Color(0xFFF59E0B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (pickedPhotoUri != null) {
                                    AsyncImage(
                                        model = pickedPhotoUri,
                                        contentDescription = "Selected Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "No Photo",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Teacher Photo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Upload profile picture", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { photoLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                        contentColor = Color(0xFFB45309)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Choose Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 2. Auto-Generated Credentials Preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = teacherId,
                                onValueChange = { teacherId = it },
                                label = { Text("Teacher ID (Auto)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = {
                                    IconButton(onClick = { teacherId = "T-${(101..999).random()}" }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate ID", modifier = Modifier.size(16.dp))
                                    }
                                }
                            )

                            OutlinedTextField(
                                value = teacherPassword,
                                onValueChange = { teacherPassword = it },
                                label = { Text("PIN Password (Auto)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = {
                                    IconButton(onClick = { teacherPassword = (100000..999999).random().toString() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate PIN", modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }

                        // 3. Teacher Personal Info
                        OutlinedTextField(
                            value = teacherName,
                            onValueChange = { teacherName = it },
                            placeholder = { Text("Teacher Full Name") },
                            label = { Text("Teacher Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // 4. Class Selector (Choose Class Dropdown Option)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = teacherClass,
                                onValueChange = {},
                                label = { Text("Choose Class") },
                                placeholder = { Text("Select Class") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Class") }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { classDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = classDropdownExpanded,
                                onDismissRequest = { classDropdownExpanded = false }
                            ) {
                                classesToChoose.forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text(cls) },
                                        onClick = {
                                            teacherClass = cls
                                            classDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 5. Section Selector
                        OutlinedTextField(
                            value = teacherSection,
                            onValueChange = { teacherSection = it },
                            placeholder = { Text("e.g. A, B, C") },
                            label = { Text("Assigned Section") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // 6. Subject Selector (Choose Subject Dropdown Option)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = teacherSubject,
                                onValueChange = {},
                                label = { Text("Choose Subject") },
                                placeholder = { Text("Select Subject") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Subject") }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { subjectDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = subjectDropdownExpanded,
                                onDismissRequest = { subjectDropdownExpanded = false }
                            ) {
                                subjectsList.forEach { subj ->
                                    DropdownMenuItem(
                                        text = { Text(subj) },
                                        onClick = {
                                            teacherSubject = subj
                                            subjectDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (teacherSubject == "Other") {
                            OutlinedTextField(
                                value = customSubject,
                                onValueChange = { customSubject = it },
                                placeholder = { Text("Enter Custom Subject Name") },
                                label = { Text("Custom Subject") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // 7. Contact Details
                        OutlinedTextField(
                            value = teacherMobile,
                            onValueChange = { teacherMobile = it },
                            placeholder = { Text("Mobile Number (Optional)") },
                            label = { Text("Mobile Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = teacherEmail,
                            onValueChange = { teacherEmail = it },
                            placeholder = { Text("Email (For PIN Credentials Delivery)") },
                            label = { Text("Teacher Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = {
                                val tid = teacherId.trim()
                                val tname = teacherName.trim()
                                val tcls = teacherClass.trim()
                                val tsec = teacherSection.trim()
                                val tmob = teacherMobile.trim()
                                val temail = teacherEmail.trim()
                                val tpass = teacherPassword.trim()
                                val tsubj = if (teacherSubject == "Other") customSubject.trim() else teacherSubject
                                val tphoto = pickedPhotoUri?.toString() ?: ""

                                if (tid.isNotEmpty() && tname.isNotEmpty() && tcls.isNotEmpty() && tsec.isNotEmpty() && temail.isNotEmpty() && tpass.isNotEmpty()) {
                                    viewModel.addTeacher(tid, tname, tcls, tsec, tmob, temail, tpass, tsubj, tphoto)

                                    // Launch email client intent prefilled
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(temail))
                                        putExtra(Intent.EXTRA_SUBJECT, "Toppers Academy - Teacher Portal Credentials")
                                        val body = """
                                            Dear $tname,

                                            Welcome to Toppers Academy! Your digital teacher portal account has been created.
                                            Please find your portal access credentials below:

                                            Teacher ID: $tid
                                            Security PIN/Password: $tpass
                                            Assigned Class: $tcls - $tsec
                                            Assigned Subject: $tsubj

                                            Please log in using the Teacher Portal in our application.

                                            Best Regards,
                                            School Administration
                                            Toppers Academy
                                        """.trimIndent()
                                        putExtra(Intent.EXTRA_TEXT, body)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(intent, "Send Portal PIN to Teacher"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No email app found. Credentials copied to clipboard!", Toast.LENGTH_LONG).show()
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Teacher Credentials", "ID: $tid, PIN: $tpass")
                                        clipboard.setPrimaryClip(clip)
                                    }

                                    // Generate new credentials for next input
                                    teacherId = "T-${(101..999).random()}"
                                    teacherPassword = (100000..999999).random().toString()
                                    teacherName = ""
                                    teacherMobile = ""
                                    teacherEmail = ""
                                    pickedPhotoUri = null
                                    customSubject = ""

                                    Toast.makeText(context, "Teacher registered successfully! Launching email composer...", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "All fields (except mobile) are required", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Register & Send Email", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleExamDialog(
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val examsList by viewModel.examsList.collectAsState()
    val batchesList by viewModel.batchesList.collectAsState()
    
    var subject by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(batchesList.firstOrNull() ?: "Class 10") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var maxMarks by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFF59E0B))
                Text("Exam Scheduler", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Active Exam Schedules (${examsList.size})", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                
                // Scrollable List of Scheduled Exams
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    if (examsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No exams scheduled", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(examsList) { exam ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${exam.subject} (${exam.studentClass})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Date: ${exam.date} | Time: ${exam.time}", fontSize = 11.sp, color = Color.Gray)
                                        Text("Max Marks: ${exam.maxMarks}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteExam(exam)
                                            Toast.makeText(context, "${exam.subject} exam removed", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Input Form
                Text("Schedule New Exam", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = { Text("Subject (e.g. Mathematics)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // DatePicker dialog setup
                val calendar = Calendar.getInstance()
                val datePickerDialog = android.app.DatePickerDialog(
                    context,
                    { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                        val formattedMonth = String.format("%02d", selectedMonth + 1)
                        val formattedDay = String.format("%02d", selectedDayOfMonth)
                        date = "$selectedYear-$formattedMonth-$formattedDay"
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                // Dynamic Class Dropdown Selector
                var classDropdownExpanded by remember { mutableStateOf(false) }
                val classesToChoose = remember(batchesList) {
                    if (batchesList.isNotEmpty()) batchesList else listOf("Class 10", "Class 9", "Class 8", "Class 7")
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedClass,
                        onValueChange = {},
                        label = { Text("Class / Batch") },
                        placeholder = { Text("Select Class") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Class") }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { classDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false }
                    ) {
                        classesToChoose.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls) },
                                onClick = {
                                    selectedClass = cls
                                    classDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Picker field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        label = { Text("Exam Date") },
                        placeholder = { Text("Select Date") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Select Date") }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    placeholder = { Text("Time (e.g. 10:00 AM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = maxMarks,
                    onValueChange = { maxMarks = it },
                    placeholder = { Text("Max Marks (e.g. 100)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        val s = subject.trim()
                        val c = selectedClass.trim()
                        val d = date.trim()
                        val t = time.trim()
                        val m = maxMarks.trim()
                        if (s.isNotEmpty() && c.isNotEmpty() && d.isNotEmpty() && t.isNotEmpty() && m.isNotEmpty()) {
                            viewModel.scheduleExam(s, c, d, t, m)
                            subject = ""
                            date = ""
                            time = ""
                            maxMarks = ""
                            Toast.makeText(context, "Exam scheduled & student alerts dispatched", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Schedule & Notify", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsDialog(
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val announcementsList by viewModel.announcementsList.collectAsState()
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetClass by remember { mutableStateOf("All Batches") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFF59E0B))
                Text("Academy Announcements", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Recent Announcements (${announcementsList.size})", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                
                // Scrollable List of Announcements
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    if (announcementsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No announcements sent", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(announcementsList) { ann ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ann.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(ann.message, fontSize = 11.sp, color = Color.Gray)
                                        Text("Target: ${ann.targetClass}", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteAnnouncement(ann)
                                            Toast.makeText(context, "Announcement deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Input Form
                Text("Create New Announcement", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Announcement Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Message details...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Dynamic Class Dropdown Selector
                val batchesList by viewModel.batchesList.collectAsState()
                var classDropdownExpanded by remember { mutableStateOf(false) }
                val classesToChoose = remember(batchesList) {
                    listOf("ALL") + (if (batchesList.isNotEmpty()) batchesList else listOf("Class 10", "Class 9", "Class 8", "Class 7"))
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = targetClass,
                        onValueChange = {},
                        label = { Text("Target Class") },
                        placeholder = { Text("Select Target Class") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Class") }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { classDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false }
                    ) {
                        classesToChoose.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls) },
                                onClick = {
                                    targetClass = cls
                                    classDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val t = title.trim()
                        val m = message.trim()
                        val tc = targetClass.trim()
                        if (t.isNotEmpty() && m.isNotEmpty() && tc.isNotEmpty()) {
                            viewModel.sendAnnouncement(t, m, tc)
                            title = ""
                            message = ""
                            Toast.makeText(context, "Announcement dispatched successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dispatch Alert", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityMessagesDialog(
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val communityMessages by viewModel.communityMessages.collectAsState()
    val allChats by viewModel.allChatMessages.collectAsState()
    val students by viewModel.students.collectAsState()
    
    var activeTab by remember { mutableStateOf(0) } // 0 = Community, 1 = Parent Chats
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var chatInputText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val unreadChatCount = remember(allChats) {
        allChats.count { it.sender == "Parent" && !it.isRead }
    }

    LaunchedEffect(selectedStudent, allChats) {
        selectedStudent?.let {
            viewModel.markMessagesAsRead(it.studentId, "Parent")
        }
    }

    // Automatically scroll to the end when a new message is appended
    LaunchedEffect(communityMessages.size) {
        if (communityMessages.isNotEmpty()) {
            listState.animateScrollToItem(communityMessages.size - 1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFFF59E0B))
                    Text(if (activeTab == 0) "Community Space" else if (selectedStudent == null) "Parent Secure Threads" else "Chat: ${selectedStudent?.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (activeTab == 1 && selectedStudent != null) {
                    TextButton(onClick = { selectedStudent = null }) {
                        Text("Back", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabRow(
                    selectedTabIndex = activeTab,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Community", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Parent Secure", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (unreadChatCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("$unreadChatCount", color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    )
                }

                if (activeTab == 0) {
                    Text("Toppers Academy Community Chat", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    
                    // Message List
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .height(240.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(communityMessages) { msg ->
                                val isMe = msg.sender == "Admin" || msg.sender == "Principal"
                                val bubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                val align = if (isMe) Alignment.End else Alignment.Start
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isMe) {
                                        IconButton(
                                            onClick = { viewModel.deleteCommunityMessage(msg) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Message",
                                                tint = Color.Red.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    Column(
                                        horizontalAlignment = align
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(msg.sender, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("(${msg.role})", fontSize = 8.sp, color = Color.Gray)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bubbleColor)
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(msg.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }

                                    if (!isMe) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteCommunityMessage(msg) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Message",
                                                tint = Color.Red.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            placeholder = { Text("Type community message...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        IconButton(
                            onClick = {
                                val text = message.trim()
                                if (text.isNotEmpty()) {
                                    viewModel.sendCommunityMessage("Admin", "Staff", text)
                                    message = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    // Parent Secure Chats Tab
                    if (selectedStudent == null) {
                        // Show all active students and their unread counts
                        val activeStudents = students.filter { it.status == "Active" }
                        if (activeStudents.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                                Text("No students available.", fontSize = 12.sp, color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(activeStudents) { student ->
                                    val threadChats = allChats.filter { it.studentId == student.studentId }
                                    val lastMessage = threadChats.lastOrNull()?.message ?: "No messages yet."
                                    val threadUnread = threadChats.count { it.sender == "Parent" && !it.isRead }
                                    
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedStudent = student },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(student.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Class ${student.studentClass} ${student.section}", fontSize = 10.sp, color = Color.Gray)
                                                Text(lastMessage, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                            }
                                            if (threadUnread > 0) {
                                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                    Text("$threadUnread New", fontSize = 8.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Active Chat Window
                        val student = selectedStudent!!
                        val threadChats = remember(allChats, student.studentId) {
                            allChats.filter { it.studentId == student.studentId }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(6.dp)
                        ) {
                            if (threadChats.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No messages in this secure channel.", fontSize = 11.sp, color = Color.Gray)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(threadChats) { chat ->
                                        val isMe = chat.sender == "Teacher" || chat.sender == "Admin"
                                        val align = if (isMe) Alignment.End else Alignment.Start
                                        val bubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isMe) {
                                                IconButton(
                                                    onClick = { viewModel.deleteChatMessage(chat) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Message",
                                                        tint = Color.Red.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }

                                            Column(
                                                horizontalAlignment = align
                                            ) {
                                                Text(if (isMe) chat.senderName else "Parent", fontSize = 9.sp, color = Color.Gray)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(bubbleColor)
                                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                                ) {
                                                    Text(chat.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }

                                            if (!isMe) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { viewModel.deleteChatMessage(chat) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Message",
                                                        tint = Color.Red.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Send secure message input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = chatInputText,
                                onValueChange = { chatInputText = it },
                                placeholder = { Text("Type secure parent message...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (chatInputText.trim().isNotEmpty()) {
                                        viewModel.sendChatMessage(
                                            studentId = student.studentId,
                                            studentName = student.name,
                                            sender = "Admin",
                                            senderName = "Admin/Principal",
                                            msgText = chatInputText.trim()
                                        )
                                        chatInputText = ""
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SummaryCard(
    count: String,
    label: String,
    subtext: String,
    icon: ImageVector,
    backgroundColor: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
            
            Text(
                text = count,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                lineHeight = 28.sp
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Text(
                    text = subtext,
                    fontSize = 9.sp,
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MarkingAttendanceCard(
    title: String,
    percentage: String,
    ratio: String,
    progress: Float,
    icon: ImageVector,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == com.example.ui.theme.DarkBackground
    val cardBg = if (isDark) Color(0xFF1E2130) else Color.White
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color.Gray else Color(0xFF475569)
    val ratioColor = if (isDark) Color.LightGray else Color(0xFF64748B)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = progressColor,
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = titleColor)
                    Text(text = "Marking Attendance", fontSize = 10.sp, color = subtitleColor)
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = percentage, fontSize = 15.sp, fontWeight = FontWeight.Black, color = progressColor)
                Text(text = ratio, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ratioColor)
            }
            
            LinearProgressIndicator(
                progress = progress,
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "FeatureButtonScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (badgeCount > 0) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(badgeCount.toString(), fontSize = 9.sp)
                        }
                    }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = Color.White,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
    }
}

@Composable
fun StudentProfileDialog(
    student: Student,
    allAttendance: List<AttendanceRecord>,
    viewModel: com.example.ui.viewmodel.AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val studentAttendance = allAttendance.filter { it.studentId == student.studentId }
    val presentCount = studentAttendance.count { it.status == "Present" }
    val absentCount = studentAttendance.count { it.status == "Absent" }
    val leaveCount = studentAttendance.count { it.status == "Leave" }
    val holidayCount = studentAttendance.count { it.status == "Holiday" }
    
    val totalDays = studentAttendance.size
    val rate = if (totalDays > 0) {
        ((presentCount + leaveCount + holidayCount) * 100) / totalDays
    } else {
        100
    }

    var studentFees by remember(student.studentId) { mutableStateOf(viewModel.getStudentFees(student.studentId)) }
    var showFeeManager by remember { mutableStateOf(false) }
    var showIDCard by remember { mutableStateOf(false) }
    var showShareAttendanceDialog by remember { mutableStateOf(false) }
    val appLogoUri by viewModel.appLogoUri.collectAsState()
    val principalSignatureUri by viewModel.principalSignatureUri.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Student Profile View", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        StudentPhoto(
                            photoData = student.photo,
                            modifier = Modifier.fillMaxSize(),
                            placeholderIcon = Icons.Default.School,
                            tintColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(student.name, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Student ID: ${student.studentId}", fontSize = 12.sp, color = Color.Gray)
                        Text("Roll Number: ${student.rollNumber}", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Class: ${student.studentClass} - ${student.section}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Attendance: $rate%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }

                // Dynamic Fee Summary Card
                val total = studentFees.sumOf { it.amount }
                val paid = studentFees.filter { it.isPaid }.sumOf { it.amount }
                val due = studentFees.filter { !it.isPaid }.sumOf { it.amount }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Student Fees Overview", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = if (due == 0) "CLEARED" else "DUE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (due == 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 2.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Fees:", fontSize = 11.sp, color = Color.Gray)
                            Text("₹${String.format("%,d", total)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Paid:", fontSize = 11.sp, color = Color(0xFF2E7D32))
                            Text("₹${String.format("%,d", paid)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pending Due:", fontSize = 11.sp, color = Color(0xFFC62828))
                            Text("₹${String.format("%,d", due)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Button(
                            onClick = { showFeeManager = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manage Custom Fees / Send Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = { showShareAttendanceDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Date-wise Attendance / उपस्थिति साझा करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProfileField(label = "Father's Name", value = student.fatherName)
                    ProfileField(label = "Mother's Name", value = student.motherName)
                    ProfileField(label = "Mobile Phone", value = student.mobile)
                    ProfileField(label = "Parent Email", value = student.email.ifEmpty { "Not listed" })
                    ProfileField(label = "Date of Birth", value = student.dob)
                    ProfileField(label = "Aadhaar Card", value = student.aadhaar.ifEmpty { "Not provided" })
                    ProfileField(label = "Admission Date", value = student.admissionDate)
                    ProfileField(label = "Home Address", value = student.address)
                    ProfileField(label = "Status", value = student.status)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showIDCard = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "Badge Icon",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View ID Card", fontSize = 12.sp)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close Profile", fontSize = 12.sp)
                }
            }
        }
    )

    if (showIDCard) {
        StudentIDCardDialog(
            student = student,
            appLogoUri = appLogoUri,
            principalSignatureUri = principalSignatureUri,
            onDismiss = { showIDCard = false }
        )
    }

    if (showFeeManager) {
        StudentFeeManagerDialog(
            student = student,
            viewModel = viewModel,
            onDismiss = { showFeeManager = false },
            onFeesChanged = {
                studentFees = viewModel.getStudentFees(student.studentId)
            }
        )
    }

    if (showShareAttendanceDialog) {
        val context = LocalContext.current
        ShareCustomAttendanceDialog(
            student = student,
            allAttendance = allAttendance,
            onDismiss = { showShareAttendanceDialog = false },
            shareExportIntent = { content, formatName ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Toppers Academy $formatName Export")
                    putExtra(Intent.EXTRA_TEXT, content)
                }
                context.startActivity(Intent.createChooser(intent, "Export via"))
            }
        )
    }
}

@Composable
fun StudentFeeManagerDialog(
    student: Student,
    viewModel: com.example.ui.viewmodel.AttendanceViewModel,
    onDismiss: () -> Unit,
    onFeesChanged: () -> Unit
) {
    val context = LocalContext.current
    var studentFees by remember { mutableStateOf(viewModel.getStudentFees(student.studentId)) }
    var newFeeName by remember { mutableStateOf("") }
    var newFeeAmount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manage Fees: ${student.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val total = studentFees.sumOf { it.amount }
                val paid = studentFees.filter { it.isPaid }.sumOf { it.amount }
                val due = studentFees.filter { !it.isPaid }.sumOf { it.amount }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Fee:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("₹${String.format("%,d", total)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Paid amount:", fontSize = 12.sp, color = Color(0xFF2E7D32))
                            Text("₹${String.format("%,d", paid)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pending Due:", fontSize = 12.sp, color = Color(0xFFC62828))
                            Text("₹${String.format("%,d", due)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC62828))
                        }
                    }
                }

                Text("Add Custom Fee Item:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newFeeName,
                        onValueChange = { newFeeName = it },
                        label = { Text("Fee Name / Particulars", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newFeeAmount,
                        onValueChange = { newFeeAmount = it },
                        label = { Text("Amount (₹)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    Button(
                        onClick = {
                            if (newFeeName.isBlank()) {
                                Toast.makeText(context, "Please enter fee name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val amt = newFeeAmount.toIntOrNull() ?: 0
                            if (amt <= 0) {
                                Toast.makeText(context, "Please enter valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.addStudentFeeItem(student.studentId, newFeeName, amt)
                            studentFees = viewModel.getStudentFees(student.studentId)
                            newFeeName = ""
                            newFeeAmount = ""
                            onFeesChanged()
                            Toast.makeText(context, "Added student-specific fee", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Fee Item")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Fee Items & Payments:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (studentFees.isEmpty()) {
                    Text("No custom fees defined.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    studentFees.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("₹${String.format("%,d", item.amount)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (item.isPaid) Color(0xFF4CAF50) else Color(0xFFF44336))
                                    )
                                    Text(
                                        text = if (item.isPaid) "PAID" else "PENDING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isPaid) Color(0xFF4CAF50) else Color(0xFFF44336)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.isPaid,
                                    onCheckedChange = { isChecked ->
                                        viewModel.toggleStudentFeePaidStatus(student.studentId, item.id, isChecked)
                                        studentFees = viewModel.getStudentFees(student.studentId)
                                        onFeesChanged()
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.deleteStudentFeeItem(student.studentId, item.id)
                                        studentFees = viewModel.getStudentFees(student.studentId)
                                        onFeesChanged()
                                        Toast.makeText(context, "Deleted fee item", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Button(
                    onClick = {
                        if (due <= 0) {
                            Toast.makeText(context, "No outstanding dues!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.sendFeeReminderNotification(student.studentId, student.name, due)
                            Toast.makeText(context, "🔔 Fee Reminder push notification sent!", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Fee Reminder Notification")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ProfileField(label: String, value: String) {
    val isPhone = label.contains("Mobile", ignoreCase = true) || label.contains("Phone", ignoreCase = true) || label.contains("Contact", ignoreCase = true)
    val context = LocalContext.current
    val clickableModifier = if (isPhone && value.trim().isNotEmpty()) {
        Modifier.clickable {
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${value.trim()}"))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        Modifier
    }

    Row(
        modifier = Modifier.fillMaxWidth().then(clickableModifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPhone) MaterialTheme.colorScheme.primary else Color.Gray
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isPhone) FontWeight.Bold else FontWeight.Medium,
            color = if (isPhone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkManagementDialog(
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val homeworkList by viewModel.homeworkList.collectAsState()
    val batchesList by viewModel.batchesList.collectAsState()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(batchesList.firstOrNull() ?: "Class 10") }
    var selectedSection by remember { mutableStateOf("A") }
    var dueDate by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Attachment State
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pickedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pickedPdfName by remember { mutableStateOf("") }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        pickedPhotoUri = uri
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pickedPdfUri = it
            pickedPdfName = viewModel.getFileName(it)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ListAlt, contentDescription = null, tint = Color(0xFF0C3C44))
                Text("Homework Manager", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Active Homework (${homeworkList.size})", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    if (homeworkList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No homework assigned yet", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(homeworkList) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${item.title} (${item.studentClass} - ${item.section})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(item.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Due: ${item.dueDate}", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                        if (item.photoPath.isNotEmpty() || item.pdfPath.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                if (item.photoPath.isNotEmpty()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Image, contentDescription = "Photo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("Photo", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                if (item.pdfPath.isNotEmpty()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.Red, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(item.pdfName.take(15) + (if(item.pdfName.length > 15) "..." else ""), fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteHomework(item)
                                            Toast.makeText(context, "Homework removed", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text("Assign New Homework", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Subject / Title (e.g. Algebra)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // DatePicker dialog setup
                val calendar = Calendar.getInstance()
                val datePickerDialog = android.app.DatePickerDialog(
                    context,
                    { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                        val formattedMonth = String.format("%02d", selectedMonth + 1)
                        val formattedDay = String.format("%02d", selectedDayOfMonth)
                        dueDate = "$selectedYear-$formattedMonth-$formattedDay"
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                // Dynamic Class Dropdown Selector
                var classDropdownExpanded by remember { mutableStateOf(false) }
                val classesToChoose = remember(batchesList) {
                    if (batchesList.isNotEmpty()) batchesList else listOf("Class 10", "Class 9", "Class 8", "Class 7")
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedClass,
                        onValueChange = {},
                        label = { Text("Class / Batch") },
                        placeholder = { Text("Select Class") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Class") }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { classDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false }
                    ) {
                        classesToChoose.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls) },
                                onClick = {
                                    selectedClass = cls
                                    classDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Section Selector
                var sectionDropdownExpanded by remember { mutableStateOf(false) }
                val sectionsToChoose = listOf("ALL", "A", "B", "C")
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSection,
                        onValueChange = {},
                        label = { Text("Section") },
                        placeholder = { Text("Select Section") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Section") }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { sectionDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = sectionDropdownExpanded,
                        onDismissRequest = { sectionDropdownExpanded = false }
                    ) {
                        sectionsToChoose.forEach { sec ->
                            DropdownMenuItem(
                                text = { Text(sec) },
                                onClick = {
                                    selectedSection = sec
                                    sectionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Due Date Picker field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = {},
                        label = { Text("Due Date") },
                        placeholder = { Text("Select Due Date") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Select Date") }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Homework Details / Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 2
                )

                // Attachment UI block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Add Attachments (Optional):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Image picking button
                        OutlinedButton(
                            onClick = { photoLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick Photo", fontSize = 11.sp)
                        }

                        // PDF picking button
                        OutlinedButton(
                            onClick = { pdfLauncher.launch("application/pdf") },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick PDF", fontSize = 11.sp)
                        }
                    }

                    // Selected attachment previews
                    if (pickedPhotoUri != null || pickedPdfUri != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            pickedPhotoUri?.let { uri ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Photo Selected", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                    IconButton(
                                        onClick = { pickedPhotoUri = null },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            pickedPdfUri?.let { uri ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = pickedPdfName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = { 
                                            pickedPdfUri = null
                                            pickedPdfName = ""
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val t = title.trim()
                        val d = description.trim()
                        val c = selectedClass.trim()
                        val s = selectedSection.trim()
                        val due = dueDate.trim()
                        if (t.isNotEmpty() && d.isNotEmpty() && c.isNotEmpty() && s.isNotEmpty() && due.isNotEmpty()) {
                            // Copy attachments to internal files app directory
                            var savedPhotoPath = ""
                            var savedPdfPath = ""
                            var savedPdfName = ""

                            pickedPhotoUri?.let { uri ->
                                val path = viewModel.saveHomeworkAttachment(uri, isPdf = false)
                                if (path != null) {
                                    savedPhotoPath = path
                                }
                            }

                            pickedPdfUri?.let { uri ->
                                val path = viewModel.saveHomeworkAttachment(uri, isPdf = true)
                                if (path != null) {
                                    savedPdfPath = path
                                    savedPdfName = pickedPdfName
                                }
                            }

                            viewModel.addHomework(
                                title = t,
                                description = d,
                                studentClass = c,
                                section = s,
                                dueDate = due,
                                photoPath = savedPhotoPath,
                                pdfPath = savedPdfPath,
                                pdfName = savedPdfName
                            )

                            title = ""
                            description = ""
                            dueDate = ""
                            pickedPhotoUri = null
                            pickedPdfUri = null
                            pickedPdfName = ""
                            Toast.makeText(context, "Homework assigned and synced", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Post Homework")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassworkManagementDialog(
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val classworkList by viewModel.classworkList.collectAsState()
    val batchesList by viewModel.batchesList.collectAsState()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(batchesList.firstOrNull() ?: "Class 10") }
    var selectedSection by remember { mutableStateOf("A") }
    var date by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF144D1E))
                Text("Classwork Manager", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Active Classwork (${classworkList.size})", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    if (classworkList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No classwork assigned yet", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(classworkList) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${item.title} (${item.studentClass} - ${item.section})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(item.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Date: ${item.date}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteClasswork(item)
                                            Toast.makeText(context, "Classwork removed", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text("Assign New Classwork", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Subject / Title (e.g. Chemistry Lab)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // DatePicker dialog setup
                val calendar = Calendar.getInstance()
                val datePickerDialog = android.app.DatePickerDialog(
                    context,
                    { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                        val formattedMonth = String.format("%02d", selectedMonth + 1)
                        val formattedDay = String.format("%02d", selectedDayOfMonth)
                        date = "$selectedYear-$formattedMonth-$formattedDay"
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                // Dynamic Class Dropdown Selector
                var classDropdownExpanded by remember { mutableStateOf(false) }
                val classesToChoose = remember(batchesList) {
                    if (batchesList.isNotEmpty()) batchesList else listOf("Class 10", "Class 9", "Class 8", "Class 7")
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedClass,
                        onValueChange = {},
                        label = { Text("Class / Batch") },
                        placeholder = { Text("Select Class") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Class") }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { classDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false }
                    ) {
                        classesToChoose.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls) },
                                onClick = {
                                    selectedClass = cls
                                    classDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Section Selector
                var sectionDropdownExpanded by remember { mutableStateOf(false) }
                val sectionsToChoose = listOf("ALL", "A", "B", "C")
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSection,
                        onValueChange = {},
                        label = { Text("Section") },
                        placeholder = { Text("Select Section") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Section") }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { sectionDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = sectionDropdownExpanded,
                        onDismissRequest = { sectionDropdownExpanded = false }
                    ) {
                        sectionsToChoose.forEach { sec ->
                            DropdownMenuItem(
                                text = { Text(sec) },
                                onClick = {
                                    selectedSection = sec
                                    sectionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Picker field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        label = { Text("Classwork Date") },
                        placeholder = { Text("Select Date") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Select Date") }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Classwork Details / Work Done") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 2
                )

                Button(
                    onClick = {
                        val t = title.trim()
                        val d = description.trim()
                        val c = selectedClass.trim()
                        val s = selectedSection.trim()
                        val dt = date.trim()
                        if (t.isNotEmpty() && d.isNotEmpty() && c.isNotEmpty() && s.isNotEmpty() && dt.isNotEmpty()) {
                            viewModel.addClasswork(t, d, c, s, dt)
                            title = ""
                            description = ""
                            date = ""
                            Toast.makeText(context, "Classwork assigned and synced", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Post Classwork")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentLeaveRequestsDialog(
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val leaveRequests by viewModel.leaveRequests.collectAsState()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    tint = Color(0xFF78909C),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Student Leave Requests",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (leaveRequests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No leave requests submitted yet.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    leaveRequests.forEach { leave ->
                        val statusColor = when (leave.status) {
                            "Approved" -> Color(0xFF2E7D32)
                            "Rejected" -> Color(0xFFC62828)
                            else -> Color(0xFFFFB300)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    Column {
                                        Text(
                                            text = leave.studentName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "ID: ${leave.studentId}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(statusColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = leave.status.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                                Text(
                                    text = "Category: ${leave.type}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Dates: ${leave.dates}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Reason: ${leave.reason}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (leave.status == "Pending Approval") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.updateLeaveStatus(leave.id, "Approved")
                                                Toast.makeText(context, "Leave approved successfully", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2E7D32)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Approve", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.updateLeaveStatus(leave.id, "Rejected")
                                                Toast.makeText(context, "Leave rejected successfully", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFC62828)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Reject", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeStructureManagementDialog(
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val feeStructureList by viewModel.feeStructureList.collectAsState()
    val context = LocalContext.current

    var itemName by remember { mutableStateOf("") }
    var itemAmount by remember { mutableStateOf("") }
    var isMandatory by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Manage Fee Structure",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Add Fee Item Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Add New Fee Item",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Fee Particulars / Name", fontSize = 11.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = itemAmount,
                            onValueChange = { itemAmount = it },
                            label = { Text("Amount (₹)", fontSize = 11.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Mandatory Fee",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Required base payment",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isMandatory,
                                onCheckedChange = { isMandatory = it }
                            )
                        }

                        Button(
                            onClick = {
                                val amountVal = itemAmount.toIntOrNull()
                                if (itemName.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter fee name", Toast.LENGTH_SHORT).show()
                                } else if (amountVal == null || amountVal <= 0) {
                                    Toast.makeText(context, "Please enter a valid positive amount", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addFeeStructureItem(itemName, amountVal, isMandatory)
                                    itemName = ""
                                    itemAmount = ""
                                    isMandatory = true
                                    Toast.makeText(context, "Fee item added successfully", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add Particular", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                // Current Fee Items List
                Text(
                    text = "Current Fee Structure",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                if (feeStructureList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No fee structure items found.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    feeStructureList.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (item.isMandatory) Color(0xFF1B5E20) else Color(0xFF37474F),
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (item.isMandatory) "Mandatory" else "Additional",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Amount: ₹${String.format("%,d", item.amount)}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.deleteFeeStructureItem(item)
                                        Toast.makeText(context, "Particular deleted", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
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
