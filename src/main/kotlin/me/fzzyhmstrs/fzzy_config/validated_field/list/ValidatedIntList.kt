package me.fzzyhmstrs.fzzy_config.validated_field.list

import java.util.function.Predicate

/**
 * A subclass of [ValidatedList] that stores integers
 *
 * To maintain ordering, consider passing something like a LinkedList that maintains entry order. Validation requires a passed predicate.
 *
 * @param defaultValue List<Int>. The default identifier list.
 * @param listEntryValidator Predicate<Int>, optional. If not provided, validation will always pass (no validation). The supplied predicate should return true on validation success, false on fail.
 * @param invalidEntryMessage String, optional. Provide a message detailing the criteria the user needs to follow in the case they make a mistake.
 */
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
