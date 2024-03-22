package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.RevertChangesWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextlessConfigActionWidget
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.literal
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.DirectionalLayoutWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Function

@Environment(EnvType.CLIENT)
class ConfigScreen(title: Text, private val scope: String, private val entriesWidget: Function<ConfigScreen,ConfigListWidget>, private val parentScopesButtons: List<Function<ConfigScreen,ClickableWidget>>) : Screen(title) {

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
        directionalLayoutWidget.add(RevertChangesWidget({UpdateManager.getChangeCount(scope)},{_ -> UpdateManager.revert(scope)}))
        //apply button

        //done button

    }

    private fun popupChangelogWidget(){

    }

    override fun initTabNavigation() {
        layout.refreshPositions()
        configList?.position(width, layout)
    }

    override fun close() {
        onClose.accept(this)
    }

}