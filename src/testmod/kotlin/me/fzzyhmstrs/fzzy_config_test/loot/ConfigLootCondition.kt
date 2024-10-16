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
import net.minecraft.loot.condition.LootCondition
import net.minecraft.loot.condition.LootConditionType
import net.minecraft.loot.context.LootContext
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

/**
 * Loot Condition tied to a boolean-type [Config][me.fzzyhmstrs.fzzy_config.config.Config] setting. Passes if the config setting is true, fails if false.
 * @param scope String - the setting `scope`. See the [wiki translation page](https://github.com/fzzyhmstrs/fconfig/wiki/Translation) or various doc examples to see an example of a scope. In general, a valid scope will be `namespace.path.settingName`, with the namespace and path coming from the Identifier used in config construction.
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ConfigLootCondition(private val scope: String): LootCondition {

    override fun test(context: LootContext): Boolean {
        return resultProvider.getResult(scope)
    }

    override fun getType(): LootConditionType {
        return TYPE
    }

    companion object {

        private val resultProvider = ConfigApi.result().createSimpleResultProvider(false, Boolean::class)

        fun create(scope: String): LootCondition.Builder {
            return LootCondition.Builder { ConfigLootCondition(scope) }
        }

        val CODEC: MapCodec<ConfigLootCondition> = RecordCodecBuilder.mapCodec { instance: RecordCodecBuilder.Instance<ConfigLootCondition> ->
            instance.group(
                Codec.STRING.fieldOf("scope").forGetter(ConfigLootCondition::scope)
            ).apply(instance, ::ConfigLootCondition)
        }

        val TYPE: LootConditionType =  Registry.register(Registries.LOOT_CONDITION_TYPE, Identifier.of("fzzy_config_test","config"), LootConditionType(CODEC))

        internal fun init(){}
    }
}