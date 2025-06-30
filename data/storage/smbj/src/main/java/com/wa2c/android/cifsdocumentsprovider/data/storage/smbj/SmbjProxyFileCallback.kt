package com.samsung.cifs.storage.smbj

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import com.hierynomus.smbj.share.File
import com.samsung.cifs.common.utils.logD
import com.samsung.cifs.common.values.AccessMode
import com.samsung.cifs.storage.utils.BackgroundBufferReader
import com.samsung.cifs.storage.utils.checkAccessMode
import com.samsung.cifs.storage.utils.processFileIo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Proxy File Callback for SMBJ (Buffering IO)
 */
class SmbjProxyFileCallback(
    private val file: File,
    private val accessMode: AccessMode,
    private val onFileRelease: suspend () -> Unit,
) : ProxyFileDescriptorCallback(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    /** File size */
    private val fileSize: Long by lazy {
        processFileIo(coroutineContext) { file.fileInformation.standardInformation.endOfFile }
    }

    private val readerLazy = lazy {
        BackgroundBufferReader(fileSize) { start, array, off, len ->
            file.read(array, start, off, len)
        }
    }

    private val reader: BackgroundBufferReader get() = readerLazy.value

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        return fileSize
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo(coroutineContext) {
            reader.readBuffer(offset, size, data)
        }
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo(coroutineContext) {
            checkAccessMode(accessMode)
            file.writeAsync(data, offset, 0, size)
            size
        }
    }
    
    @Throws(ErrnoException::class)
    override fun onFsync() {
        // Nothing to do
    }

    @Throws(ErrnoException::class)
    override fun onRelease() {
        logD("onRelease: ${file.uncPath}")
        processFileIo(coroutineContext) {
            logD("release begin")
            if (readerLazy.isInitialized()) {
                reader.close()
            }
            onFileRelease()
            logD("release end")
        }
    }

}
