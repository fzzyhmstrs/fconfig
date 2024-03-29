package me.fzzyhmstrs.fzzy_config.validation.entry

/**
 * Applies input values into a complex [Entry]
 *
 * SAM: [applyEntry] consumes an instance of T
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@FunctionalInterface
fun interface EntryApplier<T> {
    fun applyEntry(input: T)

}