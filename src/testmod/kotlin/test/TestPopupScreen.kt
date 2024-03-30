package test

import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.PositionRelativePos
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextWidget

class TestPopupScreen: PopupWidgetScreen(FcText.empty()) {

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fillGradient(0, 0, width, height, -1072689136, -804253680)
    }

    override fun init() {
        super.init()
        addDrawableChild(ButtonWidget.builder("Test Popup".lit()) { openTestPopupWidget() }.dimensions(20,20,90,20).build())
    }

    private fun openTestPopupWidget(){
        Builder("Test Popup".lit())
            .addElement("test_text_1",TextWidget("Cool Text Text Whee".lit(),MinecraftClient.getInstance().textRenderer),"title", PositionRelativePos.BELOW)
            .addElement("test_done_button",ButtonWidget.builder("Done".lit()){this.setPopup(null)}.dimensions(0,0,70,20).build(),"test_text_1", PositionRelativePos.BELOW)
    }
}