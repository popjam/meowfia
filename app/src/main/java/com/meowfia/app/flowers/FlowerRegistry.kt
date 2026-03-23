package com.meowfia.app.flowers

import com.meowfia.app.data.model.RoleId

/** Central registry for flower handlers. */
object FlowerRegistry {
    private val handlers = mutableMapOf<RoleId, FlowerHandler>()

    fun register(handler: FlowerHandler) {
        handlers[handler.roleId] = handler
    }

    fun get(roleId: RoleId): FlowerHandler? = handlers[roleId]

    fun allRegistered(): List<RoleId> = handlers.keys.toList()

    fun initialize() {
        handlers.clear()
        register(SunflowerHandler())
        register(DandelionHandler())
        register(WolfsbaneHandler())
    }
}
