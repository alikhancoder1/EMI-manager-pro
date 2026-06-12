package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val cnic: String, // format: XXXXX-XXXXXXX-X
    val address: String,
    val guarantorName: String,
    val guarantorPhone: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val ownerId: String, // Links to Google UID for backup safety
    val photoUri: String? = null
)

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val customerName: String,
    val itemName: String,
    val modelNo: String,
    val purchasePrice: Double,
    val downPayment: Double,
    val interestRate: Double = 0.0, // optional markup
    val outstandingAmount: Double, // total to be paid back (purchasePrice - downPayment + markup)
    val remainingBalance: Double,
    val paymentFrequency: String, // "Daily", "Weekly", "Monthly"
    val totalInstallments: Int,
    val installmentAmount: Double,
    val startDate: Long,
    val maturityDate: Long, // auto computed completion date
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "DEFAULTED"
    val ownerId: String,
    val productCategory: String = ""
)

@Entity(tableName = "installments")
data class Installment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val loanId: String,
    val installmentNumber: Int,
    val dueDate: Long,
    val amountDue: Double,
    val amountPaid: Double = 0.0,
    val status: String = "UNPAID", // "UNPAID", "PAID", "PARTIAL", "OVERDUE"
    val paymentDate: Long? = null,
    val ownerId: String
)

@Entity(tableName = "collections")
data class CollectionLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val loanId: String,
    val installmentNumber: Int,
    val customerName: String,
    val itemName: String,
    val amountReceived: Double,
    val receiptDate: Long = System.currentTimeMillis(),
    val paymentMethod: String = "Cash", // Cash, Bank, EasyPaisa, JazzCash
    val transactionRef: String = "",
    val receivedBy: String = "System Admin",
    val ownerId: String
)

data class DashboardStats(
    val totalCollected: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val collectedToday: Double = 0.0,
    val collectedThisMonth: Double = 0.0,
    val activeCustomersCount: Int = 0,
    val activeLoansCount: Int = 0,
    val totalDueToday: Double = 0.0,
    val recoveryRate: Double = 0.0 // representation of recovery progress
)

data class EmiNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedCustomerName: String = "",
    val relatedPhone: String = "",
    val relatedAmount: Double = 0.0
)
