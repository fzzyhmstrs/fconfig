package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.entry.EntryHandler
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedColor.ColorHolder
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedColor.Companion.validatedColor
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper
import net.peanuuutz.tomlkt.*
import java.awt.Color
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * A validated color value
 *
 * This is a [ValidatedField] of type [ColorHolder], a basic Color data class.
 * @param r the default red component, 0 to 255
 * @param g the default green component, 0 to 255
 * @param b the default blue component, 0 to 255
 * @param a the default alpha(transparency) component, 0 to 255 or Int.MIN_VALUE to set the color as opaque. Defaults to Int.MIN_VALUE
 * @see [validatedColor]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedColor]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedColorOpaque]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedColorString]
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.fieldLang
 * @throws IllegalStateException if the input RGBA values aren't in bounds (not in the range 0..255)
 * @author fzzyhmstrs
 * @since 0.1.2
 */
class ValidatedColor: ValidatedField<ColorHolder> {

    @JvmOverloads constructor(r: Int, g: Int, b: Int, a: Int = Int.MIN_VALUE): super(ColorHolder(r, g, b, if(a > Int.MIN_VALUE) a else 255, a > Int.MIN_VALUE)) {
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
    constructor(color: Color, transparent: Boolean = true): this(color.red,color.green,color.blue,color.alpha, transparent)

    private constructor(r: Int, g: Int, b: Int, a: Int, alphaMode: Boolean): super(ColorHolder(r, g, b, a, alphaMode))

    fun toHexString(): String {
        return if(get().opaque()) String.format("%06X", get().toInt()) else String.format("%08X", get().toInt())
    }
    fun setFromHexString(s: String) {
        val colorInt = try {
            Integer.parseUnsignedInt(s, 16)
        } catch (e: Exception){
            get().toInt()
        }
        validateAndSet(get().fromInt(colorInt))
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<ColorHolder> {
        return storedValue.deserializeEntry(toml, mutableListOf(), fieldName, true)
    }

    override fun serialize(input: ColorHolder): ValidationResult<TomlElement> {
        val errors: MutableList<String> = mutableListOf()
        return ValidationResult.predicated(storedValue.serializeEntry(input, errors, true), errors.isEmpty(), errors.toString())
    }

    override fun correctEntry(input: ColorHolder, type: EntryValidator.ValidationType): ValidationResult<ColorHolder> {
        return storedValue.correctEntry(input, type)
    }

    override fun validateEntry(input: ColorHolder, type: EntryValidator.ValidationType): ValidationResult<ColorHolder> {
        return storedValue.validateEntry(input, type)
    }

    override fun copyStoredValue(): ColorHolder {
        return storedValue.copy()
    }

    override fun instanceEntry(): ValidatedColor {
        return storedValue.instance()
    }

    override fun isValidEntry(input: Any?): Boolean {
        return input is ColorHolder && validateEntry(input,EntryValidator.ValidationType.STRONG).isValid()
    }

    override fun widgetEntry(choicePredicate: ChoiceValidator<ColorHolder>): ClickableWidget {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        val validation = if(get().opaque())
            "RGB 0..255, no Transparency"
        else
            "RGBA 0.255"
        return "ValidatedColor[value=${toHexString()}, validation=$validation]"
    }

    /////////////////////////

    private fun validatedString(): ValidatedString {
        return validatedString(toHexString(), this.get().opaque())
    }

    companion object {
        fun String.validatedColor(transparent: Boolean = true): ValidatedColor {
            val str = this.replace("#","").replace("0x","")
            val validatedString = validatedString(str, !transparent)
            val result = validatedString.validateEntry(str, EntryValidator.ValidationType.WEAK)
            if (result.isError())
                throw IllegalStateException(result.getError())
            val colorInt = try {
                Integer.parseUnsignedInt(str, 16)
            } catch (e: Exception){
                throw IllegalStateException("Error parsing shorthand Expression [$this]")
            }
            return Color(colorInt, transparent).validated(transparent)
        }

        private fun validatedString(str: String, opaque: Boolean): ValidatedString {
            fun isNotF(chr: Char): Boolean{
                return chr != 'f' && chr != 'F'
            }
            fun toHexChar(chr: Char): Char{
                val chk = Character.digit(chr,16)
                return if(chk == -1) '0' else chr
            }
            fun transform(s: String): String{
                var ss = ""
                for (chr in s){
                    ss += toHexChar(chr)
                }
                return ss
            }
            return ValidatedString.Builder(str)
                .both { s,_ ->
                    if (s.length > 8)
                        ValidationResult.error(s,"String too long for a valid color Integer")
                    else
                        try{
                            Integer.parseUnsignedInt(s, 16)
                            ValidationResult.success(s)
                        }catch (e: Exception){
                            ValidationResult.error(s,"String not parsable as color Integer: ${e.localizedMessage}")
                        }
                }
                .withCorrector()
                .both { s,_ ->
                    if(s.contains('#'))
                        ValidationResult.error(s.replace("#",""), "'#' prefixes not allowed")
                    else if(s.contains("0x"))
                        ValidationResult.error(s.replace("0x",""), "'0x' prefixes not allowed")
                    else if(s.length > 8)
                        ValidationResult.error(s.substring(0,8), "Too long. 8 characters maximum")
                    else if(s.length == 7 && isNotF(s[0]) && opaque)
                        ValidationResult.error(s.replaceRange(0,1,if(s[0].isLowerCase())"f" else "F"), "Opaque colors only.")
                    else if(s.length == 8 && (isNotF(s[0]) || isNotF(s[1])) && opaque)
                        ValidationResult.error(s.replaceRange(0,2,"${if(s[0].isLowerCase())"f" else "F"}${if(s[1].isLowerCase())"f" else "F"}"), "Opaque colors only.")
                    else
                        s.let { transform(it) }.let { if(it == s) ValidationResult.success(it) else ValidationResult.error(it,"Invalid characters found in color string") }
                    ValidationResult.success(s)
                }
                .build()
        }
    }

    data class ColorHolder(val r: Int, val g: Int, val b: Int, val a: Int, private val alphaMode: Boolean):
        EntryHandler<ColorHolder> {

        private val validator: Predicate<Int> = Predicate{i -> i in 0..255 }

        fun transparent(): Boolean{
            return alphaMode
        }
        fun opaque(): Boolean{
            return !alphaMode
        }

        fun toInt(): Int {
            return ColorHelper.Argb.getArgb(a,r,g,b)
        }
        fun fromInt(i: Int): ColorHolder{
            val components = Color(i,alphaMode)
            return this.copy(r = components.red, g = components.green, b = components.green, a = components.alpha)
        }

        fun instance(): ValidatedColor{
            return ValidatedColor(r,g,b,a,alphaMode)
        }

        override fun serializeEntry(
            input: ColorHolder?,
            errorBuilder: MutableList<String>,
            ignoreNonSync: Boolean
        ): TomlElement {
            val toml = TomlTableBuilder()
            try {
                toml.element("r", TomlLiteral(r), TomlComment("Red component, 0 to 255"))
                toml.element("g", TomlLiteral(g), TomlComment("Green component, 0 to 255"))
                toml.element("b", TomlLiteral(b), TomlComment("Blue component, 0 to 255"))
                if (alphaMode) toml.element("a", TomlLiteral(r), TomlComment("Alpha component, 0 to 255"))
            } catch (e: Exception){
                errorBuilder.add("Critical exception while serializing color: ${e.localizedMessage}")
            }
            return toml.build()
        }

        override fun deserializeEntry(
            toml: TomlElement,
            errorBuilder: MutableList<String>,
            fieldName: String,
            ignoreNonSync: Boolean
        ): ValidationResult<ColorHolder> {
            return try {
                val table = toml.asTomlTable()
                val tomlR = table["r"]?.asTomlLiteral()?.toInt() ?: return ValidationResult.error(this,"Error deserializing 'r' component of color [$fieldName], using previous value.")
                val tomlG = table["g"]?.asTomlLiteral()?.toInt() ?: return ValidationResult.error(this,"Error deserializing 'g' component of color [$fieldName], using previous value.")
                val tomlB = table["b"]?.asTomlLiteral()?.toInt() ?: return ValidationResult.error(this,"Error deserializing 'b' component of color [$fieldName], using previous value.")
                val tomlA = if(!alphaMode) 255 else table["a"]?.asTomlLiteral()?.toInt() ?: 255

                ValidationResult.success(ColorHolder(tomlR, tomlB, tomlG, tomlA, this.alphaMode))
            } catch (e: Exception){
                ValidationResult.error(this, "Critical error encountered deserializing color [$fieldName], using previous value.")
            }
        }

        override fun validateEntry(input: ColorHolder, type: EntryValidator.ValidationType): ValidationResult<ColorHolder> {
            val errors: MutableList<String> = mutableListOf()
            if (!validator.test(input.r)) errors.add("Red component out of bounds: ${input.r} outside 0-255")
            if (!validator.test(input.g)) errors.add("Green component out of bounds: ${input.g} outside 0-255")
            if (!validator.test(input.b)) errors.add("Blue component out of bounds: ${input.b} outside 0-255")
            if (input.alphaMode){
                if (!validator.test(input.a)) errors.add("Alpha component out of bounds: ${input.a} outside 0-255")
            } else {
                if (input.a != 255) errors.add("Non-transparent color with <255 Alpha")
            }
            return if (errors.isNotEmpty()){
                ValidationResult.error(input, "Errors validating color: $errors")
            } else {
                ValidationResult.success(input)
            }
        }

        override fun correctEntry(input: ColorHolder, type: EntryValidator.ValidationType): ValidationResult<ColorHolder> {
            val errors: MutableList<String> = mutableListOf()
            if (!validator.test(input.r)) errors.add("Red component out of bounds: ${input.r} outside 0-255")
            if (!validator.test(input.g)) errors.add("Green component out of bounds: ${input.g} outside 0-255")
            if (!validator.test(input.b)) errors.add("Blue component out of bounds: ${input.b} outside 0-255")
            if (input.alphaMode){
                if (!validator.test(input.a)) errors.add("Alpha component out of bounds: ${input.a} outside 0-255")
            } else {
                if (input.a != 255) errors.add("Non-transparent color with <255 Alpha")
            }

            return if (errors.isNotEmpty()){
                val newR = MathHelper.clamp(input.r,0,255)
                val newG = MathHelper.clamp(input.g,0,255)
                val newB = MathHelper.clamp(input.b,0,255)
                val newA = if(input.alphaMode) MathHelper.clamp(input.r,0,255) else 255
                val newColorHolder = ColorHolder(newR, newG, newB, newA, input.alphaMode)
                ValidationResult.error(newColorHolder, "Errors validating color, corrected to $newColorHolder: $errors")
            } else {
                ValidationResult.success(input)
            }
        }

        override fun toString(): String {
            return "[r=$r,g=$g,b=$b,a=$a]"
        }
    }

    @Environment(EnvType.CLIENT)
    class HBMapWidget(private val colorSupplier: Supplier<ColorHolder>): ClickableWidget(0,0,68,60,"fc.validated_field.color.hl".translate()){
        override fun renderWidget(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
            TODO("Not yet implemented")
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            builder.put(NarrationPart.TITLE, this.narrationMessage)
            if (active) {
                if (this.isFocused) {
                    builder.put(NarrationPart.USAGE, Text.translatable("narration.button.usage.focused") as Text)
                } else {
                    builder.put(NarrationPart.USAGE, Text.translatable("narration.button.usage.hovered") as Text)
                }
            }
        }

    }

}