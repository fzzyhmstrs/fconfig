package me.fzzyhmstrs.fzzy_config_test.test.screen

import me.fzzyhmstrs.fzzy_config.screen.widget.NewConfigListWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config_test.FC
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import net.minecraft.util.Colors

class TestEntry(parentElement: NewConfigListWidget, scope: String, group: Int, index: Int, private val n: Text) :
    NewConfigListWidget.Entry(parentElement, 24, scope.lit(), FcText.empty(), scope, group.toString())
{

    init {
        if (group == index) {
            this.visibility = NewConfigListWidget.Visibility.GROUP_VISIBLE
        }
    }

    private val button: ButtonWidget = ButtonWidget.builder(n) { _ -> FC.LOGGER.info("I Pressed {} for height {}", n, h); h += 5 }.size(75, 20).build()

    private val childs = mutableListOf(button)

    override fun selectableChildren(): List<Selectable> {
        return childs
    }

    override fun renderEntry(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        button.setPosition(x + width - 75, y + 2)
        button.render(context, mouseX, mouseY, delta)
        context.drawText(MinecraftClient.getInstance().textRenderer, "Test Entry".lit(), x + 2, y + 6, 0xFFFFFF, true)
    }

    override fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        context.drawBorder(x, y, width, height, Colors.WHITE)
    }

    override fun children(): MutableList<out Element> {
        return childs
    }


}