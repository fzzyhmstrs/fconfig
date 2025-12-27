/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Imbued Sorcery, a mod made for minecraft; as such it falls under the license of Imbued Sorcery.
 *
 * Imbued Sorcery is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css2.token

/**
 * simple interface used to make value types for proto tokens. This defines what type of value the token stores
 * @author fzzyhmstrs
 * @since ?.?.?
 */
class TokenValue<T: Any>(private val id: String) {

    override fun toString(): String {
        return id
    }
}