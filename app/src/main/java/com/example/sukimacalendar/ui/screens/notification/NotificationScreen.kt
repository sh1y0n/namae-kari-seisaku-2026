package com.example.sukimacalendar.ui.screens.notification

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ===============================================
// NotificationScreen.kt
// 役割: 友達から「お誘い」が届いたときに確認し、承認・拒否をする画面。
//      カレンダーメイン画面のベルアイコンから遷移してくる(Screen.Notification参照)。
//
// 今の実装状態(骨組み段階):
//      ダミーのお誘いを1件だけ表示。承認/拒否ボタンは押しても何も起きない。
//      TODO: Firestoreの「招待(invitations)」コレクションを購読して一覧表示し、
//           承認したらグループ or 予定に反映する処理を実装する。
//
// onBack: 戻るボタンが押されたときに呼ぶ関数(カレンダーメイン画面に戻る)
//
// @OptIn(ExperimentalMaterial3Api::class): TopAppBarが実験的APIのため必要(GroupScreen.kt参照)
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            // ---- お誘い1件分のカード(ダミー) ----
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("よしとうさんから「8月1日 夜」に誘われています", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { /* TODO: 承認処理 */ }) { Text("承認") }
                        OutlinedButton(onClick = { /* TODO: 拒否処理 */ }) { Text("拒否") }
                    }
                }
            }
        }
    }
}
