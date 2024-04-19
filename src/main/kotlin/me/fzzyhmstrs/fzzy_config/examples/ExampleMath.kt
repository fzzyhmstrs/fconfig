/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.util.Expression

object ExampleMath {

    fun maths() {

        // raw string math equation "x times 10 raised to the y power
        val mathString = "(x * 10) ^ y"

        // above math equation parsed into an expression
        val mathExpression = Expression.parse(mathString)

        //we map x and y to their current values
        val mathVariables = mapOf(
            'x' to 0.5,
            'y' to 5.0
        )

        //eval the result, in this case (0.5 * 10) ^ 5.0 = 3125.0
        val mathResult = mathExpression.eval(mathVariables)
    }

}