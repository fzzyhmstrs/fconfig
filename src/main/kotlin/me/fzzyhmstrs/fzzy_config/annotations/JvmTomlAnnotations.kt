@file:OptIn(ExperimentalSerializationApi::class)
package me.fzzyhmstrs.fzzy_config.annotations

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import net.peanuuutz.tomlkt.TomlConfigBuilder
import net.peanuuutz.tomlkt.TomlInteger

/**
 * Java field-friendly version of [TomlComment][net.peanuuutz.tomlkt.TomlComment]
 *
 * Adds comments to corresponding property.
 *
 * ```kotlin
 * class IntData(
 *     @TomlComment("""
 *         An integer,
 *         but is decoded into Long originally
 *     """)
 *     val int: Int
 * )
 * IntData(10086)
 * ```
 *
 * will produce:
 *
 * ```toml
 * # An integer,
 * # but is decoded into Long originally
 * int = 10086
 * ```
 *
 * @property value the comment text, could be multiline.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@SerialInfo
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
public annotation class Comment(val value: String)

/**
 * Java field-friendly version of [TomlInline][net.peanuuutz.tomlkt.TomlInline]
 *
 * Forces the annotated array-like or table-like property to be a one-liner.
 *
 * ```kotlin
 * class Data(
 *     @TomlInline
 *     val inlineProperty: Map<String, String>,
 *     val noInlineProperty: Map<String, String>
 * )
 * val data = mapOf("a" to "something", "b" to "another thing")
 * Data(data, data)
 * ```
 *
 * will produce:
 *
 * ```toml
 * inlineProperty = { a = "something", b = "another thing" }
 *
 * [noInlineProperty]
 * a = "something"
 * b = "another thing"
 * ```
 *
 * Without `@TomlInline`, both of these two properties will act like how
 * `noInlineProperty` behaves.
 */
@SerialInfo
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class Inline

/**
 * Java field-friendly version of [TomlBlockArray][net.peanuuutz.tomlkt.TomlBlockArray]
 *
 * Modifies the encoding process of corresponding array-like property, either to
 * force array of table to be encoded as block array, or to change how many
 * items should be encoded per line (this will override the default
 * [config][TomlConfigBuilder.itemsPerLineInBlockArray]).
 *
 * Note that, if the annotated property is also marked with [Inline], this
 * annotation will not take effect.
 *
 * ```kotlin
 * class NullablePairList<F, S>(
 *     @TomlBlockArray(2)
 *     val list: List<Pair<F, S>?>
 * )
 * NullablePairList(listOf(Pair("key", 1), null, Pair("key", 3), Pair("key", 4)))
 * ```
 *
 * will produce:
 *
 * ```toml
 * list = [
 *     { first = "key", second = 1 }, null,
 *     { first = "key", second = 3 }, { first = "key", second = 4 }
 * ]
 * ```
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@SerialInfo
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
public annotation class BlockArray(val itemsPerLine: Int = 1)

/**
 * Java field-friendly version of [TomlMultilimeString][net.peanuuutz.tomlkt.TomlMultilineString]
 *
 * Marks the annotated [String] property as multiline when encoded.
 *
 * ```kotlin
 * class MultilineStringData(
 *     @TomlMultilineString
 *     val multilineString: String
 * )
 * MultilineStringData("""
 *     Do, a deer, a female deer.
 *     Re, a drop of golden sun.
 * """.trimIndent())
 * ```
 *
 * will produce:
 *
 * ```toml
 * multilineString = """
 * Do, a deer, a female deer.
 * Re, a drop of golden sun."""
 * ```
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@SerialInfo
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class MultilineString

/**
 * Java field-friendly version of [TomlLiteralString][net.peanuuutz.tomlkt.TomlLiteralString]
 *
 * Marks the annotated [String] property as literal when encoded.
 *
 * ```kotlin
 * class LiteralStringData(
 *     @TomlLiteralString
 *     val literalString: String
 * )
 * LiteralStringData("C:\\Users\\<User>\\.m2\\repositories")
 * ```
 *
 * will produce:
 *
 * ```toml
 * literalString = 'C:\Users\<User>\.m2\repositories'
 * ```
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@SerialInfo
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
public annotation class LiteralString

/**
 * Java field-friendly version of [TomlInteger][net.peanuuutz.tomlkt.TomlInteger]
 *
 * Changes the representation of the annotated [Byte], [Short], [Int], [Long]
 * property.
 *
 * ```kotlin
 * class ByteCode(
 *     @TomlInteger(
 *         base = TomlInteger.Base.Hex,
 *         group = 2
 *     )
 *     val code: Int
 * )
 * ByteCode(0xFFE490)
 * ```
 *
 * will produce:
 *
 * ```toml
 * code = 0xFF_E4_90
 * ```
 *
 * @property group the size of a digit group separated by '_'. If set to 0, the
 * digits will not be grouped.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@SerialInfo
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
public annotation class Integer(
    val base: TomlInteger.Base = TomlInteger.Base.Dec,
    val group: Int = 0
)