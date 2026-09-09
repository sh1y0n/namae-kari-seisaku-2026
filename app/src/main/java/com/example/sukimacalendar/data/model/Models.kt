package com.example.sukimacalendar.data.model

// ===============================================
// Models.kt
// 役割: 画面をダミーデータで動かすための最小限のデータ構造。
// ===============================================

// 朝・昼・夜の3枠。PPTXの「この日の空き時間」の選択肢そのもの。
enum class TimeSlot(val label: String) {
    MORNING("朝"),
    AFTERNOON("昼"),
    NIGHT("夜")
}

data class MemberAvailability(
    val memberName: String,
    val availableSlots: List<TimeSlot>
)

// ---- 骨組み段階で画面に仮表示するダミーデータ ----
val sampleGroups = listOf(
    Group(id = "1", name = "test group"),
    Group(id = "2", name = "test group2")
)

val sampleAvailability = listOf(
    MemberAvailability("よしとう", listOf(TimeSlot.NIGHT)),
    MemberAvailability("かたな", listOf(TimeSlot.MORNING, TimeSlot.AFTERNOON, TimeSlot.NIGHT))
)