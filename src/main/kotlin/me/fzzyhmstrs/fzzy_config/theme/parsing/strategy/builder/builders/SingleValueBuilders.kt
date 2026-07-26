package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.builder.builders

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.builder.SingleValidatedValueBuilder
import me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.builder.SingleValueBuilder
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.minecraft.util.Identifier

object SingleValueBuilders {
    val STRING = SingleValueBuilder.create<String, String>(CssType.STRING).safeConverter { v -> v }
    val INT = SingleValueBuilder.create<Int, Parser.NumberValue>(CssType.NUMBER).safeConverter { v -> v.getInt() }
    val DOUBLE = SingleValueBuilder.create<Double, Parser.NumberValue>(CssType.NUMBER).safeConverter { v -> v.getDouble() }

}

class IdentifierValueBuilder(validator: (Identifier) -> Boolean = { true }): SingleValidatedValueBuilder<Identifier, String>(CssType.STRING, { s ->
    val i = Identifier.tryParse(s)
    if (i == null) {
        ValidationResult.error(null, ValidationResult.Errors.INVALID, i.toString())
    } else {
        if (validator(i)) {
            ValidationResult.success(i)
        } else {
            ValidationResult.error(null, ValidationResult.Errors.INVALID, i.toString())
        }
    }
})
