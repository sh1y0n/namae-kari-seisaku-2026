package com.example.sukimacalendar.ui.screens.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sukimacalendar.data.model.Group
import com.example.sukimacalendar.data.repository.AvailabilityRepository
import com.example.sukimacalendar.data.repository.GroupRepository
import com.example.sukimacalendar.ui.components.BottomNavBar
import com.example.sukimacalendar.ui.theme.SukimaLavender
import com.example.sukimacalendar.ui.theme.SukimaSurfaceGray
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarMainScreen(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onOpenNotification: () -> Unit
) {
    var selectedGroupIndex by remember { mutableIntStateOf(0) }

    val groupRepository = remember { GroupRepository() }
    val availabilityRepository = remember { AvailabilityRepository() }
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    val groups by groupRepository.observeMyGroups()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val currentGroup = groups.getOrNull(selectedGroupIndex) ?: groups.firstOrNull()
    val currentGroupId = currentGroup?.id ?: ""

    val groupAvailabilities by availabilityRepository.observeAvailabilitiesForGroup(currentGroupId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val availabilitiesByDate = remember(groupAvailabilities) {
        groupAvailabilities.groupBy { it["date"] as? String ?: "" }
    }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedDates by remember { mutableStateOf(setOf<String>()) }

    var showBatchRegisterSheet by remember { mutableStateOf(false) }
    var showDayDetailSheet by remember { mutableStateOf(false) }
    var clickedDateForDetail by remember { mutableStateOf("") }

    val pagerState = rememberPagerState(initialPage = 120) { 240 }

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
                    availabilitiesByDate = availabilitiesByDate,
                    currentUserId = currentUserId,
                    onDayClick = { day ->
                        val dateStr = "${targetYearMonth.year}年${targetYearMonth.monthValue}月${day}日"
                        if (isMultiSelectMode) {
                            selectedDates = if (selectedDates.contains(dateStr)) {
                                selectedDates - dateStr
                            } else {
                                selectedDates + dateStr
                            }
                        } else {
                            clickedDateForDetail = dateStr
                            showDayDetailSheet = true
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

    if (showDayDetailSheet && clickedDateForDetail.isNotEmpty()) {
        val dayAvailabilities = availabilitiesByDate[clickedDateForDetail] ?: emptyList()
        val myAvailability = dayAvailabilities.find { avail -> avail["userId"] == currentUserId }

        DayDetailBottomSheet(
            dateStr = clickedDateForDetail,
            dayAvailabilities = dayAvailabilities,
            currentUserId = currentUserId,
            initialSlots = (myAvailability?.get("timeSlots") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            initialMemo = (myAvailability?.get("memo") as? String) ?: "",
            onDismiss = { showDayDetailSheet = false },
            onSave = { slots, memo ->
                scope.launch {
                    val groupId = currentGroup?.id ?: return@launch
                    availabilityRepository.saveAvailabilities(
                        groupId = groupId,
                        dates = setOf(clickedDateForDetail),
                        timeSlots = slots,
                        memo = memo
                    ).onSuccess {
                        snackbarMessage = "空き時間を更新しました！"
                    }.onFailure { e ->
                        snackbarMessage = "更新に失敗しました: ${e.localizedMessage}"
                    }
                }
                showDayDetailSheet = false
            }
        )
    }

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
                val targetDates = selectedDates

                scope.launch {
                    if (groups.isEmpty()) {
                        snackbarMessage = "グループに所属していません"
                        return@launch
                    }

                    val groupId = currentGroup?.id ?: return@launch

                    availabilityRepository.saveAvailabilities(
                        groupId = groupId,
                        dates = targetDates,
                        timeSlots = selectedSlots,
                        memo = memo
                    ).onSuccess {
                        snackbarMessage = "空き時間を一括登録しました！"
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

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    isMultiSelectMode: Boolean,
    selectedDates: Set<String>,
    availabilitiesByDate: Map<String, List<Map<String, Any>>>,
    currentUserId: String?,
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

                                val dayAvailabilities = availabilitiesByDate[dateStr] ?: emptyList()
                                val isMyRegistered = dayAvailabilities.any { it["userId"] == currentUserId }
                                val otherAvailabilities = dayAvailabilities.filter { it["userId"] != currentUserId }

                                DayCell(
                                    day = cellDay,
                                    isSelected = isSelected,
                                    isMyRegistered = isMyRegistered,
                                    otherAvailabilities = otherAvailabilities,
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

private val memberColors = listOf(
    Color(0xFFE57373),
    Color(0xFF81C784),
    Color(0xFF64B5F6),
    Color(0xFFFFB74D),
    Color(0xFFBA68C8),
    Color(0xFF4DB6AC)
)

private fun getMemberColor(userId: String): Color {
    val index = Math.abs(userId.hashCode()) % memberColors.size
    return memberColors[index]
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isMyRegistered: Boolean,
    otherAvailabilities: List<Map<String, Any>>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> SukimaLavender.copy(alpha = 0.5f)
        isMyRegistered -> SukimaLavender.copy(alpha = 0.3f)
        else -> SukimaSurfaceGray
    }

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

            if (otherAvailabilities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                val maxVisibleDots = 3
                val visibleItems = otherAvailabilities.take(maxVisibleDots)
                val remainingCount = otherAvailabilities.size - maxVisibleDots

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visibleItems.forEach { item ->
                        val uid = item["userId"] as? String ?: ""
                        val dotColor = getMemberColor(uid)
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                    if (remainingCount > 0) {
                        Text(
                            text = "+$remainingCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailBottomSheet(
    dateStr: String,
    dayAvailabilities: List<Map<String, Any>>,
    currentUserId: String?,
    initialSlots: List<String>,
    initialMemo: String,
    onDismiss: () -> Unit,
    onSave: (List<String>, String) -> Unit
) {
    var selectedSlots by remember { mutableStateOf(if (initialSlots.isNotEmpty()) initialSlots.toSet() else setOf("一日")) }
    var memoText by remember { mutableStateOf(initialMemo) }
    // 🔴 初期状態を閉じた状態（false）にする
    var isEditExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(text = dateStr, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "メンバーの空き状況 (${dayAvailabilities.size}人)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))

            if (dayAvailabilities.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SukimaSurfaceGray,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("この日に空き時間を登録しているメンバーはいません", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(dayAvailabilities) { item ->
                        val uid = item["userId"] as? String ?: ""
                        val userName = item["userName"] as? String ?: "メンバー (${uid.take(4)})"
                        val slots = (item["timeSlots"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val memo = item["memo"] as? String ?: ""
                        val isMe = (uid == currentUserId)

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isMe) SukimaLavender.copy(alpha = 0.2f) else SukimaSurfaceGray,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (!isMe) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(getMemberColor(uid))
                                        )
                                    }
                                    Text(
                                        text = if (isMe) "あなた" else userName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (memo.isNotBlank()) {
                                        Text(
                                            text = "($memo)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Text(
                                    text = slots.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // 🔴 タップしてアコーディオンの開閉を切り替える部分
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { isEditExpanded = !isEditExpanded })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "自分の空き時間を編集",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { isEditExpanded = !isEditExpanded }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isEditExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "編集を開閉"
                    )
                }
            }

            // 🔴 isEditExpandedがtrueのときだけ表示する
            if (isEditExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                val isOneDaySelected = selectedSlots.contains("一日")
                Surface(
                    onClick = { selectedSlots = setOf("一日") },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOneDaySelected) MaterialTheme.colorScheme.primaryContainer else SukimaSurfaceGray,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "一日",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOneDaySelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

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
                                modifier = Modifier.padding(vertical = 6.dp),
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

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    label = { Text("メモ（任意）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onSave(selectedSlots.toList(), memoText) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "この内容で保存・更新する")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "空き時間を設定 (${selectedDates.size}日)",
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
                    text = "詳細（メモ）を入力する",
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
                    label = { Text("メモ（例: バイト後等）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onRegister(selectedSlots.toList(), memoText) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "選択した日に一括登録する")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}