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
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowListener
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowProvider
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
 * @param width Int - the width of the widget in pixels
 * @param height Int - the height of the widget in pixels
 * @param wrappedValue [Supplier]&lt;String&gt; - supplies strings to the text field for display
 * @param choiceValidator [ChoiceValidator]&lt;String&gt; - additional choice validation, if any. Generally this can be [ChoiceValidator.any]
 * @param validator [EntryValidator]&lt;String&gt; - String validation provider see [EntryValidator.Builder] for more details on validation construction
 * @param applier [Consumer]&lt;String&gt; - accepts newly valid user inputs.
 * @param suggestionProvider [SuggestionProvider] - provides the valid suggestions for the user
 * @param closePopup Boolean - if true, this window will 'pop' the latest PopupWidget, if any.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Environment(EnvType.CLIENT)
class SuggestionBackedTextFieldWidget(
    width: Int,
    height: Int,
    wrappedValue: Supplier<String>,
    choiceValidator: ChoiceValidator<String>,
    validator: EntryValidator<String>,
    applier: Consumer<String>,
    private val suggestionProvider: SuggestionProvider,
    private val closePopup: Boolean)
    :
    ValidationBackedTextFieldWidget(width, height, wrappedValue, choiceValidator, validator, applier),
    SuggestionWindowProvider
{

    constructor(width: Int,
                height: Int,
                wrappedValue: Supplier<String>,
                choiceValidator: ChoiceValidator<String>,
                validator: EntryValidator<String>,
                applier: Consumer<String>,
                suggestionProvider: SuggestionProvider): this(width, height, wrappedValue, choiceValidator, validator, applier, suggestionProvider, true)

    private var pendingSuggestions: CompletableFuture<Suggestions>? = null
    private var lastSuggestionText = ""
    private var shownText = ""
    private var window: SuggestionWindow? = null
    private var closeWindow = false
    private var needsUpdating = false
    private var suggestionWindowListener: SuggestionWindowListener? = null

    override fun addListener(listener: SuggestionWindowListener) {
        this.suggestionWindowListener = listener
    }

    override fun isValidTest(s: String): Boolean {
        if (s != lastSuggestionText) {
            pendingSuggestions = suggestionProvider.getSuggestions(s, this.cursor, choiceValidator)
            lastSuggestionText = s
        }
        return super.isValidTest(s)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val testValue = wrappedValue.get()
        if (cachedWrappedValue != testValue || needsUpdating) {
            needsUpdating = false
            this.storedValue = testValue
            this.cachedWrappedValue = testValue
            this.text = this.storedValue
        }
        if(isChanged()) {
            if (lastChangedTime != 0L && !ongoingChanges())
                applier.accept(storedValue)
        }
        super.renderWidget(context, mouseX, mouseY, delta)
        if(isValid) {
            if (ongoingChanges())
                context.drawGuiTexture(TextureIds.ENTRY_ONGOING, x + width - 20, y, 20, 20)
            else
                context.drawGuiTexture(TextureIds.ENTRY_OK, x + width - 20, y, 20, 20)
        } else {
            context.drawGuiTexture(TextureIds.ENTRY_ERROR, x + width - 20, y, 20, 20)
        }
        if (pendingSuggestions?.isDone == true) {
            val suggestions = pendingSuggestions?.get()
            if (suggestions != null && !suggestions.isEmpty && shownText != lastSuggestionText) {
                shownText = lastSuggestionText
                addSuggestionWindow(suggestions)
            }
        }
        window?.render(context, mouseX, mouseY, delta)
    }

    private fun addSuggestionWindow(suggestions: Suggestions) {
        val applier: Consumer<String> = Consumer { s ->
            try {
                applier.accept(s)
                needsUpdating = true
            } catch (e: Exception) {
                //
            }
        }
        val closer: Consumer<SuggestionWindow> = Consumer { closeWindow = true }
        this.window = SuggestionWindow.createSuggestionWindow(this.x, this.y, suggestions, this.text, this.cursor, applier, closer)
        suggestionWindowListener?.setSuggestionWindowElement(this)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val bl = window?.mouseClicked(mouseX.toInt(), mouseY.toInt(), button) ?: super.mouseClicked(mouseX, mouseY, button)
        if (closeWindow) {
            pendingSuggestions = null
            window = null
            suggestionWindowListener?.setSuggestionWindowElement(null)
            closeWindow = false
        }
        return if(bl) true else super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return window?.mouseScrolled(mouseX.toInt(), mouseY.toInt(), verticalAmount) ?: super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return super.isMouseOver(mouseX, mouseY) || window?.isMouseOver(mouseX.toInt(), mouseY.toInt()) == true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val bl = window?.keyPressed(keyCode, scanCode, modifiers) ?: super.keyPressed(keyCode, scanCode, modifiers)
        if (closeWindow) {
            pendingSuggestions = null
            window = null
            suggestionWindowListener?.setSuggestionWindowElement(null)
            closeWindow = false
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            pushChanges()
            if (closePopup)
                PopupWidget.pop()
        }
        return if(bl) true else super.keyPressed(keyCode, scanCode, modifiers)
    }

    fun pushChanges() {
        if(isChanged() && !needsUpdating) {
            applier.accept(storedValue)
        }
    }

    fun interface SuggestionProvider {
        fun getSuggestions(s: String, cursor: Int, choiceValidator: ChoiceValidator<String>): CompletableFuture<Suggestions>
    }
}