package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import net.minecraft.util.Identifier
import java.util.function.Predicate

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
