/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.decoration

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureSet
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import net.minecraft.client.gui.DrawContext

//client
interface SpriteDecorated: Decorated {
    fun decorationId(): TextureSet
    val w: Int
        get() = 16
    val h: Int
        get() = 16
    override fun renderDecoration(context: DrawContext, x: Int, y: Int, delta: Float, enabled: Boolean, selected: Boolean) {
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()
        context.drawTex(decorationId().get(enabled, selected), x, y, w, h)
    }
}