package me.fzzyhmstrs.fzzy_config.validation.minecraft

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.DecoratedActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.util.EnumTranslatable
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedSet
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIngredient.IngredientProvider
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedIdentifier
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.MultilineTextWidget
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.*
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * A validated provider of [Ingredient]
 *
 * This does not store an ingredient, it stores an [IngredientProvider], which lazily generates the ingredient only when requested.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ValidatedIngredient private constructor(defaultValue: IngredientProvider, predicate: Predicate<Identifier>? = null): ValidatedField<ValidatedIngredient.IngredientProvider>(defaultValue) {

    /**
     * A validated provider of [Ingredient]
     *
     * This does not store an ingredient, it stores an [IngredientProvider], which lazily generates the ingredient only when requested.
     *
     * Initializes this validation with a single-item IngredientProvider
     * @param item [Identifier] defining the item
     * @param predicate [Predicate]<Identifier>, optional - restricts the set of allowable items (default is any item in the Items registry)
     * @sample me.fzzyhmstrs.fzzy_config.examples.MinecraftExamples.validatedIngredientItem
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(item: Identifier, predicate: Predicate<Identifier>? = null): this(ItemProvider(item), predicate)

    /**
     * A validated provider of [Ingredient]
     *
     * This does not store an ingredient, it stores an [IngredientProvider], which lazily generates the ingredient only when requested.
     *
     * Initializes this validation with a multi-item IngredientProvider
     * @param set [Set]<[Identifier]> defining the items
     * @param predicate [Predicate]<Identifier>, optional - restricts the set of allowable items (default is any item in the Items registry)
     * @sample me.fzzyhmstrs.fzzy_config.examples.MinecraftExamples.validatedIngredientList
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(set: Set<Identifier>, predicate: Predicate<Identifier>? = null): this(ListProvider(set), predicate)

    /**
     * A validated provider of [Ingredient]
     *
     * This does not store an ingredient, it stores an [IngredientProvider], which lazily generates the ingredient only when requested.
     *
     * Initializes this validation with a multi-item IngredientProvider
     * @param tag [TagKey]<Item> defining the tag to pull items from
     * @param predicate [Predicate]<Identifier>, optional - restricts the set of allowable tags (default is any tag from the Items registry)
     * @sample me.fzzyhmstrs.fzzy_config.examples.MinecraftExamples.validatedIngredientTag
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(tag: TagKey<Item>, predicate: Predicate<Identifier>? = null): this(TagProvider(tag.id), predicate)


    private val tagValidator = if(predicate == null) ValidatedIdentifier.ofRegistryTags(RegistryKeys.ITEM) else ValidatedIdentifier.ofRegistryTags(RegistryKeys.ITEM,predicate)
    @Suppress("DEPRECATION")
    private val itemValidator = if(predicate == null) ValidatedIdentifier.ofRegistry(Registries.ITEM) else ValidatedIdentifier.ofRegistry(Registries.ITEM) { id, _ -> predicate.test(id) }
    private val listValidator = ValidatedSet(setOf(),itemValidator)

    /**
     * Supplies the [Ingredient] from this ValidatedIngredients Provider
     * @return [Ingredient] generated from the current [IngredientProvider]
     * @sample me.fzzyhmstrs.fzzy_config.examples.MinecraftExamples.validatedIngredientIngredient
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun toIngredient(): Ingredient{
        return storedValue.provide()
    }
    @Internal
    @Deprecated("use deserialize to avoid accidentally overwriting validation and error reporting")
    override fun deserializeEntry(
        toml: TomlElement,
        errorBuilder: MutableList<String>,
        fieldName: String,
        ignoreNonSync: Boolean
    ): ValidationResult<IngredientProvider> {
        @Suppress("DEPRECATION")
        val result = super.deserializeEntry(toml, errorBuilder, fieldName, ignoreNonSync)
        when(storedValue.type()){
            ProviderType.STACK -> itemValidator.validateAndSet((storedValue as ItemProvider).id)
            ProviderType.LIST -> listValidator.validateAndSet((storedValue as ListProvider).ids)
            ProviderType.TAG -> tagValidator.validateAndSet((storedValue as TagProvider).tag)
        }
        return result
    }
    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<IngredientProvider> {
        return try{
            ValidationResult.success(IngredientProvider.deserialize(toml, fieldName))
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error while deserializing ValidatedIngredient [$fieldName]: ${e.localizedMessage}")
        }
    }
    @Internal
    override fun serialize(input: IngredientProvider): ValidationResult<TomlElement> {
        return ValidationResult.success(IngredientProvider.serialize(input))
    }

    override fun instanceEntry(): ValidatedField<IngredientProvider> {
        return ValidatedIngredient(defaultValue)
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is IngredientProvider
    }
    @Internal
    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<IngredientProvider>): ClickableWidget {
        return DecoratedActiveButtonWidget("fc.validated_field.ingredient.edit".translate(),110,20,"widget/decoration/ingredient".fcId(),{ true },{ openIngredientPopup(it) })
    }

    @Environment(EnvType.CLIENT)
    private fun openIngredientPopup(b: ClickableWidget){
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val typeChooser = ValidatedEnum(storedValue.type().toWrapper(), ValidatedEnum.WidgetType.CYCLING)
        val popup = PopupWidget.Builder(translation())
            .addDivider()
            .addElement("hint",MultilineTextWidget("fc.validated_field.ingredient".translate(),textRenderer).setMaxWidth(110).setCentered(true), Position.BELOW, Position.ALIGN_CENTER)
            .addElement("type",typeChooser.widgetEntry(ChoiceValidator.any()), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("three_widget",ThreeTypesWidget({ fromWrapper(typeChooser.get()) }, itemValidator.widgetEntry(ChoiceValidator.any()), listValidator.widgetEntry(ChoiceValidator.any()),tagValidator.widgetEntry(ChoiceValidator.any())), Position.BELOW, Position.ALIGN_LEFT)
            .addDoneButton()
            .positionX(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 })
            .positionY(PopupWidget.Builder.popupContext { h -> b.y + b.height/2 - h/2 })
            .onClose{ this.setAndUpdate(fromWrapper(typeChooser.get()).create(this)) }
            .build()
        PopupWidget.push(popup)
    }

    @Environment(EnvType.CLIENT)
    private fun fromWrapper(wrapper: ProviderTypeWrapper): ProviderType{
        return when(wrapper){
            ProviderTypeWrapper.STACK -> ProviderType.STACK
            ProviderTypeWrapper.LIST -> ProviderType.LIST
            ProviderTypeWrapper.TAG -> ProviderType.TAG
        }
    }


    @Environment(EnvType.CLIENT)
    private class ThreeTypesWidget(private val typeSupplier: Supplier<ProviderType>, private val stack: ClickableWidget, private val list: ClickableWidget, private val tag: ClickableWidget): ClickableWidget(0,0,110,20,FcText.empty()){
        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            when(typeSupplier.get()){
                ProviderType.STACK -> stack.render(context, mouseX, mouseY, delta)
                ProviderType.LIST -> list.render(context, mouseX, mouseY, delta)
                ProviderType.TAG -> tag.render(context, mouseX, mouseY, delta)
            }
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.mouseClicked(mouseX, mouseY, button)
                ProviderType.LIST -> list.mouseClicked(mouseX, mouseY, button)
                ProviderType.TAG -> tag.mouseClicked(mouseX, mouseY, button)
            }
        }
        override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
                ProviderType.LIST -> list.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
                ProviderType.TAG -> tag.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
            }
        }
        override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.mouseReleased(mouseX, mouseY, button)
                ProviderType.LIST -> list.mouseReleased(mouseX, mouseY, button)
                ProviderType.TAG -> tag.mouseReleased(mouseX, mouseY, button)
            }
        }

        override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
                ProviderType.LIST -> list.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
                ProviderType.TAG -> tag.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
            }
        }

        override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.keyPressed(keyCode, scanCode, modifiers)
                ProviderType.LIST -> list.keyPressed(keyCode, scanCode, modifiers)
                ProviderType.TAG -> tag.keyPressed(keyCode, scanCode, modifiers)
            }
        }
        override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.keyReleased(keyCode, scanCode, modifiers)
                ProviderType.LIST -> list.keyReleased(keyCode, scanCode, modifiers)
                ProviderType.TAG -> tag.keyReleased(keyCode, scanCode, modifiers)
            }
        }

        override fun setFocused(focused: Boolean) {
            when(typeSupplier.get()){
                ProviderType.STACK -> stack.isFocused = focused
                ProviderType.LIST -> list.isFocused = focused
                ProviderType.TAG -> tag.isFocused = focused
            }
        }
        override fun isFocused(): Boolean {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.isFocused
                ProviderType.LIST -> list.isFocused
                ProviderType.TAG -> tag.isFocused
            }
        }

        override fun isHovered(): Boolean {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.isHovered
                ProviderType.LIST -> list.isHovered
                ProviderType.TAG -> tag.isHovered
            }
        }

        override fun isSelected(): Boolean {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.isSelected
                ProviderType.LIST -> list.isSelected
                ProviderType.TAG -> tag.isSelected
            }
        }

        override fun getType(): Selectable.SelectionType {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.type
                ProviderType.LIST -> list.type
                ProviderType.TAG -> tag.type
            }
        }

        override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.isMouseOver(mouseX, mouseY)
                ProviderType.LIST -> list.isMouseOver(mouseX, mouseY)
                ProviderType.TAG -> tag.isMouseOver(mouseX, mouseY)
            }
        }

        override fun getTooltip(): Tooltip? {
            return when(typeSupplier.get()){
                ProviderType.STACK -> stack.tooltip
                ProviderType.LIST -> list.tooltip
                ProviderType.TAG -> tag.tooltip
            }
        }

        override fun setX(x: Int) {
            super.setX(x)
            stack.x = x
            list.x = x
            tag.x = x
        }

        override fun setY(y: Int) {
            super.setY(y)
            stack.y = y
            list.y = y
            tag.y = y
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            when(typeSupplier.get()){
                ProviderType.STACK -> stack.appendNarrations(builder)
                ProviderType.LIST -> list.appendNarrations(builder)
                ProviderType.TAG -> tag.appendNarrations(builder)
            }
        }

    }

    enum class ProviderTypeWrapper: EnumTranslatable{
        STACK,
        LIST,
        TAG;
        override fun prefix(): String {
            return "fc.validated_field.ingredient"
        }
    }

    enum class ProviderType: EnumTranslatable{
        STACK {
            override fun create(ingredient: ValidatedIngredient): IngredientProvider {
                return ItemProvider(ingredient.itemValidator.get())
            }
            override fun type(): String {
                return "stack"
            }
            override fun toWrapper(): ProviderTypeWrapper {
                return ProviderTypeWrapper.STACK
            }
        },
        LIST {
            override fun create(ingredient: ValidatedIngredient): IngredientProvider {
                return ListProvider(ingredient.listValidator.get())
            }
            override fun type(): String {
                return "stacks"
            }
            override fun toWrapper(): ProviderTypeWrapper {
                return ProviderTypeWrapper.LIST
            }
        },
        TAG {
            override fun create(ingredient: ValidatedIngredient): IngredientProvider {
                return TagProvider(ingredient.tagValidator.get())
            }
            override fun type(): String {
                return  "tag"
            }
            override fun toWrapper(): ProviderTypeWrapper {
                return ProviderTypeWrapper.TAG
            }
        };

        override fun prefix(): String {
            return "fc.validated_field.ingredient"
        }
        abstract fun create(ingredient: ValidatedIngredient): IngredientProvider
        abstract fun type(): String
        abstract fun toWrapper(): ProviderTypeWrapper
    }

    class ItemProvider(val id: Identifier): IngredientProvider {
        override fun type(): ProviderType {
            return ProviderType.STACK
        }
        override fun provide(): Ingredient {
            val item = Registries.ITEM.get(id)
            return if (item == Items.AIR) {
                Ingredient.empty()
            } else {
                Ingredient.ofItems(item)
            }
        }
        override fun deserialize(toml: TomlElement): IngredientProvider {
            return ItemProvider(Identifier(toml.asTomlLiteral().toString()))
        }
        override fun serialize(): TomlElement {
            return TomlLiteral(id.toString())
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

    class ListProvider(val ids: Set<Identifier>): IngredientProvider {
        override fun type(): ProviderType {
            return ProviderType.LIST
        }
        override fun provide(): Ingredient {
            val items = ids.map { Registries.ITEM.get(it) }.filter { it != Items.AIR }
            return Ingredient.ofItems(*items.toTypedArray())
        }
        override fun deserialize(toml: TomlElement): IngredientProvider {
            val array = toml.asTomlArray()
            return ListProvider( array.mapNotNull { try{Identifier(it.asTomlLiteral().toString())} catch (e: Exception) { null } }.toSet() )
        }
        override fun serialize(): TomlElement {
            val toml = TomlArrayBuilder(ids.size)
            for (id in ids){
                toml.element(TomlLiteral(id.toString()))
            }
            return toml.build()
        }
        override fun toString(): String {
            return "List Ingredient $ids"
        }
        override fun equals(other: Any?): Boolean {
            if (other !is ListProvider) return false
            return ids == other.ids
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
            return Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM,tag))
        }
        override fun deserialize(toml: TomlElement): IngredientProvider {
            return TagProvider(Identifier(toml.asTomlLiteral().toString().replace("#","")))
        }
        override fun serialize(): TomlElement {
            return TomlLiteral("#${tag}")
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
        companion object{
            private val STACK_INSTANCE = ItemProvider(Identifier("dummy"))
            private val STACKS_INSTANCE = ListProvider(setOf())
            private val TAG_INSTANCE = TagProvider(Identifier("dummy"))
            fun serialize(provider: IngredientProvider): TomlElement{
                val toml = TomlTableBuilder(2)
                toml.element("type", TomlLiteral(provider.type().type()))
                toml.element("value",provider.serialize())
                return toml.build()
            }
            fun deserialize(toml: TomlElement, fieldName: String): IngredientProvider{
                val table = toml.asTomlTable()
                val type = table["type"]?.asTomlLiteral()?.toString() ?: throw IllegalStateException("Unknown or missing type in IngredientProvider [$fieldName]")
                val value = table["value"] ?: throw IllegalStateException("Unknown or missing value in IngredientProvider [$fieldName]")
                return when(type){
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
    }
}