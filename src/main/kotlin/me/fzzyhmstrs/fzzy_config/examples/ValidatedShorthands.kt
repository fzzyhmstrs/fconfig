package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.api.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.math.Expression
import me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validatedColor
import me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validatedIds
import me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validatedList
import me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validatedRegistry
import me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validatedTag
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedIdentifier
import net.minecraft.item.SwordItem
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier
import java.awt.Color
import java.util.function.BiPredicate

internal object ValidatedShorthands {

    //Shorthand unbounded int. The int is the default value
    val shorthandInt = 12.validated()


    //Shorthand unbounded byte. The byte is the default value
    val shorthandByte = 12.toByte().validated()


    //Shorthand unbounded byte. The byte is the default value
    val shorthandShort = 12.toShort().validated()


    //Shorthand unbounded long. The long is the default value
    val shorthandLong = 100L.validated()


    //Shorthand unbounded double. The double is the default value
    val shorthandDouble = 4.0.validated()


    //Shorthand unbounded float. The float is the default value
    val shorthandFloat = 4f.validated()

    enum class TestEnum: EnumTranslatable {
        TEST,
        MORE,
        EVEN,
        ODDS;

        override fun prefix(): String {
            return "my.config"
        }
    }

    val lang = """{
        "my.config.TEST": "Test",
        "my.config.TEST.desc": "A test description",
        "my.config.MORE": "More Testing",
        "my.config.MORE.desc": "Another test description",
        "my.config.EVEN": "Even Numbers",
        "my.config.EVEN.desc": "Only even numbers here!",
        "my.config.ODDS": "Odd Numbers",
        "my.config.ODDS.desc": "Odd numbers and nothing else!"
    }"""

    //shorthand validated Enum. the constant is the default value
    val shorthandEnum = TestEnum.MORE.validated()

    //Shorthand validated Color. The color values in the Color will be the default color components
    val shorthandColor = Color(255,255,128,255).validated()

    //Shorthand validated Color from a base color int. The color values in the Color will be the default color components.
    //In this example, the color does not accept transparency
    val shorthandColorInt = 0xFF5500.validatedColor(false)

    //Shorthand boolean. the bool used is the default
    val shorthandBool = true.validated()

    //Shorthand math Expression. This is directly in the Expression class itself, not in the Shorthand object
    val shorthandMath = Expression.validated("x * 0.5", setOf('x'))

    //example shorthand validated list. Shown is an identifier list. Note that identifier lists are actually string lists
    val shorthandList = listOf("minecraft:stick").validated(ValidatedIdentifier.ofRegistry(Identifier("stick"),Registries.ITEM))

    //example Number-based shorthand list
    val shorthandNumberList = listOf(1,2,5,10).validated()

    // example shorthand identifier list. Note that this example is a bit redundant, see .validatedList(list) in this case
    val shorthandIdentifierList = listOf(Identifier("stick")).validated(ValidatedIdentifier.ofList(listOf(Identifier("stick"))))

    //example shorthand identifier list with automatic tag validation
    val shorthandTagIdList = listOf(Identifier("white_bed")).validatedTag(ItemTags.BEDS)

    //example shorthand identifier list with automatic registry validation
    val shorthandRegistryIdList = listOf(Identifier("nether_star")).validatedRegistry(Registries.ITEM)

    //example shorthand identifier list with automatic predicated registry validation
    val shorthandPredicatedRegistryIdList = listOf(Identifier("stone_sword")).validatedRegistry(Registries.ITEM, BiPredicate { id, e -> e.value() is SwordItem })

    //example shorthand identifier list with automatic list validation. The list should be complete and available at validation time
    val shorthandListIdList = listOf(Identifier("arrow")).validatedList(listOf(Identifier("arrow"),Identifier("firework_rocket")))

    //example shorthand validated Identifier using a tag for validation
    val shorthandTagIds = BlockTags.AXE_MINEABLE.validatedIds()

    //example shorthand validated Identifier using a registry for validation
    val shorthandRegistryIds = Registries.ATTRIBUTE.validatedIds()

    //example shorthand validated Identifier using a registry for validation
    val shorthandPredicatedRegistryIds = Registries.ATTRIBUTE.validatedIds(BiPredicate { id, e -> id.namespace == FC.MOD_ID })

    //example shorthand validated Identifier using a list for validation. The list should be complete and available at validation time
    val shorthandListIds = listOf(Identifier("arrow"),Identifier("firework_rocket")).validatedIds()
}
