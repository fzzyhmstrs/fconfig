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

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

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
        this.drawGuiTexture(RenderPipelines.GUI_TEXTURED, id, x, y, width, height)
    }

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
    fun DrawContext.drawTexOverlay(id: Identifier, x: Int, y: Int, width: Int, height: Int) {
        this.drawGuiTexture(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, id, x, y, width, height)
    }

    /**
     * Extension function to replicate drawGuiTexture with overlay render layer from 1.21.5. Uses the [RenderLayer.getGuiTextured] method to fill in the function param
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
        this.drawGuiTexture(RenderPipelines.GUI_TEXTURED, id, x, y, width, height, color)
    }

    /**
     * Extension function to replicate drawGuiTexture with overlay render layer from 1.21.5. Uses the [RenderLayer.getGuiTextured] method to fill in the function param
     * @param id Identifier - The sprite identifier for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width - the width of the texture
     * @param height - the height of the texture
     * @param color - the color of the texture
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    fun DrawContext.drawTexOverlay(id: Identifier, x: Int, y: Int, width: Int, height: Int, color: Int) {
        this.drawGuiTexture(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, id, x, y, width, height, color)
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
        this.drawGuiTexture(RenderPipelines.GUI_TEXTURED, id, x, y, width, height, PortingUtils.getWhite(alpha))
    }

    /**
     * Extension function to replicate drawGuiTexture with overlay render layer from 1.21.5. Uses the [RenderLayer.getGuiTextured] method to fill in the function param
     * @param id Identifier - The sprite identifier for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width - the width of the texture
     * @param height - the height of the texture
     * @param alpha - the texture transparency
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    fun DrawContext.drawTexOverlay(id: Identifier, x: Int, y: Int, width: Int, height: Int, alpha: Float) {
        this.drawGuiTexture(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, id, x, y, width, height, PortingUtils.getWhite(alpha))
    }

    /**
     * Extension function to replicate the nine-slice functionality drawGuiTexture from 1.20.2+.
     *
     * __in 1.20.2+ this is a compat method; maintained as-is to avoid needing to alter mod code elsewhere.__
     * @param id Identifier - The sprite identifier (1.20.2+ style) for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width Int - the width of the drawn texture
     * @param height Int - the height of the drawn texture
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun DrawContext.drawNineSlice(id: Identifier, x: Int, y: Int, width: Int, height: Int) {
        this.drawTex(id, x, y, width, height)
    }

    /**
     * Extension function to replicate the nine-slice functionality drawGuiTexture from 1.20.2+.
     *
     * __in 1.20.2+ this is a compat method; maintained as-is to avoid needing to alter mod code elsewhere.__
     * @param id Identifier - The sprite identifier (1.20.2+ style) for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width Int - the width of the drawn texture
     * @param height Int - the height of the drawn texture
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun DrawContext.drawNineSliceOverlay(id: Identifier, x: Int, y: Int, width: Int, height: Int) {
        this.drawTexOverlay(id, x, y, width, height)
    }

    /**
     * Extension function to replicate the nine-slice functionality drawGuiTexture from 1.20.2+.
     *
     * __in 1.20.2+ this is a compat method; maintained as-is to avoid needing to alter mod code elsewhere.__
     * @param id Identifier - The sprite identifier (1.20.2+ style) for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width Int - the width of the drawn texture
     * @param height Int - the height of the drawn texture
     * @param color Int - the ARGB packed color int
     * @author fzzyhmstrs
     * @since 0.6.1
     */
    fun DrawContext.drawNineSlice(id: Identifier, x: Int, y: Int, width: Int, height: Int, color: Int) {
        this.drawTex(id, x, y, width, height, color)
    }

    /**
     * Extension function to replicate the nine-slice functionality drawGuiTexture from 1.20.2+.
     *
     * __in 1.20.2+ this is a compat method; maintained as-is to avoid needing to alter mod code elsewhere.__
     * @param id Identifier - The sprite identifier (1.20.2+ style) for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width Int - the width of the drawn texture
     * @param height Int - the height of the drawn texture
     * @param alpha Float - the texture transparency
     * @author fzzyhmstrs
     * @since 0.6.1
     */
    fun DrawContext.drawNineSlice(id: Identifier, x: Int, y: Int, width: Int, height: Int, alpha: Float) {
        this.drawTex(id, x, y, width, height, alpha)
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
        this.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, width, height, texWidth, texHeight)
    }

    /**
     * Applies the blur shader to the current drawn elements. This is used to blur stuff behind guis, but can be used for whatever else.
     *
     * You'll still have to write to the buffers after using this.
     * @param context [DrawContext]
     * @param x horizontal position of element, or 0 for blank screen/blur rendered at the start of whole-screen rendering
     * @param y vertical position of element, or 0 for blank screen/blur rendered at the start of whole-screen rendering
     * @param delta the tickDelta (unused in 1.21.x)
     * @author fzzyhmstrs
     * @since 0.6.1
     */
    fun renderBlur(context: DrawContext, x: Float, y: Float, delta: Float) {
        context.matrices.pushMatrix()
        context.matrices.translate(x, y) //, 0f)
//        context.draw()
        MinecraftClient.getInstance().gameRenderer.renderBlur()
        context.matrices.popMatrix()
    }
}