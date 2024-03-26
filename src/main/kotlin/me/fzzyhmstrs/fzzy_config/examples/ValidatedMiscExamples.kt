package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedExpression

object ValidatedMiscExamples{

    // example validated Expression; automatically parses and caches the Math Expression input in string form. 
    // The user can input any equation they like as long as it uses x, y, both, or neither expected variables passed in the set
    val validatedExpression = ValidatedExpression("2.5 * x ^ 2 - 45 * y", setOf('x', 'y'))
    
}
