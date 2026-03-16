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
import me.fzzyhmstrs.fzzy_config.api.SaveType
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryParent
import me.fzzyhmstrs.fzzy_config.event.impl.EventApiImpl
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigSet
import me.fzzyhmstrs.fzzy_config.networking.NetworkEventsClient
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.sendChat
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.client.gui.components.Tooltip
import net.peanuuutz.tomlkt.Toml
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Supplier

//client
internal class ConfigSingleUpdateManager(private val configSet: ConfigSet, private val forwardedUpdates: MutableList<ConfigScreenManager.ForwardedUpdate>, private val perms: Int): ConfigBaseUpdateManager() {

    override fun managerId(): String {
        return configSet.active.getId().toLanguageKey()
    }

    private val updatableEntries: MutableMap<String, Updatable> = hashMapOf()

    override fun setUpdatableEntry(entry: Updatable) {
        updatableEntries[entry.getEntryKey()] = entry
    }

    override fun getUpdatableEntry(key: String): Updatable? {
        return updatableEntries[key]
    }

    override fun pushUpdatableStatesInternal() {
        for (updatable in updatableEntries.values) {
            updatable.pushState()
        }
    }

    override fun hasRestores(scope: String): Boolean {
        for ((key, updatable) in updatableEntries) {
            if(!key.startsWith(scope)) continue
            if(!updatable.isDefault()) return true
        }
        return false
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
            if(updatable.isDefault()) continue
            updatable.restore()
        }
    }

    override fun forwardsCount(): Int {
        return forwardedUpdates.size
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
        val clientActions: MutableSet<Action> = hashSetOf()
        val serverActions: MutableSet<Action> = hashSetOf()
        var updatedConfig: Boolean = false
        val config = configSet.active
        val isClient = configSet.clientOnly
        var fireOnUpdate = false
        ConfigApiImpl.walk(config, config.getId().toLanguageKey(), 1) { walkable, _, new, thing, prop, annotations, globalAnnotations, _ ->
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
                    } else if (update is EntryParent) {
                        val anyActions = update.changeActions()
                        if (anyActions.isNotEmpty()) {
                            if (ConfigApiImpl.isNonSync(annotations) || isClient) {
                                clientActions.addAll(anyActions)
                            } else {
                                serverActions.addAll(anyActions)
                            }
                        }
                    }
                    try {
                        prop.setter.call(walkable, update.get())
                    } catch (e: Throwable) {
                        FC.LOGGER.error("Error pushing update to simple property [$new]", e)
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
                    val anyActions = thing.changeActions()
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
            updatedConfig = true
            try {
                config.onUpdateClient()
            } catch (e: Throwable) {
                FC.LOGGER.error("Error encountered while calling `onUpdateClient` for config ${config.getId()}!", e)
            }
            try {
                EventApiImpl.fireOnUpdateClient(config.getId(), config)
            } catch (e: Throwable) {
                FC.LOGGER.error("Error encountered while running `onUpdateClient` event for config ${config.getId()}!", e)
            }
        }

        if (clientActions.isNotEmpty()) {
            for (action in clientActions) {
                Minecraft.getInstance().player?.sendChat(action.clientUpdateMessage)
            }
        }
        if (serverActions.isNotEmpty()) {
            if (Minecraft.getInstance().isLocalServer)
                for (action in serverActions) {
                    Minecraft.getInstance().player?.sendChat(action.clientUpdateMessage)
                }
            else
                for (action in serverActions) {
                    Minecraft.getInstance().player?.sendChat(action.serverUpdateMessage)
                }
        }
        //save config updates locally
        if (isClient
            || configSet.active.saveType() == SaveType.OVERWRITE
            || Minecraft.getInstance().isLocalServer
            || outOfGame())
        {
            configSet.active.save()
        }

        val syncNeeded = !configSet.clientOnly && updatedConfig
        if (syncNeeded && !Minecraft.getInstance().isLocalServer && !outOfGame()) {
            //send updates to the server for distribution and saving there
            val updates = mapOf(this.configSet.active.getId().toLanguageKey() to ConfigApiImpl.serializeUpdate(configSet.active, this, "Error(s) while serializing update to send to the server").log().get())
            NetworkEventsClient.updateServer(updates, flush(), perms)
            ConfigApiImpl.printChangeHistory(flush(), configSet.active.getId().toLanguageKey(), Minecraft.getInstance().player)
        } else {
            ConfigApiImpl.printChangeHistory(flush(), configSet.active.getId().toLanguageKey(), Minecraft.getInstance().player)
        }
        if (!final)
            pushUpdatableStatesInternal()
    }

    private fun outOfGame(): Boolean {
        val client = Minecraft.getInstance()
        return (client.level == null || client.connection == null || client.connection?.isAcceptingMessages == false)
    }

    private class ForwardEntry(parentElement: DynamicListWidget, private val forwardedUpdate: ConfigScreenManager.ForwardedUpdate, private val manager: ConfigSingleUpdateManager)
        : DynamicListWidget.Entry(parentElement, Translatable.createScopedResult(forwardedUpdate.scope, forwardedUpdate.entry.transLit(forwardedUpdate.scope), forwardedUpdate.summary.lit()), DynamicListWidget.Scope(forwardedUpdate.scope))
    {

        override var h: Int = 20
        private val tooltip = Tooltip.create(texts.desc ?: FcText.empty())

        val acceptForwardWidget = CustomButtonWidget.builder { manager.acceptForward(forwardedUpdate); applyVisibility { stack -> stack.push(DynamicListWidget.Visibility.HIDDEN) } }
            .textures("widget/action/accept".fcId(),
                "widget/action/accept_inactive".fcId(),
                "widget/action/accept_highlighted".fcId())
            .tooltip("fc.button.accept".translate())
            .narrationSupplier { _, _ -> "fc.button.accept".translate() }
            .size(20, 20)
            .build()

        val denyForwardWidget = CustomButtonWidget.builder { manager.rejectForward(forwardedUpdate); applyVisibility { stack -> stack.push(DynamicListWidget.Visibility.HIDDEN) } }
            .textures("widget/action/delete".fcId(),
                "widget/action/delete_inactive".fcId(),
                "widget/action/delete_highlighted".fcId())
            .tooltip("fc.button.deny".translate())
            .narrationSupplier { _, _ -> "fc.button.deny".translate() }
            .size(20, 20)
            .build()

        override fun selectableChildren(): List<SelectableElement> {
            return listOf(acceptForwardWidget, denyForwardWidget).cast()
        }

        override fun children(): MutableList<out GuiEventListener> {
            return mutableListOf(acceptForwardWidget, denyForwardWidget)
        }

        override fun renderEntry(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
            val client = Minecraft.getInstance()
            if (hovered) {
                context.setTooltipForNextFrame(client.font, tooltip.toCharSequence(client), DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true)
            } else if (focused) {
                context.setTooltipForNextFrame(client.font, tooltip.toCharSequence(client), BelowOrAboveWidgetTooltipPositioner(ScreenRectangle(x + 2, y + 4, width, height)), x, y, true)
            }
            context.text(Minecraft.getInstance().font, texts.name, x, y + 5, -1)
            acceptForwardWidget.setPosition(x + 126, y)
            acceptForwardWidget.extractRenderState(context, mouseX, mouseY, delta)
            denyForwardWidget.setPosition(x + 150, y)
            denyForwardWidget.extractRenderState(context, mouseX, mouseY, delta)
        }

        override fun appendNarrations(builder: NarrationElementOutput) {
            texts.desc?.let { builder.add(NarratedElementType.HINT, it) }
        }

    }
}