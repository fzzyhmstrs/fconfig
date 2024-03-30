package me.fzzyhmstrs.fzzy_config.api

import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

/**
 * API for client-side management of config files.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Environment(EnvType.CLIENT)
object ConfigApiClient {

    /**
     * Sets a [PopupWidget] to the current screen, if the current screen is a [PopupWidgetScreen]
     *
     * If a popup is already displayed, [PopupWidget.onClose] will be called on it before the new value is input.
     * @param popup [PopupWidget] or null. If null, the widget will be cleared, otherwise the current widget will be set to the passed one.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun setPopup(popup: PopupWidget?) {
        ConfigApiImplClient.setPopup(popup)
    }

}