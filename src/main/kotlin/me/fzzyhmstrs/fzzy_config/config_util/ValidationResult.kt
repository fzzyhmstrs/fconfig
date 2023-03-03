package me.fzzyhmstrs.fzzy_config.config_util

class ValidationResult<T> private constructor(private val storedVal: T, private val error: String = ""){
    fun isError(): Boolean{
        return error.isNotEmpty()
    }
    fun getError(): String{
        return error
    }
    fun get(): T{
        return storedVal
    }
    companion object{
        fun <T>success(storedVal: T): ValidationResult<T>{
            return ValidationResult(storedVal)
        }
        fun <T>error(storedVal: T, error: String): ValidationResult<T>{
            return ValidationResult(storedVal,error)
        }
    }
}