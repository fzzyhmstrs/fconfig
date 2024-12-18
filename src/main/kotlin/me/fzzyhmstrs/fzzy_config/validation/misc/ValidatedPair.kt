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

import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.screen.widget.ActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toBoolean
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * A validated pair of values
 * @param A stored type of the left side of the tuple
 * @param B stored type of the right side of the tuple
 * @param defaultValue [Tuple] default pair of values
 * @param leftHandler [Entry]&lt;[A]&gt; handler for left side of the tuple
 * @param rightHandler [Entry]&lt;[B]&gt; handler for right side of the tuple
 * @see me.fzzyhmstrs.fzzy_config.validation.ValidatedField.pairWith
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.pairs
 * @author fzzyhmstrs
 * since 0.6.0
 */
open class ValidatedPair<A, B>(defaultValue: Tuple<A, B>, private val lefthandler: Entry<A, *>, private val rightHandler: Entry<B, *>): ValidatedField<Tuple<A, B>>(defaultValue) {

    /**
     * Convenience constructor for creating a pair of one type
     * @param C stored type of the both sides of the tuple
     * @param defaultValue [Tuple] default pair of values
     * @param handler [Entry]&lt;[C]&gt; handler for both sides of the tuple
     * @author fzzyhmstrs
     * since 0.6.0
     */
    constructor<C>(defaultValue: Tuple<C, C>, private val handler: Entry<C, *>): this(defaultValue, handler, handler)

    init {
        leftHandler.listenToEntry { e ->
            accept(
        }
    }
    
    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Boolean> {
        return try {
            val table = toml.asTomlTable()
            val errors: MutableList<String> = mutableListOf()
            val aToml = table.get("left") ?: TomlNull
            val bToml = table.get("right") ?: TomlNull
            val aResult = leftHandler.deserialize(toml, "$fieldName.left").report(errors)
            val bResult = rightHandler.deserialize(toml, "$fieldName.right").report(errors)
            ValidationResult.predicated(Tuple(aResult.get(), bResult.get()), errors.isEmpty(), "Errors encountered while deserializing pair [$fieldName]: $errors")
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error deserializing pair [$fieldName]: ${e.localizedMessage}")
        }
    }
    
    @Internal
    override fun serialize(input: Tuple<A, B>): ValidationResult<TomlElement> {
        val builder = TomlTableBuilder()
        val errors: MutableList<String> = mutableListOf()
        val aElement = leftHandler.serialize(input.left).report(errors)
        val bElement = rightHandler.serialize(input.right).report(errors)
        builder.element("left", aElement.get())
        builder.element("right", bElement.get())
        return ValidationResult.predicated(builder.build(), errors.isEmpty(), "Errors encountered serializing pair: $errors")
    }

    /**
     * creates a deep copy of this ValidatedBoolean
     * return ValidatedBoolean wrapping the current boolean value
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedBoolean {
        return ValidatedBoolean(copyStoredValue())
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is Boolean
    }
    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<Boolean>): ClickableWidget {
        return ActiveButtonWidget(
            { if(get()) "fc.validated_field.boolean.true".translate() else "fc.validated_field.boolean.false".translate() },
            110,
            20,
            { true },
            { setAndUpdate(!get()) }
        )
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Boolean[value=$storedValue, validation=true or false]"
    }

    data class Tuple<X, Y>(val left: X, val right: Y) {
        fun withLeft(newLeft: X): Tuple<X, Y> {
            return Tuple(newLeft, right)
        }

        fun withRight(newRight: Y): Tuple<X, Y> {
            return Tuple(left, newRight)
        }
    }
}
