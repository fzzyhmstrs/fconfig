/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryFlag
import me.fzzyhmstrs.fzzy_config.entry.EntryOpener
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Consumer
import java.util.function.Function

/**
 * Represents a [ValidatedField] mapped to another value representable by the wrapped delegate
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/Validation#mapping-) for more details and examples.
 * @author fzzyhmstrs
 * @since 0.5.0
 */
open class ValidatedMapped<N, T> @JvmOverloads constructor(protected val delegate: ValidatedField<T>, private val to: Function<T, out N>, private val from: Function<in N, T>, defaultValue: N = to.apply(delegate.get())): ValidatedField<N>(defaultValue), EntryOpener {

    override var storedValue: N
        get() = to.apply(delegate.get())
        set(value) {
            delegate.accept(from.apply(value))
        }

     @Deprecated("Use listenToEntry instead")
     @Suppress("DEPRECATION")
     override fun addListener(listener: Consumer<ValidatedField<N>>) {
        delegate.addListener { _ -> listener.accept(this) }
    }

    override fun listenToEntry(listener: Consumer<Entry<N, *>>) {
        delegate.listenToEntry { _ -> listener.accept(this) }
    }

    @Internal
    @Suppress("DEPRECATION")
    @Deprecated("Implement the override without an errorBuilder. Scheduled for removal in 0.8.0. In 0.7.0, the provided ValidationResult should encapsulate all encountered errors, and all passed errors will be incorporated into a parent result as applicable.")
    override fun deserializeEntry(toml: TomlElement, errorBuilder: MutableList<String>, fieldName: String, flags: Byte): ValidationResult<N> {
        return delegate.deserializeEntry(toml, errorBuilder, fieldName, flags).map(to)
    }

    override fun deserializeEntry(toml: TomlElement, fieldName: String, flags: Byte): ValidationResult<N> {
        return delegate.deserializeEntry(toml, fieldName, flags).map(to)
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<N> {
        return delegate.deserialize(toml, fieldName).map(to)
    }

    @Internal
    @Deprecated("Implement the override using ValidationResult.ErrorEntry.Mutable. Scheduled for removal in 0.8.0.")
    override fun serializeEntry(input: N?, errorBuilder: MutableList<String>, flags: Byte): TomlElement {
        return if(input == null) {
            @Suppress("DEPRECATION")
            delegate.serializeEntry(null, errorBuilder, flags)
        } else {
            @Suppress("DEPRECATION")
            delegate.serializeEntry(from.apply(input), errorBuilder, flags)
        }
    }

    override fun serializeEntry(input: N?, flags: Byte): ValidationResult<TomlElement> {
        return if(input == null) {
            delegate.serializeEntry(null, flags)
        } else {
            delegate.serializeEntry(from.apply(input), flags)
        }
    }

    @Internal
    override fun serialize(input: N): ValidationResult<TomlElement> {
        return delegate.serialize(from.apply(input))
    }

    @Internal
    override fun correctEntry(input: N, type: EntryValidator.ValidationType): ValidationResult<N> {
        return delegate.correctEntry(from.apply(input), type).map(to)
    }

    @Internal
    override fun validateEntry(input: N, type: EntryValidator.ValidationType): ValidationResult<N> {
        return delegate.validateEntry(from.apply(input), type).map(to)
    }

    @Internal
    override fun instanceEntry(): ValidatedField<N> {
        return ValidatedMapped(delegate.instanceEntry(), to, from, defaultValue)
    }

    @Suppress("UNCHECKED_CAST")
    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        try {
            return delegate.isValidEntry(from.apply(input as? N ?: return false))
        } catch (e: Throwable) {
            return false
        }
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input [N] input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: N): N {
        return to.apply(delegate.copyValue(from.apply(input)))
    }

    @Internal
    override fun widgetEntry(choicePredicate: ChoiceValidator<N>): ClickableWidget {
        return delegate.widgetEntry(choicePredicate.convert(from, from))
    }

    @Internal
    override fun open(args: List<String>) {
        delegate.nullCast<EntryOpener>()?.open(args)
    }

    @Internal
    override fun setFlag(flag: Byte) {
        delegate.setFlag(flag)
    }

    @Internal
    override fun setFlag(flag: EntryFlag.Flag) {
        setFlag(flag.flag)
    }

    @Internal
    override fun hasFlag(flag: EntryFlag.Flag): Boolean {
        return delegate.hasFlag(flag)
    }

    /**
     * Gets the wrapped result value from the mapped delegate
     * @return N. The result being wrapped and passed by this ValidationResult.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    override fun get(): N {
        return to.apply(delegate.get())
    }

    /**
     * updates the mapped value. NOTE: this method will push updates to an UpdateManager, if any. For in-game updating consider [validateAndSet]
     *
     * This method is implemented from [java.util.function.Consumer].
     * @param input new value to wrap
     * @see validateAndSet
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    override fun accept(input: N) {
        delegate.accept(from.apply(input))
    }

    @Internal
    @Suppress("DEPRECATION")
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun setUpdateManager(manager: UpdateManager) {
        val newManager = ForwardingUpdateManager(manager)
        delegate.setUpdateManager(newManager)
    }

    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun getEntryKey(): String {
        return delegate.getEntryKey()
    }

    @Internal
    @Deprecated("Internal Method, don't Override unless you know what you are doing!")
    override fun setEntryKey(key: String) {
        delegate.setEntryKey(key)
    }

    private inner class ForwardingUpdateManager(private val forwardTo: UpdateManager): UpdateManager {

        override fun update(updatable: Updatable, updateMessage: Text) {
            forwardTo.update(this@ValidatedMapped, updateMessage)
        }

        override fun hasUpdate(scope: String): Boolean {
            return false
        }

        override fun getUpdate(scope: String): Updatable? {
            return null
        }

        override fun addUpdateMessage(key: Updatable, text: Text) {
            forwardTo.addUpdateMessage(this@ValidatedMapped, text)
        }

        override fun hasChangeHistory(): Boolean {
            return false
        }

        override fun changeHistory(): List<String> {
            return emptyList()
        }

        override fun changeCount(): Int {
            return 0
        }

        override fun restoreCount(scope: String): Int {
            return 0
        }

        override fun restore(scope: String) {
        }

        override fun forwardsCount(): Int {
            return 0
        }

        override fun forwardsHandler() {
        }

        override fun revert() {
        }

        override fun revertLast() {
        }

        override fun apply(final: Boolean) {
        }

        override fun flush(): List<String> {
            return emptyList()
        }

    }
}