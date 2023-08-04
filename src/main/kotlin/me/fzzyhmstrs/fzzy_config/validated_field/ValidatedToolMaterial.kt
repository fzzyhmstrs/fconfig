package me.fzzyhmstrs.fzzy_config.validated_field

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import me.fzzyhmstrs.fzzy_config.config_util.ValidationResult
import me.fzzyhmstrs.fzzy_config.interfaces.ConfigSerializable
import me.fzzyhmstrs.fzzy_config.validated_field.list.ValidatedStringList
import net.fabricmc.yarn.constants.MiningLevels
import net.minecraft.item.ToolMaterial
import net.minecraft.recipe.Ingredient

open class ValidatedToolMaterial protected constructor(
    durabilityDefault: ValidatedInt,
    miningSpeedDefault: ValidatedFloat,
    attackDamageDefault: ValidatedFloat,
    miningLevelDefault: ValidatedInt,
    enchantabilityDefault: ValidatedInt,
    repairIngredientDefault: ValidatedIngredient)
    :
    ToolMaterial, ConfigSerializable
{
    var durability = durabilityDefault
    var miningSpeedMultiplier = miningSpeedDefault
    var attackDamage = attackDamageDefault
    var miningLevel = miningLevelDefault
    var enchantability = enchantabilityDefault
    var repairIngredient = repairIngredientDefault

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
        return repairIngredient.get()
    }

    override fun serialize(): JsonElement {
        val str = SyncedConfigHelperV1.serializeConfig(this)
        return JsonParser.parseString(str)
    }

    override fun deserialize(json: JsonElement, fieldName: String): ValidationResult<Boolean> {
        if (json is JsonObject && json.size() == 0) return ValidationResult.error(true,"Config Section $fieldName is empty! Replacing with default section.")
        val validatedSection = SyncedConfigHelperV1.deserializeConfig(this,json)
        return if (validatedSection.isError()){
            ValidationResult.error(true,validatedSection.getError())
        } else {
            ValidationResult.success(false)
        }
    }

    override fun toString(): String {
        return SyncedConfigHelperV1.serializeConfig(this)
    }

    class Builder: AbstractBuilder<ValidatedToolMaterial, Builder>() {
        override fun builderClass(): Builder{
            return this
        }
        override fun build(): ValidatedToolMaterial {
            return ValidatedToolMaterial(d, mSM, aD, mL, e, rI)
        }
    }
    
    abstract class AbstractBuilder<T: ValidatedToolMaterial, U: AbstractBuilder<T,U>>(){
        protected var d = ValidatedInt(1,1,0)
        protected var mSM = ValidatedFloat(1f,1f,0f)
        protected var aD = ValidatedFloat(1f,1f,0f)
        protected var mL = ValidatedInt(1,4,0)
        protected var e = ValidatedInt(1,50,0)
        protected var rI = ValidatedIngredient(Ingredient.empty())

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
        fun repairIngredient(ingredient: Ingredient): U{
            rI = ValidatedIngredient(ingredient)
            return builderClass()
        }
        abstract fun build(): T
    }
}
