package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Installment
import com.example.ui.theme.*
import com.example.ui.viewmodels.EmiViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    viewModel: EmiViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) } // 1: Customer, 2: Product, 3: EMI & Summary

    // ======= STEP 1 STATE: CUSTOMER DETAILS =======
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var cnic by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    // ======= STEP 2 STATE: PRODUCT DETAILS =======
    var productName by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("Electronics") }
    var totalAmountInput by remember { mutableStateOf("") }
    var advancePaymentInput by remember { mutableStateOf("") }

    val totalAmount = totalAmountInput.toDoubleOrNull() ?: 0.0
    val advancePayment = advancePaymentInput.toDoubleOrNull() ?: 0.0
    val remainingBalance = (totalAmount - advancePayment).coerceAtLeast(0.0)

    val categories = listOf("Electronics", "Bikes", "Mobiles", "Appliances", "Furniture", "Other")

    // ======= STEP 3 STATE: EMI PLAN =======
    var emiFrequency by remember { mutableStateOf("Monthly") } // Daily, Weekly, Monthly
    var emiAmountInput by remember { mutableStateOf("") }
    val emiAmount = emiAmountInput.toDoubleOrNull() ?: 0.0

    // Computations
    val totalInstallmentsRequired = if (emiAmount > 0.0 && remainingBalance > 0.0) {
        java.lang.Math.ceil(remainingBalance / emiAmount).toInt()
    } else {
        0
    }

    val estimatedCompletionDate = remember(remainingBalance, emiAmount, emiFrequency) {
        if (totalInstallmentsRequired > 0) {
            val calendar = Calendar.getInstance()
            for (i in 1..totalInstallmentsRequired) {
                when (emiFrequency) {
                    "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                    "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    "Monthly" -> calendar.add(Calendar.MONTH, 1)
                }
            }
            calendar.timeInMillis
        } else {
            System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Installment Book (نیا قسط بک)", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentStep > 1) {
                                currentStep -= 1
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("add_customer_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkObsidianBackground)
            )
        },
        containerColor = DarkObsidianBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step indicator bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepBadge(step = 1, label = "Customer", active = currentStep == 1, completed = currentStep > 1, modifier = Modifier.weight(1f))
                StepBadge(step = 2, label = "Product", active = currentStep == 2, completed = currentStep > 2, modifier = Modifier.weight(1f))
                StepBadge(step = 3, label = "EMI & Plan", active = currentStep == 3, completed = currentStep > 3, modifier = Modifier.weight(1f))
            }

            // Scrollable Content area with animated step switching
            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    1 -> StepCustomerDetails(
                        name = name,
                        onNameChange = { name = it },
                        phone = phone,
                        onPhoneChange = { phone = it },
                        cnic = cnic,
                        onCnicChange = { cnic = it },
                        address = address,
                        onAddressChange = { address = it },
                        photoUri = photoUri,
                        onPickPhoto = { photoLauncher.launch("image/*") }
                    )
                    2 -> StepProductDetails(
                        productName = productName,
                        onProductNameChange = { productName = it },
                        productCategory = productCategory,
                        onProductCategoryChange = { productCategory = it },
                        totalAmountInput = totalAmountInput,
                        onTotalAmountChange = { totalAmountInput = it },
                        advancePaymentInput = advancePaymentInput,
                        onAdvancePaymentChange = { advancePaymentInput = it },
                        remainingBalance = remainingBalance,
                        categories = categories,
                        viewModel = viewModel
                    )
                    3 -> StepEmiPlanAndSummary(
                        name = name,
                        phone = phone,
                        productName = productName,
                        productCategory = productCategory,
                        totalAmount = totalAmount,
                        advancePayment = advancePayment,
                        remainingBalance = remainingBalance,
                        emiFrequency = emiFrequency,
                        onEmiFrequencyChange = { emiFrequency = it },
                        emiAmountInput = emiAmountInput,
                        onEmiAmountChange = { emiAmountInput = it },
                        totalInstallments = totalInstallmentsRequired,
                        completionDate = estimatedCompletionDate,
                        viewModel = viewModel
                    )
                }
            }

            // Navigation Buttons Bottom Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep -= 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.DarkGray),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Back", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        when (currentStep) {
                            1 -> {
                                if (name.isBlank() || phone.isBlank()) {
                                    Toast.makeText(context, "Please enter Customer Name and Phone Number", Toast.LENGTH_SHORT).show()
                                } else {
                                    currentStep = 2
                                }
                            }
                            2 -> {
                                if (productName.isBlank()) {
                                    Toast.makeText(context, "Please enter Product Name", Toast.LENGTH_SHORT).show()
                                } else if (totalAmount <= 0.0) {
                                    Toast.makeText(context, "Please enter a valid Total Amount", Toast.LENGTH_SHORT).show()
                                } else if (advancePayment < 0.0) {
                                    Toast.makeText(context, "Advance Payment cannot be negative", Toast.LENGTH_SHORT).show()
                                } else if (advancePayment > totalAmount) {
                                    Toast.makeText(context, "Advance Payment cannot exceed Total Amount", Toast.LENGTH_SHORT).show()
                                } else {
                                    currentStep = 3
                                }
                            }
                            3 -> {
                                if (emiAmount <= 0.0) {
                                    Toast.makeText(context, "Please enter expected EMI Amount", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.createNewBuyer(
                                        name = name,
                                        phone = phone,
                                        cnic = cnic,
                                        address = address,
                                        photoUri = photoUri,
                                        productName = productName,
                                        productCategory = productCategory,
                                        totalAmount = totalAmount,
                                        advancePayment = advancePayment,
                                        frequency = emiFrequency,
                                        emiAmount = emiAmount,
                                        startDate = System.currentTimeMillis()
                                    )
                                    Toast.makeText(context, "Ledger created successfully for $name!", Toast.LENGTH_LONG).show()
                                    onNavigateBack()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("wizard_next_buttonStep$currentStep"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkEmeraldPrimary)
                ) {
                    val label = if (currentStep == 3) "Create Customer" else "Next"
                    Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StepBadge(
    step: Int,
    label: String,
    active: Boolean,
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        active -> DarkEmeraldPrimary
        completed -> DarkEmeraldPrimary.copy(alpha = 0.3f)
        else -> DarkSurfaceCard
    }
    val contentColor = when {
        active -> Color.White
        completed -> LightMintAccent
        else -> Color.Gray
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Step $step",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun StepCustomerDetails(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    cnic: String,
    onCnicChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    photoUri: String?,
    onPickPhoto: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Customer Details (گاہک کی تفصیلات)",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Photo Upload Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (photoUri != null) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, LightMintAccent, CircleShape)
                    ) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Customer Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray)
                            .clickable(onClick = onPickPhoto),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Add Photo",
                            tint = Color.LightGray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                TextButton(onClick = onPickPhoto) {
                    val label = if (photoUri != null) "Change Photo (تصویر بدلیں)" else "Add Photo Optional (تصویر لگائیں)"
                    Text(label, color = LightMintAccent, fontSize = 14.sp)
                }
            }
        }

        // Text Inputs Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Customer Name (گاہک کا نام) *", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkEmeraldPrimary,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_customer_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone Number (فون نمبر) *", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkEmeraldPrimary,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_customer_phone_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = cnic,
                    onValueChange = onCnicChange,
                    label = { Text("CNIC Optional (شناختی کارڈ)", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkEmeraldPrimary,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = { Text("Address Optional (پتہ)", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkEmeraldPrimary,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

@Composable
fun StepProductDetails(
    productName: String,
    onProductNameChange: (String) -> Unit,
    productCategory: String,
    onProductCategoryChange: (String) -> Unit,
    totalAmountInput: String,
    onTotalAmountChange: (String) -> Unit,
    advancePaymentInput: String,
    onAdvancePaymentChange: (String) -> Unit,
    remainingBalance: Double,
    categories: List<String>,
    viewModel: EmiViewModel
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Product details (پروڈکٹ کی معلومات)",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = onProductNameChange,
                    label = { Text("Product/Item Name (پروڈکٹ کا نام) *", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkEmeraldPrimary,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_product_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Category selector tiles (minimizing typing!)
                Column {
                    Text(
                        text = "Product Category (پروڈکٹ کیٹلاگ)",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = productCategory == cat
                            val borderCol = if (isSelected) LightMintAccent else Color.DarkGray
                            val bgCol = if (isSelected) DarkEmeraldPrimary.copy(alpha = 0.2f) else DarkSurfaceCard

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgCol)
                                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                    .clickable { onProductCategoryChange(cat) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) LightMintAccent else Color.White
                                )
                            }
                        }
                    }
                }

                Divider(color = Color.DarkGray)

                OutlinedTextField(
                    value = totalAmountInput,
                    onValueChange = onTotalAmountChange,
                    label = { Text("Total Retail Price (پوری قیمت - PKR) *", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkEmeraldPrimary,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_total_price_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                val advancePayment = advancePaymentInput.toDoubleOrNull() ?: 0.0
                val totalAmount = totalAmountInput.toDoubleOrNull() ?: 0.0
                val isAdvanceError = advancePayment < 0.0 || advancePayment > totalAmount

                OutlinedTextField(
                    value = advancePaymentInput,
                    onValueChange = onAdvancePaymentChange,
                    label = { Text("Advance Payment (Optional)", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.LocalAtm, contentDescription = null, tint = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isAdvanceError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkEmeraldPrimary,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_advance_price_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                if (advancePayment > totalAmount) {
                    Text(
                        text = "Advance Payment cannot exceed Total Amount",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else if (advancePayment < 0.0) {
                    Text(
                        text = "Advance Payment cannot be negative",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Computed Output
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remaining Balance (باقی رقم):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = viewModel.formatPkr(remainingBalance),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightMintAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepEmiPlanAndSummary(
    name: String,
    phone: String,
    productName: String,
    productCategory: String,
    totalAmount: Double,
    advancePayment: Double,
    remainingBalance: Double,
    emiFrequency: String,
    onEmiFrequencyChange: (String) -> Unit,
    emiAmountInput: String,
    onEmiAmountChange: (String) -> Unit,
    totalInstallments: Int,
    completionDate: Long,
    viewModel: EmiViewModel
) {
    val scrollState = rememberScrollState()
    val emiAmount = emiAmountInput.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select EMI Plan (قسط کا طریقہ کار)",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Frequency selection Row
                Column {
                    Text(
                        text = "Payment Cycle (قسط کی مدت)",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Daily", "Weekly", "Monthly").forEach { freq ->
                            val isSelected = emiFrequency == freq
                            val borderCol = if (isSelected) LightMintAccent else Color.DarkGray
                            val bgCol = if (isSelected) DarkEmeraldPrimary.copy(alpha = 0.2f) else DarkSurfaceCard

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgCol)
                                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                    .clickable { onEmiFrequencyChange(freq) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = freq,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) LightMintAccent else Color.White
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = emiAmountInput,
                    onValueChange = onEmiAmountChange,
                    label = { Text("EMI Installment Amount (قسط کی رقم - PKR) *", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkEmeraldPrimary,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_emi_amount_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // EMI dynamic calculation outputs
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.DarkGray.copy(alpha = 0.25f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DynamicCalculationRow(label = "Total Installments Required:", value = "$totalInstallments Payments")
                    DynamicCalculationRow(
                        label = "Estimated Completion Date:",
                        value = if (totalInstallments > 0) viewModel.formatDate(completionDate) else "N/A"
                    )
                    DynamicCalculationRow(label = "Remaining Balance Payable:", value = viewModel.formatPkr(remainingBalance))
                }
            }
        }

        // Summary before Saving (The Clean Voucher)
        Text(
            text = "Agreement Summary (حساب کا خلاصہ)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, LightMintAccent.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryRow(label = "Client Name (گاہک)", value = name.ifBlank { "N/A" })
                SummaryRow(label = "Contact (موبائل)", value = phone.ifBlank { "N/A" })
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                SummaryRow(label = "Product Chosen (آئٹم)", value = productName.ifBlank { "N/A" })
                SummaryRow(label = "Catalog/Category (کیٹلاگ)", value = productCategory)
                SummaryRow(label = "Total Price (پوری رقم)", value = viewModel.formatPkr(totalAmount))
                SummaryRow(label = "Advance Payment (Optional)", value = viewModel.formatPkr(advancePayment))
                SummaryRow(label = "Payable Balance (باقی رقم)", value = viewModel.formatPkr(remainingBalance), highlight = true)
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                SummaryRow(label = "EMI Plan (قسط کا شیڈول)", value = "$emiFrequency payments")
                SummaryRow(label = "EMI Amount (ہر قسط کی رقم)", value = viewModel.formatPkr(emiAmount), highlight = true)
                SummaryRow(label = "Total Installments (کل قسطیں)", value = "$totalInstallments")
                SummaryRow(label = "Completion Date (آخری قسط کی تاریخ)", value = if (totalInstallments > 0) viewModel.formatDate(completionDate) else "N/A")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DynamicCalculationRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun SummaryRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = if (highlight) LightMintAccent else Color.Gray)
        Text(
            text = value,
            fontSize = if (highlight) 14.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) LightMintAccent else Color.White
        )
    }
}

fun Modifier.fillPadding(dp: Int): Modifier = this
