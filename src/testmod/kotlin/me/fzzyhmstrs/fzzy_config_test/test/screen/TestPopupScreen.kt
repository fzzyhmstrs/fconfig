/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config_test.test.screen

import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigScreenWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.NewConfigListWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config_test.FC
import me.fzzyhmstrs.fzzy_config_test.FCC.testBoolean
import me.fzzyhmstrs.fzzy_config_test.FCC.testInt
import me.fzzyhmstrs.fzzy_config_test.FCC.testInt2
import me.fzzyhmstrs.fzzy_config_test.FCC.testString
import me.fzzyhmstrs.fzzy_config_test.test.TestBasicConfigManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextWidget
import java.util.function.Function
import java.util.function.Supplier

class TestPopupScreen(private val size: Int = 5): PopupWidgetScreen(FcText.empty()) {

    val testBasicConfigManager = TestBasicConfigManager()

    //val testEnumWidget = testEnum.widgetEntry(ChoiceValidator.any())
    //val testEnum2Widget = testEnum2.widgetEntry(ChoiceValidator.any())
    val testIntWidget = testInt.widgetEntry(ChoiceValidator.any())
    val testInt2Widget = testInt2.widgetEntry(ChoiceValidator.any())
    val testStringWidget = testString.widgetEntry(ChoiceValidator.any())
    val testBooleanWidget = testBoolean.widgetEntry(ChoiceValidator.any())
    val listTestWidget = configWidget(size)

    override fun close() {
        super.close()
        //TestConfigApi.printChangeHistory(manager.flush(), "Test Screen Closing", client?.player)
        this.client?.narratorManager?.clear()
    }

    private fun configWidget(size: Int): NewConfigListWidget {
        val list: MutableList<Function<NewConfigListWidget, TestEntry>> = mutableListOf()
        for (i in 1..size) {
            list.add(Function { widget -> TestEntry(widget, "test.entry.$i", ButtonWidget.builder(i.toString().lit()) { _ -> FC.LOGGER.info("I Pressed {}", i) }.size(75, 20).build()) })
        }
        return NewConfigListWidget(MinecraftClient.getInstance(), list, 0, 0, 200, 200)
    }

    override fun init() {
        super.init()
        addDrawableChild(ButtonWidget.builder("Test Popup".lit()) { openTestPopupWidget() }.dimensions(20, 20, 110, 20).build())
        addDrawableChild(ButtonWidget.builder("Done".lit()) { close() }.dimensions(20, 50, 110, 20).build())
        testIntWidget.setPosition(20, 80)
        addDrawableChild(testIntWidget)
        testInt2Widget.setPosition(140, 80)
        addDrawableChild(testInt2Widget)
        testStringWidget.setPosition(20, 110)
        addDrawableChild(testStringWidget)
        testBooleanWidget.setPosition(20, 140)
        addDrawableChild(testBooleanWidget)
        listTestWidget.setPosition(140, 110)
        addDrawableChild(listTestWidget)

        addDrawableChild(ConfigScreenWidget.of("fzzy_config_test", ConfigScreenWidget.Position.Corner.TOP_LEFT))
    }

    private fun openTestPopupWidget() {
        val widget = Builder("Test Popup".lit())
            .addDivider()
            .addElement("test_text_1", TextWidget("Cool Text Text Whee".lit(), MinecraftClient.getInstance().textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("test_a_button", ButtonWidget.builder("\"A\"".lit()){openTestPopupWidgetA({ it.x }, { it.y })}.dimensions(0, 0, 40, 20).build(), Position.BELOW, Position.ALIGN_LEFT_AND_JUSTIFY)
            .addElement("test_b_button", ButtonWidget.builder("B".lit()){ openTestPopupWidgetB() }.dimensions(0, 0, 40, 20).build(), Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE, Position.ALIGN_RIGHT)
            .addElement("test_done_button", ButtonWidget.builder("Done... Wow".lit()){this.setPopup(null)}.dimensions(0, 0, 70, 20).build(), "test_a_button", Position.BELOW, Position.ALIGN_JUSTIFY)
            .build()
        setPopup(widget)
    }

    private fun openTestPopupWidgetA(x: Supplier<Int>, y: Supplier<Int>) {
        val widget = Builder("A Popup".lit())
            .addElement("BEEG_BUTTON", ButtonWidget.builder("BEEG".lit()){}.dimensions(0, 0, 60, 90).build(), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("test_a_button", ButtonWidget.builder("A".lit()){}.dimensions(0, 0, 40, 20).build(), "BEEG_BUTTON", Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
            .addElement("test_b_button", ButtonWidget.builder("B".lit()){}.dimensions(0, 0, 40, 20).build(), "BEEG_BUTTON", Position.RIGHT, Position.CENTERED_VERTICALLY)
            .addElement("test_c_button", ButtonWidget.builder("C".lit()){}.dimensions(0, 0, 40, 20).build(), "BEEG_BUTTON", Position.RIGHT, Position.HORIZONTAL_TO_BOTTOM_EDGE)
            .addElement("test_done_button", ButtonWidget.builder("Done".lit()){this.setPopup(null)}.dimensions(0, 0, 70, 20).build(), "BEEG_BUTTON", Position.BELOW, Position.ALIGN_JUSTIFY)
            .positionX(Builder.at(x))
            .positionY(Builder.at(y))
            .noCloseOnClick()
            .build()
        setPopup(widget)
    }

    private fun openTestPopupWidgetB() {
        val widget = Builder("Test Popup".lit())
            .addDivider()
            .addElement("test_text_1", TextWidget("Cool Text Text Whee".lit(), MinecraftClient.getInstance().textRenderer), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("test_a_button", ButtonWidget.builder("\"A\"".lit()){}.dimensions(0, 0, 40, 20).build(), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("test_b_button", ButtonWidget.builder("B".lit()){}.dimensions(0, 0, 40, 20).build(), Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE, Position.ALIGN_RIGHT_AND_JUSTIFY)
            .addElement("test_done_button", ButtonWidget.builder("Done... Wow".lit()){this.setPopup(null)}.dimensions(0, 0, 70, 20).build(), "test_a_button", Position.BELOW, Position.ALIGN_JUSTIFY)
            .build()
        setPopup(widget)
    }
}