package me.fzzyhmstrs.fzzy_config.screen

import com.mojang.brigadier.suggestion.Suggestion
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

@Environment(EnvType.CLIENT)
class SuggestionWindow(
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
        context.fill(x,y-1,x+w,y+h+1,-805306368)
        if (index > 0){
            if (up){
                for (k in 0..w step 2) {
                    context.fill(x + k, y - 1, x + k + 1, y, -1)
                }
            } else {
                for (k in 0..w step 2){
                    context.fill(x + k, y + h, x + k + 1, y + h + 1, -1)
                }
            }
        } else if (suggestions.size > suggestionSize + index){
            if (up){
                for (k in 0..w step 2){
                    context.fill(x + k, y + h, x + k + 1, y + h + 1, -1)
                }
            } else {
                for (k in 0..w step 2) {
                    context.fill(x + k, y - 1, x + k + 1, y, -1)
                }
            }
        }
        var textY = if(up) y + h - 10 else y + 2
        for (l in index until index + suggestionSize){
            if (mouseX > x && mouseX < x + w && mouseY > textY - 2 && mouseY < textY + 10)
                select(l)
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, suggestions[l].text,x + 1,textY, if(selection == l) Colors.YELLOW else -5592406)
            textY += if(up) -12 else 12
        }
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
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
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
}