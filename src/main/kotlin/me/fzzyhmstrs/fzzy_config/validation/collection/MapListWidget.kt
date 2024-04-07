package me.fzzyhmstrs.fzzy_config.validation.collection

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.widget.TextlessConfigActionWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.gui.widget.ElementListWidget
import java.util.function.BiFunction
import java.util.function.Function
import me.fzzyhmstrs.fzzy_config.entry.Entry as Entry1

@Environment(EnvType.CLIENT)
internal class MapListWidget<K,V>(
    entryMap: Map<Entry1<K, *>,Entry1<V, *>>,
    keySupplier: Entry1<K, *>,
    valueSupplier: Entry1<V, *>,
    entryValidator: BiFunction<MapListWidget<K,V>,MapEntry<K,V>?,ChoiceValidator<K>>)
    :
    ElementListWidget<MapListWidget.MapEntry<K,V>>(MinecraftClient.getInstance(), 272, 160, 0, 20) {

    fun getRawMap(skip: MapEntry<K,V>? = null): Map<K,V>{
        val map: MutableMap<K,V> = mutableMapOf()
        for (e in this.children()){
            if (e !is ExistingEntry<K,V>) continue
            if (e == skip) continue
            val pair = e.get()
            map[pair.first] = pair.second
        }
        return map.toMap()
    }

    fun getMap(): Map<K,V>{
        val map: MutableMap<K,V> = mutableMapOf()
        for (e in this.children()){
            if (e !is ExistingEntry<K,V>) continue
            if (!e.isValid) continue
            val pair = e.get()
            map[pair.first] = pair.second
        }
        return map.toMap()
    }

    override fun drawHeaderAndFooterSeparators(context: DrawContext?) {
    }

    override fun drawMenuListBackground(context: DrawContext?) {
    }

    override fun getRowWidth(): Int {
        return 248 //16 padding, 20 slider width and padding
    }

    override fun method_57718(): Int {
        return this.x + this.width / 2 + this.rowWidth / 2 + 6
    }

    private fun makeVisible(entry: MapEntry<K,V>){
        this.ensureVisible(entry)
    }

    init{
        for (e in entryMap){
            this.addEntry(ExistingEntry(e.key,e.value,this,entryValidator))
        }
        this.addEntry(NewEntry(keySupplier,valueSupplier,this,entryValidator))
    }

    private class ExistingEntry<K,V>(private val key: me.fzzyhmstrs.fzzy_config.entry.Entry<K, *>,private val value: me.fzzyhmstrs.fzzy_config.entry.Entry<V, *>, private val parent: MapListWidget<K,V>, keyValidator: BiFunction<MapListWidget<K,V>,MapEntry<K,V>?,ChoiceValidator<K>>): MapEntry<K,V>() {

        private val keyWidget = key.widgetEntry(keyValidator.apply(parent,this))
        private val valueWidget = value.widgetEntry(ChoiceValidator.any())
        private val deleteWidget = TextlessConfigActionWidget(
            TextureIds.DELETE,
            TextureIds.DELETE_INACTIVE,
            TextureIds.DELETE_HIGHLIGHTED,
            TextureIds.DELETE_LANG,
            TextureIds.DELETE_LANG,
            { true },
            { parent.children().let { list ->
                list.indexOf(this).takeIf { i -> i >=0 && i<list.size }?.let {
                        i -> list.removeAt(i)
                }
            } })

        fun get(): Pair<K,V>{
            return Pair(key.get(),value.get())
        }

        override fun children(): MutableList<out Element> {
            return mutableListOf(keyWidget,valueWidget, deleteWidget)
        }

        override fun selectableChildren(): MutableList<out Selectable> {
            return mutableListOf(keyWidget,valueWidget, deleteWidget)
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
            if (this.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && keyWidget.tooltip != null){
                MinecraftClient.getInstance().currentScreen?.setTooltip(keyWidget.tooltip, HoveredTooltipPositioner.INSTANCE,this.isFocused)
            }
            keyWidget.setPosition(x,y)
            keyWidget.render(context, mouseX, mouseY, tickDelta)
            valueWidget.setPosition(x+114,y)
            valueWidget.render(context, mouseX, mouseY, tickDelta)
            deleteWidget.setPosition(x+228,y)
            deleteWidget.render(context, mouseX, mouseY, tickDelta)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class NewEntry<K,V>(private val keySupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<K, *>,private val valueSupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<V, *>, private val parent: MapListWidget<K,V>, private val validator: BiFunction<MapListWidget<K,V>,MapEntry<K,V>?,ChoiceValidator<K>>): MapEntry<K,V>() {

        private val addWidget = TextlessConfigActionWidget(
            TextureIds.ADD,
            TextureIds.ADD_INACTIVE,
            TextureIds.ADD_HIGHLIGHTED,
            TextureIds.ADD_LANG,
            TextureIds.ADD_LANG,
            { true },
            {
                parent.children().let { it.add(it.lastIndex,ExistingEntry(keySupplier.instanceEntry() as Entry1<K,*>,valueSupplier.instanceEntry() as Entry1<V,*>,parent,validator)) }
                parent.makeVisible(this)
            })


        override fun children(): MutableList<out Element> {
            return mutableListOf(addWidget)
        }

        override fun selectableChildren(): MutableList<out Selectable> {
            return mutableListOf(addWidget)
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
            addWidget.setPosition(x+228,y)
            addWidget.render(context, mouseX, mouseY, tickDelta)
        }
    }

    abstract class MapEntry<K,V>: Entry<MapEntry<K,V>>(){
        var isValid = true
    }

    internal class ExcludeSelfChoiceValidator<K,V>(private val self: MapEntry<K,V>?, private val disallowed: Function<MapEntry<K,V>?, Map<K,V>>) : ChoiceValidator<K>(
        ValuesPredicate(null,null)
    ) {
        override fun validateEntry(input: K, type: EntryValidator.ValidationType): ValidationResult<K> {
            if (self == null) return ValidationResult.success(input)
            return ValidationResult.predicated(input, !disallowed.apply(self).containsKey(input), "No duplicate map keys").also { self.isValid = it.isValid() }
        }

    }
}