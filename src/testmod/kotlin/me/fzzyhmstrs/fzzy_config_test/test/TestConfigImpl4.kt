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
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.sendChat
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField.Companion.withListener
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedStringMap
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedEntityAttribute
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedRegistryType
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config_test.FC
import me.fzzyhmstrs.fzzy_config_test.FC.TEST_PERMISSION_BAD
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.HoverEvent
import net.minecraft.util.Identifier

@RequiresRestart
class TestConfigImpl4: Config(Identifier("fzzy_config_test","test_config4")) {

    override fun onUpdateClient() {
        MinecraftClient.getInstance().debugHud.toggleDebugHud()
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

    @RequiresAction(Action.RELOG)
    var mapDouble = ValidatedStringMap(mapOf("a" to 1.0), ValidatedString(), ValidatedDouble(1.0, 1.0, 0.0))

    var namespaceBlackList: ValidatedList<String> = ValidatedString.fromList(FabricLoader.getInstance().allMods.map{ it.metadata.id }).toList()

    var testString = ValidatedString.fromList(FabricLoader.getInstance().allMods.map{ it.metadata.id })

    var testDynamicIdentifier = ValidatedIdentifier.ofRegistryKey(RegistryKeys.BIOME)

    var exampleValidatedAttribute1 = ValidatedEntityAttribute.Builder("generic.max_health", true)
        // supply a UUID and name, otherwise generic ones will be used for you
        .uuid("f68e98a2-0599-11ef-9262-0242ac120002")
        .name("My Example ValidatedEntityAttribute")
        //set amount, and optionally provide a range restriction
        .amount(1.0, 0.0, 8.0)
        //set the operation for the modifier, and optionally lock the modifier to the operation chosen
        .operation(EntityAttributeModifier.Operation.ADDITION, true)
        //build! gets you a ValidatedEntity Attribute
        .build()

    var exampleValidatedAttribute2 = ValidatedEntityAttribute.Builder("generic.max_health", false)
        // supply a UUID and name, otherwise generic ones will be used for you
        .uuid("8563c5ba-059b-11ef-9262-0242ac120002")
        .name("My Example ValidatedEntityAttribute")
        //set amount, and optionally provide a range restriction
        .amount(0.1, 0.0, 1.0)
        //set the operation for the modifier, and optionally lock the modifier to the operation chosen
        .operation(EntityAttributeModifier.Operation.MULTIPLY_TOTAL, false)
        //build! gets you a ValidatedEntity Attribute
        .build()

    var validatedItem = ValidatedIdentifier.ofRegistry(Registries.ITEM).map(
        Items.EGG,
        { id -> Registries.ITEM.get(id) },
        { item -> Registries.ITEM.getId(item) }
    )

    var itemButton = ConfigAction.Builder().title("Give the Item".lit()).build {
        val item = ItemStack(validatedItem.get())
        MinecraftClient.getInstance().player?.sendChat("This is the current item".lit().styled { s -> s.withHoverEvent(
        HoverEvent(HoverEvent.Action.SHOW_ITEM, HoverEvent.ItemStackContent(item))
    ) }) }

    var validatedBlock = ValidatedRegistryType.of(Registries.BLOCK)
    /*
    {
     "bl1": false,
     "bl2": false,
     "int1": 12345,
     "int2": 1
    }
    */
}