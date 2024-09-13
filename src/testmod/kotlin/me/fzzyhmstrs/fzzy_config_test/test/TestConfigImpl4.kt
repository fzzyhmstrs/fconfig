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
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedStringMap
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedEntityAttribute
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config_test.FC
import me.fzzyhmstrs.fzzy_config_test.FC.TEST_PERMISSION_BAD
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.util.Identifier

@RequiresRestart
class TestConfigImpl4: Config(Identifier("fzzy_config_test","test_config4")) {

    @WithCustomPerms([TEST_PERMISSION_BAD])
    var bl1 = true
    @Translation("test.prefix")
    var bl2 = ValidatedBoolean()

    @ValidatedInt.Restrict(0, 20)
    var int1 = 6
    var int2 = ValidatedInt(6, 10, 1)

    @RequiresAction(Action.RELOG)
    var mapDouble = ValidatedStringMap(mapOf("a" to 1.0), ValidatedString(), ValidatedDouble(1.0, 1.0, 0.0))

    var namespaceBlackList: ValidatedList<String> = ValidatedString.fromList(FabricLoader.getInstance().allMods.map{ it.metadata.id }).toList()

    var testString = ValidatedString.fromList(FabricLoader.getInstance().allMods.map{ it.metadata.id })

    var exampleValidatedAttribute1 = ValidatedEntityAttribute.Builder("generic.max_health", true)
        // supply a UUID and name, otherwise generic ones will be used for you
        .uuid("f68e98a2-0599-11ef-9262-0242ac120002")
        .name("My Example ValidatedEntityAttribute")
        //set amount, and optionally provide a range restriction
        .amount(1.0, 0.0, 8.0)
        //set the operation for the modifier, and optionally lock the modifier to the operation chosen
        .operation(EntityAttributeModifier.Operation.ADD_VALUE, true)
        //build! gets you a ValidatedEntity Attribute
        .build()

    var exampleValidatedAttribute2 = ValidatedEntityAttribute.Builder("generic.max_health", false)
        // supply a UUID and name, otherwise generic ones will be used for you
        .uuid("8563c5ba-059b-11ef-9262-0242ac120002")
        .name("My Example ValidatedEntityAttribute")
        //set amount, and optionally provide a range restriction
        .amount(0.1, 0.0, 1.0)
        //set the operation for the modifier, and optionally lock the modifier to the operation chosen
        .operation(EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false)
        //build! gets you a ValidatedEntity Attribute
        .build()
    /*
    {
      "bl1": false,
      "bl2": false,
      "int1": 12345,
      "int2": 1
    }
    */
}