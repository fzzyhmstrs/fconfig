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
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.screen.entry.BaseConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.SettingConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.widget.ActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.DecoratedActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.contextualize
import me.fzzyhmstrs.fzzy_config.util.Walkable
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Supplier
import kotlin.reflect.full.createInstance

/**
 * Validation for an arbitrary non-null POJO. It will create a "mini-config" popup with the same style of list as the main config and section screens.
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
open class ValidatedAny<T: Any>(defaultValue: T): ValidatedField<T>(defaultValue) {
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
            val new = storedValue::class.createInstance()
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
    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        return DecoratedActiveButtonWidget("fc.validated_field.object".translate(), 110, 20, "widget/decoration/object".fcId(), { true }, { openObjectPopup() })
    }

    /**
     * @suppress
     */
     override fun toString(): String {
         return "Validated Walkable[value=${ConfigApi.serializeConfig(get(), mutableListOf(), 1).lines().joinToString(" ", transform = {s -> s.trim()})}, validation=per contained member validation]"
     }

    @Environment(EnvType.CLIENT)
    private fun openObjectPopup() {
        val newThing = copyStoredValue()
        val newNewThing = try{ defaultValue::class.createInstance() } catch (e: Exception) { defaultValue }
        val manager = ValidatedObjectUpdateManager(newThing, getEntryKey())
        val entryList = ConfigListWidget(MinecraftClient.getInstance(), 298, 160, 0, false)
        ConfigApiImpl.walk(newThing, getEntryKey(), 1){_, _, new, thing, _, annotations, callback ->
            val restart = ConfigApiImpl.isRequiresRestart(annotations) || ConfigApiImpl.isRequiresRestart(callback.walkable::class.annotations)
            if (thing is Updatable && thing is Entry<*, *>) {
                val fieldName = new.substringAfterLast('.')
                val name = thing.transLit(fieldName.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
                thing.setEntryKey(new)
                thing.setUpdateManager(manager)
                manager.setUpdatableEntry(thing)
                entryList.add(SettingConfigEntry(name, thing.descLit(""), restart, entryList, thing.widgetEntry(), null, null, null))

            } else if (thing is Walkable) {
                val validation = ValidatedAny(thing)
                validation.setEntryKey(new)
                validation.setUpdateManager(manager)
                manager.setUpdatableEntry(validation)
                val name = validation.translation()
                entryList.add(SettingConfigEntry(name, validation.descLit(""), restart, entryList, validation.widgetEntry(), null, null, null))
            } else if (thing != null) {
                var basicValidation: ValidatedField<*>? = null
                val target = new.removePrefix("${getEntryKey()}.")
                ConfigApiImpl.drill(newNewThing, target, '.', 1) { _, _, _, thing2, drillProp, drillAnnotations, _ ->
                    basicValidation = manager.basicValidationStrategy(thing2, drillProp.returnType, drillAnnotations)?.instanceEntry()
                }
                val basicValidation2 = basicValidation
                if (basicValidation2 != null) {
                    basicValidation2.trySet(thing)
                    basicValidation2.setEntryKey(new)
                    basicValidation2.setUpdateManager(manager)
                    manager.setUpdatableEntry(basicValidation2)
                    val name = basicValidation2.translation()
                    entryList.add(BaseConfigEntry(name, basicValidation2.descLit(""), restart, entryList, basicValidation2.widgetEntry()))
                }
            }
        }
        manager.pushUpdatableStates()
        val popup = PopupWidget.Builder(translation())
            .addElement("list", entryList, Position.BELOW, Position.ALIGN_CENTER)
            .addElement("revert", ActiveButtonWidget("fc.button.revert".translate(), 147, 20, { manager.hasChanges() }, { manager.revert() }), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("restore", ActiveButtonWidget("fc.button.restore".translate(), 147, 20, { manager.hasRestores("") }, { manager.restore("") }), Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
            .addDoneButton()
            .onClose { manager.apply(true); if(manager.hasChanges()) setAndUpdate(newThing) }
            .build()
        PopupWidget.push(popup)
    }

    fun restartRequired(): Boolean {
        var restart = false
        ConfigApiImpl.walk(storedValue, getEntryKey(), 1){_, _, _, thing, _, annotations, callback ->
            if (ConfigApiImpl.isRequiresRestart(annotations) || ConfigApiImpl.isRequiresRestart(callback.walkable::class.annotations)) {
                restart = true
                callback.cancel()
            }
            if (thing is ValidatedAny<*>) {
                if (thing.restartRequired()) {
                    restart = true
                    callback.cancel()
                }
            }
        }
        return restart
    }

    @Environment(EnvType.CLIENT)
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

            ConfigApiImpl.walk(thing, key, 1) { walkable, _, new, thing, prop, _, _ ->
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