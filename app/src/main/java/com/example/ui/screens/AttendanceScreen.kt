package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.components.bounceClick
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val attendanceSheet by viewModel.attendanceSheet.collectAsState()
    
    val selectedClass by viewModel.selectedClass.collectAsState()
    val selectedSection by viewModel.selectedSection.collectAsState()
    val dateDisplay by viewModel.selectedDateDisplay.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var sendNotifications by remember { mutableStateOf(true) }
    var showConfirmSaveDialog by remember { mutableStateOf(false) }

    // List of classes and sections
    val classes by viewModel.batchesList.collectAsState()
    val sections = listOf("A", "B", "C")

    // Get current active students matching filters
    val filteredStudents = remember(students, selectedClass, selectedSection) {
        students.filter {
            it.studentClass.equals(selectedClass, ignoreCase = true) &&
            it.section.equals(selectedSection, ignoreCase = true) &&
            it.status == "Active"
        }
    }

    // Handlers for Toast feedbacks
    LaunchedEffect(uiState) {
        if (uiState is UiState.SuccessWithSync) {
            val state = uiState as UiState.SuccessWithSync
            Toast.makeText(
                context,
                "Attendance saved successfully. Synced ${state.syncedCount}/${state.totalCount} with Firestore!",
                Toast.LENGTH_LONG
            ).show()
            viewModel.clearUiState()
        } else if (uiState is UiState.Error) {
            val errorState = uiState as UiState.Error
            Toast.makeText(context, "Error: ${errorState.message}", Toast.LENGTH_LONG).show()
            viewModel.clearUiState()
        }
    }

    Scaffold(
        bottomBar = {
            if (filteredStudents.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Notifications toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sendNotifications = !sendNotifications },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (sendNotifications) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (sendNotifications) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Dispatch Parent Lock-Screen Alerts",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = sendNotifications,
                                onCheckedChange = { sendNotifications = it },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        // Save Button
                        Button(
                            onClick = { showConfirmSaveDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick()
                                .testTag("save_attendance_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save & Sync Attendance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Selector Panel (Class & Section & Date)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // First Row: Class Selector and Section Selector side by side
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Class Dropdown Selector
                                var classExpanded by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { classExpanded = true }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "Class: $selectedClass", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    DropdownMenu(
                                        expanded = classExpanded,
                                        onDismissRequest = { classExpanded = false }
                                    ) {
                                        classes.forEach { cls ->
                                            DropdownMenuItem(
                                                text = { Text(cls, fontSize = 13.sp) },
                                                onClick = {
                                                    viewModel.setClassAndSection(cls, selectedSection)
                                                    classExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Section Dropdown Selector
                                var sectionExpanded by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { sectionExpanded = true }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "Sec: $selectedSection", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    DropdownMenu(
                                        expanded = sectionExpanded,
                                        onDismissRequest = { sectionExpanded = false }
                                    ) {
                                        sections.forEach { sec ->
                                            DropdownMenuItem(
                                                text = { Text("Section $sec", fontSize = 13.sp) },
                                                onClick = {
                                                    viewModel.setClassAndSection(selectedClass, sec)
                                                    sectionExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Second Row: Date picker in compact style
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.changeDateByDays(-1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Day", modifier = Modifier.size(18.dp))
                                }

                                val currentDateStr by viewModel.selectedDate.collectAsState()
                                val datePickerDialog = remember(currentDateStr) {
                                    val dateParts = currentDateStr.split("-")
                                    val year = dateParts.getOrNull(0)?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                    val month = (dateParts.getOrNull(1)?.toIntOrNull() ?: (java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)) - 1
                                    val day = dateParts.getOrNull(2)?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)

                                    android.app.DatePickerDialog(
                                        context,
                                        { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                                            val formattedMonth = String.format("%02d", selectedMonth + 1)
                                            val formattedDay = String.format("%02d", selectedDayOfMonth)
                                            viewModel.setDate("$selectedYear-$formattedMonth-$formattedDay")
                                        },
                                        year,
                                        month,
                                        day
                                    ).apply {
                                        datePicker.maxDate = System.currentTimeMillis()
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                        .clickable { datePickerDialog.show() }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Pick Date",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dateDisplay,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Tap to pick historical date",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.changeDateByDays(1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // 2. Multi-Action Overrides (Mark All Present/Absent)
                if (filteredStudents.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.markAllAs("Present") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .bounceClick(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6F4EA), contentColor = Color(0xFF137333)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("All Present", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.markAllAs("Absent") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .bounceClick(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCE8E6), contentColor = Color(0xFFC5221F)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("All Absent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.markAllAs("Holiday") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .bounceClick(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F0FE), contentColor = Color(0xFF1A73E8)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.BeachAccess, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Holiday", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.markAllAs("Cleared") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .bounceClick(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF374151)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 3. Students Attendance List / Empty State
                if (filteredStudents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupOff,
                                    contentDescription = "No Students",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Students Found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "There are no active enrolled students registered in $selectedClass - Section $selectedSection. Go to Students screen to enroll students first.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(filteredStudents) { index, student ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            val currentStatus = attendanceSheet[student.studentId] ?: "Present"
                            AttendanceSelectorCard(
                                student = student,
                                serialNumber = index + 1,
                                status = currentStatus,
                                onStatusChange = { newStatus ->
                                    viewModel.setStudentStatus(student.studentId, newStatus)
                                }
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Duplicate / Confirm Overwrite Warning Dialog
        if (showConfirmSaveDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmSaveDialog = false },
                title = { Text("Confirm Attendance Save", fontWeight = FontWeight.Bold) },
                text = {
                    Text("This action will record attendance for ${filteredStudents.size} students on $dateDisplay. If records already exist, they will be securely updated without creating duplicates. Continue?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveAttendance(sendNotifications)
                            showConfirmSaveDialog = false
                        }
                    ) {
                        Text("Save & Dispatch")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AttendanceSelectorCard(
    student: Student,
    serialNumber: Int,
    status: String,
    onStatusChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.rollNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column {
                        Text("${serialNumber}. ${student.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("ID: ${student.studentId}", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                // Current Active Status indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (status) {
                                "Present" -> Color(0xFFD1FAE5)
                                "Absent" -> Color(0xFFFEE2E2)
                                "Leave" -> Color(0xFFFEF3C7)
                                "Cleared" -> Color(0xFFF3F4F6)
                                else -> Color(0xFFDBEAFE)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = status.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = when (status) {
                            "Present" -> Color(0xFF065F46)
                            "Absent" -> Color(0xFF991B1B)
                            "Leave" -> Color(0xFF92400E)
                            "Cleared" -> Color(0xFF374151)
                            else -> Color(0xFF1E40AF)
                        }
                    )
                }
            }

            // Custom Segmented Button Selector for Present, Absent, Leave, Holiday, Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val statuses = listOf("Present", "Absent", "Leave", "Holiday", "Cleared")
                statuses.forEach { item ->
                    val isSelected = status == item
                    val activeColor = when (item) {
                        "Present" -> Color(0xFF10B981)
                        "Absent" -> Color(0xFFEF4444)
                        "Leave" -> Color(0xFFF59E0B)
                        "Cleared" -> Color(0xFF6B7280)
                        else -> Color(0xFF3B82F6)
                    }

                    Button(
                        onClick = { onStatusChange(item) },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .bounceClick()
                            .testTag("btn_${item}_${student.studentId}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (item == "Cleared") "Clear" else item, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Help scaler modifier extension
private fun Modifier.scale(scale: Float): Modifier = this
