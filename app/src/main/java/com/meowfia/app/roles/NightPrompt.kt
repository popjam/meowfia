package com.meowfia.app.roles

/** Describes what the UI should show a player during the night phase. */
sealed class NightPrompt {
    /** Player must pick another player to visit. */
    data class PickPlayer(
        val instructionText: String,
        val excludeSelf: Boolean = true
    ) : NightPrompt()

    /** Action is automatic — no player input needed. */
    data class Automatic(
        val instructionText: String
    ) : NightPrompt()

    /** Player visits themselves. */
    data class SelfVisit(
        val instructionText: String
    ) : NightPrompt()
}
