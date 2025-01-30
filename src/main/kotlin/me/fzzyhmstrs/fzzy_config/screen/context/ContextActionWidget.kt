package me.fzzyhmstrs.fzzy_config.screen.context

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureSet
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import java.util.function.Consumer

internal class ContextActionWidget(
    private val action: ContextAction,
    position: Position,
    width: Int)
    :
    CustomButtonWidget(
        0,
        0,
        width,
        14,
        action.texts.name,
        { _ -> PopupWidget.pop(); action.action.apply(position) },
        DEFAULT_ACTIVE_NARRATION_SUPPLIER,
        Consumer { _-> },
        null,
        TextureSet("widget/popup/button_right_click".fcId(), "widget/popup/button_right_click".fcId(), "widget/popup/button_right_click_highlighted".fcId()))
{

    init {
        this.activeSupplier = action.active
    }


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
        if (action.icon != null) {
            super.renderCustom(context, x + 12, y, width - 12, height, mouseX, mouseY, delta)
            action.icon.renderDecoration(context, x + 1, y + 2, delta, this.active, this.isSelected)
        } else {
            super.renderCustom(context, x + 12, y, width - 12, height, mouseX, mouseY, delta)
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
            super.drawScrollableText(context, textRenderer, (i + j) / 2, i, y, j, y + height, color)
        } else {
            context.drawTextWithShadow(textRenderer, text, i, k, color)
        }
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        super.appendClickableNarrations(builder)
        if (action.texts.desc != null) {
            builder?.put(NarrationPart.HINT, action.texts.desc)
        }
    }

    companion object {

        fun getNeededWidth(action: ContextAction): Int {
            return 11 + MinecraftClient.getInstance().textRenderer.getWidth(action.texts.name) + 4
/*            return if (action.icon != null) {
                11 + MinecraftClient.getInstance().textRenderer.getWidth(action.texts.name) + 4
            } else {
                MinecraftClient.getInstance().textRenderer.getWidth(action.texts.name) + 4
            }*/
        }
    }
}