package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds.ENTRY_ERROR
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds.ENTRY_OK
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds.ENTRY_ONGOING
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

@Suppress("LeakingThis")
@Environment(EnvType.CLIENT)
open class ValidationBackedNumberFieldWidget<T: Number>(width: Int, height: Int, private val wrappedValue: Supplier<T>,private val choiceValidator: ChoiceValidator<T>, private val validationProvider: Function<Double, ValidationResult<T>>, private val listener: Consumer<T> = Consumer { _ ->}):
    TextFieldWidget(MinecraftClient.getInstance().textRenderer,0,0, width, height, FcText.empty())
{

    private var cachedWrappedValue: T = wrappedValue.get()
    private var storedValue: T = wrappedValue.get()
    private var lastChangedTime: Long = 0L
    private var isValid = true
    private var confirmActive = false
    private var prefix: Text? = null

    fun prefixed(prefix: Text): ValidationBackedNumberFieldWidget<T> {
        this.prefix = prefix
        return this
    }

    fun getValue(): T {
        return storedValue
    }

    private fun ongoingChanges(): Boolean{
        return System.currentTimeMillis() - lastChangedTime <= 350L
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val testValue = wrappedValue.get()
        if (cachedWrappedValue != testValue){
            this.storedValue = testValue
            this.cachedWrappedValue = testValue
            this.text = this.storedValue.toString()
        }
        if(isChanged()){
            if (lastChangedTime != 0L && !ongoingChanges()) {
                cachedWrappedValue = storedValue
                listener.accept(storedValue)
            }
        }
        super.renderWidget(context, mouseX, mouseY, delta)
        if(isValid){
            if (ongoingChanges())
                context.drawGuiTexture(ENTRY_ONGOING,x + width - 20, y, 20, 20)
            else
                context.drawGuiTexture(ENTRY_OK,x + width - 20, y, 20, 20)
        } else {
            context.drawGuiTexture(ENTRY_ERROR,x + width - 20, y, 20, 20)
        }

    }

    override fun getInnerWidth(): Int {
        return super.getInnerWidth() - 11
    }

    private fun isValidTest(s: String): Boolean {
        val test = s.toDoubleOrNull()
        if (test == null) {
            this.tooltip = Tooltip.of("fc.validated_field.number.textbox.invalid".translate())
            setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
            return false
        }
        val result = validationProvider.apply(test)
        return if(result.isError()) {
            this.tooltip = Tooltip.of(result.getError().lit())
            setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
            false
        } else {
            this.tooltip = null
            val result2 = choiceValidator.validateEntry(result.get(),EntryValidator.ValidationType.STRONG)
            if (result2.isError()){
                this.tooltip = Tooltip.of(result.getError().lit())
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

    private fun isChanged(): Boolean {
        return storedValue != wrappedValue.get()
    }

    init {
        text = wrappedValue.get().toString()
        setChangedListener { s -> isValid = isValidTest(s) }
    }

    override fun getNarrationMessage(): MutableText {
        return prefix?.copy()?.append(text.lit()) ?: text.lit()
    }
}