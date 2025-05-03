/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedIdentifierMap
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedStringMap
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier

object MapBuilders {

    fun stringMap() {
        // a ValidatedMap built with the Builder.
        // KeyHandler takes any String Entry, such as ValidatedIdentifier (ValidatedIdentifier is recommended, in general)
        // valueHandler takes any Entry, in this case a ValidatedInt
        // defaultIds takes the default map in Map<Identifier, V> form. see defaults for Map<String, V> impl
        val stringTest = ValidatedStringMap.Builder<Int>()
            .keyHandler(ValidatedString("yay"))
            .valueHandler(ValidatedInt(1, 100, 0))
            .defaultIds(mapOf(Identifier.of("minecraft:stick") to 50))
            .build()
    }

    fun idMap() {
        // a ValidatedIdentifierMap built with the builder
        // keyhandler uses a ValidatedIdentifier to validate key inputs
        // valuehandler takes any Entry, in this case a ValidatedBoolean (Shorthand!)
        // defaults includes the default map. In this case an empty map.
        val idTest: ValidatedIdentifierMap<Boolean> =
            ValidatedIdentifierMap.Builder<Boolean>()
                .keyHandler(ValidatedIdentifier.ofTag(ItemTags.BOATS))
                .valueHandler(ValidatedBoolean())
                .defaults(mapOf())
                .build()
    }

}