package com.example.ui.screens

import android.content.Intent
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
import com.example.data.model.AttendanceRecord
import com.example.data.model.Student
import com.example.ui.viewmodel.AttendanceViewModel
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAndHistoryScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val allAttendance by viewModel.allAttendanceRecords.collectAsState()

    var activeTabIdx by remember { mutableStateOf(0) } // 0: History Logs, 1: Performance Reports
    val tabTitles = listOf("Attendance History", "Attendance Reports")

    // Filters
    var filterDate by remember { mutableStateOf("") } // Date Filter: "yyyy-MM-dd" or empty
    var filterClass by remember { mutableStateOf("") } // Class Filter: "Class 10" etc or empty
    var filterStudentQuery by remember { mutableStateOf("") } // Student Search filter

    // Calculate dynamic history logs based on filters
    val filteredHistory = remember(allAttendance, filterDate, filterClass, filterStudentQuery) {
        allAttendance.filter { record ->
            val matchDate = filterDate.isBlank() || record.date == filterDate
            val matchClass = filterClass.isBlank() || record.studentClass.equals(filterClass, ignoreCase = true)
            val matchQuery = filterStudentQuery.isBlank() ||
                    record.studentName.contains(filterStudentQuery, ignoreCase = true) ||
                    record.studentId.contains(filterStudentQuery, ignoreCase = true) ||
                    record.rollNumber.contains(filterStudentQuery)
            
            matchDate && matchClass && matchQuery
        }.sortedByDescending { it.createdAt }
    }

    // Helper functions for sharing/export mock
    fun exportReportAsText(student: Student, present: Int, absent: Int, leave: Int, holiday: Int, percentage: Int): String {
        return """
            TOPPERS ACADEMY — ATTENDANCE PERFORMANCE REPORT
            ================================================
            Student Name: ${student.name}
            Student ID  : ${student.studentId}
            Class & Sec : ${student.studentClass} - ${student.section}
            Roll Number : ${student.rollNumber}
            
            SUMMARY STATISTICS:
            ------------------------------------------------
            Total Working Days : ${present + absent + leave} days
            Present Days       : $present days
            Absent Days        : $absent days
            Leave Days         : $leave days
            Holiday Days       : $holiday days
            Participation Rate : $percentage%
            
            Status             : ${student.status}
            Generated On       : ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
            ================================================
        """.trimIndent()
    }

    fun shareExportIntent(content: String, formatName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Toppers Academy $formatName Export")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, "Export via"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Row Switcher
        TabRow(selectedTabIndex = activeTabIdx) {
            tabTitles.forEachIndexed { idx, text ->
                Tab(
                    selected = activeTabIdx == idx,
                    onClick = { activeTabIdx = idx },
                    text = { Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }
        }

        // Shared Filter Header Component
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Search & Filters", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                
                // DatePicker dialog setup
                val calendar = java.util.Calendar.getInstance()
                val datePickerDialog = android.app.DatePickerDialog(
                    context,
                    { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                        val formattedMonth = String.format("%02d", selectedMonth + 1)
                        val formattedDay = String.format("%02d", selectedDayOfMonth)
                        filterDate = "$selectedYear-$formattedMonth-$formattedDay"
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                )

                val batchesList by viewModel.batchesList.collectAsState()
                var classDropdownExpanded by remember { mutableStateOf(false) }
                val classesToChoose = remember(batchesList) {
                    listOf("All") + batchesList
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Filter Selector
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = filterDate,
                            onValueChange = {},
                            label = { Text("Filter Date", fontSize = 11.sp) },
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

                    // Class Filter Selector
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = filterClass.ifBlank { "All" },
                            onValueChange = {},
                            label = { Text("Filter Class", fontSize = 11.sp) },
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
                                        filterClass = if (cls == "All") "" else cls
                                        classDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Student Query Text
                OutlinedTextField(
                    value = filterStudentQuery,
                    onValueChange = { filterStudentQuery = it },
                    label = { Text("Search by Student Name, Roll No or ID") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reports_search_input"),
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        if (filterDate.isNotEmpty() || filterClass.isNotEmpty() || filterStudentQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                filterDate = ""
                                filterClass = ""
                                filterStudentQuery = ""
                            }) {
                                Icon(Icons.Default.FilterListOff, contentDescription = "Clear Filters")
                            }
                        }
                    }
                )
            }
        }

        // Contents Panel
        when (activeTabIdx) {
            0 -> {
                // HISTORIC LOGS LIST
                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.HistoryToggleOff, contentDescription = null, size = 48.dp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Historic Logs Found", fontWeight = FontWeight.Bold)
                            Text("Try adjusting your dates or filter options above.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Beautiful active share header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing ${filteredHistory.size} history records",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )

                            Button(
                                onClick = {
                                    val dateHeader = if (filterDate.isBlank()) "All Dates" else filterDate
                                    val classHeader = if (filterClass.isBlank()) "All Classes" else filterClass
                                    val sb = StringBuilder()
                                    sb.append("TOPPERS ACADEMY — ATTENDANCE HISTORY LOGS\n")
                                    sb.append("=========================================\n")
                                    sb.append("Date Filter  : $dateHeader\n")
                                    sb.append("Class Filter : $classHeader\n")
                                    sb.append("Total Records: ${filteredHistory.size}\n")
                                    sb.append("=========================================\n\n")

                                    filteredHistory.forEachIndexed { idx, record ->
                                        sb.append("${idx + 1}. ${record.studentName} | Class: ${record.studentClass}-${record.section} | Roll: ${record.rollNumber} | Date: ${record.date} | Status: ${record.status.uppercase()}\n")
                                    }

                                    sb.append("\n-----------------------------------------\n")
                                    sb.append("Generated via Toppers Academy Secure Admin Portal\n")

                                    shareExportIntent(sb.toString(), "Attendance History Logs")
                                    Toast.makeText(context, "Exported history shared successfully!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share All Logs", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredHistory) { record ->
                                HistoryLogItem(record = record, onShareClick = {
                                    val itemStr = """
                                        TOPPERS ACADEMY — ATTENDANCE RECORD
                                        ==================================
                                        Student Name: ${record.studentName}
                                        Student ID  : ${record.studentId}
                                        Roll Number : ${record.rollNumber}
                                        Class & Sec : ${record.studentClass} - ${record.section}
                                        Date        : ${record.date}
                                        Status      : ${record.status.uppercase()}
                                        ----------------------------------
                                        Generated via Toppers Academy Portal
                                    """.trimIndent()
                                    shareExportIntent(itemStr, "Single Attendance Record")
                                    Toast.makeText(context, "Shared attendance for ${record.studentName}", Toast.LENGTH_SHORT).show()
                                })
                            }
                        }
                    }
                }
            }
            1 -> {
                // PERFORMANCE REPORTS LIST
                val activeStudents = students.filter { student ->
                    val matchClass = filterClass.isBlank() || student.studentClass.equals(filterClass, ignoreCase = true)
                    val matchQuery = filterStudentQuery.isBlank() ||
                            student.name.contains(filterStudentQuery, ignoreCase = true) ||
                            student.studentId.contains(filterStudentQuery, ignoreCase = true) ||
                            student.rollNumber.contains(filterStudentQuery)
                    
                    matchClass && matchQuery
                }

                if (activeStudents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Analytics, contentDescription = null, size = 48.dp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Match Found", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(activeStudents) { index, student ->
                            // Calculate stats
                            val studentHistory = allAttendance.filter { it.studentId == student.studentId }
                            val presents = studentHistory.count { it.status == "Present" }
                            val absents = studentHistory.count { it.status == "Absent" }
                            val leaves = studentHistory.count { it.status == "Leave" }
                            val holidays = studentHistory.count { it.status == "Holiday" }
                            
                            val totalWorking = presents + absents + leaves
                            val percentage = if (totalWorking > 0) {
                                (presents * 100) / totalWorking
                            } else {
                                100
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("${index + 1}. ${student.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("ID: ${student.studentId} | Roll: ${student.rollNumber} | ${student.studentClass}", fontSize = 11.sp, color = Color.Gray)
                                        }

                                        // Large Percentage Circular tag
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(
                                                    if (percentage >= 80) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "$percentage%",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = if (percentage >= 80) Color(0xFF065F46) else Color(0xFF991B1B)
                                            )
                                        }
                                    }

                                    // Display parameters
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ReportIndicatorChip(label = "Present", count = presents, color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                                        ReportIndicatorChip(label = "Absent", count = absents, color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                                        ReportIndicatorChip(label = "Leave", count = leaves, color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                                        ReportIndicatorChip(label = "Working Days", count = totalWorking, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1.2f))
                                    }

                                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                    // Quick Share Mock exports
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Export Performance Reports", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    val docStr = exportReportAsText(student, presents, absents, leaves, holidays, percentage)
                                                    shareExportIntent(docStr, "PDF Document")
                                                    Toast.makeText(context, "Dispatched PDF Report Layout Share", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    val docStr = exportReportAsText(student, presents, absents, leaves, holidays, percentage)
                                                    shareExportIntent(docStr, "Excel Sheet")
                                                    Toast.makeText(context, "Dispatched Microsoft Excel Sheet Layout Share", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.TableChart, contentDescription = "Excel", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    val docStr = exportReportAsText(student, presents, absents, leaves, holidays, percentage)
                                                    shareExportIntent(docStr, "Print Job")
                                                    Toast.makeText(context, "Dispatched System Print Layout Spooler Share", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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

@Composable
fun HistoryLogItem(record: AttendanceRecord, onShareClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(
                            when (record.status) {
                                "Present" -> Color(0xFFD1FAE5)
                                "Absent" -> Color(0xFFFEE2E2)
                                "Leave" -> Color(0xFFFEF3C7)
                                else -> Color(0xFFDBEAFE)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (record.status) {
                            "Present" -> Icons.Default.Check
                            "Absent" -> Icons.Default.Close
                            "Leave" -> Icons.Default.TimeToLeave
                            else -> Icons.Default.BeachAccess
                        },
                        contentDescription = null,
                        tint = when (record.status) {
                            "Present" -> Color(0xFF065F46)
                            "Absent" -> Color(0xFF991B1B)
                            "Leave" -> Color(0xFF92400E)
                            else -> Color(0xFF1E40AF)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(record.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("ID: ${record.studentId} | Roll: ${record.rollNumber} | Class: ${record.studentClass} - ${record.section}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(record.date, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        text = if (record.isSynced) "Cloud Synced" else "Local Only",
                        fontSize = 10.sp,
                        color = if (record.isSynced) Color(0xFF10B981) else Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Record",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReportIndicatorChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("$count", fontWeight = FontWeight.Black, fontSize = 13.sp, color = color)
            Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, color: Color) {
    Icon(imageVector, contentDescription, tint = color, modifier = Modifier.size(size))
}
