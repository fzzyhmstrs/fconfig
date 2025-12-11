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

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigEntry
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import net.peanuuutz.tomlkt.TomlTable
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock


internal object ThreadingUtils {

    internal val EXECUTOR = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("Fzzy Config Worker").factory())
    private val FILE_WATCHER = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("Fzzy Config File Watcher").factory())

    private val watcherLock: ReentrantLock = ReentrantLock()
    private val updatesLock: ReentrantLock = ReentrantLock()
    private val watchService = FileSystems.getDefault().newWatchService()
    private val configWatchers: HashMap<Path, ConfigEntry<out Config>> = hashMapOf()
    private val internalUpdates: MutableSet<Path> = mutableSetOf()

    /*
    * There will be a max of one actual config instance to care about.
    * If you register separate config instances CLIENT/SERVER you will desync in-game anyway with screen usage.
    *
    * Synced will take priority over client. If there is both, the synced will matter more
    * It will at the very least also update the client instance when needed (because they are the same),
    * and will also handle syncing back/forth when applicable
    *
    * The listener should be started and stopped on game lifecycle
    * Fabric
    * - ClientLifecycleEvents.CLIENT_STARTED
    * - ServerLifecycleEvents.SERVER_STARTED
    * - ClientLifecycleEvents.CLIENT_STOPPING
    * - ServerLivecycleEvents.SERVER_STOPPING
    *
    * Neo
    * - Think I'll have to hook into the screen event again. Silly Neo
    * - ServerStartedEvent
    * - GameShuttingDownEvent (server and client)
    *
    * These events will start/stop the ExecutorServices I have in this stack. Need to make sure I add shutdowns for the EXECUTOR above!
    *
    * Synced Registry + SEPARATE + Client + Out of Game = Update Config State + No Sync
    *
    * */

    fun start(flags: Byte, executor: Executor, applier: (ConfigEntry<*>, ValidationResult<TomlTable>) -> Unit, updater: () -> Unit, permissionCheck: ConfigApiImpl.PermissionChecker) {
        FILE_WATCHER.scheduleAtFixedRate( { //FILE_WATCHER thread
            val entries: MutableList<Pair<Path, ConfigEntry<*>>> = mutableListOf()
            try { //lock up the config watchers while we poll the watch service
                watcherLock.lock()
                updatesLock.lock()
                var watchKey: WatchKey? = watchService.poll()
                while (watchKey != null) {
                    for (event in watchKey.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue
                        val filePath = event.context() as Path
                        val dir = watchKey.watchable() as Path
                        val path = dir.resolve(filePath)
                        val entry = configWatchers[path] ?: continue
                        if (!internalUpdates.remove(path)) { //only queue up a file update if an internal mechanism didn't do the update
                            entries.add(path to entry)
                        }
                    }
                    watchKey.reset()
                    watchKey = watchService.poll()
                }
            } finally { //unlock the watchers
                watcherLock.unlock()
                updatesLock.unlock()
            }

            if (entries.isEmpty()) return@scheduleAtFixedRate

            //push the update processing to the worker executors
            CompletableFuture.supplyAsync( { //EXECUTOR threads
                val results: MutableList<Pair<ConfigEntry<*>, ValidationResult<ConfigApiImpl.FileUpdateResult>>> = mutableListOf()
                for ((path, entry) in entries) {
                    val result = ConfigApiImpl.deserializeFileUpdate(
                        entry,
                        path,
                        "Error(s) encountered while reading a changed config file",
                        flags,
                        permissionCheck
                    ).log(ValidationResult.ErrorEntry.ENTRY_ERROR_LOGGER)
                    results.add(entry to result)
                }
                results
            }, EXECUTOR).thenAcceptAsync({ results -> //CLIENT or SERVER thread
                if (results.isNotEmpty()) {
                    for ((entry, result) in results) {
                        if (result.isValid()) {
                            ConfigApiImpl.applyFileUpdate(
                                entry.config,
                                result.get().writeConfig,
                                "Error(s) encountered while updating a config from a changed config file"
                            )
                            applier(entry, result.map { it.toml })
                        }
                    }
                    updater()
                }
            }, executor)

        }, 0L, 503L, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        watchService.close()
        FILE_WATCHER.shutdown()
    }

    fun register(entry: ConfigEntry<out Config>) {
        try {
            watcherLock.lock()
            val file = entry.config.getDir()
            val dirPath = file.toPath()
            dirPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
            val path = File(file, FcText.concat(entry.config.name, entry.config.fileType().suffix())).toPath()
            configWatchers.compute(path) { p, e ->
                if (e != null) {
                    //client wins in env with both getting registered, so chats can be sent etc.
                    //in dedicated serv, clients will not be registered so server will win, otherwise client should win
                    if (e.client && !entry.client) {
                        e
                    } else {
                        entry
                    }
                } else {
                    entry
                }
            }
        } finally {
            watcherLock.unlock()
        }
    }

    fun update(file: File) {
        try {
            updatesLock.lock()
            internalUpdates.add(file.toPath())
        } finally {
            updatesLock.unlock()
        }
    }

    /*val fileWorker: Thread = Thread.ofPlatform().name("Fzzy Config File Worker").unstarted {
       //https://stackoverflow.com/questions/16251273/can-i-watch-for-single-file-change-with-watchservice-not-the-whole-directory
       //https://www.baeldung.com/java-delay-code-execution#service
    }*/
}