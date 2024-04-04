package me.fzzyhmstrs.fzzy_config.screen.widget

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.Text
import java.util.function.Consumer
import java.util.function.Supplier

class ActiveButtonWidget(title: Text, width: Int, height: Int, private val activeProvider: Supplier<Boolean>, private val pressAction: Consumer<ActiveButtonWidget>): PressableWidget(0,0,width,height,title) {

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        this.active = activeProvider.get()
        super.renderWidget(context, mouseX, mouseY, delta)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    override fun onPress() {
        pressAction.accept(this)
    }
}