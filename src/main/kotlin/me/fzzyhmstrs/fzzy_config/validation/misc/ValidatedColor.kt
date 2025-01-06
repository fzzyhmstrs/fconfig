/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.misc

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.entry.EntryHandler
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedColor.ColorHolder
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedColor.Companion.validatedColor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.client.sound.SoundManager
import net.minecraft.sound.SoundEvents
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import net.peanuuutz.tomlkt.*
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * A validated color value
 *
 * This is a [ValidatedField] of type [ColorHolder], a basic Color data class.
 * @see [validatedColor]
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.colors
 * @throws IllegalStateException if the input RGBA values aren't in bounds (not in the range 0..255)
 * @author fzzyhmstrs
 * @since 0.1.2
 */
open class ValidatedColor: ValidatedField<ColorHolder> {

    /**
     * A validated color value
     *
     * This is a [ValidatedField] of type [ColorHolder], a basic Color data class.
     * @param r the default red component, 0 to 255
     * @param g the default green component, 0 to 255
     * @param b the default blue component, 0 to 255
     * @param a the default alpha(transparency) component, 0 to 255 or Int.MIN_VALUE to set the color as opaque. Defaults to Int.MIN_VALUE
     * @see [validatedColor]
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.colors
     * @throws IllegalStateException if the input RGBA values aren't in bounds (not in the range 0..255)
     * @author fzzyhmstrs
     * @since 0.1.2
     */
    @JvmOverloads
    constructor(r: Int, g: Int, b: Int, a: Int = Int.MIN_VALUE): super(ColorHolder(r, g, b, if(a > Int.MIN_VALUE) a else 255, a > Int.MIN_VALUE)) {
        if(r<0 || r>255) throw IllegalArgumentException("Red portion of validated color not provided a default value between 0 and 255")
        if(g<0 || g>255) throw IllegalArgumentException("Green portion of validated color not provided a default value between 0 and 255")
        if(b<0 || b>255) throw IllegalArgumentException("Blue portion of validated color not provided a default value between 0 and 255")
        if((a<0 && a!=Int.MIN_VALUE) || a>255) throw IllegalArgumentException("Transparency portion of validated color not provided a default value between 0 and 255")
    }

    /**
     * A validated color value with or without transparency enabled and with default color 0xFFFFFFFF (opaque white)
     * @param transparent Boolean, whether this color supports transparency
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(transparent: Boolean = true): super(ColorHolder(255, 255, 255, 255, transparent))

    /**
     * A validated color value built from a jwt [Color] with or without transparency enabled
     * @param color [Color] defining the RGBA of this validated color
     * @param transparent Boolean, whether this color supports transparency
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(color: Color, transparent: Boolean = true): this(color.red, color.green, color.blue, color.alpha, transparent)

    private constructor(r: Int, g: Int, b: Int, a: Int, alphaMode: Boolean): super(ColorHolder(r, g, b, a, alphaMode))

    /**
     * Convert this [ValidatedColor] to an ARGB hex string if this color supports transparency (0xFFFFFFFF), or to a RGB hex string otherwise (0xFFFFFF)
     * @return Hex string representation of this color
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun toHexString(): String {
        return get().toHexString()
    }

    /**
     * Sets the value of this [ValidatedColor] using the passed hex string (0xFFFFFFFF)
     * @param s String value representing a hex color
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @Suppress("unused")
    fun setFromHexString(s: String) {
        val colorInt = try {
            Integer.parseUnsignedInt(s, 16)
        } catch (e: Throwable) {
            FC.LOGGER.warn("Validated Color can't accept input [$s], maintaining current color [${toHexString()}]")
            get().toInt()
        }
        validateAndSet(get().fromInt(colorInt))
    }

    /**
     * returns ARGB color int representing this color
     * @return Int - ARGB formatted integer storing this color
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun toInt(): Int {
        return get().argb()
    }

    /**
     * Updates this color with a new holder representing the color passed in integer form
     * @param i Int - the ARGB int representing the new color holder
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun fromInt(i: Int) {
        validateAndSet(get().fromInt(i))
    }

    /**
     * Returns the 'r' component of the held ColorHolder
     * @return Int representing red component of the color
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun r(): Int {
        return get().r
    }

    /**
     * Returns the 'g' component of the held ColorHolder
     * @return Int representing green component of the color
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun g(): Int {
        return get().g
    }

    /**
     * Returns the 'b' component of the held ColorHolder
     * @return Int representing blue component of the color
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun b(): Int {
        return get().b
    }

    /**
     * Returns the 'a' component of the held ColorHolder
     * @return Int representing alpha component of the color
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun a(): Int {
        return get().a
    }

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<ColorHolder> {
        return storedValue.deserializeEntry(toml, mutableListOf(), fieldName, ConfigApiImpl.IGNORE_NON_SYNC)
    }

    @Internal
    override fun serialize(input: ColorHolder): ValidationResult<TomlElement> {
        val errors: MutableList<String> = mutableListOf()
        return ValidationResult.predicated(storedValue.serializeEntry(input, errors, ConfigApiImpl.IGNORE_NON_SYNC), errors.isEmpty(), errors.toString())
    }

    @Internal
    override fun correctEntry(input: ColorHolder, type: EntryValidator.ValidationType): ValidationResult<ColorHolder> {
        return storedValue.correctEntry(input, type)
    }

    @Internal
    override fun validateEntry(input: ColorHolder, type: EntryValidator.ValidationType): ValidationResult<ColorHolder> {
        return storedValue.validateEntry(input, type)
    }

    /**
     * creates a deep copy of this ValidatedColor
     * return ValidatedColor wrapping a deep copy of the currently stored holder and alphaMode
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedColor {
        return storedValue.instance()
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is ColorHolder && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input [ColorHolder] input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: ColorHolder): ColorHolder {
        return input.copy()
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<ColorHolder>): ClickableWidget {
        return ActiveButtonWidget({ this.toHexString().lit() }, 110, 20, { true }, { openColorEditPopup() })
    }

    @Internal
    override fun entryDeco(): Decorated.DecoratedOffset? {
        return Decorated.DecoratedOffset(ColorDecoration { get().argb() }, 2, 2)
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        val validation = if(get().opaque())
            "RGB 0..255, no Transparency"
        else
            "RGBA 0.255"
        return "ValidatedColor[value=${toHexString()}, validation=$validation]"
    }

    /////////////////////////

    //client
    private fun openColorEditPopup() {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val mutableColor = this.get().mutable(validatedString())
        val popup = PopupWidget.Builder(translation())
            .add("r_name", TextWidget(12, 20, "fc.validated_field.color.r".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("g_name", TextWidget(12, 20, "fc.validated_field.color.g".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("b_name", TextWidget(12, 20, "fc.validated_field.color.b".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("a_name", TextWidget(12, 20, "fc.validated_field.color.a".translate(), textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("r_box", ValidationBackedNumberFieldWidget(45, 20, { mutableColor.r }, ChoiceValidator.any(), {d -> mutableColor.validate(d.toInt())}, { r -> mutableColor.updateRGB(r, mutableColor.g, mutableColor.b)}), "r_name", LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .add("g_box", ValidationBackedNumberFieldWidget(45, 20, { mutableColor.g }, ChoiceValidator.any(), {d -> mutableColor.validate(d.toInt())}, { g -> mutableColor.updateRGB(mutableColor.r, g, mutableColor.b)}), "g_name", LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .add("b_box", ValidationBackedNumberFieldWidget(45, 20, { mutableColor.b }, ChoiceValidator.any(), {d -> mutableColor.validate(d.toInt())}, { b -> mutableColor.updateRGB(mutableColor.r, mutableColor.g, b)}), "b_name", LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .add("a_box", if(get().transparent()) ValidationBackedNumberFieldWidget(45, 20, { mutableColor.a }, ChoiceValidator.any(), {d -> mutableColor.validate(d.toInt())}, { a -> mutableColor.updateA(a)}) else TextFieldWidget(textRenderer, 0, 0, 45, 20, "255".lit()).also { it.setEditable(false) }, "a_name", LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .add("hl_map", HLMapWidget(mutableColor), "r_box", LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .add("s_slider", VerticalSliderWidget({mutableColor.s.toDouble()}, 0, 0, 20, 68, FcText.EMPTY, {d -> mutableColor.updateHSL(mutableColor.h, d.toFloat(), mutableColor.l)}), "hl_map", LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .add("hex_box", ValidationBackedTextFieldWidget(84, 20, {mutableColor.hex.get()}, ChoiceValidator.any(), mutableColor.hex, {s -> mutableColor.updateHex(s) }), "hl_map", LayoutWidget.Position.BELOW, LayoutWidget.Position.VERTICAL_TO_LEFT_EDGE)
            .addDoneWidget ({ this.setAndUpdate(mutableColor.createHolder()); PopupWidget.pop()})
            .onClose { this.setAndUpdate(mutableColor.createHolder()) }
            .noCloseOnClick()
            .build()
        PopupWidget.push(popup)
    }

    private fun validatedString(): ValidatedString {
        return validatedString(toHexString(), this.get().opaque())
    }

    companion object {
        /**
         * Builds a ValidatedColor from the provided hex color string
         * @param transparent default true, whether this color will accept transparency
         * @throws IllegalStateException if the hex string isn't valid
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun String.validatedColor(transparent: Boolean = true): ValidatedColor {
            val str = this.replace("#", "").replace("0x", "")
            val validatedString = validatedString(str, !transparent)
            val result = validatedString.validateEntry(str, EntryValidator.ValidationType.WEAK)
            if (result.isError())
                throw IllegalStateException(result.getError())
            val colorInt = try {
                Integer.parseUnsignedInt(str, 16)
            } catch (e: Throwable) {
                throw IllegalStateException("Error parsing shorthand Expression [$this]")
            }
            return Color(colorInt, transparent).validated(transparent)
        }

        private fun validatedString(str: String, opaque: Boolean): ValidatedString {
            fun isNotF(chr: Char): Boolean {
                return chr != 'f' && chr != 'F'
            }
            fun toHexChar(chr: Char): Char {
                val chk = Character.digit(chr, 16)
                return if(chk == -1) '0' else chr
            }
            fun transform(s: String): String {
                var ss = ""
                for (chr in s) {
                    ss += toHexChar(chr)
                }
                return ss
            }
            return ValidatedString.Builder(str)
                .both { s, _ ->
                    if (s.length > 8)
                        ValidationResult.error(s, "String too long for a valid color Integer")
                    else
                        try {
                            Integer.parseUnsignedInt(s, 16)
                            ValidationResult.success(s)
                        }catch (e: Throwable) {
                            ValidationResult.error(s, "String not parsable as color Integer: ${e.localizedMessage}")
                        }
                }
                .withCorrector()
                .both { s, _ ->
                    if(s.contains('#'))
                        ValidationResult.error(s.replace("#", ""), "'#' prefixes not allowed")
                    else if(s.contains("0x"))
                        ValidationResult.error(s.replace("0x", ""), "'0x' prefixes not allowed")
                    else if(s.length > 8)
                        ValidationResult.error(s.substring(0, 8), "Too long. 8 characters maximum")
                    else if(s.length == 7 && isNotF(s[0]) && opaque)
                        ValidationResult.error(s.replaceRange(0, 1, if(s[0].isLowerCase())"f" else "F"), "Opaque colors only.")
                    else if(s.length == 8 && (isNotF(s[0]) || isNotF(s[1])) && opaque)
                        ValidationResult.error(s.replaceRange(0, 2, "${if(s[0].isLowerCase())"f" else "F"}${if(s[1].isLowerCase())"f" else "F"}"), "Opaque colors only.")
                    else
                        transform(s).let { if(it == s) ValidationResult.success(it) else ValidationResult.error(it, "Invalid characters found in color string") }
                    ValidationResult.success(s)
                }
                .build()
        }
    }

    /**
     * An immutable holder of an ARGB color value. The return type of [ValidatedColor], which can be used directly in code or shortcutted with helper functions in ValdiatedColor to get Int or Hex String values instead.
     * @param r int value of r component (0..255)
     * @param g int value of g component (0..255)
     * @param b int value of b component (0..255)
     * @param a int value of a component (0..255)
     * @param alphaMode whether this color holder supports transparency
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.colorClasses
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    data class ColorHolder(val r: Int, val g: Int, val b: Int, val a: Int, private val alphaMode: Boolean): EntryHandler<ColorHolder> {

        private val validator: Predicate<Int> = Predicate{i -> i in 0..255 }

        /**
         * If this color holder supports transparency
         * @return Boolean - true if transparency is supported
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun transparent(): Boolean {
            return alphaMode
        }

        /**
         * If this color holder does NOT support transparency
         * @return Boolean - false if transparency is supported
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun opaque(): Boolean {
            return !alphaMode
        }

        /**
         * converts this color holder to a hex string (without prefix)
         * @return String - 6 or 8 character hex string representing this color. Alpha digits will only appear if transparency is supported
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun toHexString(): String {
            return if(opaque()) String.format("%06X", toInt()) else String.format("%08X", toInt())
        }

        /**
         * returns ARGB color int representing this color
         * @return Int - ARGB formatted integer storing this color
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun argb(): Int {
            return ((a and 0xFF) shl 24) or
                    ((r and 0xFF) shl 16) or
                    ((g and 0xFF) shl 8) or
                    ((b and 0xFF) shl 0)
        }

        /**
         * returns ARGB color int representing this color
         * @return Int - ARGB formatted integer storing this color
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun toInt(): Int {
            return argb()
        }

        /**
         * converts this color holder into a new one representing the color integer passed. AlphaMode is maintained
         * @param i Int - the ARGB int representing the new color holder
         * @return ColorHolder - the new color holder
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun fromInt(i: Int): ColorHolder {
            val components = Color(i, alphaMode)
            return this.copy(r = components.red, g = components.green, b = components.green, a = components.alpha)
        }

        /**
         * Generates a [MutableColor] from this color holder
         * @param hex - [ValidatedString], Optional - the validation for managing hex string representations of the MutableColor. Strongly recommended to use the defaulted overload
         * @return [MutableColor] representing this color holder
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmOverloads
        fun mutable(hex: ValidatedString = validatedString(toHexString(), opaque())): MutableColor {
            val mutable = MutableColor(hex, alphaMode)
            mutable.updateRGB(r, g, b)
            mutable.updateA(a)
            return mutable
        }

        /**
         * Creates a deep copy of this color holder and wraps it
         * @return ValidatedColor - wrapping a copy of this holder
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun instance(): ValidatedColor {
            return ValidatedColor(r, g, b, a, alphaMode)
        }

        @Internal
        override fun serializeEntry(
            input: ColorHolder?,
            errorBuilder: MutableList<String>,
            flags: Byte
        ): TomlElement {
            val toml = TomlTableBuilder()
            try {
                toml.element("r", TomlLiteral(r), TomlComment("Red component, 0 to 255"))
                toml.element("g", TomlLiteral(g), TomlComment("Green component, 0 to 255"))
                toml.element("b", TomlLiteral(b), TomlComment("Blue component, 0 to 255"))
                if (alphaMode) toml.element("a", TomlLiteral(a), TomlComment("Alpha component, 0 to 255"))
            } catch (e: Throwable) {
                errorBuilder.add("Critical exception while serializing color: ${e.localizedMessage}")
            }
            return toml.build()
        }

        @Internal
        override fun deserializeEntry(
            toml: TomlElement,
            errorBuilder: MutableList<String>,
            fieldName: String,
            flags: Byte
        ): ValidationResult<ColorHolder> {
            return try {
                val table = toml.asTomlTable()
                val tomlR = table["r"]?.asTomlLiteral()?.toInt() ?: return ValidationResult.error(this, "Error deserializing 'r' component of color [$fieldName], using previous value.")
                val tomlG = table["g"]?.asTomlLiteral()?.toInt() ?: return ValidationResult.error(this, "Error deserializing 'g' component of color [$fieldName], using previous value.")
                val tomlB = table["b"]?.asTomlLiteral()?.toInt() ?: return ValidationResult.error(this, "Error deserializing 'b' component of color [$fieldName], using previous value.")
                val tomlA = if(!alphaMode) 255 else table["a"]?.asTomlLiteral()?.toInt() ?: 255

                ValidationResult.success(ColorHolder(tomlR, tomlG, tomlB, tomlA, this.alphaMode))
            } catch (e: Throwable) {
                ValidationResult.error(this, "Critical error encountered deserializing color [$fieldName], using previous value.")
            }
        }

        @Internal
        override fun validateEntry(input: ColorHolder, type: EntryValidator.ValidationType): ValidationResult<ColorHolder> {
            val errors: MutableList<String> = mutableListOf()
            if (!validator.test(input.r)) errors.add("Red component out of bounds: ${input.r} outside 0-255")
            if (!validator.test(input.g)) errors.add("Green component out of bounds: ${input.g} outside 0-255")
            if (!validator.test(input.b)) errors.add("Blue component out of bounds: ${input.b} outside 0-255")
            if (input.alphaMode) {
                if (!validator.test(input.a)) errors.add("Alpha component out of bounds: ${input.a} outside 0-255")
            } else {
                if (input.a != 255) errors.add("Non-transparent color with <255 Alpha")
            }
            return if (errors.isNotEmpty()) {
                ValidationResult.error(input, "Errors validating color: $errors")
            } else {
                ValidationResult.success(input)
            }
        }

        @Internal
        override fun correctEntry(input: ColorHolder, type: EntryValidator.ValidationType): ValidationResult<ColorHolder> {
            val errors: MutableList<String> = mutableListOf()
            if (!validator.test(input.r)) errors.add("Red component out of bounds: ${input.r} outside 0-255")
            if (!validator.test(input.g)) errors.add("Green component out of bounds: ${input.g} outside 0-255")
            if (!validator.test(input.b)) errors.add("Blue component out of bounds: ${input.b} outside 0-255")
            if (input.alphaMode) {
                if (!validator.test(input.a)) errors.add("Alpha component out of bounds: ${input.a} outside 0-255")
            } else {
                if (input.a != 255) errors.add("Non-transparent color with <255 Alpha")
            }

            return if (errors.isNotEmpty()) {
                val newR = MathHelper.clamp(input.r, 0, 255)
                val newG = MathHelper.clamp(input.g, 0, 255)
                val newB = MathHelper.clamp(input.b, 0, 255)
                val newA = if(input.alphaMode) MathHelper.clamp(input.r, 0, 255) else 255
                val newColorHolder = ColorHolder(newR, newG, newB, newA, input.alphaMode)
                ValidationResult.error(newColorHolder, "Errors validating color, corrected to $newColorHolder: $errors")
            } else {
                ValidationResult.success(input)
            }
        }

        /**
         * @suppress
         */
        override fun toString(): String {
            return "[r=$r, g=$g, b=$b, a=$a]"
        }
    }

    /**
     * A mutable color that automatically updates it's RGB and HSL values based on new inputs. Generally this should be created from a ColorHolder, not instantiated directly
     * @param hex ValidatedString - defines the correction used on the hex string internally.
     * @param alphaMode - whether this classes parent supports transparency or not, passes this on to any new holders it creates
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.colorClasses
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    class MutableColor internal constructor(val hex: ValidatedString, private val alphaMode: Boolean) {
        var r: Int = 0
        var g: Int = 0
        var b: Int = 0
        var a: Int = 0
        var h: Float = 0f
        var s: Float = 0f
        var l: Float = 0f
        private val validator: Predicate<Int> = Predicate{i -> i in 0..255 }

        /**
         * ARGB color int representation of this color
         * @return Int - ARGB formatted integer storing this color
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun argb(): Int {
            return ((a and 0xFF) shl 24) or
                    ((r and 0xFF) shl 16) or
                    ((g and 0xFF) shl 8) or
                    ((b and 0xFF) shl 0)
        }

        /**
         * Hex string representation of this color
         * @param prefix String, optional - prefix to prepend to the hex string ("#" or "0x" for example)
         * @return String - hex value of this color in string form, with optional prefix
         */
        @JvmOverloads
        fun hexString(prefix: String = ""): String {
            return "$prefix${hex.get()}"
        }

        /**
         * updates this Mutable Color with new HSL values. RGB values automatically updated to match
         * @param h Float - hue component, 0f..1f
         * @param s Float - saturation component, 0f..1f
         * @param l Float - light component, 0f..1f
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun updateHSL(h: Float, s: Float, l: Float) {
            this.h = h
            this.s = s
            this.l = l
            val rgb = Color.HSBtoRGB(h, s, l)
            val rr = rgb shr 16 and 0xFF
            val gg = rgb shr 8 and 0xFF
            val bb = rgb and 0xFF
            this.r = rr
            this.g = gg
            this.b = bb
            hex.validateAndSet(String.format(if(alphaMode) "%08X" else "%06X", argb()))
        }

        /**
         * updates this Mutable Color with new RGB values. HSL values automatically updated to match
         * @param r Int - red component, 0..255
         * @param g Int - green component, 0..255
         * @param b Int - blue component, 0..255
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun updateRGB(r: Int, g: Int, b: Int) {
            this.r = r
            this.g = g
            this.b = b
            val hsl = Color.RGBtoHSB(r, g, b, null)
            this.h = hsl[0]
            this.s = hsl[1]
            this.l = hsl[2]
            hex.validateAndSet(String.format(if(alphaMode) "%08X" else "%06X", argb()))
        }

        /**
         * updates this Mutable Color with new Alpha value
         * @param a Int - alpha component 0..255
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun updateA(a: Int) {
            this.a = a
            hex.validateAndSet(String.format(if(alphaMode) "%08X" else "%06X", argb()))
        }

        /**
         * Updates this color from a new hex-string color representation. Automatically strips common prefixes, and automatically updates HSL and RGB values
         * @param new String - new color represented by hex string
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun updateHex(new: String) {
            hex.validateAndSet(new)
            val argb = try {
                Integer.parseUnsignedInt(new, 16)
            } catch (e: Throwable) {
                argb()
            }
            val aa = argb shr 24 and 0xFF
            val rr = argb shr 16 and 0xFF
            val gg = argb shr 8 and 0xFF
            val bb = argb and 0xFF
            updateA(aa)
            this.r = rr
            this.g = gg
            this.b = bb
            val hsl = Color.RGBtoHSB(r, g, b, null)
            this.h = hsl[0]
            this.s = hsl[1]
            this.l = hsl[2]
        }

        @Internal
        fun validate(input: Int): ValidationResult<Int> {
            return ValidationResult.predicated(input, validator.test(input), "Out of bounds [0-255]")
        }

        /**
         * Creates a color holder from this color
         * @return [ColorHolder] representing this color
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun createHolder(): ColorHolder {
            return ColorHolder(r, g, b, a, alphaMode)
        }
    }

    //////////////////////////////////////////

    private class ColorDecoration(private val colorSupplier: Supplier<Int>): Decorated {

        override fun renderDecoration(context: DrawContext, x: Int, y: Int, delta: Float, enabled: Boolean, selected: Boolean) {
            TextureDeco.DECO_FRAME.renderDecoration(context, x, y, delta, enabled, selected)
            RenderSystem.enableBlend()
            context.fill(x+2, y+2, x+14, y+14, colorSupplier.get())
        }
    }

    //client
    private class HLMapWidget(private val mutableColor: MutableColor): ClickableWidget(0, 0, 60, 68, "fc.validated_field.color.hl".translate()) {

        companion object {
            private val BORDER = "widget/validation/color/hsl_border".fcId()
            private val BORDER_HIGHLIGHTED = "widget/validation/color/hsl_border_highlighted".fcId()
            private val CENTER = "widget/validation/color/hsl_center".fcId()
            private val CENTER_DESAT = "widget/validation/color/hsl_center_desat".fcId()
            private val CROSSHAIR = "widget/validation/color/hsl_crosshair".fcId()

            private const val HORIZONTAL_INC = 1f/52f
            private const val VERTICAL_INC = 1f/60f
        }

        private var mouseHasBeenClicked = false

        override fun getNarrationMessage(): MutableText {
            return message.copy()
        }

        override fun renderButton(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            RenderSystem.enableBlend()
            RenderSystem.enableDepthTest()
            context.drawTex(if (isSelected) BORDER_HIGHLIGHTED else BORDER, x, y, getWidth(), getHeight(), 1.0f)
            if (mutableColor.s == 1f) {
                RenderSystem.enableBlend()
                RenderSystem.enableDepthTest()
                context.drawTex(CENTER, x+4, y+4, 52, 60, (mutableColor.a / 255f))
            } else {
                RenderSystem.enableBlend()
                RenderSystem.enableDepthTest()
                context.drawTex(CENTER_DESAT, x+4, y+4, 52, 60, (mutableColor.a / 255f))
                RenderSystem.enableBlend()
                RenderSystem.enableDepthTest()
                context.drawTex(CENTER, x+4, y+4, 52, 60, (mutableColor.a / 255f)*(mutableColor.s / 1f))
            }
            val cX = x + 4 + MathHelper.clampedMap(mutableColor.l, 0f, 1f, 0f, 52f).toInt() - 2
            val cY = y + 4 + MathHelper.clampedMap(mutableColor.h, 0f, 1f, 0f, 60f).toInt() - 2
            RenderSystem.enableBlend()
            RenderSystem.enableDepthTest()
            context.drawTex(CROSSHAIR, cX, cY, 5, 5)
        }

        override fun onClick(mouseX: Double, mouseY: Double) {
            mouseHasBeenClicked = true
            updateHL(mouseX, mouseY)
        }

        override fun onDrag(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double) {
            updateHL(mouseX, mouseY)
        }

        private fun updateHL(mouseX: Double, mouseY: Double) {
            val light = MathHelper.clamp((mouseX - (this.x + 4).toDouble())/52.0, 0.0, 1.0).toFloat()
            val hue = MathHelper.clamp((mouseY - (this.y + 4).toDouble())/60.0, 0.0, 1.0).toFloat()
            mutableColor.updateHSL(hue, mutableColor.s, light)
        }

        override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            return when(keyCode) {
                GLFW.GLFW_KEY_LEFT -> {
                    incrementL(-HORIZONTAL_INC)
                    true
                }
                GLFW.GLFW_KEY_RIGHT -> {
                    incrementL(HORIZONTAL_INC)
                    true
                }
                GLFW.GLFW_KEY_UP -> {
                    incrementH(-VERTICAL_INC)
                    true
                }
                GLFW.GLFW_KEY_DOWN -> {
                    incrementH(VERTICAL_INC)
                    true
                }
                else -> super.keyPressed(keyCode, scanCode, modifiers)
            }
        }

        private fun incrementH(amount: Float) {
            val hue = MathHelper.clamp(mutableColor.h+amount, 0f, 1f)
            mutableColor.updateHSL(hue, mutableColor.s, mutableColor.l)
        }

        private fun incrementL(amount: Float) {
            val light = MathHelper.clamp(mutableColor.l+amount, 0f, 1f)
            mutableColor.updateHSL(mutableColor.h, mutableColor.s, light)
        }

        override fun onRelease(mouseX: Double, mouseY: Double) {
            if (mouseHasBeenClicked)
                MinecraftClient.getInstance().soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f))
        }

        override fun playDownSound(soundManager: SoundManager) {
            //soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f))
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            builder.put(NarrationPart.TITLE, this.narrationMessage)
            if (active) {
                if (this.isFocused) {
                    builder.put(NarrationPart.USAGE, "fc.validated_field.color.hl.usage.keyboard".translate())
                } else {
                    builder.put(NarrationPart.USAGE, "fc.validated_field.color.hl.usage.mouse".translate())
                }
            }
        }
    }
}