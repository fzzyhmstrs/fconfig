package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.util.FcText
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Supplier

@Environment(EnvType.CLIENT)
class TextlessConfigActionWidget(
    private val activeIcon: Identifier,
    private val inactiveIcon: Identifier,
    private val highlightedIcon: Identifier,
    private val activeNarration: Text,
    private val inactiveNarration: Text,
    private val canPress: Supplier<Boolean>,
    private val pressAction: Consumer<TextlessConfigActionWidget>)
    :
    PressableWidget(0,0, 20, 20, FcText.empty())
{

    private fun getTex(): Identifier{
        if(!active)
            return inactiveIcon
        return if (hovered || isFocused)
            highlightedIcon
        else
            activeIcon
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        this.active = canPress.get()
        //super.renderWidget(context, mouseX, mouseY, delta)
        context.drawGuiTexture(getTex(), x, y, getWidth(), getHeight())
    }

    override fun getNarrationMessage(): MutableText {
        return if(active) activeNarration.copy() else inactiveNarration.copy()
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    override fun onPress() {
       pressAction.accept(this)
    }


}