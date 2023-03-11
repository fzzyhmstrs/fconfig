package me.fzzyhmstrs.fzzy_config.validated_field.list

import java.util.function.Predicate

open class ValidatedFloatList(
    defaultValue:List<Float>,
    listEntryValidator: Predicate<Float> = Predicate {true},
    invalidEntryMessage: String = "None")
    :
    ValidatedList<Float>(
        defaultValue,
        Float::class.java,
        listEntryValidator,
        invalidEntryMessage
    ) 
{
}
