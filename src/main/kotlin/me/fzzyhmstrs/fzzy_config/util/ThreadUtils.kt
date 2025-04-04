/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util

import me.fzzyhmstrs.fzzy_config.FCC
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.screen.PopupController
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.function.Consumer
import java.util.function.Function
import kotlin.concurrent.thread


internal object ThreadUtils {

    internal val EXECUTOR = Executors.newFixedThreadPool(6, Thread.ofVirtual().name("Fzzy Config Worker", 1).factory())

    /*@Volatile
    private var doTick: Boolean = false

    fun doTick() {
        if (!clientWorker.isAlive)
            clientWorker.start()
        doTick = true
    }

    val clientWorker: Thread = Thread.ofPlatform().name("Fzzy Config Client Worker").unstarted {

        val scopeConsumer: Consumer<String> = Consumer { scopeToOpen ->
            if (scopeToOpen != "") {
                ConfigApiImplClient.openScreen(scopeToOpen)
            }
        }

        val restartFunction: Function<Boolean, Boolean> = Function { openRestartScreen ->
            if (openRestartScreen) {
                ConfigApiImplClient.openRestartScreen()
            } else
                false
        }

        while (true) {
            if (doTick) {
                FCC.withScope(scopeConsumer)
                FCC.withRestart(restartFunction)
                PopupController.popAll()
                doTick = false
            }
        }
    }

    //////////////

    private val configs: MutableMap<Path, MutableList<Config>> = Collections.synchronizedMap(HashMap())

    fun addConfig(config: Config) {
        synchronized(configs) {
            configs.computeIfAbsent(config.getDir().toPath()) { _ -> mutableListOf() }.add(config)
        }
    }

    val fileWorker: Thread = Thread.ofPlatform().name("Fzzy Config File Worker").unstarted {

    }*/
}