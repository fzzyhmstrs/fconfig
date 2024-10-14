/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.css

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.theme.css.element.CssMap
import net.minecraft.util.Identifier


object CssParser {

    private val newLines = "\\R".toRegex()
    private val whitespace = "\\s".toRegex()

    /**
     * Parses a raw css input into a CssMap
     *
     * Each separator group will be a sub-CssMap keyed to that separator
     */
    fun parse(css: String): CssMap {
        val map: MutableMap<String, MutableMap<String, CssElement>> = mutableMapOf()
        try {
            if (css.indexOf('}') < 0 || css.indexOf('{') < 0) {
                FC.LOGGER.error("Improperly formatted css file: no group found! Empty result generated")
                return CssMap.of(mapOf())
            }
            val trimmedLines = css.trimStart().trimEnd().split(newLines).map { it.trimStart().trimEnd() }
            var currentGroup: MutableList<String> = mutableListOf()
            for ((index, line) in trimmedLines.withIndex()) {
                if (line.isEmpty()) continue
                if (line.contains('}')) {
                    currentGroup.add(line)
                    val group = currentGroup.joinToString("")
                    parseGroup(group, map)
                    currentGroup = mutableListOf()
                    continue
                }
                if (!line.endsWith(',') && !line.endsWith(';') && !line.endsWith('{')) {
                    FC.LOGGER.error("Error parsing line $index, expected terminator (, or ;): $line")
                    FC.LOGGER.error("Results may be incomplete")
                    continue
                }
                currentGroup.add(line)

            }
            return CssMap.of(map.mapValues { CssMap.of(it.value) })

        } catch (e: Throwable) {
            FC.LOGGER.error("Critical exception encountered while parsing css file. Results may be incomplete")
            e.printStackTrace()
            return CssMap.of(map.mapValues { CssMap.of(it.value) })
        }
    }

    private fun parseGroup(group: String, map: MutableMap<String, MutableMap<String, CssElement>>) {

        val selectors = group.substring(0, group.indexOf('{')).split(",")
        val propVals = group.substringAfter('{').trimEnd('}').split(";")
        val propMap: MutableMap<String, String> = mutableMapOf()
        for (propVal in propVals) {
            if (!propVal.contains(':')) {
                FC.LOGGER.error("Error parsing property, : expected: $propVal")
                continue
            }
            propMap[propVal.substringBefore(':')] = propVal.substringAfter(':').trimEnd(';')
        }
        for (selector in selectors) {
            val selectorPropMap = map.computeIfAbsent(selector) { _ -> mutableMapOf() }
        }
    }


    /**
     * Processes a parsed basic CssMap into an identifier-keyed map of style-parseable elements
     *
     * This is not a general purpose CSS parser! There are some internal parsing details that alter how the actual set of properties is defined in the output
     * ```css
     * /* selectors are the namespace of the resulting identifier */
     * /* dot separators in a selector will be a subfolder in the id path: fzzy_config.widget -> fzzy_config:widget/property */
     * fzzy_config {
     *   /* the property is the path of the identifier */
     *   /* dash separated phrases have special meaning. They serve as map keys for key-like CssElement */
     *   style_property-key: value; /* values work pretty much like you would expect for css. Different properties might have different syntax for the value they need*/
     *   style_property: other_value; /* illegal in this syntax */
     *   other_property: bloop; /* this is some plain non-map value */
     *   list_property: a b c d e; /* space separated values are applied to a list of CssElement*/
     * }
     *
     * /* CssElement is a JsonElement like wrapper. It will store values as a primitive, map, or list, and primitives will be (mostly) cross compatible*/
     * /* there is a unique element type that interacts with css in a natural way: color */
     * ```
     */
    fun parseToIdentifierMap(element: CssElement): Map<Identifier, CssElement> {
        if (element !is CssMap) throw IllegalStateException("Needs to be a CssMap")
        return mapOf()
    }

}