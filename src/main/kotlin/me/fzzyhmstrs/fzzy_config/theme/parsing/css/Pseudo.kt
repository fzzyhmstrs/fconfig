/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css

import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.HashMap

class Pseudo(val getterGetter: (PseudoGetter) -> Boolean, val userAction: Boolean) {

    constructor(getterGetter: (PseudoGetter) -> Boolean): this(getterGetter, false)

    companion object {
        private val keyToPseudo: HashMap<String, Pseudo> = hashMapOf()
        private val pseudoToKey: IdentityHashMap<Pseudo, String> = IdentityHashMap()

        fun register(key: String, pseudo: Pseudo): Pseudo {
            if (keyToPseudo.put(key, pseudo) != null) throw IllegalStateException("Pseudo $key registered twice")
            pseudoToKey[pseudo] = key
            return pseudo
        }

        fun getPseudo(key: String): Pseudo? {
            return keyToPseudo[key]
        }

        fun getKey(pseudo: Pseudo): String? {
            return pseudoToKey[pseudo]
        }
    }
}