package me.fzzyhmstrs.fzzy_config.validated_field

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.fzzyhmstrs.fzzy_config.config_util.ValidationResult
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

/**
 * A validated [Ingredient] value
 *
 * Ingredients are serilaized in the standard format for MineCraft ingredients as seen in eg. Recipe JSONs. Validation is only done on deserialization.
 */
class ValidatedIdentifier(defaultValue: Ingredient)
  : 
  ValidatedField<Ingredient>(defaultValue) 
{

    override fun deserializeHeldValue(json: JsonElement, fieldName: String): ValidationResult<Ingredient> {
        return  try {
            val id = Ingredient.fromJson(json)
            ValidationResult.success(id)
        } catch (e: Exception){
            ValidationResult.error(storedValue,"json [$json] at key $fieldName is not a properly formatted Ingredient")
        }
    }

    override fun serializeHeldValue(): JsonElement {
        return storedValue.toJson()
    }

    override fun validateAndCorrectInputs(input: Ingredient): ValidationResult<Ingredient> {
        return ValidationResult.success(input)
    }

    override fun readmeText(): String{
        return "Minecraft Ingredient JSON. Needs to be a valid ingredient like one would use in a recipe JSON."
    }

    override fun toBuf(buf: PacketByteBuf) {
        storedValue.write(buf)
    }

    override fun fromBuf(buf: PacketByteBuf): Ingredient {
        return Ingredient.fromPacket(buf)
    }
}
