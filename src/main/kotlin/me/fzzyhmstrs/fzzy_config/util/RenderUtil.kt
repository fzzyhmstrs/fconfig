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

import it.unimi.dsi.fastutil.ints.IntIterator
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier
import net.minecraft.util.math.Divider
import net.minecraft.util.math.MathHelper
import kotlin.math.min

object RenderUtil {

    fun DrawContext.drawGuiTexture(id: Identifier, x: Int, y: Int, width: Int, height: Int){
        this.drawTexture(Identifier(id.namespace,"textures/gui/sprites/${id.path}.png"),x,y,width, height,0f,0f,width,height,width,height)
    }

    fun DrawContext.drawNineSlice(id: Identifier, x: Int, y: Int, width: Int, height: Int, outerWidth: Int, outerHeight: Int, textureWidth: Int,textureHeight: Int) {
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
        val texture = Identifier(id.namespace,"textures/gui/sprites/${id.path}.png")
        if (width == textureWidth && height == textureHeight) {
            this.drawTexture(texture, x, y, u, v, width, height,textureWidth,textureHeight)
            return
        }
        if (height == textureHeight) {
            this.drawTexture(texture, x, y, u, v, leftSliceWidth, height,textureWidth,textureHeight)
            drawThreeSliceHorizontal(this, texture, x + leftSliceWidth, y, width - rightSliceWidth - leftSliceWidth, height, u + leftSliceWidth, v, textureWidth, textureHeight, centerWidth)
            this.drawTexture(texture, x + width - rightSliceWidth, y, u + textureWidth - rightSliceWidth, v, rightSliceWidth, height, textureWidth, textureHeight)
            return
        }
        if (width == textureWidth) {
            this.drawTexture(texture, x, y, u, v, width, topSliceHeight,textureWidth,textureHeight)
            drawRepeatingTexture(this, texture, x, y + topSliceHeight, width, height - bottomSliceHeight - topSliceHeight, u, v + topSliceHeight, textureWidth, textureHeight, centerWidth, centerHeight)
            this.drawTexture(texture, x, y + height - bottomSliceHeight, u, v + textureHeight - bottomSliceHeight, width, bottomSliceHeight,textureWidth,textureHeight)
            return
        }
        this.drawTexture(texture, x, y, u, v, leftSliceWidth, topSliceHeight,textureWidth,textureHeight)
        drawRepeatingTexture(this, texture, x + leftSliceWidth, y, width - rightSliceWidth - leftSliceWidth, topSliceHeight, u + leftSliceWidth, v, textureWidth, textureHeight, centerWidth, centerHeight)
        this.drawTexture(texture, x + width - rightSliceWidth, y, u + textureWidth - rightSliceWidth, v, rightSliceWidth, topSliceHeight,textureWidth,textureHeight)
        this.drawTexture(texture, x, y + height - bottomSliceHeight, u, v + textureHeight - bottomSliceHeight, leftSliceWidth, bottomSliceHeight,textureWidth,textureHeight)
        drawRepeatingTexture(this, texture, x + leftSliceWidth, y + height - bottomSliceHeight, width - rightSliceWidth - leftSliceWidth, bottomSliceHeight, u + leftSliceWidth, v + textureHeight - bottomSliceHeight, textureWidth, textureHeight, centerWidth, centerHeight)
        this.drawTexture(texture, x + width - rightSliceWidth, y + height - bottomSliceHeight, u + textureWidth - rightSliceWidth, v + textureHeight - bottomSliceHeight, rightSliceWidth, bottomSliceHeight,textureWidth,textureHeight)
        drawRepeatingTexture(this, texture, x, y + topSliceHeight, leftSliceWidth, height - bottomSliceHeight - topSliceHeight, u, v + topSliceHeight, textureWidth,textureHeight, centerWidth, centerHeight)
        drawRepeatingTexture(this, texture, x + leftSliceWidth, y + topSliceHeight, width - rightSliceWidth - leftSliceWidth, height - bottomSliceHeight - topSliceHeight, u + leftSliceWidth, v + topSliceHeight, textureWidth,textureHeight, centerWidth, centerHeight)
        drawRepeatingTexture(this, texture, x + width - rightSliceWidth, y + topSliceHeight, leftSliceWidth, height - bottomSliceHeight - topSliceHeight, u + textureWidth - rightSliceWidth, v + topSliceHeight, textureWidth,textureHeight, centerWidth, centerHeight)
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
            val drawWidth = min(widthToGo,centerWidth)
            var l = y
            var heightToGo = height
            while (heightToGo > 0) {
                val drawHeight = min(heightToGo,centerHeight)
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
            val drawWidth = min(widthToGo,centerWidth)
            context.drawTexture(texture, i, y, u, v, drawWidth, height, textureWidth, textureHeight)
            i += centerWidth
            widthToGo -= centerWidth
        }
    }

}