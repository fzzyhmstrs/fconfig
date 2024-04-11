package me.fzzyhmstrs.fzzyconfig.config

class ConfigContext<T: Any>(val config: T) {
        private val contextFlags: MutableMap<String,Any> = mutableMapOf()

        fun withFlag(key: String, value: Any): ConfigContext<T>{
            contextFlags[key] = value
            return this
        } 
        fun getBoolean(key: String): Boolean {
            return contextFlags[key] as? Boolean ?: false
        }
        fun getInt(key: String): Int {
            return contextFlags[key] as? Int ?: false
        }
        fun getAsConfig(): Config?{
            return config as? Config
        }
    }
