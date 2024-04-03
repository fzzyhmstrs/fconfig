package me.fzzyhmstrs.fzzy_config.screen.widget

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.updates.UpdateApplier
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.MultilineTextWidget
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.MutableText
import net.minecraft.util.Identifier
import java.util.function.Supplier
import kotlin.math.max

class ChangesWidget(private val scope: String, private val widthSupplier: Supplier<Int>, private val changesSupplier: Supplier<Int>, private val updateApplier: UpdateApplier): PressableWidget(0,0,80,20,"fc.button.changes".translate()) {

    companion object{
        private val changesTex: Identifier = "widget/changes".fcId()
        private val changesHighlightedTex: Identifier = "widget/changes_highlighted".fcId()
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val changes = changesSupplier.get()
        this.active = changes > 0
        super.renderWidget(context, mouseX, mouseY, delta)
        RenderSystem.enableBlend()
        if (changesSupplier.get() > 0) {
            if (isFocused || isHovered)
                context.drawGuiTexture(changesHighlightedTex, x + 68, y - 4, 16, 16)
            else
                context.drawGuiTexture(changesTex, x + 68, y - 4, 16, 16)
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, changesSupplier.get().toString(),x + 76, y,0xFFFFFF)
        }
    }

    override fun getNarrationMessage(): MutableText {
        return "fc.button.changes.message".translate()
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
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
        val popup = PopupWidget.Builder("fc.button.changes.message".translate())
            // Apply Changes
            .addElement("apply", ActiveButtonWidget(applyText,client.textRenderer.getWidth(applyText) + 8, 20, { updateApplier.hasChanges() }, { updateApplier.apply() }), Position.BELOW, Position.ALIGN_JUSTIFY)
            // Revert Changes
            .addElement("revert", ActiveButtonWidget(revertText,client.textRenderer.getWidth(revertText) + 8, 20, { updateApplier.hasChanges() }, { updateApplier.revert() }), Position.BELOW, Position.ALIGN_JUSTIFY)
            // Restore Defaults > confirm popup
            .addElement("restore", ActiveButtonWidget(restoreText,client.textRenderer.getWidth(restoreText) + 8, 20, { true }, { b -> openRestoreConfirmPopup(b) }), Position.BELOW, Position.ALIGN_JUSTIFY)
            // Change History
            .addElement("changelog", ActiveButtonWidget(changelogText,client.textRenderer.getWidth(changelogText) + 8, 20, { true }, { openChangelogPopup() }), Position.BELOW, Position.ALIGN_JUSTIFY)
            .positionX(PopupWidget.Builder.at { this.x - 8 })
            .positionY(PopupWidget.Builder.popupContext { h -> this.y - h + 28 })
            .build()
        PopupWidget.setPopup(popup)
    }

    private fun openRestoreConfirmPopup(b: ActiveButtonWidget) {
        val client = MinecraftClient.getInstance()
        val confirmText = "fc.button.restore.confirm".translate()
        val confirmTextWidth = max(50,client.textRenderer.getWidth(confirmText) + 8)
        val cancelText = "fc.button.restore.cancel".translate()
        val cancelTextWidth = max(50,client.textRenderer.getWidth(cancelText) + 8)
        val buttonWidth = max(confirmTextWidth,cancelTextWidth)

        val popup = PopupWidget.Builder("fc.button.restore".translate())
            .addDivider()
            .addElement("confirm_text", MultilineTextWidget("fc.button.restore.confirm.desc".translate(), MinecraftClient.getInstance().textRenderer).setCentered(true).setMaxWidth(buttonWidth + 4 + buttonWidth), Position.BELOW, Position.ALIGN_CENTER)
            .addElement("confirm_button", ButtonWidget.builder(confirmText) { updateApplier.restore(scope); PopupWidget.pop() }.dimensions(0,0,buttonWidth,20).build(), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("cancel_button", ButtonWidget.builder(cancelText) { PopupWidget.pop() }.size(buttonWidth,20).build(),"confirm_text", Position.BELOW, Position.ALIGN_RIGHT)
            .positionX(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 })
            .positionY(PopupWidget.Builder.popupContext { h -> b.y - h + 28 })
            .build()
        PopupWidget.setPopup(popup)
    }

    private fun openChangelogPopup() {
        val changes = updateApplier.changeHistory()
        val popup = PopupWidget.Builder("fc.button.changelog".translate())
            .addElement("changelog", ChangelogListWidget(changes,widthSupplier), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("done_button", ButtonWidget.builder(ScreenTexts.DONE) { PopupWidget.pop() }.size(50,20).build(), Position.BELOW, Position.ALIGN_JUSTIFY)
            .positionX(PopupWidget.Builder.at { 0 })
            .build()
        PopupWidget.setPopup(popup)
        //
    }
}