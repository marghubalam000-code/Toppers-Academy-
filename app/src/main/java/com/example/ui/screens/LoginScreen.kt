package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.AuthState
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import com.example.ui.theme.*
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke

@Composable
fun rememberButtonPressScale(interactionSource: MutableInteractionSource): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ButtonPressScale"
    )
}

private fun isValidEmail(email: String): Boolean {
    val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
    return email.matches(emailPattern.toRegex())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("marghubalam000@gmail.com") }
    var password by remember { mutableStateOf("password123") }
    var passwordVisible by remember { mutableStateOf(false) }

    // OTP Login Verification States
    var isOtpSent by remember { mutableStateOf(false) }
    var isOtpSending by remember { mutableStateOf(false) }
    var selectedEmailForOtp by remember { mutableStateOf<String?>(null) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var otpTimer by remember { mutableStateOf(60) }
    var otpSendCooldown by remember { mutableStateOf(0) }

    LaunchedEffect(otpSendCooldown) {
        if (otpSendCooldown > 0) {
            while (otpSendCooldown > 0) {
                kotlinx.coroutines.delay(1000L)
                otpSendCooldown -= 1
            }
        }
    }

    LaunchedEffect(isOtpSent, generatedOtp) {
        if (isOtpSent) {
            otpTimer = 60
            while (otpTimer > 0) {
                kotlinx.coroutines.delay(1000L)
                otpTimer -= 1
            }
        }
    }
    val notificationHelper = remember { com.example.util.NotificationHelper(context) }
    var showDesignerDialog by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    var showChangeDialog by remember { mutableStateOf(false) }
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    // Student Portal Login States
    var selectedPortal by remember { mutableStateOf("student") }
    val isStudentPortal = selectedPortal == "student"
    var studentIdInput by remember { mutableStateOf("") }
    var studentPasswordInput by remember { mutableStateOf("") }
    var studentIdError by remember { mutableStateOf<String?>(null) }
    var studentPasswordError by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()
    val allowedEmails by viewModel.allowedEmails.collectAsState()
    val isLoading = authState is AuthState.Loading

    // Handle authentication feedback reactively
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Secure access granted. Welcome, Admin!", Toast.LENGTH_SHORT).show()
                viewModel.clearAuthState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, "Authentication Failed: ${state.message}", Toast.LENGTH_LONG).show()
                viewModel.clearAuthState()
            }
            else -> {}
        }
    }

    var isEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isEntered = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Background")

    val orb1OffsetFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Orb1"
    )

    val orb2OffsetFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Orb2"
    )

    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoScale"
    )

    val logoRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "CardScale"
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "CardAlpha"
    )

    val cardSlideUp by animateFloatAsState(
        targetValue = if (isEntered) 0f else 40f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "CardSlide"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic decorative floating orb background with a slow-flowing mesh feel
        val isDark = isSystemInDarkTheme()
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Orbit path formula
            val orb1X = width * 0.3f + (Math.cos(orb1OffsetFraction.toDouble()).toFloat() * 150f)
            val orb1Y = height * 0.25f + (Math.sin(orb1OffsetFraction.toDouble()).toFloat() * 180f)
            
            val orb2X = width * 0.7f + (Math.sin(orb2OffsetFraction.toDouble()).toFloat() * 180f)
            val orb2Y = height * 0.75f + (Math.cos(orb2OffsetFraction.toDouble()).toFloat() * 160f)

            val orb3X = width * 0.5f + (Math.cos((orb1OffsetFraction + orb2OffsetFraction).toDouble() * 0.5).toFloat() * 100f)
            val orb3Y = height * 0.5f + (Math.sin((orb1OffsetFraction + orb2OffsetFraction).toDouble() * 0.5).toFloat() * 120f)
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (isDark) 0.16f else 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = 450f
                ),
                center = Offset(orb1X, orb1Y),
                radius = 450f
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = if (isDark) 0.16f else 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = 500f
                ),
                center = Offset(orb2X, orb2Y),
                radius = 500f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryColor.copy(alpha = if (isDark) 0.12f else 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(orb3X, orb3Y),
                    radius = 400f
                ),
                center = Offset(orb3X, orb3Y),
                radius = 400f
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .align(Alignment.Center)
                .padding(24.dp)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                    translationY = cardSlideUp
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val appLogoUri by viewModel.appLogoUri.collectAsState()
                
                // Outer rotating gradient border container
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(logoScale)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .graphicsLayer {
                            rotationZ = logoRotationAngle
                        }
                        .padding(4.dp)
                ) {
                    // Inner logo holder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appLogoUri.isNotEmpty()) {
                            AsyncImage(
                                model = appLogoUri,
                                contentDescription = "Toppers Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_icon_foreground),
                                contentDescription = "Toppers Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Toppers Academy",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    AnimatedContent(
                        targetState = selectedPortal,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                                    fadeOut(animationSpec = tween(90))
                        },
                        label = "SubtitleTransition"
                    ) { portal ->
                        val subText = when (portal) {
                            "student" -> "Secure Student/Parent Portal"
                            "teacher" -> "Secure Teacher Portal"
                            else -> "Secure Administrator Portal"
                        }
                        Text(
                            text = subText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Elegant Segmented Role Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "student" to "Student",
                        "teacher" to "Teacher",
                        "admin" to "Admin"
                    ).forEach { (key, label) ->
                        val isSelected = selectedPortal == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedPortal = key }
                                .testTag("${key}_portal_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedContent(
                    targetState = selectedPortal,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(320)) togetherWith fadeOut(animationSpec = tween(320))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "PortalFormTransition"
                ) { portalKey ->
                    if (portalKey == "admin") {
                        // Admin Portal Email OTP Login
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            var isDropdownExpanded by remember { mutableStateOf(false) }

                            AnimatedContent(
                                targetState = isOtpSent,
                                transitionSpec = {
                                    if (targetState) {
                                        (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300)))
                                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300)))
                                    } else {
                                        (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300)))
                                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300)))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = "AdminOtpTransition"
                            ) { otpSentActive ->
                                if (otpSentActive) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Enter the 6-digit OTP code sent to your email:",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            // Styled custom segmented OTP input
                                            OtpDigitRow(
                                                otpText = otpInput,
                                                onOtpChange = { input ->
                                                    otpInput = input
                                                    otpError = null
                                                },
                                                isError = otpError != null
                                            )
                                        }

                                        if (otpError != null) {
                                            Text(
                                                text = otpError ?: "",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.align(Alignment.Start)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Submit OTP & Log In Button & Resend Button Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val verifyInteractionSource = remember { MutableInteractionSource() }
                                            val verifyScale by rememberButtonPressScale(verifyInteractionSource)

                                            val resendInteractionSource = remember { MutableInteractionSource() }
                                            val resendScale by rememberButtonPressScale(resendInteractionSource)

                                            Button(
                                                onClick = {
                                                    if (otpTimer <= 0) {
                                                        otpError = "OTP expired. Please click Resend to request a new code."
                                                    } else if (otpInput.trim() == generatedOtp) {
                                                        viewModel.loginWithOtpSuccess(email)
                                                    } else {
                                                        otpError = "Incorrect OTP code. Please try again."
                                                    }
                                                },
                                                enabled = !isLoading,
                                                interactionSource = verifyInteractionSource,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                                contentPadding = PaddingValues(),
                                                shape = RoundedCornerShape(26.dp),
                                                modifier = Modifier
                                                    .weight(1.2f)
                                                    .height(52.dp)
                                                    .graphicsLayer {
                                                        scaleX = verifyScale
                                                        scaleY = verifyScale
                                                    }
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.primary,
                                                                MaterialTheme.colorScheme.secondary
                                                            )
                                                        ),
                                                        shape = RoundedCornerShape(26.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = Color.White.copy(alpha = 0.25f),
                                                        shape = RoundedCornerShape(26.dp)
                                                    )
                                                    .testTag("admin_login_verify_btn")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    if (isLoading) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(20.dp),
                                                            color = Color.White,
                                                            strokeWidth = 2.dp
                                                        )
                                                    } else {
                                                        Icon(imageVector = Icons.Default.Login, contentDescription = null, tint = Color.White)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Verify & Log In", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                                    }
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    val randomOtp = (java.util.Random().nextInt(900000) + 100000).toString()
                                                    generatedOtp = randomOtp
                                                    otpInput = ""
                                                    otpError = null
                                                    otpTimer = 60
                                                    
                                                    val emailNormalized = email.trim().lowercase()
                                                    viewModel.sendSmtpOtp(emailNormalized, randomOtp) { result ->
                                                        if (result.isSuccess) {
                                                            Toast.makeText(context, "Verification OTP email sent!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                                                            Toast.makeText(context, "Failed to send OTP email: $errorMsg", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                enabled = !isLoading && otpTimer <= 0,
                                                interactionSource = resendInteractionSource,
                                                shape = RoundedCornerShape(26.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary,
                                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(52.dp)
                                                    .graphicsLayer {
                                                        scaleX = resendScale
                                                        scaleY = resendScale
                                                    }
                                                    .testTag("admin_resend_otp_btn")
                                            ) {
                                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (otpTimer > 0) "Resend (${otpTimer}s)" else "Resend OTP",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        TextButton(
                                            onClick = {
                                                isOtpSent = false
                                                isOtpSending = false
                                                otpInput = ""
                                                otpError = null
                                                otpTimer = 60
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Change Email Address", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = email,
                                                onValueChange = {
                                                    email = it
                                                    emailError = null
                                                },
                                                readOnly = false,
                                                label = { Text("Admin Email ID") },
                                                placeholder = { Text("e.g. marghubalam000@gmail.com") },
                                                singleLine = true,
                                                isError = emailError != null,
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Email,
                                                        contentDescription = "Email",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                },
                                                trailingIcon = {
                                                    IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                                        Icon(
                                                            imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                            contentDescription = "Choose Admin Email",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("admin_email_input"),
                                                shape = RoundedCornerShape(12.dp)
                                            )

                                            DropdownMenu(
                                                expanded = isDropdownExpanded,
                                                onDismissRequest = { isDropdownExpanded = false },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                allowedEmails.forEach { emailOption ->
                                                    DropdownMenuItem(
                                                        text = { 
                                                            Text(
                                                                text = emailOption,
                                                                fontWeight = if (email == emailOption) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (email == emailOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                            ) 
                                                        },
                                                        onClick = {
                                                            email = emailOption
                                                            emailError = null
                                                            isDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        if (allowedEmails.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Quick Choose Authorized Email:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                    letterSpacing = 0.5.sp
                                                )
                                                LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    items(allowedEmails.toList()) { emailOption ->
                                                        val isSelected = email.trim().lowercase() == emailOption.trim().lowercase()
                                                        SuggestionChip(
                                                            onClick = {
                                                                email = emailOption
                                                                emailError = null
                                                            },
                                                            label = {
                                                                Text(
                                                                    text = emailOption,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            },
                                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                                containerColor = if (isSelected) {
                                                                    MaterialTheme.colorScheme.primaryContainer
                                                                } else {
                                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                                },
                                                                labelColor = if (isSelected) {
                                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                                } else {
                                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                                }
                                                            ),
                                                            border = if (isSelected) {
                                                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                                            } else {
                                                                BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                                            },
                                                            shape = RoundedCornerShape(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (emailError != null) {
                                            Text(
                                                text = emailError ?: "",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.align(Alignment.Start)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Send OTP Button
                                        val sendInteractionSource = remember { MutableInteractionSource() }
                                        val sendScale by rememberButtonPressScale(sendInteractionSource)

                                        Button(
                                            onClick = {
                                                val emailNormalized = email.trim().lowercase()
                                                val allowedEmails = viewModel.allowedEmails.value.map { it.trim().lowercase() }
                                                if (email.trim().isEmpty() || !isValidEmail(email)) {
                                                    emailError = "Please enter a valid administrative email."
                                                } else if (!allowedEmails.contains(emailNormalized)) {
                                                    emailError = "Unauthorized: This email is not registered as an administrator."
                                                } else {
                                                    isOtpSending = true
                                                    val randomOtp = (java.util.Random().nextInt(900000) + 100000).toString()
                                                    generatedOtp = randomOtp
                                                    selectedEmailForOtp = emailNormalized
                                                    otpInput = ""
                                                    otpError = null
                                                    
                                                    isOtpSent = true
                                                    otpSendCooldown = 5

                                                    viewModel.sendSmtpOtp(emailNormalized, randomOtp) { result ->
                                                        isOtpSending = false
                                                        if (result.isSuccess) {
                                                            Toast.makeText(context, "OTP Sent! Check your email.", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                                                            Toast.makeText(context, "Failed to send email ($errorMsg).", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = !isLoading && !isOtpSending && otpSendCooldown <= 0,
                                            interactionSource = sendInteractionSource,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                            contentPadding = PaddingValues(),
                                            shape = RoundedCornerShape(26.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .graphicsLayer {
                                                    scaleX = sendScale
                                                    scaleY = sendScale
                                                }
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.secondary
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(26.dp)
                                                )
                                                .border(
                                                    width = 1.5.dp,
                                                    color = Color.White.copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(26.dp)
                                                )
                                                .testTag("admin_send_otp_btn")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = if (otpSendCooldown > 0) "PLEASE WAIT (${otpSendCooldown}s)" else "SEND VERIFICATION OTP",
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    letterSpacing = 1.5.sp,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Security disclaimer badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shield",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Email OTP Multi-Factor Authentication",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else if (portalKey == "teacher") {
                        // Teacher Portal ID & PIN Login
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            var teacherIdInput by remember { mutableStateOf("") }
                            var teacherPasswordInput by remember { mutableStateOf("") }
                            var teacherIdError by remember { mutableStateOf<String?>(null) }
                            var teacherPasswordError by remember { mutableStateOf<String?>(null) }

                            Text(
                                text = "Enter your Teacher ID and PIN to access your classroom portal.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Styled Teacher ID Input
                            OutlinedTextField(
                                value = teacherIdInput,
                                onValueChange = {
                                    teacherIdInput = it
                                    teacherIdError = null
                                },
                                label = { Text("Teacher ID (e.g. T-101)") },
                                isError = teacherIdError != null,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("teacher_id_input"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Badge, contentDescription = null)
                                }
                            )
                            if (teacherIdError != null) {
                                Text(
                                    text = teacherIdError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                            }

                            // Styled PIN Input
                            OutlinedTextField(
                                value = teacherPasswordInput,
                                onValueChange = {
                                    teacherPasswordInput = it
                                    teacherPasswordError = null
                                },
                                label = { Text("PIN") },
                                isError = teacherPasswordError != null,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth().testTag("teacher_password_input"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                                }
                            )
                            if (teacherPasswordError != null) {
                                Text(
                                    text = teacherPasswordError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val teacherLoginInteractionSource = remember { MutableInteractionSource() }
                            val teacherLoginScale by rememberButtonPressScale(teacherLoginInteractionSource)

                            Button(
                                onClick = {
                                    var valid = true
                                    if (teacherIdInput.trim().isEmpty()) {
                                        teacherIdError = "Teacher ID is required."
                                        valid = false
                                    }
                                    if (teacherPasswordInput.trim().isEmpty()) {
                                        teacherPasswordError = "PIN is required."
                                        valid = false
                                    }

                                    if (valid) {
                                        viewModel.loginTeacher(teacherIdInput, teacherPasswordInput) { success, msg ->
                                            if (success) {
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            } else {
                                                teacherPasswordError = msg
                                            }
                                        }
                                    }
                                },
                                interactionSource = teacherLoginInteractionSource,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(),
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .graphicsLayer {
                                        scaleX = teacherLoginScale
                                        scaleY = teacherLoginScale
                                    }
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                    .testTag("teacher_login_submit_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(imageVector = Icons.Default.Login, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "LOG IN AS TEACHER",
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.5.sp,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Contact school administration if you do not have your Teacher ID or PIN.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    } else {
                        // Student Portal ID & PIN Login
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = studentIdInput,
                                onValueChange = {
                                    studentIdInput = it
                                    studentIdError = null
                                },
                                label = { Text("Student ID") },
                                placeholder = { Text("e.g. TA-10001") },
                                singleLine = true,
                                isError = studentIdError != null,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = "Badge",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("student_id_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (studentIdError != null) {
                                Text(
                                    text = studentIdError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                            }

                            OutlinedTextField(
                                value = studentPasswordInput,
                                onValueChange = {
                                    studentPasswordInput = it
                                    studentPasswordError = null
                                },
                                label = { Text("PIN Password") },
                                placeholder = { Text("e.g. 123456") },
                                singleLine = true,
                                isError = studentPasswordError != null,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("student_password_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (studentPasswordError != null) {
                                Text(
                                    text = studentPasswordError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val studentLoginInteractionSource = remember { MutableInteractionSource() }
                            val studentLoginScale by rememberButtonPressScale(studentLoginInteractionSource)

                            Button(
                                onClick = {
                                    var valid = true
                                    if (studentIdInput.trim().isEmpty()) {
                                        studentIdError = "Student ID is required."
                                        valid = false
                                    }
                                    if (studentPasswordInput.trim().isEmpty()) {
                                        studentPasswordError = "PIN is required."
                                        valid = false
                                    }

                                    if (valid) {
                                        viewModel.loginStudent(studentIdInput, studentPasswordInput) { success, msg ->
                                            if (success) {
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            } else {
                                                studentPasswordError = msg
                                            }
                                        }
                                    }
                                },
                                interactionSource = studentLoginInteractionSource,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(),
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .graphicsLayer {
                                        scaleX = studentLoginScale
                                        scaleY = studentLoginScale
                                    }
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                    .testTag("student_login_submit_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(imageVector = Icons.Default.Login, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "LOG IN AS STUDENT",
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.5.sp,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Contact school administration if you do not have your Student ID or PIN.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Designer Footer Credit - Marghubur Rahman
        MarghubSignatureBadge(
            onClick = { showDesignerDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // Animated Designer Contact Dialog
        if (showDesignerDialog) {
            MarghubProfileDialog(
                onDismiss = { showDesignerDialog = false }
            )
        }
    }
}

@Composable
fun ContactItemRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Open Link",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun OtpDigitRow(
    otpText: String,
    onOtpChange: (String) -> Unit,
    maxLength: Int = 6,
    isError: Boolean = false
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until maxLength) {
                val char = otpText.getOrNull(i)?.toString() ?: ""
                val isFocused = otpText.length == i
                
                val borderColor by animateColorAsState(
                    targetValue = when {
                        isError -> MaterialTheme.colorScheme.error
                        isFocused -> MaterialTheme.colorScheme.primary
                        char.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    animationSpec = tween(200),
                    label = "BorderColor"
                )
                
                val borderThickness by animateDpAsState(
                    targetValue = if (isFocused) 2.2.dp else 1.dp,
                    label = "BorderThickness"
                )
                
                val scale by animateFloatAsState(
                    targetValue = if (isFocused) 1.05f else 1.0f,
                    label = "Scale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .scale(scale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        )
                        .border(borderThickness, borderColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (char.isNotEmpty()) {
                        Text(
                            text = char,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    } else if (isFocused) {
                        val cursorAlpha by rememberInfiniteTransition(label = "Cursor").animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "Alpha"
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(22.dp)
                                .graphicsLayer { alpha = cursorAlpha }
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
        
        androidx.compose.foundation.text.BasicTextField(
            value = otpText,
            onValueChange = { input ->
                if (input.length <= maxLength && input.all { it.isDigit() }) {
                    onOtpChange(input)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("admin_otp_input")
                .graphicsLayer { alpha = 0.01f }
        )
    }
}

