package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Customer
import com.example.data.Loan
import com.example.ui.theme.*
import com.example.ui.viewmodels.EmiViewModel
import java.util.Calendar

fun getStartOfDayMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: EmiViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddCustomer: () -> Unit
) {
    val customersList by viewModel.customers.collectAsState()
    val loansList by viewModel.loans.collectAsState()
    val installmentsList by viewModel.allInstallments.collectAsState()
    val activeFilter by viewModel.currentFilter.collectAsState()
    var searchPhrase by remember { mutableStateOf("") }

    val filteredList = remember(customersList, loansList, installmentsList, activeFilter, searchPhrase) {
        customersList.filter { customer ->
            // Search match
            val matchesSearch = customer.name.contains(searchPhrase, ignoreCase = true) ||
                    customer.phone.contains(searchPhrase, ignoreCase = true) ||
                    customer.cnic.contains(searchPhrase, ignoreCase = true)
            if (!matchesSearch) return@filter false

            val customerLoan = loansList.find { it.customerId == customer.id }

            when (activeFilter) {
                "Due" -> {
                    val todayStart = getStartOfDayMillis()
                    val todayEnd = todayStart + (24 * 60 * 60 * 1000)
                    installmentsList.any {
                        it.loanId == customerLoan?.id &&
                        it.status != "PAID" &&
                        it.dueDate in todayStart..todayEnd
                    }
                }
                "Pending" -> {
                    customerLoan != null && customerLoan.remainingBalance > 0.0 && customerLoan.status == "ACTIVE"
                }
                else -> true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Books (کھاتہ ڈائریکٹری)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("customers_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkObsidianBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddCustomer,
                containerColor = DarkEmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Register Buyer", modifier = Modifier.size(24.dp))
            }
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
            // Tab segment selector matching the Main Menu categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All" to "All books", "Due" to "Due Today", "Pending" to "Pending").forEach { (key, titleName) ->
                    val isSelected = activeFilter == key
                    val borderCol = if (isSelected) LightMintAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    val bgCol = if (isSelected) DarkEmeraldPrimary.copy(alpha = 0.2f) else DarkSurfaceCard

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgCol)
                            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                            .clickable { viewModel.currentFilter.value = key }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = titleName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) LightMintAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Search bar input
            OutlinedTextField(
                value = searchPhrase,
                onValueChange = { searchPhrase = it },
                placeholder = { Text("Search by name, phone or CNIC...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkEmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_search_phrasing"),
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Customers Found",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Try clearing search or filters",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("customers_lazy_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { customer ->
                        val customerLoan = loansList.find { it.customerId == customer.id }
                        CustomerListItemCard(
                            customer = customer,
                            loan = customerLoan,
                            viewModel = viewModel,
                            onClick = { onNavigateToDetail(customer.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerListItemCard(
    customer: Customer,
    loan: Loan?,
    viewModel: EmiViewModel,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_row_${customer.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!customer.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = customer.photoUri,
                        contentDescription = customer.name,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.dp, LightMintAccent, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkEmeraldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.name.take(1).uppercase(),
                            color = LightMintAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = customer.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Phone: ${customer.phone}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (loan != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Product Description", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(loan.itemName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EMI Frequency", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${loan.paymentFrequency}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LightMintAccent)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(viewModel.formatPkr(loan.purchasePrice), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EMI Amount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(viewModel.formatPkr(loan.installmentAmount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SoftGold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Remaining Bal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(viewModel.formatPkr(loan.remainingBalance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CoralWarning)
                    }
                }

                val credit = (loan.purchasePrice - loan.downPayment).coerceAtLeast(1.0)
                val ratio = (1.0 - (loan.remainingBalance / credit)).coerceIn(0.0, 1.0)
                val percent = ratio * 100

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Progress (حساب مکمل)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${String.format("%.0f", percent)}% Recouped", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LightMintAccent)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = ratio.toFloat())
                                .background(LightMintAccent)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active installment ledger files.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: EmiViewModel,
    customerId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddLoan: (String) -> Unit,
    onNavigateToLoanDetail: (String) -> Unit
) {
    var customer by remember { mutableStateOf<Customer?>(null) }
    val loansList by viewModel.loans.collectAsState()

    val customerLoans = loansList.filter { it.customerId == customerId }

    LaunchedEffect(customerId) {
        customer = viewModel.getCustomerDirect(customerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "Customer Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("customer_detail_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
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
            val validCustomer = customer!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with photo and quick stats
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!validCustomer.photoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = validCustomer.photoUri,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, LightMintAccent, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .background(DarkEmeraldPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = validCustomer.name.take(1).uppercase(),
                                        color = LightMintAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 44.sp
                                    )
                                }
                            }

                            Text(
                                text = validCustomer.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = "Contact Number: ${validCustomer.phone}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Personal Details and Guarantor references
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.DarkGray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_info_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Buyer Credentials (گاہک ریکارڈ)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightMintAccent
                            )

                            DetailRow(icon = Icons.Default.Badge, label = "CNIC Number", value = validCustomer.cnic.ifBlank { "Not provided" })
                            DetailRow(icon = Icons.Default.Home, label = "Residence", value = validCustomer.address.ifBlank { "Not provided" })

                            Divider(color = Color.DarkGray)

                            Text(
                                text = "Zamanatdar References (ضامن دہندہ)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightMintAccent
                            )

                            DetailRow(icon = Icons.Default.AssignmentInd, label = "Guarantor Name", value = validCustomer.guarantorName.ifBlank { "Not recorded" })
                            DetailRow(icon = Icons.Default.ContactPhone, label = "Guarantor Phone", value = validCustomer.guarantorPhone.ifBlank { "Not recorded" })
                        }
                    }
                }

                // Header for active books
                item {
                    Text(
                        text = "Purchase/EMI Ledgers (قسط فائل)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (customerLoans.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("This customer has no active EMI contracts.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(customerLoans) { loan ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.DarkGray),
                            onClick = { onNavigateToLoanDetail(loan.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_loan_${loan.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Receipt,
                                            contentDescription = null,
                                            tint = LightMintAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = loan.itemName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (loan.remainingBalance <= 0) DarkEmeraldPrimary.copy(alpha = 0.2f)
                                                else CoralWarning.copy(alpha = 0.2f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (loan.remainingBalance <= 0) "COMPLETED" else "ACTIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (loan.remainingBalance <= 0) DarkEmeraldPrimary else CoralWarning
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                    Column {
                                        Text("Total Bill Price", fontSize = 10.sp, color = Color.Gray)
                                        Text(viewModel.formatPkr(loan.purchasePrice), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Advanced Amount", fontSize = 10.sp, color = Color.Gray)
                                        Text(viewModel.formatPkr(loan.downPayment), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Balance Due", fontSize = 10.sp, color = Color.Gray)
                                        Text(viewModel.formatPkr(loan.remainingBalance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CoralWarning)
                                    }
                                }

                                Divider(color = Color.DarkGray)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Catalog Category", fontSize = 10.sp, color = Color.Gray)
                                        Text(loan.productCategory.ifBlank { "General" }, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Start Date", fontSize = 10.sp, color = Color.Gray)
                                        Text(viewModel.formatDate(loan.startDate), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Maturity Target Date", fontSize = 10.sp, color = Color.Gray)
                                        Text(viewModel.formatDate(loan.maturityDate), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LightMintAccent)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Plan Frequency", fontSize = 10.sp, color = Color.Gray)
                                        Text("${loan.paymentFrequency} payments", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("EMI Rate", fontSize = 10.sp, color = Color.Gray)
                                        Text(viewModel.formatPkr(loan.installmentAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SoftGold)
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
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
