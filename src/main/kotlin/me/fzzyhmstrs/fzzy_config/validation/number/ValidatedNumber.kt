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

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutClickableWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.screen.widget.ValidationBackedNumberFieldWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.ofMutable
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.KeyInput
import net.minecraft.client.resource.language.I18n
import net.minecraft.client.sound.SoundManager
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.math.MathHelper
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.glfw.GLFW
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

sealed class ValidatedNumber<T>(defaultValue: T, protected val minValue: T, protected val maxValue: T, protected val widgetType: WidgetType): ValidatedField<T>(defaultValue) where T: Number, T:Comparable<T> {

    init {
        if (minValue >= maxValue) throw IllegalStateException("Min value $minValue can't be >= Max value $maxValue")
    }

    protected abstract var increment: T?
    protected var format: DecimalFormat? = null

    @Internal
    override fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        if(input < minValue)
            return ValidationResult.error(minValue, ValidationResult.Errors.OUT_OF_BOUNDS, "Validated number [$input] below the valid range [$minValue] to [$maxValue]")
        else if(input > maxValue)
            return ValidationResult.error(maxValue, ValidationResult.Errors.OUT_OF_BOUNDS, "Validated number [$input] above the valid range [$minValue] to [$maxValue]")
        return ValidationResult.success(input)
    }

    @Internal
    override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        if(input < minValue)
            return ValidationResult.error(input, ValidationResult.Errors.OUT_OF_BOUNDS, "Validated number [$input] below the valid range [$minValue] to [$maxValue]")
        else if(input > maxValue)
            return ValidationResult.error(input, ValidationResult.Errors.OUT_OF_BOUNDS, "Validated number [$input] above the valid range [$minValue] to [$maxValue]")
        return ValidationResult.success(input)
    }

    protected abstract fun convert(input: Double): ValidationResult<T>

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        return when(widgetType) {
            SLIDER -> {
                ConfirmButtonSliderWidget(this, this.increment, this.minValue, this.maxValue, choicePredicate, {d -> convert(d).get() }, { setAndUpdate(it) }).also {
                    val f = format
                    if (f != null) {
                        it.setFormat(f)
                    }
                }
            }
            TEXTBOX -> {
                ConfirmButtonTextFieldWidget(this, choicePredicate, validator(), { setAndUpdate(it) }).also {
                    val f = format
                    if (f != null) {
                        it.setFormat(f)
                    }
                }
            }
            TEXTBOX_WITH_BUTTONS -> {
                prepareTextboxWithButtons(choicePredicate, this.increment)
            }
        }
    }

    @Internal
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

    companion object {
        /**
         * Defines a custom increment amount for this validation's widget. This will change how a SLIDER increments with keyboard input, and will change the increment applied with the up and down buttons of TEXTBOX_WITH_BUTTONS
         * @return this validation, passed through
         * @author fzzyhmstrs
         * @since 0.7.2
         */
        @JvmStatic
        fun <T, F: ValidatedNumber<T>>F.withIncrement(increment: T): F {
            this.increment = increment
            return this
        }

        /**
         * Sets a custom [DecimalFormat] for the numbers displayed on the selected widget.
         * - The default for sliders is "#.##" (decimal number with two decimal places)
         * - The default for textboxes is "0" + up to 340 decimal places (a decimal number with a sliding scale of shown decimal places, up to 340. This prevents Scientific notation from appearing)
         * @return this validation, passed through
         * @author fzzyhmstrs
         * @since 0.7.2
         */
        @JvmStatic
        fun <T, F: ValidatedNumber<T>>F.setFormat(format: DecimalFormat): F {
            this.format = format
            return this
        }
    }

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
        TEXTBOX,

        /**
         * A textbox-style widget that lets you enter the number directly, throwing error if outside of range; with two small buttons on the right side for incrementing up and down
         *
         * by default, the buttons will pick an increment based on the allowable range. Use [withIncrement] to define a custom increment amount
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        TEXTBOX_WITH_BUTTONS
    }

    protected fun validator(): Function<Double, ValidationResult<T>> {
        return Function { d ->
            val mutable = ValidationResult.createMutable("Number validation found errors")
            val result = convert(d).attachTo(mutable)
            val result2 = this.correctEntry(result.get(), EntryValidator.ValidationType.STRONG).attachTo(mutable)
            ofMutable(result2.get(), mutable)
        }
    }

    private fun prepareTextboxWithButtons(choicePredicate: ChoiceValidator<T>, incr: T?): ClickableWidget {
        fun isIntType(): Boolean {
            return maxValue is Int || maxValue is Long || maxValue is Short || maxValue is Byte
        }

        fun split(range: Double): Double {
            var d = range
            while (d < 16.0) {
                d *= 100.0
            }
            d = round(d)
            return if (d.toInt() % 16 == 0) {
                (range / 16.0)
            } else if (d.toInt() % 12 == 0) {
                (range / 12.0)
            } else {
                (range / 10.0)
            }
        }

        val increment = incr?.toDouble() ?: max(
            (maxValue.toDouble() - minValue.toDouble())/ 100.0,
            if (isIntType()) {
                max(1.0, split(maxValue.toDouble() - minValue.toDouble()))
            } else {
                min(1.0, split(maxValue.toDouble() - minValue.toDouble()))
            }
        )

        val layout = LayoutWidget.builder().paddingW(0).paddingH(0).spacingW(0).spacingH(0).build()
        val numberWidget = ConfirmButtonTextFieldWidget(this, choicePredicate, validator(), { setAndUpdate(it) }, 99, false, increment).also {
            val f = format
            if (f != null) {
                it.setFormat(f)
            }
        }

        layout.add(
            "textbox",
            numberWidget,
            LayoutWidget.Position.LEFT,
            LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY)
        layout.add(
            "up",
            CustomButtonWidget.builder("fc.button.up".translate()) {
                val n = this.convert(MathHelper.clamp(this.get().toDouble() + increment, this.minValue.toDouble(), this.maxValue.toDouble())).get()
                this.setAndUpdate(n) }
                .noMessage()
                .narrationAppender { builder -> numberWidget.appendValueNarrations(builder) }
                .size(11, 10)
                .active(this.maxValue != this.minValue)
                .textures(TextureIds.INCREMENT_UP, TextureIds.INCREMENT_UP_DISABLED, TextureIds.INCREMENT_UP_HIGHLIGHTED)
                .build(),
            LayoutWidget.Position.RIGHT,
            LayoutWidget.Position.ALIGN_RIGHT,
            LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        layout.add(
            "down",
            CustomButtonWidget.builder("fc.button.down".translate()) {
                val n = this.convert(MathHelper.clamp(this.get().toDouble() - increment, this.minValue.toDouble(), this.maxValue.toDouble())).get()
                this.setAndUpdate(n) }
                .noMessage()
                .size(11, 10)
                .active(this.maxValue != this.minValue)
                .textures(TextureIds.INCREMENT_DOWN, TextureIds.INCREMENT_DOWN_DISABLED, TextureIds.INCREMENT_DOWN_HIGHLIGHTED)
                .build(),
            LayoutWidget.Position.BELOW,
            LayoutWidget.Position.ALIGN_RIGHT,
            LayoutWidget.Position.VERTICAL_TO_LEFT_EDGE)
        return LayoutClickableWidget(0, 0, 110, 20, layout)
    }

    //client
    protected class ConfirmButtonSliderWidget<T:Number>(private val wrappedValue: Supplier<T>, incr: T?, private val minValue: T, private val maxValue: T, private val validator: ChoiceValidator<T>, private val converter: Function<Double, T>, private val valueApplier: Consumer<T>):
        ClickableWidget(0, 0, 110, 20, wrappedValue.get().toString().lit()) {

        companion object {
            private val TEXTURE = "widget/slider".simpleId()
            private val HIGHLIGHTED_TEXTURE = "widget/slider_highlighted".simpleId()
            private val HANDLE_TEXTURE = "widget/slider_handle".simpleId()
            private val HANDLE_HIGHLIGHTED_TEXTURE = "widget/slider_handle_highlighted".simpleId()

        }

        private var DECIMAL_FORMAT: DecimalFormat = Util.make(
            DecimalFormat("#.##")
        ) { format: DecimalFormat ->
            format.decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.ROOT)
        }

        fun setFormat(format: DecimalFormat) {
            this.DECIMAL_FORMAT = format
        }

        init {
            this.message = DECIMAL_FORMAT.format(wrappedValue.get()).lit()
        }

        private fun split(range: Double): Double {
            var d = range
            while (d < 16.0) {
                d *= 100.0
            }
            d = round(d)
            return if (d.toInt() % 16 == 0) {
                (range / 16.0)
            } else if (d.toInt() % 12 == 0) {
                (range / 12.0)
            } else {
                (range / 10.0)
            }
        }

        private var confirmActive = false
        private var cachedWrappedValue: T = wrappedValue.get()
        private var value: T = wrappedValue.get()
        private val increment = incr?.toDouble() ?: max(
            (maxValue.toDouble() - minValue.toDouble())/ 102.0,
            if (isIntType()) {
                max(1.0, split(maxValue.toDouble() - minValue.toDouble()))
            } else {
                min(1.0, split(maxValue.toDouble() - minValue.toDouble()))
            }
        )
        private var isValid = validator.validateEntry(wrappedValue.get(), EntryValidator.ValidationType.STRONG).isValid()

        private fun isIntType(): Boolean {
            return maxValue is Int || maxValue is Long || maxValue is Short || maxValue is Byte
        }

        private fun isChanged(): Boolean {
            return value != wrappedValue.get()
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

        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            val testValue = wrappedValue.get()
            if (cachedWrappedValue != testValue) {
                this.value = testValue
                cachedWrappedValue = testValue
                this.message = DECIMAL_FORMAT.format(this.value).lit()
            }
            this.confirmActive = isChanged() && isValid
            val minecraftClient = MinecraftClient.getInstance()
            context.drawTex(getTexture(), x, y, getWidth(), getHeight(), alpha)
            val progress = MathHelper.getLerpProgress(value.toDouble(), minValue.toDouble(), maxValue.toDouble())
            context.drawTex(getHandleTexture(), x + (progress * (width - 8).toDouble()).toInt(), y, 8, getHeight())
            this.drawScrollableText(context, minecraftClient.textRenderer, 2, 0xFFFFFF or (MathHelper.ceil(alpha * 255.0f) shl 24))
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
                    builder.put(NarrationPart.USAGE,
                        "fc.validated_field.number.slider.usage".translate(),
                        "fc.validated_field.number.slider.usage2".translate())
                } else {
                    builder.put(NarrationPart.USAGE, "fc.validated_field.number.slider.usage.unfocused".translate())
                }
            }
        }

        override fun keyPressed(input: KeyInput): Boolean {
            val bl = input.isLeft
            if (bl || input.isRight) {
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
            this.message = DECIMAL_FORMAT.format(this.value).lit()
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

    //client
    protected class ConfirmButtonTextFieldWidget<T: Number>(
        wrappedValue: Supplier<T>,
        choiceValidator: ChoiceValidator<T>,
        validationProvider: Function<Double, ValidationResult<T>>,
        valueApplier: Consumer<T>,
        width: Int = 110,
        renderStatus: Boolean = true,
        increment: Double = 0.0)
        :
        ValidationBackedNumberFieldWidget<T>(
            width,
            20,
            wrappedValue,
            choiceValidator,
            validationProvider,
            valueApplier,
            renderStatus,
            increment)
}