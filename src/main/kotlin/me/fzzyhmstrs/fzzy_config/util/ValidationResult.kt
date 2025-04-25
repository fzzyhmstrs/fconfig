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
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.config.ConfigContext
import org.slf4j.Logger
import org.slf4j.event.Level
import java.util.function.*
import java.util.function.Function

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
     * Boolean check to determine if this result is holding a critical (exception-caused) error
     *
     * @return Boolean, true is a critical error, false not.
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun isCritical(): Boolean {
        return errorContext.isCritical()
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
     * @return [ErrorEntry], or null if this isn't an errored result
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
     * @param t [ErrorEntry.Type]&lt;[C]&gt; the error type. If you don't need a particular type (to report errors for example), use the other overload
     * @param c [Consumer]&lt;[ErrorEntry.Entry]&lt;[C]&gt;&gt; accepts matching entries
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun <C: Any> consume(t: ErrorEntry.Type<C>, c: Consumer<ErrorEntry.Entry<C>>) {
        errorContext.consumeType(t, c)
    }

    /**
     * Inspects the error of the result, if any, for errors of a particular type
     * @param c [Consumer]&lt;[ErrorEntry.Entry]&gt; accepts all entries within the results ErrorEntry
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun consume(c: Consumer<ErrorEntry.Entry<*>>) {
        errorContext.consumeAll(c)
    }

    /**
     * TODO()
     * @param C error content type
     * @param t [ErrorEntry.Type]&lt;[C]&gt; the error type. If you don't need a particular type (to report errors for example), use the other overload
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun <C: Any> iterate(t: ErrorEntry.Type<C>): Iterable<ErrorEntry.Entry<C>> {
        return errorContext.iterateType(t)
    }

    /**
     * TODO()
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun iterate(c: Consumer<ErrorEntry.Entry<*>>) {
        errorContext.consumeAll(c)
    }

    /**
     * TODO()
     * @param C error content type
     * @param t [ErrorEntry.Type]&lt;[C]&gt; the error type. If you don't need a particular type (to report errors for example), use the other overload
     * @return whether the error context for this validation contains an entry of the given type
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun <C: Any> has(t: ErrorEntry.Type<C>): Boolean {
        return errorContext.hasType(t)
    }

    /**
     * TODO()
     * @param C error content type
     * @param t [ErrorEntry.Type]&lt;[C]&gt; the error type. If you don't need a particular type (to report errors for example), use the other overload
     * @param p TODO()
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun <C: Any> test(t: ErrorEntry.Type<C>, p: Predicate<ErrorEntry.Entry<C>>): Boolean {
        return errorContext.predicateType(t, p)
    }

    /**
     * TODO()
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun test(p: Predicate<ErrorEntry.Entry<*>>) {
        errorContext.predicateAll(p)
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
        @Suppress("DEPRECATION")
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
        @Suppress("DEPRECATION")
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
    @JvmOverloads
    fun log(writer: BiConsumer<String, Throwable?> = ErrorEntry.ENTRY_WARN_LOGGER): ValidationResult<T> {
        if (isValid()) return this
        writer.accept(">>>>>>>>>>>>>>>", null)
        errorContext.log(writer)
        writer.accept(">>>>>>>>>>>>>>>", null)
        return this
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
         * If storing a simple string error message, consider the overload that accepts a [ErrorEntry.Type]&lt;String&gt; and simple message/throwable inputs
         *
         * In this case, typically, [storedVal] will be the default value associated with this validation. A valid instance of T must always be passed back. Add a descriptive error message to [errorContext]. If there is no default, you will want to make your result type nullable and pass back null
         * @param T Type of result
         * @param C Type of error content to store. This is usually a string message
         * @param storedVal default or fallback instance of type T
         * @param type [ErrorEntry.Type]&lt;[C]&gt; the error type. When in doubt, use [ErrorEntry.BASIC]
         * @param builder [UnaryOperator]&lt;[ErrorEntry.Builder]&gt; operator for applying content to a provided error builder
         * @return the errored ValidationResult
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T, C: Any> error(storedVal: T, type: ErrorEntry.Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ValidationResult<T> {
            return ValidationResult(storedVal, builder.apply(ErrorEntry.Builder(type)).build())
        }

        /**
         * Create a validation result with this if there was a problem during validation.
         *
         * In this case, typically, [storedVal] will be the default value associated with this validation. A valid instance of T must always be passed back. Add a descriptive error message to [errorContext]. If there is no default, you will want to make your result type nullable and pass back null
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param type [ErrorEntry.Type]&lt;String&gt; the string-based error type. When in doubt, use [ErrorEntry.BASIC]
         * @param error string with error message
         * @param e [Throwable], optional, default null. Exception to pass if this result is critically errored
         * @return the errored ValidationResult
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        @JvmOverloads
        fun <T> error(storedVal: T, type: ErrorEntry.Type<String>, error: String, e: Throwable? = null): ValidationResult<T> {
            return ValidationResult(storedVal, ErrorEntry.Builder(type).content(error).exception(e).build())
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
         * @since 0.7.0
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
         * @param builder [UnaryOperator]&lt;[ErrorEntry.Builder]&gt; operator for applying content to a provided error builder
         * @return the error ValidationResult
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T, C: Any> predicated(storedVal: T, valid: Boolean, type: ErrorEntry.Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ValidationResult<T> {
            return if(valid) ValidationResult(storedVal) else ValidationResult(storedVal, builder.apply(ErrorEntry.Builder(type)).build())
        }

        /**
         * Convenience shortcut for creating a success or error depending on a boolean state.
         *
         * Used if the value returned will be the same regardless of validation, e.g. in the case of [EntryValidator][me.fzzyhmstrs.fzzy_config.entry.EntryValidator] usage, where no changes are being made to the result
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param valid test applied to determine validation or error.
         * @param builder [UnaryOperator]&lt;[ErrorEntry.Builder]&gt; operator for applying content to a provided error builder
         * @return the error ValidationResult
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T, C: Any> predicated(storedVal: T, valid: Predicate<T>, type: ErrorEntry.Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ValidationResult<T> {
            return if(valid.test(storedVal)) ValidationResult(storedVal) else ValidationResult(storedVal, builder.apply(ErrorEntry.Builder(type)).build())
        }

        /**
         * Create a validation result with a built error context stored in a [ErrorEntry.Mutable]
         *
         * This is used internally by processes that build error contexts in multiple steps, so the final result might or might not be errored
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param mutable [ErrorEntry.Mutable] built error entry, which might be empty or might contain errors.
         * @return the errored ValidationResult
         * @author fzzyhmstrs
         * @since 0.1.0, deprecated 0.7.0
         */
        fun <T> ofMutable(storedVal: T, mutable: ErrorEntry.Mutable): ValidationResult<T> {
            return ValidationResult(storedVal, mutable.entry)
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
            return result.mapOrElse({ r -> success(r) }, { e ->
                @Suppress("DEPRECATION")
                error(fallback, e.message())
            })
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
            return result.mapOrElse({ r -> success(r) }, { e ->
                @Suppress("DEPRECATION")
                error(null, e.message())
            })
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
         * @since 0.2.0, deprecated 0.7.0
         */
        @Deprecated("Plain error strings are deprecated but acceptable. Most of the time passing in ErrorContext is preferred")
        fun <T> ValidationResult<T>.also(newTest: Boolean, error: String): ValidationResult<T> {
            if (!newTest) {
                return error(this.storedVal, this.errorContext.addError(ErrorEntry.basic(error)))
            }
            return this
        }

        /**
         * Adds another test, and potentially another error, to a Validation.
         * @param C Type of error content to store. This is usually a string message
         * @param newTest Boolean result of another validation test
         * @param type [ErrorEntry.Type]&lt;[C]&gt; the error type. When in doubt, use [ErrorEntry.BASIC]
         * @param builder [UnaryOperator]&lt;[ErrorEntry.Builder]&gt; operator for applying content to a provided error builder.
         * @return ValidationResult with this stored value, possibly a new error state and new error message
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T, C: Any> ValidationResult<T>.also(newTest: Boolean, type: ErrorEntry.Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ValidationResult<T> {
            if (!newTest) {
                return error(this.storedVal, this.errorContext.addError(builder.apply(ErrorEntry.Builder(type)).build()))
            }
            return this
        }

        /**
         * Adds another test, and potentially another error, to a Validation.
         * @param newTest Boolean result of another validation test
         * @param type [ErrorEntry.Type]&lt;String&gt; the string-based error type. When in doubt, use [ErrorEntry.BASIC]
         * @param error The error message
         * @param e Optional [Throwable] if the error is a critical one
         * @return ValidationResult with this stored value, and possibly a new error state and new error message
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T> ValidationResult<T>.also(newTest: Boolean, type: ErrorEntry.Type<String>, error: String, e: Throwable? = null): ValidationResult<T> {
            if (!newTest) {
                return error(this.storedVal, this.errorContext.addError(ErrorEntry.Builder(type).content(error).exception(e).build()))
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
        @Deprecated("Errors should be reported with log or the overload that takes a biConsumer")
        fun <T> ValidationResult<T>.report(errorBuilder: MutableList<String>): ValidationResult<T> {
            errorContext.log { s, _ ->
                errorBuilder.add(s)
            }
            return this
        }

        /**
         * reports error, if any, to a provided reporter (such as a logger)
         * @param errorReporter Consumer&lt;String&gt; for reporting errors.
         * @return this validation
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
         * Reports errors in this validation to an error consumer.
         * @param errorReporter [BiConsumer]&lt;String, Throwable?&gt; consumes error messages, which may include an optional throwable instance
         * @return this validation
         * @see [ErrorEntry.ENTRY_WARN_LOGGER]
         * @see [ErrorEntry.createEntryLogger]
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T> ValidationResult<T>.report(errorReporter: BiConsumer<String, Throwable?>): ValidationResult<T> {
            errorContext.log(errorReporter)
            return this
        }

        /**
         * Reports errors in this validation to an error consumer.
         * @param mutable [ErrorEntry.Mutable] mutable error to attach any applicable error to
         * @return this validation
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T> ValidationResult<T>.attachTo(mutable: ErrorEntry.Mutable): ValidationResult<T> {
            mutable.addError(this)
            return this
        }

        /**
         * Maps this validation's stored value to a new type using a mapping function
         * @param N new type to store
         * @param T old type stored in this validation to map
         * @param to [Function]&lt;T, out N&gt; mapping function
         * @return Validation with the newly mapped [N] value and whatever [errorContext] might be stored in this validation
         * @author fzzyhmstrs
         * @since Unknown
         */
        fun <N, T> ValidationResult<T>.map(to: Function<T, out N>): ValidationResult<N> {
            return ValidationResult(to.apply(this.storedVal), this.errorContext)
        }

        /**
         * Maps this validation to a new type using a mapping function
         * @param N new type to store
         * @param T old type stored in this validation to map
         * @param to [Function]&lt;[ValidationResult]&lt;T&gt;, out N&gt; mapping function
         * @return Validation with the newly mapped [N] value and whatever [errorContext] might be stored in this validation
         * @author fzzyhmstrs
         * @since Unknown
         */
        fun <N, T> ValidationResult<T>.inmap(to: Function<ValidationResult<T>, out N>): ValidationResult<N> {
            return ValidationResult(to.apply(this), this.errorContext)
        }

        /**
         * Maps this validation's stored value to a new type using a mapping function
         * @param N new type to store
         * @param T old type stored in this validation to map
         * @param to [Function]&lt;T, [ValidationResult]&lt;out N&gt;&gt; mapping function
         * @return Validation with the newly mapped [N] value and whatever [errorContext] might be stored in this validation
         * @author fzzyhmstrs
         * @since Unknown
         */
        fun <N, T> ValidationResult<T>.outmap(to: Function<T, ValidationResult<out N>>): ValidationResult<N> {
            val result = to.apply(this.storedVal)
            return ValidationResult(result.get(), this.errorContext.addError(result.errorContext))
        }

        /**
         * Maps this validation to a new type using a mapping function
         * @param N new type to store
         * @param T old type stored in this validation to map
         * @param to [Function]&lt;[ValidationResult]&lt;T&gt;, [ValidationResult]&lt;out N&gt;&gt; mapping function
         * @return Validation with the newly mapped [N] value and whatever [errorContext] might be stored in this validation
         * @author fzzyhmstrs
         * @since Unknown
         */
        fun <N, T> ValidationResult<T>.bimap(to: Function<ValidationResult<T>, ValidationResult<out N>>): ValidationResult<N> {
            val result = to.apply(this)
            return ValidationResult(result.get(), this.errorContext.addError(result.errorContext))
        }
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
        fun isLoggable(): Boolean
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
        fun  <C: Any> addError(type: Type<C>, builder: UnaryOperator<Builder<C>>): ErrorEntry
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
        fun <C: Any> consumeType(t: Type<C>, c: Consumer<Entry<C>>)
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun consumeAll(c: Consumer<Entry<*>>)
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <C: Any> hasType(t: Type<C>): Boolean
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <C: Any> predicateType(t: Type<C>, p: Predicate<Entry<C>>): Boolean
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun predicateAll(p: Predicate<Entry<*>>): Boolean
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <C: Any> iterateType(t: Type<C>): Iterable<Entry<C>>
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun iterateAll(): Iterable<Entry<*>>
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
            if (isLoggable())
                writer.accept(getString(), null)
        }
        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun mutable(): Mutable {
            return Mutable(this)
        }

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        class Type<C: Any>(val name: String, val isString: Boolean = true, val isLoggable: Boolean = true, val isError: Boolean = true) {
            fun create(content: C, e: Throwable?, msg: String = ""): ErrorEntry {
                return SingleErrorEntry(this, content, e, msg)
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

            fun log(writer: BiConsumer<String, Throwable?>)
        }

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        class Builder<C: Any> internal constructor(private val type: Type<C>) {

            private var header: String = ""
            private var content: C? = null
            private var e: Throwable? = null
            private var msg: String = ""
            private var children: MutableList<ErrorEntry> = mutableListOf()

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
            fun exception(e: Throwable?): Builder<C> {
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
            fun addError(child: ErrorEntry): Builder<C> {
                this.children.add(child)
                return this
            }

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun addError(child: ValidationResult<*>): Builder<C> {
                this.children.add(child.getErrorEntry())
                return this
            }

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun build(): ErrorEntry {
                var entry = if (content == null) {
                    if (msg.isNotEmpty() && type.isString) {
                        FC.DEVLOG.warn("String-type ErrorEntry built with message() instead of content()")
                        @Suppress("UNCHECKED_CAST")
                        val entry = (type as Type<String>).create(msg, e)
                        if (header.isNotEmpty()) {
                            EmptyErrorEntry(header).addError(entry)
                        } else {
                            entry
                        }
                    } else {
                        EmptyErrorEntry(header)
                    }
                } else {
                    val entry = type.create(content!!, e, msg)
                    if (header.isNotEmpty()) {
                        EmptyErrorEntry(header).addError(entry)
                    } else {
                        entry
                    }
                }
                for (child in children) {
                    entry = entry.addError(child)
                }
                return entry
            }
        }

        /**
         * TODO()
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        class Mutable(internal var entry: ErrorEntry) {
            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            @Suppress("DEPRECATION")
            fun addError(result: ValidationResult<*>): Mutable {
                if (!result.isValid())
                    this.entry = entry.addError(result.getErrorEntry())
                return this
            }
            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun addError(other: ErrorEntry): Mutable {
                this.entry = entry.addError(other)
                return this
            }
            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun <C: Any> addError(type: Type<C>, builder: UnaryOperator<Builder<C>>): Mutable {
                this.entry = entry.addError(type, builder)
                return this
            }
            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun addError(builder: UnaryOperator<Builder<String>>): Mutable {
                return addError(BASIC, builder)
            }
            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun addError(type: Type<String>, error: String, e: Throwable? = null): Mutable {
                this.entry = entry.addError(Builder(type).content(error).exception(e).build())
                return this
            }
        }

        companion object {
            val BASIC = Type<String>("Basic Error")
            val SERIALIZATION = Type<String>("Serialization Error")
            val DESERIALIZATION = Type<String>("Deserialization Error")
            val OUT_OF_BOUNDS = Type<String>("Value Out of Bounds")
            val FILE_STRUCTURE = Type<String>("File Structure Problem")
            val RESTART = Type<Action>("Restart Required", isString = false, isLoggable = true, isError = false)
            val ACTION = Type<Action>("Action Required", isString = false, isLoggable = false, isError = false)
            val VERSION = Type<Int>("Version Number", isString = false, isLoggable = false, isError = false)
            val ACCESS_VIOLATION = Type<String>("Access Violation")

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            val LOGGER: Consumer<Entry<*>> = createLogger(FC.LOGGER)

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            val ENTRY_ERROR_LOGGER: BiConsumer<String, Throwable?> = createEntryLogger(FC.LOGGER, Level.ERROR)

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            val ENTRY_WARN_LOGGER: BiConsumer<String, Throwable?> = createEntryLogger(FC.LOGGER)

            /**
             * TODO()
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            val ENTRY_INFO_LOGGER: BiConsumer<String, Throwable?> = createEntryLogger(FC.LOGGER, Level.INFO)

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
                        e.log(entryConsumer)
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

            fun <C: Any> builder(type: Type<C>): Builder<C> {
                return Builder(type)
            }

            fun builder(): Builder<String> {
                return Builder(BASIC)
            }
        }
    }

    private data object NonErrorEntry: ErrorEntry {
        override fun isError(): Boolean = false
        override fun isEmpty(): Boolean = true
        override fun isCritical(): Boolean = false
        override fun isLoggable(): Boolean = false
        override fun addError(other: ErrorEntry): ErrorEntry {
            return other
        }
        override fun <C: Any> addError(type: ErrorEntry.Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ErrorEntry {
            return builder.apply(ErrorEntry.Builder(type)).build()
        }
        override fun <C: Any> consumeType(t: ErrorEntry.Type<C>, c: Consumer<ErrorEntry.Entry<C>>) {
        }
        override fun consumeAll(c: Consumer<ErrorEntry.Entry<*>>) {
        }
        override fun <C : Any> iterateType(t: ErrorEntry.Type<C>): Iterable<ErrorEntry.Entry<C>> {
            return listOf()
        }
        override fun iterateAll(): Iterable<ErrorEntry.Entry<*>> {
            return listOf()
        }
        override fun <C : Any> hasType(t: ErrorEntry.Type<C>): Boolean {
            return false
        }
        override fun <C : Any> predicateType(t: ErrorEntry.Type<C>, p: Predicate<ErrorEntry.Entry<C>>): Boolean {
            return false
        }
        override fun predicateAll(p: Predicate<ErrorEntry.Entry<*>>): Boolean {
            return false
        }
        override fun getString(): String {
            return "No Error"
        }
    }

    private class EmptyErrorEntry(private val header: String = ""): ErrorEntry {
        override fun isError(): Boolean = false
        override fun isEmpty(): Boolean = true
        override fun isCritical(): Boolean = false
        override fun isLoggable(): Boolean = header.isNotEmpty()
        override fun addError(other: ErrorEntry): ErrorEntry {
            return if (other.isEmpty()) {
                this
            } else if (header.isEmpty()) {
                other
            } else {
                CompoundErrorEntry(this, other)
            }
        }
        override fun <C: Any> addError(type: ErrorEntry.Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ErrorEntry {
            return if (header.isEmpty()) {
                builder.apply(ErrorEntry.Builder(type)).build()
            } else {
                CompoundErrorEntry(this, builder.apply(ErrorEntry.Builder(type)).build())
            }
        }
        override fun <C: Any> consumeType(t: ErrorEntry.Type<C>, c: Consumer<ErrorEntry.Entry<C>>) {
        }
        override fun consumeAll(c: Consumer<ErrorEntry.Entry<*>>) {
        }
        override fun <C : Any> iterateType(t: ErrorEntry.Type<C>): Iterable<ErrorEntry.Entry<C>> {
            return listOf()
        }
        override fun iterateAll(): Iterable<ErrorEntry.Entry<*>> {
            return listOf()
        }
        override fun <C : Any> hasType(t: ErrorEntry.Type<C>): Boolean {
            return false
        }
        override fun <C : Any> predicateType(t: ErrorEntry.Type<C>, p: Predicate<ErrorEntry.Entry<C>>): Boolean {
            return false
        }
        override fun predicateAll(p: Predicate<ErrorEntry.Entry<*>>): Boolean {
            return false
        }
        override fun getString(): String {
            return header.ifEmpty { "Empty Error" }
        }
    }

    private class SingleErrorEntry <T: Any>(
        override val type: ErrorEntry.Type<T>,
        override val content: T,
        override val e: Throwable? = null,
        private val msg: String = ""): ErrorEntry, ErrorEntry.Entry<T>
    {
        override fun isError(): Boolean {
            return type.isError
        }
        override fun isCritical(): Boolean {
            return e != null
        }

        override fun isLoggable(): Boolean {
            return type.isLoggable
        }
        override fun addError(other: ErrorEntry): ErrorEntry {
            return if (other.isEmpty()) {
                this
            } else {
                CompoundErrorEntry(this, other)
            }
        }
        override fun <C: Any> addError(type: ErrorEntry.Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ErrorEntry {
            return CompoundErrorEntry(this, builder.apply(ErrorEntry.Builder(type)).build())
        }
        override fun <C: Any> consumeType(t: ErrorEntry.Type<C>, c: Consumer<ErrorEntry.Entry<C>>) {
            if (t != type) return
            @Suppress("UNCHECKED_CAST")
            (this as? ErrorEntry.Entry<C>)?.let{ c.accept(it) }
        }
        override fun consumeAll(c: Consumer<ErrorEntry.Entry<*>>) {
            c.accept(this)
        }
        override fun <C : Any> iterateType(t: ErrorEntry.Type<C>): Iterable<ErrorEntry.Entry<C>> {
            return if (t != type) listOf() else listOf(this).cast()
        }
        override fun iterateAll(): Iterable<ErrorEntry.Entry<*>> {
            return listOf(this)
        }
        override fun <C : Any> hasType(t: ErrorEntry.Type<C>): Boolean {
            return t == type
        }
        override fun <C : Any> predicateType(t: ErrorEntry.Type<C>, p: Predicate<ErrorEntry.Entry<C>>): Boolean {
            if (t != type) return false
            return p.test(this.cast())
        }
        override fun predicateAll(p: Predicate<ErrorEntry.Entry<*>>): Boolean {
            return p.test(this)
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

        override fun isLoggable(): Boolean {
            return children.any { it.isLoggable() }
        }
        override fun addError(other: ErrorEntry): ErrorEntry {
            if (!other.isEmpty()) {
                children.add(other)
            }
            return this
        }
        override fun <C: Any> addError(type: ErrorEntry.Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ErrorEntry {
            val other = builder.apply(ErrorEntry.Builder(type)).build()
            return addError(other)
        }
        override fun <C: Any> consumeType(t: ErrorEntry.Type<C>, c: Consumer<ErrorEntry.Entry<C>>) {
            headerEntry.consumeType(t, c)
            for (entry in children) {
                entry.consumeType(t, c)
            }
        }
        override fun consumeAll(c: Consumer<ErrorEntry.Entry<*>>) {
            headerEntry.consumeAll(c)
            for (entry in children) {
                entry.consumeAll(c)
            }
        }
        override fun <C : Any> iterateType(t: ErrorEntry.Type<C>): Iterable<ErrorEntry.Entry<C>> {
            return children.flatMap { it.iterateType(t) }
        }
        override fun iterateAll(): Iterable<ErrorEntry.Entry<*>> {
            return children.flatMap { it.iterateAll() }
        }
        override fun <C : Any> hasType(t: ErrorEntry.Type<C>): Boolean {
            return children.any { it.hasType(t) } || headerEntry.hasType(t)
        }
        override fun <C : Any> predicateType(t: ErrorEntry.Type<C>, p: Predicate<ErrorEntry.Entry<C>>): Boolean {
            return children.any { it.predicateType(t, p) } || headerEntry.predicateType(t, p)
        }

        override fun predicateAll(p: Predicate<ErrorEntry.Entry<*>>): Boolean {
            return children.any { it.predicateAll(p) } || headerEntry.predicateAll(p)
        }
        override fun getString(): String {
            val list: MutableList<String> = mutableListOf()
            list.add(headerEntry.getString())
            for (entry in children) {
                if (entry.isLoggable())
                    list.add(entry.getString())
            }
            return list.joinToString(" ")
        }
        override fun log(writer: BiConsumer<String, Throwable?>) {
            headerEntry.log(writer)
            if (children.isEmpty()) return
            val consumer: BiConsumer<String, Throwable?> = BiConsumer { str, e ->
                writer.accept("  $str", e)
            }
            for (entry in children) {
                entry.log(consumer)
            }
        }
    }
}