/*
package me.fzzyhmstrs.fzzy_config.validated_field

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field_v2.ValidatedField
import net.minecraft.network.PacketByteBuf
import java.util.function.Predicate

*/
/**
 * A validated general String. Not recommended for use with identifier or enum strings.
 *
 * Holds any generic string value. Validation of the string value is handled manually by passing a string validator predicate into this class.
 *
 * @param defaultValue String. The default string value.
 * @param strValidator Predicate<String>, optional. If empty, any json-valid string will pass validation. Otherwise the predicate will be `tested`, and an error passed if the predicate returns false.
 * @param invalidIdMessage String, optional. Provide a message detailing the criteria the user needs to follow in the case they make a mistake.
 *
 * @see ValidatedEnum
 * @see ValidatedIdentifier
 *//*

class ValidatedString(
  defaultValue: String,
  private val strValidator: Predicate<String> = Predicate {true},
  private val invalidIdMessage: String = "None")
  :
  ValidatedField<String>(defaultValue)
{

    init{
        if (!strValidator.test(defaultValue)){
            throw IllegalArgumentException("Default string [$defaultValue] not valid per defined strValidator, in [${this.javaClass.canonicalName}] in config class [${this.javaClass.enclosingClass?.canonicalName}]")
        }
    }

    override fun deserializeHeldValue(json: JsonElement, fieldName: String): ValidationResult<String> {
        return  try {
            ValidationResult.success(gson.fromJson(json, String::class.java))
        } catch (e: Exception){
            ValidationResult.error(storedValue,"json [$json] at key $fieldName is not a properly formatted string")
        }
    }

    override fun serializeHeldValue(): JsonElement {
        return JsonPrimitive(storedValue)
    }

    override fun validateAndCorrectInputs(input: String): ValidationResult<String> {
        if (!strValidator.test(input)) {
            val errorMessage = "Config string [$input] couldn't be validated. Needs to meet the following criteria: $invalidIdMessage"
            return ValidationResult.error(storedValue,errorMessage)
        }
        return ValidationResult.success(input)
    }

    override fun readmeText(): String{
        return "String value that needs to meet the following criteria: $invalidIdMessage"
    }

    override fun toBuf(buf: PacketByteBuf) {
        buf.writeString(storedValue)
    }

    override fun fromBuf(buf: PacketByteBuf): String {
        return buf.readString()
    }
}*/