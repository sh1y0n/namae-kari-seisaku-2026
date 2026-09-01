package com.example.sukimacalendar.ui.screens.group

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sukimacalendar.data.model.Group
import com.example.sukimacalendar.data.repository.GroupRepository
import com.example.sukimacalendar.ui.components.BottomNavBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(currentRoute: String?, onNavigate: (String) -> Unit) {
    val groupRepository = remember { GroupRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val groups by groupRepository.observeMyGroups()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    // パスワード確認・変更ダイアログ用の状態
    var selectedGroupForDetail by remember { mutableStateOf<Group?>(null) }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("グループ") },
                actions = {
                    IconButton(onClick = { showJoinDialog = true }) {
                        Icon(Icons.Filled.Login, contentDescription = "グループに参加")
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "グループを作成")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(currentRoute = currentRoute, onNavigate = onNavigate)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (groups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("グループがありません。右上のアイコンから作成または参加してください。")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                items(groups, key = { it.id }) { group ->
                    GroupRow(
                        group = group,
                        onCopyCode = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("InviteCode", group.inviteCode)
                            clipboard.setPrimaryClip(clip)
                            snackbarMessage = "招待コードをコピーしました: ${group.inviteCode}"
                        },
                        onShowDetail = {
                            selectedGroupForDetail = group
                        },
                        onDelete = {
                            scope.launch {
                                groupRepository.deleteGroup(group.id)
                                    .onSuccess { snackbarMessage = "グループを削除しました" }
                                    .onFailure { snackbarMessage = "削除に失敗しました" }
                            }
                        }
                    )
                }
            }
        }
    }

    // グループ詳細（パスワード確認）ダイアログ
    selectedGroupForDetail?.let { group ->
        GroupDetailDialog(
            group = group,
            groupRepository = groupRepository,
            onDismiss = { selectedGroupForDetail = null },
            onUpdated = { newPass ->
                snackbarMessage = "パスワードを更新しました"
                selectedGroupForDetail = null
            }
        )
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, password ->
                scope.launch {
                    groupRepository.createGroup(name, password)
                        .onSuccess { snackbarMessage = "グループを作成しました" }
                        .onFailure { snackbarMessage = "作成に失敗しました: ${it.message}" }
                }
                showCreateDialog = false
            }
        )
    }

    if (showJoinDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { code, password ->
                scope.launch {
                    groupRepository.joinGroupWithCode(code, password)
                        .onSuccess { snackbarMessage = "グループに参加しました！" }
                        .onFailure { snackbarMessage = "${it.message}" }
                }
                showJoinDialog = false
            }
        )
    }
}

@Composable
private fun GroupRow(
    group: Group,
    onCopyCode: () -> Unit,
    onShowDetail: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = group.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "招待コード: ${group.inviteCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Box {
                TextButton(onClick = { showMenu = true }) {
                    Text("操作")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("招待コードをコピー") },
                        onClick = {
                            showMenu = false
                            onCopyCode()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("パスワード確認・変更") },
                        onClick = {
                            showMenu = false
                            onShowDetail()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("グループを削除") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupDetailDialog(
    group: Group,
    groupRepository: GroupRepository,
    onDismiss: () -> Unit,
    onUpdated: (String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("読み込み中...") }
    var newPassword by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // ダイアログが開いたときにFirestoreから現在のパスワードを取得する
    LaunchedEffect(group.id) {
        groupRepository.getPassword(group.id)
            .onSuccess { currentPassword = it }
            .onFailure { currentPassword = "取得失敗" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("グループ詳細: ${group.name}") },
        text = {
            Column {
                Text(text = "招待コード: ${group.inviteCode}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "現在のパスワード: $currentPassword", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新しいパスワードに変更（任意）") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newPassword.isNotBlank()) {
                        scope.launch {
                            groupRepository.updatePassword(group.id, newPassword.trim())
                                .onSuccess { onUpdated(newPassword.trim()) }
                        }
                    } else {
                        onDismiss()
                    }
                }
            ) { Text("閉じる / 変更を保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}

@Composable
private fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新しいグループ作成") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("グループ名") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("参加用パスワード") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && password.isNotBlank()) onCreate(name.trim(), password.trim()) },
                enabled = name.isNotBlank() && password.isNotBlank()
            ) { Text("作成") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}

@Composable
private fun JoinGroupDialog(onDismiss: () -> Unit, onJoin: (String, String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("グループに参加") },
        text = {
            Column {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("6桁の招待コード") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("パスワード") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (code.isNotBlank() && password.isNotBlank()) onJoin(code.trim(), password.trim()) },
                enabled = code.isNotBlank() && password.isNotBlank()
            ) { Text("参加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}