package me.fzzyhmstrs.fzzy_config.validated_field.map

import java.util.function.BiFunction
import java.util.function.BiPredicate

/**
 * Subclass of [ValidatedStringKeyMap] that restricts the map type to Map<String, String>
 *
 * Validation still requires a user-provided BiPredicate.
 *
 * @param defaultValue Map<String,String>. The default string-int map settings.
 * @param mapEntryValidator BiPredicate<String,Int>, optional. If not provided, will always test true (no validation). Pass a BiPredicate that tests both the key and entry against your specific criteria. True passes validation, false fails.
 * @param invalidEntryMessage String, optional. Provide a message detailing the criteria the user needs to follow in the case they make a mistake.
 */
open class ValidatedStringStringMap(
    defaultValue:Map<String,String>,
    mapEntryValidator: BiPredicate<String,String> = BiPredicate{_,_ -> true},
    mapEntryCorrector: BiFunction<String, String, String> = BiFunction{ _, it -> it},
    invalidEntryMessage: String = "None")
    :
    ValidatedMap<String, String>(
        defaultValue,
        String::class.java,
        String::class.java,
        mapEntryValidator,
        mapEntryCorrector,
        invalidEntryMessage,
        KeyDeserializer.STRING
    ) 
{
    constructor(defaultValue:Map<String,String>,
                mapEntryValidator: BiPredicate<String,String> = BiPredicate{_,_ -> true},
                invalidEntryMessage: String = "None")
            : this(defaultValue, mapEntryValidator,BiFunction{ _, it -> it}, invalidEntryMessage)

}
