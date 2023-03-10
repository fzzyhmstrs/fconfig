package me.fzzyhmstrs.fzzy_config

import me.fzzyhmstrs.fzzy_config.config.FcTestConfig
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import kotlin.random.Random

import org.slf4j.LoggerFactory


object FC: ModInitializer {
    const val MOD_ID = "fzzy_config"
    val LOGGER = LoggerFactory.getLogger("fzzy_config")

    override fun onInitialize() {
        SyncedConfigRegistry.registerServer()
        FcTestConfig.initConfig()
    }
}

object FCC: ClientModInitializer {
    val acRandom = Random(System.currentTimeMillis())

    override fun onInitializeClient() {
        SyncedConfigRegistry.registerClient()
    }
}
