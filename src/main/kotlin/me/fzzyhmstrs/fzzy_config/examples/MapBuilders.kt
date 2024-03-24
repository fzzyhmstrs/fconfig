package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.api.Translatable
import me.fzzyhmstrs.fzzy_config.validated_field.map.ValidatedEnumMap
import me.fzzyhmstrs.fzzy_config.validated_field.map.ValidatedIdentifierMap
import me.fzzyhmstrs.fzzy_config.validated_field.map.ValidatedMap
import me.fzzyhmstrs.fzzy_config.validated_field.misc.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validated_field.misc.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validated_field.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validated_field.number.ValidatedInt
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Identifier

object MapBuilders {

    // a ValidatedMap built with the Builder.
    // KeyHandler takes any String Entry, such as ValidatedIdentifier (ValidatedIdentifier is recommended, in general)
    // valueHandler takes any Entry, in this case a ValidatedInt
    // defaultIds takes the default map in Map<Identifier,V> form. see defaults for Map<String,V> impl
    val stringTest = ValidatedMap.Builder<Int>()
        .keyHandler(ValidatedIdentifier.ofList(Identifier("minecraft:stick"), listOf(Identifier("minecraft:stick"))))
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

    // a ValidatedIdentifierMap built with the builder
    // keyhandler uses a ValidatedIdentifier to validate key inputs
    // valuehandler takes any Entry, in this case a ValidatedBoolean (Shorthand!)
    // defaults includes the default map. In this case an empty map.
    private val idTest: ValidatedIdentifierMap<Boolean> =
        ValidatedIdentifierMap.Builder<Boolean>()
            .keyHandler(ValidatedIdentifier.ofTag(ItemTags.BOATS))
            .valueHandler(true.validated())
            .defaults(mapOf())
            .build()

}