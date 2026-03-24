package com.meowfia.app.testing.sim

/**
 * Configurable scoring rules for the simulation.
 * Defaults match v6 Meowfia rules.
 */
data class ScoringRules(
    // Hand & draw
    val startingHandSize: Int = 3,
    val postRoundDrawCount: Int = 2,
    val handCap: Int = 0,  // 0 = no cap
    val finalCardValue: Int = 1,  // points per card in hand at end (can be 0 or negative)

    // Throwing
    val minThrowPerRound: Int = 1,

    // Win scoring
    val winThrownAction: WinThrownAction = WinThrownAction.BANK_TO_SCORE_PILE,
    val flatWinBonus: Int = 0,  // extra cards drawn on win
    val flatWinPoints: Int = 0,  // flat bonus points on win
    val flatWinPointsPerCard: Int = 1,  // used with FLAT_POINTS_PER_CARD

    // Loss scoring
    val lossThrownAction: LossThrownAction = LossThrownAction.DISCARD,
    val flatLossPenaltyCards: Int = 0,  // cards discarded on loss
    val flatLossPenaltyPoints: Int = 0,  // flat penalty points on loss

    // Consolation (lose but targeted opposite team)
    val consolation: Consolation = Consolation.RETURN_HIGHEST_TO_HAND,

    // Penalty (win but targeted own team)
    val wrongTargetPenalty: WrongTargetPenalty = WrongTargetPenalty.NONE,

    // Tie-breaking
    val tieBreaker: TieBreaker = TieBreaker.MEOWFIA_WINS
)

enum class WinThrownAction(val displayName: String) {
    BANK_TO_SCORE_PILE("Bank to score pile"),
    FLAT_POINTS_PER_CARD("Flat +points per card"),
    RETURN_TO_HAND("Return to hand")
}

enum class LossThrownAction(val displayName: String) {
    DISCARD("Discard all"),
    RETURN_TO_HAND("Return to hand"),
    GIVE_TO_TARGET_SCORE("Give to target's score pile"),
    NEGATIVE_SCORE("Add as negative score"),
    FLAT_PENALTY_PER_CARD("Flat -points per card")
}

enum class Consolation(val displayName: String) {
    NONE("No consolation"),
    RETURN_HIGHEST_TO_HAND("Return highest card to hand"),
    RETURN_ALL_TO_HAND("Return all cards to hand"),
    BANK_HIGHEST_TO_SCORE("Bank highest card to score"),
    FLAT_POINTS("Flat +points bonus")
}

enum class WrongTargetPenalty(val displayName: String) {
    NONE("No penalty"),
    RETURN_LOWEST("Return lowest banked card to hand"),
    DISCARD_LOWEST("Discard lowest banked card"),
    FLAT_POINTS("Flat -points penalty"),
    GIVE_LOWEST_TO_TARGET_HAND("Give lowest to target's hand"),
    GIVE_LOWEST_TO_TARGET_SCORE("Give lowest to target's score")
}

enum class TieBreaker(val displayName: String) {
    MEOWFIA_WINS("Meowfia wins ties (Farm eliminated)"),
    FARM_WINS("Farm wins ties (Meowfia eliminated)"),
    RANDOM("Random elimination"),
    HIGHEST_CARD_VALUE("Highest total card value wins"),
    LOWEST_CARD_VALUE("Lowest total card value wins")
}
