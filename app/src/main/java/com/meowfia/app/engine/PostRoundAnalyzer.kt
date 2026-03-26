package com.meowfia.app.engine

import com.meowfia.app.config.RoleResolutionConfig
import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.AlignmentChangeEntry
import com.meowfia.app.data.model.EggSummaryEntry
import com.meowfia.app.data.model.EliminationSummary
import com.meowfia.app.data.model.FlowerSummary
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.NightActionEntry
import com.meowfia.app.data.model.PlayerAssignmentSummary
import com.meowfia.app.data.model.PoolSummary
import com.meowfia.app.data.model.PostRoundAnalysis
import com.meowfia.app.data.model.RoleChangeEntry
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.model.PlayerClaimSummary
import com.meowfia.app.data.model.SolvabilityAnalysis
import com.meowfia.app.data.model.StatusEffectEntry
import com.meowfia.app.data.model.VisitEntry
import com.meowfia.app.testing.sim.ClaimData
import com.meowfia.app.testing.sim.RoundSolver

/**
 * Builds a [PostRoundAnalysis] from the completed round's [GameState] and
 * [ResolutionContext]. This is a read-only analysis — it does not mutate
 * any game state.
 */
object PostRoundAnalyzer {

    fun analyze(
        gameState: GameState,
        context: ResolutionContext,
        winningTeam: Alignment?,
        botClaims: List<com.meowfia.app.bot.BotDayClaim> = emptyList()
    ): PostRoundAnalysis {
        val players = gameState.players

        // Pool summary
        val poolSummary = PoolSummary(
            roles = gameState.pool
                .filter { !it.roleId.isFlower }
                .map { it.roleId.displayName },
            flowers = gameState.pool
                .filter { it.roleId.isFlower }
                .map { it.roleId.displayName }
        )

        // Player assignments (using originalRoleId for what they were dealt)
        val playerAssignments = players.map { player ->
            PlayerAssignmentSummary(
                playerId = player.id,
                playerName = player.name,
                alignment = player.alignment,
                roleName = player.originalRoleId.displayName,
                roleDescription = player.originalRoleId.description,
                isBot = player.isBot
            )
        }

        // Active flowers
        val activeFlowers = gameState.activeFlowers.map { flowerId ->
            FlowerSummary(
                name = flowerId.displayName,
                description = flowerId.description
            )
        }

        // Resolution order
        val useDandelion = gameState.activeFlowers.contains(RoleId.DANDELION)
        val sortedActors = if (useDandelion) {
            RoleResolutionConfig.getSeatOrder(players, gameState.dealerSeat)
        } else {
            RoleResolutionConfig.getResolutionOrder(players, gameState.dealerSeat)
        }
        val resolutionOrder = sortedActors.map { actor ->
            val priority = RoleResolutionConfig.getPriority(actor.originalRoleId)
            "${actor.name} (${actor.originalRoleId.displayName}, tier $priority)"
        }

        // Visit map
        val visitMap = players.map { player ->
            val targetId = gameState.visitGraph[player.id]
            val targetName = targetId?.let { id -> players.find { it.id == id }?.name }
            VisitEntry(
                visitorId = player.id,
                visitorName = player.name,
                targetName = targetName
            )
        }

        // Night walkthrough — one entry per actor in resolution order
        val nightWalkthrough = sortedActors.map { actor ->
            val targetId = gameState.visitGraph[actor.id]
            val target = targetId?.let { id -> players.find { it.id == id } }
            val action = gameState.nightActions[actor.id]
            val priority = RoleResolutionConfig.getPriority(actor.originalRoleId)
            val isHugged = context.isHugged(actor.id)

            NightActionEntry(
                playerId = actor.id,
                playerName = actor.name,
                roleName = actor.originalRoleId.displayName,
                alignment = actor.alignment,
                action = describeAction(actor.originalRoleId, action),
                targetName = target?.name,
                outcome = describeOutcome(
                    roleId = actor.originalRoleId,
                    actorName = actor.name,
                    target = target,
                    context = context,
                    gameState = gameState,
                    isHugged = isHugged
                ),
                priority = priority
            )
        }

        // Role changes
        val finalRoles = context.computeFinalRoles()
        val roleChanges = finalRoles.mapNotNull { (playerId, newRole) ->
            val player = players.find { it.id == playerId } ?: return@mapNotNull null
            if (player.originalRoleId == newRole) return@mapNotNull null
            RoleChangeEntry(
                playerId = playerId,
                playerName = player.name,
                fromRole = player.originalRoleId.displayName,
                toRole = newRole.displayName,
                cause = inferRoleChangeCause(playerId, gameState)
            )
        }

        // Alignment changes
        val finalAlignments = context.computeFinalAlignments()
        val alignmentChanges = finalAlignments.mapNotNull { (playerId, newAlignment) ->
            val player = players.find { it.id == playerId } ?: return@mapNotNull null
            // Find original alignment from assignment (before sheep swap)
            val originalAlignment = if (newAlignment != player.alignment) player.alignment
            else return@mapNotNull null // no actual change
            AlignmentChangeEntry(
                playerId = playerId,
                playerName = player.name,
                fromAlignment = originalAlignment,
                toAlignment = newAlignment,
                cause = "Sheep visited this player and adopted their alignment"
            )
        }

        // Egg summary
        val eggSummary = players.map { player ->
            val delta = context.getClampedEggDelta(player.id)
            val rawDelta = context.getEggDelta(player.id)
            val clamped = rawDelta != delta
            EggSummaryEntry(
                playerId = player.id,
                playerName = player.name,
                delta = delta,
                breakdown = buildEggBreakdown(player.id, player.name, delta, clamped, rawDelta, gameState, context)
            )
        }.filter { it.delta != 0 }

        // Status effects
        val statusEffects = context.buildNightResults().values
            .flatMap { result -> result.statusApplied }
            .mapNotNull { (playerId, effect) ->
                val player = players.find { it.id == playerId } ?: return@mapNotNull null
                StatusEffectEntry(
                    playerId = playerId,
                    playerName = player.name,
                    effect = effect,
                    cause = effect.name.lowercase().replaceFirstChar { it.uppercase() }
                )
            }
            .distinctBy { it.playerId to it.effect }

        // Elimination
        val eliminationSummary = gameState.eliminatedPlayerId?.let { eliminatedId ->
            val eliminated = players.find { it.id == eliminatedId } ?: return@let null
            EliminationSummary(
                playerId = eliminatedId,
                playerName = eliminated.name,
                alignment = eliminated.alignment,
                roleName = eliminated.roleId.displayName,
                wasCorrectElimination = eliminated.alignment == Alignment.MEOWFIA
            )
        }

        // Solvability analysis — build claims from actual game data
        // Only meaningful when Farm outnumbers or equals Meowfia (Farm is trying to find cats).
        // When Meowfia is the majority, the vote dynamic flips and deduction is irrelevant.
        val farmCount = players.count { it.alignment == Alignment.FARM }
        val meowCount = players.count { it.alignment == Alignment.MEOWFIA }

        // Build claim summaries using actual bot claims where available
        val botClaimMap = botClaims.associateBy { it.playerId }
        val playerClaimSummaries = players.map { player ->
            val botClaim = botClaimMap[player.id]
            val isMeowfia = player.alignment == Alignment.MEOWFIA

            if (botClaim != null) {
                // Use the bot's actual claim (Farm bots are truthful, Meowfia bots lie)
                PlayerClaimSummary(
                    playerId = player.id,
                    playerName = player.name,
                    claimedRole = botClaim.claimedRole.displayName,
                    claimedTarget = botClaim.claimedTargetName,
                    claimedEggDelta = botClaim.claimedEggDelta,
                    actualRole = player.roleId.displayName,
                    actualAlignment = player.alignment.displayName,
                    wasLying = botClaim.isLying
                )
            } else {
                // Human player — use real data (assumed truthful if Farm)
                val targetId = gameState.visitGraph[player.id]
                val targetName = targetId?.let { id -> players.find { it.id == id }?.name }
                val delta = context.getClampedEggDelta(player.id)
                PlayerClaimSummary(
                    playerId = player.id,
                    playerName = player.name,
                    claimedRole = if (isMeowfia) "???" else player.originalRoleId.displayName,
                    claimedTarget = if (isMeowfia) "???" else targetName,
                    claimedEggDelta = if (isMeowfia) 0 else delta,
                    actualRole = player.roleId.displayName,
                    actualAlignment = player.alignment.displayName,
                    wasLying = isMeowfia
                )
            }
        }

        val solvability = try {
            if (meowCount > farmCount || farmCount == meowCount) {
                // Meowfia majority or even split — solvability doesn't apply
                val reason = if (meowCount > farmCount)
                    "Meowfia outnumbers Farm ($meowCount vs $farmCount) this round. " +
                        "The cats control the vote, so deduction from Farm's perspective doesn't matter — " +
                        "the Meowfia just need to coordinate to eliminate a Farm player."
                else
                    "Teams are evenly split ($farmCount each) this round. " +
                        "With equal numbers, Meowfia can force a tie and the vote dynamic breaks down — " +
                        "deduction from public information is not meaningful."
                val skipReason = if (meowCount > farmCount)
                    "Meowfia majority ($meowCount Meowfia, $farmCount Farm) — analysis skipped"
                else
                    "Even split ($farmCount each) — analysis skipped"
                SolvabilityAnalysis(
                    verdict = "N/A",
                    verdictExplanation = reason,
                    suspects = emptyList(),
                    cleared = emptyList(),
                    consistentWorlds = 0,
                    totalCandidates = 0,
                    reasons = listOf(skipReason),
                    playerClaims = playerClaimSummaries,
                    worldDescriptions = emptyList()
                )
            } else {
                // Build claims from what players actually said, not their real roles
                val claims = players.associate { player ->
                    val botClaim = botClaimMap[player.id]
                    if (botClaim != null) {
                        // Use the bot's actual claim
                        val claimedTargetId = players.find { it.name == botClaim.claimedTargetName }?.id
                        player.id to ClaimData(
                            playerId = player.id,
                            claimedRole = botClaim.claimedRole,
                            claimedTargetId = claimedTargetId,
                            claimedEggDelta = botClaim.claimedEggDelta
                        )
                    } else {
                        // Human: Farm assumed truthful, Meowfia unknown
                        val targetId = gameState.visitGraph[player.id]
                        val delta = context.getClampedEggDelta(player.id)
                        player.id to ClaimData(
                            playerId = player.id,
                            claimedRole = player.originalRoleId,
                            claimedTargetId = targetId,
                            claimedEggDelta = delta
                        )
                    }
                }
                val dawnReports = gameState.dawnReports.values.toList().ifEmpty {
                    players.map { p ->
                        com.meowfia.app.data.model.DawnReport(
                            playerId = p.id,
                            reportedEggDelta = context.getClampedEggDelta(p.id),
                            actualEggDelta = context.getClampedEggDelta(p.id),
                            additionalInfo = context.getInfoFor(p.id)
                        )
                    }
                }
                val assignments = players.map { p ->
                    com.meowfia.app.data.model.PlayerAssignment(p.id, p.alignment, p.originalRoleId)
                }
                val result = RoundSolver.analyze(
                    claims = claims,
                    pool = gameState.pool.map { it.roleId },
                    dawnReports = dawnReports,
                    assignments = assignments,
                    visitGraph = gameState.visitGraph,
                    actualVisitGraph = gameState.visitGraph,
                    playerNames = players.associate { it.id to it.name },
                    exhaustiveSearch = true  // thorough analysis for post-game review
                )
                val actualMeowfia = players.filter { it.alignment == Alignment.MEOWFIA }.map { it.id }.toSet()

                val verdictExplanation = when {
                    result.consistentWorlds == 0 ->
                        "Someone's claims are impossible — no combination of Meowfia assignments can explain what everyone said. At least one player is definitely lying, but we can't determine exactly who from the claims alone."
                    result.solvability == RoundSolver.Solvability.SOLVED ->
                        "Based on what everyone claimed, there's only one possible explanation for who the Meowfia are. A careful player could figure it out."
                    result.solvability == RoundSolver.Solvability.NARROWED ->
                        "Some players can be ruled out as Meowfia based on the claims, but there are still multiple possibilities. The group has useful information but can't be certain."
                    else ->
                        "Everyone's claims are consistent with many different Meowfia combinations. There's no way to narrow it down from public information alone — it's a guessing game."
                }

                val worldDescriptions = result.consistentWorldDetails.map { meowfiaSet ->
                    // In this world, Farm players have their claimed role, Meowfia get House Cat
                    val assumedRoles = players.associate { p ->
                        val name = p.name
                        val role = if (p.id in meowfiaSet) {
                            RoleId.HOUSE_CAT.displayName
                        } else {
                            claims[p.id]?.claimedRole?.displayName ?: p.originalRoleId.displayName
                        }
                        name to role
                    }
                    com.meowfia.app.data.model.WorldDescription(
                        meowfiaNames = meowfiaSet.mapNotNull { id -> players.find { it.id == id }?.name },
                        farmNames = players.filter { it.id !in meowfiaSet }.map { it.name },
                        isActualWorld = meowfiaSet == actualMeowfia,
                        assumedRoles = assumedRoles
                    )
                }

                SolvabilityAnalysis(
                    verdict = when (result.solvability) {
                        RoundSolver.Solvability.SOLVED -> "SOLVED"
                        RoundSolver.Solvability.NARROWED -> "NARROWED"
                        RoundSolver.Solvability.COIN_FLIP -> "COIN FLIP"
                    },
                    verdictExplanation = verdictExplanation,
                    suspects = result.suspects.mapNotNull { id -> players.find { it.id == id }?.name },
                    cleared = result.cleared.mapNotNull { id -> players.find { it.id == id }?.name },
                    consistentWorlds = result.consistentWorlds,
                    totalCandidates = result.totalCandidates,
                    reasons = result.reasons,
                    playerClaims = playerClaimSummaries,
                    suspicionRanking = result.suspicionRatings.map { (id, rating) ->
                        val player = players.find { it.id == id }
                        com.meowfia.app.data.model.SuspicionEntry(
                            playerName = player?.name ?: "Player $id",
                            percent = (rating * 100).toInt(),
                            isActualMeowfia = player?.alignment == Alignment.MEOWFIA
                        )
                    }.sortedByDescending { it.percent },
                    worldDescriptions = worldDescriptions
                )
            }
        } catch (_: Exception) { null }

        return PostRoundAnalysis(
            roundNumber = gameState.roundNumber,
            poolSummary = poolSummary,
            playerAssignments = playerAssignments,
            activeFlowers = activeFlowers,
            resolutionOrder = resolutionOrder,
            nightWalkthrough = nightWalkthrough,
            roleChanges = roleChanges,
            alignmentChanges = alignmentChanges,
            eggSummary = eggSummary,
            statusEffects = statusEffects,
            visitMap = visitMap,
            eliminationSummary = eliminationSummary,
            winningTeam = winningTeam,
            narrativeLog = context.getNarrativeLog(),
            solvability = solvability
        )
    }

    private fun describeAction(roleId: RoleId, action: NightAction?): String {
        return when (action) {
            is NightAction.VisitPlayer -> "Visited"
            is NightAction.VisitSelf -> "Visited self"
            is NightAction.VisitRandom -> when (roleId) {
                RoleId.MOSQUITO -> "Visited random player"
                RoleId.TIT -> "Visited random Meowfia player"
                else -> "Visited random player"
            }
            is NightAction.NoVisit -> "Stayed home"
            null -> "No action recorded"
        }
    }

    private fun describeOutcome(
        roleId: RoleId,
        actorName: String,
        target: com.meowfia.app.data.model.Player?,
        context: ResolutionContext,
        gameState: GameState,
        isHugged: Boolean
    ): String {
        if (isHugged) return "Blocked — $actorName was hugged and couldn't act."

        val targetName = target?.name ?: "nobody"

        return when (roleId) {
            RoleId.PIGEON -> {
                if (target == null) "No target — no eggs laid."
                else "Laid 1 egg in ${targetName}'s nest."
            }
            RoleId.CHICKEN -> {
                if (target == null) "No target — no eggs laid."
                else "Laid 2 eggs in ${targetName}'s nest."
            }
            RoleId.MOSQUITO -> {
                if (target == null) "No target available — no eggs laid."
                else "Randomly visited $targetName and laid 1 egg in their nest."
            }
            RoleId.TIT -> {
                if (target == null) "No Meowfia players to visit — nothing happened."
                else "Randomly visited $targetName (Meowfia) and laid 1 egg in their nest."
            }
            RoleId.HAWK -> {
                if (target == null) "No target — no investigation."
                else if (target.alignment == Alignment.MEOWFIA) {
                    "Investigated $targetName — they're Meowfia! Gained 1 egg."
                } else {
                    "Investigated $targetName — they're not Meowfia. No eggs gained."
                }
            }
            RoleId.OWL -> {
                if (target == null) "No target — no investigation."
                else {
                    val visitors = context.getVisitorsOf(target.id)
                        .filter { it.id != target.id }
                    if (visitors.isEmpty()) {
                        "Visited $targetName — nobody else visited them. Laid 1 egg in their nest."
                    } else {
                        val visitorRoles = visitors.joinToString(", ") { it.originalRoleId.displayName }
                        "Visited $targetName — learned these animals visited: $visitorRoles."
                    }
                }
            }
            RoleId.EAGLE -> {
                if (target == null) "No target — no eggs gained."
                else {
                    val visitorCount = context.getVisitorsOf(target.id)
                        .filter { it.id != target.id }
                        .size
                    if (visitorCount == 0) {
                        "Visited $targetName — nobody else visited them. No eggs gained."
                    } else {
                        "Visited $targetName — $visitorCount other visitor${if (visitorCount != 1) "s" else ""}. Gained $visitorCount egg${if (visitorCount != 1) "s" else ""}."
                    }
                }
            }
            RoleId.TURKEY -> {
                val visitors = context.getVisitorsOf(gameState.players.find { it.name == actorName }!!.id)
                if (visitors.isEmpty()) {
                    "Stayed home — nobody visited. No eggs laid."
                } else {
                    val visitorNames = visitors.joinToString(", ") { it.name }
                    "Stayed home — ${visitors.size} visitor${if (visitors.size != 1) "s" else ""} ($visitorNames). Laid 1 egg in each of their nests."
                }
            }
            RoleId.FALCON -> {
                if (target == null) "No target — no eggs laid."
                else {
                    val targetsTarget = context.getVisitTargetOf(target.id)
                    if (targetsTarget == null) {
                        "Visited $targetName, but they didn't visit anyone — no eggs laid."
                    } else {
                        "Visited $targetName — they visited ${targetsTarget.name}. Laid 1 egg in ${targetsTarget.name}'s nest."
                    }
                }
            }
            RoleId.BLACK_SWAN -> {
                val actor = gameState.players.find { it.name == actorName }!!
                if (actor.roleId == RoleId.BLACK_SWAN) {
                    "Visited self — still Black Swan. Gained 1 egg."
                } else {
                    "Visited self — role was swapped to ${actor.roleId.displayName}. No egg gained."
                }
            }
            RoleId.FROG -> {
                if (target == null) "No target — no swap."
                else "Swapped roles with $targetName. Learned new role: ${target.originalRoleId.displayName}."
            }
            RoleId.SWITCHEROO -> {
                if (target == null) "No target — no swap triggered."
                else {
                    val targetsTarget = context.getVisitTargetOf(target.id)
                    if (targetsTarget == null) {
                        "Visited $targetName, but they didn't visit anyone — no swap triggered."
                    } else {
                        "Visited $targetName — caused them to swap roles with ${targetsTarget.name}."
                    }
                }
            }
            RoleId.SHEEP -> {
                if (target == null) "No target — no alignment change."
                else "Visited $targetName — adopted their alignment (${target.alignment.displayName})."
            }
            RoleId.HOUSE_CAT -> {
                if (target == null) "No target — no intel gathered."
                else {
                    val targetsVisit = context.getVisitTargetOf(target.id)
                    val visitInfo = if (targetsVisit != null) "visited ${targetsVisit.name}" else "visited nobody"
                    "Scouted $targetName — they're a ${target.originalRoleId.displayName} who $visitInfo."
                }
            }
            else -> "Resolved with standard behavior."
        }
    }

    private fun inferRoleChangeCause(
        playerId: Int,
        gameState: GameState
    ): String {
        val player = gameState.players.find { it.id == playerId } ?: return "Unknown"

        // Check if a Frog visited this player
        for (other in gameState.players) {
            if (other.id == playerId) continue
            if (other.originalRoleId == RoleId.FROG && gameState.visitGraph[other.id] == playerId) {
                return "Frog (${other.name}) swapped roles with them"
            }
        }

        // Check if a Switcheroo caused the swap
        for (other in gameState.players) {
            if (other.originalRoleId == RoleId.SWITCHEROO) {
                val switcherooTarget = gameState.visitGraph[other.id]
                if (switcherooTarget != null) {
                    val targetVisited = gameState.visitGraph[switcherooTarget]
                    if (playerId == switcherooTarget || playerId == targetVisited) {
                        return "Switcheroo (${other.name}) caused a swap"
                    }
                }
            }
        }

        // Player is a Frog who swapped
        if (player.originalRoleId == RoleId.FROG) {
            val target = gameState.visitGraph[playerId]?.let { id ->
                gameState.players.find { it.id == id }
            }
            return if (target != null) "Swapped with ${target.name} (Frog ability)"
            else "Frog swap"
        }

        return "Role swap during night"
    }

    private fun buildEggBreakdown(
        playerId: Int,
        playerName: String,
        delta: Int,
        clamped: Boolean,
        rawDelta: Int,
        gameState: GameState,
        context: ResolutionContext
    ): String {
        val parts = mutableListOf<String>()

        if (delta > 0) parts.add("+$delta egg${if (delta != 1) "s" else ""}")
        else if (delta < 0) parts.add("$delta egg${if (delta != -1) "s" else ""}")

        if (clamped) {
            parts.add("(raw: ${if (rawDelta > 0) "+" else ""}$rawDelta, clamped to $delta)")
        }

        return parts.joinToString(" ")
    }
}
