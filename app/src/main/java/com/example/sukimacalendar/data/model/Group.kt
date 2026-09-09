package com.example.sukimacalendar.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val members: List<Map<String, String>> = emptyList()
)