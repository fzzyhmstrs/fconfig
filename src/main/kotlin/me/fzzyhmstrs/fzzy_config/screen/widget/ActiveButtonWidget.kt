package me.fzzyhmstrs.fzzy_config.screen.widget

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.updates.UpdateApplier
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
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