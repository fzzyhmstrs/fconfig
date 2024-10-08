/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.modifier_core.loot

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.fzzyhmstrs.fzzy_core.FC
import me.fzzyhmstrs.fzzy_core.fcId
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameter
import net.minecraft.loot.provider.number.LootNumberProvider
import net.minecraft.loot.provider.number.LootNumberProviderType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

class ConfigLootNumberProvider(private val scope: String, private val scaling: Float): LootNumberProvider {

    override fun nextFloat(context: LootContext): Float {
        return getResult(scope) / scaling
    }

    override fun getType(): LootNumberProviderType {
        return TYPE
    }

    override fun getRequiredParameters(): MutableSet<LootContextParameter<*>> {
        return mutableSetOf()
    }

    companion object {

        private var cachedResults: MutableMap<String, Supplier<Float>> = mutableMapOf()

        internal fun invalidateConfigLootNumberResults() {
            cachedResults = mutableMapOf()
        }
        
        private fun getResult(scope: String): Float {
            return cachedResults.computeIfAbsent(scope) { scope -> computeResultSupplier(scope) }
        }

        private fun computeResultSupplier(scope: String): Supplier<Float> {
            try {
                var startIndex = 0
                while (startIndex < scope.length) {
                    val nextStartIndex = scope.indexOf(".", startIndex)
                    if (nextStartIndex == -1) {
                        FC.LOGGER.error("Invalid scope $scope provided to a Config Loot Number. Config not found! Default value of 0.0 used.")
                        return Supplier { 0f }
                    }
                    startIndex = nextStartIndex
                    val testScope = scope.subString(0, nextStartIndex)
                    val config = SyncedConfigRegistry.syncedConfigs()[testScope] ?: continue
                    if (testScope == scope) {
                        FC.LOGGER.error("Invalid scope $scope provided to a Config Loot Number. No setting scope provided! Default value of 0.0 used.")
                        FC.LOGGER.error("Found: $scope")
                        FC.LOGGER.error("Need $scope[.subScopes].settingName")
                        return Supplier { 0f }
                    }
                    ConfigApiImpl.drill(config, scope.removePrefix("$testScope."), '.', ConfigApiImpl.IGNORE_VISIBILITY)  { config, _, _, thing, thingProp, _, _, _ ->
                        if (thing == null) {
                            FC.LOGGER.error("Error encountered while reading Config Loot Number value for $scope. Value was null! Default value of 0.0 used.")
                            return Supplier { 0f }
                        }
                        return if (thing is ValidatedNumber<*>) {
                            Supplier { (thing.get() as Number).toFloat() }
                        } else if (thing is Number) {
                            Supplier { (thingProp.get(config) as Number).toFloat() }
                        } else {
                            FC.LOGGER.error("Error encountered while reading Config Loot Number value for $scope. Value is not a number! Default value of 0.0 used.")
                            Supplier { 0f }
                        }
                    }
                }
            } catch (e: Throwable) {
                FC.LOGGER.error("Critical exception encountered while reading Config Loot Number value for $scope. Default value of 0.0 used")
                return Supplier { 0f }
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
            "config".fcId(),
            LootNumberProviderType(CODEC)
        )

        fun init() {}
    }
}
