package com.meowfia.app.data.registry

import com.meowfia.app.data.model.RoleId
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.farm.BlackSwanHandler
import com.meowfia.app.roles.farm.ChickenHandler
import com.meowfia.app.roles.farm.EagleHandler
import com.meowfia.app.roles.farm.FalconHandler
import com.meowfia.app.roles.farm.FrogHandler
import com.meowfia.app.roles.farm.HawkHandler
import com.meowfia.app.roles.farm.MosquitoHandler
import com.meowfia.app.roles.farm.OwlHandler
import com.meowfia.app.roles.farm.PigeonHandler
import com.meowfia.app.roles.farm.SheepHandler
import com.meowfia.app.roles.farm.SwitcherooHandler
import com.meowfia.app.roles.farm.TitHandler
import com.meowfia.app.roles.farm.TurkeyHandler
import com.meowfia.app.roles.farm.BlindHawkHandler
import com.meowfia.app.roles.farm.LovebirdHandler
import com.meowfia.app.roles.farm.KookaburraHandler
import com.meowfia.app.roles.farm.MagpieHandler
import com.meowfia.app.roles.farm.KoalaHandler
import com.meowfia.app.roles.farm.SheepdogHandler
import com.meowfia.app.roles.farm.UglyDucklingHandler
import com.meowfia.app.roles.meowfia.HouseCatHandler
import com.meowfia.app.roles.meowfia.TopCatHandler
import com.meowfia.app.roles.meowfia.MouserHandler
import com.meowfia.app.roles.meowfia.FlooferHandler

/** Central registry mapping each RoleId to its handler implementation. */
object RoleRegistry {
    private val handlers = mutableMapOf<RoleId, RoleHandler>()

    fun register(handler: RoleHandler) {
        handlers[handler.roleId] = handler
    }

    fun get(roleId: RoleId): RoleHandler =
        handlers[roleId] ?: error("No handler registered for $roleId")

    fun isRegistered(roleId: RoleId): Boolean = roleId in handlers

    fun allRegistered(): List<RoleId> = handlers.keys.toList()

    fun initialize() {
        handlers.clear()
        register(PigeonHandler())
        register(HouseCatHandler())
        register(HawkHandler())
        register(OwlHandler())
        register(EagleHandler())
        register(TurkeyHandler())
        register(FalconHandler())
        register(MosquitoHandler())
        register(ChickenHandler())
        register(TitHandler())
        register(BlackSwanHandler())
        register(FrogHandler())
        register(SheepHandler())
        register(SwitcherooHandler())
        register(BlindHawkHandler())
        register(LovebirdHandler())
        register(KookaburraHandler())
        register(MagpieHandler())
        register(TopCatHandler())
        register(KoalaHandler())
        register(SheepdogHandler())
        register(MouserHandler())
        register(FlooferHandler())
        register(UglyDucklingHandler())
    }
}
