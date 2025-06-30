package com.samsung.cifs.storage.manager

import android.os.ParcelFileDescriptor
import com.samsung.cifs.common.values.AccessMode
import com.samsung.cifs.common.values.StorageType
import com.samsung.cifs.common.values.ThumbnailType
import com.samsung.cifs.storage.StorageClient
import com.samsung.cifs.storage.StorageFile
import com.samsung.cifs.storage.StorageRequest
import com.samsung.cifs.storage.jcifsng.JCifsNgClient
import com.samsung.cifs.storage.smbj.SmbjClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage Client Manager
 */
@Singleton
class StorageClientManager @Inject constructor(
    private val documentFileManager: DocumentFileManager,
    private val fileDescriptorManager: FileDescriptorManager,
) {

    /** jCIFS NG (SMB2,3) client */
    private val jCifsNgClient = lazy {
        JCifsNgClient(isSmb1 = false)
    }

    /** SMBJ (SMB2,3) client */
    private val smbjClient = lazy {
        SmbjClient()
    }

    /** jCIFS NG (SMB1) client */
    private val jCifsNgLegacyClient = lazy {
        JCifsNgClient(isSmb1 = true)
    }

    /** Client map */
    private val clientMap = mapOf(
        StorageType.JCIFS to jCifsNgClient,
        StorageType.SMBJ to smbjClient,
        StorageType.JCIFS_LEGACY to jCifsNgLegacyClient,
    )

    /**
     * Get client
     */
    private fun getClient(type: StorageType): StorageClient {
        return clientMap.getValue(type).value
    }

    /**
     * Close clients
     */
    suspend fun closeClient() {
        clientMap.values.forEach {
            if (it.isInitialized()) it.value.close()
        }
        fileDescriptorManager.close()
    }

    suspend fun getFile(request: StorageRequest, ignoreCache: Boolean = false): StorageFile {
        return getClient(request.connection.storage).getFile(request, ignoreCache)
    }

    suspend fun getChildren(request: StorageRequest, ignoreCache: Boolean = false): List<StorageFile> {
        return getClient(request.connection.storage).getChildren(request, ignoreCache)
    }

    suspend fun createDirectory(request: StorageRequest): StorageFile {
        return getClient(request.connection.storage).createDirectory(request)
    }

    suspend fun createFile(request: StorageRequest): StorageFile {
        return getClient(request.connection.storage).createFile(request)
    }

    suspend fun copyFile(sourceRequest: StorageRequest, targetRequest: StorageRequest): StorageFile {
        return getClient(sourceRequest.connection.storage).copyFile(sourceRequest, targetRequest)
    }

    suspend fun renameFile(request: StorageRequest, newName: String): StorageFile {
        return getClient(request.connection.storage).renameFile(request, newName)
    }

    suspend fun moveFile(sourceRequest: StorageRequest, targetRequest: StorageRequest): StorageFile {
        return getClient(sourceRequest.connection.storage).moveFile(sourceRequest, targetRequest)
    }

    suspend fun deleteFile(request: StorageRequest): Boolean {
        return getClient(request.connection.storage).deleteFile(request)
    }

    suspend fun removeCache(request: StorageRequest): Boolean {
        return getClient(request.connection.storage).removeCache(request)
    }

    suspend fun getFileDescriptor(request: StorageRequest, mode: AccessMode, onFileRelease: suspend () -> Unit): ParcelFileDescriptor {
        return fileDescriptorManager.getFileDescriptor(
            accessMode = mode,
            callback = getClient(request.connection.storage).getProxyFileDescriptorCallback(request, mode, onFileRelease)
        )
    }

    suspend fun getThumbnailDescriptor(
        request: StorageRequest,
        onFileRelease: suspend () -> Unit
    ): ParcelFileDescriptor? {
        return when (request.thumbnailType) {
            ThumbnailType.IMAGE -> {
                getFileDescriptor(
                    request = request,
                    mode = AccessMode.R,
                    onFileRelease = onFileRelease
                )
            }
            ThumbnailType.AUDIO,
            ThumbnailType.VIDEO, -> {
                fileDescriptorManager.getThumbnailDescriptor(
                    getFileDescriptor = { getFileDescriptor(request, AccessMode.R) {} },
                    onFileRelease = onFileRelease
                )
            }
            else -> null
        }
    }

    fun cancelThumbnailLoading() {
        fileDescriptorManager.cancelThumbnailLoading()
    }
}
