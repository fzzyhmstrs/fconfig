package me.fzzyhmstrs.fzzy_config.validation.entry

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import java.util.function.Predicate

/**
 * Supplies values from a complex [Entry]
 *
 * SAM: [supplyEntry] supplies an instance of T
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@FunctionalInterface
fun interface EntrySupplier<T> {
    fun supplyEntry(): T

}