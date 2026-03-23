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
        // Information
        register(SunflowerHandler())
        register(PitcherPlantHandler())
        register(CactusFlowerHandler())
        // Communication
        register(BanksiaHandler())
        register(FlannelFlowerHandler())
        register(TumbleweedHandler())
        // Day Phase
        register(BluebellHandler())
        register(NightshadeHandler())
        register(MulberryHandler())
        register(StingingBushHandler())
        // Night Phase
        register(MoonflowerHandler())
        register(WolfsbaneHandler())
        register(TwinflowerHandler())
        register(DandelionHandler())
        // Social & Physical
        register(GoldenWattleHandler())
        register(DesertPeaHandler())
        // Round Structure
        register(BirdOfParadiseHandler())
    }
}
