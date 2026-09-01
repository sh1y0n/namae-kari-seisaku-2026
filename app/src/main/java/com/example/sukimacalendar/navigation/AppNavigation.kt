package com.example.sukimacalendar.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import com.example.sukimacalendar.ui.screens.auth.AuthScreen
import com.example.sukimacalendar.ui.screens.calendar.CalendarMainScreen
import com.example.sukimacalendar.ui.screens.group.GroupScreen
import com.example.sukimacalendar.ui.screens.notification.NotificationScreen
import com.example.sukimacalendar.ui.screens.settings.SettingsScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavHost(navController = navController, startDestination = Screen.Auth.route) {

        // ---- 1. 認証画面 ----
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = { // ← ここを AuthScreen の定義に合わせて修正
                    // ログイン成功時にカレンダー画面へ遷移し、認証画面をバックスタックから削除
                    navController.navigate(Screen.Calendar.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // ---- 2. グループ選択・管理画面 ----
        composable(Screen.Group.route) {
            GroupScreen(
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigateToTab(route) }
            )
        }

        // ---- 3. カレンダーメイン画面 ----
        composable(Screen.Calendar.route) {
            CalendarMainScreen(
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigateToTab(route) },
                onOpenNotification = { navController.navigate(Screen.Notification.route) }
            )
        }

        // ---- 4. 設定画面 ----
        composable(Screen.Settings.route) {
            SettingsScreen(
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigateToTab(route) },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ---- 5. 通知画面 ----
        composable(Screen.Notification.route) {
            NotificationScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    this.navigate(route) {
        popUpTo(this@navigateToTab.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}