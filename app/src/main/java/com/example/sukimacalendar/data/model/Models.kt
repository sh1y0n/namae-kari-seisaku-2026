package com.example.sukimacalendar.data.model

// ===============================================
// Models.kt
// 役割: 画面をダミーデータで動かすための最小限のデータ構造。
//      今はFirestoreと繋がっていないので、
//      各画面ではここで定義したダミーデータ(sampleGroups等)を仮表示に使う。
//      後でFirestoreと連携するときは、このクラスの形をコレクションの
//      ドキュメント構造に合わせて調整して使い回す想定。
// ===============================================

data class Group(
    val id: String,
    val name: String
)

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
    MemberAvailability("かたな", listOf(TimeSlot.MORNING, TimeSlot.AFTERNOON, TimeSlot.NIGHT)) // 「一日」= 全枠
)
