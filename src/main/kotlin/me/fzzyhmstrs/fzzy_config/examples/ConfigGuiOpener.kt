package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

object ConfigGuiOpener {

    @Environment(EnvType.CLIENT)
    fun exampleScreenOpening() {
        //opens all configs registered under the "fzzy_config" namespace
        ConfigApi.openScreen(FC.MOD_ID)
    }
}