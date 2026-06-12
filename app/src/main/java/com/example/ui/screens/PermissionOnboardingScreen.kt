package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.theme.DarkEmeraldPrimary
import com.example.ui.theme.DarkObsidianBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.LightMintAccent
import com.example.ui.viewmodels.EmiViewModel

enum class OnboardingState {
    WELCOME,
    PERMISSIONS
}

data class PermissionItem(
    val id: String,
    val permissionString: String?,
    val title: String,
    val description: String,
    val detailedRationale: String,
    val icon: ImageVector,
    val isMandatory: Boolean
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PermissionOnboardingScreen(
    viewModel: EmiViewModel,
    onOnboardingCompleted: () -> Unit
) {
    val context = LocalContext.current
    var currentState by remember { mutableStateOf(OnboardingState.WELCOME) }
    
    // Track runtime permissions
    var permissionStatuses by remember {
        mutableStateOf(getInitialPermissionStatuses(context))
    }

    // Refresh permission statuses whenever view is shown or state is focused
    LaunchedEffect(currentState) {
        permissionStatuses = getInitialPermissionStatuses(context)
    }

    // Active permission being requested (for showing rationale modals if denied)
    var rationalePermissionItem by remember { mutableStateOf<PermissionItem?>(null) }
    var permanentlyDeniedItem by remember { mutableStateOf<PermissionItem?>(null) }

    // Launcher for requesting individual permission
    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        rationalePermissionItem?.let { item ->
            val updated = permissionStatuses.toMutableMap()
            updated[item.id] = isGranted
            permissionStatuses = updated

            if (!isGranted) {
                // If user denied, check if they permanently denied
                val activity = context as? Activity
                val permission = item.permissionString
                val shouldShowRationale = permission?.let {
                    activity?.let { act ->
                        ActivityCompat.shouldShowRequestPermissionRationale(act, it)
                    }
                } ?: false

                if (!shouldShowRationale) {
                    // Permanently denied (shouldShowRequestPermissionRationale returns false and was not granted)
                    permanentlyDeniedItem = item
                }
            } else {
                // Granted, clear any rationales
                rationalePermissionItem = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidianBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() with
                slideOutHorizontally { width -> -width } + fadeOut()
            }
        ) { targetState ->
            when (targetState) {
                OnboardingState.WELCOME -> {
                    WelcomeLayout(
                        onNext = { currentState = OnboardingState.PERMISSIONS }
                    )
                }
                OnboardingState.PERMISSIONS -> {
                    PermissionsDashboard(
                        permissionStatuses = permissionStatuses,
                        onRequestPermission = { item ->
                            if (item.permissionString != null) {
                                rationalePermissionItem = item
                                singlePermissionLauncher.launch(item.permissionString)
                            }
                        },
                        onFinish = {
                            // Save completion in SharedPreferences
                            val prefs = context.getSharedPreferences("emi_tracker_settings", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("permissions_completed", true).apply()
                            onOnboardingCompleted()
                        }
                    )
                }
            }
        }

        // Rationale Dialog for standard Denial
        if (rationalePermissionItem != null && permissionStatuses[rationalePermissionItem!!.id] == false) {
            val item = rationalePermissionItem!!
            AlertDialog(
                onDismissRequest = { rationalePermissionItem = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = DarkEmeraldPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Permission Required",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "${item.title} access is highly recommended for the app to function properly.",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.detailedRationale,
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary),
                        onClick = {
                            val perm = item.permissionString
                            if (perm != null) {
                                singlePermissionLauncher.launch(perm)
                            }
                        }
                    ) {
                        Text("Grant Permission", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { rationalePermissionItem = null }
                    ) {
                        Text("Dismiss anyway", color = Color.Gray)
                    }
                },
                containerColor = DarkSurfaceCard,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Dialog for Permanently Denied settings redirection
        if (permanentlyDeniedItem != null) {
            val item = permanentlyDeniedItem!!
            AlertDialog(
                onDismissRequest = { permanentlyDeniedItem = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Permission Configured Off",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "You have permanently disabled ${item.title}. This stops EMI Tracker from using its features.",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${item.detailedRationale}\n\nPlease click the button below to open system App Settings, and give permissions manually.",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary),
                        onClick = {
                            permanentlyDeniedItem = null
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Open App Settings", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { permanentlyDeniedItem = null }
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = DarkSurfaceCard,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun WelcomeLayout(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Aesthetic app visual icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(DarkEmeraldPrimary, LightMintAccent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Welcome to EMI Tracker",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your premium companion for secure installment bookkeeping, client portfolios, automatic reminders, and reliable ledger-audit database synchronizations.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3232))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = DarkEmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "We value your Privacy & Trust",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "We only request permissions that are absolutely critical to deliver the premium experience you expect. No unnecessary background tracking.",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("onboarding_start_button"),
            colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Configure App Access",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun PermissionsDashboard(
    permissionStatuses: Map<String, Boolean>,
    onRequestPermission: (PermissionItem) -> Unit,
    onFinish: () -> Unit
) {
    val items = getPermissionItemsList()
    
    // Core (mandatory) are Notifications and SMS
    val mandatoryPermissionsGranted = items
        .filter { it.isMandatory && it.permissionString != null }
        .all { permissionStatuses[it.id] == true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "App Requirements",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Permissions keep features functional and fully secured. Configure each carefully.",
            fontSize = 13.sp,
            color = Color.LightGray,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                val isGranted = permissionStatuses[item.id] == true
                PermissionCard(
                    item = item,
                    isGranted = isGranted,
                    onClick = {
                        if (!isGranted) {
                            onRequestPermission(item)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!mandatoryPermissionsGranted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFFAB40),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configure Push Notifications and SMS receipts to complete setup.",
                    fontSize = 11.sp,
                    color = Color(0xFFFFAB40)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onFinish,
            enabled = mandatoryPermissionsGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("onboarding_finish_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkEmeraldPrimary,
                disabledContainerColor = Color(0xFF225248)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (mandatoryPermissionsGranted) "Enter Dashboard" else "Finish Core Settings First",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (mandatoryPermissionsGranted) Color.White else Color.Gray
            )
        }
    }
}

@Composable
fun PermissionCard(
    item: PermissionItem,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted, onClick = onClick)
            .testTag("permission_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isGranted) DarkEmeraldPrimary.copy(alpha = 0.4f) else Color(0xFF2E3232)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Permission Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) DarkEmeraldPrimary.copy(alpha = 0.15f) else Color(0xFF242727)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (isGranted) DarkEmeraldPrimary else Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    if (item.isMandatory) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF5252).copy(alpha = 0.1f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Required",
                                fontSize = 9.sp,
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action / Status Indicator
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = DarkEmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF225248).copy(alpha = 0.3f))
                        .border(1.dp, DarkEmeraldPrimary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Grant",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkEmeraldPrimary
                    )
                }
            }
        }
    }
}

fun getPermissionItemsList(): List<PermissionItem> {
    return listOf(
        PermissionItem(
            id = "net",
            permissionString = null, // auto-granted
            title = "Network Sync & Cloud Backup",
            description = "Active for seamless backups and recoveries.",
            detailedRationale = "Allows the ledger account to back up dynamic database state records to encrypted Firestore servers.",
            icon = Icons.Default.Cloud,
            isMandatory = false
        ),
        PermissionItem(
            id = "notifications",
            permissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                null
            },
            title = "Push Notification Alerts",
            description = "Used for EMI reminders and payment alerts.",
            detailedRationale = "Dispatches local system banners, transaction feedback reminders, and installment notification schedules so portfolios never go overdue.",
            icon = Icons.Default.Notifications,
            isMandatory = true
        ),
        PermissionItem(
            id = "sms",
            permissionString = Manifest.permission.SEND_SMS,
            title = "SMS Broadcasts & Receipts",
            description = "Used to prepare payment receipt messages for customers.",
            detailedRationale = "Drafts and directly templates customized SMS payments and ledger ledger-receipts to clients right from your mobile device.",
            icon = Icons.Default.Sms,
            isMandatory = true
        ),
        PermissionItem(
            id = "contacts",
            permissionString = Manifest.permission.READ_CONTACTS,
            title = "Contacts Integration",
            description = "Quick customer imports (Optional feature).",
            detailedRationale = "Allows populating user portfolio entries from your contact address book directly inside transaction screen builders.",
            icon = Icons.Default.ImportContacts,
            isMandatory = false
        )
    )
}

fun getInitialPermissionStatuses(context: Context): Map<String, Boolean> {
    val statuses = mutableMapOf<String, Boolean>()
    
    // Internet and Network state are normal permissions, implicitly granted via AndroidManifest.
    statuses["net"] = true

    // Notifications state
    val hasNotifPermString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }
    
    statuses["notifications"] = if (hasNotifPermString == null) {
        true
    } else {
        ContextCompat.checkSelfPermission(context, hasNotifPermString) == PackageManager.PERMISSION_GRANTED
    }

    // SMS status
    statuses["sms"] = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    // Contacts status
    statuses["contacts"] = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    return statuses
}
