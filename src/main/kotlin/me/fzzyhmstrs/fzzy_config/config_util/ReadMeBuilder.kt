package me.fzzyhmstrs.fzzy_config.config_util

import me.fzzyhmstrs.fzzy_config.FC
import java.util.function.Function
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

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

    override fun readmeText(): List<String>{
        return readMeList
    }

    open fun writeReadMe(){
        writeReadMe(file, base)
    }

    open fun build(indent: Int = 0): List<String>{
        readMeList.addAll(headerText.provideHeader())
        val fields = this::class.java.declaredFields
        val orderById = fields.withIndex().associate { it.value.name to it.index }
        for (it in this.javaClass.kotlin.declaredMemberProperties.sortedBy { orderById[it.name] }){
            if (it is KMutableProperty<*>) {
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
                        readMeList.add(readMeLineDecorator(AcText.translatable(translate).string, it.name, indent))
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

    open fun readMeLineDecorator(rawLine: String, propName: String, indent: Int): String{
        return decorator.decorate(rawLine, propName, indent)
    }

    open fun addToReadMe(list: List<String>){
        readMeList.addAll(list)
    }

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
            AcText.translatable(key).string
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
