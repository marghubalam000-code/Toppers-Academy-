package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.components.WelcomeDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.animation.*
import androidx.compose.animation.core.*

class MainActivity : ComponentActivity() {
    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Gracefully request notification permissions for Android 13+ to support parent push alerts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        // Schedule daily summaries for teachers on startup
        com.example.util.DailySummaryReceiver.scheduleDailySummary(this)

        // Fetch latest data from Supabase on startup if configured
        if (viewModel.isSupabaseAvailable()) {
            viewModel.fetchFromSupabase()
        }

        setContent {
            val themePref by viewModel.themePreference.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themePref) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            MyApplicationTheme(darkTheme = useDarkTheme) {
                val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
                val isStudentLoggedIn by viewModel.isStudentLoggedIn.collectAsState()
                val welcomeUser by viewModel.welcomeUser.collectAsState()
                
                if (isAdminLoggedIn) {
                    MainAppLayout(viewModel = viewModel)
                } else if (isStudentLoggedIn) {
                    StudentDashboardScreen(viewModel = viewModel)
                } else {
                    LoginScreen(viewModel = viewModel)
                }

                welcomeUser?.let { name ->
                    WelcomeDialog(
                        userName = name,
                        onDismiss = { viewModel.dismissWelcome() }
                    )
                }
            }
        }
    }
}

sealed class TabScreen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : TabScreen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Attendance : TabScreen("attendance", "Record Attendance", Icons.Default.EditCalendar)
    object Students : TabScreen("students", "Student Directory", Icons.Default.People)
    object HistoryReports : TabScreen("history_reports", "History & Reports", Icons.Default.Analytics)
    object Sync : TabScreen("sync", "Cloud Sync", Icons.Default.CloudSync)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: AttendanceViewModel) {
    val appLogoUri by viewModel.appLogoUri.collectAsState()
    var selectedTab by remember { mutableStateOf<TabScreen>(TabScreen.Dashboard) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 720

    val tabs = listOf(
        TabScreen.Dashboard,
        TabScreen.Attendance,
        TabScreen.Students,
        TabScreen.HistoryReports,
        TabScreen.Sync
    )

    val drawerContentComposable: @Composable ColumnScope.() -> Unit = {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                text = "Toppers Academy",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "Attendance Management System",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)
        )
        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        tabs.forEach { tab ->
            NavigationDrawerItem(
                icon = { Icon(tab.icon, contentDescription = tab.title) },
                label = { Text(tab.title, fontWeight = FontWeight.Bold) },
                selected = selectedTab == tab,
                onClick = {
                    selectedTab = tab
                    if (!isWideScreen) {
                        coroutineScope.launch { drawerState.close() }
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
        
        // Sign Out Link inside drawer
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.error) },
            label = { Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
            selected = false,
            onClick = {
                if (!isWideScreen) {
                    coroutineScope.launch { drawerState.close() }
                }
                showLogoutConfirmDialog = true
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        
        // Designer Footer Credit in Drawer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            MarghubSignatureBadge(
                onClick = { showDeveloperDialog = true }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    val mainContentComposable = @Composable {
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
                                text = selectedTab.title,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    navigationIcon = {
                        if (!isWideScreen) {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Open Sidebar Menu")
                            }
                        }
                    },
                    actions = {
                        val themePref by viewModel.themePreference.collectAsState()
                        val themeIcon = when (themePref) {
                            "light" -> Icons.Default.LightMode
                            "dark" -> Icons.Default.DarkMode
                            else -> Icons.Default.Settings
                        }
                        
                        IconButton(
                            onClick = { showThemeDialog = true },
                            modifier = Modifier.testTag("theme_toggle_btn")
                        ) {
                            Icon(
                                imageVector = themeIcon,
                                contentDescription = "Change Theme"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                            slideInHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                initialOffsetX = { width -> width / 12 }
                            )).togetherWith(
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                                slideOutHorizontally(
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    targetOffsetX = { width -> -width / 12 }
                                )
                    )
                },
                label = "TabTransition"
            ) { targetTab ->
                val modifier = Modifier.padding(innerPadding)
                when (targetTab) {
                    TabScreen.Dashboard -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAttendance = { selectedTab = TabScreen.Attendance },
                        modifier = modifier
                    )
                    TabScreen.Attendance -> AttendanceScreen(
                        viewModel = viewModel,
                        modifier = modifier
                    )
                    TabScreen.Students -> StudentsScreen(
                        viewModel = viewModel,
                        modifier = modifier
                    )
                    TabScreen.HistoryReports -> ReportsAndHistoryScreen(
                        viewModel = viewModel,
                        modifier = modifier
                    )
                    TabScreen.Sync -> SyncScreen(
                        viewModel = viewModel,
                        modifier = modifier
                    )
                }
            }
        }
    }

    if (isWideScreen) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(modifier = Modifier.width(280.dp)) {
                    drawerContentComposable()
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            mainContentComposable()
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    drawerContentComposable()
                }
            }
        ) {
            mainContentComposable()
        }
    }

    if (showThemeDialog) {
        ThemeSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showThemeDialog = false }
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

    if (showDeveloperDialog) {
        MarghubProfileDialog(
            onDismiss = { showDeveloperDialog = false }
        )
    }
}
