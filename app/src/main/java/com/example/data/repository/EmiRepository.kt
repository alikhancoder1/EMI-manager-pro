package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.*
import com.example.data.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmiRepository(
    context: Context,
    private val customerDao: CustomerDao,
    private val loanDao: LoanDao,
    private val installmentDao: InstallmentDao,
    private val collectionDao: CollectionDao
) {
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
            Log.d("EmiRepository", "Firebase Firestore connected successfully.")
        } catch (e: Exception) {
            Log.e("EmiRepository", "Firestore init ignored: ${e.message}. System functioning offline-first.")
        }
    }

    private fun triggerAutoBackup(ownerId: String) {
        if (ownerId.isNotBlank() && !ownerId.contains("demo_")) {
            backupToCloud(
                ownerId = ownerId,
                onSuccess = { Log.d("EmiRepository", "Auto background backup succeeded.") },
                onError = { Log.e("EmiRepository", "Auto background backup failed: $it") }
            )
        }
    }

    // Reactively observe local database flows
    fun getCustomers(ownerId: String): Flow<List<Customer>> = customerDao.getAllCustomers(ownerId)
    fun getLoans(ownerId: String): Flow<List<Loan>> = loanDao.getAllLoans(ownerId)
    fun getInstallments(ownerId: String): Flow<List<Installment>> = installmentDao.getAllInstallments(ownerId)
    fun getCollections(ownerId: String): Flow<List<CollectionLog>> = collectionDao.getAllCollections(ownerId)

    suspend fun getCustomerById(id: String): Customer? = customerDao.getCustomerById(id)
    suspend fun getLoanById(id: String): Loan? = loanDao.getLoanById(id)
    fun getLoanDetailInstallments(loanId: String): Flow<List<Installment>> = installmentDao.getInstallmentsForLoan(loanId)
    fun getLoanDetailCollections(loanId: String): Flow<List<CollectionLog>> = collectionDao.getCollectionsForLoan(loanId)
    fun getLoansForCustomer(customerId: String): Flow<List<Loan>> = loanDao.getLoansForCustomer(customerId)

    // Write Operations
    suspend fun saveCustomer(customer: Customer) {
        customerDao.insertCustomer(customer)
        triggerAutoBackup(customer.ownerId)
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(customer)
        triggerAutoBackup(customer.ownerId)
    }

    suspend fun saveLoanWithSchedule(loan: Loan, installments: List<Installment>) {
        loanDao.insertLoan(loan)
        installmentDao.insertInstallments(installments)
        triggerAutoBackup(loan.ownerId)
    }

    suspend fun recordCollectionPayment(collection: CollectionLog, installmentId: String, newPaidAmount: Double, isFullyPaid: Boolean) {
        // 1. Record collection log
        collectionDao.insertCollection(collection)

        // 2. Update individual Installment state
        val installment = installmentDao.getInstallmentById(installmentId) ?: return
        val currentPaid = installment.amountPaid + newPaidAmount
        val updatedInstallment = installment.copy(
            amountPaid = currentPaid,
            status = if (isFullyPaid || currentPaid >= installment.amountDue) "PAID" else "PARTIAL",
            paymentDate = System.currentTimeMillis()
        )
        installmentDao.updateInstallment(updatedInstallment)

        // 3. Recalculate Loan Remaining Balance
        val loan = loanDao.getLoanById(collection.loanId) ?: return
        val remaining = (loan.remainingBalance - newPaidAmount).coerceAtLeast(0.0)
        val loanStatus = if (remaining <= 0.0) "COMPLETED" else "ACTIVE"
        loanDao.updateLoan(loan.copy(remainingBalance = remaining, status = loanStatus))

        triggerAutoBackup(collection.ownerId)
    }

    suspend fun updateInstallmentManual(installment: Installment) {
        installmentDao.updateInstallment(installment)
        triggerAutoBackup(installment.ownerId)
    }

    suspend fun updateInstallmentsBatch(installments: List<Installment>) {
        installmentDao.insertInstallments(installments)
        val firstOwner = installments.firstOrNull()?.ownerId ?: ""
        triggerAutoBackup(firstOwner)
    }

    suspend fun updateLoanDirect(loan: Loan) {
        loanDao.updateLoan(loan)
        triggerAutoBackup(loan.ownerId)
    }

    suspend fun recordRawCollectionOnly(collectionLog: CollectionLog) {
        collectionDao.insertCollection(collectionLog)
        triggerAutoBackup(collectionLog.ownerId)
    }

    // Stats Calculation Flow
    fun getDashboardStats(ownerId: String): Flow<DashboardStats> {
        return combine(
            getLoans(ownerId),
            getCollections(ownerId),
            getInstallments(ownerId)
        ) { loans, collections, installments ->
            val totalOutstanding = loans.filter { it.status == "ACTIVE" }.sumOf { it.remainingBalance }
            val activeLoansCount = loans.count { it.status == "ACTIVE" }
            val activeCustomers = loans.filter { it.status == "ACTIVE" }.map { it.customerId }.distinct().size

            val totalCollected = collections.sumOf { it.amountReceived }

            val todayStart = getStartOfDayMillis()
            val todayEnd = todayStart + (24 * 60 * 60 * 1000)

            val collectedToday = collections.filter { it.receiptDate in todayStart..todayEnd }.sumOf { it.amountReceived }
            
            val startOfMonth = getStartOfMonthMillis()
            val collectedThisMonth = collections.filter { it.receiptDate >= startOfMonth }.sumOf { it.amountReceived }

            val totalDueToday = installments.filter { 
                it.status != "PAID" && it.dueDate in todayStart..todayEnd
            }.sumOf { it.amountDue - it.amountPaid }

            val paidCount = installments.count { it.status == "PAID" }
            val totalCount = installments.size
            val recoveryRate = if (totalCount > 0) (paidCount.toDouble() / totalCount * 100) else 0.0

            DashboardStats(
                totalCollected = totalCollected,
                totalOutstanding = totalOutstanding,
                collectedToday = collectedToday,
                collectedThisMonth = collectedThisMonth,
                activeCustomersCount = activeCustomers,
                activeLoansCount = activeLoansCount,
                totalDueToday = totalDueToday,
                recoveryRate = recoveryRate
            )
        }
    }

    private fun getStartOfDayMillis(): Long {
        val current = System.currentTimeMillis()
        return current - (current % (24 * 60 * 60 * 1000))
    }

    private fun getStartOfMonthMillis(): Long {
        val current = System.currentTimeMillis()
        // Simple approximate calculation for standard metrics
        return current - (30L * 24 * 60 * 60 * 1000)
    }

    // CLOUD BACKUP INTEGRATION (Push Local Room -> Cloud Firestore)
    fun backupToCloud(
        ownerId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val db = firestore
        if (db == null) {
            onError("Firebase not initialized. Please load Google-Services Config.")
            return
        }

        ioScope.launch {
            try {
                // Fetch all records locally first
                val customersList = customerDao.getAllCustomers(ownerId).first()
                val loansList = loanDao.getAllLoans(ownerId).first()
                val installmentsList = installmentDao.getAllInstallments(ownerId).first()
                val collectionsList = collectionDao.getAllCollections(ownerId).first()

                val batch = db.batch()

                // Register Customers under /users/{userId}/customers/{customerId}
                customersList.forEach { customer ->
                    val docRef = db.collection("users").document(ownerId)
                        .collection("customers").document(customer.id)
                    batch.set(docRef, customer)
                }

                // Register Loans under /users/{userId}/loans/{loanId}
                loansList.forEach { loan ->
                    val docRef = db.collection("users").document(ownerId)
                        .collection("loans").document(loan.id)
                    batch.set(docRef, loan)
                }

                // Register Installments under /users/{userId}/installments/{id}
                installmentsList.forEach { inst ->
                    val docRef = db.collection("users").document(ownerId)
                        .collection("installments").document(inst.id)
                    batch.set(docRef, inst)
                }

                // Register Collections under /users/{userId}/collections/{id}
                collectionsList.forEach { col ->
                    val docRef = db.collection("users").document(ownerId)
                        .collection("collections").document(col.id)
                    batch.set(docRef, col)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("EmiRepository", "Cloud Backup successful for ownerId: $ownerId")
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("EmiRepository", "Cloud Backup failed: ${exception.message}")
                        onError(exception.message ?: "Unknown Firestore Sync Error")
                    }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Local Read Failure")
                }
            }
        }
    }

    suspend fun clearLocalData() {
        withContext(Dispatchers.IO) {
            try {
                customerDao.clearAll()
                loanDao.clearAll()
                installmentDao.clearAll()
                collectionDao.clearAll()
                Log.d("EmiRepository", "Local Database Cache Cleared.")
            } catch (e: Exception) {
                Log.e("EmiRepository", "Failed to clear local database cache: ${e.message}")
            }
        }
    }

    // CLOUD RESTORE INTEGRATION (Pull Cloud Firestore -> Local Room Room)
    fun restoreFromCloud(
        ownerId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val db = firestore
        if (db == null) {
            onError("Firebase Connection is not active.")
            return
        }

        // We fetch historical data from firestore and overwrite it into Room
        db.collection("users").document(ownerId).collection("customers")
            .get()
            .addOnSuccessListener { customerSnap ->
                val customers = customerSnap.toObjects(Customer::class.java)
                
                db.collection("users").document(ownerId).collection("loans")
                    .get()
                    .addOnSuccessListener { loanSnap ->
                        val loans = loanSnap.toObjects(Loan::class.java)

                        db.collection("users").document(ownerId).collection("installments")
                            .get()
                            .addOnSuccessListener { instSnap ->
                                val installments = instSnap.toObjects(Installment::class.java)

                                db.collection("users").document(ownerId).collection("collections")
                                    .get()
                                    .addOnSuccessListener { colSnap ->
                                        val collections = colSnap.toObjects(CollectionLog::class.java)

                                        // Now execute Room Write operations transactions
                                        ioScope.launch {
                                            try {
                                                // Wipe existing for clean restore
                                                customerDao.clearAll()
                                                loanDao.clearAll()
                                                installmentDao.clearAll()
                                                collectionDao.clearAll()

                                                // Populate restored records
                                                customers.forEach { customerDao.insertCustomer(it) }
                                                loans.forEach { loanDao.insertLoan(it) }
                                                
                                                // Batch write installments
                                                if (installments.isNotEmpty()) {
                                                    installmentDao.insertInstallments(installments)
                                                }
                                                
                                                collections.forEach { collectionDao.insertCollection(it) }

                                                withContext(Dispatchers.Main) {
                                                    Log.d("EmiRepository", "Restored whole catalog from Cloud database successfully.")
                                                    onSuccess()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    onError(e.message ?: "Failed writing restored data locally.")
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { onError(it.message ?: "Restore collections failed.") }
                            }
                            .addOnFailureListener { onError(it.message ?: "Restore installments failed.") }
                    }
                    .addOnFailureListener { onError(it.message ?: "Restore loans failed.") }
            }
            .addOnFailureListener { onError(it.message ?: "Restore customers failed.") }
    }
}
