package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.api.Translatable
import me.fzzyhmstrs.fzzy_config.validation.map.ValidatedEnumMap
import me.fzzyhmstrs.fzzy_config.validation.map.ValidatedIdentifierMap
import me.fzzyhmstrs.fzzy_config.validation.map.ValidatedStringMap
import me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier
import java.util.EnumMap

object MapBuilders {

    // a ValidatedMap built with the Builder.
    // KeyHandler takes any String Entry, such as ValidatedIdentifier (ValidatedIdentifier is recommended, in general)
    // valueHandler takes any Entry, in this case a ValidatedInt
    // defaultIds takes the default map in Map<Identifier,V> form. see defaults for Map<String,V> impl
    val stringTest = ValidatedStringMap.Builder<Int>()
        .keyHandler(ValidatedString("yay"))
        .valueHandler(ValidatedInt(1,100,0))
        .defaultIds(mapOf(Identifier("minecraft:stick") to 50))
        .build()

    private enum class TestEnum: Translatable {
        TEST,
        MORE,
        EVEN,
        ODDS;

        override fun translationKey(): String {
            return "my.config.${this.name}"
        }
        override fun descriptionKey(): String {
            return "my.config.${this.name}.desc"
        }
    }

    // a ValidatedEnumMap built with the Builder.
    // KeyHandler automatically builds a ValidatedEnum of the enum class of the default passed
    // valueHandler takes any Entry, in this case a ValidatedFloat
    // defaults builds the default map. In this case out of vararg set of Pair<Enum,V>
    private val enumTest: ValidatedEnumMap<TestEnum, Float> =
        ValidatedEnumMap.Builder<TestEnum,Float>()
            .keyHandler(TestEnum.TEST)
            .valueHandler(ValidatedFloat(1f,1f,0f))
            .defaults(TestEnum.TEST to 1f, TestEnum.EVEN to 0.6f, TestEnum.ODDS to 0.5f)
            .build()

    //An EnumMap builder using an empty EnumMap as default
    private val enumEnumMapTest: ValidatedEnumMap<TestEnum, Float> =
        ValidatedEnumMap.Builder<TestEnum,Float>()
            .keyHandler(TestEnum.TEST)
            .valueHandler(ValidatedFloat(1f,1f,0f))
            .defaults(EnumMap(TestEnum::class.java))
            .build()

    // a ValidatedIdentifierMap built with the builder
    // keyhandler uses a ValidatedIdentifier to validate key inputs
    // valuehandler takes any Entry, in this case a ValidatedBoolean (Shorthand!)
    // defaults includes the default map. In this case an empty map.
    private val idTest: ValidatedIdentifierMap<Boolean> =
        ValidatedIdentifierMap.Builder<Boolean>()
            .keyHandler(ValidatedIdentifier.ofTag(ItemTags.BOATS))
            .valueHandler(ValidatedBoolean())
            .defaults(mapOf())
            .build()

}