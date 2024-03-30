package me.fzzyhmstrs.fzzy_config.screen.widget

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.fcId
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

class ChangesWidget(title: Text, private val message: Function<Int,MutableText>, private val changesSupplier: Supplier<Int>, private val pressAction: Consumer<ChangesWidget>): PressableWidget(0,0,80,20,title) {

    companion object{
        private val changesTex: Identifier = "widget/changes".fcId()
        private val changesHighlightedTex: Identifier = "widget/changes_highlighted".fcId()
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val changes = changesSupplier.get()
        this.active = changes > 0
        super.renderWidget(context, mouseX, mouseY, delta)
        RenderSystem.enableBlend()
        if (changesSupplier.get() > 0) {
            if (isFocused || isHovered)
                context.drawGuiTexture(changesHighlightedTex, x + 68, y - 4, 16, 16)
            else
                context.drawGuiTexture(changesTex, x + 68, y - 4, 16, 16)
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, changesSupplier.get().toString(),x + 76, y,0xFFFFFF)
        }
    }

    override fun getNarrationMessage(): MutableText {
        return message.apply(changesSupplier.get())
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    override fun onPress() {
        pressAction.accept(this)
    }
}