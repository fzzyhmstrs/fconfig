package me.fzzyhmstrs.fzzy_config.impl.config

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.Translation
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.function.*
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.function.BooleanSupplier
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

@Translation("fc.search")
internal class SearchConfig: Config("search".fcId()) {

    @Comment("Modifier key relevant to the search-passing behavior. Not relevant if the behavior is ALWAYS or NEVER")
    var modifier = ValidatedEnum(Modifier.ALT, ValidatedEnum.WidgetType.POPUP).toCondition({ behavior.get().needsMod }, "fc.search.modifier.disabled.desc".translate()) { Modifier.ALT }.withFailTitle("fc.search.modifier.disabled".translate())

    @Comment("How to pass the current search query to a child entry. When satisfied, the child screen will open with the current search term pre-loaded in its search bar.")
    var behavior = ValidatedEnum(SearchBehavior.HOLD_MODIFIER, ValidatedEnum.WidgetType.POPUP)

    @Comment("When true a config GUI will clear its search query when opened, otherwise it will cache and maintain the query.")
    var clearSearch = ValidatedBoolean()

    fun willPassSearch(): Boolean {
        return behavior.get().willPassSearch(modifier.get())
    }

    fun prefixText(suffix: List<Text>): Supplier<List<Text>> {
        return CompositingSupplier.of(behavior.get().textPrefix(modifier.get()), suffix) { l1: List<Text>, l2: List<Text> -> l1 + l2 }
    }

    enum class Modifier(private val tester: BooleanSupplier): EnumTranslatable {
        ALT({ Screen.hasAltDown() }),
        SHIFT({ Screen.hasShiftDown() }),
        CTRL({ Screen.hasControlDown() });

        fun test(): Boolean {
            return tester.asBoolean
        }

        override fun prefix(): String {
            return "fc.search.modifier"
        }
    }

    enum class SearchBehavior(val needsMod: Boolean, private val testModifier: Predicate<Modifier>, private val notMetText: FunctionSupplier<Modifier, List<Text>>, private val metText: FunctionSupplier<Modifier, List<Text>>): EnumTranslatable {
        HOLD_MODIFIER(true,
            { it.test() },
            SuppliedFunctionSupplier({ INSTANCE.modifier.get() }) { listOf("fc.search.behavior.HOLD_MODIFIER.desc".translate(it.translation()).formatted(Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate()) },
            SuppliedFunctionSupplier({ INSTANCE.modifier.get() }) { listOf("fc.search.behavior.ALWAYS.desc".translate(it.translation()).formatted(Formatting.YELLOW, Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate()) }),
        DONT_HOLD_MODIFIER(true,
            { !it.test() },
            SuppliedFunctionSupplier({ INSTANCE.modifier.get() }) { listOf("fc.search.behavior.DONT_HOLD_MODIFIER.desc".translate(it.translation()).formatted(Formatting.YELLOW, Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate()) },
            SuppliedFunctionSupplier({ INSTANCE.modifier.get() }) { listOf("fc.search.behavior.NEVER.desc".translate(it.translation()).formatted(Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate()) }),
        ALWAYS(false,
            ConstPredicate(true),
            ConstFunction(listOf()),
            ConstFunction(listOf("fc.search.behavior.ALWAYS.desc".translate().formatted(Formatting.YELLOW, Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate()))),
        NEVER(false,
            ConstPredicate(false),
            ConstFunction(listOf("fc.search.behavior.NEVER.desc".translate().formatted(Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate())),
            ConstFunction(listOf()));

        fun willPassSearch(modifier: Modifier): Boolean {
            return testModifier.test(modifier)
        }

        fun textPrefix(modifier: Modifier): Supplier<List<Text>> {
            return if (willPassSearch(modifier)) {
                metText
            } else {
                notMetText
            }
        }

        override fun prefix(): String {
            return "fc.search.behavior"
        }

        override fun translation(fallback: String?): MutableText {
            return FcText.translatableWithFallback(translationKey(), fallback ?: (this as Enum<*>).name, INSTANCE.modifier.get().translation("fc.search.modifier.fallback"))
        }

        override fun description(fallback: String?): MutableText {
            return FcText.translatableWithFallback(descriptionKey(), fallback ?: "", INSTANCE.modifier.get().translation("fc.search.modifier.fallback"))
        }
    }

    companion object {
        val INSTANCE = ConfigApi.registerAndLoadNoGuiConfig(::SearchConfig, RegisterType.CLIENT)
    }
}