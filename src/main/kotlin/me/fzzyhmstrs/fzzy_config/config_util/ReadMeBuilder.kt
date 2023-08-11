package me.fzzyhmstrs.fzzy_config.config_util

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.interfaces.ReadMeTextProvider
import me.fzzyhmstrs.fzzy_config.interfaces.ReadMeWriter
import java.util.function.Function
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

/**
 * A class that extends ReadMeBuilder auto-generates and indents a Readme or section of ReadMe via reflection. The top level builder in a builder chain is used as a file writer too.
 *
 * @constructor file: File name for the ReadMe, typically "readme_name.txt"
 *
 * base: the config subfolder the ReadMe is stored in, defaults to "fzzy_config"
 *
 * headerText: A [Header] instance that defines the header for this builder
 *
 * decorator: The [LineDecorating] processor for this ReadMe builder
 *
 * indentIncrement: how much the lines of sub-builders are indented compared to this builder
 *
 * @see ReadMeWriter
 */
open class ReadMeBuilder(
    private val file: String,
    private val base: String = FC.MOD_ID,
    private val headerText: Header = Header(),
    private val decorator: LineDecorating,
    private val indentIncrement: Int = 0)
    :
    ReadMeWriter
{

    private val readMeList: MutableList<String> = mutableListOf()

    /**
     * supplies a list of strings for this builders parent builder or to a file writer.
     */
    override fun readmeText(): List<String>{
        return readMeList
    }

    /**
     * writes the ReadMe to file using the [ReadMeWriter]
     */
    open fun writeReadMe(){
        writeReadMe(file, base)
    }

    /**
     * builds the [readMeList] for passing to it's parent or for later writing. Uses reflection to scrape ReadMe info off of a [ReadMeText] annotation, calls a child ReadMeBuilder, or gets default test from a [ReadMeTextProvider]. Translatable text keys are applied here.
     *
     * @param indent Int. defines how much indenting the [LineDecorator] applies. Each 1 indent is a "tab"
     *
     * @return A List of strings that represent lines in this ReadMe file.
     */
    open fun build(indent: Int = 0): List<String>{
        readMeList.addAll(headerText.provideHeader())
        val fields = this::class.java.declaredFields
        val orderById = fields.withIndex().associate { it.value.name to it.index }
        for (it in this.javaClass.kotlin.declaredMemberProperties.sortedBy { orderById[it.name] }){
            if (it is KMutableProperty<*>  && it.visibility == KVisibility.PUBLIC) {
                val propVal = it.get(this)
                val annotation = it.findAnnotation<ReadMeText>()
                if(annotation != null){
                    val translate = annotation.translationKey
                    val desc = annotation.description
                    val header = annotation.header
                    if(header.isNotEmpty()){
                        readMeList.addAll(header)
                    }
                    if (translate != ""){
                        readMeList.add(readMeLineDecorator(FcText.translatable(translate).string, it.name, indent))
                        continue
                    }
                    if (desc != "") {
                        readMeList.add(readMeLineDecorator(desc, it.name, indent))
                        continue
                    }
                }
                if (propVal is ReadMeBuilder){
                    readMeList.addAll(propVal.build(indent + indentIncrement))
                } else if(propVal is ReadMeTextProvider) {
                    //prioritize an added annotation over the default/builtin readmeText()
                    readMeList.add(readMeLineDecorator(propVal.readmeText(), it.name, indent))
                }
            }
        }
        return readmeText()
    }

    /**
     * decorates a raw input line with formatting, spacing, indenting, etc. By default, applies the builders [LineDecorator] to the supplied inputs. Can be overwritten to perform custom operations not possible with a decorator if needed.
     *
     * @param rawLine String. The raw readme line string, without a title.
     * @param propName String. the name of this readme line, generated from the reflected property name [build] finds.
     * @param indent Int. the amount of indenting to apply via the decorator
     *
     * @return the decorated string
     */
    open fun readMeLineDecorator(rawLine: String, propName: String, indent: Int): String{
        return decorator.decorate(rawLine, propName, indent)
    }

    open fun addToReadMe(list: List<String>){
        readMeList.addAll(list)
    }

    /**
     * A LineDecorator processes a raw readme line, and it's property name into a decorated and arranged version of those strings. LineDecorator is an enum providing default implementations of [LineDecorating] for convenience. ReadMeBuilder accepts any implementation of LineDecorating
     *
     * DEFAULT: used by default in builders. Decorates like " >> propName: This is the description for this line"
     *
     * STAR: decorates like " * propName: This is the description for this line"
     *
     * DOUBLE_SPACED: same decoration as DEFAULT, but double spaces lines
     *
     * BRACKET: decorates like: "....[propName\]: This is the description for this line"
     */
    enum class LineDecorator: LineDecorating{
        DEFAULT{
            override fun decorate(rawLine: String, propName: String, indent: Int): String {
                return "    ".repeat(indent) + " >> $propName: $rawLine"
            }
        },
        STAR{
            override fun decorate(rawLine: String, propName: String, indent: Int): String {
                return "    ".repeat(indent) + " * $propName: $rawLine"
            }
        },
        DOUBLE_SPACED{
            override fun decorate(rawLine: String, propName: String, indent: Int): String {
                return "    ".repeat(indent) + " >> $propName: $rawLine\n"
            }
        },
        BRACKET{
            override fun decorate(rawLine: String, propName: String, indent: Int): String {
                return "....".repeat(indent) + "[$propName]: $rawLine"
            }
        }
    }

    /**
     * Functional interface used by [readMeLineDecorator]
     *
     * SAM: [decorate], takes a raw line, the property name, an indent amount; returns a decorated string
     */
    fun interface LineDecorating{
        fun decorate(rawLine: String, propName: String, indent: Int): String
    }

    class Header (private val components: List<HeaderComponent>){

        constructor(): this(listOf())

        private val list: List<String> by lazy {
            val listTemp: MutableList<String> = mutableListOf()
            for (component in components){
                listTemp.addAll(component.build())
            }
            listTemp
        }

        fun provideHeader(): List<String>{
            return list
        }

        companion object{
            fun default(input: String): Header{
                return Builder().literal().space().underscore(input).build()
            }
        }

        class Builder{
            private val list: MutableList<HeaderComponent> = mutableListOf()
            private var translate = true

            fun build(): Header{
                return Header(list)
            }

            fun translate(): Builder{
                translate = true
                return this
            }
            fun literal(): Builder{
                translate = false
                return this
            }

            fun space(): Builder{
                list.add(LiteralHeaderComponent(""){str -> listOf(str)})
                return this
            }

            fun add(line: String): Builder{
                if (translate){
                    list.add(TranslatableHeaderComponent(line){ str -> listOf(str)})
                } else {
                    list.add(LiteralHeaderComponent(line) { str -> listOf(str) })
                }
                return this
            }

            fun underscore(input: String): Builder{
                if (translate) {
                    list.add(TranslatableHeaderComponent(input){ str -> listOf(str,"-".repeat(str.length))})
                } else {
                    list.add(LiteralHeaderComponent(input){ str -> listOf(str,"-".repeat(str.length))})
                }
                return this
            }

            fun overscore(input: String): Builder{
                if (translate) {
                    list.add(TranslatableHeaderComponent(input){ str -> listOf("-".repeat(str.length),str)})
                } else {
                    list.add(LiteralHeaderComponent(input){ str -> listOf("-".repeat(str.length),str)})
                }
                return this
            }

            fun underoverscore(input: String): Builder{
                if (translate) {
                    list.add(TranslatableHeaderComponent(input){ str -> listOf("-".repeat(str.length),str,"-".repeat(str.length))})
                } else {
                    list.add(LiteralHeaderComponent(input){ str -> listOf("-".repeat(str.length),str,"-".repeat(str.length))})
                }
                return this
            }

            fun box(input: String): Builder{
                if (translate) {
                    list.add(TranslatableHeaderComponent(input){ str -> listOf("#".repeat(str.length+4),"# $str #","#".repeat(str.length+4))})
                } else {
                    list.add(LiteralHeaderComponent(input){ str -> listOf("#".repeat(str.length+4),"# $str #","#".repeat(str.length+4))})
                }
                return this
            }

        }
    }

    class LiteralHeaderComponent(private val input: String, builder: Function<String,List<String>>): HeaderComponent(builder){
        override fun provideInputString(): String {
            return input
        }
    }

    class TranslatableHeaderComponent(private val key: String, builder: Function<String,List<String>>): HeaderComponent(builder){

        private val translation: String by lazy {
            FcText.translatable(key).string
        }

        override fun provideInputString(): String {
            return translation
        }

    }

    abstract class HeaderComponent(private val builder: Function<String,List<String>>){

        abstract fun provideInputString(): String

        fun build(): List<String>{
            return builder.apply(provideInputString())
        }
    }



}
