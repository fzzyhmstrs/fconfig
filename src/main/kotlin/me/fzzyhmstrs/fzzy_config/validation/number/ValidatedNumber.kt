package me.fzzyhmstrs.fzzy_config.validation.number

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.ValidationBackedNumberFieldWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.SLIDER
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.KeyCodes
import net.minecraft.client.sound.SoundManager
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import org.lwjgl.glfw.GLFW
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.math.max

sealed class ValidatedNumber<T>(defaultValue: T, protected val minValue: T, protected val maxValue: T): ValidatedField<T>(defaultValue) where T: Number, T:Comparable<T> {

    override fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        if(input < minValue)
            return ValidationResult.error(minValue, "Validated number [$input] below the valid range [$minValue] to [$maxValue]")
        else if(input < minValue)
            return ValidationResult.error(maxValue, "Validated number [$input] above the valid range [$minValue] to [$maxValue]")
        return ValidationResult.success(input)
    }

    override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        if(input < minValue)
            return ValidationResult.error(input, "Validated number [$input] below the valid range [$minValue] to [$maxValue]")
        else if(input < minValue)
            return ValidationResult.error(input, "Validated number [$input] above the valid range [$minValue] to [$maxValue]")
        return ValidationResult.success(input)
    }

    /**
     * Determines which type of selector widget will be used for the Number selection
     * @sample [SLIDER]
     * @sample [TEXTBOX]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    enum class WidgetType{
        /**
         * An MC-style slider widget bounded between min and max.
         */
        SLIDER,

        /**
         * A textbox-style widget that lets you enter the number directly, throwing error if outside of range
         *
         * Will be automatically used if the "simple" constructor is used to make an unbounded number
         */
        TEXTBOX
    }

    companion object{
        private val CONFIRM_TEXTURE = "widget/action/confirm".fcId()
        private val CONFIRM_INACTIVE_TEXTURE = "widget/action/confirm_inactive".fcId()
        private val CONFIRM_HIGHLIGHTED_TEXTURE = "widget/action/confirm_highlighted".fcId()
    }

    @Environment(EnvType.CLIENT)
    protected open class ConfirmButtonSliderWidget<T:Number>(private val wrappedValue: Supplier<T>, private val minValue: T, private val maxValue: T, private val converter: Function<Double,T>, private val valueApplier: Consumer<T>):
        ClickableWidget(0, 0, 90, 20, wrappedValue.get().toString().lit()) {
        companion object{
            private val TEXTURE = Identifier("widget/slider")
            private val HIGHLIGHTED_TEXTURE = Identifier("widget/slider_highlighted")
            private val HANDLE_TEXTURE = Identifier("widget/slider_handle")
            private val HANDLE_HIGHLIGHTED_TEXTURE = Identifier("widget/slider_handle_highlighted")

        }

        private var confirmHovered = false
        private var confirmActive = false
        private var value: T = wrappedValue.get()
        private val increment = max((maxValue.toDouble() - minValue.toDouble())/ 70.0, 1.0)

        private fun isChanged(): Boolean{
            return value != wrappedValue.get()
        }

        private fun getTexture(): Identifier {
            return if (this.isFocused)
                HIGHLIGHTED_TEXTURE
            else
                TEXTURE
        }

        private fun getConfirmTexture(): Identifier {
            return if(confirmActive) {
                if (confirmHovered || this.isFocused)
                    CONFIRM_HIGHLIGHTED_TEXTURE
                else
                    CONFIRM_TEXTURE
            } else {
                CONFIRM_INACTIVE_TEXTURE
            }
        }

        private fun getHandleTexture(): Identifier {
            return if (hovered || this.isFocused)
                HANDLE_HIGHLIGHTED_TEXTURE
            else
                HANDLE_TEXTURE
        }

        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            this.confirmActive = isChanged()
            confirmHovered = mouseX >= (x + 70) && mouseY >= y && mouseX < x + width && mouseY < y + height
            val minecraftClient = MinecraftClient.getInstance()
            context.setShaderColor(1.0f, 1.0f, 1.0f, alpha)
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            context.drawGuiTexture(getTexture(), x, y, getWidth() - 20, getHeight())
            val progress = MathHelper.getLerpProgress(value.toDouble(),minValue.toDouble(),maxValue.toDouble())
            context.drawGuiTexture(getHandleTexture(), x + (progress * (width - 28).toDouble()).toInt(), y, 8, getHeight())
            context.drawGuiTexture(getConfirmTexture(), x + getWidth() - 20, y, 20, getHeight())
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
            this.drawScrollableText(context, minecraftClient.textRenderer, 2, 0xFFFFFF or (MathHelper.ceil(alpha * 255.0f) shl 24))
        }

        override fun drawScrollableText(context: DrawContext?, textRenderer: TextRenderer?, xMargin: Int, color: Int) {
            val i = x + xMargin
            val j = x + getWidth() - xMargin - 20
            drawScrollableText(context, textRenderer, message, i, y, j, y + getHeight(), color)
        }

        override fun getNarrationMessage(): MutableText? {
            return Text.translatable("gui.narrate.slider", message)
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            builder.put(NarrationPart.TITLE, this.narrationMessage as Text?)
            if (active) {
                if (this.isFocused) {
                    builder.put(NarrationPart.USAGE, "fc.validated_field.number.slider.usage".translate())
                } else {
                    builder.put(NarrationPart.USAGE,"fc.validated_field.number.slider.usage.unfocused".translate())
                }
            }
        }

        override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            if (KeyCodes.isToggle(keyCode)) {
                if(isChanged()) {
                    valueApplier.accept(value)
                    this.confirmActive = isChanged()
                }
            }
            val bl = keyCode == GLFW.GLFW_KEY_LEFT
            if (bl || keyCode == GLFW.GLFW_KEY_RIGHT) {
                val f = if (bl) -increment else increment
                val ff = MathHelper.clamp(value.toDouble() + f, minValue.toDouble(), maxValue.toDouble())
                this.setValue(ff)
                return true
            }

            return false
        }

        private fun setValueFromMouse(mouseX: Double) {
            this.setValue(MathHelper.clampedMap((mouseX - (x + 4).toDouble()) / (width - 8).toDouble(),0.0,1.0,minValue.toDouble(), maxValue.toDouble()))
        }

        private fun setValue(value: Double) {
            this.value = converter.apply(value)
            this.message = value.toString().lit()
        }

        override fun onClick(mouseX: Double, mouseY: Double) {
            if(mouseX >= (x + 70).toDouble() && mouseY >= y.toDouble() && mouseX < (x + width).toDouble() && mouseY < (y + height).toDouble())
                if(isChanged()){
                    valueApplier.accept(value)
                    this.confirmActive = isChanged()
                    return
                }
            setValueFromMouse(mouseX)
        }

        override fun onDrag(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double) {
            setValueFromMouse(mouseX)
            super.onDrag(mouseX, mouseY, deltaX, deltaY)
        }

        override fun playDownSound(soundManager: SoundManager?) {}

        override fun onRelease(mouseX: Double, mouseY: Double) {
            super.playDownSound(MinecraftClient.getInstance().soundManager)
        }
    }

    @Environment(EnvType.CLIENT)
    protected class ConfirmButtonTextFieldWidget<T: Number>(wrappedValue: Supplier<T>, validationProvider: Function<Double, ValidationResult<T>>, private val valueApplier: Consumer<T>):
        ClickableWidget(0,0, 90, 20, FcText.empty())
    {
        private val textFieldWidget = ValidationBackedNumberFieldWidget(70,20, wrappedValue, validationProvider)
        private var confirmActive = false
        private var confirmHovered = false


        private fun isChanged(): Boolean {
            return textFieldWidget.isChanged()
        }

        override fun getNarrationMessage(): MutableText {
            return textFieldWidget.text.lit()
        }

        private fun getConfirmTexture(): Identifier {
            return if(confirmActive) {
                if (confirmHovered || this.isFocused)
                    CONFIRM_HIGHLIGHTED_TEXTURE
                else
                    CONFIRM_TEXTURE
            } else {
                CONFIRM_INACTIVE_TEXTURE
            }
        }

        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            confirmActive = isChanged()
            confirmHovered = mouseX >= (x + 70) && mouseY >= y && mouseX < x + width && mouseY < y + height
            textFieldWidget.renderWidget(context, mouseX, mouseY, delta)
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            context.drawGuiTexture(getConfirmTexture(), x + getWidth() - 20, y, 20, getHeight())
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            builder.put(NarrationPart.TITLE, this.narrationMessage)
            if (active) {
                if (this.isFocused) {
                    builder.put(NarrationPart.USAGE, "fc.validated_field.number.textbox.usage".translate())
                } else {
                    builder.put(NarrationPart.USAGE, "fc.validated_field.number.textbox.usage.unfocused".translate())
                }
            }
        }

        override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            if (KeyCodes.isToggle(keyCode)) {
                if (isChanged()) {
                    valueApplier.accept(textFieldWidget.getValue())
                    confirmActive = isChanged()
                    return true
                }
            }
            return textFieldWidget.keyPressed(keyCode, scanCode, modifiers)
        }

        override fun charTyped(chr: Char, modifiers: Int): Boolean {
            return textFieldWidget.charTyped(chr, modifiers)
        }

        override fun onClick(mouseX: Double, mouseY: Double) {
            if(mouseX >= (x + 70) && mouseY >= y && mouseX < x + width && mouseY < y + height) {
                if (isChanged()) {
                    valueApplier.accept(textFieldWidget.getValue())
                    confirmActive = isChanged()
                    return
                }
            }
            textFieldWidget.onClick(mouseX, mouseY)
        }

        override fun setX(x: Int) {
            super.setX(x)
            textFieldWidget.x = x
        }

        override fun setY(y: Int) {
            super.setY(y)
            textFieldWidget.y = y
        }

    }
}