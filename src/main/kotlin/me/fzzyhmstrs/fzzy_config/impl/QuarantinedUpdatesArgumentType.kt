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
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.command.CommandSource
import java.util.concurrent.CompletableFuture

@Environment(EnvType.CLIENT)
internal class QuarantinedUpdatesArgumentType: ArgumentType<String> {

    override fun parse(reader: StringReader): String {
        return reader.readQuotedString()
    }

    override fun <S : Any?> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        return CommandSource.suggestMatching(SyncedConfigRegistry.quarantineList().map { "\"$it\"" }, builder)
    }

    override fun getExamples(): MutableCollection<String> {
        return mutableSetOf("\"my_mod.config @fzzyhmstrs @27/08/24 17:51:38\"", "\"minecraft.game_options @steve @01/01/01 00:00:00\"")
    }

    companion object {
        fun getQuarantineId(context: CommandContext<*>, name: String?): String? {
            return context.getArgument(name, String::class.java)
        }
    }
}