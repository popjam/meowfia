package com.meowfia.app.data.model

data class NightResult(
    val playerId: Int,
    val eggDeltas: Map<Int, Int>,
    val informationGained: List<String>,
    val statusApplied: List<Pair<Int, StatusEffect>>,
    val narrative: String
)
