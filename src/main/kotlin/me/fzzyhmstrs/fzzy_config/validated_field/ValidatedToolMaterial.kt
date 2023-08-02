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
    repairIngredientDefault: ValidatedStringList)
    :
    ToolMaterial
{

    var durability = durabilityDefault
    var miningSpeedMultiplier = miningSpeedDefault
    var attackDamage = attackDamageDefault
    var miningLevel = miningLevelDefault
    var enchantability = enchantabilityDefault
    var repairIngredient = repairIngredientDefault

    override fun getDurability(): Int {
        TODO("Not yet implemented")
    }

    override fun getMiningSpeedMultiplier(): Float {
        TODO("Not yet implemented")
    }

    override fun getAttackDamage(): Float {
        TODO("Not yet implemented")
    }

    override fun getMiningLevel(): Int {
        TODO("Not yet implemented")
    }

    override fun getEnchantability(): Int {
        TODO("Not yet implemented")
    }

    override fun getRepairIngredient(): Ingredient {
        TODO("Not yet implemented")
    }
}