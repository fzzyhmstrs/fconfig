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

import com.mojang.brigadier.suggestion.Suggestions
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import java.util.concurrent.CompletableFuture

/**
 * Provides suggestions for something requesting them
 *
 * SAM: [getSuggestions] which takes input: Sting, cursor: Int, choiceValidator: ChoiceValidator<T>
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.6
 */
@FunctionalInterface
fun interface EntrySuggester<T> {
    /**
     * Return a future of suggestions based on inputs and validation
     * @param input the raw text input to build suggestions from
     * @param cursor the cursor position relative to the input string. 0 is the start of the string, the string size would be a cursor "live" at the end of the input.
     * @param choiceValidator [ChoiceValidator] validates possible choices, providing a mechanism for filtering from whatever base set of information this suggester is pulling from.
     * @return [CompletableFuture]&lt;[Suggestions]&gt; a future with the total set of suggestions applicable to the input request.
     * @author fzzyhmstrs
     * @since 0.2.6
     */
    fun getSuggestions(input: String, cursor: Int, choiceValidator: ChoiceValidator<T>): CompletableFuture<Suggestions>
}