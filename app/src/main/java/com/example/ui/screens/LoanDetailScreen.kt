package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CollectionLog
import com.example.data.Customer
import com.example.data.Installment
import com.example.data.Loan
import com.example.ui.theme.*
import com.example.ui.viewmodels.EmiViewModel
import kotlinx.coroutines.launch

data class SuccessReceiptInfo(
    val customerName: String,
    val productName: String,
    val amountPaid: Double,
    val paymentDate: String,
    val paymentTime: String,
    val remainingBalance: Double,
    val customerPhone: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    viewModel: EmiViewModel,
    loanId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var loan by remember { mutableStateOf<Loan?>(null) }
    var customer by remember { mutableStateOf<Customer?>(null) }
    val installments by viewModel.getLoanInstallments(loanId).collectAsState(initial = emptyList())
    val collectionsLog by viewModel.getLoanCollections(loanId).collectAsState(initial = emptyList())

    // Active Dialog flags
    var showLoosePaymentDialog by remember { mutableStateOf(false) }
    var selectedInstallmentForPayment by remember { mutableStateOf<Installment?>(null) }
    var showInvoiceViewer by remember { mutableStateOf(false) }
    var activeSuccessReceipt by remember { mutableStateOf<SuccessReceiptInfo?>(null) }

    LaunchedEffect(loanId) {
        val l = viewModel.getLoanDirect(loanId)
        loan = l
        if (l != null) {
            customer = viewModel.getCustomerDirect(l.customerId)
        }
    }

    // Dynamic calculations
    val sortedInstallments = installments.sortedBy { it.installmentNumber }
    val totalPaidCycles = installments.count { it.status == "PAID" }
    val totalCycles = installments.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loan?.itemName ?: "EMI Ledger Sheet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("loan_detail_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInvoiceViewer = true },
                        modifier = Modifier.testTag("share_payment_receipt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Invoice Share", tint = LightMintAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkObsidianBackground)
            )
        },
        containerColor = DarkObsidianBackground
    ) { innerPadding ->
        if (loan == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DarkEmeraldPrimary)
            }
        } else {
            val activeLoan = loan!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header overview Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.DarkGray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(
                                        text = activeLoan.customerName,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Item: ${activeLoan.itemName}",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LightMintAccent.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${activeLoan.paymentFrequency} Plan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LightMintAccent
                                    )
                                }
                            }

                            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Retail Price", fontSize = 11.sp, color = Color.Gray)
                                    Text(viewModel.formatPkr(activeLoan.purchasePrice), fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Advance Down", fontSize = 11.sp, color = Color.Gray)
                                    Text(viewModel.formatPkr(activeLoan.downPayment), fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Status", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        text = activeLoan.status,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeLoan.status == "ACTIVE") SoftGold else DarkEmeraldPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Highlighted total outstanding balance
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("REMAINING BALANCE (کل بقایا رقم)", fontSize = 11.sp, color = Color.LightGray)
                                        Text(
                                            text = viewModel.formatPkr(activeLoan.remainingBalance),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CoralWarning
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Installment Rate", fontSize = 11.sp, color = Color.LightGray)
                                        Text(
                                            text = "${viewModel.formatPkr(activeLoan.installmentAmount)} / Cycle",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Primary Action Button for Shopkeepers: QUICK COLLECT LOOSE PAYMENTS
                            Button(
                                onClick = { showLoosePaymentDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("quick_loose_payment_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Collect Loose Payment (قسط وصول کریں)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Amortization Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EMI Installment List (${totalPaidCycles}/${totalCycles} Paid)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Click row to pay custom",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                if (sortedInstallments.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No installment timeline generated.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(sortedInstallments) { inst ->
                        val isPaid = inst.status == "PAID"
                        val isPartial = inst.status == "PARTIAL"

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.DarkGray),
                            onClick = {
                                if (!isPaid) {
                                    selectedInstallmentForPayment = inst
                                } else {
                                    Toast.makeText(context, "This installment has been fully paid.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("installment_row_${inst.installmentNumber}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPaid) DarkEmeraldPrimary.copy(alpha = 0.2f)
                                            else if (isPartial) SoftGold.copy(alpha = 0.2f)
                                            else Color.DarkGray
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${inst.installmentNumber}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPaid) DarkEmeraldPrimary else if (isPartial) SoftGold else Color.LightGray
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = viewModel.formatPkr(inst.amountDue),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Due: ${viewModel.formatDate(inst.dueDate)}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    if (inst.amountPaid > 0) {
                                        Text(
                                            text = "Recovered: ${viewModel.formatPkr(inst.amountPaid)}",
                                            fontSize = 11.sp,
                                            color = LightMintAccent,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isPaid) DarkEmeraldPrimary.copy(alpha = 0.15f)
                                            else if (isPartial) SoftGold.copy(alpha = 0.15f)
                                            else CoralWarning.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = inst.status,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPaid) DarkEmeraldPrimary else if (isPartial) SoftGold else CoralWarning
                                    )
                                }
                            }
                        }
                    }
                }

                // Recovery History Logs
                if (collectionsLog.isNotEmpty()) {
                    item {
                        Text(
                            text = "Payment Collection History",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(collectionsLog.sortedByDescending { it.receiptDate }) { receipt ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.DarkGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Receipt: ${receipt.id.take(8).uppercase()}",
                                        color = LightMintAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = viewModel.formatDate(receipt.receiptDate),
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Amount: Rs. ${viewModel.formatDecimal(receipt.amountReceived)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "Cycle #${receipt.installmentNumber}", fontSize = 10.sp, color = LightMintAccent)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Modal Loose payment Dialog (Collect General Cash payments from earliest unpaid sequence)
    if (showLoosePaymentDialog) {
        var paymentAmountInput by remember { mutableStateOf("") }
        val paymentDate = System.currentTimeMillis()

        AlertDialog(
            onDismissRequest = { showLoosePaymentDialog = false },
            title = {
                Text(
                    text = "Quick Collect Cash (رقم وصول کریں)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter loose cash payment. Loose Cash automatically pays off outstanding balances starting from the oldest due cycle.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = { Text("Payment Amount (PKR) *", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkEmeraldPrimary,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loose_payment_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paying = paymentAmountInput.toDoubleOrNull() ?: 0.0
                        if (paying <= 0.0) {
                            Toast.makeText(context, "Enter a valid positive cash payment.", Toast.LENGTH_SHORT).show()
                        } else {
                            coroutineScope.launch {
                                viewModel.collectFlexiblePayment(
                                    loanId = loanId,
                                    amountPaid = paying,
                                    paymentDate = paymentDate
                                )
                                showLoosePaymentDialog = false

                                // Reload local state values
                                val l = viewModel.getLoanDirect(loanId)
                                loan = l
                                val cust = if (l != null) viewModel.getCustomerDirect(l.customerId) else null
                                customer = cust

                                val now = System.currentTimeMillis()
                                val df = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                                val tf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                val dateStr = df.format(java.util.Date(now))
                                val timeStr = tf.format(java.util.Date(now))

                                if (l != null) {
                                    activeSuccessReceipt = SuccessReceiptInfo(
                                        customerName = l.customerName,
                                        productName = l.itemName,
                                        amountPaid = paying,
                                        paymentDate = dateStr,
                                        paymentTime = timeStr,
                                        remainingBalance = l.remainingBalance,
                                        customerPhone = cust?.phone ?: ""
                                    )
                                }
                                Toast.makeText(context, "Loose payment of PKR $paying recorded!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary)
                ) {
                    Text("Record Receipt", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoosePaymentDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }

    // Modal Specific Cycle payment Dialog (Collect custom amount against one specific period)
    if (selectedInstallmentForPayment != null) {
        val activeInst = selectedInstallmentForPayment!!
        var paymentAmountInput by remember { mutableStateOf((activeInst.amountDue - activeInst.amountPaid).toString()) }

        AlertDialog(
            onDismissRequest = { selectedInstallmentForPayment = null },
            title = {
                Text(
                    text = "Log Receipt for Cycle #${activeInst.installmentNumber}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter custom amount to apply directly to billing cycle #${activeInst.installmentNumber}.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = { Text("Amount Received (PKR) *", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkEmeraldPrimary,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cycle_payment_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paying = paymentAmountInput.toDoubleOrNull() ?: 0.0
                        if (paying <= 0.0) {
                            Toast.makeText(context, "Enter a valid positive paid amount.", Toast.LENGTH_SHORT).show()
                        } else {
                            coroutineScope.launch {
                                viewModel.collectInstallmentPayment(
                                    loanId = activeInst.loanId,
                                    installmentId = activeInst.id,
                                    installmentNum = activeInst.installmentNumber,
                                    customerName = loan?.customerName ?: "Customer",
                                    itemName = loan?.itemName ?: "Product",
                                    payingAmount = paying,
                                    paymentMethod = "Cash",
                                    transactionRef = "",
                                    receivedBy = "Shopkeeper"
                                )
                                selectedInstallmentForPayment = null

                                // Reload local state values
                                val l = viewModel.getLoanDirect(loanId)
                                loan = l
                                val cust = if (l != null) viewModel.getCustomerDirect(l.customerId) else null
                                customer = cust

                                val now = System.currentTimeMillis()
                                val df = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                                val tf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                val dateStr = df.format(java.util.Date(now))
                                val timeStr = tf.format(java.util.Date(now))

                                if (l != null) {
                                    activeSuccessReceipt = SuccessReceiptInfo(
                                        customerName = l.customerName,
                                        productName = l.itemName,
                                        amountPaid = paying,
                                        paymentDate = dateStr,
                                        paymentTime = timeStr,
                                        remainingBalance = l.remainingBalance,
                                        customerPhone = cust?.phone ?: ""
                                    )
                                }
                                Toast.makeText(context, "Payment logged against installment #${activeInst.installmentNumber}!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary)
                ) {
                    Text("Apply Payment", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedInstallmentForPayment = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }

    // Modal Invoice Sharing Overlay
    if (showInvoiceViewer && loan != null) {
        val invoiceLoan = loan!!
        AlertDialog(
            onDismissRequest = { showInvoiceViewer = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ledger invoice Voucher", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = LightMintAccent)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "--------------------------------------------\n" +
                               "EMI ACCORD STATEMENT RECORD\n" +
                               "--------------------------------------------",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Client Name: ${invoiceLoan.customerName}\n" +
                               "Product: ${invoiceLoan.itemName}\n" +
                               "Total Price: Rs. ${viewModel.formatDecimal(invoiceLoan.purchasePrice)}\n" +
                               "Down Deposit: Rs. ${viewModel.formatDecimal(invoiceLoan.downPayment)}\n" +
                               "Debt Due: Rs. ${viewModel.formatDecimal(invoiceLoan.remainingBalance)}\n" +
                               "Installments remaining: ${installments.count { it.status != "PAID" }} cycle(s)",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "--------------------------------------------\n" +
                               "Secure local database snapshot.",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color.DarkGray,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Invoice copy compiled successfully and shared.", Toast.LENGTH_SHORT).show()
                        showInvoiceViewer = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary)
                ) {
                    Text("Share Voucher")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInvoiceViewer = false }) {
                    Text("Close", color = Color.Gray)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }

    // Modal SMS Receipt Saved Popup Flow
    if (activeSuccessReceipt != null) {
        val receipt = activeSuccessReceipt!!
        val amountPaidFormatted = viewModel.formatDecimal(receipt.amountPaid)
        val remainingBalanceFormatted = viewModel.formatDecimal(receipt.remainingBalance)
        val receiptMessage = """
Dear ${receipt.customerName},
Payment Received Successfully.
Product: ${receipt.productName}
Amount Paid: Rs. $amountPaidFormatted
Date: ${receipt.paymentDate}
Time: ${receipt.paymentTime}
Remaining Balance: Rs. $remainingBalanceFormatted
Thank you for your payment.
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { activeSuccessReceipt = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = DarkEmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Payment Saved!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "The payment has been recorded successfully. Choose how to send the receipt details to the customer:",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )

                    // Receipt Preview Box style
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "RECEIPT TEXT PREVIEW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightMintAccent,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = receiptMessage,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }
                    }

                    if (receipt.customerPhone.isBlank()) {
                        Text(
                            text = "⚠ Customer has no registered phone number to prefill.",
                            color = CoralWarning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Send SMS receipt through Native Composer
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:${receipt.customerPhone}")
                                    putExtra("sms_body", receiptMessage)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        type = "vnd.android-dir/mms-sms"
                                        putExtra("address", receipt.customerPhone)
                                        putExtra("sms_body", receiptMessage)
                                    }
                                    context.startActivity(intent)
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "Could not open default native SMS app.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_sms_receipt_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Sms, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send SMS Receipt", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Share Receipt globally
                    OutlinedButton(
                        onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, receiptMessage)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Receipt Via"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open share menu.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("share_receipt_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LightMintAccent),
                        border = BorderStroke(1.dp, LightMintAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = LightMintAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Receipt", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { activeSuccessReceipt = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Dismiss", color = Color.Gray)
                    }
                }
            },
            containerColor = DarkSurfaceCard
        )
    }
}
