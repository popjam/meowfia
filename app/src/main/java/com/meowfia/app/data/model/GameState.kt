package com.meowfia.app.data.model

data class GameState(
    val roundNumber: Int,
    val players: List<Player>,
    val pool: List<PoolCard>,
    val dealerSeat: Int,
    val nightActions: Map<Int, NightAction> = emptyMap(),
    val nightResults: Map<Int, NightResult> = emptyMap(),
    val dawnReports: Map<Int, DawnReport> = emptyMap(),
    val activeFlowers: List<RoleId> = emptyList(),
    val visitGraph: Map<Int, Int?> = emptyMap(),
    val phase: GamePhase,
    val cawCawCount: Int = 0,
    val eliminatedPlayerId: Int? = null
)
