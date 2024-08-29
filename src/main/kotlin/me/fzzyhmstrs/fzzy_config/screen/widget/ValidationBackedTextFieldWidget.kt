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

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.MutableText
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A [TextFieldWidget] backed by string validation that can consume and apply strings from/to an outside source
 * @param width Int - width of the widget
 * @param height Int - height of the widget
 * @param wrappedValue [Supplier]&lt;String&gt; - supplies strings to the text field for display
 * @param choiceValidator [ChoiceValidator]&lt;String&gt; - additional choice validation, if any. Generally this can be [ChoiceValidator.any]
 * @param validator [EntryValidator]&lt;String&gt; - String validation provider see [EntryValidator.Builder] for more details on validation construction
 * @param applier [Consumer]&lt;String&gt; - accepts newly valid user inputs.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Suppress("LeakingThis")
@Environment(EnvType.CLIENT)
open class ValidationBackedTextFieldWidget(width: Int, height: Int, protected val wrappedValue: Supplier<String>, protected val choiceValidator: ChoiceValidator<String>, private val validator: EntryValidator<String>, protected val applier: Consumer<String>):
    TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, width, height, FcText.empty())
{

    protected var cachedWrappedValue: String = wrappedValue.get()
    protected var storedValue = wrappedValue.get()
    protected var lastChangedTime: Long = 0L
    protected var isValid = true

    fun getValue(): String {
        return storedValue
    }

    protected fun ongoingChanges(): Boolean {
        return System.currentTimeMillis() - lastChangedTime <= 350L
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val testValue = wrappedValue.get()
        if (cachedWrappedValue != testValue) {
            this.storedValue = testValue
            this.cachedWrappedValue = testValue
            this.text = this.storedValue
        }
        if(isChanged()) {
            if (lastChangedTime != 0L && !ongoingChanges()) {
                applier.accept(storedValue)
                cachedWrappedValue = storedValue
            }
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

    }

    protected open fun isValidTest(s: String): Boolean {
        val result = validator.validateEntry(s, EntryValidator.ValidationType.STRONG)
        return if(result.isError()) {
            this.tooltip = Tooltip.of(result.getError().lit())
            setEditableColor(0xFF5555)
            false
        } else {
            this.tooltip = null
            val result2 = choiceValidator.validateEntry(result.get(), EntryValidator.ValidationType.STRONG)
            if (result2.isError()) {
                this.tooltip = Tooltip.of(result.getError().lit())
                setEditableColor(0xFF5555)
                false
            } else {
                this.storedValue = result.get()
                lastChangedTime = System.currentTimeMillis()
                setEditableColor(0xFFFFFF)
                true
            }
        }
    }

    override fun getInnerWidth(): Int {
        return super.getInnerWidth() - 11
    }

    protected fun isChanged(): Boolean {
        return storedValue != wrappedValue.get()
    }

    init {
        setMaxLength(1000)
        text = wrappedValue.get()
        setChangedListener { s -> isValid = isValidTest(s) }
    }

    override fun getNarrationMessage(): MutableText {
        return text.lit()
    }
}