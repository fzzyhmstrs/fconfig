/*
package me.fzzyhmstrs.fzzy_config.validated_field

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.fzzyhmstrs.fzzy_config.api.ConfigHelper
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.interfaces.ConfigSerializable
import me.fzzyhmstrs.fzzy_config.interfaces.ServerClientSynced
import me.fzzyhmstrs.fzzy_config.validated_field_v2.number.ValidatedInt
import net.fabricmc.yarn.constants.MiningLevels
import net.minecraft.item.ToolMaterial
import net.minecraft.recipe.Ingredient

open class ValidatedToolMaterial protected constructor(
    durabilityDefault: ValidatedInt,
    miningSpeedDefault: ValidatedFloat,
    attackDamageDefault: ValidatedFloat,
    miningLevelDefault: ValidatedInt,
    enchantabilityDefault: ValidatedInt
)
    :
    ToolMaterial, ConfigSerializable, ServerClientSynced
{
    var durability = durabilityDefault
    var miningSpeedMultiplier = miningSpeedDefault
    var attackDamage = attackDamageDefault
    var miningLevel = miningLevelDefault
    var enchantability = enchantabilityDefault

    override fun getDurability(): Int {
        return durability.get()
    }

    override fun getMiningSpeedMultiplier(): Float {
        return miningSpeedMultiplier.get()
    }

    override fun getAttackDamage(): Float {
        return attackDamage.get()
    }

    override fun getMiningLevel(): Int {
        return miningLevel.get()
    }

    override fun getEnchantability(): Int {
        return enchantability.get()
    }

    override fun getRepairIngredient(): Ingredient {
        return Ingredient.EMPTY
    }

    override fun serialize(): JsonElement {
        val str = ConfigHelper.serializeConfig(this,)
        return JsonParser.parseString(str)
    }

    override fun deserialize(json: JsonElement, fieldName: String): ValidationResult<Boolean> {
        if (json is JsonObject && json.size() == 0) return ValidationResult.error(true,"Config Section $fieldName is empty! Replacing with default section.")
        val validatedSection = ValidationResult.success(this) //SyncedConfigHelperV1.deserializeConfig(this,json)
        return if (validatedSection.isError()){
            ValidationResult.error(true,validatedSection.getError())
        } else {
            ValidationResult.success(false)
        }
    }

    override fun toString(): String {
        return ConfigHelper.serializeConfig(this,)
    }

    class Builder: AbstractBuilder<ValidatedToolMaterial, Builder>() {
        override fun builderClass(): Builder{
            return this
        }
        override fun build(): ValidatedToolMaterial {
            return ValidatedToolMaterial(d, mSM, aD, mL, e)
        }
    }

    abstract class AbstractBuilder<T: ValidatedToolMaterial, U: AbstractBuilder<T,U>>(){
        protected var d = ValidatedInt(1,1,0)
        protected var mSM = ValidatedFloat(1f,1f,0f)
        protected var aD = ValidatedFloat(1f,1f,0f)
        protected var mL = ValidatedInt(1,4,0)
        protected var e = ValidatedInt(1,50,0)

        abstract fun builderClass(): U

        fun durability(default: Int, max: Int = Short.MAX_VALUE.toInt()): U{
            d = ValidatedInt(default,max,0)
            return builderClass()
        }
        fun miningSpeedMultiplier(default: Float, max: Float = 20f): U{
            mSM = ValidatedFloat(default, max, 1f)
            return builderClass()
        }
        fun attackDamage(default: Float, max: Float = 50f): U{
            aD = ValidatedFloat(default,max,0f)
            return builderClass()
        }
        fun miningLevel(default: Int, max: Int = MiningLevels.NETHERITE): U{
            mL = ValidatedInt(default,max,0)
            return builderClass()
        }
        fun enchantability(default: Int, max: Int = 50): U{
            e = ValidatedInt(default,max,1)
            return builderClass()
        }
        abstract fun build(): T
    }
}*/