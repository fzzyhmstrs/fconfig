package me.fzzyhmstrs.fzzy_config.validated_field.map

import java.util.function.BiFunction
import java.util.function.BiPredicate

/**
 * Subclass of [ValidatedStringKeyMap] that restricts the map type to Map<String, Boolean>
 *
 * A common use case would be a map of enabled/disabled switches on a collection of items or enchantments, perhaps.
 *
 * Validation still requires a user-provided BiPredicate.
 *
 * @param defaultValue Map<String,Boolean>. The default string-bool map settings.
 * @param mapEntryValidator BiPredicate<String,Boolean>, optional. If not provided, will always test true (no validation). Pass a BiPredicate that tests both the key and entry against your specific criteria. True passes validation, false fails.
 * @param invalidEntryMessage String, optional. Provide a message detailing the criteria the user needs to follow in the case they make a mistake.
 */
open class ValidatedStringBoolMap(
    defaultValue:Map<String,Boolean>,
    mapEntryValidator: BiPredicate<String,Boolean> = BiPredicate{_,_ -> true},
    mapEntryCorrector: BiFunction<String, Boolean, Boolean> = BiFunction{ _, it -> it},
    invalidEntryMessage: String = "None")
    :
    ValidatedMap<String, Boolean>(
        defaultValue,
        String::class.java,
        Boolean::class.java,
        mapEntryValidator,
        mapEntryCorrector,
        invalidEntryMessage,
        KeyDeserializer.STRING
    ) {
}
