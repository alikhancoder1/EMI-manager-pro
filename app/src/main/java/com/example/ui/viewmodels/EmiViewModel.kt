package com.example.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.auth.AuthManager
import com.example.data.auth.UserProfile
import com.example.data.database.AppDatabase
import com.example.data.repository.EmiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class EmiViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = EmiRepository(
        application,
        database.customerDao(),
        database.loanDao(),
        database.installmentDao(),
        database.collectionDao()
    )

    val authManager = AuthManager(application)
    val currentUser: StateFlow<UserProfile?> = authManager.currentUser
    val logoutInProgress = MutableStateFlow(false)

    // Theme state
    val themePreference = MutableStateFlow("SYSTEM")

    // Backup state
    private val _syncState = MutableStateFlow<String>("") // "", "LOADING", "SUCCESS", "ERROR"
    val syncState: StateFlow<String> = _syncState

    private val _syncMessage = MutableStateFlow<String>("")
    val syncMessage: StateFlow<String> = _syncMessage

    // Notifications state
    private val _notifications = MutableStateFlow<List<EmiNotification>>(emptyList())
    val notifications: StateFlow<List<EmiNotification>> = _notifications

    // Derive active user ID for continuous reactive binds
    val currentUserId: Flow<String?> = currentUser.map { it?.uid }

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers: StateFlow<List<Customer>> = currentUserId
        .flatMapLatest { uid ->
            if (uid != null) repository.getCustomers(uid) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val loans: StateFlow<List<Loan>> = currentUserId
        .flatMapLatest { uid ->
            if (uid != null) repository.getLoans(uid) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentFilter = MutableStateFlow<String>("All")

    @OptIn(ExperimentalCoroutinesApi::class)
    val allInstallments: StateFlow<List<Installment>> = currentUserId
        .flatMapLatest { uid ->
            if (uid != null) repository.getInstallments(uid) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val collectionHistory: StateFlow<List<CollectionLog>> = currentUserId
        .flatMapLatest { uid ->
            if (uid != null) repository.getCollections(uid) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val dashboardStats: StateFlow<DashboardStats> = currentUserId
        .flatMapLatest { uid ->
            if (uid != null) repository.getDashboardStats(uid) else flowOf(DashboardStats())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    init {
        // Load local theme setting
        val prefs = application.getSharedPreferences("emi_tracker_settings", android.content.Context.MODE_PRIVATE)
        themePreference.value = prefs.getString("theme", "SYSTEM") ?: "SYSTEM"

        generateDemoDataIfEmpty()
        loadDefaultNotifications()

        // Reactive automatic cloud synchronization flow
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null && !user.isDemoAccount) {
                    val userLoans = database.loanDao().getAllLoans(user.uid).first()
                    // Fetch theme configuration from Firebase Settings
                    fetchThemePreferenceFromCloud(user.uid)
                    if (userLoans.isEmpty()) {
                        Log.d("EmiViewModel", "Auto-Restoring user data from Firebase Cloud Firestore...")
                        syncRestore()
                    } else {
                        Log.d("EmiViewModel", "Auto-Backing up user data to Firebase Cloud Firestore...")
                        syncBackup()
                    }
                }
            }
        }
    }

    // Helper functions
    fun formatPkr(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "PK"))
        // PKR symbol inside normal display is Rs.
        return "Rs. " + NumberFormat.getNumberInstance(Locale.US).format(amount)
    }

    fun formatDate(timestamp: Long): String {
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    // Sign Actions
    fun loginDemo(email: String, name: String) {
        authManager.loginWithGoogleDemo(email, name)
    }

    fun loginGoogle(email: String, name: String) {
        authManager.loginWithGoogle(email, name)
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            logoutInProgress.value = true
            repository.clearLocalData()
            authManager.logout()
            _syncState.value = ""
            _syncMessage.value = ""
            _notifications.value = emptyList()
            logoutInProgress.value = false
            onComplete()
        }
    }

    // Customer Actions
    fun addNewCustomer(name: String, phone: String, cnic: String, address: String, gName: String, gPhone: String) {
        val ownerId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            val customer = Customer(
                name = name,
                phone = phone,
                cnic = cnic,
                address = address,
                guarantorName = gName,
                guarantorPhone = gPhone,
                ownerId = ownerId
            )
            repository.saveCustomer(customer)
        }
    }

    fun removeCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    fun getLoansForCustomer(customerId: String): Flow<List<Loan>> {
        return repository.getLoansForCustomer(customerId)
    }

    suspend fun getCustomerDirect(id: String): Customer? {
        return repository.getCustomerById(id)
    }

    suspend fun getLoanDirect(id: String): Loan? {
        return repository.getLoanById(id)
    }

    fun getLoanInstallments(loanId: String): Flow<List<Installment>> {
        return repository.getLoanDetailInstallments(loanId)
    }

    fun getLoanCollections(loanId: String): Flow<List<CollectionLog>> {
        return repository.getLoanDetailCollections(loanId)
    }

    fun formatDecimal(amount: Double): String {
        return NumberFormat.getNumberInstance(Locale.US).format(amount)
    }

    fun previewNewBuyerSchedule(
        remainingBalance: Double,
        emiAmount: Double,
        frequency: String,
        startDate: Long
    ): List<Installment> {
        if (remainingBalance <= 0 || emiAmount <= 0) return emptyList()
        val count = java.lang.Math.ceil(remainingBalance / emiAmount).toInt()
        val list = mutableListOf<Installment>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate

        var remaining = remainingBalance
        for (i in 1..count) {
            val amountDue = if (remaining >= emiAmount) emiAmount else remaining
            remaining -= amountDue

            when (frequency) {
                "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
            }
            list.add(
                Installment(
                    id = "preview_$i",
                    loanId = "",
                    installmentNumber = i,
                    dueDate = calendar.timeInMillis,
                    amountDue = amountDue,
                    amountPaid = 0.0,
                    status = "UNPAID",
                    ownerId = ""
                )
            )
        }
        return list
    }

    fun createNewBuyer(
        name: String,
        phone: String,
        cnic: String,
        address: String,
        photoUri: String?,
        productName: String,
        productCategory: String,
        totalAmount: Double,
        advancePayment: Double,
        frequency: String,
        emiAmount: Double,
        startDate: Long = System.currentTimeMillis()
    ) {
        val ownerId = currentUser.value?.uid ?: return
        viewModelScope.launch {
            val customerId = UUID.randomUUID().toString()
            val customer = Customer(
                id = customerId,
                name = name,
                phone = phone,
                cnic = cnic,
                address = address,
                guarantorName = "",
                guarantorPhone = "",
                ownerId = ownerId,
                photoUri = photoUri
            )
            repository.saveCustomer(customer)

            val remainingBalance = (totalAmount - advancePayment).coerceAtLeast(0.0)
            val count = if (emiAmount > 0) java.lang.Math.ceil(remainingBalance / emiAmount).toInt() else 1
            val previewItems = previewNewBuyerSchedule(remainingBalance, emiAmount, frequency, startDate)
            val maturityDate = previewItems.lastOrNull()?.dueDate ?: startDate

            val loanId = UUID.randomUUID().toString()
            val loan = Loan(
                id = loanId,
                customerId = customerId,
                customerName = name,
                itemName = productName,
                modelNo = "",
                purchasePrice = totalAmount,
                downPayment = advancePayment,
                interestRate = 0.0,
                outstandingAmount = remainingBalance,
                remainingBalance = remainingBalance,
                paymentFrequency = frequency,
                totalInstallments = count,
                installmentAmount = emiAmount,
                startDate = startDate,
                maturityDate = maturityDate,
                ownerId = ownerId,
                productCategory = productCategory
            )

            val installmentsList = previewItems.mapIndexed { index, inst ->
                Installment(
                    id = UUID.randomUUID().toString(),
                    loanId = loanId,
                    installmentNumber = index + 1,
                    dueDate = inst.dueDate,
                    amountDue = inst.amountDue,
                    amountPaid = 0.0,
                    status = "UNPAID",
                    ownerId = ownerId
                )
            }

            repository.saveLoanWithSchedule(loan, installmentsList)

            addNotification(
                title = "New Buyer Added",
                body = "Customer $name added for $productName ($frequency EMI: Rs. $emiAmount).",
                customerName = name,
                amount = remainingBalance
            )
        }
    }

    suspend fun collectFlexiblePayment(
        loanId: String,
        amountPaid: Double,
        paymentDate: Long = System.currentTimeMillis()
    ) {
        val ownerId = currentUser.value?.uid ?: return
        val loan = repository.getLoanById(loanId) ?: return
        val customerName = loan.customerName
        val itemName = loan.itemName

        // 1. Fetch installments for this loan ordered by number
        val installmentsList = repository.getLoanDetailInstallments(loanId).first()
            .sortedBy { it.installmentNumber }

        var remainingPayment = amountPaid
        val updatedInstallments = mutableListOf<Installment>()
        var firstAffectedInstallmentNum = 1

        for (inst in installmentsList) {
            if (remainingPayment <= 0) break
            val due = inst.amountDue
            val paid = inst.amountPaid
            val left = (due - paid).coerceAtLeast(0.0)

            if (left > 0) {
                firstAffectedInstallmentNum = inst.installmentNumber
                if (remainingPayment >= left) {
                    updatedInstallments.add(
                        inst.copy(
                            amountPaid = due,
                            status = "PAID",
                            paymentDate = paymentDate
                        )
                    )
                    remainingPayment -= left
                } else {
                    updatedInstallments.add(
                        inst.copy(
                            amountPaid = paid + remainingPayment,
                            status = "PARTIAL",
                            paymentDate = paymentDate
                        )
                    )
                    remainingPayment = 0.0
                }
            }
        }

        // Save updated installments to database
        if (updatedInstallments.isNotEmpty()) {
            repository.updateInstallmentsBatch(updatedInstallments)
        }

        // 2. Log collection
        val collectionLog = CollectionLog(
            loanId = loanId,
            installmentNumber = firstAffectedInstallmentNum,
            customerName = customerName,
            itemName = itemName,
            amountReceived = amountPaid,
            receiptDate = paymentDate,
            paymentMethod = "Cash",
            transactionRef = "",
            receivedBy = "Shopkeeper",
            ownerId = ownerId
        )
        repository.recordRawCollectionOnly(collectionLog)

        // 3. Update loan remaining balance and dynamic completion date
        val rawRemaining = loan.remainingBalance - amountPaid
        val newRemaining = rawRemaining.coerceAtLeast(0.0)
        val loanStatus = if (newRemaining <= 0.0) "COMPLETED" else "ACTIVE"

        // Recalculate maturity date based on remaining installments left
        val installmentsLeft = if (loan.installmentAmount > 0) {
            java.lang.Math.ceil(newRemaining / loan.installmentAmount).toInt()
        } else {
            0
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = paymentDate
        for (j in 1..installmentsLeft) {
            when (loan.paymentFrequency) {
                "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
            }
        }
        val newMaturityDate = calendar.timeInMillis

        val updatedLoan = loan.copy(
            remainingBalance = newRemaining,
            status = loanStatus,
            maturityDate = newMaturityDate
        )
        repository.updateLoanDirect(updatedLoan)

        addNotification(
            title = "Payment Received",
            body = "Paid PKR ${formatDecimal(amountPaid)} by $customerName. New Balance: PKR ${formatDecimal(newRemaining)}",
            customerName = customerName,
            amount = amountPaid
        )
    }

    // Schedule generation logic
    fun previewInstallmentSchedule(
        purchasePrice: Double,
        downPayment: Double,
        frequency: String,
        count: Int,
        startDate: Long
    ): List<Installment> {
        val outstanding = purchasePrice - downPayment
        if (outstanding <= 0 || count <= 0) return emptyList()

        val amountPerPeriod = outstanding / count
        val list = mutableListOf<Installment>()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate

        for (i in 1..count) {
            when (frequency) {
                "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
            }
            list.add(
                Installment(
                    id = "preview_$i",
                    loanId = "",
                    installmentNumber = i,
                    dueDate = calendar.timeInMillis,
                    amountDue = amountPerPeriod,
                    amountPaid = 0.0,
                    status = "UNPAID",
                    ownerId = ""
                )
            )
        }
        return list
    }

    // Core Loan creation saving logic
    fun createLoan(
        customerId: String,
        customerName: String,
        itemName: String,
        modelNo: String,
        purchasePrice: Double,
        downPayment: Double,
        interestRate: Double,
        frequency: String,
        count: Int,
        startDate: Long
    ) {
        val ownerId = currentUser.value?.uid ?: return
        val markup = (purchasePrice - downPayment) * (interestRate / 100.0)
        val outstanding = (purchasePrice - downPayment) + markup
        val amountPerPeriod = outstanding / count

        // Auto date calculations
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        
        val previewItems = previewInstallmentSchedule(purchasePrice + markup, downPayment, frequency, count, startDate)
        val maturityDate = previewItems.lastOrNull()?.dueDate ?: startDate

        viewModelScope.launch {
            val loanId = UUID.randomUUID().toString()
            val loan = Loan(
                id = loanId,
                customerId = customerId,
                customerName = customerName,
                itemName = itemName,
                modelNo = modelNo,
                purchasePrice = purchasePrice,
                downPayment = downPayment,
                interestRate = interestRate,
                outstandingAmount = outstanding,
                remainingBalance = outstanding,
                paymentFrequency = frequency,
                totalInstallments = count,
                installmentAmount = amountPerPeriod,
                startDate = startDate,
                maturityDate = maturityDate,
                ownerId = ownerId
            )

            val installmentsList = previewItems.mapIndexed { index, inst ->
                Installment(
                    id = UUID.randomUUID().toString(),
                    loanId = loanId,
                    installmentNumber = index + 1,
                    dueDate = inst.dueDate,
                    amountDue = amountPerPeriod,
                    amountPaid = 0.0,
                    status = "UNPAID",
                    ownerId = ownerId
                )
            }

            repository.saveLoanWithSchedule(loan, installmentsList)
            
            // Generate a notification for success!
            addNotification(
                title = "Loan Created",
                body = "EMI schedule formulated for $customerName ($itemName).",
                customerName = customerName,
                amount = outstanding
            )
        }
    }

    // Payment Processing Action
    suspend fun collectInstallmentPayment(
        loanId: String,
        installmentId: String,
        installmentNum: Int,
        customerName: String,
        itemName: String,
        payingAmount: Double,
        paymentMethod: String,
        transactionRef: String,
        receivedBy: String
    ) {
        val ownerId = currentUser.value?.uid ?: return
        val paymentLog = CollectionLog(
            loanId = loanId,
            installmentNumber = installmentNum,
            customerName = customerName,
            itemName = itemName,
            amountReceived = payingAmount,
            paymentMethod = paymentMethod,
            transactionRef = transactionRef,
            receivedBy = receivedBy,
            ownerId = ownerId
        )

        // Auto check if fully paid
        val installments = repository.getLoanDetailInstallments(loanId).first()
        val inst = installments.find { it.id == installmentId }
        val isFully = if (inst != null) (inst.amountPaid + payingAmount >= inst.amountDue) else true

        repository.recordCollectionPayment(
            collection = paymentLog,
            installmentId = installmentId,
            newPaidAmount = payingAmount,
            isFullyPaid = isFully
        )

        addNotification(
            title = "Payment Received",
            body = "Received Rs. $payingAmount from $customerName for installment #$installmentNum.",
            customerName = customerName,
            amount = payingAmount
        )
    }

    // Cloud Backups triggers
    fun syncBackup() {
        val uid = currentUser.value?.uid ?: return
        _syncState.value = "LOADING"
        _syncMessage.value = "Uploading ledgers securely from SQLite to Firestore cloud..."

        repository.backupToCloud(
            ownerId = uid,
            onSuccess = {
                _syncState.value = "SUCCESS"
                _syncMessage.value = "Backup Completed! All records backed up to cloud. Linked to Google account: ${currentUser.value?.email}"
            },
            onError = { err ->
                _syncState.value = "ERROR"
                _syncMessage.value = err
            }
        )
    }

    fun syncRestore() {
        val uid = currentUser.value?.uid ?: return
        _syncState.value = "LOADING"
        _syncMessage.value = "Downloading records from Firestore. Overwriting SQLite database..."

        repository.restoreFromCloud(
            ownerId = uid,
            onSuccess = {
                _syncState.value = "SUCCESS"
                _syncMessage.value = "Restore Successful! Local databases updated in real-time."
                fetchThemePreferenceFromCloud(uid)
            },
            onError = { err ->
                _syncState.value = "ERROR"
                _syncMessage.value = err
            }
        )
    }

    fun clearSyncMessage() {
        _syncState.value = ""
        _syncMessage.value = ""
    }

    // Notification Helpers
    private fun loadDefaultNotifications() {
        _notifications.value = listOf(
            EmiNotification(
                title = "Daily Recovery Alert",
                body = "Good morning! 3 Daily installments are due today. Total collection potential Rs. 4,500.",
                relatedCustomerName = "Arsalan Malik",
                relatedPhone = "03123456789",
                relatedAmount = 1500.0
            ),
            EmiNotification(
                title = "Weekly Installment Reminder",
                body = "Reminder: Weekly payment of Rs. 8,000 due from Kashif Khan for Honda CD-70.",
                relatedCustomerName = "Kashif Khan",
                relatedPhone = "03339876543",
                relatedAmount = 8000.0
            )
        )
    }

    fun addNotification(title: String, body: String, customerName: String = "", amount: Double = 0.0) {
        val list = _notifications.value.toMutableList()
        list.add(0, EmiNotification(
            title = title,
            body = body,
            relatedCustomerName = customerName,
            relatedAmount = amount
        ))
        _notifications.value = list
    }

    fun dismissNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    // Generate Beautiful Mock Ledgers to make first boot fully populated and ready-to-test
    private fun generateDemoDataIfEmpty() {
        viewModelScope.launch {
            // Check if customers is empty - Wait, collect first values
            val currentCustomers = database.customerDao().getAllCustomers("demo_uid_admin").first()
            if (currentCustomers.isEmpty()) {
                val owner = "demo_uid_admin"
                
                // Demo Users
                val c1 = Customer("demo_c1", "Muhammad Ali", "03001234567", "35201-1234567-1", "Lahore, Pakistan", "Taimour Ali", "03009998877", ownerId = owner)
                val c2 = Customer("demo_c2", "Kamran Akmal", "03217654321", "42101-9876543-2", "Karachi, Sindh", "Zain Akmal", "03215554433", ownerId = owner)
                val c3 = Customer("demo_c3", "Sohail Ahmed", "03338765412", "37405-1122334-9", "Rawalpindi, Punjab", "Arshad Ahmed", "03331112233", ownerId = owner)

                database.customerDao().insertCustomer(c1)
                database.customerDao().insertCustomer(c2)
                database.customerDao().insertCustomer(c3)

                // Demo Loans
                val now = System.currentTimeMillis()
                val price1 = 185000.0 // Motorbike Honda
                val down1 = 45000.0
                val outstanding1 = price1 - down1
                val loan1 = Loan("demo_l1", "demo_c1", "Muhammad Ali", "Honda 125CC Motorbike", "2026-MODEL", price1, down1, 0.0, outstanding1, outstanding1 - 35000.0, "Monthly", 10, outstanding1 / 10, now - (30L * 24 * 60 * 60 * 1000), now + (270L * 24 * 60 * 60 * 1000), "ACTIVE", owner)
                
                val price2 = 120000.0 // Fridge Dawlance
                val down2 = 20000.0
                val outstanding2 = price2 - down2
                val loan2 = Loan("demo_l2", "demo_c2", "Kamran Akmal", "Dawlance Inverter Refrigerator", "DW-9173", price2, down2, 0.0, outstanding2, outstanding2, "Monthly", 6, outstanding2 / 6, now, now + (180L * 24 * 60 * 60 * 1000), "ACTIVE", owner)

                database.loanDao().insertLoan(loan1)
                database.loanDao().insertLoan(loan2)

                // Generate Installments
                val installments = mutableListOf<Installment>()
                
                // For loan 1: 10 installments monthly. Installment 1 and 2 PAID, 3 PARTIAL, rest UNPAID
                val amt1 = outstanding1 / 10
                for (i in 1..10) {
                    val isPaid = i <= 2
                    val isPartial = i == 3
                    val status = if (isPaid) "PAID" else if (isPartial) "PARTIAL" else "UNPAID"
                    val paid = if (isPaid) amt1 else if (isPartial) amt1 / 2 else 0.0
                    installments.add(
                        Installment(
                            id = "demo_inst_l1_$i",
                            loanId = "demo_l1",
                            installmentNumber = i,
                            dueDate = now - ((11 - i) * 30L * 24 * 60 * 60 * 1000),
                            amountDue = amt1,
                            amountPaid = paid,
                            status = status,
                            paymentDate = if (isPaid || isPartial) now - ((11 - i) * 28L * 24 * 60 * 60 * 1000) else null,
                            ownerId = owner
                        )
                    )
                }

                // For loan 2: 6 installments monthly. All UNPAID.
                val amt2 = outstanding2 / 6
                for (i in 1..6) {
                    installments.add(
                        Installment(
                            id = "demo_inst_l2_$i",
                            loanId = "demo_l2",
                            installmentNumber = i,
                            dueDate = now + (i * 30L * 24 * 60 * 60 * 1000),
                            amountDue = amt2,
                            amountPaid = 0.0,
                            status = "UNPAID",
                            ownerId = owner
                        )
                    )
                }

                database.installmentDao().insertInstallments(installments)

                // Generate Collections Log
                val col1 = CollectionLog("demo_col1", "demo_l1", 1, "Muhammad Ali", "Honda 125CC Motorbike", amt1, now - (60L * 24 * 60 * 60 * 1000), "Cash", "REC-0193", "System Admin", owner)
                val col2 = CollectionLog("demo_col2", "demo_l1", 2, "Muhammad Ali", "Honda 125CC Motorbike", amt1, now - (30L * 24 * 60 * 60 * 1000), "Cash", "REC-0248", "System Admin", owner)
                val col3 = CollectionLog("demo_col3", "demo_l1", 3, "Muhammad Ali", "Honda 125CC Motorbike", amt1 / 2, now - (5L * 24 * 60 * 60 * 1000), "EasyPaisa", "EP-98716", "System Admin", owner)

                database.collectionDao().insertCollection(col1)
                database.collectionDao().insertCollection(col2)
                database.collectionDao().insertCollection(col3)

                Log.d("EmiViewModel", "Demo ledger seeds processed successfully.")
            }
        }
    }

    fun selectTheme(theme: String) {
        themePreference.value = theme
        val prefs = getApplication<Application>().getSharedPreferences("emi_tracker_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("theme", theme).apply()
        
        // Save Theme setting in Firestore
        val user = currentUser.value
        if (user != null && !user.isDemoAccount) {
            viewModelScope.launch {
                try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val themeData = mapOf("theme" to theme, "themePreference" to theme)
                    
                    // 1. Write theme to users/{uid}/settings collection in "settings", "theme" and "preferences" documents
                    db.collection("users").document(user.uid)
                        .collection("settings").document("settings")
                        .set(themeData)
                        .addOnSuccessListener {
                            Log.d("EmiViewModel", "Firestore user setting saved successfully at users/${user.uid}/settings/settings")
                        }

                    db.collection("users").document(user.uid)
                        .collection("settings").document("theme")
                        .set(themeData)
                    
                    db.collection("users").document(user.uid)
                        .collection("settings").document("preferences")
                        .set(themeData)
                    
                    // 2. Also write theme settings map directly into parent users/{uid} document for maximum compatibility
                    db.collection("users").document(user.uid)
                        .set(mapOf("settings" to themeData), com.google.firebase.firestore.SetOptions.merge())
                } catch (e: Exception) {
                    Log.e("EmiViewModel", "Error saving theme preference to Firestore: ${e.message}")
                }
            }
        }
    }

    fun fetchThemePreferenceFromCloud(uid: String) {
        if (uid.isBlank() || uid.contains("demo_")) return
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(uid).collection("settings").document("settings")
                .get()
                .addOnSuccessListener { snapshotSettings ->
                    val cloudThemeSettings = snapshotSettings.getString("theme") ?: snapshotSettings.getString("themePreference")
                    if (cloudThemeSettings != null) {
                        applyThemePreferenceLocal(cloudThemeSettings)
                    } else {
                        db.collection("users").document(uid).collection("settings").document("theme")
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val cloudTheme = snapshot.getString("theme") ?: snapshot.getString("themePreference")
                                if (cloudTheme != null) {
                                    applyThemePreferenceLocal(cloudTheme)
                                } else {
                                    db.collection("users").document(uid).collection("settings").document("preferences")
                                        .get()
                                        .addOnSuccessListener { snapshotPref ->
                                            val cloudTheme2 = snapshotPref.getString("theme") ?: snapshotPref.getString("themePreference")
                                            if (cloudTheme2 != null) {
                                                applyThemePreferenceLocal(cloudTheme2)
                                            } else {
                                                db.collection("users").document(uid).get()
                                                    .addOnSuccessListener { userSnap ->
                                                        val settingsMap = userSnap.get("settings") as? Map<*, *>
                                                        val mapTheme = settingsMap?.get("theme") as? String ?: settingsMap?.get("themePreference") as? String
                                                        if (mapTheme != null) {
                                                            applyThemePreferenceLocal(mapTheme)
                                                        }
                                                    }
                                            }
                                        }
                                }
                            }
                    }
                }
        } catch (e: Exception) {
            Log.e("EmiViewModel", "Failed fetching theme preference from cloud: ${e.message}")
        }
    }

    private fun applyThemePreferenceLocal(theme: String) {
        themePreference.value = theme
        val prefs = getApplication<Application>().getSharedPreferences("emi_tracker_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("theme", theme).apply()
    }
}
