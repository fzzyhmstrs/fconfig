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

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.context.ContextType.Relevant
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.TriState
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.MathHelper
import net.peanuuutz.tomlkt.*
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.glfw.GLFW

/**
 * A validated [FzzyKeybind], which can be used for any user context input (not just keybinds, but it was a convenient name for this validation). Constructing this validation does not automatically register a context type. If you want to use this in built-int context handling, be sure to use [ContextType.create]
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Keybinds) for more details and examples.
 * @param defaultValue [FzzyKeybind] used as the default for this keybind
 * @author fzzyhmstrs
 * @since 0.6.5
 */
open class ValidatedKeybind @JvmOverloads constructor(defaultValue: FzzyKeybind, private val widgetType: WidgetType = WidgetType.STACKED): ValidatedField<FzzyKeybind>(defaultValue), Relevant {

    @JvmOverloads
    constructor(keyCode: Int, widgetType: WidgetType = WidgetType.STACKED): this(FzzyKeybindSimple(keyCode, TriState.DEFAULT, TriState.DEFAULT, TriState.DEFAULT), widgetType)

    @JvmOverloads
    constructor(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean, widgetType: WidgetType = WidgetType.STACKED): this(FzzyKeybindSimple(keyCode, ctrl, shift, alt), widgetType)

    private val modifierHandler = ValidatedTriState(TriState.DEFAULT)

    @Internal
    @Suppress("DEPRECATION")
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<FzzyKeybind> {
        return try {
            if (toml is TomlTable) {
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
                ValidationResult.predicated(
                    FzzyKeybindSimple(keyResult, ctrlResult.get(), shiftResult.get(), altResult.get()),
                    errors.isEmpty(),
                    "Errors encountered while deserializing simple keybind [$fieldName]: $errors"
                )
            } else if (toml is TomlArray) {
                val kbs: MutableList<FzzyKeybind> = mutableListOf()
                val errors: MutableList<String> = mutableListOf()
                for ((index, el) in toml.asTomlArray().withIndex()) {
                    kbs.add(deserialize(el, "fieldName$index").report(errors).get())
                }
                ValidationResult.predicated(FzzyKeybindCompound(kbs), errors.isEmpty(), "Errors encountered while deserializing compound keybind [$fieldName]: $errors")
            } else if (toml is TomlLiteral && toml.toString().lowercase() == "unbound") {
                ValidationResult.success(FzzyKeybindUnbound)
            } else {
                ValidationResult.error(storedValue, "Error in TOML representation of Keybind $fieldName. Excepted table or string value 'unbound'")
            }
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error deserializing Keybind [$fieldName]: ${e.localizedMessage}")
        }
    }

    @Internal
    @Suppress("DEPRECATION")
    override fun serialize(input: FzzyKeybind): ValidationResult<TomlElement> {
        when (input) {
            is FzzyKeybindSimple -> {
                val table = TomlTableBuilder(4)
                val errors: MutableList<String> = mutableListOf()
                table.element("ctrl", modifierHandler.serializeEntry(input.ctrl, errors, 1))
                table.element("shift", modifierHandler.serializeEntry(input.shift, errors, 1))
                table.element("alt", modifierHandler.serializeEntry(input.alt, errors, 1))
                table.element("key", TomlLiteral(input.inputCode))
                return ValidationResult.predicated(table.build(), errors.isEmpty(), "Errors encountered serializing simple keybind: $errors")
            }
            is FzzyKeybindCompound -> {
                val array = TomlArrayBuilder(input.keybinds.size)
                val errors: MutableList<String> = mutableListOf()
                for (kb in input.keybinds) {
                    array.element(serialize(kb).report(errors).get())
                }
                return ValidationResult.predicated(array.build(), errors.isEmpty(), "Errors encountered serializing compound keybind: $errors")
            }
            FzzyKeybindUnbound -> {
                return ValidationResult.success(TomlLiteral("unbound"))
            }
        }
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<FzzyKeybind>): ClickableWidget {
        return when (widgetType) {
            WidgetType.ONE_WIDGET -> {
                val layout = LayoutWidget(paddingW = 0, paddingH = 0, spacingW = 0, spacingH = 0)
                val keybindWidget = KeybindWidget()
                layout.add(
                    "textbox",
                    keybindWidget,
                    LayoutWidget.Position.LEFT,
                    LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY)
                layout.add(
                    "clear",
                    CustomButtonWidget.builder("fc.button.clear".translate()) {
                        keybindWidget.compounding = false
                        keybindWidget.resetting = false
                        this.accept(FzzyKeybindUnbound) }
                        .noMessage()
                        .size(11, 10)
                        .active(this.storedValue != FzzyKeybindUnbound)
                        .tooltip("fc.button.clear".translate())
                        .textures(TextureIds.KEYBIND_CLEAR, TextureIds.KEYBIND_CLEAR_DISABLED, TextureIds.KEYBIND_CLEAR_HIGHLIGHTED)
                        .build(),
                    LayoutWidget.Position.RIGHT,
                    LayoutWidget.Position.ALIGN_RIGHT,
                    LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
                layout.add(
                    "compound",
                    CustomButtonWidget.builder("fc.button.compound".translate()) {
                        keybindWidget.compounding = false
                        keybindWidget.resetting = false
                        this.accept(FzzyKeybindUnbound) }
                        .noMessage()
                        .size(11, 10)
                        .active(this.storedValue != FzzyKeybindUnbound)
                        .tooltip("fc.button.compound".translate())
                        .textures(TextureIds.KEYBIND_CLEAR, TextureIds.KEYBIND_CLEAR_DISABLED, TextureIds.KEYBIND_CLEAR_HIGHLIGHTED)
                        .build(),
                    LayoutWidget.Position.BELOW,
                    LayoutWidget.Position.ALIGN_RIGHT,
                    LayoutWidget.Position.VERTICAL_TO_LEFT_EDGE)
                LayoutClickableWidget(0, 0, 110, 20, layout)
            }
            WidgetType.STACKED -> {
                KeybindWidget()
            }
        }
    }

    /**
     * creates a deep copy of this ValidatedEnum
     * return ValidatedEnum wrapping a copy of the currently stored object and widget type
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedKeybind {
        return ValidatedKeybind(this.storedValue.clone())
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        return try {
            FzzyKeybind::class.java.isAssignableFrom(input::class.java) && validateEntry(input as FzzyKeybind, EntryValidator.ValidationType.STRONG).isValid()
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
        return "Validated Keybind[value=$storedValue]"
    }

    /**
     * Determines how the keybind will be displayed and handled
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    enum class WidgetType {
        /**
         * The keybind will be displayed and edited in one widget, even for compound (multiple-choice) keybinds. Compound keybinds will be added with the button or with shift-click, and clearing the keybind will clear the entire bind (not just that option)
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        ONE_WIDGET,
        /**
         * The two widgets will be stacked one on top of the other just like two settings in the normal setting list, but with only one setting title. Like a mini "group" of settings
         *
         * Labels will appear below each widget, so the total widget would be Widget 1 > Label 1 > Widget 2 > Label 2 stacked on top of each other
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        STACKED
    }

    /**
     * Determines which type of selector widget will be used for the TriState option, default is SIDE_BY_SIDE
     * @author fzzyhmstrs
     * @since 0.6.5
     */

    //client
    private inner class KeybindWidget: CustomPressableWidget(0, 0, 99, 20, this@ValidatedKeybind.storedValue.keybind()) {

        override val textures: TextureProvider = TextureSet("widget/text_field".simpleId(), "widget/text_field".simpleId(), "widget/text_field_highlighted".simpleId())

        var resetting = false
        var compounding = false

        override fun getMessage(): Text {
            return if (resetting) {
                if (compounding) {
                    FcText.translatable("fc.keybind.or", super.getMessage(), FcText.translatable("fc.keybind.selecting", FcText.literal("  ").formatted(Formatting.UNDERLINE)))
                } else {
                    FcText.translatable("fc.keybind.selecting", super.getMessage().copy().formatted(Formatting.UNDERLINE))
                }
            } else {
                this@ValidatedKeybind.storedValue.keybind()
            }
        }

        override fun setFocused(focused: Boolean) {
            super.setFocused(focused)
            if (!focused) {
                if (resetting) {
                    if (!compounding) {
                        this@ValidatedKeybind.accept(FzzyKeybindUnbound)
                    }
                }
                resetting = false
                compounding = false
                MinecraftClient.getInstance().currentScreen?.nullCast<ConfigScreen>()?.setGlobalInputHandler(null)
            }
        }

        override fun onPress() {
            resetting = true
            if (Screen.hasShiftDown() && this@ValidatedKeybind.widgetType == WidgetType.ONE_WIDGET && this@ValidatedKeybind.storedValue != FzzyKeybindUnbound) {
                compounding = true
            }
            setupHandler()
        }

        fun setupHandler() {
            MinecraftClient.getInstance().currentScreen?.nullCast<ConfigScreen>()?.setGlobalInputHandler { key, released, _, ctrl, shift, alt ->
                if (!released) {
                    return@setGlobalInputHandler TriState.DEFAULT
                }
                if (key == GLFW.GLFW_KEY_ESCAPE && !ctrl && !shift && !alt) {
                    if (!compounding) {
                        this@ValidatedKeybind.accept(FzzyKeybindUnbound)
                    }
                } else {
                    if (compounding) {
                        this@ValidatedKeybind.accept(this@ValidatedKeybind.get().compoundWith(FzzyKeybindSimple(key, ctrl, shift, alt)))
                    } else {
                        this@ValidatedKeybind.accept(FzzyKeybindSimple(key, ctrl, shift, alt))
                    }
                }
                resetting = false
                compounding = false
                MinecraftClient.getInstance().currentScreen?.nullCast<ConfigScreen>()?.setGlobalInputHandler(null)
                TriState.TRUE
            }
        }

        override fun getNarrationMessage(): MutableText {
            return FcText.translatable("fc.keybind.narrate", message)
        }
    }
}