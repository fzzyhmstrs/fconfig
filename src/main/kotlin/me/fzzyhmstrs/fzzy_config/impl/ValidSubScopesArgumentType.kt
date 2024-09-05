/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.impl

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import net.minecraft.command.CommandSource
import java.util.concurrent.CompletableFuture

//client
internal class ValidSubScopesArgumentType: ArgumentType<String> {
    override fun parse(reader: StringReader): String {
        return reader.readUnquotedString()
    }
    override fun <S : Any?> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val parentScope = try { ValidScopesArgumentType.getValidScope(context, "base_scope") ?: "" } catch (e: Exception) { "" }
        return CommandSource.suggestMatching(ClientConfigRegistry.getSubScreenScopes(parentScope), builder)
    }
    override fun getExamples(): MutableCollection<String> {
        return mutableSetOf("my_config", "item_config", "items")
    }

    companion object {
        fun getValidSubScope(context: CommandContext<*>, name: String?): String? {
            return context.getArgument(name, String::class.java)
        }
    }
}