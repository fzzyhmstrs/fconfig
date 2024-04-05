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
import net.minecraft.client.gui.widget.*
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Colors
import java.util.function.Consumer
import java.util.function.Function

@Environment(EnvType.CLIENT)
internal class ConfigScreen(title: Text, private val scope: String, private val manager: UpdateManager, private val entriesWidget: Function<ConfigScreen, ConfigListWidget>, private val parentScopesButtons: List<Function<ConfigScreen,ClickableWidget>>) : PopupWidgetScreen(title) {

    internal var parent: Screen? = null
    private var onClose: Consumer<ConfigScreen> = Consumer {_ ->
        if(this.parent == null) {
            manager.apply()
            this.client?.narratorManager?.clear()
        }
        this.client?.setScreen(parent)
    }
    private var onOpen: Consumer<ConfigScreen> = Consumer { _ -> }

    internal val layout = ThreePartsLayoutWidget(this)
    private val configList: ConfigListWidget = entriesWidget.apply(this)

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
            directionalLayoutWidget.add(TextWidget(" > ".lit(),this.textRenderer))
        }
        directionalLayoutWidget.add(TextWidget(this.title,this.textRenderer))

    }
    private fun initBody(){
        this.addDrawableChild(configList)
    }
    private fun initFooter(){
        val directionalLayoutWidget = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8))
        //search bar
        val textField = TextFieldWidget(MinecraftClient.getInstance().textRenderer,110,20,FcText.empty())
        fun setColor(entries: Int){
            if(entries > 0)
                textField.setEditableColor(Colors.WHITE)
            else
                textField.setEditableColor(0xFF5555)
        }
        textField.setMaxLength(50)
        textField.text = ""
        textField.setChangedListener { s -> setColor(configList.updateSearchedEntries(s)) }
        //forward alert button
        directionalLayoutWidget.add(TextlessConfigActionWidget("widget/action/alert".fcId(),"widget/action/alert_inactive".fcId(),"widget/action/alert_highlighted".fcId(), "fc.button.alert.active".translate(), "fc.button.alert.inactive".translate(),{ manager.hasForwards() } ) { manager.forwardsHandler() })
        //changes button
        directionalLayoutWidget.add(ChangesWidget(scope, { this.width }, manager))
        //done button
        directionalLayoutWidget.add(ButtonWidget.builder(ScreenTexts.DONE) { _ -> close() }.size(70,20).build())
    }

    override fun initTabNavigation() {
        layout.refreshPositions()
        configList.position(width, layout)
    }
}