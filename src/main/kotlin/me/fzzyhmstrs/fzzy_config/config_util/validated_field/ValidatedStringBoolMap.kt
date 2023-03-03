package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import java.util.function.BiPredicate

open class ValidatedStringBoolMap(
    defaultValue:Map<String,Boolean>,
    mapEntryValidator: BiPredicate<String, Boolean> = BiPredicate{ _, _ -> true},
    invalidEntryMessage: String = "None")
    :
    ValidatedMap<String, Boolean>(
        defaultValue,
        String::class.java,
        Boolean::class.java,
        mapEntryValidator,
        invalidEntryMessage,
        KeyDeserializer.STRING
    ) {
}
