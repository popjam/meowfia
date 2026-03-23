package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.data.model.RoleId

data class SimRoundLog(
    val roundNum: Int,
    var pool: List<RoleId> = emptyList(),
    var activeFlowers: List<RoleId> = emptyList(),
    var assignments: List<PlayerAssignment> = emptyList(),
    var dealerSeat: Int = 0,
    var meowfiaCount: Int = 0,
    var resolutionNarrative: List<String> = emptyList(),
    var dawnReports: List<DawnReport> = emptyList(),
    var votingResult: SimVotingResolver.VotingResult? = null,
    var scoringEvents: List<SimVotingResolver.ScoringEvent> = emptyList(),
    var postScores: List<Int> = emptyList()
)
