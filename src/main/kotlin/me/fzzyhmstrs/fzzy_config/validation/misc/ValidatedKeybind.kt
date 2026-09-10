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
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.context.ContextType.Relevant
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.isShiftDown
import me.fzzyhmstrs.fzzy_config.util.TomlOps
import me.fzzyhmstrs.fzzy_config.util.TriState
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.AbstractWidget
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.peanuuutz.tomlkt.*
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.sdl.SDLScancode
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
                val ctrlResult = modifierHandler.deserializeEntry(ctrlToml, "$fieldName.ctrl", 65).attachTo(errors)
                val shiftResult = modifierHandler.deserializeEntry(shiftToml, "$fieldName.shift", 65).attachTo(errors)
                val altResult = modifierHandler.deserializeEntry(altToml, "$fieldName.alt", 65).attachTo(errors)
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
    override fun widgetEntry(choicePredicate: ChoiceValidator<FzzyKeybind>): AbstractWidget {
        val layout = LayoutWidget.Builder().paddingBoth(0).spacingBoth(0).build()
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
        return this.copyProvidersTo(ValidatedKeybind(this.storedValue.clone()))
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
     * Tests whether the keybind is currently being pressed.
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    override fun isPressed(): Boolean {
        return storedValue.isPressed()
    }

    override fun needsCtrl(): Boolean {
        return storedValue.needsCtrl()
    }

    override fun needsShift(): Boolean {
        return storedValue.needsShift()
    }

    override fun needsAlt(): Boolean {
        return storedValue.needsAlt()
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

        override fun getMessage(): Component {
            return if (resetting) {
                if (compounding) {
                    FcText.translatable("fc.keybind.or", this@ValidatedKeybind.get().keybind(), FcText.translatable("fc.keybind.resetting", FcText.literal("  ").withStyle(
                        ChatFormatting.UNDERLINE)))
                } else {
                    FcText.translatable("fc.keybind.resetting", this@ValidatedKeybind.get().keybind().copy().withStyle(ChatFormatting.UNDERLINE))
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
                Minecraft.getInstance().gui.screen()?.nullCast<ConfigScreen>()?.setGlobalInputHandler(null)
            }
        }

        override fun onPress() {
            resetting = true
            justCLickedToggle = true
            if (isShiftDown() && this@ValidatedKeybind.storedValue != FzzyKeybindUnbound) {
                justClickedShift = true
                compounding = true
            }
            setupHandler()
        }

        fun setupHandler() {
            Minecraft.getInstance().gui.screen()?.nullCast<ConfigScreen>()?.setGlobalInputHandler { key, released, type, ctrl, shift, alt ->
                if (!released || justCLickedToggle || justClickedShift) {
                    if (released && (key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT)) {
                        justClickedShift = false
                    }
                    if (released && (key == InputConstants.MOUSE_BUTTON_LEFT || (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_SPACE || key == InputConstants.KEY_NUMPADENTER))) {
                        justCLickedToggle = false
                    }
                    return@setGlobalInputHandler TriState.FALSE
                }
                if (key == InputConstants.KEY_ESCAPE && !ctrl && !shift && !alt) {
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
                Minecraft.getInstance().gui.screen()?.nullCast<ConfigScreen>()?.setGlobalInputHandler(null)
                TriState.TRUE
            }
        }

        override fun createNarrationMessage(): MutableComponent {
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
                InputConstants.MOUSE_BUTTON_LEFT     to "mouse.left",
                InputConstants.MOUSE_BUTTON_RIGHT    to "mouse.right",
                InputConstants.MOUSE_BUTTON_MIDDLE   to "mouse.middle",
//                InputConstants.MOUSE_BUTTON_4        to "mouse.4",
//                InputConstants.MOUSE_BUTTON_5        to "mouse.5",
//                InputConstants.MOUSE_BUTTON_6        to "mouse.6",
//                InputConstants.MOUSE_BUTTON_7        to "mouse.7",
//                InputConstants.MOUSE_BUTTON_8        to "mouse.8",
                InputConstants.KEY_0                 to "0",
                InputConstants.KEY_1                 to "1",
                InputConstants.KEY_2                 to "2",
                InputConstants.KEY_3                 to "3",
                InputConstants.KEY_4                 to "4",
                InputConstants.KEY_5                 to "5",
                InputConstants.KEY_6                 to "6",
                InputConstants.KEY_7                 to "7",
                InputConstants.KEY_8                 to "8",
                InputConstants.KEY_9                 to "9",
                InputConstants.KEY_A                 to "a",
                InputConstants.KEY_B                 to "b",
                InputConstants.KEY_C                 to "c",
                InputConstants.KEY_D                 to "d",
                InputConstants.KEY_E                 to "e",
                InputConstants.KEY_F                 to "f",
                InputConstants.KEY_G                 to "g",
                InputConstants.KEY_H                 to "h",
                InputConstants.KEY_I                 to "i",
                InputConstants.KEY_J                 to "j",
                InputConstants.KEY_K                 to "k",
                InputConstants.KEY_L                 to "l",
                InputConstants.KEY_M                 to "m",
                InputConstants.KEY_N                 to "n",
                InputConstants.KEY_O                 to "o",
                InputConstants.KEY_P                 to "p",
                InputConstants.KEY_Q                 to "q",
                InputConstants.KEY_R                 to "r",
                InputConstants.KEY_S                 to "s",
                InputConstants.KEY_T                 to "t",
                InputConstants.KEY_U                 to "u",
                InputConstants.KEY_V                 to "v",
                InputConstants.KEY_W                 to "w",
                InputConstants.KEY_X                 to "x",
                InputConstants.KEY_Y                 to "y",
                InputConstants.KEY_Z                 to "z",
                InputConstants.KEY_F1                to "f1",
                InputConstants.KEY_F2                to "f2",
                InputConstants.KEY_F3                to "f3",
                InputConstants.KEY_F4                to "f4",
                InputConstants.KEY_F5                to "f5",
                InputConstants.KEY_F6                to "f6",
                InputConstants.KEY_F7                to "f7",
                InputConstants.KEY_F8                to "f8",
                InputConstants.KEY_F9                to "f9",
                InputConstants.KEY_F10               to "f10",
                InputConstants.KEY_F11               to "f11",
                InputConstants.KEY_F12               to "f12",
                InputConstants.KEY_F13               to "f13",
                InputConstants.KEY_F14               to "f14",
                InputConstants.KEY_F15               to "f15",
                InputConstants.KEY_F16               to "f16",
                InputConstants.KEY_F17               to "f17",
                InputConstants.KEY_F18               to "f18",
                InputConstants.KEY_F19               to "f19",
                InputConstants.KEY_F20               to "f20",
                InputConstants.KEY_F21               to "f21",
                InputConstants.KEY_F22               to "f22",
                InputConstants.KEY_F23               to "f23",
                InputConstants.KEY_F24               to "f24",
//                InputConstants.KEY_F25               to "f25",
                InputConstants.KEY_NUMLOCK           to "num.lock",
                InputConstants.KEY_NUMPAD0           to "keypad.0",
                InputConstants.KEY_NUMPAD1           to "keypad.1",
                InputConstants.KEY_NUMPAD2           to "keypad.2",
                InputConstants.KEY_NUMPAD3           to "keypad.3",
                InputConstants.KEY_NUMPAD4           to "keypad.4",
                InputConstants.KEY_NUMPAD5           to "keypad.5",
                InputConstants.KEY_NUMPAD6           to "keypad.6",
                InputConstants.KEY_NUMPAD7           to "keypad.7",
                InputConstants.KEY_NUMPAD8           to "keypad.8",
                InputConstants.KEY_NUMPAD9           to "keypad.9",
                SDLScancode.SDL_SCANCODE_KP_PLUS     to "keypad.add",
                InputConstants.KEY_NUMPADCOMMA       to "keypad.decimal",
                InputConstants.KEY_NUMPADENTER       to "keypad.enter",
                InputConstants.KEY_NUMPADEQUALS      to "keypad.equal",
                SDLScancode.SDL_SCANCODE_KP_MULTIPLY to "keypad.multiply",
                SDLScancode.SDL_SCANCODE_KP_DIVIDE   to "keypad.divide",
                SDLScancode.SDL_SCANCODE_KP_MINUS    to "keypad.subtract",
                InputConstants.KEY_DOWN              to "down",
                InputConstants.KEY_LEFT              to "left",
                InputConstants.KEY_RIGHT             to "right",
                InputConstants.KEY_UP                to "up",
                InputConstants.KEY_APOSTROPHE        to "apostrophe",
                InputConstants.KEY_BACKSLASH         to "backslash",
                InputConstants.KEY_COMMA             to "comma",
                InputConstants.KEY_EQUALS            to "equal",
                InputConstants.KEY_GRAVE             to "grave.accent",
                InputConstants.KEY_LBRACKET          to "left.bracket",
                InputConstants.KEY_MINUS             to "minus",
                InputConstants.KEY_PERIOD            to "period",
                InputConstants.KEY_RBRACKET          to "right.bracket",
                InputConstants.KEY_SEMICOLON         to "semicolon",
                InputConstants.KEY_SLASH             to "slash",
                InputConstants.KEY_SPACE             to "space",
                InputConstants.KEY_TAB               to "tab",
                InputConstants.KEY_LALT              to "left.alt",
                InputConstants.KEY_LCONTROL          to "left.control",
                InputConstants.KEY_LSHIFT            to "left.shift",
                InputConstants.KEY_LGUI              to "left.win",
                InputConstants.KEY_RALT              to "right.alt",
                InputConstants.KEY_RCONTROL          to "right.control",
                InputConstants.KEY_RSHIFT            to "right.shift",
                InputConstants.KEY_RGUI              to "right.win",
                InputConstants.KEY_RETURN            to "enter",
                InputConstants.KEY_ESCAPE            to "escape",
                InputConstants.KEY_BACKSPACE         to "backspace",
                InputConstants.KEY_DELETE            to "delete",
                InputConstants.KEY_END               to "end",
                InputConstants.KEY_HOME              to "home",
                InputConstants.KEY_INSERT            to "insert",
                InputConstants.KEY_PAGEDOWN          to "page.down",
                InputConstants.KEY_PAGEUP            to "page.up",
                InputConstants.KEY_CAPSLOCK          to "caps.lock",
                InputConstants.KEY_PAUSE             to "pause",
                InputConstants.KEY_SCROLLLOCK        to "scroll.lock",
                SDLScancode.SDL_SCANCODE_MENU        to "menu",
                InputConstants.KEY_PRINTSCREEN       to "print.screen",
//                InputConstants.KEY_WORLD_1           to "world.1",
//                InputConstants.KEY_WORLD_2           to "world.2"
            )
            int2key = SoftReference(m)
            return m
        }

        private fun initKey2Int(): Map<String, Int> {
            val m = mapOf(
                "mouse.left"      to InputConstants.MOUSE_BUTTON_LEFT,
                "mouse.right"     to InputConstants.MOUSE_BUTTON_RIGHT,
                "mouse.middle"    to InputConstants.MOUSE_BUTTON_MIDDLE,
//                "mouse.4"         to InputConstants.MOUSE_BUTTON_4,
//                "mouse.5"         to InputConstants.MOUSE_BUTTON_5,
//                "mouse.6"         to InputConstants.MOUSE_BUTTON_6,
//                "mouse.7"         to InputConstants.MOUSE_BUTTON_7,
//                "mouse.8"         to InputConstants.MOUSE_BUTTON_8,
                "0"               to InputConstants.KEY_0,
                "1"               to InputConstants.KEY_1,
                "2"               to InputConstants.KEY_2,
                "3"               to InputConstants.KEY_3,
                "4"               to InputConstants.KEY_4,
                "5"               to InputConstants.KEY_5,
                "6"               to InputConstants.KEY_6,
                "7"               to InputConstants.KEY_7,
                "8"               to InputConstants.KEY_8,
                "9"               to InputConstants.KEY_9,
                "a"               to InputConstants.KEY_A,
                "b"               to InputConstants.KEY_B,
                "c"               to InputConstants.KEY_C,
                "d"               to InputConstants.KEY_D,
                "e"               to InputConstants.KEY_E,
                "f"               to InputConstants.KEY_F,
                "g"               to InputConstants.KEY_G,
                "h"               to InputConstants.KEY_H,
                "i"               to InputConstants.KEY_I,
                "j"               to InputConstants.KEY_J,
                "k"               to InputConstants.KEY_K,
                "l"               to InputConstants.KEY_L,
                "m"               to InputConstants.KEY_M,
                "n"               to InputConstants.KEY_N,
                "o"               to InputConstants.KEY_O,
                "p"               to InputConstants.KEY_P,
                "q"               to InputConstants.KEY_Q,
                "r"               to InputConstants.KEY_R,
                "s"               to InputConstants.KEY_S,
                "t"               to InputConstants.KEY_T,
                "u"               to InputConstants.KEY_U,
                "v"               to InputConstants.KEY_V,
                "w"               to InputConstants.KEY_W,
                "x"               to InputConstants.KEY_X,
                "y"               to InputConstants.KEY_Y,
                "z"               to InputConstants.KEY_Z,
                "f1"              to InputConstants.KEY_F1,
                "f2"              to InputConstants.KEY_F2,
                "f3"              to InputConstants.KEY_F3,
                "f4"              to InputConstants.KEY_F4,
                "f5"              to InputConstants.KEY_F5,
                "f6"              to InputConstants.KEY_F6,
                "f7"              to InputConstants.KEY_F7,
                "f8"              to InputConstants.KEY_F8,
                "f9"              to InputConstants.KEY_F9,
                "f10"             to InputConstants.KEY_F10,
                "f11"             to InputConstants.KEY_F11,
                "f12"             to InputConstants.KEY_F12,
                "f13"             to InputConstants.KEY_F13,
                "f14"             to InputConstants.KEY_F14,
                "f15"             to InputConstants.KEY_F15,
                "f16"             to InputConstants.KEY_F16,
                "f17"             to InputConstants.KEY_F17,
                "f18"             to InputConstants.KEY_F18,
                "f19"             to InputConstants.KEY_F19,
                "f20"             to InputConstants.KEY_F20,
                "f21"             to InputConstants.KEY_F21,
                "f22"             to InputConstants.KEY_F22,
                "f23"             to InputConstants.KEY_F23,
                "f24"             to InputConstants.KEY_F24,
//                "f25"             to InputConstants.KEY_F25,
                "num.lock"        to InputConstants.KEY_NUMLOCK,
                "keypad.0"        to InputConstants.KEY_NUMPAD0,
                "keypad.1"        to InputConstants.KEY_NUMPAD1,
                "keypad.2"        to InputConstants.KEY_NUMPAD2,
                "keypad.3"        to InputConstants.KEY_NUMPAD3,
                "keypad.4"        to InputConstants.KEY_NUMPAD4,
                "keypad.5"        to InputConstants.KEY_NUMPAD5,
                "keypad.6"        to InputConstants.KEY_NUMPAD6,
                "keypad.7"        to InputConstants.KEY_NUMPAD7,
                "keypad.8"        to InputConstants.KEY_NUMPAD8,
                "keypad.9"        to InputConstants.KEY_NUMPAD9,
                "keypad.add"      to SDLScancode.SDL_SCANCODE_KP_PLUS,
                "keypad.decimal"  to InputConstants.KEY_NUMPADCOMMA,
                "keypad.enter"    to InputConstants.KEY_NUMPADENTER,
                "keypad.equal"    to InputConstants.KEY_NUMPADEQUALS,
                "keypad.multiply" to SDLScancode.SDL_SCANCODE_KP_MULTIPLY,
                "keypad.divide"   to SDLScancode.SDL_SCANCODE_KP_DIVIDE,
                "keypad.subtract" to SDLScancode.SDL_SCANCODE_KP_MINUS,
                "down"            to InputConstants.KEY_DOWN,
                "left"            to InputConstants.KEY_LEFT,
                "right"           to InputConstants.KEY_RIGHT,
                "up"              to InputConstants.KEY_UP,
                "apostrophe"      to InputConstants.KEY_APOSTROPHE,
                "backslash"       to InputConstants.KEY_BACKSLASH,
                "comma"           to InputConstants.KEY_COMMA,
                "equal"           to InputConstants.KEY_EQUALS,
                "grave.accent"    to InputConstants.KEY_GRAVE,
                "left.bracket"    to InputConstants.KEY_LBRACKET,
                "minus"           to InputConstants.KEY_MINUS,
                "period"          to InputConstants.KEY_PERIOD,
                "right.bracket"   to InputConstants.KEY_RBRACKET,
                "semicolon"       to InputConstants.KEY_SEMICOLON,
                "slash"           to InputConstants.KEY_SLASH,
                "space"           to InputConstants.KEY_SPACE,
                "tab"             to InputConstants.KEY_TAB,
                "left.alt"        to InputConstants.KEY_LALT,
                "left.control"    to InputConstants.KEY_LCONTROL,
                "left.shift"      to InputConstants.KEY_LSHIFT,
                "left.win"        to InputConstants.KEY_LGUI,
                "right.alt"       to InputConstants.KEY_RALT,
                "right.control"   to InputConstants.KEY_RCONTROL,
                "right.shift"     to InputConstants.KEY_RSHIFT,
                "right.win"       to InputConstants.KEY_RGUI,
                "enter"           to InputConstants.KEY_RETURN,
                "escape"          to InputConstants.KEY_ESCAPE,
                "backspace"       to InputConstants.KEY_BACKSPACE,
                "delete"          to InputConstants.KEY_DELETE,
                "end"             to InputConstants.KEY_END,
                "home"            to InputConstants.KEY_HOME,
                "insert"          to InputConstants.KEY_INSERT,
                "page.down"       to InputConstants.KEY_PAGEDOWN,
                "page.up"         to InputConstants.KEY_PAGEUP,
                "caps.lock"       to InputConstants.KEY_CAPSLOCK,
                "pause"           to InputConstants.KEY_PAUSE,
                "scroll.lock"     to InputConstants.KEY_SCROLLLOCK,
                "menu"            to SDLScancode.SDL_SCANCODE_MENU,
                "print.screen"    to InputConstants.KEY_PRINTSCREEN,
//                "world.1"         to InputConstants.KEY_WORLD_1,
//                "world.2"         to InputConstants.KEY_WORLD_2
            )
            key2int = SoftReference(m)
            return m
        }
    }
}