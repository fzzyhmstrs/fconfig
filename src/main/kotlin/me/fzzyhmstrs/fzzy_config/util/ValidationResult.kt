/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.util

import com.mojang.serialization.DataResult
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.config.ConfigContext
import org.slf4j.Logger
import org.slf4j.event.Level
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

/**
 * A result of any type T that is wrapped with an optional error message
 *
 * Used to provide contextual error information "upstream" of where the error was encountered, which allows for better logging where upstream elements can collate error messages passed back to them into an organized and sensible error log.
 *
 * This class has a private constructor to force the use of the pre-defined static instantiation methods
 * @param T result type, can be nullable
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidationResult<T> private constructor(private val storedVal: T, private val errorContext: ErrorEntry = NonErrorEntry) {
    /**
     * Boolean check to determine if this result is valid (no errors)
     *
     * @return Boolean, true is NOT an error, false if there is an error.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun isValid(): Boolean {
        return errorContext.isEmpty()
    }

    /**
     * Boolean check to determine if this result is holding an error
     *
     * @return Boolean, true is an error, false not.
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    fun isError(): Boolean {
        return errorContext.isError()
    }

    /**
     * Supplies the error message stored within
     * @return String, the error message stored or "No Error" if there is no error entry
     * @author fzzyhmstrs
     * @since 0.1.0, deprecated 0.7.0
     */
    @Deprecated("Replace with logging or inspection of the underlying ErrorEntry in most situations")
    fun getError(): String {
        return errorContext.getString()
    }

    /**
     * Supplies the error context stored within, if any
     * @return [ErrorContext], or null if this isn't an errored result
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    @Deprecated("Replace with logging or inspection of the underlying ErrorEntry in most situations")
    fun getErrorEntry(): ErrorEntry {
        return errorContext
    }

    /**
     * Inspects the error of the result, if any, for errors of a particular type
     * @param C error content type
     * @param t [ErrorContext.Type]&lt;[C]&gt; the error type. If you don't need a particular type (to report errors for example), use the other overload
     * @param c [Consumer]&lt;[ErrorContext.Entry]&lt;[C]&gt;&gt; accepts matching entries
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun <C: Any> inspect(t: ErrorEntry.Type<C>, c: Consumer<ErrorEntry.Entry<C>>) {
        errorContext.consumeType(t, c)
    }

    /**
     * Inspects the error of the result, if any, for errors of a particular type
     * @param c [Consumer]&lt;[ErrorContext.Entry]&gt; accepts all entries within the results ErrorEntry
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun inspect(c: Consumer<ErrorEntry.Entry<*>>) {
        errorContext.consumeAll(c)
    }

    /**
     * Gets the wrapped result value
     *
     * @return T. The result being wrapped and passed by this ValidationResult.
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    fun get(): T {
        return storedVal
    }

    /**
     * Writes an error log to console if this validation result is errored
     *
     * @param errors List<String> of secondary errors to add to the log
     * @author fzzyhmstrs
     * @since 0.1.0, deprecated 0.7.0
     */
    @Deprecated("Use the new overloads that consume strings and pass the appropriate logger consumer in")
    fun writeError(errors: List<String>) {
        if (!isError())return
        FC.LOGGER.error(">>>>>>>>>>>>>>>")
        FC.LOGGER.error(getError())
        FC.LOGGER.error(">>>>>>>>>>>>>>>")
        for (e in errors) {
            FC.LOGGER.error(e)
        }
        FC.LOGGER.error(">>>>>>>>>>>>>>>")
    }
    /**
     * Writes a warning log to console if this validation result is errored
     *
     * @param errors List<String> of secondary warning to add to the log
     * @author fzzyhmstrs
     * @since 0.1.0, deprecated 0.7.0
     */
    @Deprecated("Use the new overloads that consume strings and pass the appropriate logger consumer in")
    fun writeWarning(errors: List<String>) {
        if (!isError())return
        FC.LOGGER.warn(">>>>>>>>>>>>>>>")
        FC.LOGGER.warn(getError())
        FC.LOGGER.warn(">>>>>>>>>>>>>>>")
        for (e in errors) {
            FC.LOGGER.warn(e)
        }
        FC.LOGGER.warn(">>>>>>>>>>>>>>>")
    }

    /**
     * Log this result if it is errored.
     * @param writer [BiConsumer]&lt;String, Throwable?&gt; consumer of logging information for printing to console. See the various helper methods
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun log(writer: BiConsumer<String, Throwable?>) {
        if (!isError())return
        writer.accept(">>>>>>>>>>>>>>>", null)
        errorContext.log(writer)
        writer.accept(">>>>>>>>>>>>>>>", null)
    }

    /**
     * Log this result if it is errored using the standard Fzzy Config logger.
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun log() {
        log(ErrorEntry.ENTRY_LOGGER_CONSUMER)
    }

    /**
     * TODO()
     * @author fzzyhmstrs
     * @since 0.7.0
     */
     @JvmDefaultWithoutCompatibility
    sealed interface ErrorEntry {
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun isError(): Boolean
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun isEmpty(): Boolean = false
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun isCritical(): Boolean
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun addError(other: ErrorEntry): ErrorEntry
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun  <C: Any> addError(type: Type<C>, builder: UnaryOperator<Builder>): ErrorEntry
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun  addError(builder: UnaryOperator<Builder<String>>): ErrorEntry {
            return addError(BASIC, builder)
        }
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <C: Any> consumeType(t: Type<C>, c: Consumer<Entry<C>>) {
            for (entry in entries.filter { it.type == t }) {
                (entry as? Entry<C>)?.let { c.accept(it) }
            }
        }
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun consumeAll(c: Consumer<Entry<*>>) {
            for (entry in entries) {
                c.accept(entry)
            }
        }
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun getString(): String
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun log(writer: BiConsumer<String, Throwable?>) {
            writer.accept(getString(), null)
        }

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        class Type<C: Any>(val name: String, val isString: Boolean = true, val isError: Boolean = true) {
            fun create(content: C, e: Throwable?, msg: String): ErrorEntry {
                return 
            }
        }

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        sealed interface Entry<C: Any> {
            val type: Type<C>
            val content: C
            val e: Throwable?
        }
        
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        class Builder<C>(private val type: Type<C> = BASIC) {
            private var header: String = ""
            private var content: C? = null
            private var e: Throwable? = null
            private val msg: String = ""

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun header(header: String): Builder<C> {
                this.header = header
                return this
            }

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun content(content: C): Builder<C> {
                this.content = content
                return this
            }

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun exception(e: Throwable): Builder<C> {
                this.e = e
                return this
            }

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun message(msg: String): Builder<C> {
                this.msg = msg
                return this
            }

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun build(): ErrorEntry {
                return if (content == null) {
                    if (msg.isNotEmpty() && type.isString) {
                        FC.DEVLOG.warn("String-type ErrorEntry built with message() instead of content()"
                        val entry = SingleErrorEntry(type as Type<String>, msg, e)
                        if (header.isNotEmpty()) {
                            EmptyErrorEntry(header).addError(entry)
                        } else {
                            entry
                        }
                    } else {
                        EmptyErrorEntry(header)
                    }
                } else {
                    val entry = SingleErrorEntry(type, content, e, msg)
                    if (header.isNotEmpty()) {
                        EmptyErrorEntry(header).addError(entry)
                    } else {
                        entry
                    }
                }
            }
            
        }

        companion object {
            val BASIC = Type<String>("Basic Error")
            val SERIALIZATION = Type<String>("Serialization Error")
            val DESERIALIZATION = Type<String>("Deserialization Error")
            val OUT_OF_BOUNDS = Type<String>("Value Out of Bounds")
            val FILE_STRUCTURE = Type<String>("File Structure Problem")
            val RESTART = Type<Action>("Restart Required", false, false)
            val ACTION = Type<Action>("Action Required", false, false)

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            val LOGGER_CONSUMER: Consumer<Entry<*>> = createLogger(FC.LOGGER)

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            val ENTRY_LOGGER_CONSUMER: BiConsumer<String, Throwable?> = createEntryLogger(FC.LOGGER)

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            @JvmStatic
            fun createLogger(logger: Logger, level: Level = Level.WARN, errorsOnly: Boolean = false): Consumer<Entry<*>> {
                val entryConsumer = createEntryLogger(logger, level)
                return Consumer { e ->
                    if (errorsOnly && e.type.isError || !errorsOnly)
                        e.print(entryConsumer)
                }
            }

            /**
             * Basic entry print consumer that is used to print entries to a console.
             * @param logger [Logger] logger used to print to console.
             * @param level [Level], default [Level.WARN]. The logging level to use.
             * @return [BiConsumer]&lt;String, Throwable?&gt; resulting bi-consumer for printing Entries
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            @JvmStatic
            fun createEntryLogger(logger: Logger, level: Level = Level.WARN): BiConsumer<String, Throwable?> {
                return when(level) {
                    Level.ERROR -> BiConsumer { s, e ->
                        if (e != null) { logger.error(s, e) } else { logger.error(s) }
                    }
                    Level.WARN -> BiConsumer { s, e ->
                        if (e != null) { logger.warn(s, e) } else { logger.warn(s) }
                    }
                    Level.INFO -> BiConsumer { s, e ->
                        if (e != null) { logger.info(s, e) } else { logger.info(s) }
                    }
                    Level.DEBUG -> BiConsumer { s, e ->
                        if (e != null) { logger.debug(s, e) } else { logger.debug(s) }
                    }
                    Level.TRACE -> BiConsumer { s, e ->
                        if (e != null) { logger.trace(s, e) } else { logger.trace(s) }
                    }
                }
            }

            fun empty(header: String = ""): ErrorEntry {
                return EmptyErrorEntry(header)
            }

            fun basic(error: String): ErrorEntry {
                return SingleErrorEntry(BASIC, error)
            }

            fun <C: Any> builder(type: Type<C> = BASIC): Builder<C> {
                return Builder(type)
            }
        }
    }

    private object NonErrorEntry(): ErrorEntry {
        override fun isError(): Boolean = false
        override fun isEmpty(): Boolean = true
        override fun isCritical(): Boolean = false
        override fun addError(other: ErrorEntry): ErrorEntry {
            return other
        }
        override fun <C: Any> addError(type: Type<C>, builder: UnaryOperator<Builder>): ErrorEntry {
            return builder.apply(Builder(type)).build()
        }
        override fun <C: Any> consumeType(t: Type<C>, c: Consumer<Entry<C>>) {
        }
        override fun consumeAll(c: Consumer<Entry<*>>) {
        }
        override fun getString(): String {
            return "No Error"
        }
    }
    
    private class EmptyErrorEntry(private val header: String = ""): ErrorEntry {
        override fun isError(): Boolean = false
        override fun isEmpty(): Boolean = true
        override fun isCritical(): Boolean = false
        override fun addError(other: ErrorEntry): ErrorEntry {
            return if (other.isEmpty()) {
                this            
            } else if (header.isEmpty()) {
                other
            } else {
                CompoundErrorEntry(this, other)
            }
        }
        override fun <C: Any> addError(type: Type<C>, builder: UnaryOperator<Builder>): ErrorEntry {
            return if (header.isEmpty()) {
                builder.apply(Builder(type)).build()
            } else {
                CompoundErrorEntry(this, builder.apply(Builder(type)).build())
            }
        }
        override fun <C: Any> consumeType(t: Type<C>, c: Consumer<Entry<C>>) {
        }
        override fun consumeAll(c: Consumer<Entry<*>>) {
        }
        override fun getString(): String {
            return if (header.isEmpty()) "Empty Error" else header
        }
    }

    private class <T: Any> SingleErrorEntry(
        override val type: Type<T>
        override val content: T
        override val e: Throwable? = null,
        private val msg: String = ""): ErrorEntry, ErrorEntry.Entry<T> 
    {
        override fun isError(): Boolean {
            return type.isError
        }
        override fun isCritical(): Boolean {
            return e != null
        }
        override fun addError(other: ErrorEntry): ErrorEntry {
            return if (other.isEmpty()) {
                this            
            } else {
                CompoundErrorEntry(this, other)
            }
        }
        override fun <C: Any> addError(type: Type<C>, builder: UnaryOperator<Builder>): ErrorEntry {
            return CompoundErrorEntry(this, builder.apply(Builder(type)).build())
        }
        override fun <C: Any> consumeType(t: Type<C>, c: Consumer<ErrorEntry.Entry<C>>) {
            if (t != type) return
            (this as? ErrorEntry.Entry<C>)?.let{ c.accept(it) }
        }
        override fun consumeAll(c: Consumer<ErrorEntry.Entry<*>>) {
            c.accept(this)
        }
        override fun getString(): String {
            val crit = if (e != null) "Critical " else ""
            return if (msg.isNotEmpty()) 
                "$crit${type.name}: $msg ($content)"
            else
                "$crit${type.name}: $content"
        }
        override fun log(writer: BiConsumer<String, Throwable?>) {
            writer.accept(getString(), e)
        }
    }

    private class CompoundErrorEntry(private val headerEntry: ErrorEntry, private val children: MutableList<ErrorEntry>): ErrorEntry {

        constructor(headerEntry: ErrorEntry, childEntry: ErrorEntry): this(headerEntry, mutableListOf(childEntry))
        
        override fun isError(): Boolean {
            return headerEntry.isError() || children.any { it.isError() }
        }
        override fun isCritical(): Boolean {
            return headerEntry.isCritical() || children.any { it.isCritical() }
        }
        override fun addError(other: ErrorEntry): ErrorEntry {
            if (!other.isEmpty()) {
                children.add(other)
            }
            return this
        }
        override fun <C: Any> addError(type: Type<C>, builder: UnaryOperator<Builder>): ErrorEntry {
            val other = builder.apply(Builder(type)).build()
            returh addError(other)
        }
        override fun <C: Any> consumeType(t: Type<C>, c: Consumer<Entry<C>>) {
            headerEntry.consumeType(t, c)
            for (entry in children) {
                entry.consumeType(t, c)
            }
        }
        override fun consumeAll(c: Consumer<Entry<*>>) {
            headerEntry.consumeAll(c)
            for (entry in children) {
                entry.consumeAll(c)
            }
        }
        override fun getString(): String {
            val list: MutableList<String> = mutableListOf()
            list.add(headerEntry.getString())
            for (entry in children) {
                list.add(entry.getString())
            }
        }
        override fun log(writer: BiConsumer<String, Throwable?>) {
            headEntry.log(writer)
            if (children.isEmpty()) return
            val consumer: BiConsumer<String, Throwable?> = BiConsumer { str, e ->
                writer.accept("  $str", e)
            }
            for (entry in children) {
                entry.log(consumer)
            }
        }
    }
    
    companion object {
        /**
         * Creates a successful validation result.
         *
         * No error message needed as no errors were found.
         * @param T Type of result
         * @param storedVal result instance of type T
         * @return the successful ValidationResult
         * @author fzzyhmstrs
         * @since 0.1.0
         */
        fun <T> success(storedVal: T): ValidationResult<T> {
            return ValidationResult(storedVal)
        }

        /**
         * Create a validation result with this if there was a problem during validation.
         *
         * In this case, typically, [storedVal] will be the default value associated with this validation. A valid instance of T must always be passed back. Add a descriptive error message to [error]. If there is no default, you will want to make your result type nullable and pass back null
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param error string with error message
         * @return the errored ValidationResult
         * @author fzzyhmstrs
         * @since 0.1.0, deprecated 0.7.0
         */
        @Deprecated("Plain error strings are deprecated but acceptable. Most of the time passing in ErrorContext is preferred")
        fun <T> error(storedVal: T, error: String): ValidationResult<T> {
            return ValidationResult(storedVal, ErrorEntry.basic(error))
        }

        /**
         * Create a validation result with this if there was a problem during validation.
         *
         * In this case, typically, [storedVal] will be the default value associated with this validation. A valid instance of T must always be passed back. Add a descriptive error message to [errorContext]. If there is no default, you will want to make your result type nullable and pass back null
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param errorEntry [ErrorEntry] the error to pass with this result
         * @return the errored ValidationResult
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T> error(storedVal: T, errorEntry: ErrorEntry): ValidationResult<T> {
            return ValidationResult(storedVal, errorEntry)
        }

        /**
         * Create a validation result with this if there was a problem during validation.
         *
         * In this case, typically, [storedVal] will be the default value associated with this validation. A valid instance of T must always be passed back. Add a descriptive error message to [errorContext]. If there is no default, you will want to make your result type nullable and pass back null
         * @param T Type of result
         * @param C Type of error content to store. This is usually a string message
         * @param storedVal default or fallback instance of type T
         * @param type [ErrorEntry.Type]&lt;[C]&gt; the error type. When in doubt, use [ErrorContext.BASIC]
         * @param TODO()
         * @return the errored ValidationResult
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T, C: Any> error(storedVal: T, type: ErrorEntry.Type<C>, builder: UnaryOperator<Builder<C>>): ValidationResult<T> {
            return ValidationResult(storedVal, builder.apply(ErrorEntry.Builder(type)).build())
        }

        /**
         * Convenience shortcut for creating a success or error depending on a boolean state.
         *
         * Used if the value returned will be the same regardless of validation, e.g. in the case of [EntryValidator][me.fzzyhmstrs.fzzy_config.entry.EntryValidator] usage, where no changes are being made to the result
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param valid test applied to determine validation or error.
         * @param error string with error message
         * @return the error ValidationResult
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.7.0
         */
        @Deprecated("Plain error strings are deprecated but acceptable. Most of the time passing in ErrorContext is preferred")
        fun <T> predicated(storedVal: T, valid: Boolean, error: String): ValidationResult<T> {
            return if(valid) ValidationResult(storedVal) else ValidationResult(storedVal, ErrorEntry.basic(error))
        }

        /**
         * Convenience shortcut for creating a success or error depending on a boolean state.
         *
         * Used if the value returned will be the same regardless of validation, e.g. in the case of [EntryValidator][me.fzzyhmstrs.fzzy_config.entry.EntryValidator] usage, where no changes are being made to the result
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param valid test applied to determine validation or error.
         * @param error [ErrorEntry] providing information about the possible error
         * @return the error ValidationResult
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.7.0
         */
        @Deprecated("Plain error strings are deprecated but acceptable. Most of the time passing in ErrorContext is preferred")
        fun <T> predicated(storedVal: T, valid: Boolean, error: ErrorEntry): ValidationResult<T> {
            return if(valid) ValidationResult(storedVal) else ValidationResult(storedVal, error)
        }

        /**
         * Convenience shortcut for creating a success or error depending on a boolean state.
         *
         * Used if the value returned will be the same regardless of validation, e.g. in the case of [EntryValidator][me.fzzyhmstrs.fzzy_config.entry.EntryValidator] usage, where no changes are being made to the result
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param valid test applied to determine validation or error.
         * @param error supplier of strings with an error message
         * @return the error ValidationResult
         * @author fzzyhmstrs
         * @since 0.6.9, deprecated 0.7.0
         */
        @Deprecated("Plain error strings are deprecated but acceptable. Most of the time passing in ErrorContext is preferred")
        fun <T> predicated(storedVal: T, valid: Boolean, error: Supplier<String>): ValidationResult<T> {
            return if(valid) ValidationResult(storedVal) else ValidationResult(storedVal, ErrorEntry.basic(error.get()))
        }

        /**
         * Convenience shortcut for creating a success or error depending on a boolean state.
         *
         * Used if the value returned will be the same regardless of validation, e.g. in the case of [EntryValidator][me.fzzyhmstrs.fzzy_config.entry.EntryValidator] usage, where no changes are being made to the result
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param valid test applied to determine validation or error.
         * @param errorContext supplier of error contexts
         * @return the error ValidationResult
         * @author fzzyhmstrs
         * @since 0.6.9
         */
        fun <T> predicated(storedVal: T, valid: Boolean, type: ErrorEntry.Type<C>, builder: UnaryOperator<Builder<C>>): ValidationResult<T> {
            return if(valid) ValidationResult(storedVal) else ValidationResult(storedVal, builder.apply(ErrorEntry.Builder(type)).build())
        }

        /**
         * Converts a [DataResult] into a [ValidationResult]
         * @param T the data type
         * @param result [DataResult] the parsed data result from a Codec or other data source.
         * @param fallback [T] the value to pass in case data parsing has failed
         * @return a non-null validation result with the parsed or fallback value and error message as appropriate
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        fun <T> mapDataResult(result: DataResult<T>, fallback: T): ValidationResult<T> {
            return result.mapOrElse({ r -> success(r) }, { e -> error(fallback, e.message()) })
        }

        /**
         * Converts a [DataResult] into a [ValidationResult] with no fallback
         * @param T the data type
         * @param result [DataResult] the parsed data result from a Codec or other data source.
         * @return a nullable-result validation result with the parsed value or null and error message as appropriate
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        fun <T> mapDataResult(result: DataResult<T>): ValidationResult<T?> {
            return result.mapOrElse({ r -> success(r) }, { e -> error(null, e.message()) })
        }

        /**
         * Creates a new ValidationResult of type T wrapping the new value with the error(if any) from the receiver ValdiationResult (of any type, does not need to match T)
         *
         * Useful if the Validation is performed on an incompatible type, and the error needs to be passed along with type T.
         * @param T type of result
         * @param newVal the new value to wrap in the existing result
         * @return ValidationResult with the state and error of the previous result with the newly supplied Type and Value
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun <T> ValidationResult<*>.wrap(newVal: T): ValidationResult<T> {
            return ValidationResult(newVal, this.errorContext)
        }

        /**
         * Adds another test, and potentially another error, to a Validation.
         * @param newTest Boolean result of another validation test
         * @param error error message if the newTest fails validation
         * @return ValidationResult with this stored value, possibly a new error state and new error message
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @Deprecated("Plain error strings are deprecated but acceptable. Most of the time passing in ErrorContext is preferred")
        fun <T> ValidationResult<T>.also(newTest: Boolean, error: String): ValidationResult<T> {
            TODO()
            /*if (!newTest) {
                val thisError = errorContext
                if (thisError == null) {
                    return error(storedVal, ErrorContext.createBasic(error))
                } else {
                    thisError.addError(ErrorContext.createBasic(error))
                }
            }
            return this*/
        }

        /**
         * reports error, if any, to a provided string list
         * @param errorBuilder MutableList&lt;String&gt; for appending errors.
         * @return ValidationResult returns itself
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.7.0
         */
        @Deprecated("Errors should be reported with log or the overload that takes a biConsumer")
        fun <T> ValidationResult<T>.report(errorBuilder: MutableList<String>): ValidationResult<T> {
            val consumer: BiConsumer<String, Throwable?> = BiConsumer { s, _ ->
                errorBuilder.add(s)
            }
            errorContext.log(consumer)
            return this
        }

        /**
         * reports error, if any, to a provided reporter (such as a logger)
         * @param errorReporter Consumer&lt;String&gt; for reporting errors.
         * @return ValidationResult returns itself
         * @author fzzyhmstrs
         * @since 0.5.9, deprecated 0.7.0
         */
        @Deprecated("Errors should be reported with log or the overload that takes a biConsumer")
        fun <T> ValidationResult<T>.report(errorReporter: Consumer<String>): ValidationResult<T> {
            val consumer: BiConsumer<String, Throwable?> = BiConsumer { s, _ ->
                errorReporter.accept(s)
            }
            errorContext.log(consumer)
            return this
        }

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.?.?
         */
        fun <T> ValidationResult<T>.report(errorReporter: BiConsumer<String, Throwable?>): ValidationResult<T> {
            errorContext.log(errorReporter)
            return this
        }

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.?.?
         */
        fun <N, T> ValidationResult<T>.map(to: Function<T, out N>): ValidationResult<N> {
            return ValidationResult(to.apply(this.storedVal), this.errorContext)
        }

        @Deprecated("For removal")
        fun <T: Any> ValidationResult<ConfigContext<T>>.contextualize(): ValidationResult<T> {
            return this.wrap(this.get().config)
        }
    }
}
