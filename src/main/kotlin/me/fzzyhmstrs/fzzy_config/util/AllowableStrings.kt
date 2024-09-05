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
import me.fzzyhmstrs.fzzy_config.entry.EntryChecker
import me.fzzyhmstrs.fzzy_config.entry.EntrySuggester
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.command.CommandSource
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * Defines a set of allowable strings for use in validation. Also supplies [Suggestions] to generate suggestion popups in-game.
 *
 * NOTE: Expectation is that the predicate and supplier are based on matching sets of information; someone in theory could use the suppliers information to predicate an input, and the predicate could be used on a theoretical "parent" dataset of string to derive the same contents as the supplier. Behavior may be undefined if this isn't the case.
 * @param predicate Predicate&lt;String&gt; - tests a candidate String to see if it is allowable
 * @param supplier Supplier&lt;List&lt;String&gt;&gt; - supplies all allowable String in the form of a list. As typical with suppliers, it is not required but beneficial that the supplier provide a new list on each call
 * @see me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
 * @author fzzyhmstrs
 * @since 0.2.6
 */
class AllowableStrings(private val predicate: Predicate<String>, private val supplier: Supplier<List<String>>): EntryChecker<String>, EntrySuggester<String> {
    /**
     * Test the provided String vs. the predicate
     * @param str String to test
     * @return Boolean result of the predicate test
     * @author fzzyhmstrs
     * @since 0.2.6
     */
    fun test(str: String): Boolean {
        return predicate.test(str)
    }
    /**
     * Supplies the allowable identifier list
     * @return List<String> - taken from the Supplier
     * @author fzzyhmstrs
     * @since 0.2.6
     */
    fun get(): List<String> {
        return supplier.get()
    }
    /**
     * Returns Suggestions based on the allowable string list and a choice validator
     * @param input String - the current string input to filter suggestions off of
     * @param cursor Int - the index of the typing cursor in the string
     * @param choiceValidator [ChoiceValidator] - additional option filtering provided externally.
     * @return [CompletableFuture] of [Suggestions] to derive a list of suggestion names from
     * @author fzzyhmstrs
     * @since 0.2.6
     */
    override fun getSuggestions(input: String, cursor: Int, choiceValidator: ChoiceValidator<String>): CompletableFuture<Suggestions> {
        val truncatedInput: String = input.substring(0, cursor)
        val builder = SuggestionsBuilder(truncatedInput, truncatedInput.lowercase(Locale.ROOT), 0)
        return CommandSource.suggestMatching(get().filter { choiceValidator.validateEntry(it, EntryValidator.ValidationType.STRONG).isValid() }, builder)
    }

    /**
     * Validates the provided String versus the provided Predicate
     * @param input String - the String to test
     * @param type EntryValidator.ValidationType - whether this is testing with weak or strong validation
     * @return [ValidationResult]&lt;String&gt; - validation result of testing the predicate
     * @author fzzyhmstrs
     * @since 0.2.6
     */
    override fun validateEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
        return if (type == EntryValidator.ValidationType.WEAK)
            ValidationResult.success(input)
        else
            ValidationResult.predicated(input, this.test(input), "Identifier invalid or not allowed")
    }

    /**
     * Validates (not corrects) the provided String versus the provided Predicate
     *
     * This checker doesn't have enough context to know how to make a correction, so just passes validation results on correct.
     * @param input String - the Identifier to test
     * @param type EntryValidator.ValidationType - whether this is testing with weak or strong validation
     * @return [ValidationResult]&lt;String&gt; - validation result of testing the predicate (no correction provided)
     * @author fzzyhmstrs
     * @since 0.2.6
     */
    override fun correctEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
       return validateEntry(input, type)
    }

    companion object {
        /**
         * An AllowableStrings instance that allows any valid String. The supplier provides an empty list, as a list of every valid String is unbounded.
         * @author fzzyhmstrs
         * @since 0.2.6
         */
        val ANY = AllowableStrings({true}, {listOf()})
    }

}