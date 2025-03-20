/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.util

import blue.endless.jankson.*
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import me.fzzyhmstrs.fzzy_config.nullCast
import net.peanuuutz.tomlkt.*
import java.util.stream.Stream

/**
 * Dynamic Ops for dealing with TomlElements in Codecs and other related things
 * @see INSTANCE
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class TomlOps: DynamicOps<TomlElement> {

    companion object {
        @JvmStatic
        val INSTANCE = TomlOps()

        fun convertToJson5(tomlTable: TomlTable): JsonObject {
            val obj = JsonObject()
            for ((key, value) in tomlTable.content) {
                val comment = tomlTable.annotations[key]?.firstOrNull { it is TomlComment }?.nullCast<TomlComment>()?.text
                val element = convertElement(value)
                obj.put(key, element, comment)
            }
            return obj
        }

        private fun convertElement(input: TomlElement): JsonElement {
            return when (input) {
                is TomlTable -> {
                    convertToJson5(input)
                }
                is TomlArray -> {
                    val arr = JsonArray()
                    for (value in input) {
                        arr.add(convertElement(value))
                    }
                    arr
                }
                is TomlLiteral -> {
                    when (input.type) {
                        TomlLiteral.Type.Boolean -> JsonPrimitive(input.toBoolean())
                        TomlLiteral.Type.Integer -> JsonPrimitive(input.toLong())
                        TomlLiteral.Type.Float -> JsonPrimitive(input.toDouble())
                        else -> JsonPrimitive(input.toString())
                    }
                }
                else -> JsonNull.INSTANCE
            }
        }
    }

    override fun empty(): TomlElement {
        return TomlNull
    }

    override fun createNumeric(i: Number): TomlElement {
        return TomlLiteral(i)
    }
    override fun createByte(value: Byte): TomlElement {
        return TomlLiteral(value)
    }
    override fun createShort(value: Short): TomlElement {
        return TomlLiteral(value)
    }
    override fun createInt(value: Int): TomlElement {
        return TomlLiteral(value)
    }
    override fun createLong(value: Long): TomlElement {
        return TomlLiteral(value)
    }
    override fun createDouble(value: Double): TomlElement {
        return TomlLiteral(value)
    }
    override fun createFloat(value: Float): TomlElement {
        return TomlLiteral(value)
    }

    override fun createBoolean(value: Boolean): TomlElement {
        return TomlLiteral(value)
    }
    override fun createString(value: String): TomlElement {
        return try {
            TomlLiteral(NativeLocalTime(value))
        } catch (e: Throwable) {
            try {
                TomlLiteral(NativeLocalDate(value))
            } catch (e: Throwable) {
                try {
                    TomlLiteral(NativeLocalDateTime(value))
                } catch (e: Throwable) {
                    try {
                        TomlLiteral(NativeOffsetDateTime(value))
                    } catch (e: Throwable) {
                        TomlLiteral(value)
                    }
                }
            }
        }
    }

    override fun remove(input: TomlElement, key: String): TomlElement {
        if (input is TomlTable) {
            val table = TomlTableBuilder(input.size)
            for ((k, el) in input) {
                if (k == key) continue
                table.element(k, el)
            }
            return table.build()
        }
        return input
    }

    override fun createList(input: Stream<TomlElement>): TomlElement {
        val array = TomlArrayBuilder()
        for (el in input) {
            array.element(el)
        }
        return array.build()
    }

    override fun getStream(input: TomlElement): DataResult<Stream<TomlElement>> {
        if (input is TomlArray) {
            return DataResult.success(input.stream().filter { it !is TomlNull })
        }
        return DataResult.error{ "Not a toml array: $input" }
    }

    override fun createMap(map: Stream<Pair<TomlElement, TomlElement>>): TomlElement {
        val table = TomlTableBuilder()
        for (pair in map) {
            table.element(pair.first.content, pair.second)
        }
        return table.build()
    }

    override fun getMapValues(input: TomlElement): DataResult<Stream<Pair<TomlElement, TomlElement>>> {
        if(input is TomlTable) {
            return DataResult.success(input.map { e -> Pair(TomlLiteral(e.key) as TomlElement, e.value) }.stream())
        }
        return DataResult.error{ "Not a toml table: $input" }
    }

    override fun mergeToMap(map: TomlElement, key: TomlElement, value: TomlElement): DataResult<TomlElement> {
        if (map is TomlTable) {
            val table = TomlTableBuilder()
            table.elements(map.content)
            try {
                table.element(key.content, value)
            } catch (e: Throwable) {
                return DataResult.error{ "Not a valid map key: $key" }
            }
            return DataResult.success(table.build())
        }
        return DataResult.error{ "Not a toml table: $map" }
    }

    override fun mergeToList(list: TomlElement, value: TomlElement): DataResult<TomlElement> {
        if (list is TomlArray) {
            val array = TomlArrayBuilder(list.size+1)
            array.elements(list)
            array.element(value)
            return DataResult.success(array.build())
        }
        return DataResult.error { "Not a toml array: $list" }
    }

    override fun mergeToList(list: TomlElement, values: MutableList<TomlElement>): DataResult<TomlElement> {
        if(list is TomlArray) {
            val array = TomlArrayBuilder(list.size + values.size)
            array.elements(list)
            array.elements(values)
            return DataResult.success(array.build())
        }
        return DataResult.error { "Not a toml array: $list" }
    }

    override fun getStringValue(input: TomlElement): DataResult<String> {
        if (input is TomlLiteral && input.type == TomlLiteral.Type.String) {
            return DataResult.success(input.toString())
        }
        return DataResult .error { "Not a string: $input" }
    }

    override fun getNumberValue(input: TomlElement): DataResult<Number> {
        if (input is TomlLiteral) {
            return when (input.type) {
                TomlLiteral.Type.Integer -> {
                    DataResult.success(input.toLong())
                }
                TomlLiteral.Type.Float -> {
                    DataResult.success(input.toDouble())
                }
                TomlLiteral.Type.Boolean -> {
                    DataResult.success(if(input.toBoolean()) 1 else 0)
                }
                TomlLiteral.Type.String -> {
                    try {
                        DataResult.success(java.lang.Long.parseLong(input.toString()))
                    } catch (e: Throwable) {
                        DataResult.error { "Not a number: $input" }
                    }
                }
                else -> {
                    DataResult.error { "Not a number: $input" }
                }
            }
        }
        return DataResult .error { "Not a number: $input" }
    }

    override fun getBooleanValue(input: TomlElement): DataResult<Boolean> {
        return try {
            DataResult.success((input as TomlLiteral).toBoolean())
        } catch (e: Throwable) {
            DataResult.error { "Not a boolean: $input" }
        }
    }

    override fun <U : Any?> convertTo(outOps: DynamicOps<U>, input: TomlElement): U {
        if(input is TomlTable) {
            return convertMap(outOps, input)
        }
        if (input is TomlArray) {
            return convertList(outOps, input)
        }
        if (input is TomlNull) {
            return outOps.empty()
        }
        val literal = input.asTomlLiteral()
        when (literal.type) {
            TomlLiteral.Type.String -> {
                return outOps.createString(literal.toString())
            }
            TomlLiteral.Type.Boolean -> {
                return outOps.createBoolean(literal.toBoolean())
            }
            TomlLiteral.Type.Integer -> {
                val l = literal.toLong()
                return if (l <= Byte.MAX_VALUE && l >= Byte.MIN_VALUE) {
                    outOps.createByte(l.toByte())
                } else if (l <= Short.MAX_VALUE && l >= Short.MIN_VALUE ) {
                    outOps.createShort(l.toShort())
                } else if (l <= Int.MAX_VALUE && l >= Int.MIN_VALUE ) {
                    outOps.createInt(l.toInt())
                } else {
                    outOps.createLong(l)
                }
            }
            TomlLiteral.Type.Float -> {
                val d = literal.toDouble()
                return if (d <= Float.MAX_VALUE && d >= -Float.MAX_VALUE) {
                    outOps.createFloat(d.toFloat())
                } else {
                    outOps.createDouble(d)
                }
            }
            else -> {
                return outOps.createString(literal.toString())
            }
        }
    }
}