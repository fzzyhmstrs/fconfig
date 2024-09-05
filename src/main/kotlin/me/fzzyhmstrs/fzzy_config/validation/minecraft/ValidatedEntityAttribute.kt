/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.validation.minecraft

import com.google.common.base.Supplier
import com.google.common.collect.Multimap
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryHandler
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.SuppliedTextWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.text
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.TomlOps
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedEntityAttribute.Builder
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.*
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.*
import java.util.function.Consumer

/**
 * A validated Pairing of an [EntityAttribute] and a corresponding [EntityAttributeModifier]
 *
 * ValidatedEntityAttribute has a private constructor, use [Builder] to create one
 * @sample me.fzzyhmstrs.fzzy_config.examples.MinecraftExamples.entityAttributes
 * @see Builder
 * @author fzzyhmstrs
 * @since 0.3.1
 */
@Suppress("MemberVisibilityCanBePrivate")
open class ValidatedEntityAttribute private constructor(attributeId: Identifier, private val lockAttribute: Boolean, uuid: UUID, name: String, amount: Double, operation: Operation, private val lockOperation: Boolean, private val amountValidator: Entry<Double, *> = ValidatedDouble(amount)): ValidatedField<ValidatedEntityAttribute.EntityAttributeInstanceHolder>(
    EntityAttributeInstanceHolder(attributeId, uuid, name, amount, operation)
) {

    /**
     * adds the stored Attribute and its modifier to a passed modifier map. This is useful when dealing with methods such as [ItemStack.getAttributeModifiers]
     * @param map [Multimap]&lt;[EntityAttribute], [EntityAttributeModifier]&gt; - the map to add the modifier pair to
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    fun addToMap(map: Multimap<EntityAttribute, EntityAttributeModifier>) {
        storedValue.addToMap(map)
    }

    /**
     * builds and returns the [EntityAttributeModifier] associated with this validation
     * @return [EntityAttributeModifier] - built from the values in the stored holder
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    fun getModifier(): EntityAttributeModifier {
        return storedValue.createModifier()
    }

    /**
     * updates this validation with new EntityAttributeModifier values
     *
     * If the operation is locked, it will not update. If the amount is out of bounds, it will autocorrect to the nearest bound
     * @param new [EntityAttributeModifier] - the new modifier instance to update this validation's underlying holder with
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    fun updateModifier(new: EntityAttributeModifier) {
        validateAndSet(storedValue.copy(uuid = new.id, name = new.toNbt().getString("Name"), amount = amountValidator.correctEntry(new.value, EntryValidator.ValidationType.STRONG).get(), operation = if(lockOperation) storedValue.operation else new.operation))
    }
    /**
     * updates this validation with a new double value
     *
     * If the amount is out of bounds, it will autocorrect to the nearest bound
     * @param newAmount Double - the new modifier amount to update this validation's underlying holder with
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    fun updateModifierAmount(newAmount: Double) {
        validateAndSet(storedValue.copy(amount = amountValidator.correctEntry(newAmount, EntryValidator.ValidationType.STRONG).get()))
    }

    /**
     * gets the underlying [EntityAttributeInstanceHolder]. Consider another method of interaction with this validation before a raw get() call.
     * @return [EntityAttributeInstanceHolder] - this validation's underlying attribute holder
     * @see getModifier
     * @see updateModifier
     * @see updateModifierAmount
     * @see addToMap
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Returns an EntityAttributeInstanceHolder instance. You probably don't want to directly interact with that.")
    override fun get(): EntityAttributeInstanceHolder {
        return super.get()
    }
    /**
     * sets the underlying [EntityAttributeInstanceHolder]. Consider another method of interaction with this validation before a raw accept() call.
     * @param input [EntityAttributeInstanceHolder] - the new Holder instance
     * @see getModifier
     * @see updateModifier
     * @see updateModifierAmount
     * @see addToMap
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Accepts an EntityAttributeInstanceHolder instance. You probably don't want to directly interact with that.")
    override fun accept(input: EntityAttributeInstanceHolder) {
        super.accept(input)
    }

    /**
     * Creates a deep copy of the stored value and returns it
     * @return EntityAttributeInstanceHolder - deep copy of the currently stored value. Consider using another method before a raw copyStoredValue() call
     * @see getModifier
     * @see updateModifier
     * @see updateModifierAmount
     * @see addToMap
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Copies an EntityAttributeInstanceHolder instance. You probably don't want to directly interact with that.")
    override fun copyStoredValue(): EntityAttributeInstanceHolder {
        return storedValue.copy()
    }

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<EntityAttributeInstanceHolder> {
        return storedValue.deserializeEntry(toml, mutableListOf(), fieldName, ConfigApiImpl.IGNORE_NON_SYNC)
    }
    @Internal
    override fun serialize(input: EntityAttributeInstanceHolder): ValidationResult<TomlElement> {
        return ValidationResult.success(storedValue.serializeEntry(input, mutableListOf(), ConfigApiImpl.IGNORE_NON_SYNC))
    }
    /**
     * creates a deep copy of this ValidatedEntityAttribute
     * @return ValidatedEntityAttribute wrapping a deep copy of the currently stored values and validation
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    override fun instanceEntry(): ValidatedField<EntityAttributeInstanceHolder> {
        return ValidatedEntityAttribute(storedValue.attributeId, lockAttribute, storedValue.uuid, storedValue.name, storedValue.amount, storedValue.operation, lockOperation, amountValidator)
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is EntityAttributeInstanceHolder && storedValue.validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }
    @Internal
    override fun toString(): String {
        return "Validated Entity Attribute[attribute=${storedValue.attributeId}, attribute_locked=$lockAttribute, modifier=$${getModifier()}, operation_locked=$lockOperation, validation=$amountValidator]"
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<EntityAttributeInstanceHolder>): ClickableWidget {
        return EntityAttributeButtonWidget(this, { this.getButtonText(storedValue.attributeId, storedValue.amount, storedValue.operation) }, { openEntityAttributePopup() })
    }
    //client
    private fun openEntityAttributePopup() {
        val attribute = Registries.ATTRIBUTE.get(storedValue.attributeId)
        val operationValidator = ValidatedEnum(storedValue.operation, ValidatedEnum.WidgetType.CYCLING)
        amountValidator.accept(storedValue.amount)
        val popup = PopupWidget.Builder(translation())
            .addDivider()
            .addElement("text", SuppliedTextWidget({ getButtonText(storedValue.validator().get(), amountValidator.get(), operationValidator.get()) }, MinecraftClient.getInstance().textRenderer).alignCenter(), Position.BELOW, Position.ALIGN_JUSTIFY)
            .addDivider()
            .addElement("attribute", if(lockAttribute) TextWidget(0, 0, 110, 20, attribute?.translationKey?.translate() ?: storedValue.attributeId.text(), MinecraftClient.getInstance().textRenderer).alignCenter() else storedValue.validator().widgetEntry(), Position.BELOW, Position.ALIGN_JUSTIFY)
            .addElement("amount", amountValidator.widgetEntry(), Position.BELOW, Position.ALIGN_JUSTIFY)
            .addElement("operation", if(lockOperation) ButtonWidget.builder(storedValue.operation.name.lit()) { _ -> }.size(110, 20).build().also { it.active = false } else operationValidator.widgetEntry(), Position.BELOW, Position.ALIGN_JUSTIFY)
            .addDoneButton()
            .onClose{ this.setAndUpdate(storedValue.copy(
                attributeId =  if(lockAttribute) storedValue.attributeId else storedValue.validator().get(),
                amount = amountValidator.get(),
                operation = if(lockOperation) storedValue.operation else operationValidator.get()
            )) }
            .build()
        PopupWidget.push(popup)
    }
    //client
    private fun getButtonText(inputAttribute: Identifier, inputAmount: Double, inputOperation: Operation): MutableText {
        val attribute = Registries.ATTRIBUTE.get(inputAttribute) ?: return "fc.validated_field.entity_attribute.error".translate()
        val amount = when(inputOperation) {
            Operation.ADDITION -> if (attribute == EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE) inputAmount * 10.0 else inputAmount
            Operation.MULTIPLY_BASE -> inputAmount * 100.0
            Operation.MULTIPLY_TOTAL -> inputAmount * 100.0
        }
        return if (inputAmount > 0.0) {
            FcText.translatable("attribute.modifier.plus.${inputOperation.id}", ItemStack.MODIFIER_FORMAT.format(amount), FcText.translatable(attribute.translationKey)).formatted(Formatting.DARK_GREEN)
        } else {
            FcText.translatable("attribute.modifier.take.${inputOperation.id}", ItemStack.MODIFIER_FORMAT.format(-amount), FcText.translatable(attribute.translationKey)).formatted(Formatting.RED)
        }
    }

    //client
    private class EntityAttributeButtonWidget(private val entry: ValidatedEntityAttribute, private val textSupplier: Supplier<MutableText>, private val onPress: Consumer<ClickableWidget>): PressableWidget(0, 0, 110, 20, textSupplier.get()) {

        override fun getMessage(): Text {
            return textSupplier.get()
        }
        override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
            appendDefaultNarrations(builder)
        }
        override fun onPress() {
            onPress.accept(this)
        }

    }

    /**
     * A ValidatedEntityAttribute builder
     * @param attributeId Identifier - the registry ID for the default [EntityAttribute]. This can't be the attribute itself, as it will be serialized, potentially before the registry is built.
     * @param lockAttribute Boolean - If true, the user won't be able to change the stored Attribute value
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    class Builder @JvmOverloads constructor(private val attributeId: Identifier, private val lockAttribute: Boolean = false) {

        @JvmOverloads
        constructor(attributeId: String, lockAttribute: Boolean = false): this(Identifier(attributeId), lockAttribute)

        private var uuid: UUID? = null
        private var name: String = attributeId.toTranslationKey()
        private var amount: ValidatedDouble = ValidatedDouble(0.0)
        private var operation: Operation = Operation.ADDITION
        private var lockOperation = false

        /**
         * Define a UUID for the attribute modifier using a string resprentation of a UUID
         * @param uuid String - String representation of a UUID.
         * @return [Builder] this builder
         * @author fzzyhmstrs
         * @since 0.3.1
         */
        fun uuid(uuid: String): Builder {
            this.uuid = UUID.fromString(uuid)
            return this
        }
        /**
         * Define a UUID for the attribute modifier
         * @param uuid UUID
         * @return [Builder] this builder
         * @author fzzyhmstrs
         * @since 0.3.1
         */
        fun uuid(uuid: UUID): Builder {
            this.uuid = uuid
            return this
        }
        /**
         * Define a name for the attribute modifier
         * @param uuid String - String representation of a UUID.
         * @return [Builder] this builder
         * @author fzzyhmstrs
         * @since 0.3.1
         */
        fun name(name: String): Builder {
            this.name = name
            return this
        }
        @JvmOverloads
        fun amount(amount: Double, min: Double = -Double.MAX_VALUE, max: Double = Double.MAX_VALUE): Builder {
            this.amount = ValidatedDouble(amount, max, min)
            return this
        }
        fun operation(operation: Operation, lockOperation: Boolean = false): Builder {
            this.operation = operation
            this.lockOperation = lockOperation
            return this
        }

        fun build(): ValidatedEntityAttribute {
            return ValidatedEntityAttribute(attributeId, lockAttribute, uuid?:UUID.nameUUIDFromBytes("$name+${operation.name}".toByteArray()), name, amount.get(), operation, lockOperation, amount)
        }
    }

    /**
     * A holder of entity attribute and modifier information. This is a class internal to [ValidatedEntityAttribute], made public by necessity. In most cases, it's not correct to be directly interacting with this.
     *
     * It is an [EntryHandler] for its own type
     * @param attributeId Identifier - the registry id of this holders entity attribute
     * @param uuid [UUID] - uuid of this holders attribute modifier
     * @param name String - name of this holders attribute modifier
     * @param amount Double - value of this holders attribute modifier
     * @param operation [Operation] - modifier operation of this holders attribute modifier
     * @author fzzyhmstrs
     * @since 0.3.1
     */
    data class EntityAttributeInstanceHolder(val attributeId: Identifier, val uuid: UUID, val name: String, val amount: Double, val operation: Operation): EntryHandler<EntityAttributeInstanceHolder> {

        private val idValidator = ValidatedIdentifier.ofRegistry(attributeId, Registries.ATTRIBUTE)
        @Internal
        internal fun validator(): ValidatedIdentifier {
            return idValidator
        }
        @Internal
        internal fun addToMap(map: Multimap<EntityAttribute, EntityAttributeModifier>) {
                Registries.ATTRIBUTE.get(attributeId)?.let { map.put(it, EntityAttributeModifier(uuid, name, amount, operation)) }
        }
        @Internal
        internal fun createModifier(): EntityAttributeModifier {
            return EntityAttributeModifier(uuid, name, amount, operation)
        }

        @Internal
        override fun serializeEntry(
            input: EntityAttributeInstanceHolder?,
            errorBuilder: MutableList<String>,
            flags: Byte
        ): TomlElement {
            val table = TomlTableBuilder(2)
            val instance = input ?: this
            table.element("id", idValidator.serializeEntry(instance.attributeId, errorBuilder, flags))
            val toml = NbtCompound.CODEC.encodeStart(TomlOps.INSTANCE, instance.createModifier().toNbt())
            toml.result().ifPresentOrElse({ table.element("modifier", it) }, {table.element("modifier", TomlNull)})
            return table.build()
        }
        @Internal
        override fun deserializeEntry(
            toml: TomlElement,
            errorBuilder: MutableList<String>,
            fieldName: String,
            flags: Byte
        ): ValidationResult<EntityAttributeInstanceHolder> {
            val table = try {
                toml.asTomlTable()
            } catch (e: Exception) {
                return ValidationResult.error(this, "Error deserializing EntityAttributeInstance [$fieldName], TomlElement not a TomlTable")
            }
            val id = table["id"]?.let { idValidator.deserializeEntry(it, errorBuilder, fieldName, flags).takeIf { result -> result.isValid() }?.get() ?: return ValidationResult.error(this, "Error deserializing EntityAttributeInstance [$fieldName], invalid identifier")} ?: return ValidationResult.error(this, "Error deserializing EntityAttributeInstance [$fieldName], key 'id' is missing")
            val modifierElement = table["modifier"] ?: return ValidationResult.error(this, "Error deserializing EntityAttributeInstance [$fieldName], key 'modifier' is missing")
            val modifierResult = NbtCompound.CODEC.parse(TomlOps.INSTANCE, modifierElement)
            var finalResult = ValidationResult.error(this, "Error deserializing EntityAttributeInstance [$fieldName]: error deserializing modifier")
            modifierResult.result().ifPresent {
                EntityAttributeModifier.fromNbt(it)?.let { mod ->
                    finalResult = ValidationResult.success(
                        EntityAttributeInstanceHolder(
                            id,
                            this.uuid, //uuid is immutable
                            this.name, //name is immutable
                            mod.value,
                            mod.operation
                        )
                    )
                }
            }
            return finalResult
        }
        @Internal
        override fun correctEntry(
            input: EntityAttributeInstanceHolder,
            type: EntryValidator.ValidationType
        ): ValidationResult<EntityAttributeInstanceHolder> {
            return if (type == EntryValidator.ValidationType.STRONG) {
                val attribute = Registries.ATTRIBUTE.get(attributeId) ?: return ValidationResult.error(input.copy(attributeId = this.attributeId), "Attrbiute ID [${input.attributeId}] not found in Attributes Registry. Falling back to [$attributeId]")
                if (attribute.clamp(input.amount) != input.amount) { ValidationResult.error(input.copy(amount = attribute.clamp(input.amount)), "Attribute amount [${input.amount}] out of bounds of valid range for attribute [$attribute]")
                } else {
                    ValidationResult.success(input)
                }
            } else {
                ValidationResult.success(input)
            }
        }
        @Internal
        override fun validateEntry(
            input: EntityAttributeInstanceHolder,
            type: EntryValidator.ValidationType
        ): ValidationResult<EntityAttributeInstanceHolder> {
            return if (type == EntryValidator.ValidationType.STRONG) {
                val attribute = Registries.ATTRIBUTE.get(attributeId) ?: return ValidationResult.error(input, "Attrbiute ID [${input.attributeId}] not found in Attributes Registry. Falling back to [$attributeId]")
                ValidationResult.predicated(input, attribute.clamp(input.amount) == input.amount, "Attribute amount [${input.amount}] out of bounds of valid range for attribute [$attribute]")
            } else {
                ValidationResult.success(input)
            }
        }
    }
}