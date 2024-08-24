/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.util.Expression
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validatedColor
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validatedIds
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validatedList
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validatedRegistry
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validatedTag
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import net.minecraft.item.SwordItem
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier
import java.awt.Color
import java.util.function.BiPredicate

internal object ValidatedShorthands {

    enum class TestEnum: EnumTranslatable {
        TEST,
        MORE,
        EVEN,
        ODDS;

        override fun prefix(): String {
            return "my.config"
        }
    }

    val lang = """ {
        "my.config.TEST": "Test",
        "my.config.TEST.desc": "A test description",
        "my.config.MORE": "More Testing",
        "my.config.MORE.desc": "Another test description",
        "my.config.EVEN": "Even Numbers",
        "my.config.EVEN.desc": "Only even numbers here!",
        "my.config.ODDS": "Odd Numbers",
        "my.config.ODDS.desc": "Odd numbers and nothing else!"
    }"""

    fun maths() {
        //Shorthand math Expression. This is directly in the Expression class itself, not in the Shorthand object
        val shorthandMath = Expression.validated("x * 0.5", setOf('x'))
    }

    fun shorthands() {
        //shorthand validated Enum. the constant is the default value
        val shorthandEnum = TestEnum.MORE.validated()

        //Shorthand validated Color. The color values in the Color will be the default color components
        val shorthandColor = Color(255, 255, 128, 255).validated()

        //Shorthand validated Color from a base color int. The color values in the Color will be the default color components.
        //In this example, the color does not accept transparency
        val shorthandColorInt = 0xFF5500.validatedColor(false)

        //Shorthand math Expression. This is directly in the Expression class itself, not in the Shorthand object
        val shorthandMath = Expression.validated("x * 0.5", setOf('x'))

        /////////////////////////////////

        //example shorthand validated list. Shown is an identifier list. Note that identifier lists are actually string lists
        val shorthandList = listOf(Identifier.of("stick")).validated(ValidatedIdentifier.ofRegistry(Identifier.of("stick"), Registries.ITEM))

        //example Number-based shorthand list
        val shorthandNumberList = listOf(1, 2, 5, 10).validated()

        //example shorthand identifier list with automatic tag validation
        val shorthandTagIdList = listOf(Identifier.of("white_bed")).validatedTag(ItemTags.BEDS)

        //example shorthand identifier list with automatic registry validation
        val shorthandRegistryIdList = listOf(Identifier.of("nether_star")).validatedRegistry(Registries.ITEM)

        //example shorthand identifier list with automatic predicated registry validation
        val shorthandPredicatedRegistryIdList = listOf(Identifier.of("stone_sword")).validatedRegistry(Registries.ITEM, BiPredicate { id, e -> e.value() is SwordItem })

        //example shorthand identifier list with automatic list validation. The list should be complete and available at validation time
        val shorthandListIdList = listOf(Identifier.of("arrow")).validatedList(listOf(Identifier.of("arrow"), Identifier.of("firework_rocket")))

        //example shorthand validated Identifier using a tag for validation
        val shorthandTagIds = BlockTags.AXE_MINEABLE.validatedIds()

        //example shorthand validated Identifier using a registry for validation
        val shorthandRegistryIds = Registries.ATTRIBUTE.validatedIds()

        //example shorthand validated Identifier using a registry for validation
        val shorthandPredicatedRegistryIds = Registries.ATTRIBUTE.validatedIds(BiPredicate { id, e -> id.namespace == FC.MOD_ID })

        //example shorthand validated Identifier using a list for validation. The list should be complete and available at validation time
        val shorthandListIds = listOf(Identifier.of("arrow"), Identifier.of("firework_rocket")).validatedIds()

        //////////////////////////////////////

        //example shorthand validated list. Shown is an identifier list. Note that identifier lists are actually string lists
        val shorthandSet = setOf(Identifier.of("stick")).validated(ValidatedIdentifier.ofRegistry(Identifier.of("stick"), Registries.ITEM))

        //example Number-based shorthand list
        val shorthandNumberSet = setOf(1, 2, 5, 10).validated()

        //example shorthand identifier list with automatic tag validation
        val shorthandTagIdSet = setOf(Identifier.of("white_bed")).validatedTag(ItemTags.BEDS)

        //example shorthand identifier list with automatic registry validation
        val shorthandRegistryIdSet = setOf(Identifier.of("nether_star")).validatedRegistry(Registries.ITEM)

        //example shorthand identifier list with automatic predicated registry validation
        val shorthandPredicatedRegistryIdSet = setOf(Identifier.of("stone_sword")).validatedRegistry(Registries.ITEM, BiPredicate { id, e -> e.value() is SwordItem })

        //example shorthand identifier list with automatic list validation. The list should be complete and available at validation time
        val shorthandListIdSet = setOf(Identifier.of("arrow")).validatedList(listOf(Identifier.of("arrow"), Identifier.of("firework_rocket")))
    }

}