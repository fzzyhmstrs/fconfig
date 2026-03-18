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
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.ARGB
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
//client
open class ValidationBackedTextFieldWidget(width: Int, height: Int, protected val wrappedValue: Supplier<String>, protected val choiceValidator: ChoiceValidator<String>, private val validator: EntryValidator<String>, protected val applier: Consumer<String>):
    EditBox(Minecraft.getInstance().font, 0, 0, width, height, FcText.EMPTY)
{

    protected var cachedWrappedValue: String = wrappedValue.get()
    protected var storedValue = wrappedValue.get()
    protected var lastChangedTime: Long = 0L
    protected var isValid = true

    fun storeValue(): String {
        return storedValue
    }

    protected fun ongoingChanges(): Boolean {
        return System.currentTimeMillis() - lastChangedTime <= 350L
    }

    override fun extractWidgetRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val testValue = wrappedValue.get()
        if (cachedWrappedValue != testValue) {
            this.storedValue = testValue
            this.cachedWrappedValue = testValue
            this.setValue(this.storedValue)
        }
        if(isChanged()) {
            if (lastChangedTime != 0L && !ongoingChanges()) {
                applier.accept(storedValue)
                cachedWrappedValue = storedValue
            }
        }
        super.extractWidgetRenderState(context, mouseX, mouseY, delta)
        val id = if(isValid) {
            if (ongoingChanges())
                TextureIds.ENTRY_ONGOING
            else
                TextureIds.ENTRY_OK
        } else {
            TextureIds.ENTRY_ERROR
        }
        context.drawTex(id, x + width - 20, y, 20, 20)
    }

    protected open fun isValidTest(s: String): Boolean {
        val result = validator.validateEntry(s, EntryValidator.ValidationType.STRONG)
        return if(result.isError()) {
            this.setTooltip(Tooltip.create(FcText.toLinebreakText(mutableListOf<String>().apply { result.logPlain{ s, _ -> this.add(s) } }.map { it.lit() })))
            setTextColor(-43691)
            false
        } else {
            this.setTooltip(null)
            val result2 = choiceValidator.validateEntry(result.get(), EntryValidator.ValidationType.STRONG)
            if (result2.isError()) {
                this.setTooltip(Tooltip.create(FcText.toLinebreakText(mutableListOf<String>().apply { result2.logPlain{ s, _ -> this.add(s) } }.map { it.lit() })))
                setTextColor(-43691)
                false
            } else {
                this.storedValue = result.get()
                lastChangedTime = System.currentTimeMillis()
                setTextColor(-1)
                true
            }
        }
    }

    /**
     * @suppress
     */
    override fun getInnerWidth(): Int {
        return super.getInnerWidth() - 11
    }

    protected fun isChanged(): Boolean {
        return storedValue != wrappedValue.get()
    }

    /**
     * @suppress
     */
    final override fun setResponder(changedListener: Consumer<String>) {
        super.setResponder(changedListener)
    }

    /**
     * @suppress
     */
    final override fun setMaxLength(maxLength: Int) {
        super.setMaxLength(maxLength)
    }

    init {
        setMaxLength(1000)
        setValue(wrappedValue.get())
        setResponder { s -> isValid = isValidTest(s) }
    }

    /**
     * @suppress
     */
    override fun createNarrationMessage(): MutableComponent {
        return "gui.narrate.editBox".translate("", "")
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        builder.add(NarratedElementType.TITLE, this.createNarrationMessage())
        builder.nest().add(NarratedElementType.TITLE, "${this.value}. ")
        //builder.nextMessage().put(NarrationPart.USAGE, "fc.validated_field.number.editBox.usage".translate())
    }

    fun appendValueNarrations(builder: NarrationElementOutput) {
        builder.nest().add(NarratedElementType.TITLE, "fc.validated_field.current".translate(""))
        builder.nest().nest().add(NarratedElementType.TITLE, "${this.value}. ")
    }
}