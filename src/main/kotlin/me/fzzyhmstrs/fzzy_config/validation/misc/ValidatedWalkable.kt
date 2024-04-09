package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.Walkable
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.widget.ActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.DecoratedActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Supplier
import kotlin.reflect.full.createInstance

/**
 * Validation for an arbitrary POJO that implements [Walkable]. It will create a "mini-config" popup with the same style of list as the main config and section screens.
 *
 * This Validation is useful for "blocks" of config, such as Entity Stats or Tool Materials, that you want to break out
 * @param T [Walkable] object to be managed
 * @param defaultValue Instance of T to wrap
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.walkables
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ValidatedWalkable<T: Walkable>(defaultValue: T): ValidatedField<T>(defaultValue) {

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<T> {
        return ConfigApiImpl.deserializeFromToml(storedValue,toml, mutableListOf())
    }
    @Internal
    override fun serialize(input: T): ValidationResult<TomlElement> {
        val errors = mutableListOf<String>()
        return ValidationResult.predicated(ConfigApiImpl.serializeToToml(input,errors),errors.isEmpty(),"Errors encountered while serializing Object: $errors")
    }

    /**
     * creates a deep copy of this ValidatedWalkable
     * @return ValidatedWalkable wrapping a deep copy of the currently stored object
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedField<T> {
        return ValidatedWalkable(copyStoredValue())
    }

    @Internal
    override fun setAndUpdate(input: T) {
        if (input == get()) return
        val oldVal = get()
        val oldStr = ConfigApiImpl.serializeConfig(oldVal, mutableListOf(),true).lines().joinToString(" ", transform = {s -> s.trim()})
        val tVal1 = correctEntry(input, EntryValidator.ValidationType.STRONG)
        val newStr = ConfigApiImpl.serializeConfig(tVal1.get(), mutableListOf(),true).lines().joinToString(" ", transform = {s -> s.trim()})
        set(tVal1.get())
        val message = if (tVal1.isError()){
            FcText.translatable("fc.validated_field.update.error",translation(),oldStr,newStr,tVal1.getError())
        } else {
            FcText.translatable("fc.validated_field.update",translation(),oldStr,newStr)
        }
        update(message)
    }

    /**
     * Creates a deep copy of the stored value and returns it
     * @return T - deep copy of the currently stored object
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun copyStoredValue(): T {
        val new = storedValue::class.createInstance()
        val toml = serialize(this.get()).get()
        val result = ConfigApiImpl.deserializeFromToml(new, toml, mutableListOf())
        return if (result.isError()) storedValue else result.get()
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        return input::class.java == defaultValue::class.java
    }

    @Internal
    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        return DecoratedActiveButtonWidget("fc.validated_field.object".translate(),110,20,"widget/decoration/object".fcId(),{ true }, { openObjectPopup() })
    }

    /**
     * @suppress
     */
     override fun toString(): String{
         return "Validated Walkable[value=${ConfigApiImpl.serializeConfig(get(), mutableListOf(),true).lines().joinToString(" ", transform = {s -> s.trim()})}, validation=per contained member validation]"
     }

    @Environment(EnvType.CLIENT)
    private fun openObjectPopup() {
        val newThing = copyStoredValue()
        val newNewThing = defaultValue::class.createInstance()
        val manager = ValidatedObjectUpdateManager(newThing, getEntryKey())
        val entryList = ConfigListWidget(MinecraftClient.getInstance(),298,160,0,false)
        ConfigApiImpl.walk(newThing,getEntryKey(),true){_,_,new,thing,_,_ ->
            if (thing is Updatable && thing is Entry<*, *>) {
                val fieldName = new.substringAfterLast('.')
                val name = thing.transLit(fieldName.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
                thing.setEntryKey(new)
                thing.setUpdateManager(manager)
                manager.setUpdatableEntry(thing)
                entryList.add(ConfigEntry(name,thing.descLit(""),entryList,thing.widgetEntry(),null,null,null))

            } else if (thing is Walkable){
                val validation = ValidatedWalkable(thing)
                validation.setEntryKey(new)
                validation.setUpdateManager(manager)
                manager.setUpdatableEntry(validation)
                val name = validation.translation()
                entryList.add(ConfigEntry(name,validation.descLit(""),entryList,validation.widgetEntry(),null,null,null))
            } else if (thing != null) {
                var basicValidation: ValidatedField<*>? = null
                val target = new.removePrefix("${getEntryKey()}.")
                ConfigApiImpl.drill(newNewThing,target,'.',true) { _,_,_,thing2,prop,_ ->
                    basicValidation = manager.basicValidationStrategy(thing2,prop.returnType)?.instanceEntry()
                }
                val basicValidation2 = basicValidation
                if (basicValidation2 != null) {
                    basicValidation2.trySet(thing)
                    basicValidation2.setEntryKey(new)
                    basicValidation2.setUpdateManager(manager)
                    manager.setUpdatableEntry(basicValidation2)
                    val name = basicValidation2.translation()
                    entryList.add(ConfigEntry(name,basicValidation2.descLit(""),entryList,basicValidation2.widgetEntry(),null,null,null))
                }
            }
        }
        manager.pushUpdatableStates()
        val popup = PopupWidget.Builder(translation())
            .addElement("list", entryList, Position.BELOW, Position.ALIGN_CENTER)
            .addElement("revert", ActiveButtonWidget("fc.button.revert".translate(),147,20,{ manager.hasChanges() }, { manager.revert() }), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("restore",ActiveButtonWidget("fc.button.restore".translate(),147,20,{ manager.hasRestores("") }, { manager.restore("") }), Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
            .addDoneButton()
            .onClose { manager.apply(true); if(manager.hasChanges()) setAndUpdate(newThing) }
            .build()
        PopupWidget.push(popup)
    }

    @Environment(EnvType.CLIENT)
    private class ValidatedObjectUpdateManager<T: Walkable>(private val thing: T, private val key: String): BaseUpdateManager(){

        private val updatableEntries: MutableMap<String, Updatable> = mutableMapOf()

        fun setUpdatableEntry(entry: Updatable) {
            updatableEntries[entry.getEntryKey()] = entry
        }

        fun pushUpdatableStates(){
            for (updatable in updatableEntries.values){
                updatable.pushState()
            }
        }

        override fun restoreCount(scope: String): Int {
            var count = 0
            for ((_, updatable) in updatableEntries){
                if(updatable.isDefault()) continue
                count++
            }
            return count
        }

        override fun restore(scope: String) {
            for ((_, updatable) in updatableEntries){
                updatable.restore()
            }
        }

        override fun apply(final: Boolean) {
            if (updateMap.isEmpty()) return
            //push updates from basic validation to the configs

            ConfigApiImpl.walk(thing,key,true) { walkable,_,new,thing,prop,_ ->
                if (!(thing is Updatable && thing is Entry<*, *>)){
                    val update = getUpdate(new)
                    if (update != null && update is Supplier<*>){
                        try {
                            prop.setter.call(walkable, update.get())
                        } catch (e: Exception){
                            FC.LOGGER.error("Error pushing update to simple property [$new]")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}
