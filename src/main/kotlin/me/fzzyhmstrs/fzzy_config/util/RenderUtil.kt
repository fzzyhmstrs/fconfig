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
import net.minecraft.util.Identifier
import net.minecraft.util.math.ColorHelper
import java.awt.Color
import kotlin.math.min

/**
 * Render utils for DrawContext to provide functionality similar to 1.20.2+ sprite rendering
 * @author fzzyhmstrs
 * @since 0.2.0
 */
object RenderUtil {

    /**
     * Extension function to replicate drawGuiTexture from 1.20.2+. Will brute force render the texture passed as a "standard" texture by adding the necessary identifier path information (such as the .png).
     *
     * This method is surely not very efficient. It is designed to be a porting feature.
     * @param id Identifier - The sprite identifier (1.20.2+ style) for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width - the width of the texture
     * @param height - the height of the texture
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun DrawContext.drawTex(id: Identifier, x: Int, y: Int, width: Int, height: Int) {
        this.drawTexture(Identifier(id.namespace, "textures/gui/sprites/${id.path}.png"), x, y, width, height, 0f, 0f, width, height, width, height)
    }

    /**
     * Extension function to replicate drawGuiTexture from 1.20.2-1.21.1.
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
        this.drawTexture(Identifier(id.namespace, "textures/gui/sprites/${id.path}.png"), x, y, width, height, 0f, 0f, width, height, width, height)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    /**
     * Extension function to replicate drawGuiTexture from 1.20.2-1.21.1.
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
        this.drawTexture(Identifier(id.namespace, "textures/gui/sprites/${id.path}.png"), x, y, width, height, 0f, 0f, width, height, width, height)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    /**
     * Extension function to replicate the nine-slice functionality drawGuiTexture from 1.20.2+. Will brute force render the texture passed as a "standard" texture by adding the necessary identifier path information (such as the .png).
     *
     * This method is surely not very efficient. It is designed to be a porting feature.
     * @param id Identifier - The sprite identifier (1.20.2+ style) for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width Int - the width of the drawn texture
     * @param height Int - the height of the drawn texture
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun DrawContext.drawNineSlice(id: Identifier, x: Int, y: Int, width: Int, height: Int) {
        val bg = getBackground(id)
        this.drawNineSlice(id, x, y, width, height, bg.outerWidth, bg.outerHeight, bg.width, bg.height)
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
        val colors = Color(color).getRGBComponents(null)
        RenderSystem.setShaderColor(colors[0], colors[1], colors[2], colors[3])
        this.drawNineSlice(id, x, y, width, height)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
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
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha)
        val bg = getBackground(id)
        this.drawNineSlice(id, x, y, width, height, bg.outerWidth, bg.outerHeight, bg.width, bg.height)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    /**
     * Extension function to replicate the nine-slice functionality drawGuiTexture from 1.20.2+. Will brute force render the texture passed as a "standard" texture by adding the necessary identifier path information (such as the .png).
     *
     * This method is surely not very efficient. It is designed to be a porting feature.
     * @param id Identifier - The sprite identifier (1.20.2+ style) for the image.
     * @param x Int - the x location of the texture
     * @param y Int - the y location of the texture
     * @param width Int - the width of the drawn texture
     * @param height Int - the height of the drawn texture
     * @param outerWidth Int - the width in pixels of the left and right nine-slice borders
     * @param outerHeight Int - the height in pixels of the top and bottom nine-slice borders
     * @param textureWidth Int - the width of the png texture to be tiled
     * @param textureHeight Int - the height of the png texture to be tiled
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun DrawContext.drawNineSlice(id: Identifier, x: Int, y: Int, width: Int, height: Int, outerWidth: Int, outerHeight: Int, textureWidth: Int, textureHeight: Int) {
        val texture = Identifier(id.namespace, "textures/gui/sprites/${id.path}.png")
        if (outerHeight == 0 && outerWidth == 0) drawTex(texture, x, y, 0f, 0f, width, height, textureWidth, textureHeight)
        var leftSliceWidth = outerWidth
        var topSliceHeight = outerHeight
        var rightSliceWidth = outerWidth
        var bottomSliceHeight = outerHeight
        leftSliceWidth = leftSliceWidth.coerceAtMost(width / 2)
        rightSliceWidth = rightSliceWidth.coerceAtMost(width / 2)
        topSliceHeight = topSliceHeight.coerceAtMost(height / 2)
        bottomSliceHeight = bottomSliceHeight.coerceAtMost(height / 2)
        val centerWidth = textureWidth - leftSliceWidth - rightSliceWidth
        val centerHeight = textureHeight - topSliceHeight - bottomSliceHeight
        val u = 0f
        val v = 0f

        if (width == textureWidth && height == textureHeight) {
            this.drawTexture(texture, x, y, u, v, width, height, textureWidth, textureHeight)
            return
        }
        if (height == textureHeight) {
            this.drawTexture(texture, x, y, u, v, leftSliceWidth, height, textureWidth, textureHeight)
            drawThreeSliceHorizontal(this, texture, x + leftSliceWidth, y, width - rightSliceWidth - leftSliceWidth, height, u + leftSliceWidth, v, textureWidth, textureHeight, centerWidth)
            this.drawTexture(texture, x + width - rightSliceWidth, y, u + textureWidth - rightSliceWidth, v, rightSliceWidth, height, textureWidth, textureHeight)
            return
        }
        if (width == textureWidth) {
            this.drawTexture(texture, x, y, u, v, width, topSliceHeight, textureWidth, textureHeight)
            drawRepeatingTexture(this, texture, x, y + topSliceHeight, width, height - bottomSliceHeight - topSliceHeight, u, v + topSliceHeight, textureWidth, textureHeight, textureWidth, centerHeight)
            this.drawTexture(texture, x, y + height - bottomSliceHeight, u, v + textureHeight - bottomSliceHeight, width, bottomSliceHeight, textureWidth, textureHeight)
            return
        }
        this.drawTexture(texture, x, y, u, v, leftSliceWidth, topSliceHeight, textureWidth, textureHeight)
        drawRepeatingTexture(this, texture, x + leftSliceWidth, y, width - rightSliceWidth - leftSliceWidth, topSliceHeight, u + leftSliceWidth, v, textureWidth, textureHeight, centerWidth, centerHeight)
        this.drawTexture(texture, x + width - rightSliceWidth, y, u + textureWidth - rightSliceWidth, v, rightSliceWidth, topSliceHeight, textureWidth, textureHeight)
        this.drawTexture(texture, x, y + height - bottomSliceHeight, u, v + textureHeight - bottomSliceHeight, leftSliceWidth, bottomSliceHeight, textureWidth, textureHeight)
        drawRepeatingTexture(this, texture, x + leftSliceWidth, y + height - bottomSliceHeight, width - rightSliceWidth - leftSliceWidth, bottomSliceHeight, u + leftSliceWidth, v + textureHeight - bottomSliceHeight, textureWidth, textureHeight, centerWidth, centerHeight)
        this.drawTexture(texture, x + width - rightSliceWidth, y + height - bottomSliceHeight, u + textureWidth - rightSliceWidth, v + textureHeight - bottomSliceHeight, rightSliceWidth, bottomSliceHeight, textureWidth, textureHeight)
        drawRepeatingTexture(this, texture, x, y + topSliceHeight, leftSliceWidth, height - bottomSliceHeight - topSliceHeight, u, v + topSliceHeight, textureWidth, textureHeight, centerWidth, centerHeight)
        drawRepeatingTexture(this, texture, x + leftSliceWidth, y + topSliceHeight, width - rightSliceWidth - leftSliceWidth, height - bottomSliceHeight - topSliceHeight, u + leftSliceWidth, v + topSliceHeight, textureWidth, textureHeight, centerWidth, centerHeight)
        drawRepeatingTexture(this, texture, x + width - rightSliceWidth, y + topSliceHeight, leftSliceWidth, height - bottomSliceHeight - topSliceHeight, u + textureWidth - rightSliceWidth, v + topSliceHeight, textureWidth, textureHeight, centerWidth, centerHeight)
    }


    private fun drawRepeatingTexture(
        context: DrawContext,
        texture: Identifier,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        u: Float,
        v: Float,
        textureWidth: Int,
        textureHeight: Int,
        centerWidth: Int,
        centerHeight: Int
    ) {
        var i = x
        var widthToGo = width
        while (widthToGo > 0) {
            val drawWidth = min(widthToGo, centerWidth)
            var l = y
            var heightToGo = height
            while (heightToGo > 0) {
                val drawHeight = min(heightToGo, centerHeight)
                context.drawTexture(texture, i, l, u, v, drawWidth, drawHeight, textureWidth, textureHeight)
                l += centerHeight
                heightToGo -= centerHeight
            }
            i += centerWidth
            widthToGo -= centerWidth
        }
    }

    private fun drawThreeSliceHorizontal(
        context: DrawContext,
        texture: Identifier,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        u: Float,
        v: Float,
        textureWidth: Int,
        textureHeight: Int,
        centerWidth: Int
    ) {
        var i = x
        var widthToGo = width
        while (widthToGo > 0) {
            val drawWidth = min(widthToGo, centerWidth)
            context.drawTexture(texture, i, y, u, v, drawWidth, height, textureWidth, textureHeight)
            i += centerWidth
            widthToGo -= centerWidth
        }
    }

    /**
     * Extension function to draw a texture, replacing drawtexture.
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
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Planned for removal 0.7.0, use applyBlur(DrawContextFFF) instead")
    fun renderBlur() {
    }


    private val backgrounds: MutableMap<Identifier, Background> = mutableMapOf()
    internal val defaultBg = Background(20, 4, 200, 20)
    internal val fallbackBg = Background(1, 1, 20, 20)

    fun getBackground(id: Identifier): Background {
        return backgrounds[id] ?: fallbackBg
    }

    fun addBackground(id: Identifier, background: Background) {
        backgrounds[id] = background
    }

    class Background(val outerWidth: Int, val outerHeight: Int,  val width: Int, val height: Int)

    private val fillColor = Color(30, 30, 30, 90).rgb

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
        context.fill(0, 0, MinecraftClient.getInstance().currentScreen?.width ?: 0, MinecraftClient.getInstance().currentScreen?.height ?: 0, fillColor)
    }
}