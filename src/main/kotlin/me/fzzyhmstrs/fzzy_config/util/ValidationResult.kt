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
class ValidationResult<T> private constructor(private val storedVal: T, private val errorContext: ErrorContext? = null) {
    /**
     * Boolean check to determine if this result is valid (no errors)
     *
     * @return Boolean, true is NOT an error, false if there is an error.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun isValid(): Boolean {
        return errorContext == null
    }

    /**
     * Boolean check to determine if this result is holding an error
     *
     * @return Boolean, true is an error, false not.
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    fun isError(): Boolean {
        return errorContext != null
    }

    /**
     * Supplies the error message stored within
     *
     * @return String, the error message stored, or an empty string if no error
     * @author fzzyhmstrs
     * @since 0.1.0, deprecated 0.7.0
     */
    @Deprecated("Replace with inspection of the underlying ErrorContext in most situations")
    fun getError(): String {
        return errorContext?.getString() ?: ""
    }

    /**
     * Supplies the error context stored within, if any
     * @return [ErrorContext], or null if this isn't an errored result
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    @Deprecated("Replace with inspection of the underlying ErrorContext in most situations")
    fun getErrorContext(): ErrorContext? {
        return errorContext
    }

    /**
     * Inspects the error of the result, if any, for errors of a particular type
     * @param C error content type
     * @param t [ErrorContext.Type]&lt;[C]&gt; the error type. If you don't need a particular type (to report errors for example), use the other overload
     * @param c [Consumer]&lt;[ErrorContext.Entry]&lt;[C]&gt;&gt;
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun <C: Any> inspect(t: ErrorContext.Type<C>, c: Consumer<ErrorContext.Entry<C>>) {
        errorContext?.consumeType(t, c)
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
    fun log(writer: BiConsumer<String, Throwable?>, headerMessage: String = "") {
        if (!isError())return
        writer.accept(">>>>>>>>>>>>>>>", null)
        if (headerMessage.isNotEmpty()) {
            writer.accept(headerMessage, null)
            writer.accept(">>>>>>>>>>>>>>>", null)
        }
        errorContext?.log(writer)
        writer.accept(">>>>>>>>>>>>>>>", null)
    }

    /**
     * TODO()
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    class ErrorContext private constructor(private val entries: MutableList<Entry<*>> = mutableListOf()) {

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        constructor(entry: Entry<*>): this(mutableListOf(entry))

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun isError(): Boolean {
            return entries.any { it.type.isError || (it.e != null) }
        }

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <C: Any> addError(type: Type<C>, content: C, e: Throwable? = null, msg: String? = null) {
            entries.add(Entry.of(type, content, e, msg))
        }

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun addError(other: ErrorContext) {
            entries.addAll(other.entries)
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
        fun getString(): String {
            val list: MutableList<String> = mutableListOf()
            consumeAll { entry ->
                entry.print { str, _ -> list.add(str) }
            }
            return list.joinToString("\n")
        }

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun log(writer: BiConsumer<String, Throwable?>) {
            consumeAll { entry ->
                entry.print(writer)
            }
        }


        ////////////////////////////

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        sealed class Entry<C: Any>(val type: Type<C>, val content: C, val e: Throwable?) {

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            open fun print(c: BiConsumer<String, Throwable?>) {
                val crit = if (e != null) "Critical " else ""
                c.accept("$crit${type.name}: $content", e)
            }

            internal companion object {
                fun <C: Any> of(type: Type<C>, content: C, e: Throwable? = null, msg: String? = null): Entry<C> {
                    return if (msg == null) {
                        BasicEntry(type, content, e)
                    } else {
                        MsgEntry(type, content, e, msg)
                    }
                }
            }
        }

        private class BasicEntry<C: Any>(type: Type<C>, content: C, e: Throwable?): Entry<C>(type, content, e)

        private class MsgEntry<C: Any>(type: Type<C>, content: C, e: Throwable?, private val msg: String): Entry<C>(type, content, e) {
            override fun print(c: BiConsumer<String, Throwable?>) {
                val crit = if (e != null) "Critical " else ""
                c.accept("$crit${type.name}: $msg", e)
            }
        }

        ////////////////////////////

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        class Type<C: Any>(val name: String, val isError: Boolean = true)

        companion object {
            val BASIC = Type<String>("Basic Error")
            val SERIALIZATION = Type<String>("Serialization Error")
            val DESERIALIZATION = Type<String>("Deserialization Error")
            val OUT_OF_BOUNDS = Type<String>("Value Out of Bounds")
            val FILE_STRUCTURE = Type<String>("File Structure Problem")
            val RESTART = Type<Action>("Restart Required", false)
            val ACTION = Type<Action>("Action Required", false)

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

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun createBasic(error: String): ErrorContext {
                return create(BASIC, error)
            }

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun <C: Any> create(type: Type<C>, content: C, e: Throwable? = null, msg: String? = null): ErrorContext {
                return ErrorContext(Entry.of(type, content, e, msg))
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
            return ValidationResult(storedVal, ErrorContext.createBasic(error))
        }

        /**
         * Create a validation result with this if there was a problem during validation.
         *
         * In this case, typically, [storedVal] will be the default value associated with this validation. A valid instance of T must always be passed back. Add a descriptive error message to [errorContext]. If there is no default, you will want to make your result type nullable and pass back null
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param errorContext [ErrorContext] the error to pass with this result
         * @return the errored ValidationResult
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T> error(storedVal: T, errorContext: ErrorContext): ValidationResult<T> {
            return ValidationResult(storedVal, errorContext)
        }

        /**
         * Create a validation result with this if there was a problem during validation.
         *
         * In this case, typically, [storedVal] will be the default value associated with this validation. A valid instance of T must always be passed back. Add a descriptive error message to [errorContext]. If there is no default, you will want to make your result type nullable and pass back null
         * @param T Type of result
         * @param C Type of error content to store. This is usually a string message
         * @param storedVal default or fallback instance of type T
         * @param type [ErrorContext.Type]&lt;[C]&gt; the error type. When in doubt, use [ErrorContext.BASIC]
         * @param content [C] the content of the error. For simple string-based errors, the content is the error message
         * @param e [Throwable], nullable. An exception that led to this errored result. This automatically flags this result as having a critical error
         * @param msg Optional supplementary message to accompany the error context. This is most often used when the error content isn't a string message itself.
         * @return the errored ValidationResult
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T, C: Any> error(storedVal: T, type: ErrorContext.Type<C>, content: C, e: Throwable? = null, msg: String? = null): ValidationResult<T> {
            return ValidationResult(storedVal, ErrorContext.create(type, content, e, msg))
        }

        /**
         * Convenience shortcut for creating a success or error depending on a boolean state.
         *
         * Used if the value returned will be the same regardless of validation, e.g. in the case of [EntryValidator][me.fzzyhmstrs.fzzy_config.entry.EntryValidator] usage, where no changes are being made to the result
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param valid test applied to determine validation or error.
         * @param error string with error message
         * @return the errored ValidationResult
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.7.0
         */
        @Deprecated("Plain error strings are deprecated but acceptable. Most of the time passing in ErrorContext is preferred")
        fun <T> predicated(storedVal: T, valid: Boolean, error: String): ValidationResult<T> {
            return if(valid) ValidationResult(storedVal) else ValidationResult(storedVal, ErrorContext.createBasic(error))
        }

        /**
         * Convenience shortcut for creating a success or error depending on a boolean state.
         *
         * Used if the value returned will be the same regardless of validation, e.g. in the case of [EntryValidator][me.fzzyhmstrs.fzzy_config.entry.EntryValidator] usage, where no changes are being made to the result
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param valid test applied to determine validation or error.
         * @param error [ErrorContext] providing information about the possible error
         * @return the error ValidationResult
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.7.0
         */
        @Deprecated("Plain error strings are deprecated but acceptable. Most of the time passing in ErrorContext is preferred")
        fun <T> predicated(storedVal: T, valid: Boolean, error: ErrorContext): ValidationResult<T> {
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
            return if(valid) ValidationResult(storedVal) else ValidationResult(storedVal, ErrorContext.createBasic(error.get()))
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
        fun <T> predicated(storedVal: T, valid: Boolean, errorContext: Supplier<ErrorContext>): ValidationResult<T> {
            return if(valid) ValidationResult(storedVal) else ValidationResult(storedVal, errorContext.get())
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
            return result.get().map({ r -> success(r) }, { e -> error(fallback, e.message()) })
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
            return result.get().map({ r -> success(r) }, { e -> error(null, e.message()) })
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
            if (!newTest) {
                val thisError = errorContext
                if (thisError == null) {
                    return error(storedVal, ErrorContext.createBasic(error))
                } else {
                    thisError.addError(ErrorContext.createBasic(error))
                }
            }
            return this
        }

        /**
         * reports error, if any, to a provided string list
         * @param errorBuilder MutableList&lt;String&gt; for appending errors.
         * @return ValidationResult returns itself
         * @author fzzyhmstrs
         * @since 0.2.0, deprecated 0.7.0
         */
        @Deprecated("Errors should be built using addError onto a root validation result")
        fun <T> ValidationResult<T>.report(errorBuilder: MutableList<String>): ValidationResult<T> {
            errorContext?.let { errorBuilder.add(it.getString()) }
            return this
        }

        /**
         * reports error, if any, to a provided reporter (such as a logger)
         * @param errorReporter Consumer&lt;String&gt; for reporting errors.
         * @return ValidationResult returns itself
         * @author fzzyhmstrs
         * @since 0.5.9, deprecated 0.7.0
         */
        @Deprecated("Errors should be built using addError onto a root validation result")
        fun <T> ValidationResult<T>.report(errorReporter: Consumer<String>): ValidationResult<T> {
            errorContext?.let { errorReporter.accept(it.getString()) }
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