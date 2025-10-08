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
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.ErrorEntry.Entry
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.ErrorEntry.Type
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
     * - [isValid]: No errors, critical or otherwise
     * - [isError]: Contains an error (non-critical)
     * - [isCritical]: Contains a critical exception
     * @return Boolean, true is NOT an error, false if there is an error.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun isValid(): Boolean {
        return !errorContext.isError()
    }

    /**
     * Boolean check to determine if this result is holding an error
     * - [isValid]: No errors, critical or otherwise
     * - [isError]: Contains an error (non-critical)
     * - [isCritical]: Contains a critical exception
     * @return Boolean, true is an error, false not.
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    fun isError(): Boolean {
        return errorContext.isError()
    }

    /**
     * Boolean check to determine if this result is holding a critical (exception-caused) error
     * - [isValid]: No errors, critical or otherwise
     * - [isError]: Contains an error (non-critical)
     * - [isCritical]: Contains a critical exception
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
     * @see log
     * @see consume
     * @see test
     * @see iterate
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    @Deprecated("Replace with logging or inspection of the underlying ErrorEntry in most situations")
    fun getErrorEntry(): ErrorEntry {
        return errorContext
    }

    /**
     * Inspects the error of the result, if any, for errors of a particular type
     *
     * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
     * @param C error content type
     * @param t [ErrorEntry.Type]&lt;[C]&gt; the error type. If you don't need a particular type (to report errors for example), use the other overload
     * @param c [Consumer]&lt;[ErrorEntry.Entry]&lt;[C]&gt;&gt; accepts matching entries
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun <C: Any> consume(t: Type<C>, c: Consumer<Entry<C>>) {
        errorContext.consumeType(t, c)
    }

    /**
     * Inspects the error of the result, if any, for errors of a particular type
     *
     * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
     * @param c [Consumer]&lt;[ErrorEntry.Entry]&gt; accepts all entries within the results ErrorEntry
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun consume(c: Consumer<Entry<*>>) {
        errorContext.consumeAll(c)
    }

    /**
     * Provides an iterable containing any applicable [ErrorEntry.Entry] of the provided type
     *
     * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
     * @param C error content type
     * @param t [ErrorEntry.Type]&lt;[C]&gt; the error type. If you don't need a particular type (to report errors for example), use the other overload
     * @return [Iterable] with all applicable entries. May be empty.
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun <C: Any> iterate(t: Type<C>): Iterable<Entry<C>> {
        return errorContext.iterateType(t)
    }

    /**
     * Iterates over all [ErrorEntry.Entry] contained in this result
     *
     * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
     * @return [Iterable] containing all entries in this result. May be empty.
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun iterate(): Iterable<Entry<*>> {
        return errorContext.iterateAll()
    }

    /**
     * Tests whether this results error entry contains the specified error type.
     * @param C error content type
     * @param t [ErrorEntry.Type]&lt;[C]&gt; the error type.
     * @return whether the error context for this validation contains an entry of the given type
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun <C: Any> has(t: Type<C>): Boolean {
        return errorContext.hasType(t)
    }

    /**
     * Performs the provided predicate test on this results error entry and any of its children if they are of the provided type. Generally returns true on the first success.
     *
     * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
     * @param C error content type
     * @param t [ErrorEntry.Type]&lt;[C]&gt; the error type.
     * @param p [Predicate]&lt;[ErrorEntry.Entry]&lt;[C]&gt;&gt; tester for each entry of the given type present
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun <C: Any> test(t: Type<C>, p: Predicate<Entry<C>>): Boolean {
        return errorContext.predicateType(t, p)
    }

    /**
     * Performs the provided predicate test on this results error entry and any of its children. Generally returns true on the first success.
     *
     * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
     * @param p [Predicate]&lt;[ErrorEntry.Entry]&gt; tester for each entry
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun test(p: Predicate<Entry<*>>) {
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

    /**
     * Log this result if it is errored.
     * @param writer [BiConsumer]&lt;String, Throwable?&gt; consumer of logging information for printing to console. See the various helper methods
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    @JvmOverloads
    fun logPlain(writer: BiConsumer<String, Throwable?> = ErrorEntry.ENTRY_WARN_LOGGER): ValidationResult<T> {
        if (isValid()) return this
        errorContext.log(writer)
        return this
    }

    override fun toString(): String {
        val type = if (isError()) {
            if (isCritical()) {
                "critical error"
            } else {
                "error"
            }
        } else {
            "success"
        }
        return "ValidationResult($type:$storedVal)"
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
         * @param type [ErrorEntry.Type]&lt;[C]&gt; the error type. When in doubt, use [Errors.BASIC]
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
         * @param type [ErrorEntry.Type]&lt;String&gt; the string-based error type. When in doubt, use [Errors.BASIC]
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
        fun <T, C: Any> predicated(storedVal: T, valid: Boolean, type: Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ValidationResult<T> {
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
        fun <T, C: Any> predicated(storedVal: T, valid: Predicate<T>, type: Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ValidationResult<T> {
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
            val r = result.result()
            return if (r.isPresent) {
                success(r.get())
            } else {
                @Suppress("DEPRECATION")
                error(fallback, result.error().get().message())
            }
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
            val r = result.result()
            return if (r.isPresent) {
                success(r.get())
            } else {
                @Suppress("DEPRECATION")
                error(null, result.error().get().message())
            }
        }

        /**
         * Creates a new [ErrorEntry.Mutable] instance for filling with error context
         * @param header Default "", the information string that will appear at the top of any error logging. If left empty, the starting error context will be replaced by whatever actual error content is applied first, instead of it being appended to the header context.
         * @return [ErrorEntry.Mutable]
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun createMutable(header: String = ""): ErrorEntry.Mutable {
            return ErrorEntry.empty(header).mutable()
        }

        /**
         * Creates a new ValidationResult of type T wrapping the new value with the error(if any) from the receiver ValidationResult (of any type, does not need to match T)
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
         * @param type [ErrorEntry.Type]&lt;[C]&gt; the error type. When in doubt, use [Errors.BASIC]
         * @param builder [UnaryOperator]&lt;[ErrorEntry.Builder]&gt; operator for applying content to a provided error builder.
         * @return ValidationResult with this stored value, possibly a new error state and new error message
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T, C: Any> ValidationResult<T>.also(newTest: Boolean, type: Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ValidationResult<T> {
            if (!newTest) {
                return error(this.storedVal, this.errorContext.addError(builder.apply(ErrorEntry.Builder(type)).build()))
            }
            return this
        }

        /**
         * Adds another test, and potentially another error, to a Validation.
         * @param newTest Boolean result of another validation test
         * @param type [ErrorEntry.Type]&lt;String&gt; the string-based error type. When in doubt, use [Errors.BASIC]
         * @param error The error message
         * @param e Optional [Throwable] if the error is a critical one
         * @return ValidationResult with this stored value, and possibly a new error state and new error message
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <T> ValidationResult<T>.also(newTest: Boolean, type: Type<String>, error: String, e: Throwable? = null): ValidationResult<T> {
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
        fun <T> ValidationResult<T>.reportTo(errorReporter: BiConsumer<String, Throwable?>): ValidationResult<T> {
            if (!errorContext.isError()) return this
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

    object Errors {
        val BASIC = Type<String>("Error")
        val PARSE = Type<String>("Parsing Error")
        val SERIALIZATION = Type<String>("Serialization Error")
        val DESERIALIZATION = Type<String>("Deserialization Error")
        val OUT_OF_BOUNDS = Type<String>("Value(s) Out of Bounds")
        val INVALID = Type<String>("Value(s) Invalid")
        val FILE_STRUCTURE = Type<String>("File Structure Problem")
        val RESTART = Type<Action>("Restart Required", isString = false, isLoggable = true, isError = false)
        val ACTION = Type<Action>("Action Required", isString = false, isLoggable = false, isError = false)
        val VERSION = Type<Int>("Version Number", isString = false, isLoggable = false, isError = false)
        val ACCESS_VIOLATION = Type<String>("Access Violation")
    }


    /**
     * Error context stored in a [ValidationResult]. This can represent an errored or non-errored state, can include multiple children contexts, and can include non-textual information (see [ValidationResult.Errors.ACTION] for example)
     *
     * All Validation Results include an error entry; starting with a [NonErrorEntry] by default for a successful (non-errored) result.
     * @author fzzyhmstrs
     * @since 0.7.0
     */
     @JvmDefaultWithoutCompatibility
    sealed interface ErrorEntry {
        /**
         * Whether the entry contains an errored state. This is usually true for textual error types, false for empty entries or entries containing non-text information.
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun isError(): Boolean
        /**
         * Whether this entry is empty. An empty error is non-errored
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun isEmpty(): Boolean = false
        /**
         * Whether this entry contains a critical error. It will have one or more exceptions attached to it
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun isCritical(): Boolean
        /**
         * Whether this entry is loggable. Entries with a non-text type generally aren't.
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun isLoggable(): Boolean
        /**
         * Adds another error entry as a child to this one. Entries are immutable, so the updated entry is returned. The entry this is called on is not changed.
         *
         * For entries that are empty or otherwise not able to accept a child, they may return the provided entry as a new root entry instead.
         * @param other New error entry to collate onto this one
         * @return A new error entry based on the combination, the other entry as a new root if this one is empty, or this entry if the other entry is empty.
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun addError(other: ErrorEntry): ErrorEntry
        /**
         * Adds another error entry of the given type as a child to this one, using the builder to build that entry. Entries are immutable, so the updated entry is returned. The entry this is called on is not changed.
         *
         * For entries that are empty or otherwise not able to accept a child, they may return the provided entry as a new root entry instead.
         * @param C Non-null type of the error entry
         * @param type [Type]&lt;[C]&gt; the error entry type to build for
         * @param builder [UnaryOperator]&lt;[Builder]&gt; operator for applying features to the provided stub builder instance
         * @return A new error entry based on the combination, the other entry as a new root if this one is empty, or this entry if the other entry is empty.
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun  <C: Any> addError(type: Type<C>, builder: UnaryOperator<Builder<C>>): ErrorEntry
        /**
         * Adds another error entry using [ValidationResult.Errors.BASIC] as the type, as a child to this one, using the builder to build that entry. Entries are immutable, so the updated entry is returned. The entry this is called on is not changed.
         *
         * For entries that are empty or otherwise not able to accept a child, they may return the provided entry as a new root entry instead.
         * @param builder [UnaryOperator]&lt;[Builder]&gt; operator for applying features to the provided stub builder instance
         * @return A new error entry based on the combination, the other entry as a new root if this one is empty, or this entry if the other entry is empty.
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun  addError(builder: UnaryOperator<Builder<String>>): ErrorEntry {
            return addError(Errors.BASIC, builder)
        }
        /**
         * Performs the provided consumer action on this and any children entries if they are of the provided type.
         *
         * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
         * @param C Non-null type of the error entry
         * @param t [Type]&lt;[C]&gt; the error entry type to consume
         * @param c [Consumer]&lt;[Entry]&gt; action to perform on all entries of the given type
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <C: Any> consumeType(t: Type<C>, c: Consumer<Entry<C>>)
        /**
         * Performs the provided consumer action on this and any children entries.
         *
         * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
         * @param c [Consumer]&lt;[Entry]&gt; action to perform on all entries.
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun consumeAll(c: Consumer<Entry<*>>)
        /**
         * Whether this entry or one or more of its children is of the given type
         * @param C Non-null error type
         * @param t [Type]&lt;[C]&gt; the error entry type to check for
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <C: Any> hasType(t: Type<C>): Boolean
        /**
         * Performs the provided predicate test on this and any children entries if they are of the provided type. Generally returns true on the first success.
         *
         * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
         * @param C Non-null error type
         * @param t [Type]&lt;[C]&gt; the error entry type to check for
         * @param p [Predicate]&lt;[Entry]&gt; test to perform on each entry of the given type
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <C: Any> predicateType(t: Type<C>, p: Predicate<Entry<C>>): Boolean
        /**
         * Performs the provided predicate test on this and any children entries. Generally returns true on the first success.
         *
         * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
         * @param p [Predicate]&lt;[Entry]&gt; test to perform on each entry
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun predicateAll(p: Predicate<Entry<*>>): Boolean
        /**
         * Provides an [Iterable] containing this and any children entries as applicable based on the provided type.
         *
         * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
         * @param C Non-null error type
         * @param t [Type]&lt;[C]&gt; the error type to iterate over
         * @return [Iterable]&lt;[Entry]&gt; containing all relevant entries based on type.
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun <C: Any> iterateType(t: Type<C>): Iterable<Entry<C>>
        /**
         * Provides an [Iterable] containing this and any children entries.
         *
         * NOTE: this performs actions on [Entry], not [ErrorEntry]. Entry is a container stored within most error entries containing content information only.
         * @return [Iterable]&lt;[Entry]&gt; containing all entries
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun iterateAll(): Iterable<Entry<*>>
        /**
         * Returns the string representation of the logging for this entry
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun getString(): String
        /**
         * Return the string representation of the logging for this entry without extra styling and type information.
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun getPlainString(): String
        /**
         * Logs this entry with the provided writer
         * @param writer [BiConsumer]&lt;String, Throwable?&gt; writer for applying log lines. This is often a logger created with [createEntryLogger]
         * @see [createEntryLogger]
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun log(writer: BiConsumer<String, Throwable?>) {
            if (isLoggable())
                writer.accept(getString(), null)
        }
        /**
         * Logs this entry with the provided writer using plain content information
         * @param writer [BiConsumer]&lt;String, Throwable?&gt; writer for applying log lines. This is often a logger created with [createEntryLogger]
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun logPlain(writer: BiConsumer<String, Throwable?>) {
                writer.accept(getPlainString(), null)
        }
        /**
         * Converts this entry to a [Mutable]
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun mutable(): Mutable {
            return Mutable(this)
        }

        /**
         * Type class representing a certain category of error content. This styles logged lines and can be used to store non-string content information.
         * @param C Non-null error content type
         * @param name The prefix used in logging; if the name is "Deserialization Error", the output looks something like "Deserialization Error: provided TOML element isn't a table"
         * @param isString Default true; whether this type represents textual information
         * @param isLoggable Default true; whether this type should be logged when `log` or similar is called
         * @param isError Default true; whether this type represents an errored state. Types that contain miscellaneous information like [ValidationResult.Errors.ACTION] set this to false; they don't represent actual problems, simply a state
         * @param
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        class Type<C: Any>(val name: String, val isString: Boolean = true, val isLoggable: Boolean = true, val isError: Boolean = true) {
            internal fun create(content: C, e: Throwable?, msg: String = ""): ErrorEntry {
                return SingleErrorEntry(this, content, e, msg)
            }

            override fun toString(): String {
                return "Type: $name"
            }
        }

        /**
         * Represents the actual content stored within an error entry. This is used as a buffer between the error entry and code-facing interactions with it
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
         * Builds error entries. This is used in the [UnaryOperator] pattern seen in several places in this class, but can be used "freely" with [builder]
         * @param C Non-null error type being built
         * @param type [Type] the type of this error builder
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
             * Adds a header message to this error entry. This will prompt the builder to create a parent entry, and the actual entry being built will be its first child.
             * @param header The header message to display
             * @return this builder
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun header(header: String): Builder<C> {
                this.header = header
                return this
            }

            /**
             * The content of this entry. For string-based errors, use this instead of [message]
             * @param content [C] instance to store in the error entry
             * @return this builder
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun content(content: C): Builder<C> {
                this.content = content
                return this
            }

            /**
             * If this error is caused by a thrown exception, adds it to the entry
             * @param e [Throwable] to attach to this entry
             * @return this builder
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun exception(e: Throwable?): Builder<C> {
                this.e = e
                return this
            }

            /**
             * Adds a string-based message to a non-string error type. Should not be used with string-based error (most of them)
             * @param msg String message to append to the non-string content
             * @return this builder
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun message(msg: String): Builder<C> {
                this.msg = msg
                return this
            }

            /**
             * Adds a child entry to this builder. When this entry is built, this child will be attached to it.
             * @param child [ErrorEntry]
             * @return this builder
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun addError(child: ErrorEntry): Builder<C> {
                this.children.add(child)
                return this
            }

            /**
             * Adds a child entry to this builder from the provided [ValidationResult]. When this entry is built, this child will be attached to it.
             * @param child [ValidationResult] A validation result. It's error entry will be attached to this as a child. If its error is empty it will be ignored.
             * @return this builder
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun addError(child: ValidationResult<*>): Builder<C> {
                @Suppress("DEPRECATION")
                this.children.add(child.getErrorEntry())
                return this
            }

            /**
             * Builds an [ErrorEntry] from provided inputs. Usually you don't have to do this yourself.
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
         * A mutable wrapper of an error entry. This is typically used to provide an "entrypoint" for building errors based on a pre-defined header message. Use it with other validation results by calling [ValidationResult.attachTo], and then build your final compound result with [ValidationResult.ofMutable]
         *
         * Build the error directly by calling one of the various [addError] methods
         * @param entry [ErrorEntry] the wrapped error entry instance
         * @see ErrorEntry.mutable
         * @see ValidationResult.createMutable
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        class Mutable(internal var entry: ErrorEntry) {
            /**
             * Adds a child error to this mutable
             * @param result [ValidationResult]; its error entry will be added as a child. If the error is empty it will be ignored.
             * @return this mutable
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
             * Adds a child error to this mutable
             * @param other [ErrorEntry] child entry to add
             * @return this mutable
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun addError(other: ErrorEntry): Mutable {
                this.entry = entry.addError(other)
                return this
            }
            /**
             * Adds the entries from another mutable to this one
             * @param other [Mutable] the other mutable instance to copy in
             * @return this mutable
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun addError(other: Mutable): Mutable {
                this.entry = entry.addError(other.entry)
                return this
            }
            /**
             * Builds and adds a child entry to this mutable of the given type
             * @param C Non-null error type
             * @param type [Type]&lt;[C]&gt; error type to build
             * @param builder [UnaryOperator]&lt;[Builder]&gt; operator for applying features to a pre-created builder instance
             * @return this
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun <C: Any> addError(type: Type<C>, builder: UnaryOperator<Builder<C>>): Mutable {
                this.entry = entry.addError(type, builder)
                return this
            }
            /**
             * Builds and adds a string-based child entry to this mutable
             * @param builder [UnaryOperator]&lt;[Builder]&gt; operator for applying features to a pre-created builder instance. Will use the [ValidationResult.Errors.BASIC] type
             * return this mutable
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun addError(builder: UnaryOperator<Builder<String>>): Mutable {
                return addError(Errors.BASIC, builder)
            }
            /**
             * Builds and adds a string-based child entry to this mutable
             * @param type [Type]&lt;String&gt; a string-based error type
             * @param error The error message
             * @param e [Throwable], nullable exception if the error was caused by a throw
             * @return this mutable
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun addError(type: Type<String>, error: String, e: Throwable? = null): Mutable {
                this.entry = entry.addError(Builder(type).content(error).exception(e).build())
                return this
            }

            /**
             * Pass-through method that attaches a string-based error to this mutable and then provides the value back that prompted the error
             *
             * You can see an example of this in [ValidatedColor][me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedColor]
             * @param T the value type to pass through
             * @param value [T] instance that will be returned by this method
             * @param type [Type]&lt;String&gt; a string-based error type
             * @param error The error message
             * @param e [Throwable], nullable exception if the error was caused by a throw
             * @return [value] out the other end
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            fun <T> report(value: T, type: Type<String>, error: String, e: Throwable? = null): T {
                this.entry = entry.addError(Builder(type).content(error).exception(e).build())
                return value
            }
        }

        companion object {
            /**
             * Standard logger using a warning level and Fzzy Configs logger
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            val LOGGER: Consumer<Entry<*>> = createLogger(FC.LOGGER)

            /**
             * Standard error-level entry logger using Fzzy Configs logger
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            val ENTRY_ERROR_LOGGER: BiConsumer<String, Throwable?> = createEntryLogger(FC.LOGGER, Level.ERROR)

            /**
             * Standard warn-level entry logger using Fzzy Configs logger
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            val ENTRY_WARN_LOGGER: BiConsumer<String, Throwable?> = createEntryLogger(FC.LOGGER)

            /**
             * Standard info-level entry logger using Fzzy Configs logger
             * @author fzzyhmstrs
             * @since 0.7.0
             */
            val ENTRY_INFO_LOGGER: BiConsumer<String, Throwable?> = createEntryLogger(FC.LOGGER, Level.INFO)

            /**
             * Creates a consumer of [Entry] that will log
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
                return SingleErrorEntry(Errors.BASIC, error)
            }

            fun <C: Any> builder(type: Type<C>): Builder<C> {
                return Builder(type)
            }

            fun builder(): Builder<String> {
                return Builder(Errors.BASIC)
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
        override fun <C: Any> addError(type: Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ErrorEntry {
            return builder.apply(ErrorEntry.Builder(type)).build()
        }
        override fun <C: Any> consumeType(t: Type<C>, c: Consumer<Entry<C>>) {
        }
        override fun consumeAll(c: Consumer<Entry<*>>) {
        }
        override fun <C : Any> iterateType(t: Type<C>): Iterable<Entry<C>> {
            return listOf()
        }
        override fun iterateAll(): Iterable<Entry<*>> {
            return listOf()
        }
        override fun <C : Any> hasType(t: Type<C>): Boolean {
            return false
        }
        override fun <C : Any> predicateType(t: Type<C>, p: Predicate<Entry<C>>): Boolean {
            return false
        }
        override fun predicateAll(p: Predicate<Entry<*>>): Boolean {
            return false
        }
        override fun getString(): String {
            return "No Error"
        }
        override fun getPlainString(): String {
            return "No Error"
        }

        override fun toString(): String {
            return "Non-Error Entry"
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
        override fun <C: Any> addError(type: Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ErrorEntry {
            return if (header.isEmpty()) {
                builder.apply(ErrorEntry.Builder(type)).build()
            } else {
                CompoundErrorEntry(this, builder.apply(ErrorEntry.Builder(type)).build())
            }
        }
        override fun <C: Any> consumeType(t: Type<C>, c: Consumer<Entry<C>>) {
        }
        override fun consumeAll(c: Consumer<Entry<*>>) {
        }
        override fun <C : Any> iterateType(t: Type<C>): Iterable<Entry<C>> {
            return listOf()
        }
        override fun iterateAll(): Iterable<Entry<*>> {
            return listOf()
        }
        override fun <C : Any> hasType(t: Type<C>): Boolean {
            return false
        }
        override fun <C : Any> predicateType(t: Type<C>, p: Predicate<Entry<C>>): Boolean {
            return false
        }
        override fun predicateAll(p: Predicate<Entry<*>>): Boolean {
            return false
        }
        override fun getString(): String {
            return header.ifEmpty { "Empty Error" }
        }
        override fun getPlainString(): String {
            return header.ifEmpty { "Empty Error" }
        }

        override fun toString(): String {
            return if(header.isNotEmpty()) "Empty Error Entry: $header" else "Empty Error Entry"
        }
    }

    private class SingleErrorEntry <T: Any>(
        override val type: Type<T>,
        override val content: T,
        override val e: Throwable? = null,
        private val msg: String = ""): ErrorEntry, Entry<T>
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
        override fun <C: Any> addError(type: Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ErrorEntry {
            return CompoundErrorEntry(this, builder.apply(ErrorEntry.Builder(type)).build())
        }
        override fun <C: Any> consumeType(t: Type<C>, c: Consumer<Entry<C>>) {
            if (t != type) return
            @Suppress("UNCHECKED_CAST")
            (this as? Entry<C>)?.let{ c.accept(it) }
        }
        override fun consumeAll(c: Consumer<Entry<*>>) {
            c.accept(this)
        }
        override fun <C : Any> iterateType(t: Type<C>): Iterable<Entry<C>> {
            return if (t != type) listOf() else listOf(this).cast()
        }
        override fun iterateAll(): Iterable<Entry<*>> {
            return listOf(this)
        }
        override fun <C : Any> hasType(t: Type<C>): Boolean {
            return t == type
        }
        override fun <C : Any> predicateType(t: Type<C>, p: Predicate<Entry<C>>): Boolean {
            if (t != type) return false
            return p.test(this.cast())
        }
        override fun predicateAll(p: Predicate<Entry<*>>): Boolean {
            return p.test(this)
        }
        override fun getString(): String {
            val crit = if (e != null) "Critical " else ""
            return if (msg.isNotEmpty())
                "$crit${type.name}: $msg ($content)"
            else
                "$crit${type.name}: $content"
        }
        override fun getPlainString(): String {
            return if (msg.isNotEmpty())
                "$msg ($content)"
            else
                "$content"
        }
        override fun log(writer: BiConsumer<String, Throwable?>) {
            if (isLoggable())
                writer.accept(getString(), e)
        }

        override fun toString(): String {
            return "Single Error Entry - [$type]"
        }
    }

    private class CompoundErrorEntry(private val headerEntry: ErrorEntry, private val children: MutableList<ErrorEntry>): ErrorEntry {

        constructor(headerEntry: ErrorEntry, childEntry: ErrorEntry): this(headerEntry, mutableListOf(childEntry))

        override fun isEmpty(): Boolean {
            return headerEntry.isEmpty() && children.all { it.isEmpty() }
        }

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
        override fun <C: Any> addError(type: Type<C>, builder: UnaryOperator<ErrorEntry.Builder<C>>): ErrorEntry {
            val other = builder.apply(ErrorEntry.Builder(type)).build()
            return addError(other)
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
        override fun <C : Any> iterateType(t: Type<C>): Iterable<Entry<C>> {
            return listOf(headerEntry).flatMap { it.iterateType(t) } + children.flatMap { it.iterateType(t) }
        }
        override fun iterateAll(): Iterable<Entry<*>> {
            return listOf(headerEntry).flatMap { it.iterateAll() } + children.flatMap { it.iterateAll() }
        }
        override fun <C : Any> hasType(t: Type<C>): Boolean {
            return children.any { it.hasType(t) } || headerEntry.hasType(t)
        }
        override fun <C : Any> predicateType(t: Type<C>, p: Predicate<Entry<C>>): Boolean {
            return children.any { it.predicateType(t, p) } || headerEntry.predicateType(t, p)
        }

        override fun predicateAll(p: Predicate<Entry<*>>): Boolean {
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
        override fun getPlainString(): String {
            val list: MutableList<String> = mutableListOf()
            list.add(headerEntry.getPlainString())
            for (entry in children) {
                if (entry.isLoggable())
                    list.add(entry.getPlainString())
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
        override fun logPlain(writer: BiConsumer<String, Throwable?>) {
            headerEntry.logPlain(writer)
            if (children.isEmpty()) return
            val consumer: BiConsumer<String, Throwable?> = BiConsumer { str, e ->
                writer.accept("  $str", e)
            }
            for (entry in children) {
                entry.logPlain(consumer)
            }
        }

        override fun toString(): String {
            return "Compound Error Entry - Header[$headerEntry] Children[${children.size}]"
        }
    }
}