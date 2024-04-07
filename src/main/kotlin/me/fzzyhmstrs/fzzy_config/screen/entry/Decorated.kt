package me.fzzyhmstrs.fzzy_config.screen.entry

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier

interface Decorated {
    fun decorationId(): Identifier
    fun renderDecoration(context: DrawContext, x: Int, y: Int, delta: Float){
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()
        context.drawGuiTexture(decorationId(), x, y, 16, 16)
    }
}