/*
* Copyright (c) 2024-5 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config_test.test

import com.google.gson.GsonBuilder
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.result.ResultProvider
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedPair
import me.fzzyhmstrs.fzzy_config_test.FC.buildTranslation


object TestConfig {

    fun init() {
        /*println("I registered my config")
        ConfigApi.event().onSyncClient { id, _ ->
            println("Syncing config $id")
            if (id == testConfig4.getId()) {
                println("I ran a sync event! :D")
            }
        }
        ConfigApi.event().onSyncServer { id, config ->
            if (id == testConfig2.getId()) {
                println("I inspected the config2 on server sync >:3")
                println((config as? TestConfigImpl2)?.mathTest?.get())
            }
        }
        ConfigApi.event().onUpdateClient { id, config ->
            if (id == testConfig4.getId()) {
                println("I ran a update event! :D")
                println(config.name)
                println(config.folder)
            }
        }
        ConfigApi.event().onUpdateServer { id, _, player ->
            if (id == testConfig4.getId()) {
                println("I'm on the server thread right?")
                player.sendMessage("I ran a server update event!!".lit())
            }
        }*/

        buildTranslation("en_us")
        buildTranslation("es_es")
    }

    val gson = GsonBuilder().setPrettyPrinting().create()



    val resultProvider = ConfigApi.result().createSimpleResultProvider(-666, Int::class)

    val listProvider: ResultProvider<List<Int>> = ConfigApi.result().createSimpleResultProvider(emptyList(), listOf<Int>().javaClass.kotlin)

    val tupleProvider: ResultProvider<ValidatedPair.Tuple<Int, Int>> = ConfigApi.result().createSimpleResultProvider(ValidatedPair.Tuple(0, 0), ValidatedPair.Tuple(0, 0).javaClass.kotlin)

    var testConfigAny = ConfigApi.registerAndLoadConfig({ TestConfigImplAny() }, RegisterType.BOTH)

    var rootConfig = ConfigApi.registerAndLoadConfig({ TestRootConfigImpl() }, RegisterType.BOTH)
    var testConfig2 = ConfigApi.registerAndLoadConfig({ TestConfigImpl2() }, RegisterType.BOTH)
    var testConfig4 = ConfigApi.registerAndLoadConfig({ TestConfigImpl4() }, RegisterType.BOTH)

    var serverConfig = ConfigApi.registerAndLoadConfig({ TestServerConfigImpl() }, RegisterType.SERVER)

    //var javaConfig2 = ConfigApi.registerAndLoadConfig({ JavaTestConfig2() }, RegisterType.SERVER)
}