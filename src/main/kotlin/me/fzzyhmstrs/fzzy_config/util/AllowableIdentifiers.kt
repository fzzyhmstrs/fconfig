package me.fzzyhmstrs.fzzy_config.util

import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.command.CommandSource
import net.minecraft.util.Identifier
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Predicate
import java.util.function.Supplier

class AllowableIdentifiers(private val predicate: Predicate<Identifier>, private val supplier: Supplier<List<Identifier>>) {
    fun test(identifier: Identifier): Boolean{
        return predicate.test(identifier)
    }
    fun get(): List<Identifier>{
        return supplier.get()
    }
    fun getSuggestions(input: String, cursor: Int, choiceValidator: ChoiceValidator<Identifier>): CompletableFuture<Suggestions> {
        val truncatedInput: String = input.substring(0, cursor)
        val builder = SuggestionsBuilder(truncatedInput, truncatedInput.lowercase(Locale.ROOT),cursor)
        return CommandSource.suggestIdentifiers(get().filter { choiceValidator.validateEntry(it,EntryValidator.ValidationType.STRONG).isValid() },builder)
    }

    companion object{
        val ANY = AllowableIdentifiers({true},{listOf()})
    }
}