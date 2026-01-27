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
import java.util.IdentityHashMap

class Attr(val caseSensitive: Boolean = false) {

    companion object {
        private val keyToAttr: HashMap<String, Attr> = hashMapOf()
        private val attrToKey: IdentityHashMap<Attr, String> = IdentityHashMap()

        fun register(key: String, attr: Attr): Attr {
            if (keyToAttr.put(key, attr) != null) throw IllegalStateException("Attr $key registered twice")
            attrToKey[attr] = key
            return attr
        }

        fun getAttr(key: String): Attr? {
            return keyToAttr[key]
        }

        fun getKey(attr: Attr): String? {
            return attrToKey[attr]
        }
    }
}