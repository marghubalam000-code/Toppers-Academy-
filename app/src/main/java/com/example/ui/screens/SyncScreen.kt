package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SyncState
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.UiState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appLogoUri by viewModel.appLogoUri.collectAsState()
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            coroutineScope.launch {
                try {
                    val uploadedUrl = viewModel.uploadAppLogo(uri) { progress ->
                        uploadProgress = progress
                    }
                    if (uploadedUrl != null) {
                        viewModel.setAppLogoUri(uploadedUrl)
                        Toast.makeText(context, "Logo uploaded and updated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Fallback: save local Uri if cloud upload is simulated/failed
                        viewModel.setAppLogoUri(uri.toString())
                        Toast.makeText(context, "Logo applied locally!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Fallback to local URI
                    viewModel.setAppLogoUri(uri.toString())
                    Toast.makeText(context, "Logo applied locally (offline mode)!", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploading = false
                    uploadProgress = 0f
                }
            }
        }
    }

    val marghubLogoUri by viewModel.marghubLogoUri.collectAsState()
    val adminUsername by viewModel.adminUsername.collectAsState()

    val marghubLogoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setMarghubLogoUri(uri.toString())
            Toast.makeText(context, "Developer Profile Photo updated successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    val syncState by viewModel.syncState.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isCloudLinked = viewModel.isCloudAvailable()

    var showHelpInfo by remember { mutableStateOf(false) }
    var newEmailInput by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    var showErrorDialog by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            showErrorDialog = (uiState as UiState.Error).message
            viewModel.clearUiState()
        } else if (uiState is UiState.Success) {
            showSuccessDialog = true
            viewModel.clearUiState()
        }
    }

    if (uiState is UiState.Saving) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Synchronizing", fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Fetching student roster and attendance records from Supabase...", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {}
        )
    }

    showErrorDialog?.let { errMsg ->
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)) },
            title = { Text("Sync Failed", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(errMsg, style = MaterialTheme.typography.bodyMedium)
                    if (errMsg.contains("table", ignoreCase = true) || errMsg.contains("found", ignoreCase = true) || errMsg.contains("PGRST205", ignoreCase = true)) {
                        Text(
                            "This usually happens because the required database tables have not been created in your Supabase project yet. You can copy the SQL Setup script below and run it in the Supabase SQL Editor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (errMsg.contains("table", ignoreCase = true) || errMsg.contains("found", ignoreCase = true) || errMsg.contains("PGRST205", ignoreCase = true)) {
                    Button(
                        onClick = {
                            val sqlScript = """
                                -- Toppers Academy Supabase Database Tables Setup
                                -- Copy and run this script in your Supabase SQL Editor

                                -- 1. Create students table
                                CREATE TABLE IF NOT EXISTS public.students (
                                    student_id TEXT PRIMARY KEY,
                                    name TEXT NOT NULL,
                                    father_name TEXT,
                                    mother_name TEXT,
                                    student_class TEXT,
                                    section TEXT,
                                    roll_number TEXT,
                                    mobile TEXT,
                                    email TEXT,
                                    gender TEXT,
                                    dob TEXT,
                                    address TEXT,
                                    aadhaar TEXT,
                                    admission_date TEXT,
                                    photo TEXT,
                                    status TEXT DEFAULT 'Active',
                                    password TEXT,
                                    main_subject TEXT
                                );

                                -- Enable RLS and public access policies
                                ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;
                                CREATE POLICY "Allow public select" ON public.students FOR SELECT USING (true);
                                CREATE POLICY "Allow public insert" ON public.students FOR INSERT WITH CHECK (true);
                                CREATE POLICY "Allow public update" ON public.students FOR UPDATE USING (true);
                                CREATE POLICY "Allow public delete" ON public.students FOR DELETE USING (true);

                                -- 2. Create attendance table
                                CREATE TABLE IF NOT EXISTS public.attendance (
                                    attendance_id TEXT PRIMARY KEY,
                                    student_id TEXT REFERENCES public.students(student_id) ON DELETE CASCADE,
                                    student_name TEXT,
                                    student_class TEXT,
                                    section TEXT,
                                    roll_number TEXT,
                                    date TEXT NOT NULL,
                                    status TEXT NOT NULL,
                                    created_at BIGINT
                                );

                                ALTER TABLE public.attendance ENABLE ROW LEVEL SECURITY;
                                CREATE POLICY "Allow public select" ON public.attendance FOR SELECT USING (true);
                                CREATE POLICY "Allow public insert" ON public.attendance FOR INSERT WITH CHECK (true);
                                CREATE POLICY "Allow public update" ON public.attendance FOR UPDATE USING (true);
                                CREATE POLICY "Allow public delete" ON public.attendance FOR DELETE USING (true);

                                -- 3. Create allowed_users table (For Whitelisted Admin Emails)
                                CREATE TABLE IF NOT EXISTS public.allowed_users (
                                    email TEXT PRIMARY KEY
                                );

                                ALTER TABLE public.allowed_users ENABLE ROW LEVEL SECURITY;
                                CREATE POLICY "Allow public select" ON public.allowed_users FOR SELECT USING (true);
                                CREATE POLICY "Allow public insert" ON public.allowed_users FOR INSERT WITH CHECK (true);
                                CREATE POLICY "Allow public update" ON public.allowed_users FOR UPDATE USING (true);
                                CREATE POLICY "Allow public delete" ON public.allowed_users FOR DELETE USING (true);

                                -- Insert Primary Administrator Email
                                INSERT INTO public.allowed_users (email) 
                                VALUES ('marghubalam000@gmail.com') 
                                ON CONFLICT (email) DO NOTHING;
                            """.trimIndent()

                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Supabase SQL Schema", sqlScript)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "SQL Script copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showErrorDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy SQL & Close")
                    }
                } else {
                    TextButton(onClick = { showErrorDialog = null }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (errMsg.contains("table", ignoreCase = true) || errMsg.contains("found", ignoreCase = true) || errMsg.contains("PGRST205", ignoreCase = true)) {
                    TextButton(onClick = { showErrorDialog = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF10B981), modifier = Modifier.size(44.dp)) },
            title = { Text("Sync Successful", fontWeight = FontWeight.Bold) },
            text = { Text("Database synchronized perfectly with your remote Supabase instance!") },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }) {
                    Text("Awesome")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Sync Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Connection Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(
                            if (isCloudLinked) {
                                Color(0xFFDCFCE7)
                            } else {
                                Color(0xFFEFF6FF)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCloudLinked) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                        contentDescription = "Sync Cloud Status",
                        tint = if (isCloudLinked) Color(0xFF10B981) else Color(0xFF3B82F6),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Firestore Cloud Synchronization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isCloudLinked) {
                            "Status: Connected to Live Firebase Firestore"
                        } else {
                            "Status: Simulation / Offline-First Room Mode"
                        },
                        fontSize = 13.sp,
                        color = if (isCloudLinked) Color(0xFF10B981) else Color(0xFF6B7280),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                // Status parameters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Unsynced Local Records",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$unsyncedCount records waiting",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (unsyncedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Primary Persistence",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Room SQLite Database",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Force Sync Button
                Button(
                    onClick = {
                        viewModel.triggerSync()
                        Toast.makeText(context, "Cloud sync triggered successfully", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("force_sync_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync Icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Cloud Sync", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Supabase Database Integration Card
        val isSupabaseLinked = viewModel.isSupabaseAvailable()
        var isEditingSupabase by remember { mutableStateOf(false) }
        var supabaseUrlInput by remember { mutableStateOf(viewModel.getSupabaseUrl()) }
        var supabaseKeyInput by remember { mutableStateOf(viewModel.getSupabaseKey()) }
        var googleClientIdInput by remember { mutableStateOf(viewModel.getGoogleWebClientId()) }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("supabase_sync_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isSupabaseLinked) Color(0xFFD1FAE5) else Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Supabase Status",
                            tint = if (isSupabaseLinked) Color(0xFF059669) else Color(0xFF9CA3AF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Supabase DB & Google OAuth",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isSupabaseLinked) "Status: Connected to Supabase REST & Auth" else "Status: Simulation & Local Cache Backup",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSupabaseLinked) Color(0xFF059669) else Color(0xFF6B7280)
                        )
                    }
                }

                Text(
                    text = "Sync your student roster, whitelists, and attendance logs. Integrates Google OAuth via Supabase authentication endpoints.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                if (isEditingSupabase) {
                    // Credentials Form
                    OutlinedTextField(
                        value = supabaseUrlInput,
                        onValueChange = { supabaseUrlInput = it },
                        label = { Text("Supabase Project REST URL") },
                        placeholder = { Text("https://your-project.supabase.co") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = supabaseKeyInput,
                        onValueChange = { supabaseKeyInput = it },
                        label = { Text("Supabase Service / Anon API Key") },
                        placeholder = { Text("your-supabase-anon-key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = googleClientIdInput,
                        onValueChange = { googleClientIdInput = it },
                        label = { Text("Google Web Client ID (Optional)") },
                        placeholder = { Text("xxxxxx-xxxxxx.apps.googleusercontent.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Brief Instructions
                    Text(
                        text = "To configure Google OAuth: Enable Google Provider in Supabase Dashboard -> Authentication -> Providers. Paste your Web Client ID here and in Supabase configuration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isEditingSupabase = false
                                supabaseUrlInput = viewModel.getSupabaseUrl()
                                supabaseKeyInput = viewModel.getSupabaseKey()
                                googleClientIdInput = viewModel.getGoogleWebClientId()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (supabaseUrlInput.isNotBlank() && supabaseKeyInput.isNotBlank()) {
                                    viewModel.saveSupabaseCredentials(supabaseUrlInput, supabaseKeyInput)
                                    viewModel.saveGoogleWebClientId(googleClientIdInput)
                                    isEditingSupabase = false
                                    Toast.makeText(context, "Supabase credentials & Google OAuth Config updated!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please fill out all Supabase credentials", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save & Connect")
                        }
                    }
                } else {
                    // Active Display Panel
                    if (isSupabaseLinked) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Active endpoint:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = supabaseUrlInput,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (googleClientIdInput.isNotBlank()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Google Client ID:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = googleClientIdInput,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isSupabaseLinked) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearSupabaseCredentials()
                                        viewModel.saveGoogleWebClientId("")
                                        supabaseUrlInput = ""
                                        supabaseKeyInput = ""
                                        googleClientIdInput = ""
                                        Toast.makeText(context, "Supabase connection deactivated", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear Credentials")
                                }
                            }

                            Button(
                                onClick = { isEditingSupabase = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSupabaseLinked) MaterialTheme.colorScheme.secondary else Color(0xFF059669)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSupabaseLinked) "Reconfigure" else "Configure Supabase")
                            }
                        }

                        if (isSupabaseLinked) {
                            Button(
                                onClick = {
                                    viewModel.fetchFromSupabase()
                                    Toast.makeText(context, "Fetching student roster and attendance logs from Supabase...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B) // Slate 800
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("fetch_supabase_button")
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = "Sync Fetch", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetch & Sync from Supabase", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                // Permanently Visible SQL Setup Guide for Supabase Schema Configuration
                var showSupabaseSqlHelp by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showSupabaseSqlHelp = !showSupabaseSqlHelp },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_sql_help_btn")
                ) {
                    Icon(Icons.Default.Code, contentDescription = "SQL Schema Setup Code", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showSupabaseSqlHelp) "Hide SQL Setup Guide" else "Show SQL Setup Guide")
                }

                AnimatedVisibility(visible = showSupabaseSqlHelp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "To successfully fetch or sync data, your Supabase project must contain the correct schema and tables. Click below to copy the required SQL setup script, then run it in your Supabase SQL Editor:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = {
                                val sqlScript = """
                                    -- Toppers Academy Supabase Database Tables Setup
                                    -- Copy and run this script in your Supabase SQL Editor

                                    -- 1. Create students table
                                    CREATE TABLE IF NOT EXISTS public.students (
                                        student_id TEXT PRIMARY KEY,
                                        name TEXT NOT NULL,
                                        father_name TEXT,
                                        mother_name TEXT,
                                        student_class TEXT,
                                        section TEXT,
                                        roll_number TEXT,
                                        mobile TEXT,
                                        email TEXT,
                                        gender TEXT,
                                        dob TEXT,
                                        address TEXT,
                                        aadhaar TEXT,
                                        admission_date TEXT,
                                        photo TEXT,
                                        status TEXT DEFAULT 'Active',
                                        password TEXT,
                                        main_subject TEXT
                                    );

                                    -- Enable RLS and public access policies
                                    ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;
                                    CREATE POLICY "Allow public select" ON public.students FOR SELECT USING (true);
                                    CREATE POLICY "Allow public insert" ON public.students FOR INSERT WITH CHECK (true);
                                    CREATE POLICY "Allow public update" ON public.students FOR UPDATE USING (true);
                                    CREATE POLICY "Allow public delete" ON public.students FOR DELETE USING (true);

                                    -- 2. Create attendance table
                                    CREATE TABLE IF NOT EXISTS public.attendance (
                                        attendance_id TEXT PRIMARY KEY,
                                        student_id TEXT REFERENCES public.students(student_id) ON DELETE CASCADE,
                                        student_name TEXT,
                                        student_class TEXT,
                                        section TEXT,
                                        roll_number TEXT,
                                        date TEXT NOT NULL,
                                        status TEXT NOT NULL,
                                        created_at BIGINT
                                    );

                                    ALTER TABLE public.attendance ENABLE ROW LEVEL SECURITY;
                                    CREATE POLICY "Allow public select" ON public.attendance FOR SELECT USING (true);
                                    CREATE POLICY "Allow public insert" ON public.attendance FOR INSERT WITH CHECK (true);
                                    CREATE POLICY "Allow public update" ON public.attendance FOR UPDATE USING (true);
                                    CREATE POLICY "Allow public delete" ON public.attendance FOR DELETE USING (true);

                                    -- 3. Create allowed_users table (For Whitelisted Admin Emails)
                                    CREATE TABLE IF NOT EXISTS public.allowed_users (
                                        email TEXT PRIMARY KEY
                                    );

                                    ALTER TABLE public.allowed_users ENABLE ROW LEVEL SECURITY;
                                    CREATE POLICY "Allow public select" ON public.allowed_users FOR SELECT USING (true);
                                    CREATE POLICY "Allow public insert" ON public.allowed_users FOR INSERT WITH CHECK (true);
                                    CREATE POLICY "Allow public update" ON public.allowed_users FOR UPDATE USING (true);
                                    CREATE POLICY "Allow public delete" ON public.allowed_users FOR DELETE USING (true);

                                    -- Insert Primary Administrator Email
                                    INSERT INTO public.allowed_users (email) 
                                    VALUES ('marghubalam000@gmail.com') 
                                    ON CONFLICT (email) DO NOTHING;
                                """.trimIndent()

                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Supabase SQL Schema", sqlScript)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "SQL Script copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("copy_supabase_sql_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669) // Supabase Green
                            )
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy SQL Script", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Authorized Email Directory Management Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("allowed_emails_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Allowed Emails",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Authorized Email Directory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "Specify which email addresses are permitted to log into Toppers Academy. Unlisted accounts will be blocked during authentication.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Add New Email Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newEmailInput,
                            onValueChange = { 
                                newEmailInput = it
                                emailError = null
                            },
                            placeholder = { Text("e.g. teacher@toppers.com") },
                            singleLine = true,
                            isError = emailError != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                            )
                        )

                        Button(
                            onClick = {
                                val emailTrimmed = newEmailInput.trim().lowercase()
                                if (emailTrimmed.isEmpty()) {
                                    emailError = "Email cannot be empty"
                                } else if (!viewModel.isValidEmail(emailTrimmed)) {
                                    emailError = "Invalid email format"
                                } else if (viewModel.isAuthorizedAdmin(emailTrimmed)) {
                                    emailError = "Already authorized"
                                } else {
                                    viewModel.addAllowedEmail(emailTrimmed)
                                    newEmailInput = ""
                                    emailError = null
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Email")
                        }
                    }
                    if (emailError != null) {
                        Text(
                            text = emailError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                // Allowed Emails List
                Text(
                    text = "Currently Authorized Accounts",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val allowedEmails by viewModel.allowedEmails.collectAsState()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allowedEmails.forEach { email ->
                        val isProtected = email == "marghubalam000@gmail.com"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isProtected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = email,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isProtected) {
                                    Text(
                                        text = "Primary Admin",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFD1FAE5))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (!isProtected) {
                                IconButton(
                                    onClick = { viewModel.removeAllowedEmail(email) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Authorization",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SMTP Configuration Card
        var smtpHostInput by remember { mutableStateOf(viewModel.smtpHost.value) }
        var smtpPortInput by remember { mutableStateOf(viewModel.smtpPort.value.toString()) }
        var smtpUserInput by remember { mutableStateOf(viewModel.smtpUsername.value) }
        var smtpPassInput by remember { mutableStateOf(viewModel.smtpPassword.value) }
        var smtpSenderInput by remember { mutableStateOf(viewModel.smtpSenderName.value) }
        var smtpEnabledState by remember { mutableStateOf(viewModel.smtpEnabled.value) }
        var isTestingSmtp by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("smtp_config_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "SMTP Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Admin OTP Email SMTP Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Switch(
                        checked = smtpEnabledState,
                        onCheckedChange = { 
                            smtpEnabledState = it
                            viewModel.saveSmtpSettings(
                                smtpHostInput,
                                smtpPortInput.toIntOrNull() ?: 465,
                                smtpUserInput,
                                smtpPassInput,
                                smtpSenderInput,
                                it
                            )
                            Toast.makeText(context, if (it) "SMTP OTP enabled" else "SMTP OTP disabled (local fallback active)", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Text(
                    text = "Configure your institutional SMTP server to dispatch authentic verification OTP codes to admin/principal emails during portal login.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = smtpHostInput,
                    onValueChange = { smtpHostInput = it },
                    label = { Text("SMTP Host") },
                    placeholder = { Text("e.g. smtp.gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                val isSmtpTypo = smtpHostInput.contains("smpt", ignoreCase = true) || smtpHostInput.contains("stmp", ignoreCase = true)
                if (isSmtpTypo) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Typo Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Host typo detected! Use 'smtp' instead of 'smpt' or 'stmp'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "An incorrect host like 'smpt.gmail.com' causes the 'No address associated with host name' network error.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        TextButton(
                            onClick = {
                                smtpHostInput = smtpHostInput
                                    .replace("smpt", "smtp", ignoreCase = true)
                                    .replace("stmp", "smtp", ignoreCase = true)
                            }
                        ) {
                            Text("Fix", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = smtpPortInput,
                        onValueChange = { smtpPortInput = it },
                        label = { Text("Port") },
                        placeholder = { Text("e.g. 465 or 587") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )

                    OutlinedTextField(
                        value = smtpSenderInput,
                        onValueChange = { smtpSenderInput = it },
                        label = { Text("Sender Name") },
                        placeholder = { Text("Toppers Academy") },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = smtpUserInput,
                    onValueChange = { smtpUserInput = it },
                    label = { Text("SMTP Username") },
                    placeholder = { Text("e.g. portal@school.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = smtpPassInput,
                    onValueChange = { smtpPassInput = it },
                    label = { Text("SMTP Password / App Password") },
                    placeholder = { Text("App password or secret key") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val port = smtpPortInput.toIntOrNull() ?: 465
                            viewModel.saveSmtpSettings(
                                smtpHostInput,
                                port,
                                smtpUserInput,
                                smtpPassInput,
                                smtpSenderInput,
                                smtpEnabledState
                            )
                            smtpHostInput = viewModel.smtpHost.value
                            Toast.makeText(context, "SMTP configurations saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Setup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val port = smtpPortInput.toIntOrNull() ?: 465
                            if (smtpHostInput.isBlank() || smtpUserInput.isBlank() || smtpPassInput.isBlank()) {
                                Toast.makeText(context, "Please configure Host, Username, and Password first", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            
                            isTestingSmtp = true
                            viewModel.saveSmtpSettings(
                                smtpHostInput,
                                port,
                                smtpUserInput,
                                smtpPassInput,
                                smtpSenderInput,
                                smtpEnabledState
                            )
                            smtpHostInput = viewModel.smtpHost.value

                            viewModel.sendSmtpOtp(smtpUserInput, "123456") { result ->
                                isTestingSmtp = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Test email dispatched successfully! Check $smtpUserInput", Toast.LENGTH_LONG).show()
                                } else {
                                    val err = result.exceptionOrNull()?.message ?: "Unknown Error"
                                    Toast.makeText(context, "Test Failed: $err", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isTestingSmtp,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        if (isTestingSmtp) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test SMTP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // App Logo Management Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("logo_management_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Logo Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "School Branding & Logo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "Upload your custom school logo. The uploaded logo will immediately replace the default system icon across the application (on the Login Screen, Sidebar Drawer Header, and reports).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Logo Preview
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (appLogoUri.isNotEmpty()) {
                        AsyncImage(
                            model = appLogoUri,
                            contentDescription = "App Logo Preview",
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

                if (isUploading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Text(
                            text = "Uploading... ${(uploadProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { logoPickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).height(44.dp).testTag("upload_logo_btn"),
                        enabled = !isUploading,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Upload"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Logo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (appLogoUri.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { viewModel.resetAppLogoUri() },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("reset_logo_btn"),
                            enabled = !isUploading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Reset"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Default", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // UPI & Payment Configuration Card
        val paymentUpiId by viewModel.upiId.collectAsState()
        val paymentUpiName by viewModel.upiName.collectAsState()
        val paymentUpiQrUrl by viewModel.upiQrUrl.collectAsState()

        var upiIdInput by remember(paymentUpiId) { mutableStateOf(paymentUpiId) }
        var upiNameInput by remember(paymentUpiName) { mutableStateOf(paymentUpiName) }
        var isUploadingQr by remember { mutableStateOf(false) }
        var qrUploadProgress by remember { mutableStateOf(0f) }

        val qrPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                isUploadingQr = true
                coroutineScope.launch {
                    try {
                        val uploadedUrl = viewModel.uploadAppLogo(uri) { progress ->
                            qrUploadProgress = progress
                        }
                        if (uploadedUrl != null) {
                            viewModel.updatePaymentConfig(upiIdInput, upiNameInput, uploadedUrl)
                            Toast.makeText(context, "QR Scanner uploaded and updated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updatePaymentConfig(upiIdInput, upiNameInput, uri.toString())
                            Toast.makeText(context, "QR Scanner applied locally!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        viewModel.updatePaymentConfig(upiIdInput, upiNameInput, uri.toString())
                        Toast.makeText(context, "QR Scanner applied locally (offline)!", Toast.LENGTH_SHORT).show()
                    } finally {
                        isUploadingQr = false
                        qrUploadProgress = 0f
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("payment_settings_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Payment Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "UPI & QR Scanner Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "Configure the UPI ID and upload the payment QR Code Scanner that students will see on their dashboard to pay fees.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = upiIdInput,
                    onValueChange = { upiIdInput = it },
                    label = { Text("UPI ID (e.g. school@upi)") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_upi_id_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = upiNameInput,
                    onValueChange = { upiNameInput = it },
                    label = { Text("Payee Name (e.g. Toppers Academy)") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_upi_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "QR Scanner Image Preview:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (paymentUpiQrUrl.isNotEmpty()) {
                        AsyncImage(
                            model = paymentUpiQrUrl,
                            contentDescription = "QR Scanner Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "No QR Code",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No custom QR uploaded",
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (isUploadingQr) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { qrUploadProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Text(
                            text = "Uploading QR... ${(qrUploadProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { qrPickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1.5f).height(44.dp).testTag("upload_qr_btn"),
                        enabled = !isUploadingQr,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Upload QR"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload QR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.updatePaymentConfig(upiIdInput, upiNameInput, paymentUpiQrUrl)
                            Toast.makeText(context, "Payment settings saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(44.dp).testTag("save_payment_settings_btn"),
                        enabled = !isUploadingQr,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save settings"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        if (adminUsername == "marghubalam000@gmail.com") {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("developer_logo_management_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Developer Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Developer Profile Photo Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = "Update the profile photo/logo shown in the \"Design by Marghubur Rahman\" profile dialog. This setting is strictly restricted and is only visible when logged in as marghubalam000@gmail.com.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Logo Preview
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (marghubLogoUri.isNotEmpty()) {
                            AsyncImage(
                                model = marghubLogoUri,
                                contentDescription = "Developer Logo Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_icon_foreground),
                                contentDescription = "Default Developer Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { marghubLogoPickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("upload_marghub_logo_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "Upload"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Photo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (marghubLogoUri.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.resetMarghubLogoUri() },
                                modifier = Modifier.weight(1f).height(44.dp).testTag("reset_marghub_logo_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Reset"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Default", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Daily Automated Summaries Card
        val summaryPrefs = remember { context.getSharedPreferences("toppers_summary_prefs", android.content.Context.MODE_PRIVATE) }
        var isSummaryEnabled by remember { mutableStateOf(summaryPrefs.getBoolean("daily_summary_enabled", true)) }
        var summaryHourInput by remember { mutableStateOf(summaryPrefs.getInt("daily_summary_hour", 16).toString()) }
        var summaryMinuteInput by remember { mutableStateOf(summaryPrefs.getInt("daily_summary_minute", 0).toString()) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_summaries_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isSummaryEnabled) MaterialTheme.colorScheme.tertiaryContainer else Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Daily Summaries",
                            tint = if (isSummaryEnabled) MaterialTheme.colorScheme.onTertiaryContainer else Color(0xFF9CA3AF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Automated Summaries",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isSummaryEnabled) "Scheduled Daily at ${summaryHourInput.padStart(2, '0')}:${summaryMinuteInput.padStart(2, '0')}" else "Daily Summary: Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSummaryEnabled) MaterialTheme.colorScheme.primary else Color(0xFF6B7280)
                        )
                    }
                }

                Text(
                    text = "Automatically compiles and delivers a secure push notification with total registered and present student counts directly to administrative officers daily.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                // Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Enable Scheduled Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Toggle automatic background alerts",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Switch(
                        checked = isSummaryEnabled,
                        onCheckedChange = { isChecked ->
                            isSummaryEnabled = isChecked
                            summaryPrefs.edit().putBoolean("daily_summary_enabled", isChecked).apply()
                            if (isChecked) {
                                com.example.util.DailySummaryReceiver.scheduleDailySummary(context)
                                Toast.makeText(context, "Daily summaries scheduled successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                com.example.util.DailySummaryReceiver.cancelDailySummary(context)
                                Toast.makeText(context, "Daily summaries deactivated.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                if (isSummaryEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = summaryHourInput,
                            onValueChange = { input ->
                                if (input.isEmpty() || (input.toIntOrNull() != null && input.toInt() in 0..23)) {
                                    summaryHourInput = input
                                }
                            },
                            label = { Text("Hour (0-23)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = summaryMinuteInput,
                            onValueChange = { input ->
                                if (input.isEmpty() || (input.toIntOrNull() != null && input.toInt() in 0..59)) {
                                    summaryMinuteInput = input
                                }
                            },
                            label = { Text("Minute (0-59)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val hr = summaryHourInput.toIntOrNull() ?: 16
                                val min = summaryMinuteInput.toIntOrNull() ?: 0
                                summaryPrefs.edit()
                                    .putInt("daily_summary_hour", hr)
                                    .putInt("daily_summary_minute", min)
                                    .apply()
                                com.example.util.DailySummaryReceiver.scheduleDailySummary(context)
                                Toast.makeText(context, "Schedule updated to ${hr.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')}!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Schedule", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                                        val db = com.example.data.local.AppDatabase.getDatabase(context)
                                        db.attendanceDao().getAttendanceForDate(todayDate).take(1).collect { records ->
                                            val totalRecords = records.size
                                            val presentCount = records.count { it.status.equals("present", ignoreCase = true) }

                                            val notificationHelper = com.example.util.NotificationHelper(context)
                                            notificationHelper.sendDailySummaryNotification(presentCount, totalRecords, todayDate)
                                            
                                            Toast.makeText(context, "Instant Summary Alert Sent!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error triggering alert: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Test", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trigger Test Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Parent Alerts Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alerts Guide",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "How Parent Notifications Work",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "When attendance is recorded, Toppers Academy matches student rosters. Parents receive real-time alert logs on their lock screen if the student is absent, utilizing local background alert channels.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }

        // Firebase Integration Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHelpInfo = !showHelpInfo },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Code",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Admin Integration Guide",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Icon(
                        imageVector = if (showHelpInfo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand"
                    )
                }

                AnimatedVisibility(visible = showHelpInfo) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "To sync with your school's live Firestore database, configure your custom GCP Project values:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Text(
                            text = "1. Download google-services.json from Firebase Console.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "2. Copy google-services.json into your project's /app directory.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "3. Enable Firestore Database inside your Firebase Console, creating a collection named 'attendance_records'.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Sample Firestore Schema Mock
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = """
                                    // Firestore Document Structure:
                                    // Collection: attendance_records/
                                    {
                                      "studentId": 12,
                                      "studentName": "John Doe",
                                      "rollNumber": "104",
                                      "studentClass": "Grade 10-A",
                                      "isPresent": false,
                                      "timestamp": 1782875534824,
                                      "schoolName": "Toppers Academy"
                                    }
                                """.trimIndent(),
                                color = Color(0xFFF1F5F9),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
