package me.fzzyhmstrs.fzzy_config.screen

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ElementListWidget

class ConfigListWidget(minecraftClient: MinecraftClient, i: Int, j: Int, k: Int, l: Int) :
    ElementListWidget<ConfigEntry>(minecraftClient, i, j, k, l) {
}