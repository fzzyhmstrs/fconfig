package me.fzzyhmstrs.fzzy_config.util.platform.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker

class DevLogger(name: String): Logger {
    
    private val delegate = LoggerFactory.getLogger(name)
    
    override fun getName(): String {
        return delegate.name
    }

    override fun isTraceEnabled(): Boolean {
        return delegate.isTraceEnabled
    }

    override fun isTraceEnabled(marker: Marker?): Boolean {
        return delegate.isTraceEnabled(marker)
    }

    override fun trace(msg: String?) {
        if (PlatformUtils.isDev())
            delegate.trace(msg)
    }

    override fun trace(format: String?, arg: Any?) {
        if (PlatformUtils.isDev())
            delegate.trace(format, arg)
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        if (PlatformUtils.isDev())
            delegate.trace(format, arg1, arg2)
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        if (PlatformUtils.isDev())
            delegate.trace(format, *arguments)
    }

    override fun trace(msg: String?, t: Throwable?) {
        if (PlatformUtils.isDev())
            delegate.trace(msg, t)
    }

    override fun trace(marker: Marker?, msg: String?) {
        if (PlatformUtils.isDev())
            delegate.trace(marker, msg)
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        if (PlatformUtils.isDev())
            delegate.trace(marker, format, arg)
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (PlatformUtils.isDev())
            delegate.trace(marker, format, arg1, arg2)
    }

    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
        if (PlatformUtils.isDev())
            delegate.trace(marker, format, *argArray)
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        if (PlatformUtils.isDev())
            delegate.trace(marker, msg, t)
    }

    override fun isDebugEnabled(): Boolean {
        return delegate.isDebugEnabled
    }

    override fun isDebugEnabled(marker: Marker?): Boolean {
        return delegate.isDebugEnabled(marker)
    }

    override fun debug(msg: String?) {
        if (PlatformUtils.isDev())
            delegate.debug(msg)
    }

    override fun debug(format: String?, arg: Any?) {
        if (PlatformUtils.isDev())
            delegate.debug(format, arg)
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        if (PlatformUtils.isDev())
            delegate.debug(format, arg1, arg2)
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        if (PlatformUtils.isDev())
            delegate.debug(format, *arguments)
    }

    override fun debug(msg: String?, t: Throwable?) {
        if (PlatformUtils.isDev())
            delegate.debug(msg, t)
    }

    override fun debug(marker: Marker?, msg: String?) {
        if (PlatformUtils.isDev())
            delegate.debug(marker, msg)
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        if (PlatformUtils.isDev())
            delegate.debug(marker, format, arg)
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (PlatformUtils.isDev())
            delegate.debug(marker, format, arg1, arg2)
    }

    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (PlatformUtils.isDev())
            delegate.debug(marker, format, *arguments)
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        if (PlatformUtils.isDev())
            delegate.debug(marker, msg, t)
    }

    override fun isInfoEnabled(): Boolean {
        return delegate.isInfoEnabled
    }

    override fun isInfoEnabled(marker: Marker?): Boolean {
        return delegate.isInfoEnabled(marker)
    }

    override fun info(msg: String?) {
        if (PlatformUtils.isDev())
            delegate.info(msg)
    }

    override fun info(format: String?, arg: Any?) {
        if (PlatformUtils.isDev())
            delegate.info(format, arg)
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        if (PlatformUtils.isDev())
            delegate.info(format, arg1, arg2)
    }

    override fun info(format: String?, vararg arguments: Any?) {
        if (PlatformUtils.isDev())
            delegate.info(format, *arguments)
    }

    override fun info(msg: String?, t: Throwable?) {
        if (PlatformUtils.isDev())
            delegate.info(msg, t)
    }

    override fun info(marker: Marker?, msg: String?) {
        if (PlatformUtils.isDev())
            delegate.info(marker, msg)
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        if (PlatformUtils.isDev())
            delegate.info(marker, format, arg)
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (PlatformUtils.isDev())
            delegate.info(marker, format, arg1, arg2)
    }

    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (PlatformUtils.isDev())
            delegate.info(marker, format, *arguments)
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        if (PlatformUtils.isDev())
            delegate.info(marker, msg, t)
    }

    override fun isWarnEnabled(): Boolean {
        return delegate.isWarnEnabled
    }

    override fun isWarnEnabled(marker: Marker?): Boolean {
        return delegate.isWarnEnabled(marker)
    }

    override fun warn(msg: String?) {
        if (PlatformUtils.isDev())
            delegate.warn(msg)
    }

    override fun warn(format: String?, arg: Any?) {
        if (PlatformUtils.isDev())
            delegate.warn(format, arg)
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        if (PlatformUtils.isDev())
            delegate.warn(format, *arguments)
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        if (PlatformUtils.isDev())
            delegate.warn(format, arg1, arg2)
    }

    override fun warn(msg: String?, t: Throwable?) {
        if (PlatformUtils.isDev())
            delegate.warn(msg, t)
    }

    override fun warn(marker: Marker?, msg: String?) {
        if (PlatformUtils.isDev())
            delegate.warn(marker, msg)
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        if (PlatformUtils.isDev())
            delegate.warn(marker, format, arg)
    }

    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (PlatformUtils.isDev())
            delegate.warn(marker, format, arg1, arg2)
    }

    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (PlatformUtils.isDev())
            delegate.warn(marker, format, *arguments)
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        if (PlatformUtils.isDev())
            delegate.warn(marker, msg, t)
    }

    override fun isErrorEnabled(): Boolean {
        return delegate.isErrorEnabled
    }

    override fun isErrorEnabled(marker: Marker?): Boolean {
        return delegate.isErrorEnabled(marker)
    }

    override fun error(msg: String?) {
        if (PlatformUtils.isDev())
            delegate.error(msg)
    }

    override fun error(format: String?, arg: Any?) {
        if (PlatformUtils.isDev())
            delegate.error(format, arg)
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        if (PlatformUtils.isDev())
            delegate.error(format, arg1, arg2)
    }

    override fun error(format: String?, vararg arguments: Any?) {
        if (PlatformUtils.isDev())
            delegate.error(format, *arguments)
    }

    override fun error(msg: String?, t: Throwable?) {
        if (PlatformUtils.isDev())
            delegate.error(msg, t)
    }

    override fun error(marker: Marker?, msg: String?) {
        if (PlatformUtils.isDev())
            delegate.error(marker, msg)
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        if (PlatformUtils.isDev())
            delegate.error(marker, format, arg)
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (PlatformUtils.isDev())
            delegate.error(marker, format, arg1, arg2)
    }

    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
        if (PlatformUtils.isDev())
            delegate.error(marker, format, *arguments)
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        if (PlatformUtils.isDev())
            delegate.error(marker, msg, t)
    }

}