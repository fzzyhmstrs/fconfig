package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.*
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextWidget
import java.util.function.Supplier

class TestPopupScreen(): PopupWidgetScreen(FcText.empty()) {

    override fun onDisplayed() {
        super.onDisplayed()
    }

    override fun removed() {
        super.removed()
    }

    override fun init() {
        super.init()
        addDrawableChild(ButtonWidget.builder("Test Popup".lit()) { openTestPopupWidget() }.dimensions(20,20,90,20).build())
    }

    private fun openTestPopupWidget(){
        val widget = Builder("Test Popup".lit())
            .addElement("test_text_1",TextWidget("Cool Text Text Whee".lit(),MinecraftClient.getInstance().textRenderer), PositionRelativePos.BELOW, PositionGlobalAlignment.ALIGN_LEFT)
            .addElement("test_a_button",ButtonWidget.builder("A".lit()){openTestPopupWidget2({ it.x }, { it.y })}.dimensions(0,0,40,20).build(), PositionRelativePos.BELOW, PositionGlobalAlignment.ALIGN_LEFT)
            .addElement("test_b_button",ButtonWidget.builder("B".lit()){}.dimensions(0,0,40,20).build(), PositionRelativePos.RIGHT, PositionRelativeAlignment.HORIZONTAL_TO_TOP_EDGE, PositionGlobalAlignment.ALIGN_RIGHT)
            .addElement("test_done_button",ButtonWidget.builder("Done".lit()){this.setPopup(null)}.dimensions(0,0,70,20).build(),"test_a_button", PositionRelativePos.BELOW, PositionGlobalAlignment.ALIGN_JUSTIFY)
            .build()
        setPopup(widget)
    }

    private fun openTestPopupWidget2(x: Supplier<Int>, y: Supplier<Int>){
        val widget = Builder("A Popup".lit())
            .addElement("BEEG_BUTTON",ButtonWidget.builder("BEEG".lit()){}.dimensions(0,0,60,90).build(), PositionRelativePos.BELOW, PositionGlobalAlignment.ALIGN_LEFT)
            .addElement("test_a_button",ButtonWidget.builder("A".lit()){}.dimensions(0,0,40,20).build(),"BEEG_BUTTON", PositionRelativePos.RIGHT, PositionRelativeAlignment.HORIZONTAL_TO_TOP_EDGE)
            .addElement("test_b_button",ButtonWidget.builder("B".lit()){}.dimensions(0,0,40,20).build(),"BEEG_BUTTON", PositionRelativePos.RIGHT, PositionRelativeAlignment.CENTERED_VERTICALLY)
            .addElement("test_c_button",ButtonWidget.builder("C".lit()){}.dimensions(0,0,40,20).build(),"BEEG_BUTTON", PositionRelativePos.RIGHT, PositionRelativeAlignment.HORIZONTAL_TO_BOTTOM_EDGE)
            .addElement("test_done_button",ButtonWidget.builder("Done".lit()){this.setPopup(null)}.dimensions(0,0,70,20).build(),"BEEG_BUTTON", PositionRelativePos.BELOW, PositionGlobalAlignment.ALIGN_JUSTIFY)
            .positionX(Builder.at(x))
            .positionY(Builder.at(y))
            .noCloseOnClick()
            .build()
        setPopup(widget)
    }
}