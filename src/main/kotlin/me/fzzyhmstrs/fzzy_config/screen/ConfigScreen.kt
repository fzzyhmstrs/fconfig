package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.screen.widget.ChangesWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextlessConfigActionWidget
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.literal
import me.fzzyhmstrs.fzzy_config.util.FcText.translatable
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.*
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW
import java.util.function.Consumer
import java.util.function.Function

@Environment(EnvType.CLIENT)
class ConfigScreen(title: Text, private val scope: String, private val manager: ConfigScreenManager, private val entriesWidget: Function<ConfigScreen, ConfigListWidget>, private val parentScopesButtons: List<Function<ConfigScreen,ClickableWidget>>) : Screen(title) {

    private var parent: Screen? = null
    private var onClose: Consumer<ConfigScreen> = Consumer {_ -> this.client?.currentScreen = parent}
    private var onOpen: Consumer<ConfigScreen> = Consumer { _ -> }

    internal val layout = ThreePartsLayoutWidget(this)
    internal var configList: ConfigListWidget? = null
    internal var popupWidget: PopupWidget? = null

    fun setOnClose(onClose: Consumer<ConfigScreen>){
        this.onClose = onClose
    }

    fun setOnOpen(onOpen: Consumer<ConfigScreen>){
        this.onOpen = onOpen
    }

    override fun close() {
        onClose.accept(this)
    }

    override fun init() {
        super.init()
        initHeader()
        initFooter()
        initBody()
        onOpen.accept(this)
    }
    private fun initHeader(){
        val directionalLayoutWidget = layout.addHeader(DirectionalLayoutWidget.horizontal().spacing(2))
        for (scopeButton in parentScopesButtons){
            directionalLayoutWidget.add(scopeButton.apply(this))
            directionalLayoutWidget.add(TextWidget(" > ".literal(),this.textRenderer))
        }
        directionalLayoutWidget.add(TextWidget(this.title,this.textRenderer))

    }
    private fun initBody(){
        configList = this.addDrawableChild(entriesWidget.apply(this))
    }
    private fun initFooter(){
        val directionalLayoutWidget = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8))
        //change history button
        directionalLayoutWidget.add(TextlessConfigActionWidget(Identifier(FC.MOD_ID,"widget/action/changelog"), FcText.translatable("fc.button.changelog.active"), FcText.translatable("fc.button.changelog.inactive"),{ UpdateManager.hasChangeHistory() } ) { popupChangelogWidget() })
        //revert changes button
        directionalLayoutWidget.add(ChangesWidget("fc.button.revert".translatable(), { i -> "fc.button.revert.message".translatable(i) },{UpdateManager.getChangeCount(scope)},{ _ -> UpdateManager.revert(scope)}))
        //apply button
        directionalLayoutWidget.add(ChangesWidget("fc.button.apply".translatable(), { i -> "fc.button.apply.message".translatable(i) },{UpdateManager.getChangeCount(scope)},{ _ -> manager.apply()}))
        //done button
        directionalLayoutWidget.add(ButtonWidget.builder(ScreenTexts.DONE) { _ -> manager.apply(); close() }.build())
    }

    override fun initTabNavigation() {
        layout.refreshPositions()
        configList?.position(width, layout)
        popupWidget?.position(width, height)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(popupWidget != null){
            if (popupWidget?.isMouseOver(mouseX, mouseY) == true){
                return popupWidget?.mouseClicked(mouseX, mouseY, button) ?: super.mouseClicked(mouseX, mouseY, button)
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(popupWidget != null){
            if (popupWidget?.isMouseOver(mouseX, mouseY) == true || popupWidget?.isDragging == true) {
                return popupWidget?.mouseReleased(mouseX, mouseY, button) ?: super.mouseReleased(mouseX, mouseY, button)
            }
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if(popupWidget != null){
            if (popupWidget?.isMouseOver(mouseX, mouseY) == true){
                return popupWidget?.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) ?: super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if(popupWidget != null){
            if (keyCode == GLFW.GLFW_KEY_ESCAPE){
                popupWidget?.onClose()
                popupWidget = null
                return true
            }
            if(popupWidget?.keyPressed(keyCode, scanCode, modifiers) == true)
                return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return popupWidget?.keyReleased(keyCode, scanCode, modifiers) ?: super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        return popupWidget?.charTyped(chr, modifiers) ?: super.charTyped(chr, modifiers)
    }

    fun setPopup(widget: PopupWidget){
        popupWidget?.onClose()
        widget.position(width, height)
        popupWidget = widget
    }

    private fun popupChangelogWidget() {

    }

}