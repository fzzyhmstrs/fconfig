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
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup
import me.fzzyhmstrs.fzzy_config.entry.*
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.impl.config.SearchConfig
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.EntryCreators
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.NavigableTextFieldWidget
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
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.*
import java.util.function.Function
import java.util.function.Supplier
import java.util.function.UnaryOperator
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaConstructor

/**
 * Validation for an arbitrary non-null POJO (Plain Old Java Object). It will create a "mini-config" popup with the same style of list as the main config and section screens. Each field within the object will be validated just like a config, either automatically or purposefully if ValidatedFields are used.
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
open class ValidatedAny<T: Any>(defaultValue: T): ValidatedField<T>(defaultValue), EntryParent, EntryOpener, EntrySearcher {

    private var default: Boolean? = null
    private var changed: Boolean? = null

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<T> {
        return ConfigApi.deserializeFromToml(copyStoredValue(), toml, mutableListOf()).contextualize()
    }

    @Internal
    override fun serialize(input: T): ValidationResult<TomlElement> {
        val errors = mutableListOf<String>()
        return ValidationResult.predicated(ConfigApi.serializeToToml(input, errors), errors.isEmpty(), "Errors encountered while serializing Object: $errors")
    }

    @Internal
    @Suppress("SafeCastWithReturn", "UNCHECKED_CAST")
    override fun deserializedChanged(old: Any?, new: Any?): Boolean {
        old as? T ?: return true
        new as? T ?: return true
        return (ConfigApi.serializeConfig(old, mutableListOf(), 1) != ConfigApi.serializeConfig(new, mutableListOf(), 1))
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
    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        return input::class.java == defaultValue::class.java
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input [T] input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: T): T {
        return try {
            val new = createInstance()
            val toml = serialize(input).get()
            val result = ConfigApi.deserializeFromToml(new, toml, mutableListOf())
            if (result.isError()) storedValue else result.get().config
        } catch(e: Throwable) {
            storedValue //object doesn't have an empty constructor. no prob.
        }
    }

    private fun createInstance(): T {
        val noArgsConstructor = storedValue::class.constructors.singleOrNull { it.parameters.all(KParameter::isOptional) }
            ?: throw IllegalArgumentException("Class should have a single no-arg constructor: $this")
        if (storedValue::class.annotations.firstOrNull { (it is IgnoreVisibility) }?.let { true } == true)
            noArgsConstructor.javaConstructor?.trySetAccessible()
        return noArgsConstructor.callBy(emptyMap())
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        return CustomButtonWidget.builder("fc.validated_field.object".translate()) { openObjectPopup() }.size(110, 20).build()
    }

    @Internal
    override fun open(args: List<String>) {
        openObjectPopup()
    }

    @Internal
    override fun set(input: T) {
        changed = null
        default = null
        super.set(input)
    }

    @Internal
    override fun setAndUpdate(input: T) {
        if (input == get()) return
        val oldVal = get()
        val oldVersion = ConfigApiImpl.serializeToToml(oldVal, mutableListOf(), 1)
        val tVal1 = correctEntry(input, EntryValidator.ValidationType.STRONG)
        val newVersion = ConfigApiImpl.serializeToToml(tVal1.get(), mutableListOf(), 1)
        var oldStr = ""
        var newStr = ""
        for ((key, oldEl) in oldVersion) {
            val newEl = newVersion[key]
            if (newEl != oldEl) {
                newStr += "$key=$newEl "
                oldStr += "$key=$oldEl "
            }
        }
        oldStr = oldStr.trim()
        newStr = newStr.trim()
        set(tVal1.get())
        val message = if (tVal1.isError()) {
            FcText.translatable("fc.validated_field.update.error", translation(), oldStr, newStr, tVal1.getError())
        } else {
            FcText.translatable("fc.validated_field.update", translation(), oldStr, newStr)
        }
        update(message)
    }

    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun peekState(): Boolean {
        val c = changed ?: deserializedChanged(pushedValue, get())
        changed = c
        return c
    }

    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun isDefault(): Boolean {
        val e = default ?: !deserializedChanged(defaultValue, get())
        default = e
        return e
    }

    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun restore() {
        reset()
        @Suppress("DEPRECATION")
        getUpdateManager()?.addUpdateMessage(this, FcText.translatable("fc.validated_field.default", translation(), ConfigApi.serializeConfig(defaultValue, mutableListOf(), 1).lines().joinToString(" ", transform = {s -> s.trim()})))
    }

    //client
    private fun openObjectPopup() {
        val newThing = copyStoredValue()
        val newNewThing = try{ createInstance() } catch (e: Throwable) { defaultValue }
        val prefix = getEntryKey()
        val manager = ValidatedObjectUpdateManager(newThing, prefix)
        val entries: MutableList<EntryCreator.Creator> = mutableListOf()
        val groups: LinkedList<String> = LinkedList()
        val misc = EntryCreator.CreatorContextMisc()

        fun List<EntryCreator.Creator>.applyToList(functionList: MutableList<EntryCreator.Creator>) {
            functionList.addAll(this)
        }

        ConfigApiImpl.walk(newThing, prefix, 1) { _, _, new, thing, _, annotations, globalAnnotations, callback ->

            val flags = if(thing is EntryFlag) {
                EntryFlag.Flag.entries.filter { thing.hasFlag(it) }
            } else {
                EntryFlag.Flag.NONE
            }

            val entryCreator: EntryCreator?

            val prepareResult = if (thing is EntryCreator) {
                entryCreator = thing
                thing.prepare(new, groups, annotations, globalAnnotations)
                ConfigApiImplClient.prepare(thing, ConfigApiImplClient.getPlayerPermissionLevel(), newThing, prefix, new, annotations, globalAnnotations, false, flags)
            } else if (thing != null) {
                var basicValidation: ValidatedField<*>? = null
                val target = new.removePrefix("$prefix.")
                ConfigApiImpl.drill(newNewThing, target, '.', 1) { _, _, _, thing2, drillProp, drillAnnotations, _, _ ->
                    basicValidation = manager.basicValidationStrategy(thing2, drillProp.returnType, new, drillAnnotations)?.instanceEntry()
                }
                val basicValidation2 = basicValidation
                if (basicValidation2 != null) {
                    basicValidation2.trySet(thing)
                    basicValidation2.setEntryKey(new)
                    entryCreator = basicValidation2
                    basicValidation2.prepare(new, groups, annotations, globalAnnotations)
                    ConfigApiImplClient.prepare(thing, ConfigApiImplClient.getPlayerPermissionLevel(), newThing, prefix, new, annotations, globalAnnotations, false, flags)
                } else {
                    entryCreator = null
                    ConfigApiImplClient.PrepareResult.FAIL
                }
            } else {
                entryCreator = null
                ConfigApiImplClient.PrepareResult.FAIL
            }

            if (!prepareResult.fail) {
                if (prepareResult.cont) {
                    callback.cont()
                }

                if (entryCreator is Updatable) {
                    entryCreator.setUpdateManager(manager)
                    manager.setUpdatableEntry(entryCreator)
                }

                val context = EntryCreator.CreatorContext(new, if (groups.isEmpty()) ConfigGroup.emptyGroups else LinkedList(groups), false, prepareResult.texts, annotations, prepareResult.actions, misc)

                when (prepareResult.perms) {
                    ConfigApiImplClient.PermResult.FAILURE -> {
                        EntryCreators.createNoPermsEntry(context, "noPerms").applyToList(entries)
                    }
                    ConfigApiImplClient.PermResult.OUT_OF_GAME -> {
                        EntryCreators.createNoPermsEntry(context, "outOfGame").applyToList(entries)
                    }
                    else -> {
                        entryCreator?.createEntry(context)?.applyToList(entries)
                    }
                }

                ConfigGroup.pop(annotations, groups)
            }
        }
        manager.pushUpdatableStates()
        val spec = DynamicListWidget.ListSpec(leftPadding = 21, rightPadding = 15, verticalFirstPadding = 2)
        val entryList = DynamicListWidget(MinecraftClient.getInstance(), entries.map { it.entry }, 0, 0, 288, 160, spec)
        val searchField = NavigableTextFieldWidget(MinecraftClient.getInstance().textRenderer, 94, 20, FcText.EMPTY)
        fun setColor(entries: Int) {
            if(entries > 0)
                searchField.setEditableColor(-1)
            else
                searchField.setEditableColor(0xFF5555)
        }
        searchField.setMaxLength(100)
        val searchText = if (SearchConfig.INSTANCE.willPassSearch()) {
            MinecraftClient.getInstance().currentScreen.nullCast<ConfigScreen>()?.getCurrentSearch() ?: ""
        } else {
            ""
        }
        searchField.setChangedListener { s -> setColor(entryList.search(s)) }
        searchField.text = searchText
        searchField.tooltip = Tooltip.of("fc.config.search.desc".translate())
        val popup = PopupWidget.Builder(translation())
            .add("list", entryList, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("search", searchField, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("revert", CustomButtonWidget.builder("fc.button.revert".translate()) { manager.revert() }.size(93, 20).activeSupplier { manager.hasChanges() }.build(), LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .add("restore", CustomButtonWidget.builder("fc.button.restore".translate()) { manager.restore("") }.size(93, 20).activeSupplier { manager.hasRestores("") }.build(), LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .addDoneWidget()
            .onClose { manager.apply(true); if(manager.hasChanges()) setAndUpdate(newThing) }
            .build()
        PopupWidget.push(popup)
    }

    @Internal
    override fun translationKey(): String {
        return (storedValue as? Translatable)?.translationKey()?.takeIf { (storedValue as? Translatable)?.hasTranslation() == true } ?: super.translationKey()
    }

    @Internal
    override fun descriptionKey(): String {
        return (storedValue as? Translatable)?.descriptionKey()?.takeIf { (storedValue as? Translatable)?.hasDescription() == true } ?: super.descriptionKey()
    }

    @Internal
    override fun prefixKey(): String {
        return (storedValue as? Translatable)?.prefixKey()?.takeIf { (storedValue as? Translatable)?.hasPrefix() == true } ?: super.prefixKey()
    }

    @Internal
    override fun translation(fallback: String?): MutableText {
        return  Translatable.getScopedResult(this.getEntryKey())?.name?.nullCast()
            ?: storedValue.nullCast<Translatable>()?.translationOrNull(fallback)
            ?: FcText.translatableWithFallback(translationKey(), fallback ?: this.translationKey().substringAfterLast('.').split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
    }

    @Internal
    override fun description(fallback: String?): MutableText {
        return Translatable.getScopedResult(this.getEntryKey())?.desc?.nullCast()
            ?: storedValue.nullCast<Translatable>()?.descriptionOrNull(fallback)
            ?: FcText.translatableWithFallback(descriptionKey(), fallback ?: "")
    }

    @Internal
    override fun prefix(fallback: String?): MutableText {
        return Translatable.getScopedResult(this.getEntryKey())?.prefix?.nullCast()
            ?: storedValue.nullCast<Translatable>()?.prefixOrNull(fallback)
            ?: FcText.translatableWithFallback(prefixKey(), fallback ?: "")
    }

    @Internal
    override fun hasTranslation(): Boolean {
        return (storedValue as? Translatable)?.hasTranslation()?.let { if(!it && super.hasTranslation()) true else it } ?: super.hasTranslation()
    }

    @Internal
    override fun hasDescription(): Boolean {
        return (storedValue as? Translatable)?.hasDescription()?.let { if(!it && super.hasDescription()) true else it } ?: super.hasDescription()
    }

    @Internal
    override fun hasPrefix(): Boolean {
        return (storedValue as? Translatable)?.hasPrefix()?.let { if(!it && super.hasPrefix()) true else it } ?: super.hasPrefix()
    }

    @Internal
    override fun entryDeco(): Decorated.DecoratedOffset {
        return Decorated.DecoratedOffset(TextureDeco.DECO_OBJECT, 2, 2)
    }

    @Internal
    override fun contentBuilder(context: EntryCreator.CreatorContext): UnaryOperator<ConfigEntry.ContentBuilder> {
        val searchProvider = EntrySearcher.SearchProvider(
            context.misc.get(EntryCreators.CONFIG) ?: "",
            context.misc.get(EntryCreators.CONTENT_BUFFER)?.get()?.let { it.nullCast<ValidatedAny<*>>()?.get() ?: it } ?: "",
            context.scope,
            context.client
        )
        return UnaryOperator { builder ->
            super.contentBuilder(context).apply(builder)
            builder.searchResults(searchProvider)
        }
    }

    @Internal
    override fun searchEntry(config: Any, scope: String, client: Boolean): Function<String, List<Translatable.Result>> {
        return EntrySearcher.SearchProvider(config, this.get(), scope, client)
    }

    @Internal
    override fun actions(): Set<Action> {
        return ConfigApiImpl.getActions(storedValue, ConfigApiImpl.IGNORE_NON_SYNC)
    }

    @Internal
    override fun continueWalk(): Boolean {
        return true
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Walkable[value=${ConfigApi.serializeConfig(get(), mutableListOf(), 1).lines().joinToString(" ", transform = { s -> s.trim() })}, validation=per contained member validation]"
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
                        } catch (e: Throwable) {
                            FC.LOGGER.error("Error pushing update to simple property [$new]", e)
                        }
                    }
                }
            }
        }
    }
}