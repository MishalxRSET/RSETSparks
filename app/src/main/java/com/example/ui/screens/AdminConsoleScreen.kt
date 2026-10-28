package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AuditLogEntity
import com.example.data.model.ReportEntity
import com.example.data.model.UserEntity
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CampusVerifiedGreen
import com.example.ui.theme.CrimsonPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminConsoleScreen(
    currentAdmin: UserEntity,
    reports: List<ReportEntity>,
    allUsers: List<UserEntity>,
    auditLogs: List<AuditLogEntity>,
    onResolveReport: (reportId: String, status: String, adminNote: String, suspendUser: Boolean) -> Unit,
    onToggleSuspension: (userId: String, isSuspended: Boolean, reason: String) -> Unit,
    onToggleVerification: (userId: String, isVerified: Boolean) -> Unit,
    onEmergencyAuditUser: (userId: String, justification: String, (List<String>) -> Unit) -> Unit,
    onToggleAdminBadges: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state for emergency chat audit
    var auditTargetUser by remember { mutableStateOf<UserEntity?>(null) }
    var auditJustification by remember { mutableStateOf("") }
    var auditedTranscriptsResult by remember { mutableStateOf<List<String>?>(null) }

    val pendingCount = reports.count { it.status == "PENDING" || it.status == "INVESTIGATING" }
    val suspendedCount = allUsers.count { it.isSuspended }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Campus Safety Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AmberGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Campus Safety & Moderation",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Rajagiri Institutional Welfare Directorate",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberGold.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold)
                    ) {
                        Text(
                            text = "OFFICER ACCESS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // KPI Metrics Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    KpiMetricItem(label = "Students", value = "${allUsers.size}", color = Color(0xFF38BDF8))
                    KpiMetricItem(label = "Verified", value = "${allUsers.count { it.isVerified }}", color = CampusVerifiedGreen)
                    KpiMetricItem(label = "Flagged", value = "$pendingCount", color = Color(0xFFEF4444))
                    KpiMetricItem(label = "Suspended", value = "$suspendedCount", color = AmberGold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Public Officer Badge Visibility Toggle Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (currentAdmin.showBadges) "Show Badges: ON (Officer Profile)" else "Show Badges: OFF (Student Profile)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (currentAdmin.showBadges)
                                        "Officer badge & title visible to students viewing ${currentAdmin.name}"
                                    else
                                        "Officer badge & title hidden. Displays as standard student ${currentAdmin.name}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Switch(
                            checked = currentAdmin.showBadges,
                            onCheckedChange = { onToggleAdminBadges(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AmberGold
                            ),
                            modifier = Modifier.testTag("admin_console_badge_toggle")
                        )
                    }
                }
            }
        }

        // Tabs
        val tabs = listOf("Incidents ($pendingCount)", "Student Registry", "Audit Trail (${auditLogs.size})")
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> {
                    // Safety Incidents Tab
                    IncidentsQueueView(
                        reports = reports,
                        onResolveReport = onResolveReport
                    )
                }
                1 -> {
                    // Student Registry & Moderation
                    StudentRegistryView(
                        users = allUsers,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onToggleVerification = onToggleVerification,
                        onToggleSuspension = onToggleSuspension,
                        onEmergencyAudit = { target ->
                            auditTargetUser = target
                            auditJustification = ""
                            auditedTranscriptsResult = null
                        }
                    )
                }
                2 -> {
                    // Security Audit Logs
                    AuditTrailView(auditLogs = auditLogs)
                }
            }
        }
    }

    // Emergency Audit Modal
    if (auditTargetUser != null) {
        Dialog(onDismissRequest = { auditTargetUser = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Emergency Chat Investigation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFFEF4444)
                        )
                        IconButton(onClick = { auditTargetUser = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Accessing conversation archives for: ${auditTargetUser?.name} (${auditTargetUser?.email})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Institutional policy requires Campus Safety Officers to specify an official investigation reason before accessing chat archives.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (auditedTranscriptsResult == null) {
                        OutlinedTextField(
                            value = auditJustification,
                            onValueChange = { auditJustification = it },
                            placeholder = { Text("E.g. Serious harassment report received; verifying student distress...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("audit_justification_input"),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val user = auditTargetUser ?: return@Button
                                val reason = auditJustification.ifBlank { "Campus safety incident inquiry" }
                                onEmergencyAuditUser(user.id, reason) { results ->
                                    auditedTranscriptsResult = results
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_emergency_audit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Execute Authorized Audit")
                        }
                    } else {
                        // Display Retrieved Transcripts
                        Text(
                            text = "Decrypted Transcripts Dossier:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (auditedTranscriptsResult!!.isEmpty()) {
                                    Text(
                                        text = "No recorded chat messages for this account.",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    auditedTranscriptsResult!!.forEach { line ->
                                        Text(
                                            text = line,
                                            color = Color(0xFF38BDF8),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { auditTargetUser = null },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Close Dossier")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiMetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun IncidentsQueueView(
    reports: List<ReportEntity>,
    onResolveReport: (reportId: String, status: String, adminNote: String, suspendUser: Boolean) -> Unit
) {
    if (reports.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CampusVerifiedGreen,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Pending Incidents",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Rajagiri campus community is safe and peaceful.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(reports) { report ->
                IncidentReportCard(
                    report = report,
                    onResolve = { status, note, suspendUser ->
                        onResolveReport(report.id, status, note, suspendUser)
                    }
                )
            }
        }
    }
}

@Composable
fun IncidentReportCard(
    report: ReportEntity,
    onResolve: (status: String, note: String, suspendUser: Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var adminNote by remember { mutableStateOf("") }

    val statusColor = when (report.status) {
        "PENDING" -> Color(0xFFEF4444)
        "INVESTIGATING" -> AmberGold
        "RESOLVED_SUSPENDED" -> Color(0xFF7C3AED)
        "RESOLVED_WARNED" -> Color(0xFF2563EB)
        else -> CampusVerifiedGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = report.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                val dateStr = remember(report.createdAt) {
                    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(report.createdAt))
                }
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Reported: ${report.reportedUserName}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Target Account: ${report.reportedEmail}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Filed by: ${report.reporterEmail}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Category: ${report.reasonCategory}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = report.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Secured Chat Transcripts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExpanded) "Hide Secured Chat Transcripts" else "Inspect Secured Chat Transcripts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "OFFICIAL INCIDENT CHAT TRANSCRIPT DOSSIER:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = report.chatTranscriptSnapshot.ifBlank { "No chat history recorded between these users." },
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFF1F5F9),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Moderation Action Controls
                    if (report.status == "PENDING" || report.status == "INVESTIGATING") {
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = adminNote,
                            onValueChange = { adminNote = it },
                            placeholder = { Text("Enter moderation conclusion note...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onResolve("RESOLVED_SUSPENDED", adminNote.ifBlank { "Suspended following transcript verification." }, true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Suspend User", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    onResolve("RESOLVED_WARNED", adminNote.ifBlank { "Formal conduct warning issued to student." }, false)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Warn Student", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    onResolve("DISMISSED", adminNote.ifBlank { "Incident reviewed and dismissed." }, false)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Dismiss", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentRegistryView(
    users: List<UserEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleVerification: (String, Boolean) -> Unit,
    onToggleSuspension: (String, Boolean, String) -> Unit,
    onEmergencyAudit: (UserEntity) -> Unit
) {
    val filteredUsers = users.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.department.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by student name, email or branch...", fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_search_students_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Total Registered Accounts: ${filteredUsers.size}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredUsers) { student ->
                StudentModerationCard(
                    student = student,
                    onToggleVerification = { onToggleVerification(student.id, !student.isVerified) },
                    onToggleSuspension = {
                        val reason = if (!student.isSuspended) "Suspended by Campus Safety Officer" else "Reinstated"
                        onToggleSuspension(student.id, !student.isSuspended, reason)
                    },
                    onEmergencyAudit = { onEmergencyAudit(student) }
                )
            }
        }
    }
}

@Composable
fun StudentModerationCard(
    student: UserEntity,
    onToggleVerification: () -> Unit,
    onToggleSuspension: () -> Unit,
    onEmergencyAudit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (student.isSuspended) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudentAvatar(
                    name = student.name,
                    emoji = student.avatarEmoji,
                    colorIndex = student.avatarColorIndex,
                    size = 46.dp,
                    isVerified = student.isVerified,
                    isAdmin = student.isAdmin
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = student.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (student.isSuspended) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEF4444)
                            ) {
                                Text(
                                    text = "SUSPENDED",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = student.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${student.department} • Warnings: ${student.warningCount}",
                        fontSize = 11.sp,
                        color = if (student.warningCount > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Admin Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Verification toggle
                OutlinedButton(
                    onClick = onToggleVerification,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (student.isVerified) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (student.isVerified) CampusVerifiedGreen else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (student.isVerified) "Verified" else "Unverified",
                        fontSize = 11.sp
                    )
                }

                // Suspension Toggle
                OutlinedButton(
                    onClick = onToggleSuspension,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (student.isSuspended) CampusVerifiedGreen else Color(0xFFEF4444)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (student.isSuspended) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (student.isSuspended) "Reinstate" else "Suspend",
                        fontSize = 11.sp
                    )
                }

                // Emergency Chat Audit Button
                Button(
                    onClick = onEmergencyAudit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Audit Chats",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AuditTrailView(auditLogs: List<AuditLogEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(auditLogs) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.action,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val timeStr = remember(log.timestamp) {
                            SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        }
                        Text(
                            text = timeStr,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Officer: ${log.adminEmail}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Target: ${log.targetUserEmail}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Reason: ${log.reason}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
