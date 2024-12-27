package me.fzzyhmstrs.fzzy_config.screen.context

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.ActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import java.util.function.Supplier

class ContextActionWidget(
    private val applier: ContextApplier,
    width: Int)
    :
    ActiveButtonWidget(
        applier.action.texts.name,
        width,
        14,
        applier.action.active,
        { _ -> PopupWidget.pop(); applier.apply() },
        PressableTextures("widget/popup/button_right_click".fcId(), "widget/popup/button_right_click".fcId(), "widget/popup/button_right_click_highlighted".fcId())
    ) {

    override fun renderCustom(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        if (applier.action.icon != null) {
            super.renderCustom(context, x + 12, y, width - 12, height, mouseX, mouseY, delta)
            applier.action.icon.renderDecoration(context, x, y + 2, delta)
        } else {
            super.renderCustom(context, x, y, width, height, mouseX, mouseY, delta)
        }
    }

    override fun drawScrollableText(
        context: DrawContext,
        textRenderer: TextRenderer,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        xMargin: Int,
        color: Int
    ) {
        val i = x + xMargin
        val j = x + width - xMargin
        val k = y + ((height - 9 + 1) / 2)
        val text = message
        val w1 = textRenderer.getWidth(text)
        val w2 = j - i
        if (w2 > w1) {
            drawScrollableText(context, textRenderer, (i + j) / 2, i, y, j, y + height, color)
        } else {
            context.drawTextWithShadow(textRenderer, text, i, k, color)
        }
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        super.appendClickableNarrations(builder)
        if (applier.action.texts.desc != null) {
            builder?.put(NarrationPart.HINT, applier.action.texts.desc)
        }
    }

    companion object {

        fun getNeededWidth(applier: ContextApplier): Int {
            return if (applier.action.icon != null) {
                11 + MinecraftClient.getInstance().textRenderer.getWidth(applier.action.texts.name) + 4
            } else {
                MinecraftClient.getInstance().textRenderer.getWidth(applier.action.texts.name) + 4
            }
        }
    }
}