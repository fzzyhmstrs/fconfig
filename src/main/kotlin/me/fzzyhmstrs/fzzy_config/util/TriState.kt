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

import com.mojang.serialization.Codec
import net.minecraft.util.StringIdentifiable
import java.util.function.BooleanSupplier
import java.util.function.Supplier

enum class TriState(private val state: String): TriStateProvider, EnumTranslatable, StringIdentifiable {
    DEFAULT("default"),
    TRUE("true"),
    FALSE("false");

    /// TriStateProvider //////////

    /**
     * Gets the value of the tri-state. Implemented from [BooleanSupplier].
     *
     * @return true if the tri-state is [TRUE], otherwise false.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun getAsBoolean(): Boolean {
        return this == TRUE
    }

    /**
     * Gets the value of the tri-state as a boxed, nullable boolean.
     *
     * @return `null` if [DEFAULT]; otherwise `true` if [TRUE] or `false` if [FALSE].
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun getBoxed(): Boolean? {
        return if (this == DEFAULT) null else this.asBoolean
    }

    /**
     * Gets the value of this tri-state.
     * If the value is [DEFAULT] then use the supplied value.
     *
     * @param value the value to fall back to
     * @return the value of the tri-state or the supplied value if [DEFAULT].
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun orElse(value: Boolean): Boolean {
        return if (this == DEFAULT) value else this.asBoolean
    }

    /**
     * Gets the value of this tri-state.
     * If the value is [DEFAULT] then use the supplied value.
     *
     * @param supplier the supplier used to get the value to fall back to
     * @return the value of the tri-state or the value of the supplier if the tri-state is [DEFAULT].
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun orElseGet(supplier: BooleanSupplier): Boolean {
        return if (this == DEFAULT) supplier.asBoolean else this.asBoolean
    }

    /**
     * Gets the value of this tri-state.
     * If the value is [DEFAULT] then use the supplied value.
     *
     * @param supplier the supplier used to get the value to fall back to
     * @return the value of the tri-state or the value of the supplier if the tri-state is [DEFAULT].
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun orElseGet(supplier: Supplier<Boolean>): Boolean {
        return if (this == DEFAULT) supplier.get() else this.asBoolean
    }

    /**
     * Validates a provided boolean input against the current tri-state.
     * - [DEFAULT] will return true no matter what (no validation)
     * - [TRUE] will return true if the input is true
     * - [FALSE] will return true if the input is false
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun validate(input: Boolean): Boolean {
        return when(this) {
            DEFAULT -> true
            TRUE -> input
            FALSE -> !input
        }
    }

    /// EnumTranslatable //////////

    override fun prefix(): String {
        return "fc.tristate"
    }

    override fun translationKey(): String {
        return "${prefix()}.${this.state}"
    }

    override fun descriptionKey(): String {
        return "${prefix()}.${this.state}.desc"
    }

    override fun prefixKey(): String {
        return ""
    }

    override fun hasPrefix(): Boolean {
        return false
    }

    /// StringIdentifiable ///////////

    override fun asString(): String {
        return this.state
    }

    companion object {

        val CODEC: Codec<TriState> = StringIdentifiable.createCodec { TriState.entries.toTypedArray() }

        /**
         * Gets the corresponding tri-state from a boolean value.
         *
         * @param bl the boolean value
         * @return [TriState.TRUE] or [TriState.FALSE] depending on the value of the boolean.
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        fun of(bl: Boolean): TriState {
            return if (bl) TRUE else FALSE
        }

        /**
         * Gets a tri-state from a nullable boolean.
         *
         * @param bl the boolean value
         * @return [DEFAULT] if `null`, Otherwise [TRUE] or [FALSE] depending on the value of the boolean.
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        fun of(bl: Boolean?): TriState {
            return if (bl == null) DEFAULT else of(bl)
        }
    }

}