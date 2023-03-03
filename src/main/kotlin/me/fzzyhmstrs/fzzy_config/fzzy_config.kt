package me.fzzyhmstrs.fzzy_config

import com.llamalad7.mixinextras.MixinExtrasBootstrap
import me.fzzyhmstrs.fzzy_config.config.FcTestConfig
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import net.minecraft.util.Identifier
import kotlin.random.Random

import org.slf4j.LoggerFactory


object FC: ModInitializer {
    const val MOD_ID = "fzzy_core"
    val LOGGER = LoggerFactory.getLogger("emi_loot")
    val fcRandom = Random(System.currentTimeMillis())
    val fallbackId = Identifier("vanishing_curse")

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
