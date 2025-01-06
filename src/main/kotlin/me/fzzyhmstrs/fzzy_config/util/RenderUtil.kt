/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier
import java.awt.Color

/**
 * Render utils for DrawContext to provide functionality similar to 1.20.2+ sprite rendering
 * @author fzzyhmstrs
 * @since 0.2.0
 */
object RenderUtil {

    /**
     * Extension function to replicate drawGuiTexture from 1.20.2-1.21.1. Uses the [RenderLayer.getGuiTextured] method to fill in the function param
     * @param id Identifier - The sprite identifier for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width - the width of the texture
     * @param height - the height of the texture
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun DrawContext.drawTex(id: Identifier, x: Int, y: Int, width: Int, height: Int) {
        this.drawGuiTexture(id, x, y, width, height)
    }

    /**
     * Extension function to replicate drawGuiTexture from 1.20.2-1.21.1. Uses the [RenderLayer.getGuiTextured] method to fill in the function param
     * @param id Identifier - The sprite identifier for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width - the width of the texture
     * @param height - the height of the texture
     * @param color - the color of the texture
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun DrawContext.drawTex(id: Identifier, x: Int, y: Int, width: Int, height: Int, color: Int) {
        val floats = Color(color).getComponents(floatArrayOf(0f, 0f, 0f, 0f))
        RenderSystem.setShaderColor(floats[0], floats[1], floats[2], floats[3])
        this.drawGuiTexture(id, x, y, width, height)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    /**
     * Extension function to replicate drawGuiTexture from 1.20.2-1.21.1. Uses the [RenderLayer.getGuiTextured] method to fill in the function param
     * @param id Identifier - The sprite identifier for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width - the width of the texture
     * @param height - the height of the texture
     * @param alpha - the texture transparency
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun DrawContext.drawTex(id: Identifier, x: Int, y: Int, width: Int, height: Int, alpha: Float) {
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha)
        this.drawGuiTexture(id, x, y, width, height)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    /**
     * Extension function to draw a texture, replacing drawtexture. Uses the [RenderLayer.getGuiTextured] method to fill in the function param
     * @param id Identifier - The sprite identifier for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param u Float - the horizontal position of the texture region
     * @param v Float - the vertical position of the texture region
     * @param width - the width of the drawn region in pixels
     * @param height - the height of the drawn region in pixels
     * @param texWidth - the width of the texture in pixels
     * @param texHeight - the height of the texture in pixels
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun DrawContext.drawTex(id: Identifier, x: Int, y: Int, u: Float, v: Float, width: Int, height: Int, texWidth: Int, texHeight: Int) {
        RenderSystem.enableBlend()
        this.drawTexture(id, x, y, u, v, width, height, texWidth, texHeight)
        RenderSystem.disableBlend()
    }

    /**
     * Applies the blur shader to the current drawn elements. This is used to blur stuff behind guis, but can be used for whatever else.
     *
     * You'll still have to write to the buffers after using this.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun renderBlur() {
        MinecraftClient.getInstance().gameRenderer.renderBlur(0f)
    }
}