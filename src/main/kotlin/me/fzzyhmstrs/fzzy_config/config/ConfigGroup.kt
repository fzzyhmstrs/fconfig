/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.entry.EntryAnchor
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.entry.EntryKeyed
import me.fzzyhmstrs.fzzy_config.entry.EntryPermissible
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.entry.EntryCreators
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.screen.widget.TooltipChild
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.OrderedText
import net.minecraft.text.StringVisitable
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Language
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.*

/**
 * Defines the start of a config group
 *
 * Groups organize a grouping of settings into one unit that can be collapsed and expanded in the
 * @author fzzyhmstrs
 * @since 0.6.0
 */
open class ConfigGroup @JvmOverloads constructor(private val groupName: String = "",
                                                 private val decoration: Decorated? = null,
                                                 private val offsetX: Int? = null,
                                                 private val offsetY: Int? = null): Translatable, EntryKeyed, EntryAnchor, EntryCreator, EntryPermissible {

    @Transient
    private var groupKey = "fc.config.generic.group"

    @Internal
    override fun getEntryKey(): String {
        return groupKey
    }

    @Internal
    override fun setEntryKey(key: String) {
        groupKey = key
    }

    override fun translationKey(): String {
        return getEntryKey()
    }

    override fun descriptionKey(): String {
        return getEntryKey() + ".desc"
    }

    override fun prefixKey(): String {
        return getEntryKey() + ".prefix"
    }

    override fun anchorEntry(anchor: EntryAnchor.Anchor): EntryAnchor.Anchor {
        return anchor
            .decoration(decoration ?: TextureDeco.DECO_LIST,
                if (decoration != null) offsetX ?: 0 else 0,
                if (decoration != null) offsetY ?: 0 else 0)
            .type(EntryAnchor.AnchorType.INLINE)
    }

    override fun anchorId(scope: String): String {
        return if (groupName != "") groupName else scope.substringAfterLast('.')
    }

    override fun prepare(scope: String, groups: LinkedList<String>, annotations: List<Annotation>, globalAnnotations: List<Annotation>) {
        val fieldName = if (groupName != "") groupName else scope.substringAfterLast('.')
        groups.push(fieldName)
    }

    override fun createEntry(context: EntryCreator.CreatorContext): List<EntryCreator.Creator> {
        val fieldName = if (groupName != "") groupName else context.scope.substringAfterLast('.')
        return EntryCreators.createGroupEntry(context, fieldName)
    }

    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
    @Repeatable
    annotation class Pop

    companion object {
        fun pop(annotations: List<Annotation>, groups: LinkedList<String>) {
            if (annotations.firstOrNull { it is Pop } != null) {
                groups.poll()
            }
        }
    }

    internal class GroupButtonWidget(private val list: DynamicListWidget, private val group: String, private val title: Text)
    :
    CustomPressableWidget(0, 0, 110, 20, FcText.EMPTY)
    {
        override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
            if (this.active) {
                if (list.groupIsVisible(group)) {
                    if (this.isFocused) {
                        builder?.put(NarrationPart.USAGE, collapseUsageFocused)
                    } else {
                        builder?.put(NarrationPart.USAGE, collapseUsageHovered)
                    }
                } else {
                    if (this.isFocused) {
                        builder?.put(NarrationPart.USAGE, expandUsageFocused)
                    } else {
                        builder?.put(NarrationPart.USAGE, expandUsageHovered)
                    }
                }
            }
        }

        override fun getTooltip(): Tooltip? {
            return null
        }

        private fun getTex(bl: Boolean, bl2: Boolean): Identifier {
            return if (bl)
                if (bl2)
                    TextureIds.GROUP_COLLAPSE_HIGHLIGHTED
                else
                    TextureIds.GROUP_COLLAPSE
            else if (bl2)
                TextureIds.GROUP_EXPAND_HIGHLIGHTED
            else
                TextureIds.GROUP_EXPAND
        }

        override fun renderCustom(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            val bl = list.groupIsVisible(group)
            val bl2 = isMouseOver(mouseX.toDouble(), mouseY.toDouble())
            val t = if (bl2) title.copy().styled { s -> s.withUnderline(true) } else title
            val trimmed = trim(t, width - 17)
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, trimmed, x + 17, y + (getHeight() - (MinecraftClient.getInstance().textRenderer.fontHeight) / 2), -1)
            context.drawTex(getTex(bl, bl2), x, y, 20, 20)
            val h2 = y + height/2
            if (bl) { //vertical line
                context.fill(x, h2, x + 1, y + height, -1)
                context.fill(x + 1, h2, x + 2, y + height, -12698050)
            } else { //horizontal line
                val x1 = x + 17 + MinecraftClient.getInstance().textRenderer.getWidth(trimmed) + 7
                val x2 = x + width - 7
                if (x2 - 2 > x1) {
                    context.fill(x1, h2 - 1, x2, h2, -1)
                    context.fill(x1 + 1, h2, x2 + 1, h2 + 1, -12698050)
                }
            }
        }

        private fun trim(text: Text, width: Int): OrderedText? {
            val stringVisitable = MinecraftClient.getInstance().textRenderer.trimToWidth(text, width - MinecraftClient.getInstance().textRenderer.getWidth(ScreenTexts.ELLIPSIS))
            return Language.getInstance().reorder(StringVisitable.concat(stringVisitable, ScreenTexts.ELLIPSIS))
        }

        override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
            if (!parentSelected) return TooltipChild.EMPTY
            return if (list.groupIsVisible(group)) {
                collapse
            } else {
                expand
            }
        }

        companion object {
            private val collapse = listOf("fc.validated_field.collapse".translate())
            private val expand = listOf("fc.validated_field.expand".translate())
            private val collapseUsageFocused = "fc.validated_field.collapse.usage.focused".translate()
            private val expandUsageFocused = "fc.validated_field.expand.usage.focused".translate()
            private val collapseUsageHovered = "fc.validated_field.collapse.usage.hovered".translate()
            private val expandUsageHovered = "fc.validated_field.expand.usage.hovered".translate()
        }
    }
}