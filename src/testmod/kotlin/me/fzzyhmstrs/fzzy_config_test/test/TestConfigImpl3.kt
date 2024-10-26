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

import me.fzzyhmstrs.fzzy_config.annotations.ConvertFrom
import me.fzzyhmstrs.fzzy_config.annotations.IgnoreVisibility
import me.fzzyhmstrs.fzzy_config.annotations.RequiresRestart
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigAction
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.sendChat
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.EntityType
import net.minecraft.fluid.Fluids
import net.minecraft.item.Items
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.ClickEvent
import net.minecraft.util.Identifier

@IgnoreVisibility
@ConvertFrom("test_config3.json","fzzy_config_test")
class TestConfigImpl3: Config(Identifier.of("fzzy_config_test","test_config3")) {

    private var configAction = ConfigAction.Builder().title("Open Docs...".lit()).build(ClickEvent(ClickEvent.Action.OPEN_URL, "https://fzzyhmstrs.github.io/fconfig/"))

    private var configAction2 = ConfigAction.Builder().title("Say Hi...".lit()).build { MinecraftClient.getInstance().player?.sendChat("Hiya".lit()) }

    private var configAction3 = ConfigAction.Builder().title("Give Loots...".lit()).build(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/give @s minecraft:diamond"))

    var testLootIdentifier = ValidatedIdentifier.ofRegistryKey(RegistryKeys.LOOT_TABLE)

    fun getBl1(): Boolean {
        return bl1
    }
    @RequiresRestart
    private var bl1 = true

    fun getBl2(): Boolean {
        return bl2.get()
    }
    private var bl2 = ValidatedBoolean()

    fun getInt1(): Int {
        return int1
    }
    @ValidatedInt.Restrict(0, 20)
    private var int1 = 6

    fun getInt2(): Int {
        return int2.get()
    }
    @RequiresRestart
    private var int2 = ValidatedInt(6, 10, 1)

    private var floatTest = ValidatedDouble(5.0, 6.6, 5.0)

    private var itemTest = Items.EGG

    private var blockTest = Blocks.AMETHYST_BLOCK

    private var entityTest = EntityType.EGG

    private var fluidTest = Fluids.LAVA

    var testEnum = Test.B

    enum class Test(val id: Int) {
        A(1) {

            override fun getMultId(): Int {
                return id * 1
            }
        },
        B(2) {

            override fun getMultId(): Int {
                return id * 2
            }
        },
        C(3) {

            override fun getMultId(): Int {
                return id * 3
            }
        };

        fun getIdentity(): Int {
            return id
        }

        abstract fun getMultId(): Int
    }

    /*
    {
      "bl1": false,
      "bl2": false,
      "int1": 12345,
      "int2": 1
    }
    */
}