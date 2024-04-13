package me.fzzyhmstrs.fzzy_config.screen.widget

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A Button Widget that can supply its message and active state, and render a custom background
 * @param titleSupplier [Supplier]&lt;[Text]&gt; - supplies the message/label for this button
 * @param width Int - width of the widget
 * @param height Int - height of the widget
 * @param activeSupplier [Supplier]&lt;Boolean&gt; - Supplies whether this button is active or not
 * @param pressAction [Consumer]&lt;ActiveButtonWidget&gt; - action to take when the button is pressed
 * @param background [Identifier], optional - a custom background identifier. needs to be a nine-patch sprite
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Environment(EnvType.CLIENT)
open class ActiveButtonWidget(
    private val titleSupplier: Supplier<Text>,
    width: Int,
    height: Int,
    private val activeSupplier: Supplier<Boolean>,
    private val pressAction: Consumer<ActiveButtonWidget>,
    private val background: Identifier? = null)
    :
    PressableWidget(0,0,width,height,titleSupplier.get()) {

    constructor(title: Text,
                width: Int,
                height: Int,
                activeProvider: Supplier<Boolean>,
                pressAction: Consumer<ActiveButtonWidget>,
                background: Identifier? = null): this({title},width, height, activeProvider, pressAction, background)

    override fun getMessage(): Text {
        return titleSupplier.get()
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        this.active = activeSupplier.get()
        if (background != null) {
            if (this.isSelected && active){
                context.setShaderColor(1.0f, 1.0f, 1.0f, alpha)
                RenderSystem.enableBlend()
                RenderSystem.disableDepthTest()
                context.drawGuiTexture(background, x, y, getWidth(), getHeight())
            }
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