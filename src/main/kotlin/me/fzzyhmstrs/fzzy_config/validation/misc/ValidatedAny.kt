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
import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.annotations.IgnoreVisibility
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.config.ConfigAction
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryParent
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.screen.entry.BaseConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.SettingConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.ValidatedAnyConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.widget.ActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.DecoratedActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.contextualize
import me.fzzyhmstrs.fzzy_config.util.Walkable
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Supplier
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaConstructor

/**
 * Validation for an arbitrary non-null POJO (Plain Old Java Object). It will create a "mini-config" popup with the same style of list as the main config and section screens. Each field within the object will be validated just like a config, either automatically or purposefully if ValidatedFeilds are used.
 *
 * The object passed follows the same rules as that of a config or config section. Non-final fields/properties that are public unless annotated with [IgnoreVisibility]
 *
 * This Validation is useful for "blocks" of config, such as Entity Stats or Tool Materials, that you want to break out.
 *
 * You can nest other POJO within this one, as long as it either implements [Walkable] or is another ValidatedAny
 * @param T [Walkable] object to be managed
 * @param defaultValue Instance of T to wrap
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.anys
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ValidatedAny<T: Any>(defaultValue: T): ValidatedField<T>(defaultValue), EntryParent {
    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<T> {
        return ConfigApi.deserializeFromToml(storedValue, toml, mutableListOf()).contextualize()
    }
    @Internal
    override fun serialize(input: T): ValidationResult<TomlElement> {
        val errors = mutableListOf<String>()
        return ValidationResult.predicated(ConfigApi.serializeToToml(input, errors), errors.isEmpty(), "Errors encountered while serializing Object: $errors")
    }

    /**
     * creates a deep copy of this ValidatedAny
     * @return ValidatedAny wrapping a deep copy of the currently stored object
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedField<T> {
        return ValidatedAny(copyStoredValue())
    }

    @Internal
    override fun setAndUpdate(input: T) {
        if (input == get()) return
        val oldVal = get()
        val oldStr = ConfigApi.serializeConfig(oldVal, mutableListOf(), 1).lines().joinToString(" ", transform = {s -> s.trim()})
        val tVal1 = correctEntry(input, EntryValidator.ValidationType.STRONG)
        val newStr = ConfigApi.serializeConfig(tVal1.get(), mutableListOf(), 1).lines().joinToString(" ", transform = {s -> s.trim()})
        set(tVal1.get())
        val message = if (tVal1.isError()) {
            FcText.translatable("fc.validated_field.update.error", translation(), oldStr, newStr, tVal1.getError())
        } else {
            FcText.translatable("fc.validated_field.update", translation(), oldStr, newStr)
        }
        update(message)
    }

    /**
     * Creates a deep copy of the stored value and returns it
     * @return T - deep copy of the currently stored object
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun copyStoredValue(): T {
        return try {
            val new = createInstance()
            val toml = serialize(this.get()).get()
            val result = ConfigApi.deserializeFromToml(new, toml, mutableListOf())
            if (result.isError()) storedValue else result.get().config
        } catch(e: Exception) {
            storedValue //object doesn't have an empty constructor. no prob.
        }
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        return input::class.java == defaultValue::class.java
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        return DecoratedActiveButtonWidget("fc.validated_field.object".translate(), 110, 20, "widget/decoration/object".fcId(), { true }, { openObjectPopup() })
    }

    /**
     * @suppress
     */
     override fun toString(): String {
        return "Validated Walkable[value=${
            ConfigApi.serializeConfig(get(), mutableListOf(), 1).lines().joinToString(" ", transform = { s -> s.trim() })
        }, validation=per contained member validation]"
    }

    private fun createInstance(): T {
        val noArgsConstructor = storedValue::class.constructors.singleOrNull { it.parameters.all(KParameter::isOptional) }
            ?: throw IllegalArgumentException("Class should have a single no-arg constructor: $this")
        if (storedValue::class.annotations.firstOrNull { (it is IgnoreVisibility) }?.let { true } == true)
            noArgsConstructor.javaConstructor?.trySetAccessible()
        return noArgsConstructor.callBy(emptyMap())
    }

    //client
    private fun openObjectPopup() {
        val newThing = copyStoredValue()
        val newNewThing = try{ createInstance() } catch (e: Exception) { defaultValue }
        val manager = ValidatedObjectUpdateManager(newThing, getEntryKey())
        val entryList = ConfigListWidget(MinecraftClient.getInstance(), 298, 160, 0, false)
        ConfigApiImpl.walk(newThing, getEntryKey(), 1){_, _, new, thing, _, annotations, globalAnnotations, _ ->
            val action = ConfigApiImpl.requiredAction(annotations, globalAnnotations)?.let { setOf(it) } ?: setOf()
            if (thing is Updatable && thing is Entry<*, *>) {
                val fieldName = new.substringAfterLast('.')
                val name = ConfigApiImplClient.getTranslation(thing, fieldName, annotations, globalAnnotations)
                thing.setEntryKey(new)
                thing.setUpdateManager(manager)
                manager.setUpdatableEntry(thing)
                entryList.add(SettingConfigEntry(name, ConfigApiImplClient.getDescription(thing, fieldName, annotations, globalAnnotations), action, entryList, thing.widgetEntry(), null, null, null))

            } else if (thing is ConfigAction) {
                val fieldName = new.substringAfterLast('.')
                val name = ConfigApiImplClient.getTranslation(thing, fieldName, annotations, globalAnnotations)
                entryList.add(BaseConfigEntry(name, ConfigApiImplClient.getDescription(thing, fieldName, annotations, globalAnnotations), action, entryList, thing.widgetEntry()))
            } else if (thing is Walkable) {
                val validation = ValidatedAny(thing)
                validation.setEntryKey(new)
                validation.setUpdateManager(manager)
                manager.setUpdatableEntry(validation)
                val fieldName = new.substringAfterLast('.')
                val name = ConfigApiImplClient.getTranslation(validation, fieldName, annotations, globalAnnotations)
                val actions = ConfigApiImpl.getActions(thing, 1)
                entryList.add(ValidatedAnyConfigEntry(name, ConfigApiImplClient.getDescription(validation, fieldName, annotations, globalAnnotations), actions, entryList, validation.widgetEntry(), null, null, null))
            } else if (thing != null) {
                var basicValidation: ValidatedField<*>? = null
                val target = new.removePrefix("${getEntryKey()}.")
                ConfigApiImpl.drill(newNewThing, target, '.', 1) { _, _, _, thing2, drillProp, drillAnnotations, _, _ ->
                    basicValidation = manager.basicValidationStrategy(thing2, drillProp.returnType, drillAnnotations)?.instanceEntry()
                }
                val basicValidation2 = basicValidation
                if (basicValidation2 != null) {
                    basicValidation2.trySet(thing)
                    basicValidation2.setEntryKey(new)
                    basicValidation2.setUpdateManager(manager)
                    manager.setUpdatableEntry(basicValidation2)
                    val fieldName = new.substringAfterLast('.')
                    val name = ConfigApiImplClient.getTranslation(basicValidation2, fieldName, annotations, globalAnnotations)
                    entryList.add(BaseConfigEntry(name, ConfigApiImplClient.getDescription(basicValidation2, fieldName, annotations, globalAnnotations), action, entryList, basicValidation2.widgetEntry()))
                }
            }
        }
        manager.pushUpdatableStates()
        val popup = PopupWidget.Builder(translation())
            .addElement("list", entryList, Position.BELOW, Position.ALIGN_CENTER)
            .addElement("revert", ActiveButtonWidget("fc.button.revert".translate(), 147, 20, { manager.hasChanges() }, { manager.revert() }), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("restore", ActiveButtonWidget("fc.button.restore".translate(), 147, 20, { manager.hasRestores("") }, { manager.restore("") }), Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
            .addDoneWidget()
            .onClose { manager.apply(true); if(manager.hasChanges()) setAndUpdate(newThing) }
            .build()
        PopupWidget.push(popup)
    }

    override fun actions(): Set<Action> {
        return ConfigApiImpl.getActions(storedValue, ConfigApiImpl.IGNORE_NON_SYNC)
    }

    override fun translationKey(): String {
        return (storedValue as? Translatable)?.hasTranslation()?.let { (storedValue as? Translatable)?.translationKey() } ?: super.translationKey()
    }

    override fun descriptionKey(): String {
        return (storedValue as? Translatable)?.hasDescription()?.let { (storedValue as? Translatable)?.descriptionKey() } ?: super.descriptionKey()
    }

    override fun translation(fallback: String?): MutableText {
        return  (storedValue as? Translatable)?.hasTranslation()?.let { (storedValue as? Translatable)?.translation(fallback) } ?: super.translation(fallback)
    }

    override fun description(fallback: String?): MutableText {
        return (storedValue as? Translatable)?.hasDescription()?.let { (storedValue as? Translatable)?.description(fallback) } ?: super.description(fallback)
    }

    override fun hasTranslation(): Boolean {
        return (storedValue as? Translatable)?.hasTranslation()?.let { if(!it && super.hasTranslation()) true else it } ?: super.hasTranslation()
    }

    override fun hasDescription(): Boolean {
        return (storedValue as? Translatable)?.hasDescription()?.let { if(!it && super.hasTranslation()) true else it } ?: super.hasDescription()
    }

    //client
    private class ValidatedObjectUpdateManager<T: Any>(private val thing: T, private val key: String): BaseUpdateManager() {

        private val updatableEntries: MutableMap<String, Updatable> = mutableMapOf()

        fun setUpdatableEntry(entry: Updatable) {
            updatableEntries[entry.getEntryKey()] = entry
        }

        fun pushUpdatableStates() {
            for (updatable in updatableEntries.values) {
                updatable.pushState()
            }
        }

        override fun restoreCount(scope: String): Int {
            var count = 0
            for ((_, updatable) in updatableEntries) {
                if(updatable.isDefault()) continue
                count++
            }
            return count
        }

        override fun restore(scope: String) {
            for ((_, updatable) in updatableEntries) {
                updatable.restore()
            }
        }

        override fun apply(final: Boolean) {
            if (updateMap.isEmpty()) return

            //push updates from basic validation to the configs
            ConfigApiImpl.walk(thing, key, 1) { walkable, _, new, thing, prop, _, _, _ ->
                if (!(thing is Updatable && thing is Entry<*, *>)) {
                    val update = getUpdate(new)
                    if (update != null && update is Supplier<*>) {
                        try {
                            prop.setter.call(walkable, update.get())
                        } catch (e: Exception) {
                            FC.LOGGER.error("Error pushing update to simple property [$new]")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}