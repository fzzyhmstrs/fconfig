package me.fzzyhmstrs.fzzy_config.validated_field

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.fzzyhmstrs.fzzy_config.config_util.ValidationResult
import net.minecraft.network.PacketByteBuf

/**
 * A validated boolean value.
 *
 * Since booleans typically don't need much actual input validation as there are only two possible values, the majority of validation is actually JSON Syntax validation. If there is a syntax or other parsing error (for example, a player accidentally enters an integer instead of true or false), the validation will pass back the stored/default value.
 *
 * @param defaultValue Boolean. The default setting for this validated boolean.
 */
class ValidatedBoolean(defaultValue: Boolean): ValidatedField<Boolean>(defaultValue){

    override fun deserializeHeldValue(json: JsonElement, fieldName: String): ValidationResult<Boolean> {
        if (!json.isJsonPrimitive) return ValidationResult.error(storedValue,"Boolean value $fieldName couldn't be deserialized. Make sure it is 'true' or 'false'.")
        val str = json.asString
        if (!("true".equals(str,true)) && !("false".equals(str,true)))  return ValidationResult.error(storedValue,"Boolean value $fieldName couldn't be deserialized. Make sure it is 'true' or 'false'.")
        val bl = json.asBoolean
        return ValidationResult.success(bl)
    }

    override fun serializeHeldValue(): JsonElement {
        return JsonPrimitive(storedValue)
    }

    override fun validateAndCorrectInputs(input: Boolean): ValidationResult<Boolean> {
        return ValidationResult.success(input)
    }

    override fun readmeText(): String {
        return "Boolean value, enter 'true' or 'false'."
    }

    override fun toBuf(buf: PacketByteBuf) {
        buf.writeBoolean(storedValue)
    }

    override fun fromBuf(buf: PacketByteBuf): Boolean {
        return buf.readBoolean()
    }
}