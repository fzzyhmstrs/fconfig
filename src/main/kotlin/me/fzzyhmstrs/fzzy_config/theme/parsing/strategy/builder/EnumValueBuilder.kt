package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.builder

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult

open class EnumValueBuilder<T: Enum<T>>(type: TokenType<String>, map: Map<String, T>):
    SingleValidatedValueBuilder<T, String>(type, { v ->
        val l = v.lowercase()
        if (l == v) {
            Errors.uppercase(null, v)
        } else {
            val t = map[l]
            if (t == null) {
                ValidationResult.error(null, INVALID_ENUM, "Possible values: ${map.keys}, provided: $l")
            } else {
                ValidationResult.success(t)
            }
        }
    })
{
    companion object {
        private val INVALID_ENUM = ValidationResult.ErrorEntry.Type<String>("Invalid enum option")

        fun <T: Enum<T>> createMap(clazz: Class<T>): Map<String, T> {
            val values = clazz.enumConstants
            return values.associateBy { v -> v.name.lowercase() }
        }
    }
}