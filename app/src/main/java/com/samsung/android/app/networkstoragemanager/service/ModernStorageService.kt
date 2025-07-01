package com.samsung.android.app.networkstoragemanager.service

import androidx.lifecycle.LifecycleOwner
import com.samsung.android.app.networkstoragemanager.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Modern service callbacks replacing legacy AIDL callbacks
 */
interface StorageServiceCallback {
    fun onStorageAdded(storage: StorageConfiguration) {}
    fun onStorageRemoved(storageId: String) {}
    fun onStorageUpdated(storage: StorageConfiguration) {}
    fun onStorageListUpdated(storages: List<StorageConfiguration>) {}
    fun onOperationProgress(status: StorageOperationStatus) {}
    fun onError(error: StorageException) {}
}

/**
 * Modern storage service interface replacing legacy IRequestInterface
 * Uses coroutines and type-safe parameters instead of Bundle-based AIDL
 */
interface ModernStorageService {
    
    // Core storage operations
    suspend fun addStorage(configuration: StorageConfiguration): StorageResult<String>
    suspend fun removeStorage(storageId: String): StorageResult<Unit>
    suspend fun updateStorage(storageId: String, configuration: StorageConfiguration): StorageResult<Unit>
    suspend fun listStorages(): StorageResult<List<StorageConfiguration>>
    suspend fun getStorage(storageId: String): StorageResult<StorageConfiguration>
    
    // Connection testing
    suspend fun testConnection(configuration: StorageConfiguration): StorageResult<Boolean>
    
    // File operations (to be expanded)
    suspend fun listFiles(storageId: String, path: String = "/"): StorageResult<List<StorageFile>>
    suspend fun createFolder(storageId: String, path: String, name: String): StorageResult<Unit>
    suspend fun deleteFile(storageId: String, path: String): StorageResult<Unit>
    suspend fun renameFile(storageId: String, oldPath: String, newName: String): StorageResult<Unit>
    
    // Reactive state management
    val storagesFlow: StateFlow<List<StorageConfiguration>>
    val operationStatusFlow: Flow<StorageOperationStatus>
    val errorsFlow: Flow<StorageException>
    
    // Lifecycle-aware callback management
    fun registerCallback(owner: LifecycleOwner, callback: StorageServiceCallback)
    fun registerCallback(callback: StorageServiceCallback) // For non-lifecycle aware contexts
    fun unregisterCallback(callback: StorageServiceCallback)
}

/**
 * File representation for storage operations
 */
data class StorageFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val permissions: String? = null,
    val mimeType: String? = null
)

/**
 * Bridge interface to connect modern service with legacy AIDL implementation
 * This allows gradual migration from old to new architecture
 */
interface LegacyServiceBridge {
    
    /**
     * Convert modern StorageRequest to legacy Bundle format
     */
    fun convertToLegacyRequest(request: StorageRequest): Pair<Int, android.os.Bundle>
    
    /**
     * Convert legacy Bundle response to modern StorageResult
     */
    fun convertFromLegacyResponse(requestCode: Int, bundle: android.os.Bundle?): StorageResult<Any>
    
    /**
     * Map modern storage configuration to legacy format
     */
    fun convertToLegacyConfiguration(config: StorageConfiguration): android.os.Bundle
    
    /**
     * Map legacy Bundle to modern storage configuration
     */
    fun convertFromLegacyConfiguration(bundle: android.os.Bundle): StorageConfiguration?
}

/**
 * Service factory for dependency injection
 */
interface StorageServiceFactory {
    fun createStorageService(): ModernStorageService
    fun createLegacyBridge(): LegacyServiceBridge
}

/**
 * Simple implementation of ModernStorageService for testing/demo purposes
 */
class ModernStorageServiceImpl : ModernStorageService {
    
    private val _storages = mutableListOf<StorageConfiguration>()
    private val _storagesFlow = kotlinx.coroutines.flow.MutableStateFlow<List<StorageConfiguration>>(emptyList())
    override val storagesFlow: StateFlow<List<StorageConfiguration>> = _storagesFlow
    
    private val _operationStatusFlow = kotlinx.coroutines.flow.MutableSharedFlow<StorageOperationStatus>()
    override val operationStatusFlow: Flow<StorageOperationStatus> = _operationStatusFlow
    
    private val _errorsFlow = kotlinx.coroutines.flow.MutableSharedFlow<StorageException>()
    override val errorsFlow: Flow<StorageException> = _errorsFlow
    
    private val callbacks = mutableSetOf<StorageServiceCallback>()
    
    override suspend fun addStorage(configuration: StorageConfiguration): StorageResult<String> {
        return try {
            _storages.add(configuration)
            _storagesFlow.value = _storages.toList()
            callbacks.forEach { it.onStorageAdded(configuration) }
            StorageResult.Success(configuration.id)
        } catch (e: Exception) {
            val exception = StorageException(
                code = StorageException.ErrorCode.UNKNOWN_ERROR,
                message = e.message ?: "Unknown error",
                cause = e
            )
            _errorsFlow.emit(exception)
            StorageResult.Error(exception)
        }
    }
    
    override suspend fun removeStorage(storageId: String): StorageResult<Unit> {
        return try {
            _storages.removeIf { it.id == storageId }
            _storagesFlow.value = _storages.toList()
            callbacks.forEach { it.onStorageRemoved(storageId) }
            StorageResult.Success(Unit)
        } catch (e: Exception) {
            val exception = StorageException(
                code = StorageException.ErrorCode.STORAGE_NOT_FOUND,
                message = e.message ?: "Storage not found",
                cause = e
            )
            StorageResult.Error(exception)
        }
    }
    
    override suspend fun updateStorage(storageId: String, configuration: StorageConfiguration): StorageResult<Unit> {
        return try {
            val index = _storages.indexOfFirst { it.id == storageId }
            if (index >= 0) {
                _storages[index] = configuration
                _storagesFlow.value = _storages.toList()
                callbacks.forEach { it.onStorageUpdated(configuration) }
                StorageResult.Success(Unit)
            } else {
                val exception = StorageException(
                    code = StorageException.ErrorCode.STORAGE_NOT_FOUND,
                    message = "Storage not found: $storageId"
                )
                StorageResult.Error(exception)
            }
        } catch (e: Exception) {
            val exception = StorageException(
                code = StorageException.ErrorCode.UNKNOWN_ERROR,
                message = e.message ?: "Unknown error",
                cause = e
            )
            StorageResult.Error(exception)
        }
    }
    
    override suspend fun listStorages(): StorageResult<List<StorageConfiguration>> {
        return StorageResult.Success(_storages.toList())
    }
    
    override suspend fun getStorage(storageId: String): StorageResult<StorageConfiguration> {
        val storage = _storages.find { it.id == storageId }
        return if (storage != null) {
            StorageResult.Success(storage)
        } else {
            val exception = StorageException(
                code = StorageException.ErrorCode.STORAGE_NOT_FOUND,
                message = "Storage not found: $storageId"
            )
            StorageResult.Error(exception)
        }
    }
    
    override suspend fun testConnection(configuration: StorageConfiguration): StorageResult<Boolean> {
        // Simple mock implementation for now
        return try {
            // In a real implementation, this would test the actual connection
            StorageResult.Success(true)
        } catch (e: Exception) {
            val exception = StorageException(
                code = StorageException.ErrorCode.CONNECTION_TIMEOUT,
                message = e.message ?: "Connection failed",
                cause = e
            )
            StorageResult.Error(exception)
        }
    }
    
    override suspend fun listFiles(storageId: String, path: String): StorageResult<List<StorageFile>> {
        // Mock implementation
        return StorageResult.Success(emptyList())
    }
    
    override suspend fun createFolder(storageId: String, path: String, name: String): StorageResult<Unit> {
        return StorageResult.Success(Unit)
    }
    
    override suspend fun deleteFile(storageId: String, path: String): StorageResult<Unit> {
        return StorageResult.Success(Unit)
    }
    
    override suspend fun renameFile(storageId: String, oldPath: String, newName: String): StorageResult<Unit> {
        return StorageResult.Success(Unit)
    }
    
    override fun registerCallback(owner: LifecycleOwner, callback: StorageServiceCallback) {
        callbacks.add(callback)
        // TODO: Implement lifecycle observation to auto-remove callbacks
    }
    
    override fun registerCallback(callback: StorageServiceCallback) {
        callbacks.add(callback)
    }
    
    override fun unregisterCallback(callback: StorageServiceCallback) {
        callbacks.remove(callback)
    }
}
