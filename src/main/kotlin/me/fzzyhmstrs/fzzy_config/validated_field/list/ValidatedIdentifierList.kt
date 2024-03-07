package me.fzzyhmstrs.fzzy_config.validated_field.list

import me.fzzyhmstrs.fzzy_config.validated_field_v2.list.ValidatedList
import net.minecraft.util.Identifier
import java.util.function.Predicate

/**
 * A subclass of [ValidatedList] that stores identifiers
 *
 * Validation requires a predicate, unlike [ValidatedIdentifier](me.fzzyhmstrs.fzzy_config.validated_field.ValidatedIdentifier) which tests against a passed collection. Similar functionality can be achieved by passing a predicate that looks something like `{id -> collectionToTest.contains(id)}`
 *
 * @param defaultValue List<Identifier>. The default identifier list.
 * @param listEntryValidator Predicate<Identifier>, optional. If not provided, validation will always pass (no validation). The supplied predicate should return true on validation success, false on fail.
 * @param invalidEntryMessage String, optional. Provide a message detailing the criteria the user needs to follow in the case they make a mistake.
 */
open class ValidatedIdentifierList(
    defaultValue:List<Identifier>,
    listEntryValidator: Predicate<Identifier> = Predicate {true},
    invalidEntryMessage: String = "None")
    :
    ValidatedList<Identifier>(
        defaultValue,
        Identifier::class.java,
        listEntryValidator,
        invalidEntryMessage
    )
{
}