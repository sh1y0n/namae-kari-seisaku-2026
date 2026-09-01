package com.example.sukimacalendar.ui.screens.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sukimacalendar.data.model.Group
import com.example.sukimacalendar.data.repository.GroupRepository
import com.example.sukimacalendar.ui.components.BottomNavBar
import kotlinx.coroutines.launch

// ===============================================
// GroupScreen.kt
// 役割: 自分が参加しているグループの一覧を見て、新しいグループを作る画面。
//
// 今の実装状態(DB接続後):
//      GroupRepository.observeMyGroups() でFirestoreの変化をリアルタイムに監視し、
//      一覧に反映している(自分がグループに追加された瞬間、自動でリストに出てくる)。
//      「＋」ボタンでダイアログを開き、グループ名を入力して作成できる。
//
//      招待機能(友達を追加する部分)はまだ未実装。TODO参照。
//
// currentRoute / onNavigate: 下部ナビゲーションバーの状態管理用(BottomNavBar.kt参照)
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(currentRoute: String?, onNavigate: (String) -> Unit) {
    val groupRepository = remember { GroupRepository() }
    val scope = rememberCoroutineScope()

    // collectAsStateWithLifecycle(): Flow(observeMyGroups)の最新の値をComposeのStateに変換する。
    // 画面が非表示のときは自動的に購読を止めてくれるので、collectAsState()より電池に優しい。
    val groups by groupRepository.observeMyGroups()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("グループ") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "グループを作成")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(currentRoute = currentRoute, onNavigate = onNavigate)
        }
    ) { innerPadding ->
        if (groups.isEmpty()) {
            // ---- グループが1つもないときの案内表示 ----
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("グループがまだありません。右上の＋から作成してください。")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                items(groups, key = { it.id }) { group ->
                    GroupRow(group)
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                scope.launch {
                    groupRepository.createGroup(name)
                    // 作成後、observeMyGroups()のリアルタイム監視が自動で
                    // 新しいグループを一覧に反映してくれるので、ここで何もしなくてよい。
                }
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun GroupRow(group: Group) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = group.name, style = MaterialTheme.typography.titleMedium)
            // TODO: グループ詳細(メンバー一覧・招待)へ遷移する導線をここに追加する
            Text(text = "タップしてこのグループのカレンダーを見る", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ===============================================
// グループ作成ダイアログ。名前を入力して「作成」を押すとonCreateが呼ばれる。
// ===============================================
@Composable
private fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新しいグループ") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("グループ名") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("作成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}
