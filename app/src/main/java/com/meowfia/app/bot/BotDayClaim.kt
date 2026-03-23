package com.meowfia.app.bot

import com.meowfia.app.data.model.RoleId

data class BotDayClaim(
    val playerId: Int,
    val botName: String,
    val claimedRole: RoleId,
    val claimedTargetName: String?,
    val claimedEggDelta: Int,
    val isLying: Boolean
) {
    fun toDisplayText(): String {
        val roleLine = "I am a ${claimedRole.displayName}."
        val targetLine = when {
            claimedTargetName == null -> "I stayed home."
            else -> "I visited $claimedTargetName."
        }
        val eggLine = when {
            claimedEggDelta > 0 -> "I gained $claimedEggDelta egg${if (claimedEggDelta != 1) "s" else ""}."
            claimedEggDelta < 0 -> "I lost ${-claimedEggDelta} egg${if (claimedEggDelta != -1) "s" else ""}."
            else -> "My eggs didn't change."
        }
        return "$roleLine\n$targetLine\n$eggLine"
    }
}
