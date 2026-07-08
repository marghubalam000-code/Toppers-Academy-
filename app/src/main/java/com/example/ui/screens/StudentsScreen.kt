package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.Student
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.components.bounceClick
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.rememberScrollState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var studentToView by remember { mutableStateOf<Student?>(null) }
    var studentToManageFees by remember { mutableStateOf<Student?>(null) }
    var showDeveloperDialog by remember { mutableStateOf(false) }

    // Filter students
    val filteredStudents = students.filter { student ->
        student.name.contains(searchQuery, ignoreCase = true) ||
        student.rollNumber.contains(searchQuery) ||
        student.studentId.contains(searchQuery, ignoreCase = true) ||
        student.studentClass.contains(searchQuery, ignoreCase = true) ||
        student.mobile.contains(searchQuery)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.bounceClick().testTag("add_student_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Student")
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
            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("student_search_input"),
                placeholder = { Text("Search by ID, name, class, roll, mobile...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // 2. Student Directory Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Student Directory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${filteredStudents.size} enrolled",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 3. Students List
            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = if (searchQuery.isEmpty()) Icons.Default.School else Icons.Default.PersonOff,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No Students Enrolled" else "No matching students",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) {
                                "Tap the floating button below to enroll a new student into Toppers Academy."
                            } else {
                                "Refine your search parameters and try again."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
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
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(filteredStudents) { index, student ->
                        StudentCard(
                            student = student,
                            serialNumber = index + 1,
                            onEdit = { studentToEdit = student },
                            onDelete = { studentToDelete = student },
                            onView = { studentToView = student },
                            onManageFees = { studentToManageFees = student }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            MarghubSignatureBadge(
                                onClick = { showDeveloperDialog = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(80.dp)) // Floating space
                    }
                }
            }
        }

        // Add Dialog
        if (showAddDialog) {
            StudentFormDialog(
                title = "Enroll New Student",
                viewModel = viewModel,
                onDismiss = { showAddDialog = false },
                onSave = { name, email, cls, section, roll, mobile, father, mother, mainSubject, photo, status, gender, aadhaar, address, fatherMobile ->
                    viewModel.addStudent(
                        name = name,
                        email = email,
                        studentClass = cls,
                        section = section,
                        rollNumber = roll,
                        mobile = mobile,
                        fatherName = father,
                        motherName = mother,
                        mainSubject = mainSubject,
                        photo = photo,
                        status = status,
                        gender = gender,
                        aadhaar = aadhaar,
                        address = address,
                        fatherMobile = fatherMobile
                    )
                    showAddDialog = false
                    Toast.makeText(context, "Student added successfully", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Edit Dialog
        if (studentToEdit != null) {
            StudentFormDialog(
                title = "Edit Student Profile",
                student = studentToEdit,
                viewModel = viewModel,
                onDismiss = { studentToEdit = null },
                onSave = { name, email, cls, section, roll, mobile, father, mother, mainSubject, photo, status, gender, aadhaar, address, fatherMobile ->
                    val updated = studentToEdit!!.copy(
                        name = name,
                        email = email,
                        studentClass = cls,
                        section = section,
                        rollNumber = roll,
                        mobile = mobile,
                        fatherName = father,
                        motherName = mother,
                        mainSubject = mainSubject,
                        photo = photo,
                        status = status,
                        gender = gender,
                        aadhaar = aadhaar,
                        address = address,
                        fatherMobile = fatherMobile
                    )
                    viewModel.updateStudent(updated)
                    studentToEdit = null
                    Toast.makeText(context, "Student profile updated", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Delete Confirm Dialog
        if (studentToDelete != null) {
            AlertDialog(
                onDismissRequest = { studentToDelete = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteStudent(studentToDelete!!)
                            studentToDelete = null
                            Toast.makeText(context, "Student removed from Academy", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { studentToDelete = null }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Remove Student?") },
                text = { Text("Are you sure you want to remove ${studentToDelete!!.name} (ID ${studentToDelete!!.studentId}) from Toppers Academy? This action cannot be undone.") }
            )
        }

        // View Student Profile Dialog
        if (studentToView != null) {
            StudentProfileDialog(
                student = studentToView!!,
                allAttendance = emptyList(), // Can display a simple list or overview of attendance records
                viewModel = viewModel,
                onDismiss = { studentToView = null }
            )
        }

        if (studentToManageFees != null) {
            StudentFeesManagementDialog(
                student = studentToManageFees!!,
                viewModel = viewModel,
                onDismiss = { studentToManageFees = null }
            )
        }

        if (showDeveloperDialog) {
            MarghubProfileDialog(
                onDismiss = { showDeveloperDialog = false }
            )
        }
    }
}

@Composable
fun StudentCard(
    student: Student,
    serialNumber: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit,
    onManageFees: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        StudentPhoto(
                            photoData = student.photo,
                            modifier = Modifier.fillMaxSize(),
                            placeholderIcon = Icons.Default.School,
                            tintColor = if (student.status == "Active") {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Gray
                            }
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${serialNumber}. ${student.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (student.status == "Active") Color(0xFFD1FAE5) else Color(0xFFF3F4F6)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    // Make sure status badge conforms to M3 touch spacing
                            ) {
                                Text(
                                    text = student.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (student.status == "Active") Color(0xFF065F46) else Color(0xFF374151)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Roll: ${student.rollNumber}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "•",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "${student.studentClass} - ${student.section}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Action Column
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onManageFees,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("fees_student_${student.studentId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Manage fees",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onView,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("view_student_${student.studentId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "View profile",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_student_${student.studentId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit student",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_student_${student.studentId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete student",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Contact Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ID: ${student.studentId}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "PIN: ${student.password.ifEmpty { "123456" }}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone Icon",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = student.mobile,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFormDialog(
    title: String,
    student: Student? = null,
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        email: String,
        studentClass: String,
        section: String,
        rollNumber: String,
        mobile: String,
        fatherName: String,
        motherName: String,
        mainSubject: String,
        photo: String,
        status: String,
        gender: String,
        aadhaar: String,
        address: String,
        fatherMobile: String
    ) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("👤 Profile", "🎓 Academic", "👨‍👩‍👦 Parents")

    var name by remember { mutableStateOf(student?.name ?: "") }
    var email by remember { mutableStateOf(student?.email ?: "") }
    var studentClass by remember { mutableStateOf(student?.studentClass ?: "Class 10") }
    var section by remember { mutableStateOf(student?.section ?: "A") }
    var rollNumber by remember { mutableStateOf(student?.rollNumber ?: "") }
    var mobile by remember { mutableStateOf(student?.mobile ?: "") }
    var fatherName by remember { mutableStateOf(student?.fatherName ?: "") }
    var motherName by remember { mutableStateOf(student?.motherName ?: "") }
    var mainSubject by remember { mutableStateOf(student?.mainSubject ?: "") }
    var photoBase64 by remember { mutableStateOf(student?.photo ?: "") }
    var status by remember { mutableStateOf(student?.status ?: "Active") }
    var gender by remember { mutableStateOf(student?.gender ?: "Male") }
    var aadhaar by remember { mutableStateOf(student?.aadhaar ?: "") }
    var address by remember { mutableStateOf(student?.address ?: "") }
    var fatherMobile by remember { mutableStateOf(student?.fatherMobile ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var rollError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var subjectError by remember { mutableStateOf<String?>(null) }
    var aadhaarError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var fatherMobileError by remember { mutableStateOf<String?>(null) }

    val batchesListState = viewModel.batchesList.collectAsState()
    val classesList = batchesListState.value
    val allStudentsState = viewModel.students.collectAsState()
    val allStudents = allStudentsState.value
    val sectionsList = listOf("A", "B", "C")

    val context = LocalContext.current
    var showPresetPicker by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val b64 = uriToBase64(context, it)
            if (b64 != null) {
                photoBase64 = b64
            } else {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(vertical = 12.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("dialog_student_title")
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Styled tabs row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, label ->
                        val isSelected = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }.using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "TabContentAnimation"
                ) { tabIndex ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (tabIndex) {
                            0 -> {
                                // Photo selection section
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            .clickable {
                                                showPresetPicker = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        StudentPhoto(
                                            photoData = photoBase64,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .border(2.dp, Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Edit photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        TextButton(
                                            onClick = { galleryLauncher.launch("image/*") }
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Gallery Upload", fontSize = 12.sp)
                                        }
                                        TextButton(
                                            onClick = { showPresetPicker = true }
                                        ) {
                                            Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Choose Avatar", fontSize = 12.sp)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it; nameError = null },
                                    label = { Text("Student Full Name *") },
                                    isError = nameError != null,
                                    supportingText = nameError?.let { { Text(it) } },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("form_name")
                                )

                                OutlinedTextField(
                                    value = mobile,
                                    onValueChange = { input ->
                                        val digitsOnly = input.filter { it.isDigit() }
                                        if (digitsOnly.length <= 10) {
                                            mobile = digitsOnly
                                            mobileError = null
                                        }
                                    },
                                    label = { Text("Mobile Number (10 Digits) *") },
                                    placeholder = { Text("e.g. 9876543210") },
                                    isError = mobileError != null,
                                    supportingText = {
                                        if (mobileError != null) {
                                            Text(mobileError!!, color = MaterialTheme.colorScheme.error)
                                        } else {
                                            Text("${mobile.length}/10 Digits", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                        }
                                    },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("form_mobile")
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it; emailError = null },
                                    label = { Text("Email ID (Credentials) *") },
                                    placeholder = { Text("e.g. student@gmail.com") },
                                    isError = emailError != null,
                                    supportingText = emailError?.let { { Text(it) } },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("form_email")
                                )

                                // Custom Colorful and Animated Gender Selector
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Select Gender *",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val genderOptions = listOf(
                                            Triple("Male", Icons.Default.Male, Color(0xFF2196F3)),
                                            Triple("Female", Icons.Default.Female, Color(0xFFE91E63)),
                                            Triple("Other", Icons.Default.Transgender, Color(0xFF9C27B0))
                                        )
                                        
                                        genderOptions.forEach { (option, icon, color) ->
                                            val isSelected = gender == option
                                            val animBgColor by animateColorAsState(
                                                targetValue = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
                                                animationSpec = tween(250), label = "gender_bg"
                                            )
                                            val animBorderColor by animateColorAsState(
                                                targetValue = if (isSelected) color else Color.Transparent,
                                                animationSpec = tween(250), label = "gender_border"
                                            )
                                            val animTextColor by animateColorAsState(
                                                targetValue = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                                animationSpec = tween(250), label = "gender_text"
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(animBgColor)
                                                    .border(1.dp, animBorderColor, RoundedCornerShape(10.dp))
                                                    .clickable { gender = option }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = option,
                                                        tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = option,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                        color = animTextColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Custom restricted Aadhaar Number OutlinedTextField
                                OutlinedTextField(
                                    value = aadhaar,
                                    onValueChange = { input ->
                                        val digitsOnly = input.filter { it.isDigit() }
                                        if (digitsOnly.length <= 12) {
                                            aadhaar = digitsOnly
                                            aadhaarError = null
                                        }
                                    },
                                    label = { Text("Aadhaar Number (12 Digits) *") },
                                    isError = aadhaarError != null,
                                    supportingText = {
                                        if (aadhaarError != null) {
                                            Text(aadhaarError!!, color = MaterialTheme.colorScheme.error)
                                        } else {
                                            Text("${aadhaar.length}/12 Digits", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                        }
                                    },
                                    singleLine = true,
                                                                    leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("form_aadhaar")
                                )

                                // Student Address OutlinedTextField
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it; addressError = null },
                                    label = { Text("Student Address *") },
                                    isError = addressError != null,
                                    supportingText = addressError?.let { { Text(it) } },
                                    maxLines = 3,
                                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("form_address")
                                )
                            }
                            1 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Select Class *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        classesList.forEach { cls ->
                                            FilterChip(
                                                selected = studentClass == cls,
                                                onClick = { studentClass = cls },
                                                label = { Text(cls, fontSize = 12.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Select Section *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        sectionsList.forEach { sec ->
                                            FilterChip(
                                                selected = section == sec,
                                                onClick = { section = sec },
                                                label = { Text("Section $sec", fontSize = 12.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = rollNumber,
                                    onValueChange = { rollNumber = it; rollError = null },
                                    label = { Text("Roll Number *") },
                                    placeholder = { Text("e.g. 15") },
                                    isError = rollError != null,
                                    supportingText = rollError?.let { { Text(it) } },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("form_roll_number")
                                )

                                OutlinedTextField(
                                    value = mainSubject,
                                    onValueChange = { mainSubject = it; subjectError = null },
                                    label = { Text("Main Subject *") },
                                    placeholder = { Text("e.g. Mathematics, Science") },
                                    isError = subjectError != null,
                                    supportingText = subjectError?.let { { Text(it) } },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("form_main_subject")
                                )
                            }
                            2 -> {
                                OutlinedTextField(
                                    value = fatherName,
                                    onValueChange = { fatherName = it },
                                    label = { Text("Father's Name (Optional)") },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("form_father_name")
                                )

                                OutlinedTextField(
                                    value = fatherMobile,
                                    onValueChange = { input ->
                                        val digitsOnly = input.filter { it.isDigit() }
                                        if (digitsOnly.length <= 10) {
                                            fatherMobile = digitsOnly
                                            fatherMobileError = null
                                        }
                                    },
                                    label = { Text("Father's Mobile Number *") },
                                    placeholder = { Text("e.g. 9876543210") },
                                    isError = fatherMobileError != null,
                                    supportingText = {
                                        if (fatherMobileError != null) {
                                            Text(fatherMobileError!!, color = MaterialTheme.colorScheme.error)
                                         } else {
                                             Text("${fatherMobile.length}/10 Digits", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                         }
                                    },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("form_father_mobile")
                                )

                                OutlinedTextField(
                                    value = motherName,
                                    onValueChange = { motherName = it },
                                    label = { Text("Mother's Name (Optional)") },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("form_mother_name")
                                )

                                if (student != null) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        Text("Profile Status:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = status == "Active", onClick = { status = "Active" }, modifier = Modifier.testTag("status_active"))
                                                Text("Active", fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = status == "Inactive", onClick = { status = "Inactive" }, modifier = Modifier.testTag("status_inactive"))
                                                Text("Inactive", fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
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
            Button(
                onClick = {
                    var hasError = false
                    if (name.isBlank()) {
                        nameError = "Full name is required"
                        activeTab = 0
                        hasError = true
                    }
                    if (mobile.isBlank()) {
                        mobileError = "Mobile number is required"
                        if (!hasError) activeTab = 0
                        hasError = true
                    } else if (mobile.length != 10) {
                        mobileError = "Mobile number must be exactly 10 digits"
                        if (!hasError) activeTab = 0
                        hasError = true
                    }
                    if (email.isBlank()) {
                        emailError = "Email ID is required"
                        if (!hasError) activeTab = 0
                        hasError = true
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailError = "Invalid email format"
                        if (!hasError) activeTab = 0
                        hasError = true
                    }
                    if (aadhaar.isBlank()) {
                        aadhaarError = "Aadhaar number is required"
                        if (!hasError) activeTab = 0
                        hasError = true
                    } else if (aadhaar.length != 12) {
                        aadhaarError = "Aadhaar number must be exactly 12 digits"
                        if (!hasError) activeTab = 0
                        hasError = true
                    } else {
                        // Uniqueness check for Aadhaar
                        val duplicateAadhaar = allStudents.find {
                            it.aadhaar.isNotBlank() &&
                            it.aadhaar == aadhaar.trim() &&
                            it.id != (student?.id ?: 0)
                        }
                        if (duplicateAadhaar != null) {
                            aadhaarError = "Student with this Aadhaar already exists"
                            if (!hasError) activeTab = 0
                            hasError = true
                        }
                    }

                    if (address.isBlank()) {
                        addressError = "Student address is required"
                        if (!hasError) activeTab = 0
                        hasError = true
                    }

                    if (rollNumber.isBlank()) {
                        rollError = "Roll number is required"
                        if (!hasError) activeTab = 1
                        hasError = true
                    } else {
                        // Uniqueness check for Roll Number in the same Class & Section
                        val duplicateRoll = allStudents.find {
                            it.studentClass == studentClass &&
                            it.section == section &&
                            it.rollNumber.isNotBlank() &&
                            it.rollNumber.trim() == rollNumber.trim() &&
                            it.id != (student?.id ?: 0)
                        }
                        if (duplicateRoll != null) {
                            rollError = "Roll number $rollNumber already assigned in $studentClass Section $section"
                            if (!hasError) activeTab = 1
                            hasError = true
                        }
                    }

                    if (mainSubject.isBlank()) {
                        subjectError = "Main subject is required"
                        if (!hasError) activeTab = 1
                        hasError = true
                    }

                    if (fatherMobile.isBlank()) {
                        fatherMobileError = "Father's mobile number is required"
                        if (!hasError) activeTab = 2
                        hasError = true
                    } else if (fatherMobile.length != 10) {
                        fatherMobileError = "Father's mobile number must be exactly 10 digits"
                        if (!hasError) activeTab = 2
                        hasError = true
                    }

                    if (!hasError) {
                        onSave(
                            name.trim(),
                            email.trim(),
                            studentClass,
                            section,
                            rollNumber.trim(),
                            mobile.trim(),
                            fatherName.trim(),
                            motherName.trim(),
                            mainSubject.trim(),
                            photoBase64,
                            status,
                            gender,
                            aadhaar.trim(),
                            address.trim(),
                            fatherMobile.trim()
                        )
                    }
                },
                modifier = Modifier.bounceClick().testTag("dialog_student_save_btn")
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.bounceClick().testTag("dialog_student_cancel_btn")) {
                Text("Cancel")
            }
        }
    )

    if (showPresetPicker) {
        AlertDialog(
            onDismissRequest = { showPresetPicker = false },
            title = { Text("Choose a Student Avatar", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select from one of our professional, pre-styled academy avatars:", fontSize = 13.sp, color = Color.Gray)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val chunks = avatarPresets.chunked(3)
                        chunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowItems.forEach { preset ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                photoBase64 = preset.key
                                                showPresetPicker = false
                                            }
                                            .padding(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(preset.startColor, preset.endColor)
                                                    )
                                                )
                                                .border(
                                                    width = if (photoBase64 == preset.key) 3.dp else 0.dp,
                                                    color = if (photoBase64 == preset.key) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = preset.icon,
                                                contentDescription = preset.label,
                                                tint = Color.White,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(preset.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// Student Avatar Presets definition for offline selection
data class AvatarPreset(
    val key: String,
    val icon: ImageVector,
    val label: String,
    val startColor: Color,
    val endColor: Color,
    val tint: Color = Color.White
)

val avatarPresets = listOf(
    AvatarPreset("preset_1", Icons.Default.School, "Topper Teal", Color(0xFF0D9488), Color(0xFF14B8A6)),
    AvatarPreset("preset_2", Icons.Default.Person, "Royal Purple", Color(0xFF6D28D9), Color(0xFF8B5CF6)),
    AvatarPreset("preset_3", Icons.Default.Book, "Warm Amber", Color(0xFFD97706), Color(0xFFF59E0B)),
    AvatarPreset("preset_4", Icons.Default.Star, "Golden Star", Color(0xFFB45309), Color(0xFFEAB308)),
    AvatarPreset("preset_5", Icons.Default.WorkspacePremium, "Elite Blue", Color(0xFF1D4ED8), Color(0xFF3B82F6)),
    AvatarPreset("preset_6", Icons.Default.Face, "Sweet Rose", Color(0xFFBE185D), Color(0xFFEC4899))
)

@Composable
fun StudentPhoto(
    photoData: String,
    modifier: Modifier = Modifier,
    placeholderIcon: ImageVector = Icons.Default.School,
    tintColor: Color = Color.Gray
) {
    val preset = avatarPresets.find { it.key == photoData }
    
    if (preset != null) {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    colors = listOf(preset.startColor, preset.endColor)
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = preset.icon,
                contentDescription = preset.label,
                tint = preset.tint,
                modifier = Modifier.fillMaxSize(0.55f)
            )
        }
    } else {
        val bitmap = remember(photoData) {
            if (photoData.isNotEmpty() && !photoData.startsWith("http") && !photoData.startsWith("content")) {
                try {
                    val cleanBase64 = if (photoData.contains(",")) {
                        photoData.substring(photoData.indexOf(",") + 1)
                    } else {
                        photoData
                    }
                    val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Student Photo",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        } else if (photoData.isNotEmpty()) {
            AsyncImage(
                model = photoData,
                contentDescription = "Student Photo",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = placeholderIcon,
                    contentDescription = "Placeholder",
                    tint = tintColor,
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }
        }
    }
}

fun uriToBase64(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFeesManagementDialog(
    student: Student,
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val allFees by viewModel.allFeesList.collectAsState()
    val studentFees = remember(allFees, student.studentId) {
        allFees.filter { it.studentId == student.studentId }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: History, 1: Add Fee
    var feeTitle by remember { mutableStateOf("") }
    var feeAmount by remember { mutableStateOf("") }
    var dueDate by remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 7)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        mutableStateOf(sdf.format(cal.time))
    }
    
    var rejectionReasonInput by remember { mutableStateOf("") }
    var rejectingFeeId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Manage Student Fees", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "${student.name} (Class ${student.studentClass})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                // Tab switcher
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Fees (${studentFees.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Issue Fee") }
                    )
                }

                if (selectedTab == 0) {
                    if (studentFees.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No fee demands issued.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            studentFees.forEach { fee ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
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
                                                Text(
                                                    text = fee.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Due: ${fee.dueDate}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Text(
                                                text = "₹${fee.amount}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Status badge
                                        val statusColor = when (fee.status) {
                                            "Paid" -> Color(0xFF10B981) // Green
                                            "Pending" -> Color(0xFFF59E0B) // Amber
                                            "Rejected" -> Color(0xFFEF4444) // Red
                                            else -> MaterialTheme.colorScheme.error // Unpaid
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = statusColor.copy(alpha = 0.15f),
                                                contentColor = statusColor,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = fee.status.uppercase(),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteFeeRecord(fee) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Fee Demand",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (fee.status == "Rejected" && fee.rejectionReason.isNotEmpty()) {
                                            Text(
                                                text = "Reason: ${fee.rejectionReason}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }

                                        if (fee.status == "Pending") {
                                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                            
                                            Text(text = "Payment details:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "Txn ID: ${fee.transactionId}", fontSize = 11.sp)
                                            Text(text = "Paid to UPI: ${fee.upiUsed}", fontSize = 11.sp)
                                            Text(text = "Paid Date: ${fee.paymentDate}", fontSize = 11.sp)

                                            if (fee.screenshotUrl.isNotEmpty()) {
                                                Text(text = "Payment Receipt:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(150.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.Black),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AsyncImage(
                                                        model = fee.screenshotUrl,
                                                        contentDescription = "Payment Receipt screenshot",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            }

                                            if (rejectingFeeId == fee.feeId) {
                                                OutlinedTextField(
                                                    value = rejectionReasonInput,
                                                    onValueChange = { rejectionReasonInput = it },
                                                    label = { Text("Rejection Reason") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.verifyFeePayment(fee.feeId, false, rejectionReasonInput)
                                                            rejectingFeeId = null
                                                            rejectionReasonInput = ""
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("Submit", fontSize = 11.sp)
                                                    }
                                                    OutlinedButton(
                                                        onClick = { rejectingFeeId = null },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("Cancel", fontSize = 11.sp)
                                                    }
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = { viewModel.verifyFeePayment(fee.feeId, true) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                        modifier = Modifier.weight(1.2f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Approve", fontSize = 11.sp)
                                                    }
                                                    OutlinedButton(
                                                        onClick = { rejectingFeeId = fee.feeId },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                        modifier = Modifier.weight(0.8f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Reject", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Add Fee form
                    OutlinedTextField(
                        value = feeTitle,
                        onValueChange = { feeTitle = it },
                        label = { Text("Fee Title (e.g. July 2026 Tuition)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = feeAmount,
                        onValueChange = { feeAmount = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            val amt = feeAmount.toDoubleOrNull() ?: 0.0
                            if (feeTitle.isNotEmpty() && amt > 0.0) {
                                viewModel.addFeeForStudent(
                                    studentId = student.studentId,
                                    studentName = student.name,
                                    title = feeTitle,
                                    amount = amt,
                                    dueDate = dueDate
                                )
                                feeTitle = ""
                                feeAmount = ""
                                selectedTab = 0 // Go back to history list
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Issue Fee")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Issue Fee Demand", fontWeight = FontWeight.Bold)
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

