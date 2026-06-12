package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.EmiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EmiViewModel = viewModel()
            val themePreference by viewModel.themePreference.collectAsState()

            MyApplicationTheme(themePreference = themePreference) {
                val currentUser by viewModel.currentUser.collectAsState()
                val navController = rememberNavController()

                val context = androidx.compose.ui.platform.LocalContext.current
                val hasOnboarded = remember(viewModel) {
                    val prefs = context.getSharedPreferences("emi_tracker_settings", android.content.Context.MODE_PRIVATE)
                    prefs.getBoolean("permissions_completed", false)
                }
                
                val requiredPermissionsGranted = remember(context) {
                    val needsNotif = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                    val hasSms = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.SEND_SMS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasNotif = !needsNotif || androidx.core.content.ContextCompat.checkSelfPermission(
                        context, "android.permission.POST_NOTIFICATIONS"
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    hasSms && hasNotif
                }

                // Check starting route dynamically based on active session status
                val startingRoute = if (!hasOnboarded || !requiredPermissionsGranted) {
                    Screen.PermissionOnboarding.route
                } else if (currentUser != null) {
                    Screen.Dashboard.route
                } else {
                    Screen.Login.route
                }

                NavHost(
                    navController = navController,
                    startDestination = startingRoute
                ) {
                    composable(Screen.PermissionOnboarding.route) {
                        PermissionOnboardingScreen(
                            viewModel = viewModel,
                            onOnboardingCompleted = {
                                val nextRoute = if (currentUser != null) Screen.Dashboard.route else Screen.Login.route
                                navController.navigate(nextRoute) {
                                    popUpTo(Screen.PermissionOnboarding.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Login.route) {
                        LoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToCustomers = { navController.navigate(Screen.Customers.route) },
                            onNavigateToAddCustomer = { navController.navigate(Screen.AddCustomer.route) },
                            onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                            onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                            onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                            onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                            onNavigateToLoanDetail = { loanId ->
                                navController.navigate(Screen.LoanDetail.createRoute(loanId))
                            }
                        )
                    }

                    composable(Screen.Customers.route) {
                        CustomersScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { customerId ->
                                navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                            },
                            onNavigateToAddCustomer = { navController.navigate(Screen.AddCustomer.route) }
                        )
                    }

                    composable(Screen.AddCustomer.route) {
                        AddCustomerScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.CustomerDetail.route,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                        CustomerDetailScreen(
                            viewModel = viewModel,
                            customerId = customerId,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToAddLoan = { cid ->
                                navController.navigate(Screen.AddLoan.createRoute(cid))
                            },
                            onNavigateToLoanDetail = { lid ->
                                navController.navigate(Screen.LoanDetail.createRoute(lid))
                            }
                        )
                    }

                    composable(
                        route = Screen.AddLoan.route,
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                        AddLoanScreen(
                            viewModel = viewModel,
                            customerId = customerId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.LoanDetail.route,
                        arguments = listOf(navArgument("loanId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
                        LoanDetailScreen(
                            viewModel = viewModel,
                            loanId = loanId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Reports.route) {
                        ReportsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Search.route) {
                        SearchScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToCustomerDetail = { cid ->
                                navController.navigate(Screen.CustomerDetail.createRoute(cid))
                            }
                        )
                    }

                    composable(Screen.Notifications.route) {
                        NotificationScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Backup.route) {
                        BackupScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onLogoutPressed = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
