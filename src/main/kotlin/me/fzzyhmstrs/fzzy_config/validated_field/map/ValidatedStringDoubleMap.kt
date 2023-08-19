package me.fzzyhmstrs.fzzy_config.validated_field.map

import java.util.function.BiFunction
import java.util.function.BiPredicate

/**
 * Subclass of [ValidatedStringKeyMap] that restricts the map type to Map<String, Int>
 *
 * A common use case might be a map of costs or durabilities on a collection of items.
 *
 * Validation still requires a user-provided BiPredicate.
 *
 * @param defaultValue Map<String,Int>. The default string-int map settings.
 * @param mapEntryValidator BiPredicate<String,Int>, optional. If not provided, will always test true (no validation). Pass a BiPredicate that tests both the key and entry against your specific criteria. True passes validation, false fails.
 * @param invalidEntryMessage String, optional. Provide a message detailing the criteria the user needs to follow in the case they make a mistake.
 */
open class ValidatedStringDoubleMap(
    defaultValue: Map<String,Double>,
    mapEntryValidator: BiPredicate<String,Double> = BiPredicate{_,_ -> true},
    mapEntryCorrector: BiFunction<String, Double, Double> = BiFunction{ _, it -> it},
    invalidEntryMessage: String = "None")
    :
    ValidatedMap<String, Double>(
        defaultValue,
        String::class.java,
        Double::class.java,
        mapEntryValidator,
        mapEntryCorrector,
        invalidEntryMessage,
        KeyDeserializer.STRING
    ) {

    constructor(defaultValue:Map<String,Double>,
                mapEntryValidator: BiPredicate<String,Double> = BiPredicate{_,_ -> true},
                invalidEntryMessage: String = "None")
            : this(defaultValue, mapEntryValidator,BiFunction{ _, it -> it}, invalidEntryMessage)
}
