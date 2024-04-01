package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.*
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum
import me.fzzyhmstrs.fzzy_config_test.FCC.testEnum
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Selectable.SelectionType
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.util.DyeColor
import java.util.function.Supplier

class TestPopupScreen: PopupWidgetScreen(FcText.empty()) {

    val testEnumWidget = testEnum.widgetEntry(ChoiceValidator.any())

    override fun close() {
        super.close()
        this.client?.narratorManager?.clear()
    }

    override fun init() {
        super.init()
        addDrawableChild(ButtonWidget.builder("Test Popup".lit()) { openTestPopupWidget() }.dimensions(20,20,90,20).build())
        addDrawableChild(ButtonWidget.builder("Done".lit()) { close() }.dimensions(20,50,90,20).build())
        testEnumWidget.setPosition(20, 80)
        testEnumWidget.tooltip = Tooltip.of(testEnum.description())
        addDrawableChild(testEnumWidget)
    }

    private fun openTestPopupWidget(){
        val widget = Builder("Test Popup".lit())
            .addDivider()
            .addElement("test_text_1",TextWidget("Cool Text Text Whee".lit(),MinecraftClient.getInstance().textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("test_a_button",ButtonWidget.builder("\"A\"".lit()){openTestPopupWidgetA({ it.x }, { it.y })}.dimensions(0,0,40,20).build(), Position.BELOW, Position.ALIGN_LEFT_AND_JUSTIFY)
            .addElement("test_b_button",ButtonWidget.builder("B".lit()){ openTestPopupWidgetB() }.dimensions(0,0,40,20).build(), Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE, Position.ALIGN_RIGHT)
            .addElement("test_done_button",ButtonWidget.builder("Done... Wow".lit()){this.setPopup(null)}.dimensions(0,0,70,20).build(),"test_a_button", Position.BELOW, Position.ALIGN_JUSTIFY)
            .build()
        setPopup(widget)
    }

    private fun openTestPopupWidgetA(x: Supplier<Int>, y: Supplier<Int>){
        val widget = Builder("A Popup".lit())
            .addElement("BEEG_BUTTON",ButtonWidget.builder("BEEG".lit()){}.dimensions(0,0,60,90).build(), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("test_a_button",ButtonWidget.builder("A".lit()){}.dimensions(0,0,40,20).build(),"BEEG_BUTTON", Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
            .addElement("test_b_button",ButtonWidget.builder("B".lit()){}.dimensions(0,0,40,20).build(),"BEEG_BUTTON", Position.RIGHT, Position.CENTERED_VERTICALLY)
            .addElement("test_c_button",ButtonWidget.builder("C".lit()){}.dimensions(0,0,40,20).build(),"BEEG_BUTTON", Position.RIGHT, Position.HORIZONTAL_TO_BOTTOM_EDGE)
            .addElement("test_done_button",ButtonWidget.builder("Done".lit()){this.setPopup(null)}.dimensions(0,0,70,20).build(),"BEEG_BUTTON", Position.BELOW, Position.ALIGN_JUSTIFY)
            .positionX(Builder.at(x))
            .positionY(Builder.at(y))
            .noCloseOnClick()
            .build()
        setPopup(widget)
    }

    private fun openTestPopupWidgetB(){
        val widget = Builder("Test Popup".lit())
            .addDivider()
            .addElement("test_text_1",TextWidget("Cool Text Text Whee".lit(),MinecraftClient.getInstance().textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("test_a_button",ButtonWidget.builder("\"A\"".lit()){}.dimensions(0,0,40,20).build(), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("test_b_button",ButtonWidget.builder("B".lit()){}.dimensions(0,0,40,20).build(), PositionRelativePos.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE, Position.ALIGN_RIGHT_AND_JUSTIFY)
            .addElement("test_done_button",ButtonWidget.builder("Done... Wow".lit()){this.setPopup(null)}.dimensions(0,0,70,20).build(),"test_a_button", Position.BELOW, Position.ALIGN_JUSTIFY)
            .build()
        setPopup(widget)
    }
}