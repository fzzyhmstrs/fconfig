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
import me.fzzyhmstrs.fzzy_config.config.ConfigContext
import java.util.function.Consumer
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
class ValidationResult<T> private constructor(private val storedVal: T, private val error: String = "") {
    /**
     * Boolean check to determine if this result is valid (no errors)
     *
     * @return Boolean, true is NOT an error, false if there is an error.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun isValid(): Boolean {
        return error.isEmpty()
    }

    /**
     * Boolean check to determine if this result is holding an error
     *
     * @return Boolean, true is an error, false not.
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    fun isError(): Boolean {
        return error.isNotEmpty()
    }

    /**
     * Supplies the error message stored within
     *
     * @return String, the error message stored, or an empty string if no error
     * @author fzzyhmstrs
     * @since 0.1.0
     */
    fun getError(): String {
        return error
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
     * @since 0.1.0
     */
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
     * @since 0.1.0
     */
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
         * @since 0.1.0
         */
        fun <T> error(storedVal: T, error: String): ValidationResult<T> {
            return ValidationResult(storedVal, error)
        }

        /**
         * Convenience shortcut for creating a success or error depending on a boolean state.
         *
         * Used if the value returned will be the same regardless of validation, eg. in the case of [EntryValidator][me.fzzyhmstrs.fzzy_config.validation.entry.EntryValidator] usage, where no changes are being made to the result
         * @param T Type of result
         * @param storedVal default or fallback instance of type T
         * @param valid test applied to determine validation or error.
         * @param error string with error message
         * @return the errored ValidationResult
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun <T> predicated(storedVal: T, valid: Boolean, error: String): ValidationResult<T> {
            return if(valid) ValidationResult(storedVal) else ValidationResult(storedVal, error)
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
            return ValidationResult(newVal, this.error)
        }

        /**
         * Adds another test, and potentially another error, to a Validation.
         * @param newTest Boolean result of another validation test
         * @param error error message if the newTest fails validation
         * @return ValidationResult with this stored value, possibly a new error state and new error message
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun <T> ValidationResult<T>.also(newTest: Boolean, error: String): ValidationResult<T> {
            return if(!newTest) {
                val totalError = if(this.isError()) {
                    "${this.error}, also $error"
                } else {
                    error
                }
                ValidationResult(this.storedVal, totalError)
            } else {
                this
            }
        }

        /**
         * reports error, if any, to a provided string list
         * @param errorBuilder MutableList&lt;String&gt; for appending errors.
         * @return ValidationResult returns itself
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun <T> ValidationResult<T>.report(errorBuilder: MutableList<String>): ValidationResult<T> {
            if (this.isError()) errorBuilder.add(this.error)
            return this
        }

        /**
         * reports error, if any, to a provided reporter (such as a logger)
         * @param errorReporter Consumer&lt;String&gt; for reporting errors.
         * @return ValidationResult returns itself
         * @author fzzyhmstrs
         * @since 0.5.9
         */
        fun <T> ValidationResult<T>.report(errorReporter: Consumer<String>): ValidationResult<T> {
            if (this.isError()) errorReporter.accept(this.error)
            return this
        }

        /**
         *
         */
        fun <N, T> ValidationResult<T>.map(to: Function<T, out N>): ValidationResult<N> {
            return ValidationResult(to.apply(this.storedVal), this.error)
        }

        fun <T: Any> ValidationResult<ConfigContext<T>>.contextualize(): ValidationResult<T> {
            return this.wrap(this.get().config)
        }
    }
}