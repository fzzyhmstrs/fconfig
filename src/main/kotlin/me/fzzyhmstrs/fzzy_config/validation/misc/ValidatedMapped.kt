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

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
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
 * @author fzzyhmstrs
 * @since 0.5.0
 */
class ValidatedMapped<N, T> @JvmOverloads constructor(private val delegate: ValidatedField<T>, private val to: Function<T, out N>, private val from: Function<in N, T>, defaultValue: N = to.apply(delegate.get())): ValidatedField<N>(defaultValue) {

    override var storedValue: N
        get() = to.apply(delegate.get())
        set(value) {
            delegate.accept(from.apply(value))
        }

     override fun addListener(listener: Consumer<ValidatedField<N>>) {
        delegate.addListener { _ -> listener.accept(this) }
    }

    @Internal
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

    /**
     * Gets the wrapped result value from the mapped delegate
     * @return N. The result being wrapped and passed by this ValidationResult.
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    override fun get(): N {
        return to.apply(delegate.get())
    }

    override fun accept(input: N) {
        delegate.accept(from.apply(input))
    }

    @Internal
    @Deprecated("use deserialize to avoid accidentally overwriting validation and error reporting")
    override fun deserializeEntry(toml: TomlElement, errorBuilder: MutableList<String>, fieldName: String, flags: Byte): ValidationResult<N> {
        return delegate.deserializeEntry(toml, errorBuilder, fieldName, flags).map(to)
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<N> {
        return delegate.deserialize(toml, fieldName).map(to)
    }

    @Internal
    @Deprecated(
        "use serialize for consistency and to enable usage in list- and map-based Fields",
        replaceWith = ReplaceWith("serializeEntry(input: T)")
    )
    override fun serializeEntry(input: N?, errorBuilder: MutableList<String>, flags: Byte): TomlElement {
        return if(input == null) {
            delegate.serializeEntry(null, errorBuilder, flags)
        } else {
            delegate.serializeEntry(from.apply(input), errorBuilder, flags)
        }
    }

    override fun serialize(input: N): ValidationResult<TomlElement> {
        return delegate.serialize(from.apply(input))
    }

    override fun instanceEntry(): ValidatedField<N> {
        return ValidatedMapped(delegate.instanceEntry(), to, from, defaultValue)
    }

    override fun correctEntry(input: N, type: EntryValidator.ValidationType): ValidationResult<N> {
        return delegate.correctEntry(from.apply(input), type).map(to)
    }

    override fun validateEntry(input: N, type: EntryValidator.ValidationType): ValidationResult<N> {
        return delegate.validateEntry(from.apply(input), type).map(to)
    }

    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        try {
            return delegate.isValidEntry(from.apply(input as? N ?: return false))
        } catch (e: Throwable) {
            return false
        }
    }

    override fun widgetEntry(choicePredicate: ChoiceValidator<N>): ClickableWidget {
        return delegate.widgetEntry(choicePredicate.convert(from, from))
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
            return listOf()
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
            return listOf()
        }

    }
}