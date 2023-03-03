package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import java.util.function.Predicate

open class ValidatedIntList(
    defaultValue:List<Int>,
    listEntryValidator: Predicate<Int> = Predicate {true},
    invalidEntryMessage: String = "None")
    :
    ValidatedList<Int>(
        defaultValue,
        Int::class.java,
        listEntryValidator,
        invalidEntryMessage
    ) 
{
}
