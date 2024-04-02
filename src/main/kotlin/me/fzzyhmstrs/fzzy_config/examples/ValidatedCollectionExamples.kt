package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedMap
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.minecraft.util.Identifier

object ValidatedCollectionExamples {

    //validated int list, with validation on entries restricting inputs to 1 to 16 (inclusive)
    val validatedList = ValidatedList(listOf(1,2,4,8), ValidatedInt(1..16))

    enum class KeyEnum: EnumTranslatable {
        KEY_1,
        KEY_2,
        KEY_3;
        override fun prefix(): String{
            return "my.config.key_enum"
        }
    }

    //validated set, based on TestEnum, validation limiting to those keys
    val validatedSet = ValidatedList(listOf(KeyEnum.KEY_1), KeyEnum::class.java.validated())

    //Example ValidatedMap. NOTE: this is not a ValidatedEnumMap, but that can be used too
    val validatedMap = ValidatedMap(mapOf(KeyEnum.KEY_1 to true),KeyEnum.KEY_1.validated(),ValidatedBoolean())

    //wraps the vararg valued provided with a blank validated field (identifiers in this case). validation with actual bounds and logic can of course be used too
    val listFromFieldVararg = ValidatedIdentifier().toList(Identifier("stick"), Identifier("blaze_rod"))

    //wraps the collection provided with a blank validated field (identifiers in this case). validation with actual bounds and logic can of course be used too
    val listFromFieldCollection = ValidatedIdentifier().toList(listOf(Identifier("wooden_sword"), Identifier("stone_sword")))


}