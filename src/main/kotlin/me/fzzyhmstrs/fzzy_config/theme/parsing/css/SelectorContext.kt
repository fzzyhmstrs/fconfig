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

/**
 * Only class a selector context provider *needs* to have support for is type
 */
interface SelectorContext {
    fun selectorType(): String
    fun selectorNamespace(): String? {
        return null
    }
    fun selectorId(): String? {
        return null
    }
    fun isSelectorClass(clazz: String): Boolean {
        return false
    }
    fun getAttrValue(attr: Attr): String? {
        return null
    }
    fun selectorParent(): SelectorContext? {
        return null
    }
    fun pseudoHovered(): Boolean {
        return false
    }
    fun pseudoFocused(): Boolean {
        return false
    }
    fun pseudoValid(): Boolean {
        return true
    }
}