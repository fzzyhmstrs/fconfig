/*
* Copyright (c) 2024-5 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.api

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.peanuuutz.tomlkt.TomlElement

import java.util.function.Function

/**
 * Defines the output file
 * @author fzzyhmstrs
 * @since 0.6.7
 */
enum class ConvertType(private val encoder: Function<TomlElement, ValidationResult<String>>, private val decoder: Function<String, ValidationResult<TomlElement>>) {
    TOML(ConfigApiImpl::encodeToml, ConfigApiImpl::decodeToml),
    JSON(ConfigApiImpl::encodeJson, ConfigApiImpl::decodeJson);
    //JSON5(ConfigApiImpl::encodeJson5, ConfigApiImpl::decodeJson5);

    fun encode(input: TomlElement): ValidationResult<String>> {
        return encoder.apply(input)
    }

    fun decode(input: String): ValidationResult<TomlElement> {
        return decoder.apply(input)
    }
}
