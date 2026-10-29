package com.example.ui.screens

import android.content.Context
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
import androidx.compose.material3.TextButton
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
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.model.UserEntity
import com.example.data.model.UserProfile
import com.example.ui.theme.CampusVerifiedGreen
import com.example.ui.theme.CrimsonPrimary
import java.io.File

/**
 * Profile setup shown once, after the student's Rajagiri email is verified.
 */
@Composable
fun OnboardingScreen(
    email: String,
    suggestedName: String,
    onSubmit: (profile: UserProfile, onError: (String) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) = ProfileFormScreen(
    email = email,
    existing = null,
    suggestedName = suggestedName,
    onSubmit = onSubmit,
    secondaryActionLabel = "Use a different account",
    onSecondaryAction = onSignOut,
    modifier = modifier
)

/** Lets a signed-in student change their profile details and photo. */
@Composable
fun EditProfileScreen(
    user: UserEntity,
    onSave: (profile: UserProfile, onError: (String) -> Unit) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    notice: String? = null,
    cancelLabel: String = "Cancel"
) = ProfileFormScreen(
    email = user.email,
    existing = user,
    suggestedName = user.name,
    onSubmit = onSave,
    secondaryActionLabel = cancelLabel,
    onSecondaryAction = onCancel,
    modifier = modifier,
    notice = notice
)

/**
 * Shared profile form: photo (camera or photo picker), name, age (18+), department, year, bio and
 * interests. [existing] switches it from onboarding to edit mode.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileFormScreen(
    email: String,
    existing: UserEntity?,
    suggestedName: String,
    onSubmit: (profile: UserProfile, onError: (String) -> Unit) -> Unit,
    secondaryActionLabel: String,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    notice: String? = null
) {
    val context = LocalContext.current
    val editing = existing != null

    var name by remember { mutableStateOf(suggestedName) }
    var age by remember { mutableStateOf(existing?.age?.toString().orEmpty()) }
    var department by remember { mutableStateOf(existing?.department ?: "B.Tech Computer Science") }
    var year by remember { mutableStateOf(existing?.year?.takeIf { it > 0 }?.toString().orEmpty()) }
    var bio by remember { mutableStateOf(existing?.bio.orEmpty()) }
    var photoUriString by remember { mutableStateOf(existing?.photoUri) }
    var gender by remember { mutableStateOf(existing?.gender.orEmpty()) }
    var showMe by remember { mutableStateOf(existing?.showMe.orEmpty()) }
    var orientation by remember { mutableStateOf(existing?.orientation.orEmpty()) }
    var lookingFor by remember { mutableStateOf(existing?.lookingFor.orEmpty()) }
    var pronouns by remember { mutableStateOf(existing?.pronouns.orEmpty()) }
    var customInterestInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
        mutableStateOf(existing?.getInterestsList()?.toSet() ?: setOf())
    }

    val departments = listOf(
        "B.Tech Computer Science",
        "B.Tech AI & Data Science",
        "B.Tech Electronics & Comm",
        "B.Tech Electrical & Electronics",
        "B.Tech Mechanical Engg",
        "B.Tech Civil Engg",
        "B.Tech Information Tech",
        "MBA",
        "MCA",
        "M.Tech",
        "Other"
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

    // Gallery Photo Picker Fallback (Zero permissions required via Android Photo Picker)
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUriString = uri.toString()
            Toast.makeText(context, "Photo selected from device gallery!", Toast.LENGTH_SHORT).show()
        }
    }

    val rootModifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 20.dp)

    Column(
        modifier = rootModifier
    ) {
        if (notice != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = notice,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (editing) "Edit your profile" else "Set up your profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Signed in as $email",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (BuildConfig.BLAZE) { // photos need a Cloud Storage bucket (Blaze plan)
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
                        text = "Optional. Use a clear photo of yourself; it's visible to other verified students.",
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
                                // The system camera app captures the photo, so no CAMERA permission is needed.
                                val uri = createCameraUri()
                                if (uri != null) {
                                    pendingCameraUri = uri
                                    cameraLauncher.launch(uri)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✓ Profile picture attached",
                                fontSize = 11.sp,
                                color = CampusVerifiedGreen,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { photoUriString = null },
                                modifier = Modifier.testTag("remove_photo_button")
                            ) { Text("Remove", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                        }
                    }
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

        // Full Name Field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(UserProfile.MAX_NAME_LENGTH) },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_name_input"),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Age and Year Row
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = age,
                onValueChange = { age = it.filter(Char::isDigit).take(2) },
                label = { Text("Age (18+)") },
                isError = age.isNotEmpty() && (age.toIntOrNull() ?: 0) < UserProfile.MIN_AGE,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
                value = year,
                onValueChange = { year = it.filter(Char::isDigit).take(4) },
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
            color = MaterialTheme.colorScheme.primary
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

        OptionChips("Gender", null, UserProfile.GENDERS, gender, optional = false, tag = "reg_gender") { gender = it }
        Spacer(modifier = Modifier.height(14.dp))
        OptionChips("Show me", "Whose profiles appear in your Sparks deck", UserProfile.SHOW_ME, showMe, optional = false, tag = "reg_show_me") { showMe = it }
        Spacer(modifier = Modifier.height(14.dp))
        OptionChips("Orientation", "Optional · shown on your profile", UserProfile.ORIENTATIONS, orientation, optional = true, tag = "reg_orientation") { orientation = it }
        Spacer(modifier = Modifier.height(14.dp))
        OptionChips("Looking for", "Optional · shown on your profile", UserProfile.LOOKING_FOR, lookingFor, optional = true, tag = "reg_looking_for") { lookingFor = it }
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = pronouns,
            onValueChange = { if (it.length <= UserProfile.MAX_PRONOUNS_LENGTH) pronouns = it },
            label = { Text("Pronouns (optional)") },
            placeholder = { Text("e.g. she/her") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_pronouns_input"),
            shape = RoundedCornerShape(14.dp)
        )

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
            onValueChange = { if (it.length <= UserProfile.MAX_BIO_LENGTH) bio = it },
            placeholder = { Text("e.g. AI researcher by day, filter coffee enthusiast at the campus cafeteria! ☕💻") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_bio_input"),
            shape = RoundedCornerShape(14.dp),
            minLines = 3,
            maxLines = 4,
            supportingText = {
                Text(
                    text = "${bio.length}/${UserProfile.MAX_BIO_LENGTH}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontSize = 11.sp
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campus Interests Section (Multi-Select Chip Tags)
        Text(
            text = "Interests",
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

        if (!editing) Text(
            text = "By continuing you confirm you are 18 or older and agree to the Terms of Use and Privacy Policy.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val profile = UserProfile(
                    name = name.trim(),
                    bio = bio.trim(),
                    interests = selectedInterests.value.toList(),
                    photoUri = photoUriString,
                    department = department,
                    year = year.toIntOrNull() ?: 0,
                    age = age.toIntOrNull() ?: 0,
                    gender = gender,
                    showMe = showMe,
                    orientation = orientation,
                    lookingFor = lookingFor,
                    pronouns = pronouns.trim()
                )
                errorMessage = UserProfile.validate(profile)
                if (errorMessage == null) {
                    onSubmit(profile) { errorMessage = it }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_registration_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (editing) "Save Changes" else "Start Sparking",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSecondaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_form_secondary_button")
        ) {
            Text(text = secondaryActionLabel, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Single-choice chips in the department style. Optional groups clear when the selected chip is tapped again. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChips(
    title: String,
    hint: String?,
    options: Map<String, String>,
    selected: String,
    optional: Boolean,
    tag: String,
    onSelect: (String) -> Unit
) {
    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
    if (hint != null) Text(text = hint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = selected == key
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .testTag("${tag}_$key")
                    .clickable { onSelect(if (isSelected && optional) "" else key) }
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
