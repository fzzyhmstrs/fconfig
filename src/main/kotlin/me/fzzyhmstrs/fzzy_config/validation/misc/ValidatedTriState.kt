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
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.*
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawNineSlice
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.TriState.*
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedTriState.WidgetType
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BooleanSupplier
import java.util.function.Supplier

/**
 * A validated [TriState] (True, False, or Default). This validation is itself a [TriStateProvider], so can directly act as the stored TriState without needing to call [get]. TriStateProvider is itself a [BooleanSupplier], so this validation can be passed directly into places that use one of those, such as [ValidatedField.toCondition] (default treated as false in that case)
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/TriStates) for more details and examples.
 * @param defaultValue Enum Constant used as the default for this setting
 * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
 * @author fzzyhmstrs
 * @since 0.6.5
 */
open class ValidatedTriState @JvmOverloads constructor(defaultValue: TriState, private val widgetType: WidgetType = WidgetType.SIDE_BY_SIDE): ValidatedField<TriState>(defaultValue), TriStateProvider {

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<TriState> {
        return try {
            val result = TriState.CODEC.parse(TomlOps.INSTANCE, toml)
            return ValidationResult.mapDataResult(result, storedValue)
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error deserializing TriState [$fieldName]: ${e.localizedMessage}")
        }
    }

    @Internal
    override fun serialize(input: TriState): ValidationResult<TomlElement> {
        val result = TriState.CODEC.encodeStart(TomlOps.INSTANCE, input)
        return ValidationResult.mapDataResult(result, TomlLiteral(input.asString()))
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<TriState>): ClickableWidget {
        return when(widgetType) {
            WidgetType.SIDE_BY_SIDE -> {
                val t = SideBySideWidget(
                    TriState.TRUE,
                    { c, x, y, w, h ->
                        c.drawTex(TextureIds.ENTRY_OK, x + (w / 2) - 10, y + (h / 2) - 10, 20, 20)
                    },
                    { c, x, y, w, h ->
                        c.drawTex(TextureIds.ENTRY_OK_DISABLED, x + (w / 2) - 10, y + (h / 2) - 10, 20, 20)
                    }
                )
                t.width = 37
                val d = SideBySideWidget(
                    TriState.DEFAULT
                )
                d.width = 36
                val f = SideBySideWidget(
                    TriState.FALSE,
                    { c, x, y, w, h ->
                        c.drawTex(TextureIds.ENTRY_NO, x + (w / 2) - 10, y + (h / 2) - 10, 20, 20)
                    },
                    { c, x, y, w, h ->
                        c.drawTex(TextureIds.ENTRY_NO_DISABLED, x + (w / 2) - 10, y + (h / 2) - 10, 20, 20)
                    }
                )
                f.width = 37
                val layout = LayoutWidget(paddingW = 0, spacingW = 0).clampWidth(110)
                layout.add("true", t, LayoutWidget.Position.ALIGN_LEFT)
                layout.add("default", d, LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
                layout.add("false", f, LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
                LayoutClickableWidget(0, 0, 110, 20, layout)
            }
            WidgetType.CYCLING -> CyclingOptionsWidget()
        }
    }

    /**
     * creates a deep copy of this ValidatedTriState
     * return ValidatedTriState wrapping the currently stored state and widget type
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun instanceEntry(): ValidatedTriState {
        return ValidatedTriState(this.defaultValue)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        return try {
            input::class.java == TriState::class.java && validateEntry(input as TriState, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Gets the value of the tri-state. Implemented from [BooleanSupplier].
     *
     * @return boolean state of the stored value
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun getAsBoolean(): Boolean {
        return storedValue.asBoolean
    }

    /**
     * Gets the value of the tri-state as a boxed, nullable boolean.
     *
     * @return `null` if [DEFAULT]; otherwise `true` if [TRUE] or `false` if [FALSE].
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun getBoxed(): Boolean? {
        return storedValue.getBoxed()
    }

    /**
     * Gets the value of this tri-state.
     * If the value is [DEFAULT] then use the supplied value.
     *
     * @param value the value to fall back to
     * @return the value of the tri-state or the supplied value if [DEFAULT].
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun orElse(value: Boolean): Boolean {
        return storedValue.orElse(value)
    }

    /**
     * Gets the value of this tri-state.
     * If the value is [DEFAULT] then use the supplied value.
     *
     * @param supplier the supplier used to get the value to fall back to
     * @return the value of the tri-state or the value of the supplier if the tri-state is [DEFAULT].
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun orElseGet(supplier: BooleanSupplier): Boolean {
        return storedValue.orElseGet(supplier)
    }

    /**
     * Gets the value of this tri-state.
     * If the value is [DEFAULT] then use the supplied value.
     *
     * @param supplier the supplier used to get the value to fall back to
     * @return the value of the tri-state or the value of the supplier if the tri-state is [DEFAULT].
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun orElseGet(supplier: Supplier<Boolean>): Boolean {
        return storedValue.orElseGet(supplier)
    }

    /**
     * Validates a provided boolean input against the current tri-state.
     * - [DEFAULT] will return true no matter what (no validation)
     * - [TRUE] will return true if the input is true
     * - [FALSE] will return true if the input is false
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun validate(input: Boolean): Boolean {
        return storedValue.validate(input)
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated TriState[value=$storedValue]"
    }

    /**
     * Determines which type of selector widget will be used for the TriState option, default is SIDE_BY_SIDE
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    enum class WidgetType {
        /**
         * Three sub-widgets arranged side-by-side [TriState.TRUE] then [TriState.DEFAULT] then [TriState.FALSE], represented by a green check widget, a blank widget, and a red X widget respectively.
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        SIDE_BY_SIDE,
        /**
         * A traditional MC cycling button widget, looping through the tri-state; [TriState.DEFAULT] then [TriState.TRUE] then [TriState.FALSE] etc., starting on the state set as the default value.
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        CYCLING
    }

    //client
    private inner class SideBySideWidget(private val state: TriState, private val enabledRender: (context: DrawContext, x: Int, y: Int, w: Int, h: Int) -> Unit = {_, _, _, _, _ -> }, private val disabledRender: (context: DrawContext, x: Int, y: Int, w: Int, h: Int) -> Unit = { _, _, _, _, _ -> }): CustomPressableWidget(0, 0, 110, 20, FcText.EMPTY) {

        init {
            tooltip = Tooltip.of(state.descLit(state.asString()))
        }

        override val textures: TextureProvider = TextureSet.Quad(tex, disabled, highlighted, "widget/button_disabled_highlighted".fcId())

        var choiceSelected = state == this@ValidatedTriState.get()

        override fun renderBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
            choiceSelected = state == this@ValidatedTriState.get()
            context.drawNineSlice(textures.get(choiceSelected, this.isSelected), x, y, width, height, this.alpha)
        }

        override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
            if (choiceSelected)
                enabledRender(context, x, y, width, height)
            else
                disabledRender(context, x, y, width, height)
        }

        override fun getNarrationMessage(): MutableText {
            val m = state.transLit(state.asString())
            return if (choiceSelected)
                FcText.translatable("fc.validated_field.choice_set.selected", m)
            else
                FcText.translatable("fc.validated_field.choice_set.deselected", m)

        }

        override fun onPress() {
            this@ValidatedTriState.accept(state)
        }
    }

    //client
    private inner class CyclingOptionsWidget: CustomPressableWidget(0, 0, 110, 20, this@ValidatedTriState.get().let { it.transLit(it.asString()) }) {

        init {
            this@ValidatedTriState.descLit("").takeIf { it.string != "" }?.let { tooltip = Tooltip.of(it) }
        }

        override fun getNarrationMessage(): MutableText {
            return this@ValidatedTriState.get().let { it.transLit(it.asString()) }
        }

        override fun onPress() {
            val newIndex = (TriState.entries.indexOf(this@ValidatedTriState.get()) + 1).takeIf { it < TriState.entries.size } ?: 0
            val newConst = TriState.entries[newIndex]
            message = newConst.let { it.transLit(it.asString()) }
            newConst.descLit("").takeIf { it.string != "" }?.also { tooltip = Tooltip.of(it) }
            this@ValidatedTriState.accept(newConst)
        }

    }
}