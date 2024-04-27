/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.internal

import com.google.common.collect.Lists
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.brigadier.suggestion.Suggestion
import com.mojang.brigadier.suggestion.Suggestions
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.math.MathHelper
import org.lwjgl.glfw.GLFW
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

@Environment(EnvType.CLIENT)
internal class SuggestionWindow(
    private val suggestions: List<Suggestion>,
    private val x: Int,
    private val y: Int,
    private val w: Int,
    private val h: Int,
    private val up: Boolean,
    private val applier: Consumer<String>,
    private val closer: Consumer<SuggestionWindow>
){
    private var selection = -1
    private var lastNarrationIndex = -1
    private val suggestionSize = h / 12
    private var index = 0

    fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.matrices.push()
        context.matrices.translate(0f,0f,5f)
        RenderSystem.disableDepthTest()
        context.fill(x,y-1,x+w,y+h+1,-805306368)
        if (index > 0){
            if (!up){
                for (k in 0..w step 2) {
                    RenderSystem.enableBlend()
                    RenderSystem.disableDepthTest()
                    context.fill(x + k, y - 1, x + k + 1, y, -1)
                }
            } else {
                for (k in 0..w step 2){
                    RenderSystem.enableBlend()
                    RenderSystem.disableDepthTest()
                    context.fill(x + k, y + h, x + k + 1, y + h + 1, -1)
                }
            }
        }
        if (suggestions.size > suggestionSize + index){
            if (!up){
                for (k in 0..w step 2){
                    RenderSystem.enableBlend()
                    RenderSystem.disableDepthTest()
                    context.fill(x + k, y + h, x + k + 1, y + h + 1, -1)
                }
            } else {
                for (k in 0..w step 2) {
                    RenderSystem.enableBlend()
                    RenderSystem.disableDepthTest()
                    context.fill(x + k, y - 1, x + k + 1, y, -1)
                }
            }
        }
        var textY = if(up) y + h - 10 else y + 2
        for (l in index until index + suggestionSize){
            RenderSystem.enableBlend()
            RenderSystem.disableDepthTest()
            if (mouseX > x && mouseX < x + w && mouseY > textY - 2 && mouseY < textY + 10)
                select(l)
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, suggestions[l].text,x + 1,textY, if(selection == l) Colors.YELLOW else -5592406)
            textY += if(up) -12 else 12
        }
        context.matrices.pop()
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) return false
        var testY = if(up) y + h - 12 else y
        for (l in index until index + suggestionSize){
            if (mouseX > x && mouseX < x + w && mouseY > testY && mouseY < testY + 12) {
                val chosen = suggestions[l].text
                applier.accept(chosen)
                closer.accept(this)
                return true
            }
            testY += if(up) -12 else 12
        }
        return false
    }

    fun mouseScrolled(mouseX: Int, mouseY: Int, amount: Double): Boolean {
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) return false
        val d = if (up){
            if (amount < 0.0){
                -1
            } else {
                1
            }
        } else {
            if (amount < 0.0){
                1
            } else {
                -1
            }
        }
        index = MathHelper.clamp(index + d, 0, max(suggestions.size - suggestionSize,0))
        return true
    }

    fun isMouseOver(mouseX: Int, mouseY: Int): Boolean{
        return !(mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h)
    }

    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        when (keyCode) {
            GLFW.GLFW_KEY_UP -> {
                val d = if (up)
                    1
                else
                    -1
                if (selection == -1){
                    selection = if (up) -1 else suggestions.lastIndex + 1
                }
                select(MathHelper.clamp(selection + d, 0, suggestions.lastIndex))
                if (selection < index)
                    index = selection
                if (selection > index + suggestionSize - 1)
                    index += 1
                return true
            }
            GLFW.GLFW_KEY_DOWN -> {
                val d = if (up)
                    -1
                else
                    1
                if (selection == -1){
                    selection = if (up) suggestions.lastIndex + 1 else -1
                }
                select(MathHelper.clamp(selection + d, 0, suggestions.lastIndex))
                if (selection < index)
                    index = selection
                if (selection > index + suggestionSize - 1)
                    index += 1
                return true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_TAB -> {
                return if (selection != -1){
                    applier.accept(suggestions[selection].text)
                    closer.accept(this)
                    true
                } else {
                    false
                }
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                closer.accept(this)
                return true
            }
            else -> return false
        }
    }

    private fun select(i: Int) {
        selection = i
        if (lastNarrationIndex != selection) {
            MinecraftClient.getInstance().narratorManager.narrate(getNarration())
        }
    }

    private fun getNarration(): Text? {
        this.lastNarrationIndex = this.selection
        val suggestion = suggestions[this.selection]
        val message = suggestion.tooltip
        return if (message != null) {
            Text.translatable("narration.suggestion.tooltip", this.selection + 1, suggestions.size, suggestion.text, Text.of(message))
        } else Text.translatable("narration.suggestion", this.selection + 1, suggestions.size, suggestion.text)
    }

    companion object{
        fun createSuggestionWindow(windowX: Int, windowY: Int,suggestions: Suggestions,text: String, cursor: Int, applier: Consumer<String>, closer: Consumer<SuggestionWindow>): SuggestionWindow {
            var w = 0
            for (suggestion in suggestions.list) {
                w = max(w, MinecraftClient.getInstance().textRenderer.getWidth(suggestion.text))
            }
            val sWidth = MinecraftClient.getInstance().currentScreen?.width ?: Int.MAX_VALUE
            val sHeight = MinecraftClient.getInstance().currentScreen?.height ?: Int.MAX_VALUE
            val x = max(min(windowX,sWidth - w),0)
            var h = min(suggestions.list.size * 12, 120)
            val up = windowY
            val down = sHeight - (windowY + 20)
            val upBl: Boolean
            val y = if(up >= down) {
                upBl = true
                while (windowY - h < 0){
                    h -= 12
                }
                windowY - h
            } else {
                upBl = false
                while (windowY + 20 + h > sHeight){
                    h -= 12
                }
                windowY + 20
            }
            return SuggestionWindow(
                sortSuggestions(suggestions,text, cursor), x, y, w, h, upBl,
                applier,
                closer)
        }
        fun sortSuggestions(suggestions: Suggestions, text: String, cursor: Int): List<Suggestion> {
            val string: String = text.substring(0, cursor)
            val string2 = string.lowercase()
            val list = Lists.newArrayList<Suggestion>()
            val list2 = Lists.newArrayList<Suggestion>()
            for (suggestion in suggestions.list) {
                if (suggestion.text.startsWith(string2) || suggestion.text.startsWith("minecraft:$string2")) {
                    list.add(suggestion)
                    continue
                }
                list2.add(suggestion)
            }
            list.addAll(list2)
            return list
        }
    }
}