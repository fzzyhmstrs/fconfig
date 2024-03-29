package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.MutableText

class NoPermsButtonWidget : PressableWidget(0,0,90,20, FcText.empty()) {

    init{
        this.active = false
    }

    private val title = FcText.translatable("fc.button.noPerms")

    override fun getNarrationMessage(): MutableText {
        return this.message.copy()
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, this.narrationMessage)
        //builder.put(NarrationPart.USAGE, FcText.translatable("narration.component_list.usage"))
    }

    override fun drawScrollableText(context: DrawContext?, textRenderer: TextRenderer?, xMargin: Int, color: Int) {
        val i = x + xMargin
        val j = x + getWidth() - xMargin
        drawScrollableText(context, textRenderer, title, i, y, j, y + getHeight(), color)
    }

    override fun onPress() {
    }
}