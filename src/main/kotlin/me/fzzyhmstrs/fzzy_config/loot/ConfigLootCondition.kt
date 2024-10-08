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

import com.mojang.serialization.MapCodec
import me.fzzyhmstrs.modifier_core.MC
import me.fzzyhmstrs.modifier_core.event.EventSource
import net.minecraft.entity.mob.Monster
import net.minecraft.loot.condition.LootCondition
import net.minecraft.loot.condition.LootConditionType
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameter
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

/**
 * Tests if the source entity is a [Monster]
 * @param source [EventSource] the entity to test
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ConfigLootCondition(private val scope: String): LootCondition {
    override fun test(context: LootContext): Boolean {
        val entity = context.get(source.parameter)
        return entity is Monster
    }

    override fun getType(): LootConditionType {
        return TYPE
    }

    override fun getRequiredParameters(): MutableSet<LootContextParameter<*>> {
        return mutableSetOf(source.parameter)
    }

    companion object {

        private var cachedResults: MutableMap<String, Supplier<Boolean>> = mutableMapOf()

        internal fun invalidateResults() {
            cachedResults = mutableMapOf()
        }
        
        private fun getResult(scope: String): Boolean {
            return cachedResults.computeIfAbsent(scope) { scope -> computeResultSupplier(scope) }?.get() ?: false
        }

        private fun computeResultSupplier(scope: String): Supplier<Boolean> {
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
        
        fun create(scope: String): LootCondition.Builder {
            return LootCondition.Builder { ConfigLootCondition(scope) }
        }

        val CODEC: MapCodec<ConfigLootCondition> = RecordCodecBuilder.mapCodec { instance: RecordCodecBuilder.Instance<ConfigLootCondition> ->
            instance.group(
                Codec.STRING.fieldOf("scope").forGetter(ConfigLootCondition::scope)
            ).apply(instance, ::ConfigLootCondition)
        }

        val TYPE: LootConditionType =  Registry.register(Registries.LOOT_CONDITION_TYPE, "config".fcId(), LootConditionType(CODEC))

        internal fun init(){}
    }
}
