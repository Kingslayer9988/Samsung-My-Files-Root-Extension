package com.samsung.android.app.networkstoragemanager.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.samsung.android.app.networkstoragemanager.model.StorageConfiguration
import com.samsung.android.app.networkstoragemanager.model.StorageException
import com.samsung.android.app.networkstoragemanager.model.StorageResult
import com.samsung.android.app.networkstoragemanager.service.ModernStorageService
import com.samsung.android.app.networkstoragemanager.service.StorageServiceCallback
import kotlinx.coroutines.launch

/**
 * Modern base activity following Android Architecture Components patterns
 * Replaces legacy Activity implementations with proper lifecycle management
 */
abstract class ModernStorageActivity : AppCompatActivity(), StorageServiceCallback {
    
    companion object {
        private const val TAG = "ModernStorageActivity"
    }
    
    // Modern service interface (will be injected later)
    protected open val storageService: ModernStorageService? by lazy {
        com.samsung.android.app.networkstoragemanager.service.ModernStorageServiceImpl()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Modern edge-to-edge display
        enableEdgeToEdge()
        
        // Set up window insets handling
        setupWindowInsets()
        
        // Register for storage service callbacks
        registerServiceCallbacks()
        
        Log.d(TAG, "${this::class.simpleName} onCreate")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Unregister callbacks to prevent memory leaks
        unregisterServiceCallbacks()
        
        Log.d(TAG, "${this::class.simpleName} onDestroy")
    }
    
    private fun setupWindowInsets() {
        // Handle system bars insets properly
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    private fun registerServiceCallbacks() {
        storageService?.registerCallback(this, this)
    }
    
    private fun unregisterServiceCallbacks() {
        storageService?.unregisterCallback(this)
    }
    
    // Helper methods for common operations
    protected fun addStorageConfiguration(config: StorageConfiguration) {
        lifecycleScope.launch {
            try {
                showLoading(true)
                val result = storageService?.addStorage(config)
                when (result) {
                    is StorageResult.Success -> {
                        onStorageAdded(config)
                        finish() // Close activity on success
                    }
                    is StorageResult.Error -> {
                        onError(result.exception)
                    }
                    else -> {
                        onError(StorageException(
                            StorageException.ErrorCode.UNKNOWN_ERROR,
                            "Service not available"
                        ))
                    }
                }
            } catch (e: Exception) {
                onError(StorageException(
                    StorageException.ErrorCode.UNKNOWN_ERROR,
                    e.message ?: "Unknown error",
                    e
                ))
            } finally {
                showLoading(false)
            }
        }
    }
    
    protected fun testStorageConnection(config: StorageConfiguration) {
        lifecycleScope.launch {
            try {
                showLoading(true)
                val result = storageService?.testConnection(config)
                when (result) {
                    is StorageResult.Success -> {
                        onConnectionTestResult(result.data)
                    }
                    is StorageResult.Error -> {
                        onError(result.exception)
                    }
                    else -> {
                        onError(StorageException(
                            StorageException.ErrorCode.UNKNOWN_ERROR,
                            "Service not available"
                        ))
                    }
                }
            } catch (e: Exception) {
                onError(StorageException(
                    StorageException.ErrorCode.UNKNOWN_ERROR,
                    e.message ?: "Connection test failed",
                    e
                ))
            } finally {
                showLoading(false)
            }
        }
    }
    
    // Abstract methods for subclasses to implement
    abstract fun showLoading(show: Boolean)
    abstract fun onConnectionTestResult(success: Boolean)
    
    // StorageServiceCallback implementations with default behavior
    override fun onStorageAdded(storage: StorageConfiguration) {
        Log.d(TAG, "Storage added: ${storage.displayName}")
        // Default implementation - subclasses can override
    }
    
    override fun onStorageRemoved(storageId: String) {
        Log.d(TAG, "Storage removed: $storageId")
        // Default implementation - subclasses can override
    }
    
    override fun onError(error: StorageException) {
        Log.e(TAG, "Storage error: ${error.message}", error.cause)
        showErrorMessage(error.message)
    }
    
    // Helper method to show error messages (to be implemented by subclasses)
    protected open fun showErrorMessage(message: String) {
        // Default implementation using simple log
        Log.e(TAG, "Error: $message")
    }
}
