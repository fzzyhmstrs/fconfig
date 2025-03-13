/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.minecraft

import com.mojang.serialization.JsonOps
import me.fzzyhmstrs.fzzy_config.entry.EntryOpener
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.regRef
import me.fzzyhmstrs.fzzy_config.util.TomlOps
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.wrap
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlNull
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.jvm.optionals.getOrNull

/**
 * A validated TagKey
 *
 * By default, validation will allow any TagKey currently known by the registry of the provided default Tag.
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/TagKeys) for more details and examples.
 * @param T the TagKey type
 * @param defaultValue [TagKey] - the default tag
 * @param predicate [Predicate]&lt;[Identifier]&gt;, Optional - use to restrict the allowable tag selection
 * @sample me.fzzyhmstrs.fzzy_config.examples.MinecraftExamples.tags
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ValidatedTagKey<T: Any> @JvmOverloads constructor(defaultValue: TagKey<T>, private val predicate: Predicate<Identifier>? = null): ValidatedField<TagKey<T>>(defaultValue), EntryOpener {

    private val validator = if(predicate == null) ValidatedIdentifier.ofRegistryTags(defaultValue.regRef()) else ValidatedIdentifier.ofRegistryTags(defaultValue.regRef(), predicate)
    private val codec = TagKey.codec(defaultValue.regRef())

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<TagKey<T>> {
        return try {
            val json = TomlOps.INSTANCE.convertTo(JsonOps.INSTANCE, toml)
            val dataResult = codec.parse(JsonOps.INSTANCE, json)
            if (dataResult.isSuccess) {
                ValidationResult.success(dataResult.orThrow)
            } else {
                ValidationResult.error(storedValue, "Error deserializing Validated Tag [$fieldName]: ${dataResult.error().getOrNull()?.message()}")
            }
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error encountered while deserializing Validated Tag [$fieldName]: ${e.localizedMessage}")
        }
    }

    @Internal
    override fun serialize(input: TagKey<T>): ValidationResult<TomlElement> {
        val encodeResult = codec.encodeStart(JsonOps.INSTANCE, input)
        if (encodeResult.isError) {
            return ValidationResult.error(TomlNull, "Error serializing TagKey: ${encodeResult.error().getOrNull()?.message()}")
        }
        return try {
            ValidationResult.success(JsonOps.INSTANCE.convertTo(TomlOps.INSTANCE, encodeResult.orThrow))
        } catch (e: Throwable) {
            ValidationResult.error(TomlNull, "Critical Error while serializing TagKey: ${e.localizedMessage}")
        }
    }


    /**
     * creates a deep copy of this ValidatedTagKey
     * return ValidatedTagKey wrapping a deep copy of the currently stored tag and predicate
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedField<TagKey<T>> {
        return ValidatedTagKey(copyStoredValue(), predicate)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is TagKey<*> && input.regRef() == storedValue.regRef()
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input TagKey input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: TagKey<T>): TagKey<T> {
        return TagKey.of(input.regRef(), input.id)
    }

    override fun set(input: TagKey<T>) {
        validator.validateAndSet(input.id)
        super.set(input)
    }

    @Internal
    override fun widgetEntry(choicePredicate: ChoiceValidator<TagKey<T>>): ClickableWidget {
        return OnClickTextFieldWidget({ validator.get().toString() }, { b, isKb, key, code, mods ->
            openTagPopup(isKb, key, code, mods, choicePredicate, { _, _ -> b.x - 8 }, { _, h -> b.y + 28 + 24 - h })
        })
    }

    @Internal
    override fun open(args: List<String>) {
        openTagPopup(false, 0, 0, 0)
    }

    @Internal
    override fun entryDeco(): Decorated.DecoratedOffset {
        return Decorated.DecoratedOffset(TextureDeco.DECO_TAG, 2, 2)
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated TagKey[value=$storedValue, validation=${if(predicate==null) "any tag in ${storedValue.regRef()}" else "tags from ${storedValue.regRef()} restricted with a predicate"}]"
    }

    @Internal
    //client
    private fun openTagPopup(isKeyboard: Boolean, keyCode: Int, scanCode: Int, modifiers: Int, choicePredicate: ChoiceValidator<TagKey<T>> = ChoiceValidator.any(), xPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center(), yPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center()) {
        val entryValidator = EntryValidator<String>{s, _ -> Identifier.tryParse(s)?.let { validator.validateEntry(it, EntryValidator.ValidationType.STRONG)}?.wrap(s) ?: ValidationResult.error(s, "invalid Identifier")}
        val entryApplier = Consumer<String> { e -> setAndUpdate(TagKey.of(defaultValue.regRef(), e.simpleId())) }
        val suggestionProvider = SuggestionBackedTextFieldWidget.SuggestionProvider {s, c, cv -> validator.allowableIds.getSuggestions(s, c, cv.convert({ it.simpleId() }, { it.simpleId() }))}
        val textField = SuggestionBackedTextFieldWidget(170, 20, { validator.get().toString() }, choicePredicate.convert({it.id.toString()}, {it.id.toString()}), entryValidator, entryApplier, suggestionProvider)
        val popup = PopupWidget.Builder(translation())
            .add("text_field", textField, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .addDoneWidget({ textField.pushChanges(); PopupWidget.pop() })
            .positionX(xPosition)
            .positionY(yPosition)
            .build()
        PopupWidget.push(popup)
        PopupWidget.focusElement(textField)
        if (isKeyboard)
            textField.keyPressed(keyCode, scanCode, modifiers)
    }
}