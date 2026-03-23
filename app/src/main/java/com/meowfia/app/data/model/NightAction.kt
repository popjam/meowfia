package com.meowfia.app.data.model

sealed class NightAction {
    data class VisitPlayer(val targetId: Int) : NightAction()
    data object VisitSelf : NightAction()
    data object VisitRandom : NightAction()
    data object NoVisit : NightAction()
}
