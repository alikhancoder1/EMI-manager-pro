package com.example.ui.navigation

sealed class Screen(val route: String) {
    object PermissionOnboarding : Screen("permission_onboarding")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Customers : Screen("customers")
    object AddCustomer : Screen("add_customer")
    object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(customerId: String) = "customer_detail/$customerId"
    }
    object AddLoan : Screen("add_loan/{customerId}") {
        fun createRoute(customerId: String) = "add_loan/$customerId"
    }
    object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: String) = "loan_detail/$loanId"
    }
    object Reports : Screen("reports")
    object Search : Screen("search")
    object Notifications : Screen("notifications")
    object Backup : Screen("backup")
}
