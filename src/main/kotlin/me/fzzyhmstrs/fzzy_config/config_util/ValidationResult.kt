package me.fzzyhmstrs.fzzy_config.config_util

/**
 * A result of any type T that is wrapped with an optional error message
 *
 * Used to provide contextual error information "upstream" of where the error was encountered, which allows for better logging where upstream elements can collate error messages passed back to them into an organized and sensible error log.
 *
 * This class has a private constructor to force the use of the pre-defined static instantiation methods [success] and [error]
 */
class ValidationResult<T> private constructor(private val storedVal: T, private val error: String = ""){
    /**
     * Boolean check to determine if this result is holding an error
     *
     * @return Boolean, true is an error, false not.
     */
    fun isError(): Boolean{
        return error.isNotEmpty()
    }

    /**
     * Supplies the error message stored within
     *
     * @return String, the error message stored, or "" if no error is within
     */
    fun getError(): String{
        return error
    }

    /**
     * Gets the wrapped result value
     *
     * @return T. The result being wrapped and passed by this ValidationResult.
     */
    fun get(): T{
        return storedVal
    }
    companion object{
        /**
         * Create a validation result with this if validation was successful. No error message needed as no errors were found.
         */
        fun <T>success(storedVal: T): ValidationResult<T>{
            return ValidationResult(storedVal)
        }

        /**
         * Create a validation result with this if there was a problem during validation. Typically in this case, [storedVal] will be the default value associated with this validation. A valid instance of T must always be passed back. Add a descriptive error message to [error]
         */
        fun <T>error(storedVal: T, error: String): ValidationResult<T>{
            return ValidationResult(storedVal,error)
        }
    }
}