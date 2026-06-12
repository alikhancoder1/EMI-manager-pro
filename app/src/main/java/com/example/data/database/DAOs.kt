package com.example.data.database

import androidx.room.*
import com.example.data.Customer
import com.example.data.Loan
import com.example.data.Installment
import com.example.data.CollectionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE ownerId = :ownerId ORDER BY name ASC")
    fun getAllCustomers(ownerId: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM customers")
    suspend fun clearAll()
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE ownerId = :ownerId ORDER BY startDate DESC")
    fun getAllLoans(ownerId: String): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanById(id: String): Loan?

    @Query("SELECT * FROM loans WHERE customerId = :customerId")
    fun getLoansForCustomer(customerId: String): Flow<List<Loan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan)

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    @Query("DELETE FROM loans")
    suspend fun clearAll()
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments WHERE ownerId = :ownerId")
    fun getAllInstallments(ownerId: String): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    fun getInstallmentsForLoan(loanId: String): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE id = :id LIMIT 1")
    suspend fun getInstallmentById(id: String): Installment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<Installment>)

    @Update
    suspend fun updateInstallment(installment: Installment)

    @Query("DELETE FROM installments WHERE loanId = :loanId")
    suspend fun deleteInstallmentsForLoan(loanId: String)

    @Query("DELETE FROM installments")
    suspend fun clearAll()
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections WHERE ownerId = :ownerId ORDER BY receiptDate DESC")
    fun getAllCollections(ownerId: String): Flow<List<CollectionLog>>

    @Query("SELECT * FROM collections WHERE loanId = :loanId ORDER BY receiptDate DESC")
    fun getCollectionsForLoan(loanId: String): Flow<List<CollectionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionLog)

    @Query("DELETE FROM collections")
    suspend fun clearAll()
}
