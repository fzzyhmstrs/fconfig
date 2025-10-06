/*
* Copyright (c) 2025 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config_test.test

import me.fzzyhmstrs.fzzy_config.annotations.*
import me.fzzyhmstrs.fzzy_config.api.SaveType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.TriState
import me.fzzyhmstrs.fzzy_config.util.Walkable
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedRegistryType
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlComment

@Translatable.Name("Test Any Config")
class TestConfigImplAny: Config(Identifier("fzzy_config_test","test_config_any")) {

    override fun saveType(): SaveType {
        return SaveType.SEPARATE
    }

    @Translatable.Name("My Test Object")
    var any1 = MyTestAny()

    @Translatable.Name("My Test Object 2")
    var any2 = ValidatedAny(MyTestAny2()).toCondition({ true }, FcText.literal("Test"), ::MyTestAny2)

    //var bl1 = true

    class MyTestAny: Walkable {
        @Translatable.Name("Test 1")
        @Translatable.Desc("Test 1 Description; very cool and powerful")
        @RequiresAction(Action.RESTART)
        var test: Int = 5

        @Translatable.Name("Test 2")
        @Translatable.Name("Prueba Dos", "es_es")
        var test2: Double = 4.5

        @Translatable.Name("Test 3")
        @Comment("Do comments work?")
        var test3: String = "ggg"

        @Translatable.Name("Test 4")
        @TomlComment("Do TomlComments work?")
        var test4: Boolean = false

        @Translatable.Name("Test 5")
        @Translatable.Prefix("Test prefix that is very cool")
        @Translatable.Prefix("Prefijo de prueba que es muy genial", "es_es")
        var test5: TriState = TriState.DEFAULT
    }

    class MyTestAny2 {
        @Translatable.Name("Delegate Test")
        @Translatable.Desc("Delegate Test Description; very cool and powerful")
        @RequiresAction(Action.RESTART)
        var test: Int = 5

        @Translatable.Name("Mapped List Test")
        var test2 = ValidatedList(listOf(Items.ACACIA_BOAT, Items.DIAMOND), ValidatedRegistryType.of(Registries.ITEM))
    }
}