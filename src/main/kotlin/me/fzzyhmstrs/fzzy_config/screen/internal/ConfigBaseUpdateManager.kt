package me.fzzyhmstrs.fzzy_config.screen.internal

import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.MinecraftClient
import java.util.function.BiFunction

open class ConfigBaseUpdateManager: BaseUpdateManager() {

    open fun managerId() = "Unknown Update Manager"

    protected var lastPush: String? = null

    open fun setUpdatableEntry(entry: Updatable) {
    }

    open fun getUpdatableEntry(key: String): Updatable? {
        return null
    }

    protected open fun pushUpdatableStatesInternal() {
        /*for (updatable in updatableEntries.values) {
            updatable.pushState()
        }*/
    }

    fun pushUpdatableStates(scope: String = "") {
        if (scope == lastPush) return
        lastPush = scope
        flush()
        pushUpdatableStatesInternal()
    }

    fun invalidatePush() {
        lastPush = null
    }

    open fun forwardHandlerEntries(): List<BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>> {
        return listOf()
    }

    override fun forwardsHandler() {
        val entries = forwardHandlerEntries()
        val list = DynamicListWidget(MinecraftClient.getInstance(), entries, 0, 0, 190, 120)
        val popup = PopupWidget.Builder("fc.config.forwarded".translate())
            .add("list", list, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
            .addDoneWidget()
            .build()
        PopupWidget.push(popup)
    }
}