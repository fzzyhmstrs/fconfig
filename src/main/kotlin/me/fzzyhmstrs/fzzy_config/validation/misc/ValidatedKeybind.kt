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
import me.fzzyhmstrs.fzzy_config.screen.context.ContextType.Relevant
import me.fzzyhmstrs.fzzy_config.screen.context.ContextType.RelevantImpl
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.*
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawNineSlice
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedTriState.WidgetType
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.TomlNull
import net.peanuuutz.tomlkt.TomlTableBuilder
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.asTomlTable
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BooleanSupplier
import java.util.function.Supplier

/**
 * A validated [ContextType.RelevantImpl], which can be used for any user context input (not just keybinds, but it was a convenient name for this validation). Constructing this validation does not automatically register a context type. If you want to use this in built-int context handling, be sure to use [ContextType.create]
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Keybinds) for more details and examples.
 * @param defaultValue Enum Constant used as the default for this setting
 * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
 * @author fzzyhmstrs
 * @since 0.6.5
 */
open class ValidatedKeybind @JvmOverloads constructor(defaultValue: RelevantImpl): ValidatedField<RelevantImpl>(defaultValue), Relevant {

    
    constructor(keyCode: Int): this(RelevantImpl(keyCode, TriState.DEFAULT, TriState.DEFAULT, TriState.DEFAULT)

    
    constructor(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): this(RelevantImpl(keyCode, ctrl, shift, alt))

    private val modifierHandler = ValidatedTriState(TriState.DEFAULT)
  
    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<RelevantImpl> {
        return try {
            val table = toml.asTomlTable()
            val errors: MutableList<String> = mutableListOf()
            val ctrlToml = table["ctrl"] ?: TomlNull
            val shiftToml = table["shift"] ?: TomlNull
            val altToml = table["alt"] ?: TomlNull
            val keyToml = table["key"] ?: TomlNull
            val ctrlResult = modifierHandler.deserializeEntry(ctrlToml, errors, "$fieldName.ctrl", 1)
            val shiftResult = modifierHandler.deserializeEntry(shiftToml, errors, "$fieldName.shift", 1)
            val altResult = modifierHandler.deserializeEntry(altToml, errors, "$fieldName.alt", 1)
            val keyResult = keyToml.asTomlLiteral().toInt()
            ValidationResult.predicated(RelevantImpl(keyResult.get(), ctrlResult.get(), shiftResult.get(), altResult.get()), errors.isEmpty(), "Errors encountered while deserializing keybind [$fieldName]: $errors")
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error deserializing Keybind [$fieldName]: ${e.localizedMessage}")
        }
    }

    @Internal
    override fun serialize(input: RelevantImpl): ValidationResult<TomlElement> {
        val table = TomlTableBuilder(4)
        val errors: MutableList<String> = mutableListOf()
        builder.element("ctrl", modifierHandler.serializeEntry(input.ctrl, errors, 1))
        builder.element("shift", modifierHandler.serializeEntry(input.shift, errors, 1))
        builder.element("alt", modifierHandler.serializeEntry(input.alt, errors, 1))
        builder.element("key", TomlLiteral(input.inputCode))
        return ValidationResult.predicated(table.build(), errors.isEmpty(), "Errors encountered serializing pair: $errors")
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<RelevantImpl>): ClickableWidget {
        return TODO()
    }

    /**
     * creates a deep copy of this ValidatedEnum
     * return ValidatedEnum wrapping a copy of the currently stored object and widget type
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedKeybind {
        return ValidatedKeybind(this.storedValue.copy())
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        return try {
            input::class.java == RelevantImpl::class.java && validateEntry(input as RelevantImpl, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Throwable) {
            false
        }
    }

    override fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
        return storedValue.relevant(inputCode, ctrl, shift, alt)
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Keybind[key=${storedValue.inputCode}, ctrl=${storedValue.ctrl}, shift=${storedValue.shift}, alt=${storedValue.alt}]"
    }

    /**
     * Determines which type of selector widget will be used for the TriState option, default is SIDE_BY_SIDE
     * @author fzzyhmstrs
     * @since 0.6.5
     */

    //client
    private inner class CyclingOptionsWidget: CustomPressableWidget(0, 0, 110, 20, this@ValidatedKeybind.storedValue.keybind()) {

        override fun getNarrationMessage(): MutableText {
            return this@ValidatedTriState.get().let { it.transLit(it.asString()) }
        }

    @Internal
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!this.active || !this.visible) {
            return false
        } else {
            val c = Screen.hasControlDown()
            val s = Screen.hasShiftDown()
            val a = Screen.hasAltDown()
            return true
        } else {
            return false
        }
    }
    }
}
