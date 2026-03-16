package me.fzzyhmstrs.fzzy_config_test.test.screen

import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawOutline
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config_test.FC
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors

class TestEntry(parentElement: DynamicListWidget, scope: String, group: Int, index: Int, private val n: Component) :
    DynamicListWidget.Entry(parentElement, Translatable.createResult(scope.lit()), DynamicListWidget.Scope(scope, if (group == index) group.toString() else "", listOf(group.toString())), if (group == index) DynamicListWidget.Visibility.GROUP_VISIBLE else DynamicListWidget.Visibility.VISIBLE)
    {

        private val button: Button =
            Button.builder(n) { _ -> FC.LOGGER.info("I Pressed {} for height {}", n, h); h += 5 }.size(75, 20).build()

        private val childs = mutableListOf(button)
        override var h: Int = 24

        override fun selectableChildren(): List<SelectableElement> {
            return childs.cast()
        }

        override fun renderEntry(
            context: GuiGraphicsExtractor,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            focused: Boolean,
            delta: Float
        ) {
            button.setPosition(x + width - 75, y + 2)
            button.extractRenderState(context, mouseX, mouseY, delta)
            context.text(Minecraft.getInstance().font, "Test Entry".lit(), x + 2, y + 6, 0xFFFFFF, true)
        }

        override fun renderBorder(
            context: GuiGraphicsExtractor,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            focused: Boolean,
            delta: Float
        ) {
            if (hovered || focused)
                context.drawOutline(x, y, width, height, CommonColors.WHITE)
        }

        override fun children(): MutableList<out GuiEventListener> {
            return childs
        }


    }