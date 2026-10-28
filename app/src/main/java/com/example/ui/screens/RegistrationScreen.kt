package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.security.FirebaseAuthHelper
import com.example.ui.theme.CampusVerifiedGreen
import com.example.ui.theme.CrimsonPrimary
import java.io.File

/**
 * Registration screen enabling Rajagiri students to create their verified profile:
 * - Profile picture capture via camera with gallery photo picker fallback
 * - Student bio, department, and graduation year
 * - Campus interest selection using multi-select chip tags
 * - Links with Firebase Auth account with strict @rajagiri.edu.in domain enforcement
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationScreen(
    initialEmail: String = "",
    initialName: String = "",
    onRegisterComplete: (UserProfile) -> Unit,
    onGoogleSignInRequest: () -> Unit = {},
    modifier: Modifier = Modifier,
    scrollable: Boolean = false
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf(initialEmail) }
    var name by remember { mutableStateOf(initialName) }
    var age by remember { mutableStateOf("21") }
    var department by remember { mutableStateOf("B.Tech Computer Science") }
    var year by remember { mutableStateOf("2026") }
    var bio by remember { mutableStateOf("") }
    var photoUriString by remember { mutableStateOf<String?>(null) }
    var customInterestInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Multi-select campus interest chip tags
    val defaultInterests = remember {
        listOf(
            "Robotics",
            "Hackathons",
            "Filter Coffee",
            "Coding & AI",
            "Campus Music",
            "Badminton",
            "Startups",
            "Photography",
            "Literature & Quizzing",
            "Gaming",
            "Open Source",
            "Design & Arts",
            "Canteen Vibes",
            "IEEE Club",
            "CSI Chapter"
        )
    }

    val selectedInterests = remember {
        mutableStateOf(mutableSetOf("Robotics", "Hackathons", "Filter Coffee"))
    }

    val departments = listOf(
        "B.Tech Computer Science",
        "B.Tech AI & Data Science",
        "B.Tech Electronics & Comm",
        "B.Tech Mechanical Engg",
        "B.Tech Information Tech",
        "MBA International Business",
        "MCA (Cloud & Cyber Security)"
    )

    // Helper to generate a content URI for camera output
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun createCameraUri(): Uri? {
        return try {
            val imagesDir = File(context.cacheDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val imageFile = File.createTempFile("avatar_snap_", ".jpg", imagesDir)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to create camera file: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            photoUriString = pendingCameraUri.toString()
            Toast.makeText(context, "Profile picture captured successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createCameraUri()
            if (uri != null) {
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            Toast.makeText(
                context,
                "Camera permission denied. You can pick a photo from your gallery instead.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Gallery Photo Picker Fallback (Zero permissions required via Android Photo Picker)
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUriString = uri.toString()
            Toast.makeText(context, "Photo selected from device gallery!", Toast.LENGTH_SHORT).show()
        }
    }

    val isEmailDomainValid = FirebaseAuthHelper.isRajagiriEmail(email)

    val rootModifier = if (scrollable) {
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    }

    Column(
        modifier = rootModifier
    ) {
        // Institutional Verification Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CrimsonPrimary.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Official Rajagiri Campus Network",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CrimsonPrimary
                    )
                    Text(
                        text = "Registration restricted strictly to @rajagiri.edu.in accounts.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Profile Picture Upload Section (Camera with Gallery Fallback)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Profile Picture",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Capture with live camera or select from device gallery",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Preview Box
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(3.dp, CrimsonPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUriString != null) {
                        AsyncImage(
                            model = photoUriString,
                            contentDescription = "Selected Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Avatar",
                            tint = CrimsonPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Camera and Gallery Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Take Photo (Camera)
                    Button(
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            )
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                val uri = createCameraUri()
                                if (uri != null) {
                                    pendingCameraUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("camera_capture_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Camera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Gallery Fallback
                    OutlinedButton(
                        onClick = {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gallery_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Gallery", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (photoUriString != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ Profile picture attached",
                        fontSize = 11.sp,
                        color = CampusVerifiedGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Student Account Details
        Text(
            text = "Student Details",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Institutional Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Institutional Email (@rajagiri.edu.in)") },
            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
            trailingIcon = {
                if (isEmailDomainValid) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified Domain",
                        tint = CampusVerifiedGreen
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_email_input"),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonPrimary,
                focusedLabelColor = CrimsonPrimary
            )
        )

        if (email.isNotBlank() && !isEmailDomainValid) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⚠️ Access restricted: Email must end with @rajagiri.edu.in",
                fontSize = 11.sp,
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Full Name Field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_name_input"),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonPrimary,
                focusedLabelColor = CrimsonPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Age and Year Row
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Grad Year") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Department Selection
        Text(
            text = "Branch / Department",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = CrimsonPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            departments.forEach { dept ->
                val isSelected = department == dept
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { department = dept }
                ) {
                    Text(
                        text = dept,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Student Bio Field
        Text(
            text = "Campus Bio",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Share what you're passionate about, your campus routine, or favorite spots",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= 250) bio = it },
            placeholder = { Text("e.g. AI researcher by day, filter coffee enthusiast at the campus cafeteria! ☕💻") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_bio_input"),
            shape = RoundedCornerShape(14.dp),
            minLines = 3,
            maxLines = 4,
            supportingText = {
                Text(
                    text = "${bio.length}/250",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontSize = 11.sp
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campus Interests Section (Multi-Select Chip Tags)
        Text(
            text = "Campus Interests (Multi-select Chip Tags)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Select tags to match with like-minded students across campus",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val allInterests = (defaultInterests + selectedInterests.value).distinct()
            allInterests.forEach { interestTag ->
                val isSelected = selectedInterests.value.contains(interestTag)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val updated = selectedInterests.value.toMutableSet()
                        if (isSelected) {
                            updated.remove(interestTag)
                        } else {
                            updated.add(interestTag)
                        }
                        selectedInterests.value = updated
                    },
                    label = {
                        Text(
                            text = interestTag,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrimsonPrimary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom Interest Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customInterestInput,
                onValueChange = { customInterestInput = it },
                placeholder = { Text("Add custom tag (e.g. Badminton)") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_interest_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val tag = customInterestInput.trim()
                    if (tag.isNotBlank()) {
                        val updated = selectedInterests.value.toMutableSet()
                        updated.add(tag)
                        selectedInterests.value = updated
                        customInterestInput = ""
                    }
                })
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val tag = customInterestInput.trim()
                    if (tag.isNotBlank()) {
                        val updated = selectedInterests.value.toMutableSet()
                        updated.add(tag)
                        selectedInterests.value = updated
                        customInterestInput = ""
                    }
                },
                modifier = Modifier
                    .background(CrimsonPrimary, RoundedCornerShape(12.dp))
                    .size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add custom interest tag",
                    tint = Color.White
                )
            }
        }

        // Error message banner
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFEE2E2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFF991B1B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Link with Firebase Auth & Submit Profile
        Button(
            onClick = {
                if (!isEmailDomainValid) {
                    errorMessage = "Access Restricted: Institutional email must end with @rajagiri.edu.in"
                    return@Button
                }
                if (name.isBlank()) {
                    errorMessage = "Please enter your student name."
                    return@Button
                }
                if (selectedInterests.value.isEmpty()) {
                    errorMessage = "Please select at least one campus interest tag."
                    return@Button
                }

                isSubmitting = true
                errorMessage = null

                val profile = UserProfile(
                    id = "", // Will be populated with Firebase UID during sync
                    email = email.trim().lowercase(),
                    name = name.trim(),
                    bio = bio.trim().ifBlank { "Excited to meet fellow Rajagiri students! 👋" },
                    interests = selectedInterests.value.toList(),
                    photoUri = photoUriString,
                    department = department,
                    year = year.toIntOrNull() ?: 2026,
                    age = age.toIntOrNull() ?: 21,
                    isVerified = true
                )

                onRegisterComplete(profile)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_registration_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
            shape = RoundedCornerShape(16.dp),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Registering & Linking...", fontWeight = FontWeight.Bold)
            } else {
                Text(
                    text = "Register Verified Student Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Google Sign-In Link Alternative
        OutlinedButton(
            onClick = onGoogleSignInRequest,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reg_google_signin_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = CrimsonPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fast-Track with Google Account (@rajagiri.edu.in)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CrimsonPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
