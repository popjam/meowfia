package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.RoleId

data class SimGameResult(
    val seed: Long,
    val config: SimConfig,
    val finalScores: List<Int>,
    val strategies: List<String>,
    val roundLogs: List<SimRoundLog>,
    val perRoundDeltas: List<List<Int>>,
    val roleAssignmentCounts: Map<RoleId, Int>,
    val roleEggTotals: Map<RoleId, MutableList<Int>>,
    val alignmentWins: Map<Alignment, Int>,
    val zeroMeowfiaRounds: Int,
    val allMeowfiaRounds: Int,
    val fullLog: String
)
