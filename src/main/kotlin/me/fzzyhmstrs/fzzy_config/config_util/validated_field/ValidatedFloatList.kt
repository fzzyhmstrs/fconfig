package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import java.util.function.BiPredicate
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
