package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.util.FcText
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.KeyCodes
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A button widget that masquerades as a text field widget. The text within is not editable.
 * @param textSupplier [Supplier]&lt;String&gt; - supplier of the message the "text field" displays
 * @param onClick [Consumer]&lt;OnClickTextFieldWidget&gt; - action to take when the button is pressed
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Environment(EnvType.CLIENT)
class OnClickTextFieldWidget(private val textSupplier: Supplier<String>, private val onClick: Consumer<OnClickTextFieldWidget>)
    :
    TextFieldWidget(MinecraftClient.getInstance().textRenderer,0,0, 110, 20, FcText.empty())
{
    init {
        setMaxLength(1000)
        this.text = textSupplier.get()
    }

    override fun renderWidget(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderWidget(context, mouseX, mouseY, delta)
        this.text = textSupplier.get()
    }

    override fun onClick(mouseX: Double, mouseY: Double) {
        onClick.accept(this)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return if (!this.isFocused) {
            false
        } else if(KeyCodes.isToggle(keyCode)) {
            onClick.accept(this)
            return true
        } else super.keyPressed(keyCode, scanCode, modifiers)
    }

}