package me.fzzyhmstrs.fzzy_config.validated_field.entry

import me.fzzyhmstrs.fzzy_config.api.ValidationResult

/**
 * Deserializes individual entries in a complex [Entry]
 *
 * SAM: [correctEntry] takes a TomlElement, returns a deserialized instance of T
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@FunctionalInterface
fun interface EntryCorrector<T> {
    fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T>
}