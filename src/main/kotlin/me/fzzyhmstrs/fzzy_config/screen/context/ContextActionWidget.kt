package me.fzzyhmstrs.fzzy_config.screen.context

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureSet
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.Ref
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.narration.NarratedElementType
import java.util.function.Consumer

internal class ContextActionWidget(
    private val action: ContextAction,
    private val ref: Ref<Runnable?>,
    position: Position,
    width: Int)
    :
    CustomButtonWidget(
        0,
        0,
        width,
        14,
        action.texts.name,
        { _ -> PopupWidget.pop(); ref.set { action.action.apply(position) } },
        DEFAULT_ACTIVE_NARRATION_SUPPLIER,
        Consumer { _-> },
        null,
        TextureSet("widget/popup/button_right_click".fcId(), "widget/popup/button_right_click".fcId(), "widget/popup/button_right_click_highlighted".fcId()))
{

    init {
        this.activeSupplier = action.active
    }


    override fun renderCustom(
        context: GuiGraphicsExtractor,
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
            action.icon.renderDecoration(context, x + 1, y + 2, delta, this.active, this.isHoveredOrFocused)
        } else {
            super.renderCustom(context, x + 12, y, width - 12, height, mouseX, mouseY, delta)
        }
    }

    override fun drawScrollableText(
        context: GuiGraphicsExtractor,
        textRenderer: Font,
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
        val w1 = textRenderer.width(text)
        val w2 = j - i
        if (w2 > w1) {
            super.drawScrollableText(context, textRenderer, (i + j) / 2, i, y, j, y + height, color)
        } else {
            context.text(textRenderer, text, i, k, color)
        }
    }

    override fun updateWidgetNarration(builder: NarrationElementOutput) {
        super.updateWidgetNarration(builder)
        if (action.texts.desc != null) {
            builder.add(NarratedElementType.HINT, action.texts.desc!!)
        }
    }

    companion object {

        fun getNeededWidth(action: ContextAction): Int {
            return 11 + Minecraft.getInstance().font.width(action.texts.name) + 4
/*            return if (action.icon != null) {
                11 + MinecraftClient.getInstance().textRenderer.getWidth(action.texts.name) + 4
            } else {
                MinecraftClient.getInstance().textRenderer.getWidth(action.texts.name) + 4
            }*/
        }
    }
}