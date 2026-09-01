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

// ===============================================
// AppNavigation.kt
// 役割: アプリ全体の「今どの画面を表示するか」を一箇所で管理する、司令塔となるファイル。
//      スライド6の画面遷移図をそのままコードにしたもの:
//
//        Auth ──(ログイン成功)──▶ Calendar(下部ナビでGroup/Settingsとも行き来)
//        Calendar ──(ベルアイコン)──▶ Notification
//        Calendar ──(日付タップ)──▶ DateDetailBottomSheet(画面遷移ではなくボトムシートなので
//                                                        ここには出てこない。CalendarMainScreen.kt内で完結)
//
// MainActivity.ktからは AppNavigation() を1回呼ぶだけでよい設計にしている。
// ===============================================
@Composable
fun AppNavigation() {
    // NavHostController: 画面遷移の履歴(バックスタック)を管理してくれるコントローラー。
    // 「戻るボタンで前の画面に戻る」等の動作もこれが自動でやってくれる。
    val navController: NavHostController = rememberNavController()

    // 今表示中の画面のルート名を監視(BottomNavBarのハイライト判定に使う)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavHost(navController = navController, startDestination = Screen.Auth.route) {

        // ---- 認証・スタート画面 ----
        composable(Screen.Auth.route) {
            AuthScreen(
                onLoginSuccess = {
                    // ログイン画面には戻れないようにする(popUpTo + inclusive)。
                    // これをしないと、カレンダー画面で戻るボタンを押すとログイン画面に戻ってしまう。
                    navController.navigate(Screen.Calendar.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // ---- グループ選択・管理画面 ----
        composable(Screen.Group.route) {
            GroupScreen(
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigateToTab(route) }
            )
        }

        // ---- ★カレンダーメイン画面 ----
        composable(Screen.Calendar.route) {
            CalendarMainScreen(
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigateToTab(route) },
                onOpenNotification = { navController.navigate(Screen.Notification.route) }
            )
        }

        // ---- 設定画面 ----
        composable(Screen.Settings.route) {
            SettingsScreen(
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigateToTab(route) }
            )
        }

        // ---- 通知画面 ----
        composable(Screen.Notification.route) {
            NotificationScreen(onBack = { navController.popBackStack() })
        }
    }
}

// ===============================================
// 下部ナビ(グループ・カレンダー・設定)のタブ切り替え専用の遷移処理。
// 普通のnavigate()だけだと「タブを行き来するたびに画面が積み重なっていく」
// (グループ→カレンダー→グループ→カレンダー…でバックスタックが無限に伸びる)ので、
// launchSingleTop / popUpTo で「タブは常に1枚だけ」になるよう制御している。
// ===============================================
private fun NavHostController.navigateToTab(route: String) {
    this.navigate(route) {
        popUpTo(this@navigateToTab.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
