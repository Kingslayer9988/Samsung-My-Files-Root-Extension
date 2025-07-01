package com.samsung.android.app.networkstoragemanager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Modern type-safe storage configuration classes replacing Bundle-based approach
 */
@Parcelize
sealed interface StorageConfiguration : Parcelable {
    val id: String
    val displayName: String
    val isEnabled: Boolean
    
    @Parcelize
    data class RootStorage(
        override val id: String,
        override val displayName: String,
        override val isEnabled: Boolean = true,
        val rootPath: String,
        val description: String? = null
    ) : StorageConfiguration
    
    @Parcelize
    data class SmbStorage(
        override val id: String,
        override val displayName: String,
        override val isEnabled: Boolean = true,
        val host: String,
        val port: Int = 445,
        val username: String,
        val password: String, // Consider encryption in production
        val shareName: String,
        val workgroup: String? = null,
        val description: String? = null
    ) : StorageConfiguration
}

/**
 * Type-safe request classes replacing integer request codes
 */
@Parcelize
sealed interface StorageRequest : Parcelable {
    val requestId: String
    val timestamp: Long
    
    @Parcelize
    data class AddStorageRequest(
        override val requestId: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val configuration: StorageConfiguration
    ) : StorageRequest
    
    @Parcelize
    data class RemoveStorageRequest(
        override val requestId: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val storageId: String
    ) : StorageRequest
    
    @Parcelize
    data class ListStorageRequest(
        override val requestId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : StorageRequest
    
    @Parcelize
    data class TestConnectionRequest(
        override val requestId: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val configuration: StorageConfiguration
    ) : StorageRequest
    
    @Parcelize
    data class UpdateStorageRequest(
        override val requestId: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val storageId: String,
        val updatedConfiguration: StorageConfiguration
    ) : StorageRequest
}

/**
 * Modern result handling replacing Bundle-based callbacks
 */
sealed interface StorageResult<out T> {
    data class Success<T>(val data: T) : StorageResult<T>
    data class Error(val exception: StorageException) : StorageResult<Nothing>
    object Loading : StorageResult<Nothing>
}

/**
 * Type-safe error handling
 */
data class StorageException(
    val code: ErrorCode,
    override val message: String,
    override val cause: Throwable? = null,
    val requestId: String? = null
) : Exception(message, cause) {
    
    enum class ErrorCode {
        // Network related errors
        NETWORK_UNREACHABLE,
        CONNECTION_TIMEOUT,
        CONNECTION_REFUSED,
        
        // Authentication errors
        AUTHENTICATION_FAILED,
        INVALID_CREDENTIALS,
        PERMISSION_DENIED,
        
        // Storage related errors
        STORAGE_NOT_FOUND,
        STORAGE_ALREADY_EXISTS,
        INVALID_CONFIGURATION,
        INVALID_PATH,
        
        // System errors
        INSUFFICIENT_PERMISSIONS,
        ROOT_ACCESS_DENIED,
        
        // Generic errors
        UNKNOWN_ERROR,
        OPERATION_CANCELLED
    }
}

/**
 * Storage operation status for UI updates
 */
@Parcelize
data class StorageOperationStatus(
    val requestId: String,
    val operation: String,
    val progress: Int = 0, // 0-100
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
