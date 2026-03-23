package com.meowfia.app.data.model

data class DawnReport(
    val playerId: Int,
    val reportedEggDelta: Int,
    val actualEggDelta: Int,
    val additionalInfo: List<String>,
    val isConfused: Boolean = false
)
