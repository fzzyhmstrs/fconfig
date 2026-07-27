/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.value

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.minecraft.client.MinecraftClient

class LengthValue(private val value: Double, private val unit: UnitType) {

    constructor(value: Int): this(value.toDouble(), Unit.PIXELS)

    fun getValue(): Int {
        return if(value == 0.0) 0 else unit.applyUnit(value)
    }

    interface UnitType {
        fun applyUnit(value: Double): Int
    }

    enum class Unit: UnitType {
        PIXELS {
            override fun applyUnit(value: Double): Int {
                return value.toInt()
            }
        },
        INCHES {
            override fun applyUnit(value: Double): Int {
                return ((value * 96) / MinecraftClient.getInstance().window.scaleFactor).toInt()
            }
        },
        PICAS {
            override fun applyUnit(value: Double): Int {
                return (((value * 96) / 6) / MinecraftClient.getInstance().window.scaleFactor).toInt()
            }
        },
        POINTS {
            override fun applyUnit(value: Double): Int {
                return (((value * 96) / 72) / MinecraftClient.getInstance().window.scaleFactor).toInt()
            }
        },
        CMS {
            override fun applyUnit(value: Double): Int {
                return (((value * 96) / 2.54) / MinecraftClient.getInstance().window.scaleFactor).toInt()
            }
        },
        MMS {
            override fun applyUnit(value: Double): Int {
                return (((value * 96) / 25.4) / MinecraftClient.getInstance().window.scaleFactor).toInt()
            }
        },
        QS {
            override fun applyUnit(value: Double): Int {
                return (((value * 96) / 101.6) / MinecraftClient.getInstance().window.scaleFactor).toInt()
            }
        };
    }

    companion object {
        private val unit2Unit: Map<String, Unit> = mapOf(
            "px" to Unit.PIXELS,
            "in" to Unit.INCHES,
            "pc" to Unit.PICAS,
            "pt" to Unit.POINTS,
            "cm" to Unit.CMS,
            "mm" to Unit.MMS,
            "Q" to Unit.QS)

        private val UNKNOWN_UNIT = ValidationResult.ErrorEntry.Type<String>("Unknown Length Unit")

        fun parse(input: CssType.NumberWithUnitValue): ValidationResult<LengthValue?> {
            val u = unit2Unit[input.unit()] ?: return ValidationResult.error(null, UNKNOWN_UNIT, "Allowable: ${unit2Unit.keys}, got ${input.unit()}")
            return ValidationResult.success(LengthValue(input.getDouble(), u))
        }
    }
}