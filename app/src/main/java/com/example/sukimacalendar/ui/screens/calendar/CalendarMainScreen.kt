package com.example.sukimacalendar.ui.screens.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sukimacalendar.data.model.Group
import com.example.sukimacalendar.data.repository.AvailabilityRepository
import com.example.sukimacalendar.data.repository.GroupRepository
import com.example.sukimacalendar.ui.components.BottomNavBar
import com.example.sukimacalendar.ui.theme.SukimaLavender
import com.example.sukimacalendar.ui.theme.SukimaSurfaceGray
import kotlinx.coroutines.launch
import java.time.YearMonth

// ===============================================
// CalendarMainScreen.kt (完成版)
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarMainScreen(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onOpenNotification: () -> Unit
) {
    var selectedGroupIndex by remember { mutableIntStateOf(0) }

    // リポジトリの用意
    val groupRepository = remember { GroupRepository() }
    val availabilityRepository = remember { AvailabilityRepository() }
    val scope = rememberCoroutineScope()

    // Firestoreから自分が所属するグループをリアルタイム取得する
    val groups by groupRepository.observeMyGroups()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // スナックバー用
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // 複数選択モードの状態管理
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedDates by remember { mutableStateOf(setOf<String>()) }

    // 空き時間登録ボトムシートの開閉フラグ
    var showBatchRegisterSheet by remember { mutableStateOf(false) }

    // 240ヶ月分（前後10年）のページャーを用意。中央（120ページ目）を現在月に設定
    val pagerState = rememberPagerState(initialPage = 120) { 240 }

    // 現在表示中のYearMonthを計算
    val currentYearMonth = remember(pagerState.currentPage) {
        YearMonth.now().plusMonths((pagerState.currentPage - 120).toLong())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isMultiSelectMode) {
                        Text("${selectedDates.size}日選択中")
                    } else {
                        Text("${currentYearMonth.year}年 ${currentYearMonth.monthValue}月")
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedDates = emptySet()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "選択解除")
                        }
                        IconButton(onClick = {
                            if (selectedDates.isNotEmpty()) {
                                showBatchRegisterSheet = true
                            }
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "選択した日を一括登録")
                        }
                    } else {
                        IconButton(onClick = onOpenNotification) {
                            Icon(Icons.Filled.Notifications, contentDescription = "通知")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(currentRoute = currentRoute, onNavigate = onNavigate)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // ---- グループ切り替えタブ（Firestoreのリアルタイムグループを使用） ----
            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("グループがありません。「グループ」タブから作成・参加してください", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                GroupTabRow(
                    groups = groups,
                    selectedIndex = selectedGroupIndex,
                    onSelect = { selectedGroupIndex = it }
                )
            }

            // ---- 横スワイプで月を変更できるHorizontalPager ----
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val targetYearMonth = YearMonth.now().plusMonths((page - 120).toLong())
                MonthGrid(
                    yearMonth = targetYearMonth,
                    isMultiSelectMode = isMultiSelectMode,
                    selectedDates = selectedDates,
                    onDayClick = { day ->
                        val dateStr = "${targetYearMonth.year}年${targetYearMonth.monthValue}月${day}日"
                        if (isMultiSelectMode) {
                            selectedDates = if (selectedDates.contains(dateStr)) {
                                selectedDates - dateStr
                            } else {
                                selectedDates + dateStr
                            }
                        } else {
                            selectedDates = setOf(dateStr)
                            showBatchRegisterSheet = true
                        }
                    },
                    onDayLongClick = { day ->
                        val dateStr = "${targetYearMonth.year}年${targetYearMonth.monthValue}月${day}日"
                        if (!isMultiSelectMode) {
                            isMultiSelectMode = true
                            selectedDates = setOf(dateStr)
                        }
                    }
                )
            }
        }
    }

    // ---- 空き時間登録ボトムシート ----
    if (showBatchRegisterSheet) {
        BatchRegisterBottomSheet(
            selectedDates = selectedDates,
            onDismiss = {
                showBatchRegisterSheet = false
                if (!isMultiSelectMode) {
                    selectedDates = emptySet()
                }
            },
            onRegister = { selectedSlots, memo ->
                // 🔴 選択されている日付を非同期処理の前に退避させておく
                val targetDates = selectedDates

                scope.launch {
                    if (groups.isEmpty()) {
                        snackbarMessage = "グループに所属していません"
                        return@launch
                    }

                    // 現在選択されているリアルタイムグループのIDを取得
                    val currentGroup = groups.getOrNull(selectedGroupIndex) ?: groups.first()
                    val groupId = currentGroup.id

                    availabilityRepository.saveAvailabilities(
                        groupId = groupId,
                        dates = targetDates,
                        timeSlots = selectedSlots,
                        memo = memo
                    ).onSuccess {
                        snackbarMessage = "空き時間を登録しました！"
                    }.onFailure { e ->
                        snackbarMessage = "登録に失敗しました: ${e.localizedMessage}"
                    }
                }

                showBatchRegisterSheet = false
                isMultiSelectMode = false
                selectedDates = emptySet()
            }
        )
    }
}

// ===============================================
// グループ切り替えタブ部分
// ===============================================
@Composable
private fun GroupTabRow(groups: List<Group>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val safeIndex = selectedIndex.coerceIn(0, groups.size - 1)
    ScrollableTabRow(selectedTabIndex = safeIndex) {
        groups.forEachIndexed { index, group ->
            Tab(
                selected = safeIndex == index,
                onClick = { onSelect(index) },
                text = { Text(group.name) }
            )
        }
    }
}

// ===============================================
// 月間カレンダーのグリッド本体
// ===============================================
@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    isMultiSelectMode: Boolean,
    selectedDates: Set<String>,
    onDayClick: (Int) -> Unit,
    onDayLongClick: (Int) -> Unit
) {
    val weekLabels = listOf("日", "月", "火", "水", "木", "金", "土")

    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek
    val startDayOffset = firstDayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    val calendarCells = buildList {
        repeat(startDayOffset) { add(null) }
        for (day in 1..daysInMonth) {
            add(day)
        }
        while (size % 7 != 0) {
            add(null)
        }
    }

    val weeks = calendarCells.chunked(7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            weekLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    week.forEach { cellDay ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                        ) {
                            if (cellDay != null) {
                                val dateStr = "${yearMonth.year}年${yearMonth.monthValue}月${cellDay}日"
                                val isSelected = selectedDates.contains(dateStr)

                                DayCell(
                                    day = cellDay,
                                    isSelected = isSelected,
                                    onClick = { onDayClick(cellDay) },
                                    onLongClick = { onDayLongClick(cellDay) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===============================================
// カレンダー1マス分
// ===============================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = if (isSelected) SukimaLavender.copy(alpha = 0.5f) else SukimaSurfaceGray

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(text = day.toString(), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ===============================================
// 空き時間登録ボトムシート
// ===============================================
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BatchRegisterBottomSheet(
    selectedDates: Set<String>,
    onDismiss: () -> Unit,
    onRegister: (List<String>, String) -> Unit
) {
    var selectedSlots by remember { mutableStateOf(setOf("一日")) }
    var isExpandedDetails by remember { mutableStateOf(false) }
    var memoText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            val titleText = if (selectedDates.size == 1) {
                "${selectedDates.first()} の空き時間を設定"
            } else {
                "空き時間を設定 (${selectedDates.size}日)"
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "時間帯を選択", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            val isOneDaySelected = selectedSlots.contains("一日")
            Surface(
                onClick = { selectedSlots = setOf("一日") },
                shape = RoundedCornerShape(8.dp),
                color = if (isOneDaySelected) MaterialTheme.colorScheme.primaryContainer else SukimaSurfaceGray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "一日",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOneDaySelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val subSlots = listOf("朝", "昼", "夜")
                subSlots.forEach { slot ->
                    val isSelected = selectedSlots.contains(slot)
                    Surface(
                        onClick = {
                            selectedSlots = if (isSelected) {
                                val next = selectedSlots - slot
                                if (next.isEmpty()) setOf("一日") else next
                            } else {
                                (selectedSlots - "一日") + slot
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else SukimaSurfaceGray,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = slot,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { isExpandedDetails = !isExpandedDetails }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "詳細（メモや個別時間）を入力する",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                IconButton(onClick = { isExpandedDetails = !isExpandedDetails }) {
                    Icon(
                        imageVector = if (isExpandedDetails) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "詳細を開閉"
                    )
                }
            }

            if (isExpandedDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    label = { Text("メモ（例: バイト終わってから空いてます 等）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val buttonText = if (selectedDates.size == 1) "この日に登録する" else "選択した日に一括登録する"
            Button(
                onClick = { onRegister(selectedSlots.toList(), memoText) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = buttonText)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}