package me.fzzyhmstrs.fzzy_config.screen.widget

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text

class PopupWidget(x: Int, y: Int, width: Int, height: Int, message: Text) :
    ClickableWidget(x, y, width, height, message) {

    override fun renderWidget(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        TODO("Not yet implemented")
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        TODO("Not yet implemented")
    }
}