/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.context.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.ActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.Popups
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.MutableText
import net.minecraft.util.Identifier
import java.util.function.Supplier
import java.util.function.UnaryOperator

//client
internal class ChangesWidget(private val scope: String, private val widthSupplier: Supplier<Int>, private val manager: UpdateManager): CustomPressableWidget(0, 0, 80, 20, "fc.button.changes".translate()) {

    companion object {
        private val changesTex: Identifier = "widget/changes".fcId()
        private val changesHighlightedTex: Identifier = "widget/changes_highlighted".fcId()
    }

    override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        this.active = manager.hasChanges() || manager.hasChangeHistory() || manager.hasRestores(scope)
        super.renderCustom(context, x, y, width, height, mouseX, mouseY, delta)
        RenderSystem.enableBlend()
        if (manager.hasChanges()) {
            if (isFocused || isHovered)
                context.drawTex(changesHighlightedTex, x + 68, y - 4, 16, 16)
            else
                context.drawTex(changesTex, x + 67, y - 4, 16, 16)
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, manager.changeCount().toString(), x + 76, y, 0xFFFFFF)
        }
    }

    override fun getNarrationMessage(): MutableText {
        return if (manager.hasChanges()) "fc.button.changes.message".translate(manager.changeCount()) else "fc.button.changes.message.noChanges".translate()
    }

    override fun onPress() {
        openChangesPopup()
    }

    private fun openChangesPopup() {
        val client = MinecraftClient.getInstance()
        val applyText = "fc.button.apply".translate()
        val revertText = "fc.button.revert".translate()
        val restoreText = "fc.button.restore".translate()
        val changelogText = "fc.button.changelog".translate()
        val popup = PopupWidget.Builder("fc.button.changes.title".translate())
            // Apply Changes
            .add("apply",
                ActiveButtonWidget(applyText, client.textRenderer.getWidth(applyText) + 8, 20, { manager.hasChanges() }, { manager.apply(false) }),
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_JUSTIFY)
            .pushSpacing(UnaryOperator.identity()) { _ -> 2 }
            // Revert Changes
            .add("revert",
                ActiveButtonWidget(revertText, client.textRenderer.getWidth(revertText) + 8, 20, { manager.hasChanges() }, { manager.revert() }),
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_JUSTIFY)
            // Restore Defaults > confirm popup
            .add("restore",
                ActiveButtonWidget(restoreText, client.textRenderer.getWidth(restoreText) + 8, 20, { manager.hasRestores(scope) }, { b -> Popups.openConfirmPopup(Position.fromWidget(b), "fc.config.restore.confirm.desc".translate()) { manager.restore(scope) } }),
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_JUSTIFY)
            // Change History
            .add("changelog",
                ActiveButtonWidget(changelogText, client.textRenderer.getWidth(changelogText) + 8, 20, { manager.hasChangeHistory() }, { openChangelogPopup() }),
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_JUSTIFY)
            .addDoneWidget(spacingH = 2)
            .popSpacing()
            .positionX(PopupWidget.Builder.at { this.x - 8 })
            .positionY(PopupWidget.Builder.popupContext { h -> this.y - h + 28 })
            .build()
        PopupWidget.push(popup)
    }

    private fun openChangelogPopup() {
        val changes = manager.changeHistory()
        val popup = PopupWidget.Builder("fc.button.changelog".translate())
            .add("changelog",
                ChangelogListWidget(changes, widthSupplier),
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_LEFT)
            .add("done_button",
                CustomButtonWidget.builder(ScreenTexts.DONE) { PopupWidget.pop() }.size(50, 20).build(),
                LayoutWidget.Position.BELOW,
                LayoutWidget.Position.ALIGN_JUSTIFY)
            .positionX(PopupWidget.Builder.at { 0 })
            .build()
        PopupWidget.push(popup)
        //
    }
}