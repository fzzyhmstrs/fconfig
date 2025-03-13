/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.minecraft

import me.fzzyhmstrs.fzzy_config.entry.EntryOpener
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.SuppliedTextWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.PortingUtils
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIngredient.IngredientProvider
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.*
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BiFunction
import java.util.function.Predicate

/**
 * A validated provider of [Ingredient]
 *
 * This does not store an ingredient, it stores an [IngredientProvider], which lazily generates the ingredient only when requested.
 * @sample me.fzzyhmstrs.fzzy_config.examples.MinecraftExamples.ingredients
 * @author fzzyhmstrs
 * @since 0.2.0, marked final 0.6.0
 */
class ValidatedIngredient private constructor(defaultValue: IngredientProvider, private val itemPredicate: Predicate<Identifier>? = null, private val tagPredicate: Predicate<Identifier>? = null): ValidatedField<IngredientProvider>(defaultValue), EntryOpener {

    /**
     * A validated provider of [Ingredient]
     *
     * This does not store an ingredient, it stores an [IngredientProvider], which lazily generates the ingredient only when requested.
     *
     * Initializes this validation with a single-item IngredientProvider
     * @param item [Identifier] defining the item
     * @param itemPredicate [Predicate]<Identifier>, optional - restricts the set of allowable items (default is any item in the Items registry)
     * @param tagPredicate [Predicate]<Identifier>, optional - restricts the set of allowable tags (default is any tag in the Items registry)
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(item: Identifier, itemPredicate: Predicate<Identifier>? = null, tagPredicate: Predicate<Identifier>? = null): this(ItemProvider(item), itemPredicate, tagPredicate)

    /**
     * A validated provider of [Ingredient]
     *
     * This does not store an ingredient, it stores an [IngredientProvider], which lazily generates the ingredient only when requested.
     *
     * Initializes this validation with a multi-item IngredientProvider
     * @param set [Set]<Any> - Set defining the default tags and items. Anything that isn't an Identifier or Tagkey&lt;Item&gt; will be ignored
     * @param itemPredicate [Predicate]<Identifier>, optional - restricts the set of allowable items (default is any item in the Items registry)
     * @param tagPredicate [Predicate]<Identifier>, optional - restricts the set of allowable tags (default is any tag in the Items registry)
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(set: Set<Any>, itemPredicate: Predicate<Identifier>? = null, tagPredicate: Predicate<Identifier>? = null): this(ListProvider(set), itemPredicate, tagPredicate)

    /**
     * A validated provider of [Ingredient]
     *
     * This does not store an ingredient, it stores an [IngredientProvider], which lazily generates the ingredient only when requested.
     *
     * Initializes this validation with a multi-item IngredientProvider
     * @param tag [TagKey]<Item> defining the tag to pull items from
     * @param itemPredicate [Predicate]<Identifier>, optional - restricts the set of allowable tags (default is any tag from the Items registry)
     * @param tagPredicate [Predicate]<Identifier>, optional - restricts the set of allowable tags (default is any tag in the Items registry)
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(tag: TagKey<Item>, itemPredicate: Predicate<Identifier>? = null, tagPredicate: Predicate<Identifier>? = null): this(
        TagProvider(tag.id), itemPredicate, tagPredicate)


    private val tagValidator = if(tagPredicate == null) ValidatedIdentifier.ofRegistryTags(RegistryKeys.ITEM) else ValidatedIdentifier.ofRegistryTags(RegistryKeys.ITEM, tagPredicate)
    @Suppress("DEPRECATION")
    private val itemValidator = if(itemPredicate == null) ValidatedIdentifier.ofRegistry(Registries.ITEM) else ValidatedIdentifier.ofRegistry(Registries.ITEM) { id, _ -> itemPredicate.test(id) }
    private val listTagValidator = tagValidator.toSet()
    private val listItemValidator = itemValidator.toSet()


    init {
        when(storedValue.type()) {
            ProviderType.STACK -> {
                listItemValidator.validateAndSet(setOf((storedValue as ItemProvider).id))
                listTagValidator.validateAndSet(setOf())
            }
            ProviderType.LIST -> {
                listItemValidator.validateAndSet((storedValue as ListProvider).ids)
                listTagValidator.validateAndSet((storedValue as ListProvider).tags)
            }
            ProviderType.TAG -> {
                listItemValidator.validateAndSet(setOf())
                listTagValidator.validateAndSet(setOf((storedValue as TagProvider).tag))
            }
        }
    }

    /**
     * Supplies the [Ingredient] from this ValidatedIngredients Provider
     * @return [Ingredient] generated from the current [IngredientProvider]
     * @throws UnsupportedOperationException - if the ingredient can't be correctly created, [Ingredient] now throws an exception, and no longer allows for empty ingredients.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("24w34a+: Ingredient creation can now throw UnsupportedOperationException, be sure to handle.")
    fun toIngredient(): Ingredient {
        return storedValue.provide()
    }

    /**
     * Updates this ValidatedIngredient with a new single-item Ingredient
     * @param identifier [Identifier] - the id of the new single item to wrap in this ingredient
     * @return This validated ingredient after validating and updating with the new input
     * @author fzzyhmstrs
     * @since 0.3.2
     */
    fun validateAndSet(identifier: Identifier): ValidatedIngredient {
        val provider = ItemProvider(identifier)
        validateAndSet(provider)
        return this
    }

    /**
     * Updates this ValidatedIngredient with a new multi-item Ingredient
     * @param identifiers [List]&lt;[Any]&gt; - List of tags and ids. Anything that isn't an Identifier or Tagkey&lt;Item&gt; will be ignored
     * @return This validated ingredient after validating and updating with the new input
     * @author fzzyhmstrs
     * @since 0.3.3
     */
    fun validateAndSet(identifiers: Set<Any>): ValidatedIngredient {
        val provider = ListProvider(identifiers)
        validateAndSet(provider)
        return this
    }

    /**
     * Updates this ValidatedIngredient with a new tag Ingredient
     * @param tag [TagKey] - the tagkey of the new item tag to wrap in this ingredient
     * @return This validated ingredient after validating and updating with the new input
     * @author fzzyhmstrs
     * @since 0.3.2
     */
    fun validateAndSet(tag: TagKey<Item>): ValidatedIngredient {
        val provider = TagProvider(tag.id)
        validateAndSet(provider)
        return this
    }

    @Internal
    @Deprecated("use deserialize to avoid accidentally overwriting validation and error reporting")
    override fun deserializeEntry(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        flags: Byte
    ): ValidationResult<IngredientProvider> {
        @Suppress("DEPRECATION")
        val result = super.deserializeEntry(toml, errorBuilder, fieldName, flags)
        when(storedValue.type()) {
            ProviderType.STACK -> {
                listItemValidator.validateAndSet(setOf((storedValue as ItemProvider).id))
                listTagValidator.validateAndSet(setOf())
            }
            ProviderType.LIST -> {
                listItemValidator.validateAndSet((storedValue as ListProvider).ids)
                listTagValidator.validateAndSet((storedValue as ListProvider).tags)
            }
            ProviderType.TAG -> {
                listItemValidator.validateAndSet(setOf())
                listTagValidator.validateAndSet(setOf((storedValue as TagProvider).tag))
            }
        }
        return result
    }

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<IngredientProvider> {
        return try {
            ValidationResult.success(IngredientProvider.deserialize(toml, fieldName))
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error while deserializing ValidatedIngredient [$fieldName]: ${e.localizedMessage}")
        }
    }

    @Internal
    override fun serialize(input: IngredientProvider): ValidationResult<TomlElement> {
        return ValidationResult.success(IngredientProvider.serialize(input))
    }

    /**
     * creates a deep copy of this ValidatedIngredient
     * return ValidatedIngredient wrapping a deep copy of the currently stored ingredient provider as well as predicates, if any
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedField<IngredientProvider> {
        return ValidatedIngredient(copyStoredValue(), itemPredicate, tagPredicate)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is IngredientProvider
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input Ingredient input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: IngredientProvider): IngredientProvider {
        return input.copy()
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<IngredientProvider>): ClickableWidget {
        return CustomButtonWidget.builder("fc.validated_field.ingredient.edit".translate()) { b ->
            openIngredientPopup(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 }, PopupWidget.Builder.popupContext { h -> b.y + b.height/2 - h/2 })
        }.size(110, 20).build()
    }

    override fun open(args: List<String>) {
        openIngredientPopup()
    }

    @Internal
    override fun entryDeco(): Decorated.DecoratedOffset {
        return Decorated.DecoratedOffset(TextureDeco.DECO_INGREDIENT, 2, 2)
    }

    //client
    private fun openIngredientPopup(xPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center(), yPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center()) {
        val textRenderer = MinecraftClient.getInstance().textRenderer

        val popupNew = PopupWidget.Builder(translation())
            .addDivider()
            .add("items_label", TextWidget(110, 13, "fc.validated_field.ingredient.items".translate(), textRenderer).alignLeft(), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("items", listItemValidator.widgetAndTooltipEntry(ChoiceValidator.any()), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("items_clear", CustomButtonWidget.builder("fc.validated_field.ingredient.clear".translate()){ _ -> listItemValidator.validateAndSet(setOf()) }.size(60, 20).build(), "items", LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .add("items_textbox", SuppliedTextWidget({ listItemValidator.get().toString().lit().formatted(Formatting.GRAY) }, textRenderer, 110, 20).supplyTooltipOnOverflow { listItemValidator.get().joinToString("\n").lit() }, "items", LayoutWidget.Position.ALIGN_JUSTIFY, LayoutWidget.Position.BELOW)
            .addDivider()
            .add("tags_label", TextWidget(110, 13, "fc.validated_field.ingredient.tags".translate(), textRenderer).alignLeft(), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("tags", listTagValidator.widgetAndTooltipEntry(ChoiceValidator.any()), LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
            .add("tags_clear", CustomButtonWidget.builder("fc.validated_field.ingredient.clear".translate()){ _ -> listTagValidator.validateAndSet(setOf()) }.size(60, 20).build(), "tags", LayoutWidget.Position.RIGHT, LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            .add("tags_textbox", SuppliedTextWidget({ listTagValidator.get().toString().lit().formatted(Formatting.GRAY) }, textRenderer, 110, 20).supplyTooltipOnOverflow { listTagValidator.get().joinToString("\n").lit() }, "tags", LayoutWidget.Position.ALIGN_JUSTIFY, LayoutWidget.Position.BELOW)
            .addDoneWidget()
            .positionX(xPosition)
            .positionY(yPosition)
            .onClose{ this.setAndUpdate(fromLists()) }
            .build()
        PopupWidget.push(popupNew)
    }

    //client
    private fun fromLists(): IngredientProvider {
        if (listItemValidator.isEmpty() && listTagValidator.isEmpty())
            return ListProvider(setOf(), setOf())
        return if (listItemValidator.size == 1 && listTagValidator.isEmpty()) {
            ItemProvider(listItemValidator.first())
        } else if (listItemValidator.isEmpty() && listTagValidator.size == 1) {
            TagProvider(listTagValidator.first())
        } else {
            ListProvider(listItemValidator.get(), listTagValidator.get())
        }
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Ingredient[value=$storedValue, validation={$itemValidator, $tagValidator, $listItemValidator, $listTagValidator}]"
    }

    enum class ProviderType: EnumTranslatable {
        STACK {
            override fun create(ingredient: ValidatedIngredient): IngredientProvider {
                return ItemProvider(ingredient.itemValidator.get())
            }
            override fun type(): String {
                return "stack"
            }
        },
        LIST {
            override fun create(ingredient: ValidatedIngredient): IngredientProvider {
                return ListProvider(ingredient.listItemValidator.get(), ingredient.listTagValidator.get())
            }
            override fun type(): String {
                return "stacks"
            }
        },
        TAG {
            override fun create(ingredient: ValidatedIngredient): IngredientProvider {
                return TagProvider(ingredient.tagValidator.get())
            }
            override fun type(): String {
                return  "tag"
            }
        };

        override fun prefix(): String {
            return "fc.validated_field.ingredient"
        }
        abstract fun create(ingredient: ValidatedIngredient): IngredientProvider
        abstract fun type(): String
    }

    class ItemProvider(val id: Identifier): IngredientProvider {
        override fun type(): ProviderType {
            return ProviderType.STACK
        }
        override fun provide(): Ingredient {
            val item = Registries.ITEM.get(id)
            return if (item == Items.AIR) {
                PortingUtils.emptyIngredient(id.toString())
            } else {
                PortingUtils.itemIngredient(item)
            }
        }
        override fun deserialize(toml: TomlElement): IngredientProvider {
            return ItemProvider(toml.asTomlLiteral().toString().simpleId())
        }
        override fun serialize(): TomlElement {
            return TomlLiteral(id.toString())
        }
        override fun copy(): IngredientProvider {
            return ItemProvider(id.toString().simpleId())
        }
        override fun toString(): String {
            return "Item Ingredient {$id}"
        }
        override fun equals(other: Any?): Boolean {
            if (other !is ItemProvider) return false
            return id == other.id
        }
        override fun hashCode(): Int {
            return id.hashCode()
        }
    }

    class ListProvider(val ids: Set<Identifier>, val tags: Set<Identifier>): IngredientProvider {

        constructor(input: Set<Any>):this(input.mapNotNull { it as? Identifier }.toSet(), input.mapNotNull { try { (it as? TagKey<*>)?.id } catch (e: Throwable){ null } }.toSet())

        override fun type(): ProviderType {
            return ProviderType.LIST
        }
        override fun provide(): Ingredient {
            if (ids.isEmpty() && tags.isEmpty())
                throw UnsupportedOperationException("Ingredients can't be empty; no item ids nor tags provided")
            val items = ids.map { Registries.ITEM.get(it) }.filter { it != Items.AIR }
            val tagItems: MutableList<Item> = mutableListOf()
            for (tag in tags) {
                Registries.ITEM.iterateEntries(TagKey.of(RegistryKeys.ITEM, tag)).forEach {
                    tagItems.add(it.value())
                }
            }
            return PortingUtils.listIngredient(items + tagItems)
        }
        override fun deserialize(toml: TomlElement): IngredientProvider {
            val array = toml.asTomlArray()
            val ids = array.mapNotNull {
                try {
                    val str = it.asTomlLiteral().toString()
                    if (str.startsWith("#")) {
                        null
                    } else {
                        str.simpleId()
                    }
                } catch (e: Throwable) {
                    null
                }
            }.toSet()
            val tags = array.mapNotNull {
                try {
                    val str = it.asTomlLiteral().toString()
                    if (str.startsWith("#")) {
                        str.substring(1).simpleId()
                    } else {
                        null
                    }
                } catch (e: Throwable) {
                    null
                }
            }.toSet()
            return ListProvider(ids, tags)
        }
        override fun serialize(): TomlElement {
            val toml = TomlArrayBuilder(ids.size)
            for (id in ids) {
                toml.element(TomlLiteral(id.toString()))
            }
            for (tag in tags) {
                toml.element(TomlLiteral("#$tag"))
            }
            return toml.build()
        }
        override fun copy(): IngredientProvider {
            return ListProvider(ids.toSet(), tags.toSet())
        }
        override fun toString(): String {
            return "List Ingredient {Items: $ids, Tags: $tags}"
        }
        override fun equals(other: Any?): Boolean {
            if (other !is ListProvider) return false
            return ids == other.ids && tags == other.tags
        }
        override fun hashCode(): Int {
            return ids.hashCode()
        }
    }

    class TagProvider(val tag: Identifier):IngredientProvider {
        override fun type(): ProviderType {
            return ProviderType.TAG
        }
        override fun provide(): Ingredient {
            return PortingUtils.tagIngredient(TagKey.of(RegistryKeys.ITEM, tag))
        }
        override fun deserialize(toml: TomlElement): IngredientProvider {
            return TagProvider(toml.asTomlLiteral().toString().replace("#", "").simpleId())
        }
        override fun serialize(): TomlElement {
            return TomlLiteral("#${tag}")
        }
        override fun copy(): IngredientProvider {
            return TagProvider(tag.toString().simpleId())
        }
        override fun toString(): String {
            return "Tag Ingredient {$tag}"
        }
        override fun equals(other: Any?): Boolean {
            if (other !is TagProvider) return false
            return tag == other.tag
        }
        override fun hashCode(): Int {
            return tag.hashCode()
        }
    }

    sealed interface IngredientProvider {
        companion object {
            private val STACK_INSTANCE = ItemProvider("dummy".simpleId())
            private val STACKS_INSTANCE = ListProvider(setOf())
            private val TAG_INSTANCE = TagProvider("dummy".simpleId())
            fun serialize(provider: IngredientProvider): TomlElement {
                val toml = TomlTableBuilder(2)
                toml.element("type", TomlLiteral(provider.type().type()))
                toml.element("value", provider.serialize())
                return toml.build()
            }
            fun deserialize(toml: TomlElement, fieldName: String): IngredientProvider {
                val table = toml.asTomlTable()
                val type = table["type"]?.asTomlLiteral()?.toString() ?: throw IllegalStateException("Unknown or missing type in IngredientProvider [$fieldName]")
                val value = table["value"] ?: throw IllegalStateException("Unknown or missing value in IngredientProvider [$fieldName]")
                return when(type) {
                    "stack" -> STACK_INSTANCE.deserialize(value)
                    "stacks"-> STACKS_INSTANCE.deserialize(value)
                    "tag"-> TAG_INSTANCE.deserialize(value)
                    else -> throw IllegalStateException("Unknown or missing type in IngredientProvider [$fieldName]: [$type]")
                }
            }
        }
        fun type(): ProviderType
        fun provide(): Ingredient
        fun deserialize(toml: TomlElement): IngredientProvider
        fun serialize(): TomlElement
        fun copy(): IngredientProvider
    }
}