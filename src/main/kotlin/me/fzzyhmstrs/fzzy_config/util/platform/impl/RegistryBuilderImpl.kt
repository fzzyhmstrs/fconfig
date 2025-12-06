package me.fzzyhmstrs.fzzy_config.util.platform.impl

import com.mojang.serialization.Lifecycle
import me.fzzyhmstrs.fzzy_config.util.platform.RegistryBuilder
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemGroup
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.SimpleDefaultedRegistry
import net.minecraft.registry.SimpleRegistry
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class RegistryBuilderImpl(namespace: String): RegistryBuilder(namespace) {

    override fun <T : Any> build(key: RegistryKey<Registry<T>>): Registry<T> {
        return FabricRegistryBuilder.createSimple(key).buildAndRegister()
    }

    override fun <T : Any> buildDefaulted(key: RegistryKey<Registry<T>>, defaultId: Identifier): Registry<T> {
        return FabricRegistryBuilder.createDefaulted(key, defaultId).buildAndRegister()
    }

    override fun <T : Any> buildIntrusive(key: RegistryKey<Registry<T>>): Registry<T> {
        return FabricRegistryBuilder.from(SimpleRegistry(key, Lifecycle.stable(), true)).buildAndRegister()
    }

    override fun <T : Any> buildDefaultedIntrusive(key: RegistryKey<Registry<T>>, defaultId: Identifier): Registry<T> {
        return FabricRegistryBuilder.from(SimpleDefaultedRegistry<T>(defaultId.toString(), key, Lifecycle.stable(), true)).buildAndRegister()
    }

    override fun itemGroup(): ItemGroup.Builder {
        return FabricItemGroup.builder()
    }

    override fun getTagName(tagKey: TagKey<*>): Text {
        return tagKey.name
    }
}