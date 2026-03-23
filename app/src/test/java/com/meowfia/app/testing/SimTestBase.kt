package com.meowfia.app.testing

import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.flowers.FlowerRegistry
import org.junit.Before

abstract class SimTestBase {
    @Before
    fun setup() {
        RoleRegistry.initialize()
        FlowerRegistry.initialize()
    }
}
