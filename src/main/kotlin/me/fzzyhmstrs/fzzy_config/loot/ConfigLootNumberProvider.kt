/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.loot

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.result.ResultProviderSupplier
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameter
import net.minecraft.loot.provider.number.LootNumberProvider
import net.minecraft.loot.provider.number.LootNumberProviderType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import java.util.function.Supplier

class ConfigLootNumberProvider(private val scope: String, private val scaling: Float): LootNumberProvider {

    override fun nextFloat(context: LootContext): Float {
        return resultProvider.getResult(scope) / scaling
    }

    override fun getType(): LootNumberProviderType {
        return TYPE
    }

    override fun getRequiredParameters(): MutableSet<LootContextParameter<*>> {
        return mutableSetOf()
    }

    companion object {

        private val resultProvider = ConfigApiImpl.createSimpleResultProvider(0f, ResultProviderSupplier { scope, _, thing, thingProp ->
            when (thing) {
                is ValidatedNumber<*> -> Supplier { (thing.get().toFloat()) }
                is Number -> Supplier { ((thingProp.getter.call()as Number).toFloat()) }
                else -> {
                    FC.LOGGER.error("Error encountered while reading value for $scope. Value is not a number! Default value ${Supplier { 0f }} used.")
                    Supplier { 0f }
                }
            }
        })

        private val CODEC: MapCodec<ConfigLootNumberProvider> = RecordCodecBuilder.mapCodec { instance: RecordCodecBuilder.Instance<ConfigLootNumberProvider> ->
            instance.group(
                Codec.STRING.fieldOf("scope").forGetter(ConfigLootNumberProvider::scope),
                Codec.FLOAT.optionalFieldOf("scaling", 1f).forGetter(ConfigLootNumberProvider::scaling)
            ).apply(instance, ::ConfigLootNumberProvider)
        }

        internal val TYPE = Registry.register(
            Registries.LOOT_NUMBER_PROVIDER_TYPE,
            "config".fcId(),
            LootNumberProviderType(CODEC)
        )

        fun init() {}
    }
}