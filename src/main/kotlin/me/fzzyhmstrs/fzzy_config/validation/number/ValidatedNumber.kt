/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.number

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.widget.ValidationBackedNumberFieldWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.also
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
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
import net.minecraft.client.resource.language.I18n
import net.minecraft.client.sound.SoundManager
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.glfw.GLFW
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

sealed class ValidatedNumber<T>(defaultValue: T, protected val minValue: T, protected val maxValue: T, protected val widgetType: WidgetType): ValidatedField<T>(defaultValue) where T: Number, T:Comparable<T> {

    @Internal
    override fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        if(input < minValue)
            return ValidationResult.error(minValue, "Validated number [$input] below the valid range [$minValue] to [$maxValue]")
        else if(input > maxValue)
            return ValidationResult.error(maxValue, "Validated number [$input] above the valid range [$minValue] to [$maxValue]")
        return ValidationResult.success(input)
    }
    @Internal
    override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        if(input < minValue)
            return ValidationResult.error(input, "Validated number [$input] below the valid range [$minValue] to [$maxValue]")
        else if(input > maxValue)
            return ValidationResult.error(input, "Validated number [$input] above the valid range [$minValue] to [$maxValue]")
        return ValidationResult.success(input)
    }

    protected abstract fun convert(input: Double): ValidationResult<T>

    @ApiStatus.Internal
    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        return when(widgetType) {
            SLIDER -> {
                ConfirmButtonSliderWidget(this, this.minValue, this.maxValue, choicePredicate, {d -> convert(d).get() }, { setAndUpdate(it) })
            }
            TEXTBOX -> {
                ConfirmButtonTextFieldWidget(this, choicePredicate, {d -> val result = convert(d); this.validateEntry(result.get(), EntryValidator.ValidationType.STRONG).also(result.isValid(), result.getError())}, { setAndUpdate(it) })
            }
        }
    }

    override fun description(fallback: String?): MutableText {
        return if(I18n.hasTranslation(descriptionKey())) super.description(fallback) else genericDescription()
    }
    private fun genericDescription(): MutableText {
        return if (minValue.compareTo(minBound()) == 0) {
            if (maxValue.compareTo(maxBound()) == 0) {
                "fc.validated_field.number.desc.fallback.any".translate()
            } else {
                "fc.validated_field.number.desc.fallback.min".translate(maxValue)
            }
        } else if (maxValue.compareTo(maxBound()) == 0) {
            "fc.validated_field.number.desc.fallback.max".translate(minValue)
        } else {
            "fc.validated_field.number.desc.fallback".translate(minValue, maxValue)
        }
    }

    override fun hasDescription(): Boolean {
        return true
    }

    @Internal
    protected abstract fun minBound(): T
    @Internal
    protected abstract fun maxBound(): T

    /**
     * Determines which type of selector widget will be used for the Number selection
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    enum class WidgetType {
        /**
         * An MC-style slider widget bounded between min and max.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        SLIDER,

        /**
         * A textbox-style widget that lets you enter the number directly, throwing error if outside of range
         *
         * Will be automatically used if the "simple" constructor is used to make an unbounded number
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        TEXTBOX
    }

    @Environment(EnvType.CLIENT)
    protected class ConfirmButtonSliderWidget<T:Number>(private val wrappedValue: Supplier<T>, private val minValue: T, private val maxValue: T, private val validator: ChoiceValidator<T>, private val converter: Function<Double, T>, private val valueApplier: Consumer<T>):
        ClickableWidget(0, 0, 110, 20, wrappedValue.get().toString().lit()) {
        companion object {
            private val TEXTURE = Identifier("widget/slider")
            private val HIGHLIGHTED_TEXTURE = Identifier("widget/slider_highlighted")
            private val HANDLE_TEXTURE = Identifier("widget/slider_handle")
            private val HANDLE_HIGHLIGHTED_TEXTURE = Identifier("widget/slider_handle_highlighted")
        }

        private fun split(range: Double): Double {
            var d = range
            while (d.toInt().toDouble() != d) {
                d *= 10.0
            }
            return if (d % 16.0 == 0.0) {
                (range / 16.0)
            } else if (d % 12.0 == 0.0) {
                (range / 12.0)
            } else {
                (range / 10.0)
            }
        }

        private fun getTexture(): Identifier {
            return if (this.isFocused)
                HIGHLIGHTED_TEXTURE
            else
                TEXTURE
        }

        private fun getHandleTexture(): Identifier {
            return if (hovered || this.isFocused)
                HANDLE_HIGHLIGHTED_TEXTURE
            else
                HANDLE_TEXTURE
        }

        private var confirmActive = false
        private var cachedWrappedValue: T = wrappedValue.get()
        private var value: T = wrappedValue.get()
        private val increment = max((maxValue.toDouble() - minValue.toDouble())/ 102.0, min(1.0, split(maxValue.toDouble() - minValue.toDouble())))
        private var isValid = validator.validateEntry(wrappedValue.get(), EntryValidator.ValidationType.STRONG).isValid()

        private fun isChanged(): Boolean {
            return value != wrappedValue.get()
        }

        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            val testValue = wrappedValue.get()
            if (cachedWrappedValue != testValue) {
                this.value = testValue
                cachedWrappedValue = testValue
                this.message = this.value.toString().lit()
            }
            this.confirmActive = isChanged() && isValid
            val minecraftClient = MinecraftClient.getInstance()
            context.setShaderColor(1.0f, 1.0f, 1.0f, alpha)
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            context.drawGuiTexture(getTexture(), x, y, getWidth(), getHeight())
            val progress = MathHelper.getLerpProgress(value.toDouble(), minValue.toDouble(), maxValue.toDouble())
            context.drawGuiTexture(getHandleTexture(), x + (progress * (width - 8).toDouble()).toInt(), y, 8, getHeight())
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
            this.drawScrollableText(context, minecraftClient.textRenderer, 2, 0xFFFFFF or (MathHelper.ceil(alpha * 255.0f) shl 24))
        }

        private fun getYImage(): Int {
            val i = if (this.isFocused) 1 else 0
            return i * 20
        }

        private fun getTextureV(): Int {
            val i = if (hovered || this.isFocused) 3 else 2
            return i * 20
        }

        override fun drawScrollableText(context: DrawContext?, textRenderer: TextRenderer?, xMargin: Int, color: Int) {
            val i = x + xMargin
            val j = x + getWidth() - xMargin
            drawScrollableText(context, textRenderer, message, i, y, j, y + getHeight(), color)
        }

        override fun getNarrationMessage(): MutableText {
            return FcText.translatable("gui.narrate.slider", message)
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            builder.put(NarrationPart.TITLE, this.narrationMessage as Text?)
            if (active) {
                if (this.isFocused) {
                    builder.put(NarrationPart.USAGE, "fc.validated_field.number.slider.usage".translate())
                } else {
                    builder.put(NarrationPart.USAGE, "fc.validated_field.number.slider.usage.unfocused".translate())
                }
            }
        }

        override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            val bl = keyCode == GLFW.GLFW_KEY_LEFT
            if (bl || keyCode == GLFW.GLFW_KEY_RIGHT) {
                val f = if (bl) -increment else increment
                val ff = MathHelper.clamp(value.toDouble() + f, minValue.toDouble(), maxValue.toDouble())
                this.setValue(ff)
                this.isValid = validator.validateEntry(this.value, EntryValidator.ValidationType.STRONG).isValid()
                if(isChanged() && isValid) {
                    cachedWrappedValue = value
                    valueApplier.accept(value)
                    this.confirmActive = isChanged() && isValid
                }
                return true
            }

            return false
        }

        private fun setValueFromMouse(mouseX: Double) {
            this.setValue(MathHelper.clampedMap((mouseX - (x + 4).toDouble()) / (width - 8).toDouble(), 0.0, 1.0, minValue.toDouble(), maxValue.toDouble()))
        }

        private fun setValue(value: Double) {
            this.value = converter.apply(value)
            this.message = this.value.toString().lit()
        }

        override fun onClick(mouseX: Double, mouseY: Double) {
            setValueFromMouse(mouseX)
        }

        override fun onDrag(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double) {
            setValueFromMouse(mouseX)
            super.onDrag(mouseX, mouseY, deltaX, deltaY)
        }

        override fun playDownSound(soundManager: SoundManager?) {}

        override fun onRelease(mouseX: Double, mouseY: Double) {
            this.isValid = validator.validateEntry(this.value, EntryValidator.ValidationType.STRONG).isValid()
            if(isChanged() && isValid) {
                cachedWrappedValue = value
                valueApplier.accept(value)
                this.confirmActive = isChanged() && isValid
            }
            super.playDownSound(MinecraftClient.getInstance().soundManager)
        }
    }

    @Environment(EnvType.CLIENT)
    protected class ConfirmButtonTextFieldWidget<T: Number>(
        wrappedValue: Supplier<T>,
        choiceValidator: ChoiceValidator<T>,
        validationProvider: Function<Double, ValidationResult<T>>,
        valueApplier: Consumer<T>)
        :
        ValidationBackedNumberFieldWidget<T>(110, 20, wrappedValue, choiceValidator, validationProvider, valueApplier)
}