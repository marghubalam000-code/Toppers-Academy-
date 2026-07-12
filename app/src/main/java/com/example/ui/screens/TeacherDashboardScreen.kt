package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Student
import com.example.data.model.ExamMark
import com.example.data.model.ChatMessage
import com.example.data.model.Teacher
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.HomeworkItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(viewModel: AttendanceViewModel) {
    val context = LocalContext.current
    val currentTeacher by viewModel.currentTeacher.collectAsState()
    val teacherName = currentTeacher?.name ?: "Teacher"
    val assignedClass = currentTeacher?.assignedClass ?: "Class 10"
    val assignedSection = currentTeacher?.assignedSection ?: "A"

    val studentsList by viewModel.students.collectAsState()
    val classStudents = remember(studentsList, assignedClass, assignedSection) {
        studentsList.filter {
            it.studentClass.equals(assignedClass, ignoreCase = true) &&
                    it.section.equals(assignedSection, ignoreCase = true) &&
                    it.status == "Active"
        }
    }

    val allChats by viewModel.allChatMessages.collectAsState()
    val unreadChatCount = remember(allChats, classStudents) {
        val studentIds = classStudents.map { it.studentId }.toSet()
        allChats.count { it.studentId in studentIds && it.sender == "Parent" && !it.isRead }
    }

    var showAttendanceDialog by remember { mutableStateOf(false) }
    var showHomeworkDialog by remember { mutableStateOf(false) }
    var showMarksDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "TEACHER PORTAL",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showLogoutConfirmDialog = true },
                        modifier = Modifier.testTag("teacher_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant Welcome & Class Info Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = teacherName.take(1).uppercase(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Namaste, $teacherName",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                    Text(
                                        "$assignedClass - $assignedSection",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    "|  ${classStudents.size} Students Active",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Quick Info stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Today",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Classroom",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$assignedClass ($assignedSection)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "CLASSROOM ACTIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Grid Items
            item {
                TeacherActionRow(
                    title1 = "Record Attendance",
                    desc1 = "Mark daily absent/presents",
                    icon1 = Icons.Default.FactCheck,
                    color1 = MaterialTheme.colorScheme.primary,
                    onClick1 = {
                        // Pre-set selections in ViewModel for this teacher class/section
                        viewModel.setClassAndSection(assignedClass, assignedSection)
                        showAttendanceDialog = true
                    },
                    title2 = "Homework",
                    desc2 = "Post class homework assignments",
                    icon2 = Icons.Default.Assignment,
                    color2 = MaterialTheme.colorScheme.secondary,
                    onClick2 = { showHomeworkDialog = true }
                )
            }

            item {
                TeacherActionRow(
                    title1 = "Report Cards",
                    desc1 = "Enter marks & grades",
                    icon1 = Icons.Default.Analytics,
                    color1 = Color(0xFF00C853),
                    onClick1 = { showMarksDialog = true },
                    title2 = "Parent Chat",
                    desc2 = "Talk with parents directly",
                    icon2 = Icons.Default.Forum,
                    color2 = Color(0xFFFF9100),
                    onClick2 = { showChatDialog = true },
                    badge2 = unreadChatCount
                )
            }
        }
    }

    // Attendance Dialog
    if (showAttendanceDialog) {
        TeacherAttendanceDialog(
            viewModel = viewModel,
            classStudents = classStudents,
            assignedClass = assignedClass,
            assignedSection = assignedSection,
            onDismiss = { showAttendanceDialog = false }
        )
    }

    // Homework Dialog
    if (showHomeworkDialog) {
        TeacherHomeworkDialog(
            viewModel = viewModel,
            assignedClass = assignedClass,
            assignedSection = assignedSection,
            onDismiss = { showHomeworkDialog = false }
        )
    }

    // Report Card Dialog
    if (showMarksDialog) {
        TeacherMarksDialog(
            viewModel = viewModel,
            classStudents = classStudents,
            onDismiss = { showMarksDialog = false }
        )
    }

    // Chat Dialog
    if (showChatDialog) {
        TeacherChatDialog(
            viewModel = viewModel,
            classStudents = classStudents,
            onDismiss = { showChatDialog = false }
        )
    }

    // Logout confirmation
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Logout Confirmation") },
            text = { Text("Are you sure you want to log out from the Teacher Portal?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logoutTeacher()
                    },
                    modifier = Modifier.testTag("teacher_logout_confirm_btn")
                ) {
                    Text("YES, LOGOUT", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherActionRow(
    title1: String,
    desc1: String,
    icon1: ImageVector,
    color1: Color,
    onClick1: () -> Unit,
    title2: String,
    desc2: String,
    icon2: ImageVector,
    color2: Color,
    onClick2: () -> Unit,
    badge1: Int = 0,
    badge2: Int = 0
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .height(130.dp)
                .clickable { onClick1() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color1.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (badge1 > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    Text(badge1.toString(), fontSize = 9.sp)
                                }
                            }
                        ) {
                            Icon(imageVector = icon1, contentDescription = null, tint = color1, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Icon(imageVector = icon1, contentDescription = null, tint = color1, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Text(title1, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(desc1, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .height(130.dp)
                .clickable { onClick2() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color2.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (badge2 > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    Text(badge2.toString(), fontSize = 9.sp)
                                }
                            }
                        ) {
                            Icon(imageVector = icon2, contentDescription = null, tint = color2, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Icon(imageVector = icon2, contentDescription = null, tint = color2, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Text(title2, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(desc2, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

// Teacher Attendance Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAttendanceDialog(
    viewModel: AttendanceViewModel,
    classStudents: List<Student>,
    assignedClass: String,
    assignedSection: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val attendanceSheet by viewModel.attendanceSheet.collectAsState()

    // Initialize all as Present if empty
    LaunchedEffect(classStudents) {
        classStudents.forEach {
            if (attendanceSheet[it.studentId] == null) {
                viewModel.setStudentStatus(it.studentId, "Present")
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Take Attendance", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        actions = {
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveAttendance(sendNotifications = true)
                                Toast.makeText(context, "Attendance saved! Absent alerts sent.", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(50.dp)
                                .testTag("teacher_save_attendance_btn")
                        ) {
                            Text("SAVE ATTENDANCE & ALERT PARENTS")
                        }
                    }
                }
            ) { pad ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pad)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date: $selectedDate", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.markAllAs("Present") }) {
                                Text("All Present")
                            }
                            TextButton(onClick = { viewModel.markAllAs("Absent") }) {
                                Text("All Absent")
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(classStudents) { index, student ->
                            val status = attendanceSheet[student.studentId] ?: "Present"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (status == "Absent") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${index + 1}. ${student.name}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Roll No: ${student.rollNumber}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("Present", "Absent", "Leave", "Cleared").forEach { state ->
                                            val isSelected = status == state
                                            val color = when (state) {
                                                "Present" -> Color(0xFF00C853)
                                                "Absent" -> Color(0xFFD50000)
                                                "Leave" -> Color(0xFFFFAB00)
                                                else -> Color(0xFF757575)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) color else color.copy(alpha = 0.08f))
                                                    .clickable { viewModel.setStudentStatus(student.studentId, state) }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (state == "Cleared") "Clear" else state,
                                                    color = if (isSelected) Color.White else color,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
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

// Teacher Homework Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeworkDialog(
    viewModel: AttendanceViewModel,
    assignedClass: String,
    assignedSection: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    val homeworkList by viewModel.homeworkList.collectAsState()

    val filteredHomework = remember(homeworkList) {
        homeworkList.filter {
            it.studentClass.equals(assignedClass, ignoreCase = true) &&
                    it.section.equals(assignedSection, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Class Homework Manager", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        actions = {
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    )
                }
            ) { pad ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pad)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Assign New Homework", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Homework Title") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("Instructions") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = dueDate,
                                    onValueChange = { dueDate = it },
                                    label = { Text("Due Date (e.g. 10 July)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (title.isNotEmpty() && description.isNotEmpty() && dueDate.isNotEmpty()) {
                                            viewModel.addHomework(title, description, assignedClass, assignedSection, dueDate)
                                            Toast.makeText(context, "Homework assigned successfully!", Toast.LENGTH_SHORT).show()
                                            title = ""
                                            description = ""
                                            dueDate = ""
                                        } else {
                                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("teacher_add_hw_btn")
                                ) {
                                    Text("ASSIGN HOMEWORK")
                                }
                            }
                        }
                    }

                    item {
                        Text("Active Homework Assignments", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    if (filteredHomework.isEmpty()) {
                        item {
                            Text("No homework assigned yet.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                        }
                    } else {
                        items(filteredHomework) { hw ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(hw.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        IconButton(onClick = { viewModel.deleteHomework(hw) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                    Text(hw.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Due Date: ${hw.dueDate}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Teacher Marks Entry & Report Card Generation Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherMarksDialog(
    viewModel: AttendanceViewModel,
    classStudents: List<Student>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var examType by remember { mutableStateOf("Unit Test") } // "Unit Test", "Half-Yearly", "Final Exam"
    var subject by remember { mutableStateOf("Mathematics") } // Mathematics, Science, English, Hindi, Social Science
    var marksObtained by remember { mutableStateOf("") }
    var maxMarks by remember { mutableStateOf("100") }
    var remarks by remember { mutableStateOf("") }

    val allMarks by viewModel.allMarks.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (selectedStudent == null) "Select Student for Marks" else "Enter Marks: ${selectedStudent?.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                        navigationIcon = {
                            if (selectedStudent != null) {
                                IconButton(onClick = { selectedStudent = null }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    )
                }
            ) { pad ->
                if (selectedStudent == null) {
                    // List of class students to select
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(pad)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(classStudents) { idx, student ->
                            val studentMarks = allMarks.filter { it.studentId == student.studentId }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStudent = student },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${idx + 1}. ${student.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Roll No: ${student.rollNumber}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                        Text(
                                            "${studentMarks.size} records",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Enter marks for the selected student
                    val student = selectedStudent!!
                    val currentStudentMarks = remember(allMarks, student.studentId) {
                        allMarks.filter { it.studentId == student.studentId }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(pad)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Enter Marks", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                    
                                    // Exam Type Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("Unit Test", "Half-Yearly", "Final Exam").forEach { type ->
                                            val isSelected = examType == type
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable { examType = type }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    type,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // Subject Selector Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("Maths", "Science", "English", "Hindi", "S.St").forEach { sub ->
                                            val fullSub = when (sub) {
                                                "Maths" -> "Mathematics"
                                                "S.St" -> "Social Science"
                                                else -> sub
                                            }
                                            val isSelected = subject == fullSub
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable { subject = fullSub }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    sub,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = marksObtained,
                                            onValueChange = { marksObtained = it },
                                            label = { Text("Marks") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = maxMarks,
                                            onValueChange = { maxMarks = it },
                                            label = { Text("Max") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = remarks,
                                        onValueChange = { remarks = it },
                                        label = { Text("Remarks (e.g. Excellent)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            val obt = marksObtained.toDoubleOrNull()
                                            val mx = maxMarks.toDoubleOrNull()
                                            if (obt != null && mx != null) {
                                                if (obt > mx) {
                                                    Toast.makeText(context, "Marks obtained cannot exceed max marks", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.addExamMark(student.studentId, student.name, examType, subject, obt, mx, remarks)
                                                    Toast.makeText(context, "Marks saved successfully!", Toast.LENGTH_SHORT).show()
                                                    marksObtained = ""
                                                    remarks = ""
                                                }
                                            } else {
                                                Toast.makeText(context, "Enter valid numbers", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("save_marks_btn")
                                    ) {
                                        Text("SAVE EXAM MARKS")
                                    }
                                }
                            }
                        }

                        item {
                            Text("Student Report Card - Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Group marks by Exam Type
                        val examsGrouped = currentStudentMarks.groupBy { it.examType }
                        if (examsGrouped.isEmpty()) {
                            item {
                                Text("No marks recorded yet.", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            examsGrouped.forEach { (exam, marksList) ->
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(exam, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                                // Auto-calculate percentage
                                                val totalObt = marksList.sumOf { it.marksObtained }
                                                val totalMax = marksList.sumOf { it.maxMarks }
                                                val percent = if (totalMax > 0) (totalObt / totalMax) * 100 else 0.0
                                                val grade = when {
                                                    percent >= 90 -> "A+"
                                                    percent >= 80 -> "A"
                                                    percent >= 70 -> "B"
                                                    percent >= 60 -> "C"
                                                    percent >= 50 -> "D"
                                                    else -> "F"
                                                }
                                                Badge(containerColor = if (percent >= 50) Color(0xFF00C853) else Color.Red) {
                                                    Text(
                                                        "${String.format("%.1f", percent)}% (Grade $grade)",
                                                        modifier = Modifier.padding(6.dp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider()
                                            Spacer(modifier = Modifier.height(8.dp))

                                            marksList.forEach { mk ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(mk.subject, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        if (mk.remarks.isNotEmpty()) {
                                                            Text(mk.remarks, fontSize = 10.sp, color = Color.Gray)
                                                        }
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("${mk.marksObtained.toInt()} / ${mk.maxMarks.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        IconButton(
                                                            onClick = { viewModel.deleteExamMark(mk) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
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
        }
    }
}

// Teacher Secure Parent Chats Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherChatDialog(
    viewModel: AttendanceViewModel,
    classStudents: List<Student>,
    onDismiss: () -> Unit
) {
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var chatInputText by remember { mutableStateOf("") }
    val allChats by viewModel.allChatMessages.collectAsState()
    val currentTeacher by viewModel.currentTeacher.collectAsState()

    LaunchedEffect(selectedStudent, allChats) {
        selectedStudent?.let {
            viewModel.markMessagesAsRead(it.studentId, "Parent")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (selectedStudent == null) "Select Student Thread" else "Chat with: ${selectedStudent?.name}'s Parent", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        navigationIcon = {
                            if (selectedStudent != null) {
                                IconButton(onClick = { selectedStudent = null }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    )
                },
                bottomBar = {
                    if (selectedStudent != null) {
                        Surface(
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth().imePadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = chatInputText,
                                    onValueChange = { chatInputText = it },
                                    placeholder = { Text("Type messaging alert...") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        if (chatInputText.trim().isNotEmpty()) {
                                            viewModel.sendChatMessage(
                                                studentId = selectedStudent!!.studentId,
                                                studentName = selectedStudent!!.name,
                                                sender = "Teacher",
                                                senderName = currentTeacher?.name ?: "Class Teacher",
                                                msgText = chatInputText.trim()
                                            )
                                            chatInputText = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .testTag("teacher_send_chat_btn")
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Default.Send, contentDescription = "Send", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            ) { pad ->
                if (selectedStudent == null) {
                    // Chat Threads list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(pad)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(classStudents) { idx, student ->
                            val threadChats = allChats.filter { it.studentId == student.studentId }
                            val lastMessage = threadChats.lastOrNull()?.message ?: "No messages yet."
                            val unreadCount = threadChats.count { it.sender == "Parent" && !it.isRead }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStudent = student },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${idx + 1}. ${student.name}'s Parent", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(lastMessage, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                    }
                                    if (unreadCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text("$unreadCount New", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Chat messages interface
                    val student = selectedStudent!!
                    val threadChats = remember(allChats, student.studentId) {
                        allChats.filter { it.studentId == student.studentId }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(pad)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(threadChats) { chat ->
                            val isMe = chat.sender == "Teacher" || chat.sender == "Admin"
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 2.dp,
                                                bottomEnd = if (isMe) 2.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = chat.message,
                                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = chat.senderName,
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
