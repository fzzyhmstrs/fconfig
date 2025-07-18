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

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.entry.EntryFlag
import me.fzzyhmstrs.fzzy_config.entry.EntryOpener
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.context.ContextAction
import me.fzzyhmstrs.fzzy_config.screen.context.ContextType
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.decoration.SpriteDecorated
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureSet
import me.fzzyhmstrs.fzzy_config.screen.widget.TooltipChild
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.isEmpty
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.*
import java.util.function.BooleanSupplier
import java.util.function.Function
import java.util.function.Supplier

/**
 * a validated value with a fallback that is supplied if provided conditions aren't passed
 * @param T the type being wrapped
 * @param delegate [ValidatedField] the field being wrapped with conditions
 * @param fallback [Supplier]&lt;[T]&gt; supplies fallback values if the condition fails
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.conditions
 * @throws IllegalStateException if the passed fallback is the same as the delegate
 * @author fzzyhmstrs
 * since 0.5.4
 */
open class ValidatedCondition<T> internal constructor(delegate: ValidatedField<T>, private val fallback: Supplier<T>): ValidatedMapped<T, T>(delegate, Function.identity(), Function.identity()) {

    init {
        if (delegate === fallback) throw IllegalStateException("Can't use the conditional delegate as it's own fallback")
    }

    internal var conditions: Vector<Condition> = Vector(2)

    private var singleFailText: Text? = null
    private var pluralFailText: Text? = null

    /**
     * creates a deep copy of this ValidatedCondition
     * @return ValidatedCondition wrapping a deep copy of the currently stored object and conditions
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    override fun instanceEntry(): ValidatedField<T> {
        val condition = ValidatedCondition(delegate.instanceEntry(), fallback)
        condition.conditions = conditions
        condition.singleFailText = singleFailText?.copy()
        condition.pluralFailText = pluralFailText?.copy()
        return condition
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input [T] input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: T): T {
        return delegate.copyValue(input)
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        return ConditionalActiveButtonWidget(
            110,
            20,
            { checkConditions() },
            { conditionFailMessages() },
            delegate.widgetEntry(choicePredicate))
    }

    @Internal
    override fun open(args: List<String>) {
        delegate.nullCast<EntryOpener>()?.let { if (checkConditions()) it.open(args) }
    }

    @Internal
    override fun setFlag(flag: EntryFlag.Flag) {
        setFlag(flag.flag)
    }

    @Internal
    override fun setFlag(flag: Byte) {
        delegate.setFlag(flag)
    }

    @Internal
    override fun hasFlag(flag: EntryFlag.Flag): Boolean {
        return delegate.hasFlag(flag)
    }

    /**
     * provide the stored value, gated behind the results of any provided conditions. If any condition fails, this will return false no matter the underlying value
     * @return the stored value if the conditions pass, the supplied fallback value otherwise
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    override fun get(): T {
        for (condition in conditions) {
            if (!condition.get()) return fallback.get()
        }
        return super.get()
    }

    /**
     * Retrieves the wrapped value without performing condition checks
     * @return stored value [T]
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    fun getUnconditional(): T {
        return super.get()
    }

    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun isDefault(): Boolean {
        for (condition in conditions) {
            if (!condition.get()) return true
        }
        @Suppress("DEPRECATION")
        return delegate.isDefault()
    }

    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun restore() {
        for (condition in conditions) {
            if (!condition.get()) return
        }
        reset()
        @Suppress("DEPRECATION")
        getUpdateManager()?.addUpdateMessage(this, FcText.translatable("fc.validated_field.default", translation(), defaultValue.toString()))
    }

    @Internal
    override fun entryDeco(): Decorated.DecoratedOffset? {
        return Decorated.DecoratedOffset(ConditionDecoration{ checkConditions() }, 2, 2)
    }

    @Internal
    override fun contextActionBuilder(context: EntryCreator.CreatorContext): MutableMap<String, MutableMap<ContextType, ContextAction.Builder>> {
        val map = super.contextActionBuilder(context)
        val map2 = map["entry"] ?: return map
        for ((_, builder) in map2) {
            builder.withActive { supplier -> Supplier{ supplier.get() && this.checkConditions() } }
        }
        return map
    }

    /**
     * Defines a custom condition fail-state title to appear in the de-activated widget. By default, (in english) these are "Condition not met" and "Conditions not met". A more specific message may be appropriate for user guidance, such as `"|setting| disabled"`
     * @param singleFailText [Text] a message for a single condition failure, or if the number of conditions failing doesn't matter
     * @param pluralFailText [Text] a message for when multiple conditions are not met. [singleFailText] will be used if this is null and the single is provided.
     * @return this condition
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    @JvmOverloads
    fun withFailTitle(singleFailText: Text, pluralFailText: Text? = null): ValidatedCondition<T> {
        this.singleFailText = singleFailText
        this.pluralFailText = pluralFailText
        return this
    }

    /**
     * Adds a conditional check to this [ValidatedCondition]. Apply conditional checks on top of the stored value by calling [get]. Get the base value with [getUnconditional].
     * @param condition [Condition] a condition to check before passing the stored value
     * @return this condition
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    open fun withCondition(condition: Condition): ValidatedCondition<T> {
        conditions.add(condition)
        return this
    }

    /**
     * Adds a conditional check to this [ValidatedCondition]. Apply conditional checks on top of the stored value by calling [get]. Get the base value with [getUnconditional].
     *
     * Note: a ValidatedField is a supplier. If you want a custom failMessage, this is a valid overload of `withCondition(ValidatedField<Boolean>)`
     * @param condition [Supplier]&lt;Boolean&gt; a supplier of booleans for the condition to check against
     * @param failMessage [Text] a message to provide to a tooltip if a condition isn't met
     * @return this condition
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    open fun withCondition(condition: Supplier<Boolean>, failMessage: Text): ValidatedCondition<T> {
        conditions.add(ConditionSupplierImpl(condition, failMessage))
        return this
    }

    /**
     * Adds a conditional check to this [ValidatedCondition]. Apply conditional checks on top of the stored value by calling [get]. Get the base value with [getUnconditional].
     *
     * Note: a ValidatedField is a supplier. If you want a custom failMessage, this is a valid overload of `withCondition(ValidatedField<Boolean>)`
     * @param failMessage [Text] a message to provide to a tooltip if a condition isn't met
     * @param condition [Supplier]&lt;Boolean&gt; a supplier of booleans for the condition to check against
     * @return this condition
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    open fun withCondition(failMessage: Text, condition: BooleanSupplier): ValidatedCondition<T> {
        conditions.add(ConditionBooleanSupplierImpl(condition, failMessage))
        return this
    }

    /**
     * Adds a conditional check to this [ValidatedCondition] using the provided validated field as a supplier. Apply conditional checks on top of the stored value by calling [get]. Get the base value with [getUnconditional].
     * @param condition [ValidatedField]&lt;Boolean&gt; a condition to check before passing the stored value
     * @throws IllegalStateException if this field is passed into itself
     * @return this condition
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    open fun withCondition(condition: ValidatedField<Boolean>): ValidatedCondition<T> {
        if (condition === this) throw IllegalStateException("Illegal looping condition")
        return withCondition(condition, condition.translation())
    }

    /**
     * Adds a conditional check to this [ValidatedCondition] using the provided scope against a boolean provider. Apply conditional checks on top of the stored value by calling [get]. Get the base value with [getUnconditional]. The provided scope must point to a valid boolean config scope otherwise the condition will never pass.
     * @param scope String - a config `scope` pointing to a boolean or validated boolean.
     * @param failMessage [Text] a message to provide to a tooltip if a condition isn't met
     * @return this condition
     * @author fzzyhmstrs
     * @since 0.5.4, explicitly uses BooleanSupplier internally 0.6.5
     */
    open fun withCondition(scope: String, failMessage: Text): ValidatedCondition<T> {
        return withCondition(failMessage) { booleanProvider.getResult(scope) }
    }

    /**
     * @suppress
     */
    @Deprecated("Will throw an error, this is already a condition")
    final override fun toCondition(condition: Condition, fallback: Supplier<T>): ValidatedCondition<T> {
        throw UnsupportedOperationException("Already a ValidatedCondition!")
    }

    /**
     * @suppress
     */
    @Deprecated("Will throw an error, this is already a condition")
    final override fun toCondition(condition: ValidatedField<Boolean>, fallback: Supplier<T>): ValidatedCondition<T> {
        throw UnsupportedOperationException("Already a ValidatedCondition!")
    }

    /**
     * @suppress
     */
    @Deprecated("Will throw an error, this is already a condition")
    final override fun toCondition(condition: Supplier<Boolean>, failMessage: Text, fallback: Supplier<T>): ValidatedCondition<T> {
        throw UnsupportedOperationException("Already a ValidatedCondition!")
    }

    /**
     * @suppress
     */
    @Deprecated("Will throw an error, this is already a condition")
    final override fun toCondition(scope: String, failMessage: Text, fallback: Supplier<T>): ValidatedCondition<T> {
        throw UnsupportedOperationException("Already a ValidatedCondition!")
    }

    private fun checkConditions(): Boolean {
        for (condition in conditions) {
            if (!condition.get()) return false
        }
        return true
    }

    private fun conditionFailMessages(): List<Text> {
        val list: MutableList<Text> = mutableListOf()
        for (condition in conditions) {
            if (!condition.get()) list.add(condition.failMessage())
        }
        return list
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Condition[delegate=$delegate, conditions=$conditions]"
    }

    private class ConditionDecoration(private val activeSupplier: Supplier<Boolean>): SpriteDecorated {

        override fun textures(): TextureProvider {
            return TextureSet.Single(TextureIds.ENTRY_ERROR)
        }

        override fun renderDecoration(context: DrawContext, x: Int, y: Int, delta: Float, enabled: Boolean, selected: Boolean) {
            if (!activeSupplier.get())
                super.renderDecoration(context, x, y, delta, enabled, selected)
        }
    }

    //client
    private inner class ConditionalActiveButtonWidget(
        width: Int,
        height: Int,
        activeSupplier: Supplier<Boolean>,
        private val conditionMessages: Supplier<List<Text>>,
        private val delegateWidget: ClickableWidget)
        :
        CustomButtonWidget(
            0,
            0,
            width,
            height,
            FcText.EMPTY,
            { _ -> },
            Companion.DEFAULT_ACTIVE_NARRATION_SUPPLIER,
            { _ -> },
            null)
    {

        init {
            this.messageSupplier = Supplier {
                if (conditionMessages.get().size == 1)
                    singleFailText ?: "fc.validated_field.condition".translate()
                else
                    pluralFailText ?: singleFailText ?: "fc.validated_field.conditions".translate()
            }
            this.activeSupplier = activeSupplier
        }

        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            this.active = activeSupplier.get()
            if (active) {
                super.setTooltip(delegateWidget.tooltip)
                delegateWidget.render(context, mouseX, mouseY, delta)
            } else {
                super.setTooltip(makeTooltip())
                super.renderWidget(context, mouseX, mouseY, delta)
            }
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            return if (active)
                delegateWidget.mouseClicked(mouseX, mouseY, button)
            else
                super.mouseClicked(mouseX, mouseY, button)
        }

        override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
            return if (active)
                delegateWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
            else
                super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        }

        override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
            return if(active)
                delegateWidget.mouseReleased(mouseX, mouseY, button)
            else
                super.mouseReleased(mouseX, mouseY, button)
        }

        override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
            return if (active)
                delegateWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
            else
                super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        }

        override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            return if (active)
                delegateWidget.keyPressed(keyCode, scanCode, modifiers)
            else
                super.keyPressed(keyCode, scanCode, modifiers)
        }

        override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            return if (active)
                delegateWidget.keyReleased(keyCode, scanCode, modifiers)
            else
                super.keyReleased(keyCode, scanCode, modifiers)
        }

        override fun setFocused(focused: Boolean) {
            super.setFocused(focused)
            delegateWidget.isFocused = focused
        }

        override fun isFocused(): Boolean {
            return if (active)
                delegateWidget.isFocused
            else
                super.isFocused()
        }

        override fun isHovered(): Boolean {
            return if (active)
                delegateWidget.isHovered
            else
                super.isHovered()
        }

        override fun isSelected(): Boolean {
            return if (active)
                delegateWidget.isSelected
            else
                super.isSelected()
        }

        override fun getType(): Selectable.SelectionType {
            return if (active)
                delegateWidget.type
            else
                super.getType()
        }

        override fun getTooltip(): Tooltip? {
            return if (active)
                delegateWidget.tooltip
            else
                super.getTooltip()
        }

        override fun setTooltip(tooltip: Tooltip?) {
            if (active) {
                println("A")
                delegateWidget.tooltip = tooltip
            } else {
                println("B")
                super.setTooltip(tooltip)
            }
        }

        override fun setX(x: Int) {
            super.setX(x)
            delegateWidget.x = x
        }

        override fun setY(y: Int) {
            super.setY(y)
            delegateWidget.y = y
        }

        override fun getWidth(): Int {
            return delegateWidget.width
        }

        override fun getHeight(): Int {
            return delegateWidget.height
        }
    
        override fun setWidth(width: Int) {
            delegateWidget.width = width
        }
    
        override fun setHeight(height: Int) {
            delegateWidget.height = height
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
            if (active) {
                delegateWidget.appendNarrations(builder)
            } else {
                builder?.put(NarrationPart.TITLE, this.narrationMessage)
                val messages = conditionMessages.get()
                builder?.put(NarrationPart.HINT, *messages.toTypedArray())
            }
        }

        private fun makeTooltip(): Tooltip? {
            if (active) return null
            val messages = conditionMessages.get()
            if (messages.isEmpty()) return null
            val text = FcText.toLinebreakText(messages)
            if (text.isEmpty()) return null
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
            return if ((!this.active && this@ValidatedCondition.conditions.isEmpty()) || !this.visible) {
                null
            } else {
                if (!this.isFocused) GuiNavigationPath.of(this) else null
            }
        }

        override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
            return if (active) {
                delegateWidget.nullCast<TooltipChild>()?.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused) ?: TooltipChild.EMPTY
            } else if (parentSelected) {
                val messages = conditionMessages.get()
                if (messages.isEmpty()) return TooltipChild.EMPTY
                return messages
            } else {
                TooltipChild.EMPTY
            }
        }
    }

    companion object {
        private val booleanProvider = ConfigApi.result().createResultProvider({ FC.LOGGER.error("Condition boolean provider failed to find a matching boolean scope"); false }, Boolean::class)
    }

    /**
     * A condition supplier with an attached message. The message should explain what is needed to pass the condition successfully
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    @FunctionalInterface
    @JvmDefaultWithoutCompatibility
    fun interface Condition: Supplier<Boolean> {

        /**
         * A message to provide if the condition fails. Ideally indicates what is needed to make the condition pass.
         * @author fzzyhmstrs
         * @since 0.5.4
         */
        fun failMessage(): Text {
            return FcText.literal("Condition failed")
        }
    }

    internal class ConditionSupplierImpl(private val condition: Supplier<Boolean>, private val failMessage: Text): Condition {

        override fun get(): Boolean {
            return condition.get()
        }

        override fun failMessage(): Text {
            return failMessage
        }
    }

    internal class ConditionBooleanSupplierImpl(private val condition: BooleanSupplier, private val failMessage: Text): Condition {

        override fun get(): Boolean {
            return condition.asBoolean
        }

        override fun failMessage(): Text {
            return failMessage
        }
    }
}
