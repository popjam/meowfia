package com.meowfia.app.testing.sim

import com.meowfia.app.bot.BotBrain
import com.meowfia.app.bot.BotClaimGenerator
import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.engine.GameCoordinator
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.testing.reporting.SimLogger
import com.meowfia.app.util.RandomProvider

/**
 * Simulates a complete Meowfia game using the real engine.
 *
 * Layer 1 (Night Resolution): exercises real GameCoordinator, RoleHandlers, NightResolver.
 * Layer 2 (Voting & Scoring): simulated using SimVotingResolver with AI strategies.
 */
class SimEngine(private val config: SimConfig) {

    private val random = RandomProvider(config.seed ?: System.currentTimeMillis())
    private val logger = SimLogger(config.verbosity)
    private val votingResolver = SimVotingResolver()

    val seed: Long get() = random.seed

    fun runGame(): SimGameResult {
        RoleRegistry.initialize()
        FlowerRegistry.initialize()

        val names = config.playerNames ?: SimConfig.DEFAULT_NAMES.take(config.playerCount)
        val strategies = config.strategies
            ?: SimStrategyArchetypes.assignForDistribution(config.playerCount, config.strategyDistribution, random)
        val deck = SimDeck.create(random)
        val discard = mutableListOf<SimCard>()

        val simPlayers = (0 until config.playerCount).map { i ->
            SimPlayer(
                player = Player(id = i, name = names[i]),
                strategy = strategies[i]
            )
        }

        // Deal starting hands
        for (sp in simPlayers) {
            repeat(3) { if (deck.isNotEmpty()) sp.hand.add(deck.removeAt(0)) }
        }

        logger.header("MEOWFIA SIMULATION — Seed: ${random.seed}")
        logger.line("Players: ${config.playerCount}, Rounds: ${config.roundCount}")
        logger.line("Strategies: ${strategies.joinToString { it.name }}")
        logger.separator()

        val roundLogs = mutableListOf<SimRoundLog>()
        val roleAssignmentCounts = mutableMapOf<RoleId, Int>()
        val roleEggTotals = mutableMapOf<RoleId, MutableList<Int>>()
        val alignmentWins = mutableMapOf(Alignment.FARM to 0, Alignment.MEOWFIA to 0)
        var zeroMeowfiaRounds = 0
        var allMeowfiaRounds = 0

        for (roundNum in 1..config.roundCount) {
            val roundLog = runRound(roundNum, simPlayers, deck, discard, strategies)
            roundLogs.add(roundLog)

            // Accumulate metrics
            for (a in roundLog.assignments) {
                roleAssignmentCounts[a.roleId] = (roleAssignmentCounts[a.roleId] ?: 0) + 1
                val dawn = roundLog.dawnReports.find { it.playerId == a.playerId }
                if (dawn != null) {
                    roleEggTotals.getOrPut(a.roleId) { mutableListOf() }.add(dawn.actualEggDelta)
                }
            }

            val mc = roundLog.meowfiaCount
            if (mc == 0) zeroMeowfiaRounds++
            if (mc == config.playerCount) allMeowfiaRounds++

            roundLog.votingResult?.let { vr ->
                alignmentWins[vr.winningTeam] = (alignmentWins[vr.winningTeam] ?: 0) + 1
            }
        }

        return SimGameResult(
            seed = random.seed,
            config = config,
            finalScores = simPlayers.map { it.totalScore() },
            strategies = strategies.map { it.name },
            roundLogs = roundLogs,
            perRoundDeltas = simPlayers.map { sp ->
                roundLogs.map { log -> log.postScores.getOrElse(sp.id) { 0 } }
            },
            roleAssignmentCounts = roleAssignmentCounts,
            roleEggTotals = roleEggTotals,
            alignmentWins = alignmentWins,
            zeroMeowfiaRounds = zeroMeowfiaRounds,
            allMeowfiaRounds = allMeowfiaRounds,
            fullLog = logger.getFullLog()
        )
    }

    private fun runRound(
        roundNum: Int,
        simPlayers: List<SimPlayer>,
        deck: MutableList<SimCard>,
        discard: MutableList<SimCard>,
        strategies: List<SimStrategy>
    ): SimRoundLog {
        logger.roundHeader(roundNum)
        val log = SimRoundLog(roundNum = roundNum)

        // Pool setup
        repeat(3) { if (deck.isNotEmpty()) discard.add(deck.removeAt(0)) }

        val pool = config.forcedPool?.map { PoolCard(it) }
            ?: generateRandomPool()
        val activeFlowers = config.activeFlowerOverride
            ?: pool.filter { it.roleId.isFlower }.map { it.roleId }

        log.pool = pool.map { it.roleId }
        log.activeFlowers = activeFlowers

        // Role assignment (real engine)
        val coordinator = GameCoordinator(random)
        val dealerSeat = (roundNum - 1) % config.playerCount
        log.dealerSeat = dealerSeat

        coordinator.startNewRound(
            roundNumber = roundNum,
            poolCards = pool,
            playerNames = simPlayers.map { it.name },
            dealerSeat = dealerSeat
        )

        val assignments = coordinator.assignRoles(
            forcedAlignments = config.forcedAlignments,
            forcedRoles = config.forcedRoles
        )
        log.assignments = assignments
        log.meowfiaCount = assignments.count { it.alignment == Alignment.MEOWFIA }

        for (a in assignments) {
            simPlayers[a.playerId].updateFromAssignment(a.alignment, a.roleId)
        }

        // Draw 2 cards
        for (sp in simPlayers) {
            repeat(2) { if (deck.isNotEmpty()) sp.hand.add(deck.removeAt(0)) }
        }

        // Night phase (real engine + BotBrain targeting)
        val gameState = coordinator.state
        for (sp in simPlayers) {
            val action = if (config.forcedVisits?.containsKey(sp.id) == true) {
                NightAction.VisitPlayer(config.forcedVisits[sp.id]!!)
            } else {
                BotBrain.chooseNightAction(
                    bot = sp.player,
                    allPlayers = gameState.players,
                    random = random,
                    strategy = sp.strategy.toBotStrategy()
                )
            }
            coordinator.submitNightAction(sp.id, action)
        }

        // Resolution (real engine)
        val resolvedState = coordinator.resolveNight()
        log.resolutionNarrative = resolvedState.nightResults.values.map { it.narrative }.distinct()
        log.visitGraph = resolvedState.visitGraph.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

        // Dawn
        val dawnReports = (0 until config.playerCount).map { pid ->
            coordinator.getDawnReport(pid)
        }
        log.dawnReports = dawnReports
        log.confusedPlayers = dawnReports.count { it.isConfused }

        // v6: draw or discard based on egg delta
        for (report in dawnReports) {
            val sp = simPlayers[report.playerId]
            val delta = report.reportedEggDelta
            if (delta > 0) {
                repeat(delta.coerceAtMost(5)) { if (deck.isNotEmpty()) sp.hand.add(deck.removeAt(0)) }
            } else if (delta < 0) {
                repeat((-delta).coerceAtMost(sp.hand.size)) {
                    if (sp.hand.isNotEmpty()) {
                        discard.add(sp.hand.removeAt(random.nextInt(sp.hand.size)))
                    }
                }
            }
        }

        // Generate claims for solvability analysis
        val farmRolesInPool = pool.map { it.roleId }.filter { it.isFarmAnimal }
        val claims = mutableMapOf<Int, ClaimData>()
        for (sp in simPlayers) {
            val claim = BotClaimGenerator.generateClaim(
                bot = sp.player,
                allPlayers = gameState.players,
                dawnReport = dawnReports.find { it.playerId == sp.id }!!,
                visitGraph = coordinator.state.visitGraph,
                pool = farmRolesInPool,
                random = random
            )
            claims[sp.id] = ClaimData(
                playerId = sp.id,
                claimedRole = claim.claimedRole,
                claimedTargetId = gameState.players.find { it.name == claim.claimedTargetName }?.id,
                claimedEggDelta = claim.claimedEggDelta
            )
        }

        // Solvability analysis
        log.solvability = RoundSolver.analyze(
            claims = claims,
            pool = pool.map { it.roleId },
            dawnReports = dawnReports,
            assignments = assignments,
            visitGraph = coordinator.state.visitGraph
        )
        log.solvability?.let { logger.solvability(it) }

        // Voting (simulated)
        val votingResult = votingResolver.resolve(simPlayers, assignments, dawnReports, random)
        log.votingResult = votingResult

        // Scoring (simulated)
        val scoringEvents = votingResolver.resolveScoring(simPlayers, votingResult, assignments, random)
        log.scoringEvents = scoringEvents
        log.postScores = simPlayers.map { it.totalScore() }

        // Reshuffle if low
        if (deck.size < config.playerCount * 3) {
            deck.addAll(random.shuffle(discard))
            discard.clear()
        }

        logger.separator()
        return log
    }

    private fun generateRandomPool(): List<PoolCard> {
        val base = listOf(PoolCard(RoleId.PIGEON), PoolCard(RoleId.HOUSE_CAT))
        val allowed = config.allowedRoles
        val candidates = RoleId.entries.filter { role ->
            role.implemented && !role.isBuffer &&
                (config.includeFlowers || !role.isFlower) &&
                (allowed == null || role in allowed)
        }
        val revealed = random.shuffle(candidates).take(3)
        return base + revealed.map { PoolCard(it) }
    }
}
