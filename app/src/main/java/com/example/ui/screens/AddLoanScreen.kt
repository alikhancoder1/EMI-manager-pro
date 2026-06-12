package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Customer
import com.example.ui.theme.DarkEmeraldPrimary
import com.example.ui.theme.DarkObsidianBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.LightMintAccent
import com.example.ui.viewmodels.EmiViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    viewModel: EmiViewModel,
    customerId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var customer by remember { mutableStateOf<Customer?>(null) }

    var itemName by remember { mutableStateOf("") }
    var modelNo by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var downPayment by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("0") } // Markup %
    
    var paymentFrequency by remember { mutableStateOf("Monthly") } // Daily, Weekly, Monthly
    val frequencies = listOf("Daily", "Weekly", "Monthly")
    
    var totalInstallments by remember { mutableStateOf("6") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(customerId) {
        customer = viewModel.getCustomerDirect(customerId)
    }

    // Auto EMI calculations preview flow based on inputs
    val pPrice = purchasePrice.toDoubleOrNull() ?: 0.0
    val dPayment = downPayment.toDoubleOrNull() ?: 0.0
    val markupRate = interestRate.toDoubleOrNull() ?: 0.0
    val instCount = totalInstallments.toIntOrNull() ?: 1

    val previewList = remember(pPrice, dPayment, paymentFrequency, instCount, startDate, markupRate) {
        val outstanding = pPrice - dPayment
        val markupAmount = outstanding * (markupRate / 100.0)
        viewModel.previewInstallmentSchedule(
            purchasePrice = pPrice + markupAmount,
            downPayment = dPayment,
            frequency = paymentFrequency,
            count = instCount,
            startDate = startDate
        )
    }

    val finalMaturityDate = previewList.lastOrNull()?.dueDate ?: startDate

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Formulate EMI Schedule", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_loan_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkObsidianBackground)
            )
        },
        containerColor = DarkObsidianBackground
    ) { innerPadding ->
        if (customer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DarkEmeraldPrimary)
            }
        } else {
            val user = customer!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer details banner
                Text(
                    text = "Buyer: ${user.name} | CNIC: ${user.cnic}",
                    fontSize = 13.sp,
                    color = LightMintAccent,
                    fontWeight = FontWeight.Bold
                )

                // Scrollable fields but hold preview static at bottom or keep everything in LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Purchase Item Specifications",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                OutlinedTextField(
                                    value = itemName,
                                    onValueChange = { itemName = it },
                                    label = { Text("Product/Item Name", color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DarkEmeraldPrimary,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("loan_item_name_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    placeholder = { Text("e.g. Honda 125CC Motorcycle", color = Color.DarkGray) }
                                )

                                OutlinedTextField(
                                    value = modelNo,
                                    onValueChange = { modelNo = it },
                                    label = { Text("Model or Registration Number", color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DarkEmeraldPrimary,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("loan_model_input"),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Financial Matrix (PKR)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = purchasePrice,
                                        onValueChange = { purchasePrice = it },
                                        label = { Text("Retail Price", color = Color.Gray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DarkEmeraldPrimary,
                                            unfocusedBorderColor = Color.DarkGray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.LightGray
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("loan_price_input"),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        val isAdvanceError = dPayment < 0.0 || dPayment > pPrice
                                        OutlinedTextField(
                                            value = downPayment,
                                            onValueChange = { downPayment = it },
                                            isError = isAdvanceError,
                                            label = { Text("Advance Payment (Optional)", color = Color.Gray) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DarkEmeraldPrimary,
                                                unfocusedBorderColor = Color.DarkGray,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.LightGray,
                                                errorBorderColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("loan_downpayment_input"),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        if (dPayment > pPrice) {
                                            Text(
                                                text = "Advance Payment cannot exceed Total Amount",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                            )
                                        } else if (dPayment < 0.0) {
                                            Text(
                                                text = "Advance Payment cannot be negative",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = interestRate,
                                        onValueChange = { interestRate = it },
                                        label = { Text("Markup / Interest %", color = Color.Gray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DarkEmeraldPrimary,
                                            unfocusedBorderColor = Color.DarkGray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.LightGray
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("loan_makeup_input"),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    OutlinedTextField(
                                        value = totalInstallments,
                                        onValueChange = { totalInstallments = it },
                                        label = { Text("Shed installments count", color = Color.Gray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DarkEmeraldPrimary,
                                            unfocusedBorderColor = Color.DarkGray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.LightGray
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("loan_installment_count"),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Installment Frequency Schedule",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                // Simple Tab selection or custom outline selection row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    frequencies.forEach { freq ->
                                        val selected = paymentFrequency == freq
                                        Button(
                                            onClick = { paymentFrequency = freq },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selected) DarkEmeraldPrimary else Color.DarkGray,
                                                contentColor = if (selected) Color.White else Color.LightGray
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("frequency_tab_$freq")
                                        ) {
                                            Text(text = freq, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Start Date selection Dialog
                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        cal.timeInMillis = startDate
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val selCal = Calendar.getInstance()
                                                selCal.set(year, month, dayOfMonth)
                                                startDate = selCal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LightMintAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("loan_datepicker_action")
                                ) {
                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Plan Start: ${viewModel.formatDate(startDate)}")
                                }

                                Text(
                                    text = "Auto Maturity/Completion Date: ${viewModel.formatDate(finalMaturityDate)}",
                                    color = LightMintAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Installment Preview Generation",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (previewList.isEmpty()) {
                        item {
                            Text("Formulate pricing variables above to generate preview matrix.", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        items(previewList) { installment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Installment #${installment.installmentNumber}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = viewModel.formatPkr(installment.amountDue),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Due: ${viewModel.formatDate(installment.dueDate)}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Final commit button
                Button(
                    onClick = {
                        if (itemName.isBlank() || pPrice <= 0 || instCount <= 0) {
                            Toast.makeText(context, "Please correctly specify Product, Retail Price and installment cycles.", Toast.LENGTH_LONG).show()
                        } else if (dPayment < 0.0) {
                            Toast.makeText(context, "Advance Payment cannot be negative", Toast.LENGTH_LONG).show()
                        } else if (dPayment > pPrice) {
                            Toast.makeText(context, "Advance Payment cannot exceed Total Amount", Toast.LENGTH_LONG).show()
                        } else {
                            viewModel.createLoan(
                                customerId = customerId,
                                customerName = user.name,
                                itemName = itemName,
                                modelNo = modelNo,
                                purchasePrice = pPrice,
                                downPayment = dPayment,
                                interestRate = markupRate,
                                frequency = paymentFrequency,
                                count = instCount,
                                startDate = startDate
                            )
                            Toast.makeText(context, "EMI schedule created successfully.", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_schedule_master_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Build & Save Schedule Ledger",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
