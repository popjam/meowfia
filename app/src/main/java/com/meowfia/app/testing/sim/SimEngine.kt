package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.engine.GameCoordinator
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.roles.NightPrompt
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
        val strategies = config.strategies ?: SimStrategyArchetypes.assignToPlayers(config.playerCount, random)
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
            repeat(3) { if (deck.isNotEmpty()) sp.hand.add(deck.removeFirst()) }
        }

        logger.header("MEOWFIA SIMULATION — Seed: ${random.seed}")
        logger.line("Players: ${config.playerCount}, Rounds: ${config.roundCount}")
        logger.line("Strategies: ${strategies.joinToString { it.name }}")
        logger.separator()

        val roundLogs = mutableListOf<SimRoundLog>()
        val roleAssignmentCounts = mutableMapOf<RoleId, Int>()
        val roleEggTotals = mutableMapOf<RoleId, MutableList<Int>>()
        val alignmentWins = mutableMapOf(Alignment.FARM to 0, Alignment.MEOWFIA to 0)
        val suitThrows = mutableMapOf<Suit, Int>()
        val suitWins = mutableMapOf<Suit, Int>()
        val suitLosses = mutableMapOf<Suit, Int>()
        val clubGiftedValues = mutableListOf<Int>()
        var spadeSteals = 0
        var spadeGives = 0
        var diamondLocks = 0
        var diamondDemotes = 0
        var zeroMeowfiaRounds = 0
        var allMeowfiaRounds = 0

        for (roundNum in 1..config.roundCount) {
            val roundLog = runRound(roundNum, simPlayers, deck, discard, strategies)
            roundLogs.add(roundLog)

            // Accumulate metrics
            for (a in roundLog.assignments) {
                roleAssignmentCounts[a.roleId] = (roleAssignmentCounts[a.roleId] ?: 0) + 1
            }

            val mc = roundLog.meowfiaCount
            if (mc == 0) zeroMeowfiaRounds++
            if (mc == config.playerCount) allMeowfiaRounds++

            roundLog.votingResult?.let { vr ->
                alignmentWins[vr.winningTeam] = (alignmentWins[vr.winningTeam] ?: 0) + 1
                for ((_, cards) in vr.thrown) {
                    for (card in cards) {
                        suitThrows[card.suit] = (suitThrows[card.suit] ?: 0) + 1
                    }
                }
            }

            for (event in roundLog.scoringEvents) {
                if (event.suit != null) {
                    if (event.isWin) suitWins[event.suit] = (suitWins[event.suit] ?: 0) + 1
                    else suitLosses[event.suit] = (suitLosses[event.suit] ?: 0) + 1
                }
                if (event.description.contains("gifted")) {
                    clubGiftedValues.add(event.cardValue)
                }
                if (event.description.contains("Spade stole")) spadeSteals++
                if (event.description.contains("gave")) spadeGives++
                if (event.description.contains("locked")) diamondLocks++
                if (event.description.contains("demoted")) diamondDemotes++
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
            suitThrows = suitThrows,
            suitWins = suitWins,
            suitLosses = suitLosses,
            clubGiftedValues = clubGiftedValues,
            spadeSteals = spadeSteals,
            spadeGives = spadeGives,
            diamondLocks = diamondLocks,
            diamondDemotes = diamondDemotes,
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
        repeat(3) { if (deck.isNotEmpty()) discard.add(deck.removeFirst()) }

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
            repeat(2) { if (deck.isNotEmpty()) sp.hand.add(deck.removeFirst()) }
        }

        // Night phase (real engine)
        val gameState = coordinator.state
        for (sp in simPlayers) {
            val handler = RoleRegistry.get(sp.roleId)
            val prompt = handler.getNightPrompt(sp.player, gameState.players)

            val action: NightAction = when (prompt) {
                is NightPrompt.PickPlayer -> {
                    val targetId = config.forcedVisits?.get(sp.id)
                        ?: sp.strategy.chooseNightTarget(sp, simPlayers, random)
                        ?: random.nextInt(config.playerCount)
                    NightAction.VisitPlayer(targetId)
                }
                is NightPrompt.Automatic -> NightAction.VisitRandom
                is NightPrompt.SelfVisit -> NightAction.VisitSelf
            }
            coordinator.submitNightAction(sp.id, action)
        }

        // Resolution (real engine)
        val resolvedState = coordinator.resolveNight()
        log.resolutionNarrative = resolvedState.nightResults.values.map { it.narrative }.distinct()

        // Dawn
        val dawnReports = (0 until config.playerCount).map { pid ->
            coordinator.getDawnReport(pid)
        }
        log.dawnReports = dawnReports

        for (report in dawnReports) {
            val sp = simPlayers[report.playerId]
            val eggsToDraw = report.reportedNestEggs.coerceIn(0, 5)
            repeat(eggsToDraw) { if (deck.isNotEmpty()) sp.hand.add(deck.removeFirst()) }
        }

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
        val candidates = RoleId.entries.filter { role ->
            role.implemented && !role.isBuffer && (config.includeFlowers || !role.isFlower)
        }
        val revealed = random.shuffle(candidates).take(3)
        return base + revealed.map { PoolCard(it) }
    }
}
