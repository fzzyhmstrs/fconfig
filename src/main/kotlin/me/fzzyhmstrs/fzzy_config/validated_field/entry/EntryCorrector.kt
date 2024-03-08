package me.fzzyhmstrs.fzzy_config.validated_field.entry

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field.ValidatedField

/**
 * Deserializes individual entries in a complex [ValidatedField]
 *
 * SAM: [deserialize] takes a TomlElement, returns a deserialized instance of T
 * @author fzzyhmstrs
 * @since 0.1.1
 */
@FunctionalInterface
fun interface EntryCorrector<T> {
    fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T>
}