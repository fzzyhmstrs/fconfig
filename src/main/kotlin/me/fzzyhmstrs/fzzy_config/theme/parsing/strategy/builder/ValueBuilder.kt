package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.builder

import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult

interface ValueBuilder<T> {
    fun build(input: TokenQueue): ValidationResult<T?>
}