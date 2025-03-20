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

import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.peanuuutz.tomlkt.TomlElement

import java.util.function.Function

/**
 * Defines the output file type used for the config.
 * @author fzzyhmstrs
 * @since 0.6.7
 */
enum class FileType(private val suffix: String, private val encoder: Function<TomlElement, ValidationResult<String>>, private val decoder: Function<String, ValidationResult<TomlElement>>) {
    /**
     * TOML output format. The standard output.
     * @author fzzyhmstrs
     * @since 0.6.7
     */
    TOML(".toml", ConfigApiImpl::encodeToml, ConfigApiImpl::decodeToml),
    /**
     * JSON output format. Uses GSON and codecs to parse the json to/from the TOML used internally.
     * @author fzzyhmstrs
     * @since 0.6.7
     */
    JSON(".json", ConfigApiImpl::encodeJson, ConfigApiImpl::decodeJson),
    /**
     * JSON5 output format. Uses Jankson and codecs to parse the json5 to/from the TOML used internally. Comments will be automatically carried over from any @Comment or @TomlComment annotations applied.
     * @author fzzyhmstrs
     * @since 0.6.7
     */
    JSON5(".json5", ConfigApiImpl::encodeJson5, ConfigApiImpl::decodeJson5),
    /**
     * JSONC output format. Uses Jankson and codecs to parse the jsonc to/from the TOML used internally. Comments will be automatically carried over from any @Comment or @TomlComment annotations applied.
     *
     * Note that this is using the same json5 codec, which by default is set to mimic jsonc structure (quoted keys, no trailing comma, only json data structs).
     * @author fzzyhmstrs
     * @since 0.6.7
     */
    JSONC(".jsonc", ConfigApiImpl::encodeJson5, ConfigApiImpl::decodeJson5);

    fun suffix(): String {
        return suffix
    }

    fun encode(input: TomlElement): ValidationResult<String> {
        return encoder.apply(input)
    }

    fun decode(input: String): ValidationResult<TomlElement> {
        return decoder.apply(input)
    }
}