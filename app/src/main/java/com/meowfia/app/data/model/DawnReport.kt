package com.meowfia.app.data.model

data class DawnReport(
    val playerId: Int,
    val reportedNestEggs: Int,
    val actualNestEggs: Int,
    val additionalInfo: List<String>,
    val isConfused: Boolean = false
)
