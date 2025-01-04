/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.result.ResultArg
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedPair

class LeftArg<X>(fallback: X): ResultArg<ValidatedPair.Tuple<X, *>, X>("left", fallback, true) {

    override fun applyArg(scopeValue: ValidatedPair.Tuple<X, *>, argValue: String): X {
        return scopeValue.left
    }
}

class RightArg<Y>(fallback: Y): ResultArg<ValidatedPair.Tuple<*, Y>, Y>("right", fallback, true) {

    override fun applyArg(scopeValue: ValidatedPair.Tuple<*, Y>, argValue: String): Y {
        return scopeValue.right
    }
}