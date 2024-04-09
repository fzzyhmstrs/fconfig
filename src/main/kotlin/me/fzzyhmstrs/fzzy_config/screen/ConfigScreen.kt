package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.ChangesWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextlessConfigActionWidget
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.*
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Colors
import org.lwjgl.glfw.GLFW
import java.util.function.Function

@Environment(EnvType.CLIENT)
internal class ConfigScreen(title: Text, private val scope: String, private val manager: UpdateManager, entriesWidget: Function<ConfigScreen, ConfigListWidget>, private val parentScopesButtons: List<Function<ConfigScreen,ClickableWidget>>) : PopupWidgetScreen(title) {

    private var parent: Screen? = null

    internal val layout = ThreePartsLayoutWidget(this)
    private val searchField = TextFieldWidget(MinecraftClient.getInstance().textRenderer,110,20,FcText.empty())
    private val doneButton = ButtonWidget.builder(ScreenTexts.DONE) { _ -> close() }.size(70,20).build()
    private val configList: ConfigListWidget = entriesWidget.apply(this)

    fun setParent(screen: Screen?) {
        this.parent = screen
        if (screen !is ConfigScreen) return
        doneButton.message = "fc.config.back".translate()
        doneButton.tooltip = Tooltip.of("fc.config.back.desc".translate(screen.title))
    }

    override fun close() {
        if(this.parent == null || this.parent !is ConfigScreen) {
            manager.apply(true)
            this.client?.narratorManager?.clear()
        }
        this.client?.setScreen(parent)
    }

    override fun init() {
        super.init()
        initHeader()
        initFooter()
        initBody()
        initTabNavigation()
    }
    private fun initHeader() {
        val directionalLayoutWidget = layout.addHeader(DirectionalLayoutWidget.horizontal().spacing(2))
        for (scopeButton in parentScopesButtons) {
            directionalLayoutWidget.add(scopeButton.apply(this))
            directionalLayoutWidget.add(TextWidget(textRenderer.getWidth(" > ".lit()),20," > ".lit(),this.textRenderer))
        }
        directionalLayoutWidget.add(TextWidget(textRenderer.getWidth(this.title),20,this.title,this.textRenderer))

    }
    private fun initBody() {
        this.addDrawableChild(configList)
        layout.forEachChild { drawableElement: ClickableWidget? ->
            addDrawableChild(drawableElement)
        }
        configList.scrollAmount = 0.0
    }
    private fun initFooter() {
        val directionalLayoutWidget = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8))
        //search bar
        fun setColor(entries: Int){
            if(entries > 0)
                searchField.setEditableColor(Colors.WHITE)
            else
                searchField.setEditableColor(0xFF5555)
        }
        searchField.setMaxLength(50)
        searchField.text = ""
        searchField.setChangedListener { s -> setColor(configList.updateSearchedEntries(s)) }
        directionalLayoutWidget.add(searchField)
        //forward alert button
        directionalLayoutWidget.add(TextlessConfigActionWidget("widget/action/alert".fcId(),"widget/action/alert_inactive".fcId(),"widget/action/alert_highlighted".fcId(), "fc.button.alert.active".translate(), "fc.button.alert.inactive".translate(),{ manager.hasForwards() } ) { manager.forwardsHandler() })
        //changes button
        directionalLayoutWidget.add(ChangesWidget(scope, { this.width }, manager))
        //done button
        directionalLayoutWidget.add(doneButton)
    }

    override fun initTabNavigation() {
        layout.refreshPositions()
        configList.position(width, layout)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            configList.page(true)
            return true
        } else if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            configList.page(false)
            return true
        } else if (isCopy(keyCode)) {
            configList.copy()
            return true
        } else if (isPaste(keyCode)) {
            configList.paste()
            return true
        } else if (keyCode == GLFW.GLFW_KEY_Z && hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            manager.revertLast()
            return true
        } else if (keyCode == GLFW.GLFW_KEY_F && hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            this.focused = searchField
            return true
        } else if (keyCode == GLFW.GLFW_KEY_S && hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            manager.apply(false)
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}