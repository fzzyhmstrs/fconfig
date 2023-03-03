package me.fzzyhmstrs.fzzy_config.config_util.validated_field

import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import java.util.function.BiPredicate

open class ValidatedStringKeyMap<T>(
    defaultValue:Map<String,T>,
    type:Class<T>,
    mapEntryValidator: BiPredicate<String, T> = BiPredicate{ _, _ -> true},
    invalidEntryMessage: String = "None",
    entryDeserializer: EntryDeserializer<T> =
        EntryDeserializer { json -> SyncedConfigHelperV1.gson.fromJson(json, type) })
    :
    ValidatedMap<String, T>(
        defaultValue,
        String::class.java,
        type,
        mapEntryValidator,
        invalidEntryMessage,
        KeyDeserializer.STRING,
        entryDeserializer
    ) {
}
