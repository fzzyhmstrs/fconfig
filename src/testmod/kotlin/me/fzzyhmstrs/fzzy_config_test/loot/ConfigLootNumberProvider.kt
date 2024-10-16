/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config_test.loot

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.provider.number.LootNumberProvider
import net.minecraft.loot.provider.number.LootNumberProviderType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import java.util.function.Supplier

class ConfigLootNumberProvider(private val scope: String, private val scaling: Float): LootNumberProvider {

    override fun nextFloat(context: LootContext): Float {
        return resultProvider.getResult(scope) / scaling
    }

    override fun getType(): LootNumberProviderType {
        return TYPE
    }

    companion object {

        private val resultProvider = ConfigApi.result().createSimpleResultProvider(0f) { scope, _, thing, thingProp ->
            when (thing) {
                is ValidatedNumber<*> -> Supplier { (thing.get().toFloat()) }
                is Number -> Supplier { ((thingProp.getter.call() as Number).toFloat()) }
                else -> {
                    println("Error encountered while reading value for $scope. Value is not a number! Default value 0f used.")
                    Supplier { 0f }
                }
            }
        }

        private val CODEC: MapCodec<ConfigLootNumberProvider> = RecordCodecBuilder.mapCodec { instance: RecordCodecBuilder.Instance<ConfigLootNumberProvider> ->
            instance.group(
                Codec.STRING.fieldOf("scope").forGetter(ConfigLootNumberProvider::scope),
                Codec.FLOAT.optionalFieldOf("scaling", 1f).forGetter(ConfigLootNumberProvider::scaling)
            ).apply(instance, ::ConfigLootNumberProvider)
        }

        internal val TYPE = Registry.register(
            Registries.LOOT_NUMBER_PROVIDER_TYPE,
            Identifier.of("fzzy_config_test","config"),
            LootNumberProviderType(CODEC)
        )

        fun init() {}
    }
}