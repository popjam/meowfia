package com.meowfia.app.data.model

data class PoolCard(
    val roleId: RoleId,
    val cardType: CardType = roleId.cardType
)
