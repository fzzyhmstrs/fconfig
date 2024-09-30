/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.impl

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.util.TomlOps
import net.minecraft.resource.ResourceFinder
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.SynchronousResourceReloader
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.TomlNativeReader

class ThemeReloader: SynchronousResourceReloader {

    private val directory = "fzzy_config/themes"

    override fun reload(manager: ResourceManager) {

        val builders: MutableMap<Identifier, ThemeBuilder> = mutableMapOf()

        val jsonFinder = ResourceFinder.json(directory)
        val jsonResources = jsonFinder.findAllResources(manager)
        for ((id, jsonResourceList) in jsonResources) {
            FC.LOGGER.info("Found json theme file(s) for $id")
            for (jsonResource in jsonResourceList) {
                jsonResource.reader.use {
                    try {
                        val json = JsonParser.parseReader(it)
                        val parseResult = ThemeFile.codec.parse(JsonOps.INSTANCE, json)
                        if (parseResult.isError) {
                            FC.LOGGER.error(parseResult.error().get().message())
                            return@use
                        } else {
                            val themeId = stripIdentifier(id, ".json")
                            val themeFile = parseResult.result().get()
                            val themeBuilder = builders.computeIfAbsent(themeId) { _ -> ThemeBuilder() }
                            themeBuilder.addFile(themeFile)
                        }
                    } catch (e: Throwable) {
                        FC.LOGGER.error("Critical error encountered while parsing json theme $id")
                        FC.LOGGER.error(e.message)
                    }
                }
            }
        }

        val tomlFinder = ResourceFinder(directory, ".toml")
        val tomlResources = tomlFinder.findAllResources(manager)
        for ((id, tomlResourceList) in tomlResources) {
            FC.LOGGER.info("Found toml theme file(s) for $id")
            for (tomlResource in tomlResourceList) {
                tomlResource.reader.use {
                    try {
                        val toml = Toml.parseToTomlTable(TomlNativeReader(it))
                        val parseResult = ThemeFile.codec.parse(TomlOps.INSTANCE, toml)
                        if (parseResult.isError) {
                            FC.LOGGER.error(parseResult.error().get().message())
                            return@use
                        } else {
                            val themeId = stripIdentifier(id, ".toml")
                            val themeFile = parseResult.result().get()
                            val themeBuilder = builders.computeIfAbsent(themeId) { _ -> ThemeBuilder() }
                            themeBuilder.addFile(themeFile)
                        }
                    } catch (e: Throwable) {
                        FC.LOGGER.error("Critical error encountered while parsing toml theme $id")
                        FC.LOGGER.error(e.message)
                    }
                }

            }
        }

        ThemeApiImpl.updateThemes(builders.mapValues { (_, builder) -> builder.build() }.also { println(it) })
    }

    private fun stripIdentifier(resourceId: Identifier, fileExtension: String): Identifier {
        resourceId.namespace.removePrefix(directory)
        return Identifier.of(resourceId.namespace, resourceId.path.substring(directory.length + 1).removeSuffix(fileExtension))
    }
}