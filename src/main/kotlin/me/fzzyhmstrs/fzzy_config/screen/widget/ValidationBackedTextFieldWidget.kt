package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.entry.EntryApplier
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
import net.minecraft.text.Text
import java.util.function.Supplier

@Suppress("LeakingThis")
@Environment(EnvType.CLIENT)
open class ValidationBackedTextFieldWidget(width: Int, height: Int, private val wrappedValue: Supplier<String>, private val choiceValidator: ChoiceValidator<String>, private val validator: EntryValidator<String>, private val applier: EntryApplier<String>):
    TextFieldWidget(MinecraftClient.getInstance().textRenderer,0,0, width, height, FcText.empty())
{

    private var cachedWrappedValue: String = wrappedValue.get()
    private var storedValue = wrappedValue.get()
    private var lastChangedTime: Long = 0L
    private var isValid = true
    private var prefix: Text? = null

    fun prefixed(prefix: Text): ValidationBackedTextFieldWidget {
        this.prefix = prefix
        return this
    }

    fun getValue(): String {
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
            this.text = this.storedValue
        }
        if(isChanged()){
            if (lastChangedTime != 0L && !ongoingChanges()) {
                applier.accept(storedValue)
                cachedWrappedValue = storedValue
            }
        }
        super.renderWidget(context, mouseX, mouseY, delta)
        if(isValid){
            if (ongoingChanges())
                context.drawGuiTexture(TextureIds.ENTRY_ONGOING,x + width - 20, y, 20, 20)
            else
                context.drawGuiTexture(TextureIds.ENTRY_OK,x + width - 20, y, 20, 20)
        } else {
            context.drawGuiTexture(TextureIds.ENTRY_ERROR,x + width - 20, y, 20, 20)
        }

    }

    private fun isValidTest(s: String): Boolean {
        val result = validator.validateEntry(s, EntryValidator.ValidationType.STRONG)
        return if(result.isError()) {
            this.tooltip = Tooltip.of(result.getError().lit())
            setEditableColor(0xFF5555)
            false
        } else {
            this.tooltip = null
            val result2 = choiceValidator.validateEntry(result.get(), EntryValidator.ValidationType.STRONG)
            if (result2.isError()){
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

    private fun isChanged(): Boolean {
        return storedValue != wrappedValue.get()
    }

    init {
        setMaxLength(1000)
        text = wrappedValue.get()
        setChangedListener { s -> isValid = isValidTest(s) }
    }

    override fun getNarrationMessage(): MutableText {
        return prefix?.copy()?.append(text.lit()) ?: text.lit()
    }
}