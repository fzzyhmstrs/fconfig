package me.fzzyhmstrs.fzzy_config.test

import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import net.minecraft.entity.player.PlayerEntity

object TestConfigApi {

    fun printChangeHistory(history: List<String>, id: String, player: PlayerEntity? = null){
        ConfigApiImpl.printChangeHistory(history, id, player)
    }

}