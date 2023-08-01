package me.fzzyhmstrs.fzzy_config.validated_field.map

import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import java.util.function.BiFunction
import java.util.function.BiPredicate

/**
 * Subclass of [ValidatedMap] that restricts keys to string values
 *
 * Validation still requires the user to test on a BiPredicate, but only entry deserialization remains as an input.
 *
 * @param defaultValue Map<String,T>. The default map settings.
 * @param type Class<T>. The java class of the map value type.
 * @param mapEntryValidator BiPredicate<String,T>, optional. If not provided, will always test true (no validation). Pass a BiPredicate that tests both the key and entry against your specific criteria. True passes validation, false fails.
 * @param invalidEntryMessage String, optional. Provide a message detailing the criteria the user needs to follow in the case they make a mistake.
 * @param entryDeserializer EntryDeserializer<T>, optional. If not provided, will attempt to use GSON to parse the values. Otherwise, provide a deserializer that parses the provided JsonElement.
 */

open class ValidatedStringKeyMap<T>(
    defaultValue:Map<String,T>,
    type:Class<T>,
    mapEntryValidator: BiPredicate<String,T> = BiPredicate{_,_ -> true},
    mapEntryCorrector: BiFunction<String, T, T> = BiFunction{ _, it -> it},
    invalidEntryMessage: String = "None",
    entryDeserializer: EntryDeserializer<T> =
        EntryDeserializer { json -> SyncedConfigHelperV1.gson.fromJson(json, type) })
    :
    ValidatedMap<String, T>(
        defaultValue,
        String::class.java,
        type,
        mapEntryValidator,
        mapEntryCorrector,
        invalidEntryMessage,
        KeyDeserializer.STRING,
        entryDeserializer
    ) {
}
