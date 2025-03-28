package me.fzzyhmstrs.fzzy_config.impl.config

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.function.BooleanSupplier
import java.util.function.Function
import java.util.function.Predicate

internal class SearchConfig: Config("search".fcId()) {

    @Comment("Scrolls up a 'page' in the Config GUI")
    var modifier = ValidatedEnum(Modifier.ALT, ValidatedEnum.WidgetType.POPUP)
    @Comment("Scrolls down a 'page' in the Config GUI")
    var behavior = ValidatedEnum(SearchBehavior.HOLD_MODIFIER, ValidatedEnum.WidgetType.POPUP)

    fun willPassSearch(): Boolean {
        return behavior.get().willPassSearch(modifier.get())
    }

    fun textPrefix(): List<Text> {
        return behavior.get().textPrefix(modifier.get())
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

    enum class SearchBehavior(private val testModifier: Predicate<Modifier>, private val notMetText: Function<Modifier, List<Text>>, private val metText: Function<Modifier, List<Text>>): EnumTranslatable {
        HOLD_MODIFIER({ it.test() },
            { listOf("fc.search.behavior.HOLD_MODIFIER.desc".translate(it.translation()).formatted(Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate()) },
            { listOf("fc.search.behavior.ALWAYS.desc".translate(it.translation()).formatted(Formatting.YELLOW, Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate()) }),
        DONT_HOLD_MODIFIER({ !it.test() },
            { listOf("fc.search.behavior.DONT_HOLD_MODIFIER.desc".translate(it.translation()).formatted(Formatting.YELLOW, Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate()) },
            { listOf("fc.search.behavior.NEVER.desc".translate(it.translation()).formatted(Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate()) }),
        ALWAYS({ true },
            { listOf("fc.search.behavior.ALWAYS.desc".translate(it.translation()).formatted(Formatting.YELLOW, Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate()) },
            { listOf() }),
        NEVER({ false },
            { listOf() },
            { listOf("fc.search.behavior.NEVER.desc".translate(it.translation()).formatted(Formatting.ITALIC), FcText.empty(), "fc.search.indirect".translate()) });

        fun willPassSearch(modifier: Modifier): Boolean {
            return testModifier.test(modifier)
        }

        fun textPrefix(modifier: Modifier): List<Text> {
            return if (willPassSearch(modifier)) {
                metText.apply(modifier)
            } else {
                notMetText.apply(modifier)
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
        val INSTANCE = ConfigApi.registerAndLoadNoGuiConfig( { SearchConfig() }, RegisterType.CLIENT)
    }
}