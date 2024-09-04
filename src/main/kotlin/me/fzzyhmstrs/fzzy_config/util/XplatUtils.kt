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

import java.util.*

abstract class XplatUtils {

    companion object {
        private val delegate: XplatUtil by lazy {
            try {
			    return@lazy Class.forName("me.fzzyhmstrs.fzzy_config.util.fabric.XplatUtilsFabric").newInstance();
    		} catch (t: Throwable) {
    		}
    		try {
    			return@lazy Class.forName("me.fzzyhmstrs.fzzy_config.util.neoforge.XplatUtilsNeoForge").newInstance();
    		} catch (t: Throwable) {
    		}
    		try {
    			return@lazy Class.forName("me.fzzyhmstrs.fzzy_config.util.forge.XplatUtilsForge").newInstance();
    		} catch (t: Throwable) {
                throw IllegalStateException("Couldn't initialize any Platform Utilities. This is an unexpected bug! Please report to the author of Fzzy Config")
    		}
        }

        fun get(): XplatUtils {
            return delegate
        }
    }

    abstract fun isClient(): Boolean //ConfigApiImpl
    
    abstract fun configDir(): File //ConfigApiImpl

    abstract fun configName(): String //ConfigScreenManager

    abstract fun customScopes(): List<String> //ClientConfigRegistry
}
