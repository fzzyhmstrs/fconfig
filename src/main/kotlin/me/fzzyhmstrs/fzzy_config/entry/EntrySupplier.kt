package me.fzzyhmstrs.fzzy_config.entry

import java.util.function.Supplier

/**
 * Supplies values from a complex [Entry]
 *
 * SAM: [supplyEntry] supplies an instance of T
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@FunctionalInterface
@JvmDefaultWithCompatibility
fun interface EntrySupplier<T>: Supplier<T> {
    fun supplyEntry(): T{
        return get()
    }

}