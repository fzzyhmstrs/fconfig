package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import me.fzzyhmstrs.fzzy_config.validation.entry.EntryHandler
import me.fzzyhmstrs.fzzy_config.validation.entry.EntryValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.util.math.MathHelper
import net.peanuuutz.tomlkt.*
import java.util.function.Predicate

class ValidatedColor: ValidatedField<ValidatedColor.ColorHolder> {

    constructor(r: Int, g: Int, b: Int, a: Int = Int.MIN_VALUE): super(ColorHolder(r, g, b, if(a > Int.MIN_VALUE) a else 255, a > Int.MIN_VALUE)){
        if(r<0 || r>255) throw IllegalArgumentException("Red portion of validated color not provided a default value between 0 and 255")
        if(g<0 || g>255) throw IllegalArgumentException("Green portion of validated color not provided a default value between 0 and 255")
        if(b<0 || b>255) throw IllegalArgumentException("Blue portion of validated color not provided a default value between 0 and 255")
        if((a<0 && a!=Int.MIN_VALUE) || a>255) throw IllegalArgumentException("Transparency portion of validated color not provided a default value between 0 and 255")
    }
    private constructor(r: Int, g: Int, b: Int, a: Int, alphaMode: Boolean): super(ColorHolder(r, g, b, a, alphaMode))

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

    override fun instanceEntry(): Entry<ColorHolder> {
        return defaultValue.instance()
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }

    data class ColorHolder(val r: Int, val g: Int, val b: Int, val a: Int, private val alphaMode: Boolean):
        EntryHandler<ColorHolder> {

        private val validator: Predicate<Int> = Predicate{i -> i in 0..255 }

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
}