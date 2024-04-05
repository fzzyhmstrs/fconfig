package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import java.util.function.Consumer

class ScreenOpenButtonWidget(private val buttonTitle: Text, private val pressAction: Consumer<ScreenOpenButtonWidget>) : PressableWidget(0,0,110,20, FcText.empty()) {

    override fun getNarrationMessage(): MutableText {
        return this.message.copy()
    }

    override fun drawScrollableText(context: DrawContext?, textRenderer: TextRenderer?, xMargin: Int, color: Int) {
        val i = x + xMargin
        val j = x + getWidth() - xMargin
        drawScrollableText(context, textRenderer, buttonTitle, i, y, j, y + getHeight(), color)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    override fun onPress() {
        pressAction.accept(this)
    }
}