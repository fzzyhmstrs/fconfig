/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.util

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import me.fzzyhmstrs.fzzy_config.entry.EntrySuggester
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.command.CommandSource
import net.minecraft.util.Identifier
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * Defines a set of allowable identifiers for use in validation. Also supplies [Suggestions] to generate suggestion popups in-game.
 *
 * NOTE: Expectation is that the predicate and supplier are based on matching sets of information; someone in theory could use the suppliers information to predicate an input, and the predicate could be used on a theoretical "parent" dataset of identifiers to derive the same contents as the supplier. Behavior may be undefined if this isn't the case.
 * @param predicate Predicate&lt;Identifier&gt; - tests a candidate Identifier to see if it is allowable
 * @param supplier Supplier&lt;List&lt;Identifier&gt;&gt; - supplies all allowable identifiers in the form of a list. As typical with suppliers, it is not required but beneficial that the supplier provide a new list on each call
 * @param cache Boolean that determines if [get] calls are cached. This can improve performance on high volume calls where the data doesn't change in the background (like Registries, once populated).
 * @see me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
 * @author fzzyhmstrs
 * @since 0.2.0, added cache flag 0.5.0
 */
class AllowableIdentifiers @JvmOverloads constructor(
    private val predicate: Predicate<Identifier>,
    private val supplier: Supplier<List<Identifier>>,
    private val cache: Boolean = false)
    :
    EntryValidator<Identifier>,
    EntrySuggester<Identifier>
{

    private val cached by lazy {
        supplier.get()
    }

    /**
     * Test the provided Identifier vs. the predicate
     * @param identifier Identifier to test
     * @return Boolean result of the predicate test
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun test(identifier: Identifier): Boolean {
        return predicate.test(identifier)
    }

    /**
     * Supplies the allowable identifier list. As of 0.5.0, can be set to pull a memoized (cached) value.
     * @return List<Identifier> - taken from the Supplier
     * @author fzzyhmstrs
     * @since 0.2.0, added caching 0.5.0
     */
    fun get(): List<Identifier> {
        return if(cache) {
            cached
        } else {
            supplier.get()
        }
    }
    /**
     * Returns Suggestions based on the allowable identifier list and a choice validator
     * @param input String - the current string input to filter suggestions off of
     * @param cursor Int - the index of the typing cursor in the string
     * @param choiceValidator [ChoiceValidator] - additional option filtering provided externally.
     * @return [CompletableFuture] of [Suggestions] to derive a list of suggestion names from
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun getSuggestions(input: String, cursor: Int, choiceValidator: ChoiceValidator<Identifier>): CompletableFuture<Suggestions> {
        val truncatedInput: String = input.substring(0, cursor)
        val builder = SuggestionsBuilder(truncatedInput, truncatedInput.lowercase(Locale.ROOT), 0)
        return CommandSource.suggestIdentifiers(get().filter { choiceValidator.validateEntry(it, EntryValidator.ValidationType.STRONG).isValid() }, builder)
    }

    /**
     * Validates the provided Identifier versus the provided Predicate
     * @param input Identifer - the Identifier to test
     * @param type EntryValidator.ValidationType - whether this is testing with weak or strong validation
     */
    override fun validateEntry(input: Identifier, type: EntryValidator.ValidationType): ValidationResult<Identifier> {
        return if (type == EntryValidator.ValidationType.WEAK)
            ValidationResult.success(input)
        else
            ValidationResult.predicated(input, this.test(input), "Identifier invalid or not allowed")
    }

    companion object {
        /**
         * An AllowableIdentifiers instance that allows any valid Identifier. The supplier provides an empty list, as a list of every valid Identifier is unbounded.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        val ANY = AllowableIdentifiers({ true }, { listOf() })
    }

}