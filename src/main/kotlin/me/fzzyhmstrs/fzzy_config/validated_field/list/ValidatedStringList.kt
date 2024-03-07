package me.fzzyhmstrs.fzzy_config.validated_field.list

import me.fzzyhmstrs.fzzy_config.validated_field_v2.list.ValidatedList
import java.util.function.Predicate

/**
 * A subclass of [ValidatedList] that stores general strings
 *
 * @param defaultValue List<String>. The default identifier list.
 * @param listEntryValidator Predicate<String>, optional. If not provided, validation will always pass (no validation). The supplied predicate should return true on validation success, false on fail.
 * @param invalidEntryMessage String, optional. Provide a message detailing the criteria the user needs to follow in the case they make a mistake.
 */
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