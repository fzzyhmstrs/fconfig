/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.entry

import me.fzzyhmstrs.fzzy_config.config.ConfigGroup
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.screen.widget.TooltipChild
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Ref
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import java.util.function.BiFunction
import java.util.function.Consumer

/**
 * Built in entry creators (except for [ValidatedField][me.fzzyhmstrs.fzzy_config.validation.ValidatedField]) The methods themselves are internal to FC, but can be used as reference.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
object EntryCreators {

    /**
     * Provides access to the current screen managers screen opener. Pass a screen scope in to open a screen related to the active config set. For an arbitrary config screen, use the API.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    val OPEN_SCREEN = EntryCreator.CreatorContextKey<Consumer<String>>()

    /**
     * Provides access to the current screen managers copy buffer.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    val COPY_BUFFER = EntryCreator.CreatorContextKey<Ref<Any?>>()

    internal fun createConfigEntry(context: EntryCreator.CreatorContext): List<EntryCreator.Creator> {
        val function: BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry> = BiFunction { listWidget, _ ->
            val contentBuilder = ConfigEntry.ContentBuilder(context, context.actions.map { ConfigEntry.ActionDecorationWidget.config(it) })
            contentBuilder.decoration(TextureDeco.DECO_OPEN_SCREEN, 2, 2)
            contentBuilder.layoutContent { contentLayout ->
                contentLayout.add(
                    "open_screen",
                    CustomButtonWidget.builder(context.texts.name) { context.misc.get(OPEN_SCREEN)?.accept(context.scope) }
                        .narrationSupplier { _ -> context.texts.name.copyContentOnly() }
                        .width(110)
                        .build(),
                    LayoutWidget.Position.ALIGN_JUSTIFY,
                    LayoutWidget.Position.BELOW)
            }

            ConfigEntry(listWidget, contentBuilder.build(), context.texts)
        }
        return listOf(EntryCreator.Creator(context.scope, context.texts, function))
    }

    internal fun createSectionEntry(context: EntryCreator.CreatorContext): List<EntryCreator.Creator> {
        val function: BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry> = BiFunction { listWidget, _ ->
            val contentBuilder = ConfigEntry.ContentBuilder(context, context.actions.map { ConfigEntry.ActionDecorationWidget.section(it) })
            contentBuilder.decoration(TextureDeco.DECO_OPEN_SCREEN, 2, 2)
            contentBuilder.layoutContent { contentLayout ->
                contentLayout.add(
                    "open_screen",
                    CustomButtonWidget.builder(context.texts.name) { context.misc.get(OPEN_SCREEN)?.accept(context.scope) }
                        .narrationSupplier { _ -> context.texts.name.copyContentOnly() }
                        .width(110)
                        .build(),
                    LayoutWidget.Position.ALIGN_JUSTIFY,
                    LayoutWidget.Position.BELOW)
            }

            ConfigEntry(listWidget, contentBuilder.build(), context.texts)
        }
        return listOf(EntryCreator.Creator(context.scope, context.texts, function))
    }

    internal fun createNoPermsEntry(context: EntryCreator.CreatorContext, type: String): List<EntryCreator.Creator> {
        val child: TooltipChild = object: TooltipChild {
            override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
                return if (parentSelected || keyboardFocused) {
                    listOf("fc.button.$type.desc".translate())
                } else {
                    TooltipChild.EMPTY
                }
            }
        }
        val function: BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry> = BiFunction { listWidget, _ ->
            val contentBuilder = ConfigEntry.ContentBuilder(context)
            contentBuilder.decoration(TextureDeco.DECO_LOCKED, 2, 2)
            contentBuilder.layoutContent { contentLayout ->
                contentLayout.add(
                    "lock_button",
                    CustomButtonWidget.builder("fc.button.$type".translate()) { }
                        .narrationSupplier { _ -> FcText.empty() }
                        .width(110)
                        .active(false)
                        .child(child)
                        .build(),
                    LayoutWidget.Position.ALIGN_JUSTIFY,
                    LayoutWidget.Position.BELOW)
            }

            ConfigEntry(listWidget, contentBuilder.build(), context.texts)
        }
        return listOf(EntryCreator.Creator(context.scope, context.texts, function))
    }

    internal fun createHeaderEntry(context: EntryCreator.CreatorContext): List<EntryCreator.Creator> {
        val function: BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry> = BiFunction { listWidget, _ ->
            val contentBuilder = ConfigEntry.ContentBuilder(context, setOf())
            @Suppress("DEPRECATION")
            contentBuilder.layoutMain { _ ->
                LayoutWidget(paddingW = 0, spacingW = 0)
            }
            .visibility(DynamicListWidget.Visibility.HEADER_VISIBLE)

            ConfigEntry(listWidget, contentBuilder.build(), context.texts)
        }
        return listOf(EntryCreator.Creator(context.scope, context.texts, function))
    }

    internal fun createGroupEntry(context: EntryCreator.CreatorContext, group: String): List<EntryCreator.Creator> {
        val function: BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry> = BiFunction { listWidget, _ ->
            val contentBuilder = ConfigEntry.ContentBuilder(context, setOf())
            @Suppress("DEPRECATION")
            contentBuilder.layoutMain { _ ->
                LayoutWidget(paddingW = 0, spacingW = 0).add(
                    "group",
                    ConfigGroup.GroupButtonWidget(listWidget, group, context.texts.name),
                    LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY,
                    LayoutWidget.Position.BELOW)
            }
            .visibility(DynamicListWidget.Visibility.GROUP_VISIBLE)
            if (group.isNotEmpty()) {
                contentBuilder.group(group)
            }

            ConfigEntry(listWidget, contentBuilder.build(), context.texts)
        }
        return listOf(EntryCreator.Creator(context.scope, context.texts, function))
    }

    internal fun createActionEntry(context: EntryCreator.CreatorContext, decoration: Decorated?, widget: ClickableWidget): List<EntryCreator.Creator> {
        val function: BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry> = BiFunction { listWidget, _ ->
            val contentBuilder = ConfigEntry.ContentBuilder(context, setOf())
            contentBuilder.layoutContent { content ->
                content.add(
                    "action",
                    widget,
                    LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY,
                    LayoutWidget.Position.BELOW)
            }
            if (decoration != null) {
                contentBuilder.decoration(decoration, 2, 2)
            }

            ConfigEntry(listWidget, contentBuilder.build(), context.texts)
        }
        return listOf(EntryCreator.Creator(context.scope, context.texts, function))
    }
}