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
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.also
import net.peanuuutz.tomlkt.TomlElement

/**
 * Deserializes individual entries in a complex [Entry]
 *
 * SAM: [deserializeEntry] takes a TomlElement, errorBuilder list, fieldName, and ignoreNonSync boolean, returns [ValidationResult]<T> with the deserialized or fallback value
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0, added [deserializedChanged] 0.6.0
 */
@FunctionalInterface
@JvmDefaultWithCompatibility
fun interface EntryDeserializer<T> {
    /**
     * Deserializes the provided [TomlElement]. This deserialization should store the result within this deserializer (deserialize "in-place") as well as returning the result. The return has to have a fallback value.
     * @param toml [TomlElement] incoming data to deserialize. This should be deserialized both into this object and returned
     * @param errorBuilder List of error strings. Deserialization should fail softly, returning a fallback and reporting error messages to this builder instead of crashing
     * @param fieldName String scope of the field being deserialized
     * @param flags deserialization flags for use with built-in deserialization methods if needed.
     * @return [ValidationResult]&lt;[T]&gt; wrapped deserialization result or a fallback value on total failure, with any applicable direct error messages stored in the result. The [errorBuilder] can be used for populating detail error information while providing a general alert in this error.
     * @author fzzyhmstrs
     * @since 0.2.0, deprecated 0.7.0 and scheduled for removal by 0.8.0
     */
    @Deprecated("Implement the override without an errorBuilder. Scheduled for removal in 0.8.0. In 0.7.0, the provided ValidationResult should encapsulate all encountered errors, and all passed errors will be incorporated into a parent result as applicable.")
    fun deserializeEntry(toml: TomlElement, errorBuilder: MutableList<String>, fieldName: String, flags: Byte): ValidationResult<T>

    /**
     * TODO()
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun deserializeEntry(toml: TomlElement, fieldName: String, flags: Byte): ValidationResult<T> {
        val errors: MutableList<String> = mutableListOf()
        @Suppress("DEPRECATION")
        var result = deserializeEntry(toml, errors, fieldName, flags)
        for (error in errors) {
            result = result.also(false, ValidationResult.ErrorEntry.DESERIALIZATION, error)
        }
        return result
    }

    /**
     * Specialized `equals` method for determining if a newly deserialized value is *effectively* equal to its old counterpart.
     *
     * This method should evaluate inputs as if they are being compared with `A.equals(B)`, *even if they don't implement equals themselves*. This method should include mechanisms for providing equals like behavior for inputs either way. For example, the method might re-serialize both inputs to check the serialized forms for structural equality.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun deserializedChanged(old: Any?, new: Any?): Boolean {
        return old != new
    }
}