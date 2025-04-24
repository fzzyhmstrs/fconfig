/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.entry

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.peanuuutz.tomlkt.TomlElement

/**
 * Deserializes individual entries in a complex [Entry]
 *
 * SAM: [serializeEntry] takes a nullable input, error builder list, and ignoreNonSync boolean, returns a serialized TomlElement. Expectation is that if input is null, the Entry will serialize its own stored T, otherwise it will handle serialization of the externally provied T
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.1.1
 */
@FunctionalInterface
@JvmDefaultWithoutCompatibility
fun interface EntrySerializer<T> {
    /**
     * Serializes either the provided input or stored value to a [TomlElement]
     *
     * If the input is not null, it should be serialized, otherwise the stored value of this serializer (or a fallback value) should be serialized. Serialization of the correct type should occur either way.
     * @param input [T], nullable. The optional external value to serialize
     * @param errorBuilder List for appending error messages. Serialization should fail soft, returning a fallback TomlElement or [TomlNull][net.peanuuutz.tomlkt.TomlNull] as a last resort instead of crashing. Problems should be appended to the builder.
     * @param flags serialization flags for passing to built in serialization methods as needed.
     * @return [TomlElement] with the serialized result.
     * @author fzzyhmstrs
     * @since 0.1.1, deprecated 0.7.0 and scheduled for removal 0.8.0
     */
    @Deprecated("Implement the override using ValidationResult.ErrorEntry.Mutable. Scheduled for removal in 0.8.0.")
    fun serializeEntry(input: T?, errorBuilder: MutableList<String>, flags: Byte): TomlElement

    /**
     * Serializes either the provided input or stored value to a [TomlElement]
     *
     * If the input is not null, it should be serialized, otherwise the stored value of this serializer (or a fallback value) should be serialized. Serialization of the correct type should occur either way.
     * @param input [T], nullable. The optional external value to serialize
     * @param errorBuilder [ValidationResult.ErrorEntry.Mutable] for appending error messages. Serialization should fail soft, returning a fallback TomlElement or [TomlNull][net.peanuuutz.tomlkt.TomlNull] as a last resort instead of crashing. Problems should be appended to the builder.
     * @param flags serialization flags for passing to built in serialization methods as needed.
     * @return validation result wrapping a [TomlElement] with the serialized result.
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun serializeEntry(input: T?, flags: Byte): ValidationResult<TomlElement> {
        val errors: MutableList<String> = mutableListOf()
        @Suppress("DEPRECATION")
        val result = serializeEntry(input, errors, flags)
        if (errors.isNotEmpty()) {
            val err = ValidationResult.ErrorEntry.empty().mutable()
            for (error in errors) {
                err.addError(ValidationResult.ErrorEntry.SERIALIZATION, error)
            }
            return ValidationResult.ofMutable(result, err)
        } else {
            return ValidationResult.success(result)
        }
    }
}