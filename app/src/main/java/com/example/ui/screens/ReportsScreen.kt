package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
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
import com.example.ui.theme.DarkEmeraldPrimary
import com.example.ui.theme.DarkObsidianBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.LightMintAccent
import com.example.ui.viewmodels.EmiViewModel
import com.example.util.PdfExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: EmiViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val collectionHistory by viewModel.collectionHistory.collectAsState()
    val loansList by viewModel.loans.collectAsState()

    val totalReturned = collectionHistory.sumOf { it.amountReceived }
    val totalPending = loansList.filter { it.status == "ACTIVE" }.sumOf { it.remainingBalance }
    val totalVolume = totalReturned + totalPending

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Recovery Reports", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("reports_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            PdfExporter.generateAndShareEmiReport(
                                context = context,
                                collectionHistory = collectionHistory,
                                totalOutlay = totalVolume,
                                totalRecovered = totalReturned,
                                outstandingActive = totalPending,
                                formatPkr = { viewModel.formatPkr(it) },
                                formatDate = { viewModel.formatDate(it) }
                            )
                        },
                        modifier = Modifier.testTag("reports_export_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export PDF Report", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkObsidianBackground)
            )
        },
        containerColor = DarkObsidianBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General business summary card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Capital Outstanding Summary",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightMintAccent
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Outlay Portfolio:", fontSize = 12.sp, color = Color.Gray)
                            Text(viewModel.formatPkr(totalVolume), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Volume Recovered:", fontSize = 12.sp, color = Color.Gray)
                            Text(viewModel.formatPkr(totalReturned), fontSize = 13.sp, color = DarkEmeraldPrimary, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Outstanding Active Credit:", fontSize = 12.sp, color = Color.Gray)
                            Text(viewModel.formatPkr(totalPending), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Small progress collection bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.DarkGray)
                        ) {
                            val ratio = if (totalVolume > 0) (totalReturned / totalVolume) else 0.0
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = ratio.toFloat())
                                    .background(DarkEmeraldPrimary)
                             )
                        }

                        Text(
                            text = "Collection Efficiency: ${if (totalVolume > 0) String.format("%.1f", (totalReturned/totalVolume*100)) else "0"}%",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                PdfExporter.generateAndShareEmiReport(
                                    context = context,
                                    collectionHistory = collectionHistory,
                                    totalOutlay = totalVolume,
                                    totalRecovered = totalReturned,
                                    outstandingActive = totalPending,
                                    formatPkr = { viewModel.formatPkr(it) },
                                    formatDate = { viewModel.formatDate(it) }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("reports_download_pdf_card_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Report PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Secondary header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions Log",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filters", tint = Color.Gray)
                }
            }

            if (collectionHistory.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Assignment, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No payment receipts recorded yet.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(collectionHistory) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.customerName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Payment: ${item.itemName} (#${item.installmentNumber})",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Date: ${viewModel.formatDate(item.receiptDate)} | Via: ${item.paymentMethod}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = viewModel.formatPkr(item.amountReceived),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightMintAccent
                                )
                                Text(
                                    text = "SUCCESS",
                                    fontSize = 9.sp,
                                    color = DarkEmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
