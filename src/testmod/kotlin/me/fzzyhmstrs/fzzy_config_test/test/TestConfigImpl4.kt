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

import me.fzzyhmstrs.fzzy_config.annotations.RequiresRestart
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedMap
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedStringMap
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier

@RequiresRestart
class TestConfigImpl4: Config(Identifier("fzzy_config_test","test_config4")) {


    var bl1 = true
    var bl2 = ValidatedBoolean()

    @ValidatedInt.Restrict(0,20)
    var int1 = 6
    var int2 = ValidatedInt(6,10,1)

    var mapDouble = ValidatedStringMap(mapOf("a" to 1.0),ValidatedString(), ValidatedDouble(1.0,1.0,0.0))

    var namespaceBlackList: ValidatedList<String> = ValidatedString.fromList(FabricLoader.getInstance().allMods.map{ it.metadata.id }).toList()

    var testString = ValidatedString.fromList(FabricLoader.getInstance().allMods.map{ it.metadata.id })

    /*
    {
      "bl1": false,
      "bl2": false,
      "int1": 12345,
      "int2": 1
    }
    */
}