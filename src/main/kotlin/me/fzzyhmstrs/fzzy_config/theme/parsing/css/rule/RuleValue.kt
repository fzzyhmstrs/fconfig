package me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule

data class RuleValue<T>(val value: T, val default: Boolean) {

    companion object {
        fun <T> of(value: T): RuleValue<T> {
            return RuleValue(value, false)
        }

        fun <T> default(value: T): RuleValue<T> {
            return RuleValue(value, false)
        }
    }
}
