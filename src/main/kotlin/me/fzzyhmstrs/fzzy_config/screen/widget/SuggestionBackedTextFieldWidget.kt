/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget

import com.mojang.brigadier.suggestion.Suggestions
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindow
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawGuiTexture
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A [ValidationBackedTextFieldWidget] that provides suggestions for string completion to the user
 * @param
 */
@Environment(EnvType.CLIENT)
class SuggestionBackedTextFieldWidget(
    width: Int,
    height: Int,
    wrappedValue: Supplier<String>,
    choiceValidator: ChoiceValidator<String>,
    validator: EntryValidator<String>,
    applier: Consumer<String>,
    private val suggestionProvider: SuggestionProvider)
    :
    ValidationBackedTextFieldWidget(width, height, wrappedValue, choiceValidator, validator, applier)
{
    private var pendingSuggestions: CompletableFuture<Suggestions>? = null
    private var lastSuggestionText = ""
    private var shownText = ""
    private var window: SuggestionWindow? = null
    private var closeWindow = false
    private var needsUpdating = false

    override fun isValidTest(s: String): Boolean {
        if (s != lastSuggestionText) {
            pendingSuggestions = suggestionProvider.getSuggestions(s, this.cursor, choiceValidator)
            lastSuggestionText = s
        }
        return super.isValidTest(s)
    }

    override fun renderButton(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val testValue = wrappedValue.get()
        if (cachedWrappedValue != testValue || needsUpdating) {
            needsUpdating = false
            this.storedValue = testValue
            this.cachedWrappedValue = testValue
            this.text = this.storedValue
        }
        if(isChanged()){
            if (lastChangedTime != 0L && !ongoingChanges())
                applier.accept(storedValue)
        }
        super.renderButton(context, mouseX, mouseY, delta)
        if(isValid){
            if (ongoingChanges())
                context.drawGuiTexture(TextureIds.ENTRY_ONGOING,x + width - 20, y, 20, 20)
            else
                context.drawGuiTexture(TextureIds.ENTRY_OK,x + width - 20, y, 20, 20)
        } else {
            context.drawGuiTexture(TextureIds.ENTRY_ERROR,x + width - 20, y, 20, 20)
        }
        if (pendingSuggestions?.isDone == true){
            val suggestions = pendingSuggestions?.get()
            if (suggestions != null && !suggestions.isEmpty && shownText != lastSuggestionText){
                shownText = lastSuggestionText
                addSuggestionWindow(suggestions)
            }
        }
        window?.render(context, mouseX, mouseY, delta)
    }

    private fun addSuggestionWindow(suggestions: Suggestions){
        val applier: Consumer<String> = Consumer { s ->
            try {
                applier.accept(s)
            } catch (e: Exception) {
                //
            }
        }
        val closer: Consumer<SuggestionWindow> = Consumer { closeWindow = true }
        this.window = SuggestionWindow.createSuggestionWindow(this.x,this.y,suggestions,this.text,this.cursor,applier,closer)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val bl = window?.mouseClicked(mouseX.toInt(), mouseY.toInt(), button) ?: super.mouseClicked(mouseX, mouseY, button)
        if (closeWindow) {
            pendingSuggestions = null
            window = null
            closeWindow = false
        }
        return if(bl) true else super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, verticalAmount: Double): Boolean {
        return window?.mouseScrolled(mouseX.toInt(),mouseY.toInt(),verticalAmount) ?: super.mouseScrolled(mouseX, mouseY, verticalAmount)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return super.isMouseOver(mouseX, mouseY) || window?.isMouseOver(mouseX.toInt(),mouseY.toInt()) == true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val bl = window?.keyPressed(keyCode, scanCode, modifiers) ?: super.keyPressed(keyCode, scanCode, modifiers)
        if (closeWindow) {
            pendingSuggestions = null
            window = null
            closeWindow = false
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER){
            pushChanges()
            PopupWidget.pop()
        }
        return if(bl) true else super.keyPressed(keyCode, scanCode, modifiers)
    }

    fun pushChanges(){
        if(isChanged()){
            applier.accept(storedValue)
        }
    }

    fun interface SuggestionProvider{
        fun getSuggestions(s: String, cursor: Int, choiceValidator: ChoiceValidator<String>): CompletableFuture<Suggestions>
    }
}