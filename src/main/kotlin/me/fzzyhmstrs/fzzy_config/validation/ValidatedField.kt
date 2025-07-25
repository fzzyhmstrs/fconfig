/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.entry.EntryFlag
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.context.ContextAction
import me.fzzyhmstrs.fzzy_config.screen.context.ContextResultBuilder
import me.fzzyhmstrs.fzzy_config.screen.context.ContextType
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.EntryCreators
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.Popups
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.TranslatableEntry
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.outmap
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedSet
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition.*
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedMapped
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedPair
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlNull
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.*
import java.util.function.Function
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

/**
 * Validated Field Collection - serialization is indistinguishable from their wrapped values, but deserialized into a validated wrapper
 *
 * Validated Fields CANNOT be serialized and deserialized by GSON or other "automagic" serializers properly. The Toml Element does not provide enough context, because the validation is hidden within code only, not serialized. These fields are not building new classes from scratch, they are updating and validating a pre-existing default class framework.
 *
 * Helper methods are provided to more easily sync configs directly via [PacketByteBuf]s, rather than serializing and then deserializing the entire JSON
 * @param T Type of the wrapped value
 * @param storedValue T. The wrapped value that this field validates, serializes, and syncs between server and client.
 * @param defaultValue T. The default value of the wrapped value
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTexts.lang
 * @author fzzyhmstrs
 * @since 0.1.0
 */
@Suppress("DeprecatedCallableAddReplaceWith")
abstract class ValidatedField<T>(protected open var storedValue: T, protected var defaultValue: T = storedValue):
    Entry<T, ValidatedField<T>>,
    Updatable,
    TranslatableEntry,
    EntryCreator
{

    protected var pushedValue: T? = null
    override var translatableEntryKey: String = ""
    private var updateManager: UpdateManager? = null
    private var listener: Consumer<Entry<T, *>>? = null
    protected var flags: Byte = 0

    /**
     * Attaches a listener to this field. This listener will be called any time the field is written to ("set"). `accept`, `validateAndSet`, `setAndUpdate` and so on will all call the listener.
     * @param listener [Consumer]&lt;ValidatedField&lt;[T]&gt;&gt; called whenever the field changes. This should, generally speaking, not try to further modify the fields state unless there is a method to prevent infinite recursion.
     * @see withListener for an extension function that "passes through"
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    @Deprecated("Use listenToEntry instead")
    open fun addListener(listener: Consumer<ValidatedField<T>>) {
        @Suppress("UNCHECKED_CAST")
        this.listener = listener as? Consumer<Entry<T, *>>
    }

    /**
     * Attaches a listener to this field. This listener will be called any time the field is written to ("set"). `accept`, `validateAndSet`, `setAndUpdate` and so on will all call the listener.
     *
     * Note that Validated Fields are Entry&lt;T, *&gt;
     * @param listener [Consumer]&lt;[Entry]&lt;[T], *&gt;&gt; called whenever the field changes. This should, generally speaking, not try to further modify the fields state unless there is a method to prevent infinite recursion.
     * @see withListener for an extension function that "passes through"
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun listenToEntry(listener: Consumer<Entry<T, *>>) {
        this.listener = listener
    }

    /////////////// SERIALIZATION /////////////////

    @Internal
    @Deprecated("Implement the override without an errorBuilder. Scheduled for removal in 0.8.0. In 0.7.0, the provided ValidationResult should encapsulate all encountered errors, and all passed errors will be incorporated into a parent result as applicable.")
    override fun deserializeEntry(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        flags: Byte
    ): ValidationResult<T> {
        val tVal = deserialize(toml, fieldName) //1
        if (tVal.isCritical()){ //2
            @Suppress("DEPRECATION")
            return ValidationResult.error(get(), ValidationResult.Errors.DESERIALIZATION) { b -> b.content("Exception deserializing entry [$fieldName], using default value [${get()}]").addError(tVal) }.report(errorBuilder)
        }
        val tVal2 = tVal.outmap { correctEntry(it, EntryValidator.ValidationType.WEAK) } //3
        set(tVal2.get()) //4
        if (tVal2.isCritical()) { //5
            @Suppress("DEPRECATION")
            return ValidationResult.error(get(), ValidationResult.Errors.DESERIALIZATION) { b -> b.content("Config entry [$fieldName] had validation errors, corrected to [${get()}]").addError(tVal2) }.report(errorBuilder)
        }
        @Suppress("DEPRECATION")
        return ValidationResult.predicated(get(), tVal2.isValid(), ValidationResult.Errors.DESERIALIZATION) { b -> b.content("Encountered non-critical errors while deserializing entry $fieldName").addError(tVal2) }.report(errorBuilder)
    }

    @Internal
    override fun deserializeEntry(toml: TomlElement, fieldName: String, flags: Byte): ValidationResult<T> {
        val tVal = deserialize(toml, fieldName) //1
        if (tVal.isCritical()) { //2
            return ValidationResult.error(get(), ValidationResult.Errors.DESERIALIZATION) { b ->
                b.content("Exception deserializing entry [$fieldName], using default value [${get()}]").addError(tVal)
            }
        }
        val tVal2 = tVal.outmap { correctEntry(it, EntryValidator.ValidationType.WEAK) } //3
        set(tVal2.get()) //4
        if (tVal2.isCritical()) { //5
            return ValidationResult.error(get(), ValidationResult.Errors.DESERIALIZATION) { b ->
                b.content("Exception correcting deserialized entry [$fieldName], using value [${get()}]").addError(tVal2)
            }
        }
        return ValidationResult.predicated(get(), tVal2.isValid(), ValidationResult.Errors.DESERIALIZATION) { b ->
            b.content("Encountered non-critical errors while deserializing entry $fieldName").addError(tVal2)
        }
    }

    /**
     * deserializes the fields stored value from TomlElement. This should not set the fields stored value, or interact with the field at all except to get the stored value for error reporting. [deserializeEntry] handles that.
     *
     * Any of the built-in validations can be used for inspiration and help in parsing Toml Elements or using ValidationResult.
     * @param toml [TomlElement] element to deserialize from.
     * @param fieldName String representation of the field name in the config, for error reporting
     * @return [ValidationResult]&lt;[T]&gt; - result of the deserialization. If there is a problem, report a `ValidationResult.error`, using the fields current stored value, and the fieldName as relevant to the error message.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    abstract fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<T>

    @Internal
    @Deprecated("Implement the override returning a ValidationResult. Scheduled for removal in 0.8.0.")
    override fun serializeEntry(input: T?, errorBuilder: MutableList<String>, flags: Byte): TomlElement {
        @Suppress("DEPRECATION")
        return (if(input != null) serialize(input) else serialize(get())).report(errorBuilder).get()
    }

    @Internal
    override fun serializeEntry(input: T?, flags: Byte): ValidationResult<TomlElement> {
        return if(input != null) serialize(input) else serialize(get())
    }

    /**
     * Serializes the provided input to a [TomlElement]
     * @param input [T] the value to serialize. This may not be the stored value, if this validation is being used as a parser for something else.
     * @return [ValidationResult]&lt;[TomlElement]&gt; - the resulting TomlElement, or a [TomlNull][net.peanuuutz.tomlkt.TomlNull] along with an error message if there is a problem.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    abstract fun serialize(input: T): ValidationResult<TomlElement>

    @Internal
    @Deprecated("Scheduled for removal 0.8.0")
    fun trySerialize(input: Any?, errorBuilder: MutableList<String>, flags: Byte): TomlElement? {
        return try {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            serializeEntry(input as T?, errorBuilder, flags)
        } catch (e: Throwable) {
            errorBuilder.add("Incompatible input type. Field of type ${this::class.jvmName} can't accept input of type ${input?.let { it::class.jvmName }}")
            null
        }
    }

    @Internal
    fun trySerialize(input: Any?, flags: Byte): ValidationResult<TomlElement> {
        return try {
            @Suppress("UNCHECKED_CAST")
            serializeEntry(input as T?, flags)
        } catch (e: Throwable) {
            ValidationResult.error(TomlNull, ValidationResult.Errors.SERIALIZATION) { b -> b.content("Incompatible input type. Field of type ${this::class.jvmName} can't accept input of type ${input?.let { it::class.jvmName }}").exception(e) }
        }
    }

    override fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        return validateEntry(input, type)
    }

    override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        return ValidationResult.success(input)
    }

    /////////// END SERIALIZATION /////////////////

    /////////////// FLAGS /////////////////////////

    internal open fun setFlag(flag: Byte) {
        if (hasFlag(flag)) return
        this.flags = (this.flags + flag).toByte()
    }

    private fun hasFlag(flag: Byte): Boolean {
        return (this.flags and flag) == flag
    }

    protected fun compositeFlags(other: EntryFlag) {
        this.flags = this.flags or other.flags()
    }

    override fun setFlag(flag: EntryFlag.Flag) {
        setFlag(flag.flag)
    }

    override fun hasFlag(flag: EntryFlag.Flag): Boolean {
        return this.hasFlag(flag.flag)
    }

    override fun flags(): Byte {
        return flags
    }

    /////////// END FLAGS /////////////////////////

    /////////////// GET & SET /////////////////////

    /**
     * supplies the wrapped value
     *
     * This method is implemented from [java.util.function.Supplier].
     * @return This field wrapped value
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    override fun get(): T {
        return storedValue
    }

    /**
     * Provides this validations default value
     * @return the default value
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun getDefault(): T {
        return defaultValue
    }

    @Internal
    protected open fun set(input: T) {
        storedValue = input
        listener?.accept(this)
    }

    /**
     * updates the wrapped value. NOTE: this method will push updates to an UpdateManager, if any. For in-game updating consider [validateAndSet]
     *
     * This method is implemented from [java.util.function.Consumer].
     * @param input new value to wrap
     * @see validateAndSet
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    override fun accept(input: T) {
        setAndUpdate(input)
    }

    override fun trySet(input: Any?) {
        try {
            @Suppress("UNCHECKED_CAST")
            setAndUpdate(input as T)
        } catch (e: Throwable) {
            //
        }
    }

    /**
     * A setter method for the [storedValue] that first validates the value being set and then stores the post-validation result.
     *
     * Flags will be ignored. Validation will be weak, listener will be called if present, and sync state will not be updated.
     * @param input T. the pre-validation input of type T that will be validated and then stored, where T is the type of the wrapped value in this field.
     * @return ValidationResult of the input, after corrections, with applicable error messages.
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    open fun validateAndSet(input: T): ValidationResult<T> {
        if (input == get()) return ValidationResult.success(get())
        val tVal1 = correctEntry(input, EntryValidator.ValidationType.WEAK)
        set(tVal1.get())
        if (tVal1.isCritical()) {
            return ValidationResult.error(get(), ValidationResult.Errors.BASIC) { b -> b.content("Exception validating and setting input [$input]. Setting to [${get()}]").addError(tVal1) }
        }
        return ValidationResult.predicated(get(), tVal1.isValid(), ValidationResult.Errors.BASIC) { b -> b.content("Encountered non-critical errors validating and setting input [$input]").addError(tVal1) }
    }

    /**
     * A setter method for the [storedValue] that first validates the value being set and then stores the post-validation result.
     *
     * Flags applied to this field or passed into this method alter the normal behavior of [validateAndSet]
     * - [EntryFlag.Flag.QUIET]: Will not call listeners
     * - [EntryFlag.Flag.STRONG]: Will use strong validation as opposed to the normal weak validation
     * - [EntryFlag.Flag.UPDATE]: Sync state will be updated as if changed in a GUI directly. This should only be used when the field has had it's state pushed (typically while a config GUI is open).
     * @param input T. the pre-validation input of type T that will be validated and then stored, where T is the type of the wrapped value in this field.
     * @return ValidationResult of the input, after corrections, with applicable error messages.
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    open fun validateAndSetFlagged(input: T, vararg flag: EntryFlag.Flag): ValidationResult<T> {
        if (input == get()) return ValidationResult.success(get())
        val tVal1 = if (hasFlag(EntryFlag.Flag.STRONG) || flag.contains(EntryFlag.Flag.STRONG)) {
            correctEntry(input, EntryValidator.ValidationType.STRONG)
        } else {
            correctEntry(input, EntryValidator.ValidationType.WEAK)
        }
        if (hasFlag(EntryFlag.Flag.UPDATE) || flag.contains(EntryFlag.Flag.UPDATE)) {
            val message = if (tVal1.isError() || tVal1.isCritical()) {
                FcText.translatable("fc.validated_field.update.error", translation(), get().toString(), tVal1.get().toString(), tVal1.getError())
            } else {
                updateMessage(get(), tVal1.get())
            }
            update(message)
        }
        if (flag.contains(EntryFlag.Flag.QUIET)) {
            storedValue = tVal1.get()
        } else {
            set(tVal1.get())
        }
        if (tVal1.isCritical()) {
            return ValidationResult.error(get(), ValidationResult.Errors.BASIC) { b -> b.content("Exception validating and setting input [$input] with flags $flags. Setting to [${get()}]").addError(tVal1) }
        }
        return ValidationResult.predicated(get(), tVal1.isValid(), ValidationResult.Errors.BASIC) { b -> b.content("Encountered non-critical errors validating and setting input [$input] with flags $flags").addError(tVal1) }
    }

    @Internal
    open fun setAndUpdate(input: T) {
        if (input == get()) return
        val oldVal = get()
        val tVal1 = correctEntry(input, EntryValidator.ValidationType.STRONG)
        set(tVal1.get())
        val message = if (tVal1.isError() || tVal1.isCritical()) {
            FcText.translatable("fc.validated_field.update.error", translation(), oldVal.toString(), get().toString(), tVal1.getError())
        } else {
            updateMessage(oldVal, get())
        }
        update(message)
    }

    /////////// END GET & SET /////////////////////

    /////////////// UPDATES ///////////////////////

    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun getUpdateManager(): UpdateManager? {
        return updateManager
    }

    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun setUpdateManager(manager: UpdateManager) {
        this.updateManager = manager
    }

    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun isDefault(): Boolean {
        return !deserializedChanged(defaultValue, get())
    }

    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun restore() {
        reset()
        updateManager?.addUpdateMessage(this, FcText.translatable("fc.validated_field.default", translation(), defaultValue.toString()))
    }

    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun revert() {
        if(pushedValue != null) {
            try {
                pushedValue?.let {
                    updateManager?.addUpdateMessage(this, FcText.translatable("fc.validated_field.revert", translation(), get().toString(), pushedValue.toString()))
                    set(it)
                }
            } catch (e: Throwable) {
                updateManager?.addUpdateMessage(this, FcText.translatable("fc.validated_field.revert.error", translation(), e.localizedMessage))
            }
        } else {
            updateManager?.addUpdateMessage(this, FcText.translatable("fc.validated_field.revert.error", translation(), "Unexpected null PushedState."))
        }
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun pushState() {
        pushedValue = copyStoredValue()
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun peekState(): Boolean {
        return deserializedChanged(pushedValue, get())
    }
    /**
     * @suppress
     */
    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun popState(): Boolean {
        if (pushedValue == null) return false
        val updated = deserializedChanged(pushedValue, get())
        pushedValue = null
        return updated
    }

    /**
     * Copies the stored value and returns it.
     *
     * In the default implementation, the value isn't actually copied; there is no way to copy in a generic fashion. Many subclasses of ValidatedField do truly make copies in varying levels of deep and shallow.
     * @return [T] copied value instance
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    open fun copyStoredValue(): T {
        return copyValue(get())
    }

    private fun updateDefault(newDefault: T) {
        this.defaultValue = newDefault
    }

    protected open fun reset() {
        setAndUpdate(defaultValue)
    }

    protected open fun updateMessage(old: T, new: T): Text {
        return FcText.translatable("fc.validated_field.update", translation(), old.toString(), new.toString())
    }

    /////////// END UPDATES ///////////////////////

    /////////////// TRANSLATION ///////////////////

    @Internal
    override fun translation(fallback: String?): MutableText {
        return Translatable.getScopedResult(this.getEntryKey())?.name?.nullCast() ?: FcText.translatableWithFallback(translationKey(), fallback ?: this.translationKey().substringAfterLast('.').split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
    }

    /////////// END TRANSLATION ///////////////////

    /////////////// ENTRY CREATION ////////////////

    /**
     * Defines a decoration for the entry
     * @return [Decorated.DecoratedOffset] containing the decoration and pixel xy alignment offsets.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    protected open fun entryDeco(): Decorated.DecoratedOffset? {
        return null
    }

    /**
     * Allows for modification of the entry content. If you want to add onto the super calls operator, consider [UnaryOperator.andThen] or [UnaryOperator.compose]
     * @param context [EntryCreator.CreatorContext] context specific to the entry being built
     * @return [UnaryOperator]&lt;[ConfigEntry.ContentBuilder]&gt;
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    protected open fun contentBuilder(context: EntryCreator.CreatorContext): UnaryOperator<ConfigEntry.ContentBuilder> {
        val deco = entryDeco()
        return UnaryOperator { builder ->
            builder.layoutContent { contentLayout ->
                contentLayout.add(
                    "widget",
                    widgetEntry(),
                    LayoutWidget.Position.ALIGN_JUSTIFY,
                    LayoutWidget.Position.BELOW)
            }
            if (deco != null)
                builder.decoration(deco.decorated, deco.offsetX, deco.offsetY)
            builder
        }
    }

    /**
     * Builds a set of grouped context actions to pass into this fields GUI entry
     *
     * Call super first and add into the returned result if you want to add new content to the base actions. (Unless you specifically don't want the super-actions of course)
     * - Group: string id used to cluster similar context actions together in a context menu
     * - [ContextType]: key for the action, used simply as map keys here for organization
     * - [ContextAction.Builder]: builder of the action to perform. This is a builder for 2 reasons; most aspects of FCs GUIs use a lazy instantiation pattern, and this allows subclasses or parents to modify the builder as needed.
     * @param context [EntryCreator.CreatorContext] context specific to the entry being built
     * @return Map of groups to Maps of [ContextType] to [ContextAction.Builder]
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    @Suppress("DEPRECATION")
    protected open fun contextActionBuilder(context: EntryCreator.CreatorContext): MutableMap<String, MutableMap<ContextType, ContextAction.Builder>> {
        val map: MutableMap<ContextType, ContextAction.Builder> = mutableMapOf()
        val copy = ContextAction.Builder("fc.button.copy".translate()) {
            context.misc.get(EntryCreators.COPY_BUFFER)?.set(this.get())
            true }
            .icon(TextureDeco.CONTEXT_COPY)
        val paste = ContextAction.Builder("fc.button.paste".translate()) {
            context.misc.get(EntryCreators.COPY_BUFFER)?.get()?.let { this.trySet(it) }
            true }
            .active { this.isValidEntry(context.misc.get(EntryCreators.COPY_BUFFER)?.get()) }
            .icon(TextureDeco.CONTEXT_PASTE)
        val revert = ContextAction.Builder("fc.button.revert".translate()) { this.revert(); true }
            .active {  this.peekState() }
            .icon(TextureDeco.CONTEXT_REVERT)
        val restore = ContextAction.Builder("fc.button.restore".translate()) { b ->
            Popups.openConfirmPopup(b, "fc.config.restore.confirm.desc".translate()) { this.restore() }
            true }
            .active {  !this.isDefault() }
            .icon(TextureDeco.CONTEXT_RESTORE)

        map[ContextType.COPY] = copy
        map[ContextType.PASTE] = paste
        map[ContextType.REVERT] = revert
        map[ContextType.RESTORE] = restore

        if (context.client) {
            val forward = ContextAction.Builder("fc.button.forward".translate()) { Popups.openEntryForwardingPopup(this); true }
                .icon(TextureDeco.CONTEXT_FORWARD)
            map[ContextType.FORWARD] = forward
        }
        val map2: MutableMap<String, MutableMap<ContextType, ContextAction.Builder>> = mutableMapOf()
        map2[ContextResultBuilder.ENTRY] = map
        return map2
    }

    @Internal
    override fun createEntry(context: EntryCreator.CreatorContext): List<EntryCreator.Creator> {
        val function: BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry> = BiFunction { listWidget, _ ->
            val contentBuilder = ConfigEntry.ContentBuilder(context)
            contentBuilder.contextActions(contextActionBuilder(context))
            contentBuilder(context).apply(contentBuilder)
            ConfigEntry(listWidget, contentBuilder.build(), context.texts)
        }
        return listOf(EntryCreator.Creator(context.scope, context.texts, function))
    }

    /////////// END ENTRY CREATION ////////////////

    /////////////// MAPPING ///////////////////////

    /**
     * Wraps the provided values into a [ValidatedList] with this field as validation
     * @param elements the inputs for the list generation. Same type as this field
     * @return [ValidatedList] wrapping the provided values and this field as validation
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.lists
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun toList(vararg elements: T): ValidatedList<T> {
        return ValidatedList(listOf(*elements), this)
    }

    /**
     * Wraps the provided collection into a [ValidatedList] with this field as validation
     * @param collection the collection to wrap. Same type as this field
     * @return [ValidatedList] wrapping the collection and this field as validation
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.lists
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun toList(collection: Collection<T>): ValidatedList<T> {
        return ValidatedList(collection.toList(), this)
    }

    /**
     * Wraps the provided values into a [ValidatedSet] with this field as validation
     * @param elements the inputs for the set generation. Same type as this field
     * @return [ValidatedSet] wrapping the provided values and this field as validation
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.sets
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    fun toSet(vararg elements: T): ValidatedSet<T> {
        return ValidatedSet(setOf(*elements), this)
    }

    /**
     * Wraps the provided collection into a [ValidatedSet] with this field as validation
     * @param collection the collection to wrap. Same type as this field
     * @return [ValidatedSet] wrapping the collection and this field as validation
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.sets
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    fun toSet(collection: Collection<T>): ValidatedSet<T> {
        return ValidatedSet(collection.toSet(), this)
    }

    /**
     * Pairs this validation with another validation into one [ValidatedPair]
     *
     * Note that the resulting entry in the GUI will have two widgets "squashed" next to each other with the default pair layout, making each widget 53 px wide instead of the normal 110. Pick widgets that work within that constraint, or consider using the ValidatedPair constructor that lets you pick the layout type.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun <B> pairWith(other: Entry<B, *>): ValidatedPair<T, B> {
        return ValidatedPair(ValidatedPair.Tuple(this.storedValue, other.get()), this, other)
    }

    /**
     * Maps this validation to a new convertible type. The default value will be applied from this delegates current storedValue
     *
     * The field will be internally managed by this validation as a delegate, so the serialization will take the form of this validation, the widget will be from this, and so on.
     * @param N the new type to map to
     * @param to [Function]&lt;T, [N]&gt; - maps values from this delegate into the new type
     * @param from [Function]&lt;[N], T&gt; - maps values of the new type back into the type of this delegate
     * @return ValidatedMapped&lt;[N]&gt; - A Mapped validation that provides and receives the new type with this as a delegate
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun <N> map(to: Function<T, out N>, from: Function<in N, T>): ValidatedField<N> {
        return ValidatedMapped(this, to, from)
    }

    /**
     * Maps this validation to a new convertible type.
     *
     * The field will be internally managed by this validation as a delegate, so the serialization will take the form of this validation, the widget will be from this, and so on.
     * @param N the new type to map to
     * @param defaultValue [N] - the default value of the new type
     * @param to [Function]&lt;T, [N]&gt; - maps values from this delegate into the new type
     * @param from [Function]&lt;[N], T&gt; - maps values of the new type back into the type of this delegate
     * @return ValidatedMapped&lt;[N]&gt; - A Mapped validation that provides and receives the new type with this as a delegate
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun <N> map(defaultValue: N, to: Function<T, out N>, from: Function<in N, T>): ValidatedField<N> {
        this.updateDefault(from.apply(defaultValue))
        this.set(from.apply(defaultValue))
        return  ValidatedMapped(this, to, from, defaultValue)
    }

    /**
     * Maps this validation to a new convertible type.
     *
     * The field will be internally managed by this validation as a delegate, so the serialization will take the form of this validation, the widget will be from this, and so on.
     * @param N the new type to map to
     * @param to [Function]&lt;T, [N]&gt; - maps values from this delegate into the new type
     * @param from [Function]&lt;[N], T&gt; - maps values of the new type back into the type of this delegate
     * @param defaultValue [T] - the default value of this delegates type. Mapped to type [N] with [to]
     * @return ValidatedMapped&lt;[N]&gt; - A Mapped validation that provides and receives the new type with this as a delegate
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun <N> map(to: Function<T, out N>, from: Function<in N, T>, defaultValue: T): ValidatedField<N> {
        this.updateDefault(defaultValue)
        this.set(defaultValue)
        return ValidatedMapped(this, to, from, to.apply(defaultValue))
    }

    /**
     * Provides a [Codec] representing the value type of this validation, backed by the validators within as applicable
     *
     * For example, if you have a double with a validity range 0.0 to 1.0, this will de/serialize the double using the Codec, and enforce the valid range.
     * @return [Codec]&lt;[T]&gt; - Codec of type T backed by this validation.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun codec(): Codec<T> {
        return Codec.STRING.flatXmap(
            { str ->
                val result = ConfigApiImpl.deserializeEntry(this.instanceEntry(), str, "Field Codec", ConfigApiImpl.IGNORE_NON_SYNC)
                if(result.isError())
                    DataResult.error {
                        @Suppress("DEPRECATION")
                        "Deserialization failed, with errors: ${result.getError()}"
                    }
                else
                    DataResult.success(result.get())
            },
            { t ->
                val serializer = this.instanceEntry()
                serializer.trySet(t)
                val result = ConfigApiImpl.serializeEntry(serializer, ConfigApiImpl.CHECK_NON_SYNC)
                if(result.isError())
                    DataResult.error {
                        @Suppress("DEPRECATION")
                        "Serialization failed with errors: ${result.getError()}"
                    }
                else
                    DataResult.success(result.get())
            }
        )
    }

    /**
     * Convert this field to a [ValidatedCondition]. The provided condition (and any others you append) must pass for the stored value to be provided, otherwise the fallback will be supplied.
     * @param condition [Condition] a condition to check before passing the stored value
     * @param fallback [Supplier]&lt;[T]&gt; provides the fallback value to use if the condition fails
     * @throws IllegalStateException if the fallback is this
     * @return this condition
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    open fun toCondition(condition: Condition, fallback: Supplier<T>): ValidatedCondition<T> {
        val newField = ValidatedCondition(this, fallback)
        return newField.withCondition(condition)
    }

    /**
     * Convert this field to a [ValidatedCondition]. The provided condition (and any others you append) must pass for the stored value to be provided, otherwise the fallback will be supplied.
     *
     * Note: a ValidatedField is a supplier. If you want a custom failMessage, this is a valid overload of `withCondition(ValidatedField<Boolean>)`
     * @param condition [Supplier]&lt;Boolean&gt; a supplier of booleans for the condition to check against
     * @param failMessage [Text] a message to provide to a tooltip if a condition isn't met
     * @param fallback [Supplier]&lt;[T]&gt; provides the fallback value to use if the condition fails
     * @throws IllegalStateException if the fallback is this
     * @return this condition
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    open fun toCondition(condition: Supplier<Boolean>, failMessage: Text, fallback: Supplier<T>): ValidatedCondition<T> {
        val newField = ValidatedCondition(this, fallback)
        newField.withCondition(ConditionSupplierImpl(condition, failMessage))
        return newField
    }

    /**
     * Convert this field to a [ValidatedCondition]. The provided condition (and any others you append) must pass for the stored value to be provided, otherwise the fallback will be supplied.
     *
     * Note: a ValidatedField is a supplier. If you want a custom failMessage, this is a valid overload of `withCondition(ValidatedField<Boolean>)`
     * @param condition [BooleanSupplier] a supplier of booleans for the condition to check against
     * @param fallback [Supplier]&lt;[T]&gt; provides the fallback value to use if the condition fails
     * @param failMessage [Text] a message to provide to a tooltip if a condition isn't met
     * @throws IllegalStateException if the fallback is this
     * @return this condition
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    open fun toCondition(condition: BooleanSupplier, fallback: Supplier<T>, failMessage: Text): ValidatedCondition<T> {
        val newField = ValidatedCondition(this, fallback)
        newField.withCondition(ConditionBooleanSupplierImpl(condition, failMessage))
        return newField
    }

    /**
     * Convert this field to a [ValidatedCondition] using the provided validation as a supplier. The provided condition (and any others you append) must pass for the stored value to be provided, otherwise the fallback will be supplied.
     * @param condition [ValidatedField]&lt;Boolean&gt; a condition to check before passing the stored value
     * @throws IllegalStateException if this field is passed into itself
     * @return this condition
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    open fun toCondition(condition: ValidatedField<Boolean>, fallback: Supplier<T>): ValidatedCondition<T> {
        val newField = ValidatedCondition(this, fallback)
        return newField.withCondition(condition, condition.translation())
    }

    /**
     * Convert this field to a [ValidatedCondition] using the provided scope with a default boolean provider. The provided condition (and any others you append) must pass for the stored value to be provided, otherwise the fallback will be supplied. The provided scope must point to a valid boolean config scope otherwise the initial condition will never pass.
     * @param scope String - a config `scope` pointing to a boolean or validated boolean.
     * @param failMessage [Text] a message to provide to a tooltip if a condition isn't met
     * @throws IllegalStateException if the fallback is this
     * @return this condition
     * @author fzzyhmstrs
     * @since 0.5.4
     */
    open fun toCondition(scope: String, failMessage: Text, fallback: Supplier<T>): ValidatedCondition<T> {
        val newField = ValidatedCondition(this, fallback)
        return newField.withCondition(scope, failMessage)
    }

    /////////// END MAPPING ///////////////////////

    internal fun argumentType(): KType? {
        var superType: KType? = null
        this::class.allSupertypes.filter { it.jvmErasure == ValidatedField::class }.forEach { superType = it }
        return superType?.arguments?.get(0)?.type
    }

    companion object {

        /**
         * Attaches a listener to the receiver field. This listener will be called any time the field is written to ("set"). `accept`, `validateAndSet`, `setAndUpdate` and so on will all call the listener.
         * @param [F] the subtype of [ValidatedField]
         * @param listener [Consumer]&lt;[F]&gt; called whenever the field changes. This should, generally speaking, not try to further modify the fields state unless there is a method to prevent infinite recursion.
         * @return [F] the receiver is passed through
         * @author fzzyhmstrs
         * @since 0.5.0
         */
        fun<T, F: ValidatedField<T>> F.withListener(listener: Consumer<F>): F {
            @Suppress("UNCHECKED_CAST") //ok since Consumers type will be erased anyway, and the listener will always be provided with the receiver itself (F)
            this.listenToEntry(listener as Consumer<Entry<T, *>>)
            return this
        }


        fun <T, F: ValidatedField<T>> F.withFlag(flag: EntryFlag.Flag): F {
            this.setFlag(flag.flag)
            return this
        }
    }
}