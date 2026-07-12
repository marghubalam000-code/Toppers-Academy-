package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Color as AndroidColor
import android.graphics.Path
import androidx.print.PrintHelper
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.model.Student
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudentIDCardDialog(
    student: Student,
    appLogoUri: String = "",
    principalSignatureUri: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isFlipped by remember { mutableStateOf(false) }
    
    // Animation angle for 3D flip effect
    val rotationAngle by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "IDCardFlip"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant top message / hint
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Card",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Tap Card to view double-sided details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3D Flippable Container
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(440.dp)
                    .graphicsLayer {
                        rotationY = rotationAngle
                        cameraDistance = 12f * density
                    }
                    .clickable { isFlipped = !isFlipped },
                contentAlignment = Alignment.Center
            ) {
                if (rotationAngle <= 90f) {
                    // Front Side
                    IDCardFront(
                        student = student,
                        appLogoUri = appLogoUri,
                        principalSignatureUri = principalSignatureUri
                    )
                } else {
                    // Back Side (mirrored back to read correctly)
                    Box(
                        modifier = Modifier.graphicsLayer {
                            rotationY = 180f
                        }
                    ) {
                        IDCardBack(
                            student = student,
                            appLogoUri = appLogoUri
                        )
                    }
                }
            }

            // Interactive Actions Panel
            Row(
                modifier = Modifier.fillMaxWidth(0.95f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        saveIDCardAsImageToGallery(context, student, appLogoUri, principalSignatureUri)
                    },
                    modifier = Modifier.weight(1f).testTag("download_id_card_img_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download Image", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save to Gallery", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        printStudentIDCard(context, student, appLogoUri, principalSignatureUri)
                    },
                    modifier = Modifier.weight(1f).testTag("print_id_card_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Print ID Card", tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print Card", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).testTag("close_id_card_dialog_btn"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun IDCardFront(
    student: Student,
    appLogoUri: String,
    principalSignatureUri: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(12.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.6f)) // Classic Golden border
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Stylish luxury diagonal brush background
                    val gradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F2027), // Midnight Navy
                            Color(0xFF203A43),
                            Color(0xFF2C5364)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                    drawRect(brush = gradient)

                    // Abstract golden/light curved ribbons at the bottom
                    val goldBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFD4AF37).copy(alpha = 0.15f), Color.Transparent)
                    )
                    drawCircle(
                        brush = goldBrush,
                        radius = size.width * 0.7f,
                        center = Offset(size.width * 0.8f, size.height)
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header (School Identity)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (appLogoUri.isNotEmpty()) {
                            AsyncImage(
                                model = appLogoUri,
                                contentDescription = "School Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Default School Icon",
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "TOPPERS ACADEMY",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Nurturing Excellence",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD4AF37),
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                    
                    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                        drawLine(
                            color = Color(0xFFD4AF37).copy(alpha = 0.5f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 2f
                        )
                    }
                }

                // Student Photograph with Stylish Gold Frame
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(6.dp, shape = RoundedCornerShape(16.dp))
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                        .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    StudentPhoto(
                        photoData = student.photo,
                        modifier = Modifier.fillMaxSize(),
                        placeholderIcon = Icons.Default.Person,
                        tintColor = Color(0xFF2C5364)
                    )
                }

                // Student Identity Label
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFD4AF37), // Solid Golden Badge
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "STUDENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F2027),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        letterSpacing = 1.5.sp
                    )
                }

                // Core Student Details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = student.name.uppercase(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "ID No: ${student.studentId}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CLASS", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            Text(student.studentClass, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.2f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SEC", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            Text(student.section, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.2f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ROLL", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            Text(student.rollNumber, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Footer (Session & Authenticator line)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "SESSION",
                            fontSize = 7.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "2026-2027",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Principal signature graphic
                        if (principalSignatureUri.isNotEmpty()) {
                            AsyncImage(
                                model = principalSignatureUri,
                                contentDescription = "Principal Signature",
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(70.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = "Principal",
                                fontFamily = FontFamily.Serif,
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4AF37)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SIGNATURE",
                            fontSize = 6.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IDCardBack(
    student: Student,
    appLogoUri: String
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(12.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFF0F2027).copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Dark header strip
                    drawRect(
                        color = Color(0xFF0F2027),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.16f)
                    )
                    // Gold line separation
                    drawLine(
                        color = Color(0xFFD4AF37),
                        start = Offset(0f, size.height * 0.16f),
                        end = Offset(size.width, size.height * 0.16f),
                        strokeWidth = 3f
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Strip text (aligned in top 16% height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EMERGENCY INFORMATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                }

                // Info Rows List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BackCardRow(label = "Father Name", value = student.fatherName)
                    BackCardRow(label = "Mother Name", value = student.motherName)
                    BackCardRow(
                        label = "Emergency Mobile",
                        value = student.fatherMobile.ifEmpty { student.mobile }
                    )
                    BackCardRow(label = "Date of Birth", value = student.dob)
                    BackCardRow(label = "Blood Group", value = "B+ (Optional)")
                    BackCardRow(label = "Home Address", value = student.address)
                }

                // Terms / Verification Box
                Surface(
                    color = Color(0xFFF8F9FA),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Terms & Instructions:",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F2027)
                        )
                        Text(
                            text = "1. This ID card is non-transferable and must be displayed during school hours.\n" +
                                   "2. If found, please return to the school administration office immediately.\n" +
                                   "3. Contact Administration: support@toppersacademy.com | +91 98765 43210",
                            fontSize = 6.sp,
                            color = Color.DarkGray,
                            lineHeight = 8.sp
                        )
                    }
                }

                // Mock Barcode / QR Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Custom Barcode Graphic
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(28.dp)
                    ) {
                        val barcodeColor = Color(0xFF0F2027)
                        val totalLines = 36
                        val segmentWidth = size.width / totalLines
                        val pattern = listOf(2, 1, 4, 1, 2, 3, 1, 2, 4, 1, 2, 1, 3, 2, 1, 4, 1, 2, 1, 3, 2, 1, 4, 2, 1, 2, 3, 1, 2, 4, 1, 2, 1, 4, 2, 1)
                        
                        var currentX = 0f
                        for (i in 0 until totalLines) {
                            val strokeMultiplier = pattern[i % pattern.size]
                            val isSpace = i % 2 == 1
                            if (!isSpace) {
                                drawRect(
                                    color = barcodeColor,
                                    topLeft = Offset(currentX, 0f),
                                    size = androidx.compose.ui.geometry.Size(segmentWidth * 0.7f * strokeMultiplier, size.height)
                                )
                            }
                            currentX += segmentWidth
                        }
                    }

                    Text(
                        text = "*${student.studentId}*",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun BackCardRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F2027),
            modifier = Modifier.width(85.dp)
        )
        Text(
            text = value.ifEmpty { "Not provided" },
            fontSize = 9.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
    }
}

fun generateIDCardBitmap(
    context: android.content.Context,
    student: Student,
    appLogoUri: String = "",
    principalSignatureUri: String = ""
): Bitmap {
    val cardW = 600
    val cardH = 900
    val padding = 40
    val spacing = 40
    
    val totalW = cardW * 2 + padding * 2 + spacing
    val totalH = cardH + padding * 2 + 100
    
    val bitmap = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    
    val bgPaint = Paint().apply {
        color = AndroidColor.parseColor("#121212")
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, totalW.toFloat(), totalH.toFloat(), bgPaint)
    
    val titlePaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 32f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("TOPPERS ACADEMY - STUDENT ID CARD", (totalW / 2).toFloat(), 60f, titlePaint)
    
    val subtitlePaint = Paint().apply {
        color = AndroidColor.parseColor("#D4AF37")
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("Digital Identity Card Proof (Front & Back)", (totalW / 2).toFloat(), 95f, subtitlePaint)
    
    val startY = 130f
    
    // --- DRAW FRONT CARD (Left side) ---
    val frontX = padding.toFloat()
    drawFrontCardOnCanvas(context, canvas, frontX, startY, cardW.toFloat(), cardH.toFloat(), student, appLogoUri, principalSignatureUri)
    
    // --- DRAW BACK CARD (Right side) ---
    val backX = (padding + cardW + spacing).toFloat()
    drawBackCardOnCanvas(context, canvas, backX, startY, cardW.toFloat(), cardH.toFloat(), student)
    
    val footerPaint = Paint().apply {
        color = AndroidColor.parseColor("#888888")
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("Generated on ${SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault()).format(Date())} - Toppers Academy", (totalW / 2).toFloat(), totalH.toFloat() - 30f, footerPaint)
    
    return bitmap
}

fun drawFrontCardOnCanvas(
    context: android.content.Context,
    canvas: AndroidCanvas,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    student: Student,
    appLogoUri: String,
    principalSignatureUri: String
) {
    val rect = RectF(x, y, x + w, y + h)
    
    val cardPaint = Paint().apply {
        isAntiAlias = true
    }
    val gradient = LinearGradient(
        x, y, x + w, y + h,
        AndroidColor.parseColor("#0F2027"),
        AndroidColor.parseColor("#2C5364"),
        Shader.TileMode.CLAMP
    )
    cardPaint.shader = gradient
    canvas.drawRoundRect(rect, 30f, 30f, cardPaint)
    cardPaint.shader = null
    
    val borderPaint = Paint().apply {
        color = AndroidColor.parseColor("#D4AF37")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    canvas.drawRoundRect(rect, 30f, 30f, borderPaint)
    
    val decorativePaint = Paint().apply {
        color = AndroidColor.argb(20, 212, 175, 55)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(x + w * 0.8f, y + h, w * 0.6f, decorativePaint)
    
    val logoW = 64f
    val logoH = 64f
    var logoBitmap: Bitmap? = null
    try {
        val logoFile = File(context.filesDir, "custom_app_logo.png")
        if (logoFile.exists()) {
            logoBitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    val logoX = x + 30f
    val logoY = y + 30f
    if (logoBitmap != null) {
        val circularBitmap = getRoundedCroppedBitmap(logoBitmap)
        val destRect = Rect(logoX.toInt(), logoY.toInt(), (logoX + logoW).toInt(), (logoY + logoH).toInt())
        canvas.drawBitmap(circularBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
    } else {
        val shieldPaint = Paint().apply {
            color = AndroidColor.parseColor("#D4AF37")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val starPath = Path().apply {
            moveTo(logoX + logoW / 2, logoY)
            lineTo(logoX + logoW, logoY + logoH * 0.35f)
            lineTo(logoX + logoW * 0.8f, logoY + logoH)
            lineTo(logoX + logoW * 0.2f, logoY + logoH)
            lineTo(logoX, logoY + logoH * 0.35f)
            close()
        }
        canvas.drawPath(starPath, shieldPaint)
    }
    
    val schoolTitlePaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    canvas.drawText("TOPPERS ACADEMY", logoX + logoW + 20f, logoY + 28f, schoolTitlePaint)
    
    val schoolSubPaint = Paint().apply {
        color = AndroidColor.parseColor("#D4AF37")
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    canvas.drawText("Nurturing Excellence", logoX + logoW + 20f, logoY + 54f, schoolSubPaint)
    
    val sepPaint = Paint().apply {
        color = AndroidColor.argb(128, 212, 175, 55)
        strokeWidth = 2f
        isAntiAlias = true
    }
    canvas.drawLine(x + 20f, y + 115f, x + w - 20f, y + 115f, sepPaint)
    
    val photoW = 200f
    val photoH = 220f
    val photoX = x + (w - photoW) / 2
    val photoY = y + 140f
    
    val photoBgPaint = Paint().apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawRoundRect(RectF(photoX - 6, photoY - 6, photoX + photoW + 6, photoY + photoH + 6), 20f, 20f, photoBgPaint)
    
    val photoBorderPaint = Paint().apply {
        color = AndroidColor.parseColor("#D4AF37")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    canvas.drawRoundRect(RectF(photoX - 6, photoY - 6, photoX + photoW + 6, photoY + photoH + 6), 20f, 20f, photoBorderPaint)
    
    var photoBitmap: Bitmap? = null
    if (!student.photo.isNullOrBlank()) {
        try {
            val decodedString = android.util.Base64.decode(student.photo, android.util.Base64.DEFAULT)
            photoBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    if (photoBitmap != null) {
        val destRect = Rect(photoX.toInt(), photoY.toInt(), (photoX + photoW).toInt(), (photoY + photoH).toInt())
        canvas.drawBitmap(photoBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
    } else {
        val placeholderPaint = Paint().apply {
            color = AndroidColor.parseColor("#2C5364")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(RectF(photoX, photoY, photoX + photoW, photoY + photoH), placeholderPaint)
        
        val pPaint = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(photoX + photoW / 2, photoY + photoH * 0.4f, photoW * 0.22f, pPaint)
        val shouldersPath = Path().apply {
            moveTo(photoX + photoW * 0.15f, photoY + photoH)
            quadTo(photoX + photoW / 2, photoY + photoH * 0.65f, photoX + photoW * 0.85f, photoY + photoH)
            close()
        }
        canvas.drawPath(shouldersPath, pPaint)
    }
    
    val badgeW = 160f
    val badgeH = 34f
    val badgeX = x + (w - badgeW) / 2
    val badgeY = photoY + photoH + 25f
    val badgePaint = Paint().apply {
        color = AndroidColor.parseColor("#D4AF37")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawRoundRect(RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH), 12f, 12f, badgePaint)
    
    val badgeTextPaint = Paint().apply {
        color = AndroidColor.parseColor("#0F2027")
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("STUDENT", badgeX + badgeW / 2, badgeY + 24f, badgeTextPaint)
    
    val namePaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText(student.name.uppercase(), x + w / 2, badgeY + 75f, namePaint)
    
    val idPaint = Paint().apply {
        color = AndroidColor.parseColor("#D4AF37")
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("ID No: ${student.studentId}", x + w / 2, badgeY + 110f, idPaint)
    
    val tableY = badgeY + 140f
    val tableW = w * 0.9f
    val tableH = 75f
    val tableX = x + (w - tableW) / 2
    val tableRect = RectF(tableX, tableY, tableX + tableW, tableY + tableH)
    
    val tablePaint = Paint().apply {
        color = AndroidColor.argb(20, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawRoundRect(tableRect, 15f, 15f, tablePaint)
    
    val labelPaint = Paint().apply {
        color = AndroidColor.argb(150, 255, 255, 255)
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val valPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    val col1X = tableX + tableW * 0.18f
    canvas.drawText("CLASS", col1X, tableY + 25f, labelPaint)
    canvas.drawText(student.studentClass, col1X, tableY + 58f, valPaint)
    
    canvas.drawLine(tableX + tableW * 0.36f, tableY + 15f, tableX + tableW * 0.36f, tableY + tableH - 15f, sepPaint)
    
    val col2X = tableX + tableW * 0.5f
    canvas.drawText("SEC", col2X, tableY + 25f, labelPaint)
    canvas.drawText(student.section, col2X, tableY + 58f, valPaint)
    
    canvas.drawLine(tableX + tableW * 0.64f, tableY + 15f, tableX + tableW * 0.64f, tableY + tableH - 15f, sepPaint)
    
    val col3X = tableX + tableW * 0.82f
    canvas.drawText("ROLL", col3X, tableY + 25f, labelPaint)
    canvas.drawText(student.rollNumber, col3X, tableY + 58f, valPaint)
    
    val footerY = y + h - 100f
    val sigW = 120f
    val sigH = 50f
    val sigX = x + (w - sigW) / 2
    
    var sigBitmap: Bitmap? = null
    try {
        val sigFile = File(context.filesDir, "custom_principal_signature.png")
        if (sigFile.exists()) {
            sigBitmap = BitmapFactory.decodeFile(sigFile.absolutePath)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    if (sigBitmap != null) {
        val destRect = Rect(sigX.toInt(), footerY.toInt(), (sigX + sigW).toInt(), (footerY + sigH).toInt())
        canvas.drawBitmap(sigBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
    } else {
        val cursivePaint = Paint().apply {
            color = AndroidColor.parseColor("#D4AF37")
            textSize = 24f
            typeface = Typeface.create("serif", Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Principal", x + w / 2, footerY + 35f, cursivePaint)
    }
    
    val sigLabelPaint = Paint().apply {
        color = AndroidColor.argb(128, 255, 255, 255)
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("SIGNATURE", x + w / 2, y + h - 25f, sigLabelPaint)
}

fun drawBackCardOnCanvas(
    context: android.content.Context,
    canvas: AndroidCanvas,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    student: Student
) {
    val rect = RectF(x, y, x + w, y + h)
    
    val cardPaint = Paint().apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawRoundRect(rect, 30f, 30f, cardPaint)
    
    val borderPaint = Paint().apply {
        color = AndroidColor.parseColor("#0F2027")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    canvas.drawRoundRect(rect, 30f, 30f, borderPaint)
    
    val headerHeight = h * 0.15f
    val headerRect = RectF(x + 2, y + 2, x + w - 2, y + headerHeight)
    val headerPaint = Paint().apply {
        color = AndroidColor.parseColor("#0F2027")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    canvas.save()
    val clipPath = Path().apply {
        addRoundRect(rect, 30f, 30f, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)
    canvas.drawRect(headerRect, headerPaint)
    
    val linePaint = Paint().apply {
        color = AndroidColor.parseColor("#D4AF37")
        strokeWidth = 4f
        isAntiAlias = true
    }
    canvas.drawLine(x, y + headerHeight, x + w, y + headerHeight, linePaint)
    
    val headerTextPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("EMERGENCY INFORMATION", x + w / 2, y + headerHeight * 0.6f, headerTextPaint)
    canvas.restore()
    
    val infoStartY = y + headerHeight + 50f
    val infoSpacing = 48f
    val labelX = x + 40f
    val valX = x + 230f
    
    val infoLabelPaint = Paint().apply {
        color = AndroidColor.parseColor("#0F2027")
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val infoValPaint = Paint().apply {
        color = AndroidColor.parseColor("#333333")
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    
    val rows = listOf(
        Pair("Father Name", student.fatherName),
        Pair("Mother Name", student.motherName),
        Pair("Emergency Mobile", student.fatherMobile.ifEmpty { student.mobile }),
        Pair("Date of Birth", student.dob),
        Pair("Blood Group", "B+ (Optional)"),
        Pair("Home Address", student.address)
    )
    
    var currentY = infoStartY
    for (row in rows) {
        canvas.drawText(row.first, labelX, currentY, infoLabelPaint)
        if (row.first == "Home Address" && row.second.length > 25) {
            val words = row.second.split(" ")
            var line1 = ""
            var line2 = ""
            for (word in words) {
                if ((line1 + word).length < 25) {
                    line1 += "$word "
                } else {
                    line2 += "$word "
                }
            }
            canvas.drawText(line1.trim(), valX, currentY, infoValPaint)
            if (line2.isNotBlank()) {
                currentY += 28f
                canvas.drawText(line2.trim(), valX, currentY, infoValPaint)
            }
        } else {
            canvas.drawText(row.second.ifEmpty { "Not provided" }, valX, currentY, infoValPaint)
        }
        currentY += infoSpacing
    }
    
    val termsY = y + h - 230f
    val termsW = w * 0.9f
    val termsH = 110f
    val termsX = x + (w - termsW) / 2
    val termsRect = RectF(termsX, termsY, termsX + termsW, termsY + termsH)
    
    val termsBgPaint = Paint().apply {
        color = AndroidColor.parseColor("#F8F9FA")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawRoundRect(termsRect, 12f, 12f, termsBgPaint)
    
    val termsBorderPaint = Paint().apply {
        color = AndroidColor.parseColor("#E0E0E0")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    canvas.drawRoundRect(termsRect, 12f, 12f, termsBorderPaint)
    
    val termsTitlePaint = Paint().apply {
        color = AndroidColor.parseColor("#0F2027")
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    canvas.drawText("Terms & Instructions:", termsX + 15f, termsY + 25f, termsTitlePaint)
    
    val termsTextPaint = Paint().apply {
        color = AndroidColor.parseColor("#555555")
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    canvas.drawText("1. This ID card is non-transferable and must be displayed.", termsX + 15f, termsY + 48f, termsTextPaint)
    canvas.drawText("2. If found, please return to school admin office immediately.", termsX + 15f, termsY + 68f, termsTextPaint)
    canvas.drawText("3. Administration Contact: support@toppersacademy.com", termsX + 15f, termsY + 88f, termsTextPaint)
    
    val barcodeY = y + h - 90f
    val barcodeW = w * 0.6f
    val barcodeH = 45f
    val barcodeX = x + (w - barcodeW) / 2
    
    val barcodePaint = Paint().apply {
        color = AndroidColor.parseColor("#0F2027")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    var bX = barcodeX
    val randomWidths = listOf(4, 2, 8, 3, 5, 2, 6, 2, 8, 4, 3, 6, 2, 8, 4, 3, 5, 2, 7, 3, 4, 2)
    var drawLine = true
    for (width in randomWidths) {
        if (drawLine) {
            canvas.drawRect(RectF(bX, barcodeY, bX + width, barcodeY + barcodeH), barcodePaint)
        }
        bX += width + 2
        drawLine = !drawLine
    }
    
    val barcodeTextPaint = Paint().apply {
        color = AndroidColor.parseColor("#0F2027")
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("*${student.studentId}*", x + w / 2, barcodeY + barcodeH + 20f, barcodeTextPaint)
}

fun getRoundedCroppedBitmap(bitmap: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(output)
    
    val paint = Paint().apply {
        isAntiAlias = true
    }
    val rect = Rect(0, 0, bitmap.width, bitmap.height)
    
    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawCircle(
        (bitmap.width / 2).toFloat(),
        (bitmap.height / 2).toFloat(),
        (bitmap.width / 2).toFloat(),
        paint
    )
    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)
    return output
}

fun saveIDCardAsImageToGallery(
    context: android.content.Context,
    student: Student,
    appLogoUri: String = "",
    principalSignatureUri: String = ""
) {
    try {
        val bitmap = generateIDCardBitmap(context, student, appLogoUri, principalSignatureUri)
        val filename = "ID_Card_${student.name.replace(" ", "_")}_${student.studentId}.png"
        
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ToppersAcademy")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            Toast.makeText(context, "ID Card Image saved to Photos/Gallery successfully!", Toast.LENGTH_LONG).show()
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val toppersDir = File(picturesDir, "ToppersAcademy")
            if (!toppersDir.exists()) toppersDir.mkdirs()
            val file = File(toppersDir, filename)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            Toast.makeText(context, "ID Card Image saved to Pictures: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving ID Card image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun printStudentIDCard(
    context: android.content.Context,
    student: Student,
    appLogoUri: String = "",
    principalSignatureUri: String = ""
) {
    try {
        val bitmap = generateIDCardBitmap(context, student, appLogoUri, principalSignatureUri)
        val printHelper = PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
        }
        printHelper.printBitmap("Toppers_Academy_ID_Card_${student.name}", bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error printing ID Card: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

