package me.fzzyhmstrs.fzzy_config.validated_field

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import me.fzzyhmstrs.fzzy_config.config_util.ValidationResult
import me.fzzyhmstrs.fzzy_config.interfaces.ConfigSerializable
import me.fzzyhmstrs.fzzy_config.validated_field.list.ValidatedSeries
import me.fzzyhmstrs.fzzy_config.validated_field.list.ValidatedStringList
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ArmorMaterial
import net.minecraft.item.ToolMaterial
import net.minecraft.recipe.Ingredient
import net.minecraft.sound.SoundEvent
import java.util.function.BiPredicate

open class ValidatedArmorMaterial protected constructor(
    protected val armorName: String,
    protected val armorSoundEvent: SoundEvent,
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
        return armorName
    }
    override fun getEquipSound(): SoundEvent{
        return armorSoundEvent
    }
    override fun getRepairIngredient(): Ingredient?{
        return repairIngredient.get()
    }
    override fun getEnchantability(): Int{
        return enchantability.get()
    }
    override fun getProtectionAmount(slot: EquipmentSlot): Int{
        return protectionAmounts.get()[slot.entitySlotId]
    }
    override fun getDurability(slot: EquipmentSlot): Int{
        return BASE_DURABILITY[slot.entitySlotId] * durabilityMultiplier.get()
    }
    override fun getKnockbackResistance(): Float{
        return knockbackResistance.get()
    }
    override fun getToughness(): Float{
        return toughness.get()
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

    class Builder(name: String, soundEvent: SoundEvent): AbstractBuilder<ValidatedArmorMaterial, Builder>(name, soundEvent){
        override fun builderClass(): Builder{
            return this
        }
        override fun build(): ValidatedArmorMaterial {
            return ValidatedArmorMaterial(name, soundEvent, rI, e, pA, dM, kR, t)
        }
    }
    
    abstract class AbstractBuilder<T: ArmorMaterial, U: AbstractBuilder<T,U>>(protected val name: String, protected val soundEvent: SoundEvent){
        protected var rI = ValidatedIngredient(Ingredient.empty())
        protected var e = ValidatedInt(1,50,0)
        protected var pA = ValidatedSeries(arrayOf(1,1,1,1),Int::class.java, {a,b -> a >= 0 && b >= 0})
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
            pA = ValidatedSeries(arrayOf(boots,leggings,chestplate,helmet),Int::class.java, {a,b -> a >= 0 && b >= 0})
            return builderClass()
        }
        fun durabilityMultiplier(default: Int, max: Int = 100): U{
            dM = ValidatedInt(default, max, 1)
            return builderClass()
        }
        fun knockbackResistance(default: Float): U{
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
