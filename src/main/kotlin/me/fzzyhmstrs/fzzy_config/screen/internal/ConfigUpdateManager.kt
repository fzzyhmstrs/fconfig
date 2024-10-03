/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.internal

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryParent
import me.fzzyhmstrs.fzzy_config.event.impl.EventApiImpl
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigSet
import me.fzzyhmstrs.fzzy_config.networking.NetworkEventsClient
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.TextlessActionWidget
import me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.sendChat
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.util.Colors
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.Toml
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Supplier

//client
internal class ConfigUpdateManager(private val configs: List<ConfigSet>, private val forwardedUpdates: MutableList<ConfigScreenManager.ForwardedUpdate>, private val perms: Int): BaseUpdateManager() {

    private val updatableEntries: MutableMap<String, Updatable> = mutableMapOf()

    fun setUpdatableEntry(entry: Updatable) {
        updatableEntries[entry.getEntryKey()] = entry
    }

    fun getUpdatableEntry(key: String): Updatable? {
        return updatableEntries[key]
    }

    fun pushUpdatableStates() {
        for (updatable in updatableEntries.values) {
            updatable.pushState()
        }
    }

    override fun restoreCount(scope: String): Int {
        var count = 0
        for ((key, updatable) in updatableEntries) {
            if(!key.startsWith(scope)) continue
            if(updatable.isDefault()) continue
            count++
        }
        return count
    }

    override fun restore(scope: String) {
        for ((key, updatable) in updatableEntries) {
            if(!key.startsWith(scope)) continue
            updatable.restore()
        }
    }

    override fun forwardsCount(): Int {
        return forwardedUpdates.size
    }

    override fun forwardsHandler() {
        val popup = PopupWidget.Builder("fc.config.forwarded".translate())
            .addElement("list", ForwardedEntryListWidget(forwardedUpdates, this), Position.BELOW, Position.ALIGN_CENTER)
            .addDoneButton()
            .build()
        PopupWidget.push(popup)
    }

    private fun acceptForward(forwardedUpdate: ConfigScreenManager.ForwardedUpdate) {
        val toml = try {
            Toml.parseToTomlTable(forwardedUpdate.update)
        } catch (e:Exception) {
            return
        }
        val element = toml["entry"] ?: return
        forwardedUpdate.entry.deserializeEntry(element, mutableListOf(), forwardedUpdate.scope, 1)
        forwardedUpdates.remove(forwardedUpdate)
        apply(false)
    }

    private fun rejectForward(forwardedUpdate: ConfigScreenManager.ForwardedUpdate) {
        forwardedUpdates.remove(forwardedUpdate)
    }

    @Internal
    override fun apply(final: Boolean) {
        if (updateMap.isEmpty()) return
        //push updates from basic validation to the configs
        val clientActions: MutableSet<Action> = mutableSetOf()
        val serverActions: MutableSet<Action> = mutableSetOf()
        val updatedConfigs: MutableSet<Identifier> = mutableSetOf()
        for ((config, _, isClient) in configs) {
            var fireOnUpdate = false
            ConfigApiImpl.walk(config, config.getId().toTranslationKey(), 1) { walkable, _, new, thing, prop, annotations, globalAnnotations, _ ->
                if (!(thing is Updatable && thing is Entry<*, *>)) {
                    val update = getUpdate(new)
                    if (update != null && update is Supplier<*>) {
                        val action = ConfigApiImpl.requiredAction(annotations, globalAnnotations)
                        if (action != null) {
                            if (ConfigApiImpl.isNonSync(annotations) || isClient) {
                                clientActions.add(action)
                            } else {
                                serverActions.add(action)
                            }
                        }
                        try {
                            prop.setter.call(walkable, update.get())
                        } catch (e: Exception) {
                            FC.LOGGER.error("Error pushing update to simple property [$new]")
                            e.printStackTrace()
                        }
                        fireOnUpdate = true
                    }
                } else if (getUpdate(new) != null) {
                    val action = ConfigApiImpl.requiredAction(annotations, globalAnnotations)
                    if (action != null) {
                        if (ConfigApiImpl.isNonSync(annotations) || isClient) {
                            clientActions.add(action)
                        } else {
                            serverActions.add(action)
                        }
                    } else if (thing is EntryParent) {
                        val anyActions = thing.actions()
                        if (anyActions.isNotEmpty()) {
                            if (ConfigApiImpl.isNonSync(annotations) || isClient) {
                                clientActions.addAll(anyActions)
                            } else {
                                serverActions.addAll(anyActions)
                            }
                        }
                    }
                    fireOnUpdate = true
                }
            }
            if (fireOnUpdate) {
                updatedConfigs.add(config.getId())
                try {
                    config.onUpdateClient()
                } catch (e: Throwable) {
                    FC.LOGGER.error("Error encountered while calling `onUpdateClient` for config ${config.getId()}!")
                    e.printStackTrace()
                }
                try {
                    EventApiImpl.fireOnUpdateClient(config.getId(), config)
                } catch (e: Throwable) {
                    FC.LOGGER.error("Error encountered while running `onUpdateClient` event for config ${config.getId()}!")
                    e.printStackTrace()
                }
            }
        }
        if (clientActions.isNotEmpty()) {
            for (action in clientActions) {
                MinecraftClient.getInstance().player?.sendChat(action.clientUpdateMessage)
            }
        }
        if (serverActions.isNotEmpty()) {
            if (MinecraftClient.getInstance().isInSingleplayer)
                for (action in serverActions) {
                    MinecraftClient.getInstance().player?.sendChat(action.clientUpdateMessage)
                }
            else
                for (action in serverActions) {
                    MinecraftClient.getInstance().player?.sendChat(action.serverUpdateMessage)
                }
        }
        //save config updates locally
        for (config in configs) {
            config.active.save()
        }
        var count = 0
        for (config in configs) {
            if (!config.clientOnly && updatedConfigs.contains(config.active.getId()))
                count++
        }
        if (count > 0) {
            //send updates to the server for distribution and saving there
            val updates = this.configs.filter { !it.clientOnly && updatedConfigs.contains(it.active.getId()) }.associate { it.active.getId().toTranslationKey() to ConfigApiImpl.serializeUpdate(it.active, this, mutableListOf()) }
            if (updates.isNotEmpty()) {
                NetworkEventsClient.updateServer(updates, flush(), perms)
            } else {
                ConfigApiImpl.printChangeHistory(flush(), updatedConfigs.toString(), MinecraftClient.getInstance().player)
            }
        } else {
            ConfigApiImpl.printChangeHistory(flush(), updatedConfigs.toString(), MinecraftClient.getInstance().player)
        }
        if (!final)
            pushUpdatableStates()
    }

    private class ForwardedEntryListWidget(forwardedUpdates: MutableList<ConfigScreenManager.ForwardedUpdate>, manager: ConfigUpdateManager): ElementListWidget<ForwardedEntryListWidget.ForwardEntry>(MinecraftClient.getInstance(), 190, 120, 0, 22) {

        override fun drawHeaderAndFooterSeparators(context: DrawContext?) {
        }
        override fun drawMenuListBackground(context: DrawContext?) {
        }
        override fun getRowWidth(): Int {
            return 170
        }

        override fun getScrollbarX(): Int {
            return this.x + this.width / 2 + this.rowWidth / 2 + 4
        }

        init {
            for (forwardedUpdate in forwardedUpdates) {
                this.addEntry(ForwardEntry(forwardedUpdate, this, manager))
            }
        }

        private class ForwardEntry(private val forwardedUpdate: ConfigScreenManager.ForwardedUpdate, private val parent: ForwardedEntryListWidget, manager: ConfigUpdateManager): Entry<ForwardEntry>() {

            val name = forwardedUpdate.entry.transLit(forwardedUpdate.scope)
            val tooltip = Tooltip.of(forwardedUpdate.summary.lit())

            val acceptForwardWidget = TextlessActionWidget(
                "widget/action/accept".fcId(),
                "widget/action/accept_inactive".fcId(),
                "widget/action/accept_highlighted".fcId(),
                "fc.button.accept".translate(),
                "fc.button.accept".translate(),
                { true },
                { manager.acceptForward(forwardedUpdate); parent.removeEntry(this) }
            )
            val denyForwardWidget = TextlessActionWidget(
                "widget/action/delete".fcId(),
                "widget/action/delete_inactive".fcId(),
                "widget/action/delete_highlighted".fcId(),
                "fc.button.deny".translate(),
                "fc.button.deny".translate(),
                { true },
                { manager.rejectForward(forwardedUpdate); parent.removeEntry(this) }
            )
            override fun render(
                context: DrawContext,
                index: Int,
                y: Int,
                x: Int,
                entryWidth: Int,
                entryHeight: Int,
                mouseX: Int,
                mouseY: Int,
                hovered: Boolean,
                tickDelta: Float
            ) {
                if (isMouseOver(mouseX.toDouble(), mouseY.toDouble()))
                    parent.client.currentScreen?.setTooltip(tooltip.getLines(parent.client))
                context.drawTextWithShadow(parent.client.textRenderer, name, x, y+5, Colors.WHITE)
                acceptForwardWidget.setPosition(x + 126, y)
                acceptForwardWidget.render(context, mouseX, mouseY, tickDelta)
                denyForwardWidget.setPosition(x + 150, y)
                denyForwardWidget.render(context, mouseX, mouseY, tickDelta)
            }

            override fun children(): MutableList<out Element> {
                return mutableListOf(acceptForwardWidget, denyForwardWidget)
            }

            override fun selectableChildren(): MutableList<out Selectable> {
                return mutableListOf(acceptForwardWidget, denyForwardWidget)
            }
        }
    }
}