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

import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A base Entry for configs.
 *
 * Performs 10 basic functions
 * - serialize contents
 * - deserialize input
 * - validate updates
 * - correct errors
 * - provide widgets
 * - apply inputs
 * - supply outputs
 * - create instances
 * - manages flags
 * - accepts listeners
 *
 * @param T the non-null type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0, add EntryFlag 0.5.6, add listenToEntry 0.6.0
 */
@JvmDefaultWithCompatibility
interface Entry<T, E: Entry<T, E>>: EntryHandler<T>, EntryWidget<T>, EntryFlag, Consumer<T>, Supplier<T> {
    /**
     * Creates as deep a copy as possible of this Entry
     * @return [E] new instance of this entry whenever possible, with internal values also deeply copied when possible
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun instanceEntry(): E

    /**
     * Tests an arbitrary input to determine if it is a valid input to this Entry. Inputs are not (necessarily) [Entry], the term is used here in the general sense
     * @param input Nullable, Any value to test for validity
     * @returns if this entry can accept the value. If this returns true, [trySet] should succeed
     * @see trySet
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun isValidEntry(input: Any?): Boolean

    /**
     * Attempts to set an arbitrary input into this Entry. Should fail soft if the input is incompatible
     * @param input Any value to attempt to set
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun trySet(input: Any?)

    /**
     * Applies a listener to this entry. The consumer(s) passed be invoked whenever the value of this [Entry] is updated
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun listenToEntry(listener: Consumer<Entry<T, *>>)
}