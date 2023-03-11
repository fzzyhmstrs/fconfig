package me.fzzyhmstrs.fzzy_config.validated_field

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.fzzyhmstrs.fzzy_config.config_util.ValidationResult
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

/**
 * A validated [Identifier] value
 *
 * Identifiers are serialized as plain strings in the format "namespace:path". Validation happens both on deserialization, where checking for a valid identifier string is performed, and also during [validateAndCorrectInputs], where the validIds are checked for a match
 *
 * @param validIds Collection<Identifier>, optional. A collection of valid identifiers the user can select from. If left empty any validly formatted identifier will pass validation.
 * @param invalidIdMessage String, optional. Provide a message detailing the criteria the user needs to follow in the case they make a mistake. For example, "Needs to be a registered item identifier." would be a good invalidIdMessage for validIds limited to `Registries.ITEM`. For reference, all the identifiers in a minecraft Registry can be acquired by calling `getIds()`.
 */
class ValidatedIdentifier(
    defaultValue: Identifier,
    private val validIds: Collection<Identifier> = listOf(),
    private val invalidIdMessage: String = "None")
  : 
  ValidatedField<Identifier>(defaultValue) 
{

    init{
        if (!validIds.contains(defaultValue) && !validIds.isEmpty()){
            throw IllegalArgumentException("Default identifier [$defaultValue] not valid per defined idValidator in validated identifier [${this.javaClass.canonicalName}] in config class [${this.javaClass.enclosingClass?.canonicalName}]")
        }
    }

    override fun deserializeHeldValue(json: JsonElement, fieldName: String): ValidationResult<Identifier> {
        return  try {
            val string = gson.fromJson(json, String::class.java)
            val id = Identifier.tryParse(string)
            if (id == null){
                ValidationResult.error(storedValue,"Identifier $id couldn't be parsed, resorting to fallback.")
            } else {
                ValidationResult.success(id)
            }
        } catch (e: Exception){
            ValidationResult.error(storedValue,"json [$json] at key $fieldName is not a properly formatted string")
        }
    }

    override fun serializeHeldValue(): JsonElement {
        return JsonPrimitive(storedValue.toString())
    }

    override fun validateAndCorrectInputs(input: Identifier): ValidationResult<Identifier> {
        if (!validIds.contains(input) && !validIds.isEmpty()) {
            val errorMessage = "Config Identifier $input couldn't be validated. Needs to meet the following criteria: $invalidIdMessage"
            return ValidationResult.error(storedValue,errorMessage)
        }
        return ValidationResult.success(input)
    }

    override fun readmeText(): String{
        return "Identifier stored as a string that needs to meet the following criteria: $invalidIdMessage"
    }

    override fun toBuf(buf: PacketByteBuf) {
        buf.writeIdentifier(storedValue)
    }

    override fun fromBuf(buf: PacketByteBuf): Identifier {
        return buf.readIdentifier()
    }
}
