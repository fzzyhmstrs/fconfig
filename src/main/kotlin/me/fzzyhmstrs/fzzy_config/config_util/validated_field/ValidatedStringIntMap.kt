package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import java.util.function.BiPredicate

open class ValidatedStringIntMap(
    defaultValue: Map<String,Int>,
    mapEntryValidator: BiPredicate<String, Int> = BiPredicate{ _, _ -> true},
    invalidEntryMessage: String = "None")
    :
    ValidatedMap<String, Int>(
        defaultValue,
        String::class.java,
        Int::class.java,
        mapEntryValidator,
        invalidEntryMessage,
        KeyDeserializer.STRING
    ) {
}
