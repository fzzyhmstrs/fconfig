/*
* Copyright (c) 2025 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.context.ContextType.Relevant
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.TomlOps
import me.fzzyhmstrs.fzzy_config.util.TriState
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
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
import java.lang.ref.SoftReference
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
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<FzzyKeybind> {
        return try {
            if (toml is TomlTable) {
                val table = toml.asTomlTable()
                val errors = ValidationResult.createMutable("Error(s) found deserializing simple keybind [$fieldName]")
                val ctrlToml = table["ctrl"] ?: TomlNull
                val shiftToml = table["shift"] ?: TomlNull
                val altToml = table["alt"] ?: TomlNull
                val typeToml = table["type"] ?: TomlNull
                val keyToml = table["key"] ?: TomlNull
                val ctrlResult = modifierHandler.deserializeEntry(ctrlToml, "$fieldName.ctrl", 1).attachTo(errors)
                val shiftResult = modifierHandler.deserializeEntry(shiftToml, "$fieldName.shift", 1).attachTo(errors)
                val altResult = modifierHandler.deserializeEntry(altToml, "$fieldName.alt", 1).attachTo(errors)
                val typeResult = ValidationResult.mapDataResult(ContextInput.CODEC.parse(TomlOps.INSTANCE, typeToml), ContextInput.KEYBOARD).attachTo(errors)
                val keyResult = deserializeKey(keyToml).attachTo(errors)
                ValidationResult.ofMutable(
                    FzzyKeybindSimple(keyResult.get(), typeResult.get(), ctrlResult.get(), shiftResult.get(), altResult.get()),
                    errors)
            } else if (toml is TomlArray) {
                val kbs: MutableList<FzzyKeybind> = mutableListOf()
                val errors = ValidationResult.createMutable("Error(s) found deserializing compound keybind [$fieldName]")
                for ((index, el) in toml.asTomlArray().withIndex()) {
                    kbs.add(deserialize(el, "fieldName @index:$index").attachTo(errors).get())
                }
                ValidationResult.ofMutable(FzzyKeybindCompound(kbs), errors)
            } else if (toml is TomlLiteral) {
                if (toml.toString().lowercase() == "unbound") {
                    ValidationResult.success(FzzyKeybindUnbound)
                } else {
                    deserializeKeyTyped(toml).map { FzzyKeybindSimple(it.second, it.first, ctrl = false, shift = false, alt = false) }
                }
            } else {
                ValidationResult.error(storedValue, ValidationResult.Errors.INVALID, "Invalid TOML representation of Keybind $fieldName. Expected keybind table, list of keybinds, integer or keycode matching a keybind, or 'unbound'")
            }
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, ValidationResult.Errors.DESERIALIZATION, "Exception deserializing Keybind [$fieldName]", e)
        }
    }

    @Internal
    override fun serialize(input: FzzyKeybind): ValidationResult<TomlElement> {
        when (input) {
            is FzzyKeybindSimple -> {
                val table = TomlTableBuilder(4)
                val errors = ValidationResult.createMutable("Errors encountered serializing simple keybind")
                table.element("ctrl", modifierHandler.serializeEntry(input.ctrl, 1).attachTo(errors).get())
                table.element("shift", modifierHandler.serializeEntry(input.shift, 1).attachTo(errors).get())
                table.element("alt", modifierHandler.serializeEntry(input.alt, 1).attachTo(errors).get())
                table.element("type", ContextInput.CODEC.encodeStart(TomlOps.INSTANCE, input.type).mapOrElse(Function.identity()) { _ -> ContextInput.fallback() }, TomlComment("'keyboard' or 'mouse'"))
                table.element("key", serialize(input.inputCode), TomlComment("""
                    |String representation of the key, or the integer keycode
                    |Convert minecraft names: 'key.keyboard.pause' -> 'pause' or 'key.mouse.right' -> 'mouse.right'.
                    """.trimMargin()))
                return ValidationResult.ofMutable(table.build(), errors)
            }
            is FzzyKeybindCompound -> {
                val array = TomlArrayBuilder(input.keybinds.size)
                val errors = ValidationResult.createMutable("Errors encountered serializing compound keybind")
                for (kb in input.keybinds) {
                    array.element(serialize(kb).attachTo(errors).get())
                }
                return ValidationResult.ofMutable(array.build(), errors)
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
            CustomButtonWidget.builder(TextureIds.KEYBIND_CLEAR_LANG) {
                keybindWidget.compounding = false
                keybindWidget.resetting = false
                this.accept(FzzyKeybindUnbound) }
                .noMessage()
                .size(11, 10)
                .activeSupplier { this.get() != FzzyKeybindUnbound }
                .tooltip(TextureIds.KEYBIND_CLEAR_LANG)
                .textures(TextureIds.KEYBIND_CLEAR, TextureIds.KEYBIND_CLEAR_DISABLED, TextureIds.KEYBIND_CLEAR_HIGHLIGHTED)
                .build(),
            LayoutWidget.Position.RIGHT,
            LayoutWidget.Position.ALIGN_RIGHT,
            LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        layout.add(
            "compound",
            CustomButtonWidget.builder(TextureIds.KEYBIND_ADD_LANG) {
                keybindWidget.compounding = true
                keybindWidget.resetting = true
                keybindWidget.justCLickedToggle = true
                keybindWidget.setupHandler() }
                .noMessage()
                .size(11, 10)
                .activeSupplier { this.get() != FzzyKeybindUnbound }
                .tooltip(TextureIds.KEYBIND_ADD_LANG)
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

        override val textures: TextureProvider = TextureSet("widget/text_field".fcId(), "widget/text_field".fcId(), "widget/text_field_highlighted".fcId())

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

    companion object {
        private var key2int: SoftReference<Map<String, Int>> = SoftReference(mapOf())
        private var int2key: SoftReference<Map<Int, String>> = SoftReference(mapOf())

        private fun serialize(int: Int): TomlElement {
            var int2key = int2key.get()
            if (int2key.isNullOrEmpty()) {
                int2key = initInt2Key()
            }
            val key = int2key[int]
            return if (key == null) {
                TomlLiteral(int)
            } else {
                TomlLiteral(key)
            }
        }

        private fun deserializeKey(element: TomlElement): ValidationResult<Int> {
            var key2Int = key2int.get()
            if (key2Int.isNullOrEmpty()) {
                key2Int = initKey2Int()
            }
            if (element !is TomlLiteral) {
                return ValidationResult.error(-1, ValidationResult.Errors.DESERIALIZATION, "Keybind toml element not a TomlLiteral")
            }
            return when (element.type) {
                TomlLiteral.Type.String -> {
                    val key = element.toString().lowercase()
                    val int = key2Int[key] ?: -1
                    ValidationResult.predicated(int, int != -1, ValidationResult.Errors.INVALID) { b -> b.content("String key [$key] not valid") }
                }
                TomlLiteral.Type.Integer -> {
                    val int = element.toIntOrNull() ?: -1
                    ValidationResult.predicated(int, int != -1, ValidationResult.Errors.INVALID) { b -> b.content("Int key [$element] not valid") }
                }
                else -> {
                    return ValidationResult.error(-1, ValidationResult.Errors.INVALID, "Keybind element invalid")
                }
            }
        }

        private fun deserializeKeyTyped(element: TomlElement): ValidationResult<Pair<ContextInput, Int>> {
            var key2Int = key2int.get()
            if (key2Int.isNullOrEmpty()) {
                key2Int = initKey2Int()
            }
            if (element !is TomlLiteral) {
                return ValidationResult.error(Pair(ContextInput.KEYBOARD, -1), ValidationResult.Errors.DESERIALIZATION, "Keybind toml element not a TomlLiteral")
            }
            return when (element.type) {
                TomlLiteral.Type.String -> {
                    val key = element.toString().lowercase()
                    val int = key2Int[key] ?: -1
                    val type = if (int in 0..7) ContextInput.MOUSE else ContextInput.KEYBOARD
                    ValidationResult.predicated(Pair(type, int), int != -1, ValidationResult.Errors.INVALID) { b -> b.content("String key [$key] not valid") }
                }
                TomlLiteral.Type.Integer -> {
                    val int = element.toIntOrNull() ?: -1
                    val type = if (int in 0..7) ContextInput.MOUSE else ContextInput.KEYBOARD
                    ValidationResult.predicated(Pair(type, int), int != -1, ValidationResult.Errors.INVALID) { b -> b.content("Int key [$element] not valid") }
                }
                else -> {
                    return ValidationResult.error(Pair(ContextInput.KEYBOARD, -1), ValidationResult.Errors.INVALID, "Keybind element invalid")
                }
            }
        }

        private fun initInt2Key(): Map<Int, String> {
            val m = mapOf(
                GLFW.GLFW_MOUSE_BUTTON_LEFT   to "mouse.left",
                GLFW.GLFW_MOUSE_BUTTON_RIGHT  to "mouse.right",
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE to "mouse.middle",
                GLFW.GLFW_MOUSE_BUTTON_4      to "mouse.4",
                GLFW.GLFW_MOUSE_BUTTON_5      to "mouse.5",
                GLFW.GLFW_MOUSE_BUTTON_6      to "mouse.6",
                GLFW.GLFW_MOUSE_BUTTON_7      to "mouse.7",
                GLFW.GLFW_MOUSE_BUTTON_8      to "mouse.8",
                GLFW.GLFW_KEY_0               to "0",
                GLFW.GLFW_KEY_1               to "1",
                GLFW.GLFW_KEY_2               to "2",
                GLFW.GLFW_KEY_3               to "3",
                GLFW.GLFW_KEY_4               to "4",
                GLFW.GLFW_KEY_5               to "5",
                GLFW.GLFW_KEY_6               to "6",
                GLFW.GLFW_KEY_7               to "7",
                GLFW.GLFW_KEY_8               to "8",
                GLFW.GLFW_KEY_9               to "9",
                GLFW.GLFW_KEY_A               to "a",
                GLFW.GLFW_KEY_B               to "b",
                GLFW.GLFW_KEY_C               to "c",
                GLFW.GLFW_KEY_D               to "d",
                GLFW.GLFW_KEY_E               to "e",
                GLFW.GLFW_KEY_F               to "f",
                GLFW.GLFW_KEY_G               to "g",
                GLFW.GLFW_KEY_H               to "h",
                GLFW.GLFW_KEY_I               to "i",
                GLFW.GLFW_KEY_J               to "j",
                GLFW.GLFW_KEY_K               to "k",
                GLFW.GLFW_KEY_L               to "l",
                GLFW.GLFW_KEY_M               to "m",
                GLFW.GLFW_KEY_N               to "n",
                GLFW.GLFW_KEY_O               to "o",
                GLFW.GLFW_KEY_P               to "p",
                GLFW.GLFW_KEY_Q               to "q",
                GLFW.GLFW_KEY_R               to "r",
                GLFW.GLFW_KEY_S               to "s",
                GLFW.GLFW_KEY_T               to "t",
                GLFW.GLFW_KEY_U               to "u",
                GLFW.GLFW_KEY_V               to "v",
                GLFW.GLFW_KEY_W               to "w",
                GLFW.GLFW_KEY_X               to "x",
                GLFW.GLFW_KEY_Y               to "y",
                GLFW.GLFW_KEY_Z               to "z",
                GLFW.GLFW_KEY_F1              to "f1",
                GLFW.GLFW_KEY_F2              to "f2",
                GLFW.GLFW_KEY_F3              to "f3",
                GLFW.GLFW_KEY_F4              to "f4",
                GLFW.GLFW_KEY_F5              to "f5",
                GLFW.GLFW_KEY_F6              to "f6",
                GLFW.GLFW_KEY_F7              to "f7",
                GLFW.GLFW_KEY_F8              to "f8",
                GLFW.GLFW_KEY_F9              to "f9",
                GLFW.GLFW_KEY_F10             to "f10",
                GLFW.GLFW_KEY_F11             to "f11",
                GLFW.GLFW_KEY_F12             to "f12",
                GLFW.GLFW_KEY_F13             to "f13",
                GLFW.GLFW_KEY_F14             to "f14",
                GLFW.GLFW_KEY_F15             to "f15",
                GLFW.GLFW_KEY_F16             to "f16",
                GLFW.GLFW_KEY_F17             to "f17",
                GLFW.GLFW_KEY_F18             to "f18",
                GLFW.GLFW_KEY_F19             to "f19",
                GLFW.GLFW_KEY_F20             to "f20",
                GLFW.GLFW_KEY_F21             to "f21",
                GLFW.GLFW_KEY_F22             to "f22",
                GLFW.GLFW_KEY_F23             to "f23",
                GLFW.GLFW_KEY_F24             to "f24",
                GLFW.GLFW_KEY_F25             to "f25",
                GLFW.GLFW_KEY_NUM_LOCK        to "num.lock",
                GLFW.GLFW_KEY_KP_0            to "keypad.0",
                GLFW.GLFW_KEY_KP_1            to "keypad.1",
                GLFW.GLFW_KEY_KP_2            to "keypad.2",
                GLFW.GLFW_KEY_KP_3            to "keypad.3",
                GLFW.GLFW_KEY_KP_4            to "keypad.4",
                GLFW.GLFW_KEY_KP_5            to "keypad.5",
                GLFW.GLFW_KEY_KP_6            to "keypad.6",
                GLFW.GLFW_KEY_KP_7            to "keypad.7",
                GLFW.GLFW_KEY_KP_8            to "keypad.8",
                GLFW.GLFW_KEY_KP_9            to "keypad.9",
                GLFW.GLFW_KEY_KP_ADD          to "keypad.add",
                GLFW.GLFW_KEY_KP_DECIMAL      to "keypad.decimal",
                GLFW.GLFW_KEY_KP_ENTER        to "keypad.enter",
                GLFW.GLFW_KEY_KP_EQUAL        to "keypad.equal",
                GLFW.GLFW_KEY_KP_MULTIPLY     to "keypad.multiply",
                GLFW.GLFW_KEY_KP_DIVIDE       to "keypad.divide",
                GLFW.GLFW_KEY_KP_SUBTRACT     to "keypad.subtract",
                GLFW.GLFW_KEY_DOWN            to "down",
                GLFW.GLFW_KEY_LEFT            to "left",
                GLFW.GLFW_KEY_RIGHT           to "right",
                GLFW.GLFW_KEY_UP              to "up",
                GLFW.GLFW_KEY_APOSTROPHE      to "apostrophe",
                GLFW.GLFW_KEY_BACKSLASH       to "backslash",
                GLFW.GLFW_KEY_COMMA           to "comma",
                GLFW.GLFW_KEY_EQUAL           to "equal",
                GLFW.GLFW_KEY_GRAVE_ACCENT    to "grave.accent",
                GLFW.GLFW_KEY_LEFT_BRACKET    to "left.bracket",
                GLFW.GLFW_KEY_MINUS           to "minus",
                GLFW.GLFW_KEY_PERIOD          to "period",
                GLFW.GLFW_KEY_RIGHT_BRACKET   to "right.bracket",
                GLFW.GLFW_KEY_SEMICOLON       to "semicolon",
                GLFW.GLFW_KEY_SLASH           to "slash",
                GLFW.GLFW_KEY_SPACE           to "space",
                GLFW.GLFW_KEY_TAB             to "tab",
                GLFW.GLFW_KEY_LEFT_ALT        to "left.alt",
                GLFW.GLFW_KEY_LEFT_CONTROL    to "left.control",
                GLFW.GLFW_KEY_LEFT_SHIFT      to "left.shift",
                GLFW.GLFW_KEY_LEFT_SUPER      to "left.win",
                GLFW.GLFW_KEY_RIGHT_ALT       to "right.alt",
                GLFW.GLFW_KEY_RIGHT_CONTROL   to "right.control",
                GLFW.GLFW_KEY_RIGHT_SHIFT     to "right.shift",
                GLFW.GLFW_KEY_RIGHT_SUPER     to "right.win",
                GLFW.GLFW_KEY_ENTER           to "enter",
                GLFW.GLFW_KEY_ESCAPE          to "escape",
                GLFW.GLFW_KEY_BACKSPACE       to "backspace",
                GLFW.GLFW_KEY_DELETE          to "delete",
                GLFW.GLFW_KEY_END             to "end",
                GLFW.GLFW_KEY_HOME            to "home",
                GLFW.GLFW_KEY_INSERT          to "insert",
                GLFW.GLFW_KEY_PAGE_DOWN       to "page.down",
                GLFW.GLFW_KEY_PAGE_UP         to "page.up",
                GLFW.GLFW_KEY_CAPS_LOCK       to "caps.lock",
                GLFW.GLFW_KEY_PAUSE           to "pause",
                GLFW.GLFW_KEY_SCROLL_LOCK     to "scroll.lock",
                GLFW.GLFW_KEY_MENU            to "menu",
                GLFW.GLFW_KEY_PRINT_SCREEN    to "print.screen",
                GLFW.GLFW_KEY_WORLD_1         to "world.1",
                GLFW.GLFW_KEY_WORLD_2         to "world.2"
            )
            int2key = SoftReference(m)
            return m
        }

        private fun initKey2Int(): Map<String, Int> {
            val m = mapOf(
                "mouse.left"      to GLFW.GLFW_MOUSE_BUTTON_LEFT,
                "mouse.right"     to GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                "mouse.middle"    to GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                "mouse.4"         to GLFW.GLFW_MOUSE_BUTTON_4,
                "mouse.5"         to GLFW.GLFW_MOUSE_BUTTON_5,
                "mouse.6"         to GLFW.GLFW_MOUSE_BUTTON_6,
                "mouse.7"         to GLFW.GLFW_MOUSE_BUTTON_7,
                "mouse.8"         to GLFW.GLFW_MOUSE_BUTTON_8,
                "0"               to GLFW.GLFW_KEY_0,
                "1"               to GLFW.GLFW_KEY_1,
                "2"               to GLFW.GLFW_KEY_2,
                "3"               to GLFW.GLFW_KEY_3,
                "4"               to GLFW.GLFW_KEY_4,
                "5"               to GLFW.GLFW_KEY_5,
                "6"               to GLFW.GLFW_KEY_6,
                "7"               to GLFW.GLFW_KEY_7,
                "8"               to GLFW.GLFW_KEY_8,
                "9"               to GLFW.GLFW_KEY_9,
                "a"               to GLFW.GLFW_KEY_A,
                "b"               to GLFW.GLFW_KEY_B,
                "c"               to GLFW.GLFW_KEY_C,
                "d"               to GLFW.GLFW_KEY_D,
                "e"               to GLFW.GLFW_KEY_E,
                "f"               to GLFW.GLFW_KEY_F,
                "g"               to GLFW.GLFW_KEY_G,
                "h"               to GLFW.GLFW_KEY_H,
                "i"               to GLFW.GLFW_KEY_I,
                "j"               to GLFW.GLFW_KEY_J,
                "k"               to GLFW.GLFW_KEY_K,
                "l"               to GLFW.GLFW_KEY_L,
                "m"               to GLFW.GLFW_KEY_M,
                "n"               to GLFW.GLFW_KEY_N,
                "o"               to GLFW.GLFW_KEY_O,
                "p"               to GLFW.GLFW_KEY_P,
                "q"               to GLFW.GLFW_KEY_Q,
                "r"               to GLFW.GLFW_KEY_R,
                "s"               to GLFW.GLFW_KEY_S,
                "t"               to GLFW.GLFW_KEY_T,
                "u"               to GLFW.GLFW_KEY_U,
                "v"               to GLFW.GLFW_KEY_V,
                "w"               to GLFW.GLFW_KEY_W,
                "x"               to GLFW.GLFW_KEY_X,
                "y"               to GLFW.GLFW_KEY_Y,
                "z"               to GLFW.GLFW_KEY_Z,
                "f1"              to GLFW.GLFW_KEY_F1,
                "f2"              to GLFW.GLFW_KEY_F2,
                "f3"              to GLFW.GLFW_KEY_F3,
                "f4"              to GLFW.GLFW_KEY_F4,
                "f5"              to GLFW.GLFW_KEY_F5,
                "f6"              to GLFW.GLFW_KEY_F6,
                "f7"              to GLFW.GLFW_KEY_F7,
                "f8"              to GLFW.GLFW_KEY_F8,
                "f9"              to GLFW.GLFW_KEY_F9,
                "f10"             to GLFW.GLFW_KEY_F10,
                "f11"             to GLFW.GLFW_KEY_F11,
                "f12"             to GLFW.GLFW_KEY_F12,
                "f13"             to GLFW.GLFW_KEY_F13,
                "f14"             to GLFW.GLFW_KEY_F14,
                "f15"             to GLFW.GLFW_KEY_F15,
                "f16"             to GLFW.GLFW_KEY_F16,
                "f17"             to GLFW.GLFW_KEY_F17,
                "f18"             to GLFW.GLFW_KEY_F18,
                "f19"             to GLFW.GLFW_KEY_F19,
                "f20"             to GLFW.GLFW_KEY_F20,
                "f21"             to GLFW.GLFW_KEY_F21,
                "f22"             to GLFW.GLFW_KEY_F22,
                "f23"             to GLFW.GLFW_KEY_F23,
                "f24"             to GLFW.GLFW_KEY_F24,
                "f25"             to GLFW.GLFW_KEY_F25,
                "num.lock"        to GLFW.GLFW_KEY_NUM_LOCK,
                "keypad.0"        to GLFW.GLFW_KEY_KP_0,
                "keypad.1"        to GLFW.GLFW_KEY_KP_1,
                "keypad.2"        to GLFW.GLFW_KEY_KP_2,
                "keypad.3"        to GLFW.GLFW_KEY_KP_3,
                "keypad.4"        to GLFW.GLFW_KEY_KP_4,
                "keypad.5"        to GLFW.GLFW_KEY_KP_5,
                "keypad.6"        to GLFW.GLFW_KEY_KP_6,
                "keypad.7"        to GLFW.GLFW_KEY_KP_7,
                "keypad.8"        to GLFW.GLFW_KEY_KP_8,
                "keypad.9"        to GLFW.GLFW_KEY_KP_9,
                "keypad.add"      to GLFW.GLFW_KEY_KP_ADD,
                "keypad.decimal"  to GLFW.GLFW_KEY_KP_DECIMAL,
                "keypad.enter"    to GLFW.GLFW_KEY_KP_ENTER,
                "keypad.equal"    to GLFW.GLFW_KEY_KP_EQUAL,
                "keypad.multiply" to GLFW.GLFW_KEY_KP_MULTIPLY,
                "keypad.divide"   to GLFW.GLFW_KEY_KP_DIVIDE,
                "keypad.subtract" to GLFW.GLFW_KEY_KP_SUBTRACT,
                "down"            to GLFW.GLFW_KEY_DOWN,
                "left"            to GLFW.GLFW_KEY_LEFT,
                "right"           to GLFW.GLFW_KEY_RIGHT,
                "up"              to GLFW.GLFW_KEY_UP,
                "apostrophe"      to GLFW.GLFW_KEY_APOSTROPHE,
                "backslash"       to GLFW.GLFW_KEY_BACKSLASH,
                "comma"           to GLFW.GLFW_KEY_COMMA,
                "equal"           to GLFW.GLFW_KEY_EQUAL,
                "grave.accent"    to GLFW.GLFW_KEY_GRAVE_ACCENT,
                "left.bracket"    to GLFW.GLFW_KEY_LEFT_BRACKET,
                "minus"           to GLFW.GLFW_KEY_MINUS,
                "period"          to GLFW.GLFW_KEY_PERIOD,
                "right.bracket"   to GLFW.GLFW_KEY_RIGHT_BRACKET,
                "semicolon"       to GLFW.GLFW_KEY_SEMICOLON,
                "slash"           to GLFW.GLFW_KEY_SLASH,
                "space"           to GLFW.GLFW_KEY_SPACE,
                "tab"             to GLFW.GLFW_KEY_TAB,
                "left.alt"        to GLFW.GLFW_KEY_LEFT_ALT,
                "left.control"    to GLFW.GLFW_KEY_LEFT_CONTROL,
                "left.shift"      to GLFW.GLFW_KEY_LEFT_SHIFT,
                "left.win"        to GLFW.GLFW_KEY_LEFT_SUPER,
                "right.alt"       to GLFW.GLFW_KEY_RIGHT_ALT,
                "right.control"   to GLFW.GLFW_KEY_RIGHT_CONTROL,
                "right.shift"     to GLFW.GLFW_KEY_RIGHT_SHIFT,
                "right.win"       to GLFW.GLFW_KEY_RIGHT_SUPER,
                "enter"           to GLFW.GLFW_KEY_ENTER,
                "escape"          to GLFW.GLFW_KEY_ESCAPE,
                "backspace"       to GLFW.GLFW_KEY_BACKSPACE,
                "delete"          to GLFW.GLFW_KEY_DELETE,
                "end"             to GLFW.GLFW_KEY_END,
                "home"            to GLFW.GLFW_KEY_HOME,
                "insert"          to GLFW.GLFW_KEY_INSERT,
                "page.down"       to GLFW.GLFW_KEY_PAGE_DOWN,
                "page.up"         to GLFW.GLFW_KEY_PAGE_UP,
                "caps.lock"       to GLFW.GLFW_KEY_CAPS_LOCK,
                "pause"           to GLFW.GLFW_KEY_PAUSE,
                "scroll.lock"     to GLFW.GLFW_KEY_SCROLL_LOCK,
                "menu"            to GLFW.GLFW_KEY_MENU,
                "print.screen"    to GLFW.GLFW_KEY_PRINT_SCREEN,
                "world.1"         to GLFW.GLFW_KEY_WORLD_1,
                "world.2"         to GLFW.GLFW_KEY_WORLD_2
            )
            key2int = SoftReference(m)
            return m
        }
    }
}