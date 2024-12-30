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
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomMultilineTextWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Ref
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import java.util.function.Consumer
import java.util.function.Function

object EntryCreators {

    //TODO
    val OPEN_SCREEN = EntryCreator.CreatorContextKey<Consumer<String>>()

    //TODO
    val COPY_BUFFER = EntryCreator.CreatorContextKey<Ref<Any?>>()

    //TODO
    fun createConfigEntry(context: EntryCreator.CreatorContext): List<EntryCreator.Creator> {
        val function: Function<DynamicListWidget, out DynamicListWidget.Entry> = Function { listWidget ->
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

    //TODO
    fun createSectionEntry(context: EntryCreator.CreatorContext): List<EntryCreator.Creator> {
        val function: Function<DynamicListWidget, out DynamicListWidget.Entry> = Function { listWidget ->
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

    //TODO
    fun createNoPermsEntry(context: EntryCreator.CreatorContext, type: String): List<EntryCreator.Creator> {
        val child: TooltipChild = object: TooltipChild {
            override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
                return if (parentSelected || keyboardFocused) {
                    listOf("fc.button.$type.desc".translate())
                } else {
                    TooltipChild.EMPTY
                }
            }
        }
        val function: Function<DynamicListWidget, out DynamicListWidget.Entry> = Function { listWidget ->
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

    //TODO
    fun createHeaderEntry(context: EntryCreator.CreatorContext, prefix: Text): List<EntryCreator.Creator> {
        val function: Function<DynamicListWidget, out DynamicListWidget.Entry> = Function { listWidget ->
            val contentBuilder = ConfigEntry.ContentBuilder(context, setOf())
            contentBuilder.layoutMain { _ ->
                LayoutWidget(paddingW = 0, spacingW = 0).add(
                    "header",
                    CustomMultilineTextWidget(prefix, 10),
                    LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY,
                    LayoutWidget.Position.BELOW)
            }
            .visibility(DynamicListWidget.Visibility.HEADER_VISIBLE)

            ConfigEntry(listWidget, contentBuilder.build(), context.texts)
        }
        return listOf(EntryCreator.Creator(context.scope, context.texts, function))
    }

    //TODO
    fun createGroupEntry(context: EntryCreator.CreatorContext, group: String): List<EntryCreator.Creator> {
        val function: Function<DynamicListWidget, out DynamicListWidget.Entry> = Function { listWidget ->
            val contentBuilder = ConfigEntry.ContentBuilder(context, setOf())
            contentBuilder.layoutMain { _ ->
                LayoutWidget(paddingW = 0, spacingW = 0).add(
                    "group",
                    ConfigGroup.GroupButtonWidget(listWidget, group, context.texts.name),
                    LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY,
                    LayoutWidget.Position.BELOW)
            }
            .visibility(DynamicListWidget.Visibility.GROUP_VISIBLE)
            .group(group)

            ConfigEntry(listWidget, contentBuilder.build(), context.texts)
        }
        return listOf(EntryCreator.Creator(context.scope, context.texts, function))
    }

    //TODO
    fun createActionEntry(context: EntryCreator.CreatorContext, decoration: Decorated?, widget: ClickableWidget): List<EntryCreator.Creator> {
        val function: Function<DynamicListWidget, out DynamicListWidget.Entry> = Function { listWidget ->
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