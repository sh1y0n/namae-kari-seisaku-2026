package com.example.sukimacalendar.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.sukimacalendar.navigation.Screen

// ===============================================
// BottomNavBar.kt
// 役割: PPTXのモックアップ(スライド8)の一番下にある、
//      「グループ」「カレンダー」「設定」の3アイコンのバーを部品化したもの。
//      カレンダーメイン画面・グループ画面・設定画面の3つで共通して使う。
//
// currentRoute: 今どの画面が表示されているか(タブのハイライト判定に使う)
// onNavigate:   タブがタップされたときに呼ぶ関数。Screen.routeを渡す。
// ===============================================
private data class NavItem(val screen: Screen, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val navItems = listOf(
    NavItem(Screen.Group, "グループ", Icons.Filled.Group),
    NavItem(Screen.Calendar, "カレンダー", Icons.Filled.CalendarMonth),
    NavItem(Screen.Settings, "設定", Icons.Filled.Settings)
)

@Composable
fun BottomNavBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.screen.route, // 今の画面と一致していればハイライト
                onClick = { onNavigate(item.screen.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
