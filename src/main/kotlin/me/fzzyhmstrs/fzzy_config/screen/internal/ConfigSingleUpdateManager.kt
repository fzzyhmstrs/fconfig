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
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.FocusedTooltipPositioner
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.sendChat
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.gui.tooltip.Tooltip
import net.peanuuutz.tomlkt.Toml
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Supplier

//client
internal class ConfigSingleUpdateManager(private val configSet: ConfigSet, private val forwardedUpdates: MutableList<ConfigScreenManager.ForwardedUpdate>, private val perms: Int): ConfigBaseUpdateManager() {

    override fun managerId(): String {
        return configSet.active.getId().toTranslationKey()
    }

    private val updatableEntries: MutableMap<String, Updatable> = mutableMapOf()

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
        val clientActions: MutableSet<Action> = mutableSetOf()
        val serverActions: MutableSet<Action> = mutableSetOf()
        var updatedConfig: Boolean = false
        val config = configSet.active
        val isClient = configSet.clientOnly
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
                    } catch (e: Throwable) {
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
            updatedConfig = true
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

        configSet.active.save()

        val syncNeeded = !configSet.clientOnly && updatedConfig
        if (syncNeeded && !MinecraftClient.getInstance().isInSingleplayer) {
            //send updates to the server for distribution and saving there
            val updates = mapOf(this.configSet.active.getId().toTranslationKey() to ConfigApiImpl.serializeUpdate(configSet.active, this, mutableListOf()))
            NetworkEventsClient.updateServer(updates, flush(), perms)
            ConfigApiImpl.printChangeHistory(flush(), configSet.active.getId().toTranslationKey(), MinecraftClient.getInstance().player)
        } else {
            ConfigApiImpl.printChangeHistory(flush(), configSet.active.getId().toTranslationKey(), MinecraftClient.getInstance().player)
        }
        if (!final)
            pushUpdatableStatesInternal()
    }

    private class ForwardEntry(parentElement: DynamicListWidget, private val forwardedUpdate: ConfigScreenManager.ForwardedUpdate, private val manager: ConfigSingleUpdateManager)
        : DynamicListWidget.Entry(parentElement, Translatable.Result(forwardedUpdate.entry.transLit(forwardedUpdate.scope), forwardedUpdate.summary.lit()), DynamicListWidget.Scope(forwardedUpdate.scope))
    {

        override var h: Int = 20
        private val tooltip = Tooltip.of(texts.desc)

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

        override fun children(): MutableList<out Element> {
            return mutableListOf(acceptForwardWidget, denyForwardWidget)
        }

        override fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
            if (hovered) {
                MinecraftClient.getInstance().currentScreen?.setTooltip(tooltip, HoveredTooltipPositioner.INSTANCE, true)
            } else if (focused) {
                MinecraftClient.getInstance().currentScreen?.setTooltip(tooltip, FocusedTooltipPositioner(ScreenRect(x + 2, y + 4, width, height)), true)
            }
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, texts.name, x, y + 5, -1)
            acceptForwardWidget.setPosition(x + 126, y)
            acceptForwardWidget.render(context, mouseX, mouseY, delta)
            denyForwardWidget.setPosition(x + 150, y)
            denyForwardWidget.render(context, mouseX, mouseY, delta)
        }

        override fun appendNarrations(builder: NarrationMessageBuilder) {
            builder.put(NarrationPart.HINT, texts.desc)
        }

    }
}