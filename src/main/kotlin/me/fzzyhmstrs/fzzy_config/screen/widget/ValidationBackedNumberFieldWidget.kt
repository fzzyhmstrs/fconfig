package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

@Environment(EnvType.CLIENT)
class ValidationBackedNumberFieldWidget<T: Number>(width: Int, height: Int, private val wrappedValue: Supplier<T>, private val validationProvider: Function<Double, ValidationResult<T>>, private val listener: Consumer<T> = Consumer { _ ->}):
    TextFieldWidget(MinecraftClient.getInstance().textRenderer,0,0, width, height, FcText.empty())
{

    private var storedValue = wrappedValue.get()
    private var isValid = true
    private var confirmActive = false
    private var prefix: Text? = null

    fun prefixed(prefix: Text): ValidationBackedNumberFieldWidget<T>{
        this.prefix = prefix
        return this
    }

    fun getValue(): T {
        return storedValue
    }

    private fun isValidTest(s: String): Boolean {
        val test = s.toDoubleOrNull()
        if (test == null) {
            this.tooltip = Tooltip.of("fc.validated_field.number.textbox.invalid".translate())
            setEditableColor(Formatting.RED.colorValue?:0xFFFFFF)
            return false
        }
        val result = validationProvider.apply(test)
        return if(result.isError()) {
            this.tooltip = Tooltip.of(result.getError().lit())
            setEditableColor(Formatting.RED.colorValue?:0xFFFFFF)
            false
        } else {
            this.storedValue = result.get()
            listener.accept(storedValue)
            confirmActive = isChanged()
            setEditableColor(0xFFFFFF)
            true
        }
    }

    fun isChanged(): Boolean {
        return storedValue != wrappedValue.get()
    }

    init {
        setChangedListener { s -> isValid = isValidTest(s) }
        text = wrappedValue.get().toString()
    }

    override fun getNarrationMessage(): MutableText {
        return prefix?.copy()?.append(text.lit()) ?: text.lit()
    }
}