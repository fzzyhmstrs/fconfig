package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.ChangesWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextlessConfigActionWidget
import me.fzzyhmstrs.fzzy_config.updates.UpdateApplier
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.*
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import java.util.function.Consumer
import java.util.function.Function

@Environment(EnvType.CLIENT)
internal class ConfigScreen(title: Text, private val manager: UpdateApplier, private val entriesWidget: Function<ConfigScreen, ConfigListWidget>, private val parentScopesButtons: List<Function<ConfigScreen,ClickableWidget>>) : PopupWidgetScreen(title) {

    internal var parent: Screen? = null
    private var onClose: Consumer<ConfigScreen> = Consumer {_ -> if(this.parent == null) manager.apply(); this.client?.setScreen(parent)}
    private var onOpen: Consumer<ConfigScreen> = Consumer { _ -> }

    internal val layout = ThreePartsLayoutWidget(this)
    internal var configList: ConfigListWidget? = null

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
        configList = this.addDrawableChild(entriesWidget.apply(this))
    }
    private fun initFooter(){
        val directionalLayoutWidget = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8))
        //forward alert button
        directionalLayoutWidget.add(TextlessConfigActionWidget("widget/action/alert".fcId(),"widget/action/alert_inactive".fcId(),"widget/action/alert_highlighted".fcId(), "fc.button.alert.active".translate(), "fc.button.alert.inactive".translate(),{ manager.hasForwards() } ) { manager.forwardedWidget() })
        //change history button
        directionalLayoutWidget.add(TextlessConfigActionWidget("widget/action/changelog".fcId(),"widget/action/changelog_inactive".fcId(),"widget/action/changelog_highlighted".fcId(), "fc.button.changelog.active".translate(), "fc.button.changelog.inactive".translate(),{ manager.changes() > 0 } ) { manager.changesWidget() })
        //revert changes button
        directionalLayoutWidget.add(ChangesWidget("fc.button.revert".translate(), { i -> "fc.button.revert.message".translate(i) }, { manager.changes() }, { _ -> manager.revert()}))
        //apply button
        directionalLayoutWidget.add(ChangesWidget("fc.button.apply".translate(), { i -> "fc.button.apply.message".translate(i) }, { manager.changes() }, { _ -> manager.apply() }))
        //done button
        directionalLayoutWidget.add(ButtonWidget.builder(ScreenTexts.DONE) { _ -> close() }.build())
    }

    override fun initTabNavigation() {
        layout.refreshPositions()
        configList?.position(width, layout)
    }
}