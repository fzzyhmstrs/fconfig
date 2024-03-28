package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.api.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.validation.list.ValidatedIdentifierList
import me.fzzyhmstrs.fzzy_config.validation.map.ValidatedMap
import me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedIdentifier
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier

object ValidatedCollectionExamples {

    // validated identifier list. default list and ValidatedIdentifier for handling
    val validatedIdList = ValidatedIdentifierList(listOf(Identifier("stone_axe")), ValidatedIdentifier.ofTag(ItemTags.AXES))

    enum class KeyEnum: EnumTranslatable {
        KEY_1,
        KEY_2,
        KEY_3;
        override fun prefix(): String{
            return "my.config.key_enum"
        }
    }

    //Example ValidatedMap. NOTE: this is not a ValidatedEnumMap, but that can be used too
    val validatedMap = ValidatedMap(mapOf(KeyEnum.KEY_1 to true),KeyEnum.KEY_1.validated(),ValidatedBoolean())

}