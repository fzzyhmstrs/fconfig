package me.fzzyhmstrs.fzzy_config.validated_field

import me.fzzyhmstrs.fzzy_config.validated_field.list.ValidatedStringList
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

    class Builder: AbstractBuilder<ValidatedToolMaterial, Builder>{
        override fun builderClass(): Builder{
            return this
        }
        override fun build(): ValidatedToolMaterial {
            return ValidatedToolMaterial(d, mSM, aD, mL, e, rI)
        }
    }
    
    abstract class AbstractBuilder<T: ValidatedToolMaterial, U: AbstractBuilder>(){
        protected var d = ValidatedInt(1,1,0)
        protected var mSM = ValidatedFloat(1f,1f,0f)
        protected var aD = ValidatedFloat(1f,1f,0f)
        protected var mL = ValidatedInt(1,4,0)
        protected var e = ValidatedInt(1,35,0)
        protected var rI = ValidatedIngredient(Ingredient.empty())

        abstract fun builderClass(): U
        
        fun durability(default: Int, max: Int = Short.MAX_VALUE): U{
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
            mL = ValidatedInt(default,max,1)
            return builderClass()
        }
        fun enchantability(default: Int, max: Int): U{
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
