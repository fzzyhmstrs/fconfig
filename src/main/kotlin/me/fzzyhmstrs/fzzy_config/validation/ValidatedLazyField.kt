/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.validation

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.reportTo
import net.peanuuutz.tomlkt.TomlElement
import java.util.function.BiFunction
import java.util.function.Supplier

/**
 * Represents Validation that may want to serialize its data as late as possible (lazily). This is achieved by returning a placeholder during direct deserialization, and then applying the provided handler (which will normally be the deserialize method) the first time that the value is accessed.
 *
 * If the value is accessed to early, deserialization will of course still fail in the manner expected
 * @author fzzyhmstrs
 * @since 0.7.3
 */
abstract class ValidatedLazyField<T: Any>(defaultValue: T, private val placeholder: Supplier<T>): ValidatedField<T>(defaultValue) {

    protected abstract val handler: BiFunction<TomlElement, String, ValidationResult<T>>?

    private var actualStoredValue: T = defaultValue
    private var rawInput: TomlElement? = null
    private var fieldName = "Unknown Entry"

    override var storedValue: T
        get() {
            if (handler != null && rawInput != null) {
                try {
                    actualStoredValue = handler!!.apply(rawInput!!, fieldName).reportTo(ValidationResult.ErrorEntry.ENTRY_ERROR_LOGGER).get()
                    rawInput = null
                } catch (e: Exception) {
                    FC.LOGGER.error("Unexpected error while deserializing a lazy field")
                }
            }
            return actualStoredValue
        }
        set(value) {
            actualStoredValue = value
        }

    @Deprecated("Implement the override without an errorBuilder. Scheduled for removal in 0.8.0. In 0.7.0, the provided ValidationResult should encapsulate all encountered errors, and all passed errors will be incorporated into a parent result as applicable.")
    override fun deserializeEntry(toml: TomlElement, errorBuilder: MutableList<String>, fieldName: String, flags: Byte): ValidationResult<T> {
        if (handler != null) {
            rawInput = toml
            this.fieldName = fieldName
            return ValidationResult.success(placeholder.get())
        }
        @Suppress("DEPRECATION")
        return super.deserializeEntry(toml, errorBuilder, fieldName, flags)
    }

    override fun deserializeEntry(toml: TomlElement, fieldName: String, flags: Byte): ValidationResult<T> {
        if (handler != null) {
            rawInput = toml
            this.fieldName = fieldName
            return ValidationResult.success(placeholder.get())
        }
        return super.deserializeEntry(toml, fieldName, flags)
    }
}