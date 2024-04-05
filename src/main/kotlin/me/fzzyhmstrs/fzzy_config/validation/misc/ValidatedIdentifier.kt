package me.fzzyhmstrs.fzzy_config.validation.misc

import com.google.common.collect.Lists
import com.mojang.brigadier.suggestion.Suggestion
import com.mojang.brigadier.suggestion.Suggestions
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.SuggestionWindow
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.AllowableIdentifiers
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedIdentifier.Companion.ofList
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedIdentifier.Companion.ofRegistry
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedIdentifier.Companion.ofTag
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatInputSuggestor
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.KeyCodes
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import org.lwjgl.glfw.GLFW
import java.util.concurrent.CompletableFuture
import java.util.function.*
import kotlin.math.max
import kotlin.math.min

/**
 * A validated Identifier field.
 *
 * NOTE: The base handler of this validated field is actually string. As such, usage in, for example, a [ValidatedList][me.fzzyhmstrs.fzzy_config.validation.list.ValidatedList] will yield a List<String>
 *
 * There are various shortcut methods available for building ValidatedIdentifiers more easily than with the primary constructor. Check out options in the See Also section
 * @param defaultValue String, the string value of the default identifier
 * @param allowableIds [AllowableIdentifiers] instance. Defines the predicate for valid ids, and the supplier of valid id lists
 * @param validator [EntryValidator]<String> handles validation of individual entries. Defaults to validation based on the predicate provided in allowableIds
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedIdentifier
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.fieldLang
 * @see me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedIdentifierMap
 * @see ofTag
 * @see ofRegistry
 * @see ofList

 * @author fzzyhmstrs
 * @since 0.1.2
 */
@Suppress("unused")
class ValidatedIdentifier @JvmOverloads constructor(defaultValue: Identifier, private val allowableIds: AllowableIdentifiers, private val validator: EntryValidator<Identifier> = default(allowableIds))
    :
    ValidatedField<Identifier>(defaultValue),
    Updatable,
    Translatable,
    Comparable<Identifier>
{
    /**
     * An unbounded validated identifier
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @param defaultValue [Identifier] the default identifier for this validation
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.unboundedIdentifier]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Identifier): this(defaultValue, AllowableIdentifiers.ANY)

    /**
     * An unbounded validated identifier constructed from a string
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @param defaultValue [String] the default identifier (in string form) for this validation
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.stringIdentifier]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: String): this(Identifier(defaultValue), AllowableIdentifiers.ANY)

    /**
     * An unbounded validated identifier constructed from namespace and path strings
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @param defaultNamespace [String] the default namespace for this validation
     * @param defaultPath [String] the default path for this validation
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.stringStringIdentifier]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultNamespace: String, defaultPath: String): this(Identifier(defaultNamespace, defaultPath), AllowableIdentifiers.ANY)

    /**
     * An unbounded validated identifier with a dummy default value
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.emptyIdentifier]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(Identifier("c:/c"), AllowableIdentifiers.ANY)

    override fun copyStoredValue(): Identifier {
        return Identifier(storedValue.toString())
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Identifier> {
        return try {
            val string = toml.toString()
            val id = Identifier.tryParse(string) ?: return ValidationResult.error(storedValue,"Invalid identifier [$fieldName].")
            ValidationResult.success(id)
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing identifier [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: Identifier): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input.toString()))
    }

    override fun correctEntry(input: Identifier, type: EntryValidator.ValidationType): ValidationResult<Identifier> {
        val result = validator.validateEntry(input, type)
        return if(result.isError()) {
            ValidationResult.error(storedValue, "Invalid identifier [$input] found, corrected to [$storedValue]: ${result.getError()}")} else result
    }

    override fun validateEntry(input: Identifier, type: EntryValidator.ValidationType): ValidationResult<Identifier> {
        return validator.validateEntry(input, type)
    }

    override fun instanceEntry(): ValidatedIdentifier {
        return ValidatedIdentifier(copyStoredValue(), allowableIds, validator)
    }

    override fun widgetEntry(choicePredicate: ChoiceValidator<Identifier>): ClickableWidget {
        return OnClickTextFieldWidget({ this.get().toString() }, {
            val popup = PopupWidget.Builder(this.translation())
                .addElement("text_field",PopupIdentifierTextFieldWidget(110,20,choicePredicate,this))
                .positionX { _, _ -> it.x - 8 }
                .positionY { _, w -> it.y - (w - 28) }
                .build()
            PopupWidget.setPopup(popup)
        })
    }

    ////////////////////////

    /**
     * @return the path of the cached Identifier
     */
    fun getPath(): String {
        return storedValue.path
    }
    /**
     * @return the namespace of the cached Identifier
     */
    fun getNamespace(): String {
        return storedValue.namespace
    }

    fun withPath(path: String?): Identifier {
        return storedValue.withPath(path)
    }

    fun withPath(pathFunction: UnaryOperator<String?>): Identifier {
        return storedValue.withPath(pathFunction)
    }

    fun withPrefixedPath(prefix: String): Identifier {
        return storedValue.withPrefixedPath(prefix)
    }

    fun withSuffixedPath(suffix: String): Identifier {
        return storedValue.withSuffixedPath(suffix)
    }

    override fun toString(): String {
        return storedValue.toString()
    }

    override fun translationKey(): String {
        return getEntryKey()
    }

    override fun descriptionKey(): String {
        return getEntryKey() + ".desc"
    }

    override fun equals(other: Any?): Boolean {
        return storedValue == other
    }

    override fun hashCode(): Int {
        return storedValue.hashCode()
    }

    override fun compareTo(other: Identifier): Int {
        return storedValue.compareTo(other)
    }

    fun toUnderscoreSeparatedString(): String {
        return storedValue.toUnderscoreSeparatedString()
    }

    fun toTranslationKey(): String {
        return storedValue.toTranslationKey()
    }

    fun toShortTranslationKey(): String {
        return storedValue.toShortTranslationKey()
    }

    fun toTranslationKey(prefix: String): String {
        return storedValue.toTranslationKey(prefix)
    }

    fun toTranslationKey(prefix: String, suffix: String): String? {
        return storedValue.toTranslationKey(prefix, suffix)
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    companion object{

        @JvmStatic
        val DEFAULT_WEAK: EntryValidator<Identifier> = EntryValidator { i, _ -> ValidationResult.success(i) }

        /**
         * builds a String EntryValidator with default behavior
         *
         * Use if your identifier list may not be available at load-time (during modInitializtion, typically), but will be available during updating (in-game). Lists from a Tag or Registry are easy examples, as the registry may not be fully populated yet, and the tag may not be loaded.
         * @param allowableIds an [AllowableIdentifiers] instance.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun default(allowableIds: AllowableIdentifiers): EntryValidator<Identifier> {
            return EntryValidator.Builder<Identifier>()
                .weak(DEFAULT_WEAK)
                .strong { i, _ -> ValidationResult.predicated(i, allowableIds.test(i), "Identifier invalid or not allowed") }
                .buildValidator()
        }

        /**
         * builds a String EntryValidator with always-strong behavior
         *
         * Use if your identifier list is available both at loading (during modInitializtion, typically), and during updating (in-game).
         * @param allowableIds an [AllowableIdentifiers] instance.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun strong(allowableIds: AllowableIdentifiers): EntryValidator<Identifier> {
            return EntryValidator.Builder<Identifier>()
                .weak { i, _ -> ValidationResult.predicated(i, allowableIds.test(i), "Identifier invalid or not allowed") }
                .strong { i, _ -> ValidationResult.predicated(i, allowableIds.test(i), "Identifier invalid or not allowed") }
                .buildValidator()
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable tag of values
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param tag the tag of allowable values to choose from
         * @return [ValidatedIdentifier] wrapping the provided default and tag
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> ofTag(defaultValue: Identifier, tag: TagKey<T>): ValidatedIdentifier{
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(tag.registry().value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ false }, { listOf() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ false }, { listOf() }))
            val supplier = Supplier { registry.iterateEntries(tag).mapNotNull { registry.getId(it.value()) } }
            return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable tag of values
         *
         * Uses "minecraft:air" as the default value.
         * @param tag the tag of allowable values to choose from
         * @return [ValidatedIdentifier] wrapping the provided tag
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofTag(tag: TagKey<T>): ValidatedIdentifier{
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(tag.registry().value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(Identifier("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(Identifier("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val supplier = Supplier { registry.iterateEntries(tag).mapNotNull { registry.getId(it.value()) } }
            return ValidatedIdentifier(Identifier("minecraft:air"), AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier))
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param registry the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided default and registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun <T> ofRegistry(defaultValue: Identifier, registry: Registry<T>): ValidatedIdentifier {
            return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ id -> registry.containsId(id) }, { registry.ids.toList() }))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, filtered by the provided predicate
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param registry the registry whose ids are valid for this identifier
         * @param predicate Predicate<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided default and predicated registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun <T> ofRegistry(defaultValue: Identifier, registry: Registry<T>, predicate: Predicate<RegistryEntry<T>>): ValidatedIdentifier {
            return ValidatedIdentifier(defaultValue,
                AllowableIdentifiers(
                    { id -> registry.containsId(id) && predicate.test ((registry.getEntry(id).takeIf { it.isPresent } ?: return@AllowableIdentifiers false).get()) },
                    { registry.ids.filter { id -> predicate.test ((registry.getEntry(id).takeIf { it.isPresent } ?: return@filter false).get()) } }
                )
            )
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values
         *
         * Uses "minecraft:air" as the default value
         * @param registry the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistry(registry: Registry<T>): ValidatedIdentifier {
            return ValidatedIdentifier(Identifier("minecraft:air"), AllowableIdentifiers({ id -> registry.containsId(id) }, { registry.ids.toList() }))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, filtered by the provided predicate
         *
         * Uses "minecraft:air" as the default value
         * @param registry the registry whose ids are valid for this identifier
         * @param predicate [BiPredicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided predicated registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistry(registry: Registry<T>, predicate: BiPredicate<Identifier,RegistryEntry<T>>): ValidatedIdentifier {
            return ValidatedIdentifier(Identifier("minecraft:air"),
                AllowableIdentifiers(
                    { id -> registry.containsId(id) && predicate.test (id, (registry.getEntry(id).takeIf { it.isPresent } ?: return@AllowableIdentifiers false).get()) },
                    { registry.ids.filter { id -> predicate.test (id, (registry.getEntry(id).takeIf { it.isPresent } ?: return@filter false).get()) } }
                )
            )
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable list of values
         *
         * This list should be available and complete at validation time
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param list the list whose entries are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided default and list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
        fun ofList(defaultValue: Identifier, list: List<Identifier>): ValidatedIdentifier{
            val allowableIds = AllowableIdentifiers({ id -> list.contains(id) }, list.supply())
            val validator = strong(allowableIds)
            return ValidatedIdentifier(defaultValue, allowableIds, validator)
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable list of values
         *
         * This list should be available and complete at validation time
         *
         * uses "minecraft:air" as the default value
         * @param list the list whose entries are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Use only for validation of a list or map. Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
        fun ofList(list: List<Identifier>): ValidatedIdentifier{
            val allowableIds = AllowableIdentifiers({ id -> list.contains(id) }, list.supply())
            val validator = strong(allowableIds)
            return ValidatedIdentifier(Identifier("minecraft:air"), allowableIds, validator)
        }

        /**
         * wraps a list in a [Supplier]
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @Deprecated("Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
        fun<T> List<T>.supply(): Supplier<List<T>>{
            return Supplier { this }
        }
    }

    @Environment(EnvType.CLIENT)
    class OnClickTextFieldWidget(private val textSupplier: Supplier<String>, private val onClick: Consumer<OnClickTextFieldWidget>)
        :
        TextFieldWidget(MinecraftClient.getInstance().textRenderer,0,0, 110, 20, FcText.empty())
    {
        init {
            setMaxLength(1000)
            this.text = textSupplier.get()
        }

        override fun renderWidget(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
            super.renderWidget(context, mouseX, mouseY, delta)
            this.text = textSupplier.get()
        }

        override fun onClick(mouseX: Double, mouseY: Double) {
            onClick.accept(this)
        }

        override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            return if (!this.isFocused) {
                false
            } else if(KeyCodes.isToggle(keyCode)) {
                onClick.accept(this)
                return true
            } else super.keyPressed(keyCode, scanCode, modifiers)
        }

    }

    @Environment(EnvType.CLIENT)
    class PopupIdentifierTextFieldWidget(
        width: Int,
        height: Int,
        private val choiceValidator: ChoiceValidator<Identifier>,
        private val validatedIdentifier: ValidatedIdentifier
    ): TextFieldWidget(MinecraftClient.getInstance().textRenderer,0,0, width, height, FcText.empty()){


        private var storedValue = validatedIdentifier.get()
        private var lastChangedTime: Long = 0L
        private var isValid = true
        private var pendingSuggestions: CompletableFuture<Suggestions>? = null
        private var window: SuggestionWindow? = null
        private var closeWindow = false


        private fun isValidTest(s: String): Boolean {
            pendingSuggestions = validatedIdentifier.allowableIds.getSuggestions(s,this.cursor, choiceValidator)
            val id = Identifier.tryParse(s)
            if (id == null){
                setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
                return false
            }
            return if (validatedIdentifier.validateEntry(id, EntryValidator.ValidationType.STRONG).isValid()) {
                val result = choiceValidator.validateEntry(id,EntryValidator.ValidationType.STRONG)
                if (result.isValid()) {
                    storedValue = result.get()
                    lastChangedTime = System.currentTimeMillis()
                    setEditableColor(0xFFFFFF)
                    true
                } else {
                    setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
                    false
                }
            } else {
                setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
                false
            }
        }

        override fun getInnerWidth(): Int {
            return super.getInnerWidth() - 11
        }

        private fun isChanged(): Boolean {
            return storedValue != validatedIdentifier.get()
        }

        private fun ongoingChanges(): Boolean{
            return System.currentTimeMillis() - lastChangedTime <= 350L
        }

        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            if(isChanged()){
                if (lastChangedTime != 0L && !ongoingChanges())
                    validatedIdentifier.accept(storedValue)
            }
            super.renderWidget(context, mouseX, mouseY, delta)
            if(isValid){
                if (ongoingChanges())
                    context.drawGuiTexture(TextureIds.ENTRY_ONGOING,x + width - 20, y, 20, 20)
                else
                    context.drawGuiTexture(TextureIds.ENTRY_OK,x + width - 20, y, 20, 20)
            } else {
                context.drawGuiTexture(TextureIds.ENTRY_ERROR,x + width - 20, y, 20, 20)
            }
            window?.render(context, mouseX, mouseY, delta)
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            val bl = window?.mouseClicked(mouseX.toInt(), mouseY.toInt(), button) ?: super.mouseClicked(mouseX, mouseY, button)
            if (closeWindow)
                window = null
            return bl
        }

        override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
            return window?.mouseScrolled(mouseX.toInt(),mouseY.toInt(),verticalAmount) ?: super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        }

        override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            val bl = window?.keyPressed(keyCode, scanCode, modifiers) ?: super.keyPressed(keyCode, scanCode, modifiers)
            if (closeWindow)
                this.window = null
            return bl
        }

        init {
            setMaxLength(1000)
            text = validatedIdentifier.get().toString()
            setChangedListener { s -> isValid = isValidTest(s) }
        }

        private fun addSuggestionWindow(suggestions: Suggestions){
            var w = 0
            for (suggestion in suggestions.list) {
                w = max(w, MinecraftClient.getInstance().textRenderer.getWidth(suggestion.text))
            }
            val sWidth = MinecraftClient.getInstance().currentScreen?.width ?: Int.MAX_VALUE
            val sHeight = MinecraftClient.getInstance().currentScreen?.height ?: Int.MAX_VALUE
            val x = max(min(this.x,sWidth - w),0)
            var h = min(suggestions.list.size * 12, 120)
            val up = this.y
            val down = sHeight - (this.y + 20)
            val upBl: Boolean
            val y = if(up >= down) {
                upBl = true
                while (this.y - h < 0){
                    h -= 12
                }
                this.y - h
            } else {
                upBl = false
                while (this.y + 20 + h > sHeight){
                    h -= 12
                }
                this.y + 20
            }
            this.window = SuggestionWindow(sortSuggestions(suggestions), x, y, w, h, upBl,
                { s ->
                    try {
                        validatedIdentifier.applyEntry(Identifier(s))
                    } catch (e: Exception) {
                        //
                    }
                },
                {
                    closeWindow = true
                })
        }

        private fun sortSuggestions(suggestions: Suggestions): List<Suggestion> {
            val string: String = this.text.substring(0, this.cursor)
            val string2 = string.lowercase()
            val list = Lists.newArrayList<Suggestion>()
            val list2 = Lists.newArrayList<Suggestion>()
            for (suggestion in suggestions.list) {
                if (suggestion.text.startsWith(string2) || suggestion.text.startsWith("minecraft:$string2")) {
                    list.add(suggestion)
                    continue
                }
                list2.add(suggestion)
            }
            list.addAll(list2)
            return list
        }

    }
}