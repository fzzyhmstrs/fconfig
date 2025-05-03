/*
* Copyright (c) 2024-5 Fzzyhmstrs
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
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.lwjgl.glfw.GLFW
import java.util.Locale
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

/**
 * A [TextFieldWidget] for entering number values, backed by validation of some sort.
 * @param T - extends [Number]
 * @param width Int - width of the text field
 * @param height Int - height of the text field
 * @param wrappedValue [Supplier]&lt;T&gt; - supplier of the value to display on the text field. This is separate from any values stored within the widget, and generally is the same source as the [applier], so this widget is updated if any external changes are made to the number
 * @param choiceValidator [ChoiceValidator]&lt;T&gt; - additional choice validation, if any. Generally this can be [ChoiceValidator.any]
 * @param validationProvider [Function]&lt;Double, [ValidationResult]&lt;T&gt;&gt; - validates the number entered. all numbers entered are treated as doubles internally until application.
 * @param applier [Consumer]&lt;T&gt; - accepts results from valid user entries
 * @author fzzyhmstrs
 * @since 0.2.0
 */
//client
open class ValidationBackedNumberFieldWidget<T: Number>(
    width: Int,
    height: Int,
    private val wrappedValue: Supplier<T>,
    private val choiceValidator: ChoiceValidator<T>,
    private val validationProvider: Function<Double, ValidationResult<T>>,
    private val applier: Consumer<T> = Consumer { _ ->},
    private val renderStatus: Boolean = true,
    private val increment: Double = 0.0)
    :
    TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, width, height, FcText.EMPTY), TooltipChild
{

    private var cachedWrappedValue: T = wrappedValue.get()
    private var storedValue: T = wrappedValue.get()
    private var lastChangedTime: Long = 0L
    private var isValid = true
    private var confirmActive = false
    private var error: List<Text> = emptyList()

    fun isIntType(): Boolean {
        return storedValue is Int || storedValue is Long || storedValue is Short || storedValue is Byte
    }

    private fun isValidChar(chr: Char): Boolean {
        return if(isIntType()) {
            chr in '0'..'9' || chr == '-'
        } else {
            chr in '0'..'9' || chr == '.' || chr == '-'
        }
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (!isValidChar(chr)) return false
        return super.charTyped(chr, modifiers)
    }

    private fun ongoingChanges(): Boolean {
        return System.currentTimeMillis() - lastChangedTime <= 350L
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val testValue = wrappedValue.get()
        if (cachedWrappedValue != testValue) {
            this.storedValue = testValue
            this.cachedWrappedValue = testValue
            this.text = this.storedValue.toString()
        }
        if(isChanged()) {
            if (lastChangedTime != 0L && !ongoingChanges()) {
                cachedWrappedValue = storedValue
                applier.accept(storedValue)
            }
        }
        super.renderWidget(context, mouseX, mouseY, delta)
        if (renderStatus) {
            val id = if (isValid) {
                if (ongoingChanges())
                    TextureIds.ENTRY_ONGOING
                else
                    TextureIds.ENTRY_OK
            } else {
                TextureIds.ENTRY_ERROR
            }
            context.drawTex(id, x + width - 20, y, 20, 20)
        }
    }

    /**
     * @suppress
     */
    override fun getInnerWidth(): Int {
        return super.getInnerWidth() - 11
    }

    private fun isValidTest(s: String): Boolean {
        val test = s.toDoubleOrNull()
        if (test == null) {
            this.error = listOf("fc.validated_field.number.textbox.invalid".translate())
            setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
            return false
        }
        val result = validationProvider.apply(test)
        return if(result.isError()) {
            this.error = mutableListOf<String>().apply { result.logPlain{ s, _ -> this.add(s) } }.map { it.lit() }
            setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
            false
        } else {
            this.error = emptyList()
            val result2 = choiceValidator.validateEntry(result.get(), EntryValidator.ValidationType.STRONG)
            if (result2.isError()) {
                this.error = mutableListOf<String>().apply { result2.logPlain{ s, _ -> this.add(s) } }.map { it.lit() }
                setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
                false
            } else {
                this.storedValue = result.get()
                lastChangedTime = System.currentTimeMillis()
                confirmActive = isChanged()
                setEditableColor(0xFFFFFF)
                true
            }
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val inc = when (keyCode) {
            GLFW.GLFW_KEY_RIGHT -> increment
            GLFW.GLFW_KEY_LEFT -> -increment
            else -> 0.0
        }
        return if (inc != 0.0) {
            val test = storedValue.toDouble() + inc
            val testResult = validationProvider.apply(test)
            if (testResult.isError()) {
                if (testResult.get() != storedValue) {
                    applier.accept(testResult.get())
                }
            } else {
                val testResult2 = choiceValidator.validateEntry(testResult.get(), EntryValidator.ValidationType.STRONG)
                if (testResult2.get() != storedValue) {
                    applier.accept(testResult2.get())
                }
            }
            true
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers)
        }
    }

    private fun isChanged(): Boolean {
        return storedValue != wrappedValue.get()
    }

    /**
     * @suppress
     */
    final override fun setChangedListener(changedListener: Consumer<String>?) {
        super.setChangedListener(changedListener)
    }

    /**
     * @suppress
     */
    override fun getNarrationMessage(): MutableText {
        return "gui.narrate.editBox".translate("", "")
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, this.narrationMessage)
        builder.nextMessage().put(NarrationPart.TITLE, "${this.text}. ")
        if (increment != 0.0) {
            if (isFocused) {
                builder.nextMessage().put(NarrationPart.USAGE,
                    "fc.validated_field.number.editBox.usage".translate(),
                    "fc.validated_field.number.slider.usage".translate())
            } else {
                builder.nextMessage().put(NarrationPart.USAGE,
                    "fc.validated_field.number.editBox.usage".translate())
            }
        }
    }

    fun appendValueNarrations(builder: NarrationMessageBuilder) {
        builder.nextMessage().put(NarrationPart.TITLE, "fc.validated_field.current".translate(""))
        builder.nextMessage().nextMessage().put(NarrationPart.TITLE, "${this.text}. ")
    }

    override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
        if (!parentSelected) return TooltipChild.EMPTY
        return error
    }

    init {
        text = wrappedValue.get().toString()
        setChangedListener { s -> isValid = isValidTest(s) }
    }
}