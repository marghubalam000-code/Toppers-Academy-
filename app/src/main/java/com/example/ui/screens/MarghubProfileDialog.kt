package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import coil.compose.AsyncImage

@Composable
fun MarghubProfileDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("toppers_admin_prefs", Context.MODE_PRIVATE) }
    val marghubLogoUri = remember { sharedPrefs.getString("marghub_logo_uri", "") ?: "" }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Animating content entry
            var animateIn by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                animateIn = true
            }

            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight()
                        .clickable(enabled = false) {}, // prevent click propagation
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF111214) // Dark card surface as in screenshot
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Beautiful Golden/Blue top bar highlighting
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF00E5FF), // Cyan
                                            Color(0xFFFFB300), // Gold
                                            Color(0xFFFF3D00)  // Coral Red
                                        )
                                    )
                                )
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // 2. Large Round Portrait with Premium Golden Gradient Ring
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            // Spinning glowing background ring
                            val infiniteTransition = rememberInfiniteTransition(label = "ring_glow")
                            val angle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(8000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "angle"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            colors = listOf(
                                                Color(0xFFFFD700), // Gold
                                                Color(0xFFB8860B), // Dark Gold
                                                Color(0xFFDAA520), // Goldenrod
                                                Color(0xFFFFD700)  // Gold back
                                            )
                                        )
                                    )
                            )

                            // Inner black ring separator for high-end look
                            Box(
                                modifier = Modifier
                                    .size(122.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF111214))
                            )

                            // Actual Designer Image
                            if (marghubLogoUri.isNotEmpty()) {
                                AsyncImage(
                                    model = marghubLogoUri,
                                    contentDescription = "Marghubur Rahman Profile Photo",
                                    modifier = Modifier
                                        .size(114.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_icon_foreground),
                                    contentDescription = "Marghubur Rahman Profile Photo",
                                    modifier = Modifier
                                        .size(114.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. Name & Creative Title
                        Text(
                            text = "Marghubur Rahman",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "FULL-STACK CREATIVE ARCHITECT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF90A4AE),
                            letterSpacing = 1.8.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Designer Skill Badge Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE65100).copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                    .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("🔥 Android Expert", color = Color(0xFFFFB74D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF006064).copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("⚡ UI/UX Dev", color = Color(0xFF80DEEA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1B5E20).copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("💼 Top Rated", color = Color(0xFF69F0AE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 4. Custom Styled Contact Options Stack
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // WHATSAPP CHAT
                            DeveloperContactCard(
                                title = "WHATSAPP CHAT",
                                subtitle = "9263960341",
                                icon = Icons.Default.ChatBubble,
                                iconBgColor = Color(0xFF1E3A1E),
                                iconColor = Color(0xFF00E676)
                            ) {
                                openWhatsApp(context, "9263960341")
                            }

                            // MOBILE NUMBER
                            DeveloperContactCard(
                                title = "MOBILE NUMBER",
                                subtitle = "9263960341",
                                icon = Icons.Default.Phone,
                                iconBgColor = Color(0xFF1A365D),
                                iconColor = Color(0xFF2196F3)
                            ) {
                                makeCall(context, "9263960341")
                            }

                            // EMAIL ADDRESS
                            DeveloperContactCard(
                                title = "EMAIL ADDRESS",
                                subtitle = "marghubalam000@gmail.com",
                                icon = Icons.Default.Email,
                                iconBgColor = Color(0xFF3E2723),
                                iconColor = Color(0xFFFF9800)
                            ) {
                                sendEmail(context, "marghubalam000@gmail.com")
                            }

                            // INSTAGRAM PROFILE
                            DeveloperContactCard(
                                title = "INSTAGRAM PROFILE",
                                subtitle = "@marghub07_dz",
                                icon = Icons.Default.CameraAlt,
                                iconBgColor = Color(0xFF3A1C3B),
                                iconColor = Color(0xFFE91E63)
                            ) {
                                openInstagram(context, "marghub07_dz")
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // 5. Sleek Dismiss Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.15f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF1B1C20),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "DISMISS PROFILE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperContactCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1B1E) // Premium dark slate card
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container with Circular tinted BG
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Intent launching helpers
private fun openWhatsApp(context: Context, number: String) {
    try {
        val cleanNumber = number.replace("+", "").replace(" ", "")
        val formattedNumber = if (cleanNumber.startsWith("91")) cleanNumber else "91$cleanNumber"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedNumber"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

private fun makeCall(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open Dialer", Toast.LENGTH_SHORT).show()
    }
}

private fun sendEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open Email Client", Toast.LENGTH_SHORT).show()
    }
}

private fun openInstagram(context: Context, handle: String) {
    try {
        val cleanHandle = handle.replace("@", "")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$cleanHandle"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open Instagram", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MarghubSignatureBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SignatureGlow")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BadgeScale"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerOffset"
    )

    val primaryColor = Color(0xFFFFD700) // Deep Gold
    val secondaryColor = Color(0xFF00E5FF) // Cyber Cyan
    val tertiaryColor = Color(0xFFE91E63) // Neon Pink

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(30.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0D0E11).copy(alpha = 0.85f),
                        Color(0xFF15181F).copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor, tertiaryColor, primaryColor),
                    start = androidx.compose.ui.geometry.Offset(shimmerOffset, 0f),
                    end = androidx.compose.ui.geometry.Offset(shimmerOffset + 400f, 400f)
                ),
                shape = RoundedCornerShape(30.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val indicatorAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "IndicatorAlpha"
            )
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = indicatorAlpha }
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
            )

            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Design by Marghubur Rahman",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )
                Text(
                    text = "TAP TO VIEW PORTFOLIO & CONTACT",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = Color(0xFF90A4AE)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Open Profile",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

