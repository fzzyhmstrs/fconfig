package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import java.util.function.BiPredicate

open class ValidatedStringStringMap(
    defaultValue:Map<String,String>,
    mapEntryValidator: BiPredicate<String, String> = BiPredicate{ _, _ -> true},
    invalidEntryMessage: String = "None")
    :
    ValidatedMap<String, String>(
        defaultValue,
        String::class.java,
        String::class.java,
        mapEntryValidator,
        invalidEntryMessage,
        KeyDeserializer.STRING
    ) 
{
}
