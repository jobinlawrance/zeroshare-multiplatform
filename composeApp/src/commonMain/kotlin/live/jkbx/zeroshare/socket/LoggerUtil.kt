package live.jkbx.zeroshare.socket

import co.touchlab.kermit.Logger
import org.slf4j.Marker

fun getKermitLoggerForSLF4J(log: Logger): org.slf4j.Logger {
    return object: org.slf4j.Logger {
        override fun error(message: String?, cause: Throwable?) {
            log.e(cause) { message.orEmpty() }
        }

        override fun error(marker: Marker?, msg: String?) {

        }

        override fun error(marker: Marker?, format: String?, arg: Any?) {

        }

        override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {

        }

        override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {

        }

        override fun error(marker: Marker?, msg: String?, t: Throwable?) {

        }

        override fun warn(message: String?) {
            log.w { message.orEmpty() }
        }

        override fun warn(format: String?, arg: Any?) {

        }

        override fun warn(format: String?, vararg arguments: Any?) {

        }

        override fun warn(format: String?, arg1: Any?, arg2: Any?) {

        }

        override fun warn(msg: String?, t: Throwable?) {

        }

        override fun warn(marker: Marker?, msg: String?) {

        }

        override fun warn(marker: Marker?, format: String?, arg: Any?) {

        }

        override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {

        }

        override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {

        }

        override fun warn(marker: Marker?, msg: String?, t: Throwable?) {

        }

        override fun info(message: String?) {
            log.i { message.orEmpty() }
        }

        override fun info(format: String?, arg: Any?) {

        }

        override fun info(format: String?, arg1: Any?, arg2: Any?) {

        }

        override fun info(format: String?, vararg arguments: Any?) {

        }

        override fun info(msg: String?, t: Throwable?) {

        }

        override fun info(marker: Marker?, msg: String?) {

        }

        override fun info(marker: Marker?, format: String?, arg: Any?) {

        }

        override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {

        }

        override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {

        }

        override fun info(marker: Marker?, msg: String?, t: Throwable?) {

        }

        override fun debug(message: String?) {
            log.d { message.orEmpty() }
        }

        override fun debug(format: String?, arg: Any?) {

        }

        override fun debug(format: String?, arg1: Any?, arg2: Any?) {

        }

        override fun debug(format: String?, vararg arguments: Any?) {

        }

        override fun debug(msg: String?, t: Throwable?) {

        }

        override fun debug(marker: Marker?, msg: String?) {

        }

        override fun debug(marker: Marker?, format: String?, arg: Any?) {

        }

        override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {

        }

        override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {

        }

        override fun debug(marker: Marker?, msg: String?, t: Throwable?) {

        }

        override fun trace(message: String?) {
            log.v { message.orEmpty() }
        }

        override fun trace(format: String?, arg: Any?) {

        }

        override fun trace(format: String?, arg1: Any?, arg2: Any?) {

        }

        override fun trace(format: String?, vararg arguments: Any?) {

        }

        override fun trace(msg: String?, t: Throwable?) {

        }

        override fun trace(marker: Marker?, msg: String?) {

        }

        override fun trace(marker: Marker?, format: String?, arg: Any?) {

        }

        override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {

        }

        override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {

        }

        override fun trace(marker: Marker?, msg: String?, t: Throwable?) {

        }

        override fun getName(): String = "CustomLogger"

        override fun isErrorEnabled(): Boolean = true
        override fun isErrorEnabled(marker: Marker?): Boolean = true
        override fun error(msg: String?) {

        }

        override fun error(format: String?, arg: Any?) {

        }

        override fun error(format: String?, arg1: Any?, arg2: Any?) {

        }

        override fun error(format: String?, vararg arguments: Any?) {

        }

        override fun isWarnEnabled(): Boolean = true
        override fun isWarnEnabled(marker: Marker?): Boolean = true
        override fun isInfoEnabled(): Boolean = true
        override fun isInfoEnabled(marker: Marker?): Boolean = true
        override fun isDebugEnabled(): Boolean = true
        override fun isDebugEnabled(marker: Marker?): Boolean = true
        override fun isTraceEnabled(): Boolean = true
        override fun isTraceEnabled(marker: Marker?): Boolean = true


    }
}