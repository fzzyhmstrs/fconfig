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

import me.fzzyhmstrs.fzzy_config.screen.widget.ActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.isNotEmpty
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.tooltip.TooltipState
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toBoolean
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * a validated boolean value that accepts secondary conditions
 * @param defaultValue the default boolean state
 * @author fzzyhmstrs
 * since 0.5.4
 */
open class ValidatedCondition(defaultValue: Boolean): ValidatedBoolean(defaultValue) {

    /**
     * A validated boolean value that accepts secondary conditions, wth default 'true' value
     * @author fzzyhmstrs
     * since 0.5.4
     */
    constructor(): this(true)

    private var conditions: Vector<Condition>? = null

    /**
     * Adds a conditional check to this [ValidatedCondition]. Apply conditional checks on top of the stored value by calling `getConditionally()`
     * @param condition [Condition] a condition
     */
    override fun withCondition(condition: Condition): ValidatedCondition {
        if (conditions == null) {
            conditions = Vector(2)
        }
        conditions?.add(condition)
        return this
    }

    override fun withCondition(condition: Supplier<Boolean>, failMessage: Text): ValidatedCondition {
        if (conditions == null) {
            conditions = Vector(2)
        }
        conditions?.add(ConditionImpl(condition, failMessage))
        return this
    }

    private fun checkConditions(): Boolean {
        if (conditions != null) {
            for (condition in conditions!!) {
                if (!condition.get()) return false
            }
        }
        return true
    }

    private fun conditionFailMessages(): List<Text> {
        val list: MutableList<Text> = mutableListOf()
        if (conditions != null) {
            for (condition in conditions!!) {
                if (!condition.get()) list.add(condition.failMessage())
            }
        }
        return list
    }

    fun getConditionally(): Boolean {
        if (conditions != null) {
            for (condition in conditions!!) {
                if (!condition.get()) return false
            }
        }
        return get()
    }

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Boolean> {
        return try {
            ValidationResult.success(toml.asTomlLiteral().toBoolean())
        } catch (e: Exception) {
            ValidationResult.error(storedValue, "Critical error deserializing boolean [$fieldName]: ${e.localizedMessage}")
        }
    }
    @Internal
    override fun serialize(input: Boolean): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    /**
     * creates a deep copy of this ValidatedBoolean
     * return ValidatedBoolean wrapping the current boolean value
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedCondition {
        return ValidatedCondition(copyStoredValue())
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is Boolean
    }
    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<Boolean>): ClickableWidget {
        return BooleanActiveButtonWidget(
            {if(getConditionally()) "fc.validated_field.boolean.true".translate() else "fc.validated_field.boolean.false".translate()},
            110,
            20,
            { checkConditions() },
            { if(checkConditions()) setAndUpdate(!get()) },
            { conditionFailMessages() }
        )
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Boolean[value=$storedValue, validation=true or false]"
    }

    //client
    private inner class BooleanActiveButtonWidget(
        titleSupplier: Supplier<Text>,
        width: Int,
        height: Int,
        activeSupplier: Supplier<Boolean>,
        pressAction: Consumer<ActiveButtonWidget>,
        private val conditionMessages: Supplier<List<Text>>,
        background: Identifier? = null
    ) : ActiveButtonWidget(titleSupplier, width, height, activeSupplier, pressAction, background) {

        private val tt = TooltipState()

        override fun renderCustom(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            if (this@ValidatedCondition.conditions?.isNotEmpty() == true)
                tooltip = makeTooltip()
            super.renderCustom(context, mouseX, mouseY, delta)
        }

        private fun makeTooltip(): Tooltip? {
            if (active) return null
            val messages = conditionMessages.get()
            if (messages.isEmpty()) return null
            var text: MutableText? = null
            for (message in messages) {
                if (text == null) {
                    text = message.copy()
                } else {
                    text.append(FcText.literal("\n")).append(message)
                }
            }
            if (text == null) return null
            val desc = this@ValidatedCondition.description("")
            if (desc.isNotEmpty()) {
                text.append(FcText.literal("\n\n"))
                text.append(desc)
            }
            return Tooltip.of(text)
        }

        override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
            return (this.visible
                    && (mouseX >= x.toDouble()
                    ) && (mouseY >= y.toDouble()
                    ) && (mouseX < (this.x + this.width).toDouble()
                    ) && (mouseY < (this.y + this.height).toDouble()))
        }

        override fun getNavigationPath(navigation: GuiNavigation?): GuiNavigationPath? {
            return if ((!this.active && this@ValidatedCondition.conditions?.isEmpty() == true) || !this.visible) {
                null
            } else {
                if (!this.isFocused) GuiNavigationPath.of(this) else null
            }
        }
    }

    /**
     * A condition supplier with an attached message. The message should explain what is
     */
    @FunctionalInterface
    @JvmDefaultWithoutCompatibility
    fun interface Condition: Supplier<Boolean> {
        fun failMessage(): Text {
            return FcText.literal("Condition failed")
        }
    }

    internal class ConditionImpl(private val condition: Supplier<Boolean>, private val failMessage: Text): Condition {

        override fun get(): Boolean {
            return condition.get()
        }

        override fun failMessage(): Text {
            return failMessage
        }
    }
}