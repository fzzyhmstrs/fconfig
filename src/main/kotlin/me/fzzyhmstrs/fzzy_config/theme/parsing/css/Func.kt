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

import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.HashMap

interface Func<T: Any> {
    fun apply(args: T, context: SelectorContext): Boolean
    fun prepare(queue: TokenQueue, args: Set<String>, selectorCreator: (Func<T>, T) -> Selector?): Selector?

    companion object {
        private val keyToFunc: HashMap<String, Func<*>> = hashMapOf()
        private val funcToKey: IdentityHashMap<Func<*>, String> = IdentityHashMap()

        fun register(key: String, attr: Func<*>): Func<*> {
            if (keyToFunc.put(key, attr) != null) throw IllegalStateException("Func $key registered twice")
            funcToKey[attr] = key
            return attr
        }

        fun getFunc(key: String): Func<*>? {
            return keyToFunc[key]
        }

        fun getKey(attr: Func<*>): String? {
            return funcToKey[attr]
        }
    }
}