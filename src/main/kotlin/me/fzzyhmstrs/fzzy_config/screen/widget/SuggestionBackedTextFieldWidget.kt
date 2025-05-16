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
import me.fzzyhmstrs.fzzy_config.screen.SuggestionWindowListener
import me.fzzyhmstrs.fzzy_config.screen.SuggestionWindowProvider
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindow
import me.fzzyhmstrs.fzzy_config.screen.widget.SuggestionBackedTextFieldWidget.SuggestionProvider
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
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
//client
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

    override fun renderButton(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
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
        super.renderButton(context, mouseX, mouseY, delta)
        if(isValid) {
            if (ongoingChanges())
                context.drawTex(TextureIds.ENTRY_ONGOING, x + width - 20, y, 20, 20)
            else
                context.drawTex(TextureIds.ENTRY_OK, x + width - 20, y, 20, 20)
        } else {
            context.drawTex(TextureIds.ENTRY_ERROR, x + width - 20, y, 20, 20)
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
            } catch (e: Throwable) {
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

    override fun mouseScrolled(mouseX: Double, mouseY: Double, verticalAmount: Double): Boolean {
        return window?.mouseScrolled(mouseX.toInt(), mouseY.toInt(), verticalAmount) ?: super.mouseScrolled(mouseX, mouseY, verticalAmount)
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
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || (keyCode == GLFW.GLFW_KEY_TAB && bl)) {
            pushChanges()
            if (closePopup)
                PopupWidget.pop()
        }
        return if(bl) true else super.keyPressed(keyCode, scanCode, modifiers)
    }

    /**
     * Pushes changes stored in this widget into the linked [applier]. This is typically used to force immediate visual updates in the GUI.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun pushChanges() {
        if(isChanged() && !needsUpdating) {
            applier.accept(storedValue)
        }
    }

    /**
     * Interface for providing suggestions into the widget based on current widget and validator state.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun interface SuggestionProvider {
        /**
         * Provide suggestions for the widget based on current widget state. The typical approach involves using either [AllowableStrings][me.fzzyhmstrs.fzzy_config.util.AllowableStrings] or [AllowableIdentifiers][me.fzzyhmstrs.fzzy_config.util.AllowableIdentifiers], both of which have a convenient built-in method for generating suggestions.
         *
         * For example:
         * ```
         * //this assumes the ChoiceValidator is a ChoiceValidator<String>
         * val suggestionProvider = SuggestionProvider {s, c, cv -> allowableIdentifiers.getSuggestions(s, c, cv))}
         * ```
         *
         * But of course you can implement a totally custom way to get suggestions as needed.
         * @param s current string input for this widget
         * @param cursor integer position of the cursor relative to the text (0 being the start, and whatever the end index is being the cursor "active" at the end of the string for more typing)
         * @param choiceValidator [ChoiceValidator]&lt;String&gt; a validator to filter allowable inputs from a set of string sources.
         * @return [CompletableFuture]&lt;[Suggestions]&lt;
         * @see [me.fzzyhmstrs.fzzy_config.util.AllowableStrings]
         * @see [me.fzzyhmstrs.fzzy_config.util.AllowableIdentifiers]
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun getSuggestions(s: String, cursor: Int, choiceValidator: ChoiceValidator<String>): CompletableFuture<Suggestions>
    }
}