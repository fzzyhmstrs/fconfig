package me.fzzyhmstrs.fzzy_config.validated_field

import me.fzzyhmstrs.fzzy_config.validated_field.list.ValidatedStringList
import net.minecraft.item.ToolMaterial
import net.minecraft.recipe.Ingredient

open class ValidatedArmorMaterial protected constructor(
    protected val name: String,
    protected val soundEvent: SoundEvent,
    repairIngredientDefault: ValidatedIngredient,
    enchantabilityDefault: ValidatedInt,
    protectionAmountsDefault: ValidatedSeries<Int>,
    durabilityMultiplierDefault: ValidatedInt,
    knockbackResistanceDefault: ValidatedFloat,
    toughnessDefault: ValidatedFloat)
    :
    ArmorMaterial, ConfigSerializable
{
    private val BASE_DURABILITY = intArrayOf(13, 15, 16, 11)

    var repairIngredient = repairIngredientDefault
    var enchantability = enchantabilityDefault
    var protectionAmounts = protectionAmountsDefault
    var durabilityMultiplier = durabilityMultiplierDefault
    var knockbackResistance = knockbackResistanceDefault
    var toughness = toughnessDefault
    
    override fun getName(): String{
        return name
    }
    override fun getEquipSound(): SoundEvent{
        return soundEvent
    }
    override fun getRepairIngredient(): Ingredient?{
        return repairIngredient.get()
    }
    override fun getEnchantability(): Int{
        return enchantability.get()
    }
    override fun getProtection(type: Type): Int{
        return protectionAmounts.get(type.equipmentSlot.entitySlotId)
    }
    override fun getDurability(type: Type): Int{
        return BASE_DURABILITY[type.equipmentSlot.entitySlotId] * durabilityMultiplier.get()
    }
    override fun getKnockbackResistance(): Float{
        return knockbackResistance.get()
    }
    override fun getToughness(): Float{
        return toughness.get()
    }

    class Builder(name: String, soundEvent: SoundEvent): AbstractBuilder<ValidatedToolMaterial, Builder>{
        override fun builderClass(): Builder{
            return this
        }
        override fun build(): ValidatedArmorMaterial {
            return ValidatedArmorMaterial(name, soundEvent, rI, e, pA, dM, kR, t)
        }
    }
    
    abstract class AbstractBuilder(protected val name: String, protected val soundEvent: SoundEvent)<T: ArmorMaterial, U: AbstractBuilder>{
        protected var rI = ValidatedIngredient(Ingredient.empty())
        protected var e = ValidatedInt(1,50,0)
        protected var pA = ValidatedSeries(arrayOf(1,1,1,1),Int::class.java) {a,b -> a >= 0 && b >= 0}
        protected var dM = ValidatedInt(1,100,0)
        protected var kR = ValidatedFloat(0f,0.25f,0f)
        protected var t = ValidatedFloat(0f,1f,0f)

        abstract fun builderClass(): U
        
        fun repairIngredient(ingredient: Ingredient): U{
            rI = ValidatedIngredient(ingredient)
            return builderClass()
        }
        fun enchantability(default: Int, max: Int = 50): U{
            e = ValidatedInt(default,max,1)
            return builderClass()
        }
        fun protectionAmounts(helmet: Int, chestplate: Int, leggings: Int, boots: Int): U{
            pA = ValidatedSeries(arrayOf(boots,leggings,chestplate,helmet),Int::class.java) {a,b -> a >= 0 && b >= 0}
            return builderClass()
        }
        fun durabilityMultiplier(default: Int, max: Float = 100f): U{
            dM = ValidatedInt(default, max, 1)
            return builderClass()
        }
        fun knockbackReistance(default: Float): U{
            kR = ValidatedFloat(default,0.25f,0f)
            return builderClass()
        }
        fun toughness(default: Float, max: Float = 5f): U{
            t = ValidatedFloat(default,max,0f)
            return builderClass()
        }
        
        abstract fun build(): T
    }
}
