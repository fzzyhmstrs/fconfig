package me.fzzyhmstrs.fzzy_config.screen.widget

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import java.util.function.Consumer
import java.util.function.Supplier

class ActiveButtonWidget(
    title: Text,
    width: Int,
    height: Int,
    private val activeProvider: Supplier<Boolean>,
    private val pressAction: Consumer<ActiveButtonWidget>,
    private val background: Boolean = true)
    :
    PressableWidget(0,0,width,height,title) {

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        this.active = activeProvider.get()
        if (!background) {
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
            val i = if (active) 0xFFFFFF else 0xA0A0A0
            drawMessage(context, MinecraftClient.getInstance().textRenderer, i or (MathHelper.ceil(alpha * 255.0f) shl 24))
        } else {
            super.renderWidget(context, mouseX, mouseY, delta)
        }
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    override fun onPress() {
        pressAction.accept(this)
    }
}