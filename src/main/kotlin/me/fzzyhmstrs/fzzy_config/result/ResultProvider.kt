/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.result

import org.jetbrains.annotations.ApiStatus

/**
 * Generic provider of values based on a string scope provided
 * @param T Non-null type to return
 * @author fzzyhmstrs
 * @since 0.5.3
 */
@ApiStatus.Experimental
interface ResultProvider<T: Any> {
    /**
     * Provides a result. Note non-null return value, so implementations should have a fallback mechanism.
     *
     * See [the translation wiki page](https://github.com/fzzyhmstrs/fconfig/wiki/Translation) for an overview of how scopes work. Append args to the scope with `?` like `mod_id.config_id.settingName?arg?arg2`
     * @param scope String scope to provide a result based on.
     * @return T result based on scope provided. In general the result should be consistent for like-keys. If keyA.equals(keyB), the result should either be the same or have a consistent source (derive from the same supplier, function, etc.)
     * @author fzzyhmstrs
     * @since 0.5.3
     */
    @ApiStatus.Experimental
    fun getResult(scope: String): T
}