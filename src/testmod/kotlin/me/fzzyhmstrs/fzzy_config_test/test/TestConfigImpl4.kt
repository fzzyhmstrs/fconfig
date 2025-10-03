/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.annotations.*
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigAction
import me.fzzyhmstrs.fzzy_config.screen.context.ContextInput
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.sendChat
import me.fzzyhmstrs.fzzy_config.util.TriState
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField.Companion.withListener
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedStringMap
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedRegistryType
import me.fzzyhmstrs.fzzy_config.validation.misc.*
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config_test.FC
import me.fzzyhmstrs.fzzy_config_test.FC.TEST_PERMISSION_BAD
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.loot.LootTables
import net.minecraft.loot.context.LootContextTypes
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.HoverEvent
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW

@RequiresRestart
class TestConfigImpl4: Config(Identifier.of("fzzy_config_test","test_config4")) {

    override fun onUpdateClient() {
        MinecraftClient.getInstance().debugHudEntryList.toggleF3Enabled()
    }

    override fun onSyncClient() {
        println("I ran a sync method!!")
    }

    override fun onSyncServer() {
        println("I synced on the server :>")
    }

    override fun onUpdateServer(playerEntity: ServerPlayerEntity) {
        println("Boo?")
        playerEntity.sendMessage("BOO".lit())
    }

    @WithCustomPerms([TEST_PERMISSION_BAD])
    var bl1 = true
    @Translation("test.prefix")
    var bl2 = ValidatedBoolean().withListener { blah -> if (blah.get()) println("Blah was set to true") else println("Blah was set to false") }

    @ValidatedInt.Restrict(0, 20)
    var int1 = 6
    var int2 = ValidatedInt(6, 10, 1).also { it.addListener { i2 -> println(i2.get()) } }

    var int1Button = ConfigAction.Builder().title("Int1 Value".lit()).build(Runnable { println(TestConfig.resultProvider.getResult("fzzy_config_test.test_config4.int1")) })

    var int2Button = ConfigAction.Builder().title("Int2 Value".lit()).build(Runnable { println(TestConfig.resultProvider.getResult("fzzy_config_test.test_config4.int2")) })

    @RequiresAction(Action.RELOG)
    var mapDouble = ValidatedStringMap(mapOf("a" to 1.0), ValidatedString(), ValidatedDouble(1.0, 1.0, 0.0))

    var namespaceBlackList: ValidatedList<String> = ValidatedString.fromList(FabricLoader.getInstance().allMods.map{ it.metadata.id }).toList()

    var testString = ValidatedString.fromList(FabricLoader.getInstance().allMods.map{ it.metadata.id })

    var testSimpleIdentifier = ValidatedIdentifier("minecraft:stick")

    var testDynamicIdentifier = ValidatedIdentifier.ofRegistryKey(RegistryKeys.BIOME)

    var testLootIdentifier = ValidatedIdentifier.ofRegistryKey(RegistryKeys.LOOT_TABLE)

    var testLootIdentifier2 = ValidatedIdentifier.ofDynamicKey(RegistryKeys.LOOT_TABLE, "test_loot_2") { id, _ -> id.path.contains("gameplay") }

    var testLootIdentifierPredicated = ValidatedIdentifier.ofRegistryKey(LootTables.IGLOO_CHEST_CHEST.value, RegistryKeys.LOOT_TABLE) { entry -> entry.value().type == LootContextTypes.CHEST }

    var validatedItem = ValidatedIdentifier.ofRegistry(Registries.ITEM).map(
        Items.EGG,
        { id -> Registries.ITEM.get(id) },
        { item -> Registries.ITEM.getId(item) }
    )

    var itemButton = ConfigAction.Builder().title("Give the Item".lit()).build(Runnable {
        val item = ItemStack(validatedItem.get())
        MinecraftClient.getInstance().player?.sendChat("This is the current item".lit().styled { s -> s.withHoverEvent(
        HoverEvent.ShowItem(item)) })
    })

    var validatedBlock = ValidatedRegistryType.of(Registries.BLOCK)

    class MyTestAny {
        var test: Int = 5
        var test2: Double = 4.5
        var test3: String = "ggg"
    }

    var myMap: ValidatedStringMap<MyTestAny> = ValidatedStringMap(mapOf("a" to MyTestAny(), "b" to MyTestAny()), ValidatedString(), ValidatedAny(MyTestAny()))

    var myTriState = TriState.TRUE
    var myTriState2 = ValidatedTriState(TriState.FALSE, ValidatedTriState.WidgetType.CYCLING)

    var myKeybind = ValidatedKeybind(GLFW.GLFW_KEY_BACKSPACE, ContextInput.KEYBOARD, ctrl = false, shift = true, alt = false)

    var keybindButton = ConfigAction.Builder().title("Test Keybind".lit()).decoration(TextureDeco.DECO_QUESTION).build(Runnable {
        FC.LOGGER.warn(
            "Testing Shift Backspace combination: {}",
            myKeybind.relevant(GLFW.GLFW_KEY_BACKSPACE, ctrl = false, shift = true, alt = false)
        )
        FC.LOGGER.warn(
            "Testing Tab key: {}",
            myKeybind.relevant(GLFW.GLFW_KEY_TAB, ctrl = false, shift = false, alt = false)
        )
    })

    /*
    {
     "bl1": false,
     "bl2": false,
     "int1": 12345,
     "int2": 1
    }
    */
}