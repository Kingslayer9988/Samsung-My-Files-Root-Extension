# Modernization Implementation Plan

## Phase 1: Immediate Actions (Week 1-2)

### 1. Project Setup for Modernization

#### Update build.gradle dependencies:
```kotlin
// app/build.gradle.kts
dependencies {
    // Android Architecture Components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Modern UI
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // Type safety
    implementation("androidx.parcelize:parcelize-runtime:1.0.0")
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

### 2. Create Modern Data Classes (New Files)

#### app/src/main/java/com/samsung/android/app/networkstoragemanager/model/StorageConfiguration.kt
```kotlin
@Parcelize
sealed interface StorageConfiguration : Parcelable {
    val id: String
    val displayName: String
    
    @Parcelize
    data class RootStorage(
        override val id: String,
        override val displayName: String,
        val rootPath: String
    ) : StorageConfiguration
    
    @Parcelize
    data class SmbStorage(
        override val id: String,
        override val displayName: String,
        val host: String,
        val port: Int = 445,
        val username: String,
        val password: String,
        val shareName: String
    ) : StorageConfiguration
}

@Parcelize
sealed interface StorageRequest : Parcelable {
    val requestId: String
    
    @Parcelize
    data class AddStorageRequest(
        override val requestId: String,
        val configuration: StorageConfiguration
    ) : StorageRequest
    
    @Parcelize
    data class RemoveStorageRequest(
        override val requestId: String,
        val storageId: String
    ) : StorageRequest
    
    @Parcelize
    data class ListStorageRequest(
        override val requestId: String
    ) : StorageRequest
}

sealed interface StorageResult {
    data class Success<T>(val data: T) : StorageResult
    data class Error(val exception: Throwable, val message: String) : StorageResult
    object Loading : StorageResult
}
```

### 3. Modern Service Interface (New File)

#### app/src/main/java/com/samsung/android/app/networkstoragemanager/service/ModernStorageService.kt
```kotlin
interface StorageServiceCallback {
    fun onStorageAdded(storage: StorageConfiguration)
    fun onStorageRemoved(storageId: String)
    fun onStorageListUpdated(storages: List<StorageConfiguration>)
    fun onError(error: StorageError)
}

data class StorageError(
    val code: ErrorCode,
    val message: String,
    val cause: Throwable? = null
) {
    enum class ErrorCode {
        NETWORK_ERROR,
        AUTHENTICATION_FAILED,
        PERMISSION_DENIED,
        STORAGE_NOT_FOUND,
        INVALID_CONFIGURATION,
        UNKNOWN_ERROR
    }
}

interface ModernStorageService {
    suspend fun addStorage(configuration: StorageConfiguration): Result<String>
    suspend fun removeStorage(storageId: String): Result<Unit>
    suspend fun listStorages(): Result<List<StorageConfiguration>>
    suspend fun testConnection(configuration: StorageConfiguration): Result<Boolean>
    
    fun registerCallback(callback: StorageServiceCallback)
    fun unregisterCallback(callback: StorageServiceCallback)
}
```

### 4. Custom DocumentProvider Implementation

#### app/src/main/java/com/samsung/android/app/networkstoragemanager/provider/CustomStorageProvider.kt
```kotlin
class CustomStorageProvider : DocumentsProvider() {
    companion object {
        private const val AUTHORITY = "com.samsung.android.app.networkstoragemanager.documents"
        private const val ROOT_ID_ROOT = "root_storage"
        private const val ROOT_ID_SMB = "smb_storage"
        
        // Custom URI schemes that Samsung cannot intercept
        const val SCHEME_CUSTOM_ROOT = "customroot"
        const val SCHEME_CUSTOM_SMB = "customsmb"
    }
    
    override fun onCreate(): Boolean {
        // Initialize provider
        return true
    }
    
    override fun queryRoots(projection: Array<String>?): Cursor {
        // Return our custom storage roots
        val result = MatrixCursor(projection ?: DocumentsContract.Root.DEFAULT_ROOT_PROJECTION)
        
        // Add Root Storage root
        val rootRow = result.newRow()
        rootRow.add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID_ROOT)
        rootRow.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_ID_ROOT)
        rootRow.add(DocumentsContract.Root.COLUMN_TITLE, "Root Storage")
        rootRow.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_folder)
        rootRow.add(DocumentsContract.Root.COLUMN_FLAGS, 
            DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
            DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD)
        
        // Add SMB Storage root
        val smbRow = result.newRow()
        smbRow.add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID_SMB)
        smbRow.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_ID_SMB)
        smbRow.add(DocumentsContract.Root.COLUMN_TITLE, "SMB/CIFS Storage")
        smbRow.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_network)
        smbRow.add(DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
            DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD)
        
        return result
    }
    
    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        // Implement document querying
        TODO("Implement document querying")
    }
    
    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?
    ): Cursor {
        // Implement child document listing
        TODO("Implement child document listing")
    }
}
```

## Phase 2: Core Service Modernization (Week 3-4)

### 1. Modern Service Implementation
- Bridge between legacy AIDL and modern interfaces
- Implement lifecycle-aware callback management
- Add proper error handling and logging

### 2. Repository Pattern
- Create StorageRepository to manage storage configurations
- Implement caching and persistence
- Add network state monitoring

## Phase 3: UI Modernization (Week 5-6)

### 1. Replace Activities with Fragments
- Convert AddRootLocationActivity to Fragment
- Convert AddSmbShareActivity to Fragment  
- Implement Navigation Component routing

### 2. Modern UI Components
- Create custom dialogs following OneUIProject patterns
- Implement proper state management
- Add loading states and error handling

## Next Immediate Steps

1. **Add Kotlin support to the project**
2. **Create the modern data classes** (StorageConfiguration.kt)
3. **Implement CustomStorageProvider** to bypass Samsung's routing
4. **Test the new DocumentProvider** with Samsung My Files

Would you like me to start implementing any of these components? I recommend beginning with the Kotlin data classes and the CustomStorageProvider, as these will give us immediate functionality improvements.
