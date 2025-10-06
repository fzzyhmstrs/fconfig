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

import com.google.common.base.Suppliers
import me.fzzyhmstrs.fzzy_config.screen.PopupWidgetScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigScreenWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.SuppliedTextWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomMultilineTextWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.function.ConstSupplier
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config_test.FCC.testBoolean
import me.fzzyhmstrs.fzzy_config_test.FCC.testInt
import me.fzzyhmstrs.fzzy_config_test.FCC.testInt2
import me.fzzyhmstrs.fzzy_config_test.FCC.testString
import me.fzzyhmstrs.fzzy_config_test.test.TestBasicConfigManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.input.KeyInput
import net.minecraft.text.ClickEvent
import org.lwjgl.glfw.GLFW
import java.util.function.BiFunction
import java.util.function.Supplier

class TestPopupScreen(size: Int = 5): PopupWidgetScreen(FcText.empty()) {

    val testBasicConfigManager = TestBasicConfigManager()

    //val testEnumWidget = testEnum.widgetEntry(ChoiceValidator.any())
    //val testEnum2Widget = testEnum2.widgetEntry(ChoiceValidator.any())
    val testIntWidget = testInt.widgetEntry(ChoiceValidator.any())
    val testInt2Widget = testInt2.widgetEntry(ChoiceValidator.any())
    val testStringWidget = testString.widgetEntry(ChoiceValidator.any())
    val testBooleanWidget = testBoolean.widgetEntry(ChoiceValidator.any())
    val listTestWidget = configWidget(size)
    val groupButton = ButtonWidget.builder("Toggle".lit()) { _ -> listTestWidget.toggleGroup("2") }.size(50, 20).build()
    val suppliedText = SuppliedTextWidget(ConstSupplier(FcText.empty()), MinecraftClient.getInstance().textRenderer, 100, 20)
    val suppliedText2 = SuppliedTextWidget(ConstSupplier(FcText.empty()), MinecraftClient.getInstance().textRenderer, 100, 20)

    val multilineTextWidget = CustomMultilineTextWidget(
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit,".lit()
            .append("sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.".lit().styled { s -> s.withUnderline(true).withClickEvent(ClickEvent.RunCommand("/give @s minecraft:stick")) })
            .append("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."))

    override fun close() {
        super.close()
        //TestConfigApi.printChangeHistory(manager.flush(), "Test Screen Closing", client?.player)
        this.client?.narratorManager?.clear()
    }

    private fun configWidget(size: Int): DynamicListWidget {
        val list: MutableList<BiFunction<DynamicListWidget, Int, TestEntry>> = mutableListOf()
        for (i in 1..size) {
            list.add(BiFunction { widget, _ -> TestEntry(widget, "test.entry.$i", (((i - 1) % 3) + 1), i, i.toString().lit()) })
        }
        return DynamicListWidget(MinecraftClient.getInstance(), list, 0, 0, 200, 100)
    }

    override fun init() {
        super.init()
        println(suppliedText)
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
        groupButton.setPosition(260, 80)
        addDrawableChild(groupButton)
        multilineTextWidget.setDimensionsAndPosition(240, 320, 140, 4)
        addDrawableChild(multilineTextWidget)

        addDrawableChild(ConfigScreenWidget.of("fzzy_config_test", ConfigScreenWidget.Position.Corner.TOP_LEFT))
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (input.key == GLFW.GLFW_KEY_PAGE_UP) {
            listTestWidget.page(true)
            return true
        } else if (input.key == GLFW.GLFW_KEY_PAGE_DOWN) {
            listTestWidget.page(false)
            return true
        }
        return super.keyPressed(input )
    }

    private fun openTestPopupWidget() {
        val widget = Builder("Test Popup".lit())
            .addDivider()
            .add("test_text_1", TextWidget("Cool Text Text Whee".lit(), MinecraftClient.getInstance().textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("test_a_button", ButtonWidget.builder("\"A\"".lit()){openTestPopupWidgetA({ it.x }, { it.y })}.dimensions(0, 0, 40, 20).build(), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY)
            .add("test_b_button", ButtonWidget.builder("B".lit()){ openTestPopupWidgetB() }.dimensions(0, 0, 40, 20).build(), LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE, LayoutWidget.Position.ALIGN_RIGHT)
            .add("test_done_button", ButtonWidget.builder("Done... Wow".lit()){this.setPopup(null)}.dimensions(0, 0, 70, 20).build(), "test_a_button", LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_JUSTIFY)
            .build()
        setPopup(widget)
    }

    private fun openTestPopupWidgetA(x: Supplier<Int>, y: Supplier<Int>) {
        val widget = Builder("A Popup".lit())
            .add("BEEG_BUTTON", ButtonWidget.builder("BEEG".lit()){}.dimensions(0, 0, 60, 90).build(), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("test_a_button", ButtonWidget.builder("A".lit()){}.dimensions(0, 0, 40, 20).build(), "BEEG_BUTTON", LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .add("test_b_button", ButtonWidget.builder("B".lit()){}.dimensions(0, 0, 40, 20).build(), "BEEG_BUTTON", LayoutWidget.Position.RIGHT, LayoutWidget.Position.CENTERED_VERTICALLY)
            .add("test_c_button", ButtonWidget.builder("C".lit()){}.dimensions(0, 0, 40, 20).build(), "BEEG_BUTTON", LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_BOTTOM_EDGE)
            .add("test_done_button", ButtonWidget.builder("Done".lit()){this.setPopup(null)}.dimensions(0, 0, 70, 20).build(), "BEEG_BUTTON", LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_JUSTIFY)
            .positionX(Builder.at(x))
            .positionY(Builder.at(y))
            .noCloseOnClick()
            .build()
        setPopup(widget)
    }

    private fun openTestPopupWidgetB() {
        val widget = Builder("Test Popup".lit())
            .addDivider()
            .add("test_text_1", TextWidget("Cool Text Text Whee".lit(), MinecraftClient.getInstance().textRenderer), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("test_a_button", ButtonWidget.builder("\"A\"".lit()){}.dimensions(0, 0, 40, 20).build(), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("test_b_button", ButtonWidget.builder("B".lit()){}.dimensions(0, 0, 40, 20).build(), LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE, LayoutWidget.Position.ALIGN_RIGHT_AND_JUSTIFY)
            .add("test_done_button", ButtonWidget.builder("Done... Wow".lit()){this.setPopup(null)}.dimensions(0, 0, 70, 20).build(), "test_a_button", LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_JUSTIFY)
            .build()
        setPopup(widget)
    }
}