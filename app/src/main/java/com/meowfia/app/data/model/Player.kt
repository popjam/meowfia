package com.meowfia.app.data.model

data class Player(
    val id: Int,
    val name: String,
    val alignment: Alignment = Alignment.FARM,
    val roleId: RoleId = RoleId.PIGEON,
    val originalRoleId: RoleId = roleId,
    val nestEggCount: Int = 0,
    val statusEffects: Set<StatusEffect> = emptySet()
)
