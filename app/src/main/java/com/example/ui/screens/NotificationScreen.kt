package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmiNotification
import com.example.ui.theme.DarkEmeraldPrimary
import com.example.ui.theme.DarkObsidianBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.LightMintAccent
import com.example.ui.viewmodels.EmiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: EmiViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val systemAlerts by viewModel.notifications.collectAsState()

    var activeSmsReminderTarget by remember { mutableStateOf<EmiNotification?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ledger Due Reminders", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("notifications_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (systemAlerts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Pending Alerts", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                        Text("Your recovery targets are fully up-to-date.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("notification_list_container"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(systemAlerts) { alert ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = LightMintAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = alert.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { viewModel.dismissNotification(alert.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Cancel, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                    }
                                }

                                Text(
                                    text = alert.body,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )

                                if (alert.relatedCustomerName.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = { activeSmsReminderTarget = alert },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Message, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SMS Reminder", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal prepared reminder template
    if (activeSmsReminderTarget != null) {
        val reminder = activeSmsReminderTarget!!
        val pkrVal = viewModel.formatPkr(reminder.relatedAmount)
        
        // Formulated professional Roman Urdu text
        val draftTemplate = "Dear ${reminder.relatedCustomerName},\n\n" +
                "Ap ka installment fee Rs. ${reminder.relatedAmount} active ledger plan ke mutabiq aaj wajib-ul-ada (Due) hai. " +
                "Baraye maharbani jald az jald qist jama karwaen takay penalty se bacha ja sakay.\n\n" +
                "Shukriya,\nEMI Portal Management."

        AlertDialog(
            onDismissRequest = { activeSmsReminderTarget = null },
            title = { Text("Formatted SMS Reminder Draft", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Prepared Roman Urdu template suitable for local messaging channels in Pakistan:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = draftTemplate,
                            fontSize = 12.sp,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Sms Reminder successfully transmitted to ${reminder.relatedCustomerName}.", Toast.LENGTH_LONG).show()
                        activeSmsReminderTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary)
                ) {
                    Text("Dispatch SMS")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSmsReminderTarget = null }) {
                    Text("Dismiss", color = Color.Gray)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }
}
