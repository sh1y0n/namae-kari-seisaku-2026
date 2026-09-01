package com.example.sukimacalendar.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sukimacalendar.ui.components.BottomNavBar

// ===============================================
// SettingsScreen.kt
// 役割: アカウント情報の変更や、アプリの設定を行う画面。
//
// 今の実装状態(骨組み段階):
//      項目を並べているだけで、タップしても何も起きない。
//      後で「ユーザー名変更」「ログアウト」などの処理をTODO箇所に実装する。
//
// @OptIn(ExperimentalMaterial3Api::class): TopAppBarが実験的APIのため必要(GroupScreen.kt参照)
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(currentRoute: String?, onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("設定") }) },
        bottomBar = { BottomNavBar(currentRoute = currentRoute, onNavigate = onNavigate) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            SettingsRow(label = "ユーザー名の変更") { /* TODO: 変更ダイアログ表示 */ }
            SettingsRow(label = "通知設定") { /* TODO: 通知ON/OFF設定 */ }
            SettingsRow(label = "ログアウト") { /* TODO: Firebase Authからサインアウト */ }
        }
    }
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    // clickableはModifierの拡張関数なので、Modifier.clickable(...) のように
    // 「Modifierに対して呼ぶ」形でなければならない。
    // (前回のバージョンは関数として直接呼んでいたため Unresolved reference になっていた)
    ListItem(
        headlineContent = { Text(label) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit // ← 引数に追加
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("設定") }) },
        bottomBar = { BottomNavBar(currentRoute = currentRoute, onNavigate = onNavigate) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            SettingsRow(label = "ユーザー名の変更") { /* TODO: 変更ダイアログ表示 */ }
            SettingsRow(label = "通知設定") { /* TODO: 通知ON/OFF設定 */ }
            SettingsRow(label = "ログアウト") {
                onLogout() // ← ここで呼び出す
            }
        }
    }
}