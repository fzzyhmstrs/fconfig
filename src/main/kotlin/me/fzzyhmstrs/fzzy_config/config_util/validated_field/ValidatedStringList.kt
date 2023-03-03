package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import java.util.function.Predicate

open class ValidatedStringList(
    defaultValue:List<String>,
    listEntryValidator: Predicate<String> = Predicate {true},
    invalidEntryMessage: String = "None")
    :
    ValidatedList<String>(
        defaultValue,
        String::class.java,
        listEntryValidator,
        invalidEntryMessage
    ) 
{
}
