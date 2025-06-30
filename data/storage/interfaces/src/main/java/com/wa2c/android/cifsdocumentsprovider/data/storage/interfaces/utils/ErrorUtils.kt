package com.samsung.cifs.storage.utils

import android.system.ErrnoException
import android.system.OsConstants
import com.samsung.cifs.common.exception.StorageException
import com.samsung.cifs.common.utils.logE
import com.samsung.cifs.common.values.AccessMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.coroutines.CoroutineContext

/**
 * Proxy Callback process
 */
@Throws(ErrnoException::class)
fun <T> processFileIo(context: CoroutineContext, process: suspend CoroutineScope.() -> T): T {
    return try {
        runBlocking(context = context) {
            process()
        }
    } catch (e: IOException) {
        logE(e)
        when (e.cause) {
            is ErrnoException -> throw (e.cause as ErrnoException)
            is StorageException -> throw ErrnoException("Writing", OsConstants.EBADF, e)
            else -> throw ErrnoException("I/O", OsConstants.EIO, e)
        }
    }
}

/**
 * Get throwable cause.
 */
fun Throwable.getCause(): Throwable {
    val c = cause
    return c?.getCause() ?: return this
}

/**
 * Check write permission.
 */
fun checkAccessMode(mode: AccessMode) {
    if (mode != AccessMode.W) {
        throw StorageException.Operation.AccessMode()
    }
}
