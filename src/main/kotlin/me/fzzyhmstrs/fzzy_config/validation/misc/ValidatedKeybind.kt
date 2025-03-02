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
import me.fzzyhmstrs.fzzy_config.util.TomlOps
import me.fzzyhmstrs.fzzy_config.util.TriState
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedPair.Tuple
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.KeyCodes
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.peanuuutz.tomlkt.*
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.glfw.GLFW
import java.util.function.Function
import java.util.function.UnaryOperator

/**
 * A validated [FzzyKeybind], which can be used for any user context input (not just keybinds, but it was a convenient name for this validation). Constructing this validation does not automatically register a context type. If you want to use this in built-int context handling, be sure to use [ContextType.create]. This validation itself implements [Relevant], so can be used in context type registration directly.
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Keybinds) for more details and examples.
 * @param defaultValue [FzzyKeybind] used as the default for this keybind
 * @author fzzyhmstrs
 * @since 0.6.5
 */
open class ValidatedKeybind(defaultValue: FzzyKeybind): ValidatedField<FzzyKeybind>(defaultValue), Relevant {

    /**
     * A validated [FzzyKeybind], which can be used for any user context input (not just keybinds, but it was a convenient name for this validation). Constructing this validation does not automatically register a context type. If you want to use this in built-int context handling, be sure to use [ContextType.create]. This validation itself implements [Relevant], so can be used in context type registration directly.
     *
     * Shorthand constructor for automatic validation. Will start with an unbound keybind.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    constructor(): this(FzzyKeybindUnbound)

    /**
     * A validated [FzzyKeybind], which can be used for any user context input (not just keybinds, but it was a convenient name for this validation). Constructing this validation does not automatically register a context type. If you want to use this in built-int context handling, be sure to use [ContextType.create]. This validation itself implements [Relevant], so can be used in context type registration directly.
     * @param keyCode Int keycode for the keybind, with no modifiers. using [GLFW] for selection of keys is recommended.
     * @param type [ContextInput] type for this keybind, mouse or keyboard
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    constructor(keyCode: Int, type: ContextInput): this(FzzyKeybindSimple(keyCode, type, TriState.DEFAULT, TriState.DEFAULT, TriState.DEFAULT))

    /**
     * A validated [FzzyKeybind], which can be used for any user context input (not just keybinds, but it was a convenient name for this validation). Constructing this validation does not automatically register a context type. If you want to use this in built-int context handling, be sure to use [ContextType.create]. This validation itself implements [Relevant], so can be used in context type registration directly.
     * @param keyCode Int keycode for the keybind, with no modifiers. using [GLFW] for selection of keys is recommended.
     * @param type [ContextInput] type for this keybind, mouse or keyboard
     * @param ctrl Whether the control/super key needs to be held down or not.
     * @param shift Whether the shift key needs to be held down or not.
     * @param alt Whether the alt key needs to be held down or not.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    constructor(keyCode: Int, type: ContextInput, ctrl: Boolean, shift: Boolean, alt: Boolean): this(FzzyKeybindSimple(keyCode, type, ctrl, shift, alt))

    /**
     * A validated [FzzyKeybind], which can be used for any user context input (not just keybinds, but it was a convenient name for this validation). Constructing this validation does not automatically register a context type. If you want to use this in built-int context handling, be sure to use [ContextType.create]. This validation itself implements [Relevant], so can be used in context type registration directly.
     *
     * This constructor builds a keybind from the FzzyKeybind builder itself, letting you easily make compound key inputs.
     * @param operator [UnaryOperator]&lt;[FzzyKeybind.Builder]&gt; - operator to apply keys to the provided empty builder. If you pass the builder back unchanged, the validation will be set with an unbound key.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    constructor(operator: UnaryOperator<FzzyKeybind.Builder>): this(operator.apply(FzzyKeybind.Builder()).build())

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
                val typeToml = table["type"] ?: TomlNull
                val keyToml = table["key"] ?: TomlNull
                val ctrlResult = modifierHandler.deserializeEntry(ctrlToml, errors, "$fieldName.ctrl", 1)
                val shiftResult = modifierHandler.deserializeEntry(shiftToml, errors, "$fieldName.shift", 1)
                val altResult = modifierHandler.deserializeEntry(altToml, errors, "$fieldName.alt", 1)
                val typeResult = ValidationResult.mapDataResult(ContextInput.CODEC.parse(TomlOps.INSTANCE, typeToml), ContextInput.KEYBOARD).report(errors)
                val keyResult = keyToml.asTomlLiteral().toInt()
                ValidationResult.predicated(
                    FzzyKeybindSimple(keyResult, typeResult.get(), ctrlResult.get(), shiftResult.get(), altResult.get()),
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
                table.element("type", ContextInput.CODEC.encodeStart(TomlOps.INSTANCE, input.type).mapOrElse(Function.identity()) { _ -> ContextInput.fallback() })
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
                .activeSupplier { this.get() != FzzyKeybindUnbound }
                .tooltip("fc.button.clear".translate())
                .textures(TextureIds.KEYBIND_CLEAR, TextureIds.KEYBIND_CLEAR_DISABLED, TextureIds.KEYBIND_CLEAR_HIGHLIGHTED)
                .build(),
            LayoutWidget.Position.RIGHT,
            LayoutWidget.Position.ALIGN_RIGHT,
            LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        layout.add(
            "compound",
            CustomButtonWidget.builder("fc.button.compound".translate()) {
                keybindWidget.compounding = true
                keybindWidget.resetting = true
                keybindWidget.justCLickedToggle = true
                keybindWidget.setupHandler() }
                .noMessage()
                .size(11, 10)
                .activeSupplier { this.get() != FzzyKeybindUnbound }
                .tooltip("fc.button.compound".translate())
                .textures(TextureIds.KEYBIND_ADD, TextureIds.KEYBIND_ADD_DISABLED, TextureIds.KEYBIND_ADD_HIGHLIGHTED)
                .build(),
            LayoutWidget.Position.BELOW,
            LayoutWidget.Position.ALIGN_RIGHT,
            LayoutWidget.Position.VERTICAL_TO_LEFT_EDGE)
        return LayoutClickableWidget(0, 0, 110, 20, layout)
    }

    /**
     * creates a deep copy of this ValidatedKeybind
     * @return ValidatedKeybind wrapping a copy of the currently stored keybind(s)
     * @author fzzyhmstrs
     * @since 0.6.5
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

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input [FzzyKeybind] input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun copyValue(input: FzzyKeybind): FzzyKeybind {
        return input.clone()
    }

    /**
     * Tests whether the provided user input is relevant to the current keybind.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
        return storedValue.relevant(inputCode, ctrl, shift, alt)
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Keybind[value=$storedValue]"
    }

    //client
    private inner class KeybindWidget: CustomPressableWidget(0, 0, 99, 20, this@ValidatedKeybind.get().keybind()) {

        override val textures: TextureProvider = TextureSet("widget/text_field".simpleId(), "widget/text_field".simpleId(), "widget/text_field_highlighted".simpleId())

        var resetting = false
        var compounding = false
        var justCLickedToggle = false
        var justClickedShift = false

        override fun getMessage(): Text {
            return if (resetting) {
                if (compounding) {
                    FcText.translatable("fc.keybind.or", this@ValidatedKeybind.get().keybind(), FcText.translatable("fc.keybind.resetting", FcText.literal("  ").formatted(Formatting.UNDERLINE)))
                } else {
                    FcText.translatable("fc.keybind.resetting", this@ValidatedKeybind.get().keybind().copy().formatted(Formatting.UNDERLINE))
                }
            } else {
                this@ValidatedKeybind.get().keybind()
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
            justCLickedToggle = true
            if (Screen.hasShiftDown() && this@ValidatedKeybind.storedValue != FzzyKeybindUnbound) {
                justClickedShift = true
                compounding = true
            }
            setupHandler()
        }

        fun setupHandler() {
            MinecraftClient.getInstance().currentScreen?.nullCast<ConfigScreen>()?.setGlobalInputHandler { key, released, type, ctrl, shift, alt ->
                if (!released || justCLickedToggle || justClickedShift) {
                    if (released && (key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT)) {
                        justClickedShift = false
                    }
                    if (released && (key == GLFW.GLFW_MOUSE_BUTTON_1 || KeyCodes.isToggle(key))) {
                        justCLickedToggle = false
                    }
                    return@setGlobalInputHandler TriState.FALSE
                }
                if (key == GLFW.GLFW_KEY_ESCAPE && !ctrl && !shift && !alt) {
                    if (!compounding) {
                        this@ValidatedKeybind.accept(FzzyKeybindUnbound)
                    }
                } else {
                    if (compounding) {
                        this@ValidatedKeybind.accept(this@ValidatedKeybind.get().compoundWith(FzzyKeybindSimple(key, type, ctrl, shift, alt)))
                    } else {
                        this@ValidatedKeybind.accept(FzzyKeybindSimple(key, type, ctrl, shift, alt))
                    }
                }
                resetting = false
                compounding = false
                MinecraftClient.getInstance().currentScreen?.nullCast<ConfigScreen>()?.setGlobalInputHandler(null)
                TriState.TRUE
            }
        }

        override fun getNarrationMessage(): MutableText {
            return if (resetting)
                FcText.translatable("fc.keybind.resetting.narrate", message)
            else
                FcText.translatable("fc.keybind.narrate", message)
        }
    }
}