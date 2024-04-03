package me.fzzyhmstrs.fzzy_config.validation.collection

import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.util.Expression.Impl.validated
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.*
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlArrayBuilder
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.asTomlArray

/**
 * a validated list
 *
 * This [ValidatedField] implements [List], so you can directly use it as if it were an immutable list
 * @param T any non-null type
 * @param defaultValue default list of values
 * @param entryHandler [Entry] used to handle individual list entries
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.validatedList
 * @see me.fzzyhmstrs.fzzy_config.validation.ValidatedField.toList
 * @see me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validated
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedList<T>(defaultValue: List<T>, private val entryHandler: Entry<T,*>): ValidatedField<List<T>>(defaultValue), List<T> {

    init {
        for(thing in defaultValue){
            if (entryHandler.validateEntry(thing,EntryValidator.ValidationType.WEAK).isError())
                throw IllegalStateException("Default List entry [$thing] not valid per entryHandler provided")
        }
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<List<T>> {
        return try{
            val array = toml.asTomlArray()
            val list: MutableList<T> = mutableListOf()
            val errors: MutableList<String> = mutableListOf()
            for ((index, el) in array.content.withIndex()){
                val result = entryHandler.deserializeEntry(el, errors, "$fieldName[$index]", true).report(errors)
                if (!result.isError()){
                    list.add(index,result.get())
                }
            }
            if (errors.isNotEmpty()) {
                ValidationResult.error(list, "Error(s) encountered while deserializing list, some entries were skipped: $errors")
            } else {
                ValidationResult.success(list)
            }
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Critical error enountered while deserializing list [$fieldName], using defaults.")
        }
    }

    override fun serialize(input: List<T>): ValidationResult<TomlElement> {
        val toml = TomlArrayBuilder()
        val errors: MutableList<String> = mutableListOf()
        try {
            for (entry in input) {
                val tomlEntry = entryHandler.serializeEntry(entry, errors, true)
                val annotations = if (entry != null)
                    try {
                        ConfigApiImpl.tomlAnnotations(entry!!::class)
                    } catch (e: Exception){
                        listOf()
                    }
                else
                    listOf()
                toml.element(tomlEntry, annotations)
            }
        } catch (e: Exception){
            return ValidationResult.error(toml.build(),"Critical error encountered while serializing list: ${e.localizedMessage}")
        }
        return ValidationResult.predicated(toml.build(), errors.isEmpty(), errors.toString())
    }

    override fun correctEntry(input: List<T>, type: EntryValidator.ValidationType): ValidationResult<List<T>> {
        val list: MutableList<T> = mutableListOf()
        val errors: MutableList<String> = mutableListOf()
        for (entry in input){
            val result = entryHandler.correctEntry(entry, type)
            list.add(result.get())
            if (result.isError()) errors.add(result.getError())
        }
        return if (errors.isNotEmpty()){
            ValidationResult.error(list,"Errors corrected in list: $errors")
        } else {
            ValidationResult.success(list)
        }
    }

    override fun validateEntry(input: List<T>, type: EntryValidator.ValidationType): ValidationResult<List<T>> {
        val errors: MutableList<String> = mutableListOf()
        for (entry in input){
            val result = entryHandler.validateEntry(entry, type)
            if (result.isError()) errors.add(result.getError())
        }
        return if (errors.isNotEmpty()){
            ValidationResult.error(input,"Errors corrected in list: $errors")
        } else {
            ValidationResult.success(input)
        }
    }

    override fun copyStoredValue(): List<T> {
        return storedValue.toList()
    }

    override fun instanceEntry(): ValidatedList<T> {
        return ValidatedList(copyStoredValue(), entryHandler)
    }

    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<List<T>>): ClickableWidget {
        return ButtonWidget.builder("fc.validated_field.list".translate()) { b -> openListEditPopup(b) }.size(110,20).build()
    }

    @Environment(EnvType.CLIENT)
    private fun openListEditPopup(b: ButtonWidget){
        val list = storedValue.map { entryHandler.instanceEntry().also { e -> e.applyEntry(it) } }
        val listWidget = ListListWidget(list, { entryHandler.instanceEntry() })
        val popup = PopupWidget.Builder(this.translation())
            .addElement("list", listWidget, Position.BELOW, Position.ALIGN_LEFT)
            .addDoneButton({ PopupWidget.pop() })
            .positionX()
            .positionY()
            .onClose({ this.validateAndSet(listWidget.getList()) })
            .build()
        PopupWidget.setPopup(popup)
    }
    
    // List Interface
    //////////////////////////////////
    override val size: Int
        get() = storedValue.size

    override fun get(index: Int): T {
        return storedValue[index]
    }

    override fun isEmpty(): Boolean {
        return storedValue.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return storedValue.iterator()
    }

    override fun listIterator(): ListIterator<T> {
        return storedValue.listIterator()
    }

    override fun listIterator(index: Int): ListIterator<T> {
        return storedValue.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        return storedValue.subList(fromIndex, toIndex)
    }

    override fun lastIndexOf(element: T): Int {
        return storedValue.lastIndexOf(element)
    }

    override fun indexOf(element: T): Int {
        return storedValue.indexOf(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return storedValue.containsAll(elements)
    }

    override fun contains(element: T): Boolean {
        return storedValue.contains(element)
    }

    companion object{

        /**
         * attempts to create a ValidatedList from the provided list and Entry
         *
         * This is utilized by [me.fzzyhmstrs.fzzy_config.updates.UpdateManagerImpl] to create ValidatedLists reflectively
         * @param T List type
         * @param list input List<T>
         * @param entry Entry of *any* type. Will attempt to cast it to a properly-typed Entry, or fail soft to null
         * @return [ValidatedList] nullable wrapping the list and entry provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun <T> tryMake(list: List<T>, entry: Entry<*,*>): ValidatedList<T>?{
            return try{
                ValidatedList(list, entry as Entry<T,*>)
            } catch (e: Exception){
                null
            }
        }

        /**
         * creates a Validated Integer List
         *
         * Allows any int value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param list input List<Int>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofInt(list: List<Int>): ValidatedList<Int> {
            return ValidatedList(list, ValidatedInt())
        }
        /**
         * creates a Validated Integer List
         *
         * Allows any int value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param i vararg int inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofInt(vararg i: Int): ValidatedList<Int> {
            return ValidatedList(listOf(*i.toTypedArray()), ValidatedInt())
        }

        /**
         * creates a Validated Byte List
         *
         * Allows any byte value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param list input List<Byte>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofByte(list: List<Byte>): ValidatedList<Byte> {
            return ValidatedList(list, ValidatedByte())
        }
        /**
         * creates a Validated Byte List
         *
         * Allows any byte value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param b vararg byte inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofByte(vararg b: Byte): ValidatedList<Byte> {
            return ValidatedList(listOf(*b.toTypedArray()), ValidatedByte())
        }

        /**
         * creates a Validated Short List
         *
         * Allows any short value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param list input List<Short>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofShort(list: List<Short>): ValidatedList<Short> {
            return ValidatedList(list, ValidatedShort())
        }
        /**
         * creates a Validated Short List
         *
         * Allows any short value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param s vararg short inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofShort(vararg s: Short): ValidatedList<Short> {
            return ValidatedList(listOf(*s.toTypedArray()), ValidatedShort())
        }

        /**
         * creates a Validated Long List
         *
         * Allows any long value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param list input List<Long>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofLong(list: List<Long>): ValidatedList<Long> {
            return ValidatedList(list, ValidatedLong())
        }
        /**
         * creates a Validated Long List
         *
         * Allows any long value from MIN_VALUE to MAX_VALUE, repeating elements allowed
         * @param l vararg long inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofLong(vararg l: Long): ValidatedList<Long> {
            return ValidatedList(listOf(*l.toTypedArray()), ValidatedLong())
        }

        /**
         * creates a Validated Double List
         *
         * Allows any double value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param list input List<Double>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofDouble(list: List<Double>): ValidatedList<Double> {
            return ValidatedList(list, ValidatedDouble())
        }
        /**
         * creates a Validated Double List
         *
         * Allows any double value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param d vararg double inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofDouble(vararg d: Double): ValidatedList<Double> {
            return ValidatedList(listOf(*d.toTypedArray()), ValidatedDouble())
        }

        /**
         * creates a Validated Float List
         *
         * Allows any float value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param list input List<Float>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofFloat(list: List<Float>): ValidatedList<Float> {
            return ValidatedList(list, ValidatedFloat())
        }
        /**
         * creates a Validated Float List
         *
         * Allows any float value from NEGATIVE_INFINITY to POSITIVE_INFINITY, repeating elements allowed
         * @param f vararg float inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofFloat(vararg f: Float): ValidatedList<Float> {
            return ValidatedList(listOf(*f.toTypedArray()), ValidatedFloat())
        }

        /**
         * creates a Validated Enum List
         *
         * Allows any value in the Enum, repeating elements allowed
         * @param T, the Enum type
         * @param list input List<Enum>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        inline fun <reified T: Enum<T>> ofEnum(list: List<T>): ValidatedList<T> {
            return ValidatedList(list, T::class.java.validated())
        }
        /**
         * creates a Validated Enum List
         *
         * Allows any value in the Enum, repeating elements allowed
         * @param T, the Enum type
         * @param e vararg Enum inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        inline fun <reified T: Enum<T>> ofEnum(vararg e: T): ValidatedList<T> {
            return ValidatedList(listOf(*e), T::class.java.validated())
        }


        /**
         * creates a Validated String List
         *
         * Allows any string value, repeating elements allowed
         * @param list input List<String>
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofString(list: List<String>): ValidatedList<String> {
            return ValidatedList(list, ValidatedString())
        }
        /**
         * creates a Validated String List
         *
         * Allows any string value, repeating elements allowed
         * @param s vararg string inputs to construct the list
         * @return [ValidatedList] from the list provided
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofString(vararg s: String): ValidatedList<String> {
            return ValidatedList(listOf(*s), ValidatedString())
        }
    }

    @Environment(EnvType.CLIENT)
    class ListListWidget<T>(entryList: List<Entry<T,*>>, private val entrySupplier: Supplier<Entry<T,*>>) : ElementListWidget<ChangelogListWidget.Entry>(MinecraftClient.getInstance(), 158, 160, 0, 20) {

        fun getList(): List<T>{
            val list: MutableList<T> = mutableListOf()
            for (e in entries){
                if (e !is ExistingEntry) continue
                list.add(e.get())
            }
            return list.toList()
        }
        
        override fun drawHeaderAndFooterSeparators(context: DrawContext?) {
        }
    
        override fun drawMenuListBackground(context: DrawContext?) {
        }
    
        override fun getRowWidth(): Int {
            return 134 //16 padding, 20 slider width and padding
        }
    
        override fun method_57718(): Int {
            return this.x + this.width / 2 + this.rowWidth / 2 + 6
        }
    
        init{
            for (e in entryList){
                this.addEntry(ExistingEntry(e))
            }
        }
    

        inner class ExistingEntry<T>(private val entry: Entry<T,*>): ElementListWidget.Entry<ExistingEntry<T>>() {

            private val entryWidget = entry.entryWidget(ChoiceValidator.any())
            private val deleteWidget = TextlessConfigActionWidget(
                "widget/action/delete".fcId(), 
                "widget/action/delete_inactive".fcId(), 
                "widget/action/delete_highlighted".fcId(),
                "fc.button.delete".translate(),
                "fc.button.delete".translate(),
                { true },
                { this@ListListWidget.removeEntry(this) })

            fun get(): T{
                return entry.get()
            }
            
            override fun children(): MutableList<out Element> {
                return mutableListOf(entryWidget, deleteWidget)
            }
        
            override fun selectableChildren(): MutableList<out Selectable> {
                return mutableListOf(entryWidget, deleteWidget)
            }
            
            override fun render(
                context: DrawContext,
                index: Int,
                y: Int,
                x: Int,
                entryWidth: Int,
                entryHeight: Int,
                mouseX: Int,
                mouseY: Int,
                hovered: Boolean,
                tickDelta: Float
            ) {
                if (this.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && widget.tooltip != null){
                    MinecraftClient.getInstance().currentScreen?.setTooltip(widget.tooltip, HoveredTooltipPositioner.INSTANCE,this.isFocused)
                }
                entryWidget.setPosition(x,y)
                entryWidget.render(context, mouseX, mouseY, tickDelta)
                deleteWidget.setPosition(x+114,y)
                deleteWidget.render(context, mouseX, mouseY, tickDelta)
            }
        }

        inner class NewEntry<T>(private val entrySupplier: Supplier<Entry<T,*>>): ElementListWidget.Entry<ExistingEntry<T>>() {

            private val addWidget = TextlessConfigActionWidget(
                "widget/action/add".fcId(), 
                "widget/action/add_inactive".fcId(), 
                "widget/action/add_highlighted".fcId(),
                "fc.button.add".translate(),
                "fc.button.add".translate(),
                { true },
                { this@ListListWidget.addEntry(TODO(),ExistingEntry<T>(entrySupplier.get())) })

            fun get(): T{
                return entry.get()
            }
            
            override fun children(): MutableList<out Element> {
                return mutableListOf(entryWidget, deleteWidget)
            }
        
            override fun selectableChildren(): MutableList<out Selectable> {
                return mutableListOf(entryWidget, deleteWidget)
            }
            
            override fun render(
                context: DrawContext,
                index: Int,
                y: Int,
                x: Int,
                entryWidth: Int,
                entryHeight: Int,
                mouseX: Int,
                mouseY: Int,
                hovered: Boolean,
                tickDelta: Float
            ) {
                if (this.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && widget.tooltip != null){
                    MinecraftClient.getInstance().currentScreen?.setTooltip(widget.tooltip, HoveredTooltipPositioner.INSTANCE,this.isFocused)
                }
                entryWidget.setPosition(x,y)
                entryWidget.render(context, mouseX, mouseY, tickDelta)
                deleteWidget.setPosition(x+114,y)
                deleteWidget.render(context, mouseX, mouseY, tickDelta)
            }
        }
    }
}
