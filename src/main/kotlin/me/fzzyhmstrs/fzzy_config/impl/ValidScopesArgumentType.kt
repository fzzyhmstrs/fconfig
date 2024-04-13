package me.fzzyhmstrs.fzzy_config.impl

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.command.CommandSource
import java.util.concurrent.CompletableFuture

@Environment(EnvType.CLIENT)
internal class ValidScopesArgumentType: ArgumentType<String> {
    override fun parse(reader: StringReader): String {
        return reader.readUnquotedString()
    }
    override fun <S : Any?> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        return CommandSource.suggestMatching(ClientConfigRegistry.getScreenScopes(),builder)
    }
    override fun getExamples(): MutableCollection<String> {
        return mutableSetOf("my_mod", "mod_id", "fzzy_config")
    }

    companion object{
        fun getValidScope(context: CommandContext<*>, name: String?): String? {
            return context.getArgument(name, String::class.java)
        }
    }
}