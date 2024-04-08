package me.fzzyhmstrs.fzzy_config.screen.widget

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(EnvType.CLIENT)
@JvmDefaultWithCompatibility
interface Scalable {
    fun setWidth(width: Int)
    fun setHeight(height: Int)
}