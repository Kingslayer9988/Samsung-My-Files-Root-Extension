package com.samsung.android.app.networkstoragemanager.provider

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.util.Log
import com.samsung.android.app.networkstoragemanager.R
import com.samsung.android.app.networkstoragemanager.activity.AddRootLocationActivity
import com.samsung.android.app.networkstoragemanager.activity.AddSmbShareActivity
import com.samsung.android.app.networkstoragemanager.activity.BrowseRootFoldersActivity

/**
 * Custom DocumentProvider to bypass Samsung My Files protocol detection
 * This provider creates our own storage roots that Samsung cannot intercept
 */
class CustomStorageProvider : DocumentsProvider() {
    
    companion object {
        private const val TAG = "CustomStorageProvider"
        private const val AUTHORITY = "com.samsung.android.app.networkstoragemanager.documents"
        
        // Root IDs for our custom storage types
        private const val ROOT_ID_CUSTOM = "custom_storage_root"
        private const val ROOT_ID_ROOT = "root_storage"
        private const val ROOT_ID_SMB = "smb_storage" 
        private const val ROOT_ID_BROWSE = "browse_root"
        
        // Document IDs
        private const val DOC_ID_ROOT = "root"
        private const val DOC_ID_ADD_ROOT = "add_root"
        private const val DOC_ID_ADD_SMB = "add_smb"
        private const val DOC_ID_BROWSE_ROOT = "browse_root"
        
        // Custom URI schemes that Samsung cannot recognize
        const val SCHEME_CUSTOM_ROOT = "customroot"
        const val SCHEME_CUSTOM_SMB = "customsmb"
        const val SCHEME_BROWSE_ROOT = "browseroot"
        
        // Default projection for root queries
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
        )
        
        // Default projection for document queries
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_ICON
        )
    }
    
    override fun onCreate(): Boolean {
        Log.d(TAG, "CustomStorageProvider onCreate")
        return true
    }
    
    override fun queryRoots(projection: Array<String>?): Cursor {
        Log.d(TAG, "queryRoots called")
        
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        
        // Main custom storage root
        val mainRow = result.newRow()
        mainRow.add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID_CUSTOM)
        mainRow.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, DOC_ID_ROOT)
        mainRow.add(DocumentsContract.Root.COLUMN_TITLE, "Custom Storage")
        mainRow.add(DocumentsContract.Root.COLUMN_SUMMARY, "Root and Network Storage")
        mainRow.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_storage)
        mainRow.add(DocumentsContract.Root.COLUMN_FLAGS, 
            DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
            DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD or
            DocumentsContract.Root.FLAG_LOCAL_ONLY)
        mainRow.add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
        
        Log.d(TAG, "Added custom storage root")
        return result
    }
    
    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        Log.d(TAG, "queryDocument: $documentId")
        
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        
        when (documentId) {
            DOC_ID_ROOT -> {
                val row = result.newRow()
                row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DOC_ID_ROOT)
                row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
                row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "Custom Storage")
                row.add(DocumentsContract.Document.COLUMN_FLAGS, 
                    DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE)
                row.add(DocumentsContract.Document.COLUMN_ICON, R.drawable.ic_storage)
            }
            
            DOC_ID_ADD_ROOT -> {
                val row = result.newRow()
                row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DOC_ID_ADD_ROOT)
                row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, "vnd.android.document/action")
                row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "Add Root Location")
                row.add(DocumentsContract.Document.COLUMN_FLAGS, 
                    DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT)
                row.add(DocumentsContract.Document.COLUMN_ICON, R.drawable.ic_add)
            }
            
            DOC_ID_ADD_SMB -> {
                val row = result.newRow()
                row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DOC_ID_ADD_SMB)
                row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, "vnd.android.document/action")
                row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "Add SMB/CIFS Share")
                row.add(DocumentsContract.Document.COLUMN_FLAGS, 
                    DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT)
                row.add(DocumentsContract.Document.COLUMN_ICON, R.drawable.ic_network)
            }
            
            DOC_ID_BROWSE_ROOT -> {
                val row = result.newRow()
                row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DOC_ID_BROWSE_ROOT)
                row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, "vnd.android.document/action")
                row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "Browse Root Folders")
                row.add(DocumentsContract.Document.COLUMN_FLAGS, 
                    DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT)
                row.add(DocumentsContract.Document.COLUMN_ICON, R.drawable.ic_folder)
            }
        }
        
        return result
    }
    
    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?
    ): Cursor {
        Log.d(TAG, "queryChildDocuments: $parentDocumentId")
        
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        
        when (parentDocumentId) {
            DOC_ID_ROOT -> {
                // Add our action items as children of the root
                addActionDocument(result, DOC_ID_ADD_ROOT, "Add Root Location", R.drawable.ic_add)
                addActionDocument(result, DOC_ID_ADD_SMB, "Add SMB/CIFS Share", R.drawable.ic_network)
                addActionDocument(result, DOC_ID_BROWSE_ROOT, "Browse Root Folders", R.drawable.ic_folder)
            }
        }
        
        return result
    }
    
    private fun addActionDocument(cursor: MatrixCursor, docId: String, displayName: String, iconRes: Int) {
        val row = cursor.newRow()
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docId)
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, "vnd.android.document/action")
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName)
        row.add(DocumentsContract.Document.COLUMN_FLAGS, 
            DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT)
        row.add(DocumentsContract.Document.COLUMN_ICON, iconRes)
        row.add(DocumentsContract.Document.COLUMN_SIZE, 0)
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis())
    }
    
    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        Log.d(TAG, "openDocument: $documentId")
        
        // For action documents, we launch the appropriate activity
        when (documentId) {
            DOC_ID_ADD_ROOT -> {
                launchActivity(AddRootLocationActivity::class.java)
            }
            DOC_ID_ADD_SMB -> {
                launchActivity(AddSmbShareActivity::class.java)
            }
            DOC_ID_BROWSE_ROOT -> {
                launchActivity(BrowseRootFoldersActivity::class.java)
            }
        }
        
        throw UnsupportedOperationException("Action documents cannot be opened as files")
    }
    
    private fun launchActivity(activityClass: Class<*>) {
        Log.d(TAG, "Launching activity: ${activityClass.simpleName}")
        
        try {
            val context = context ?: return
            val intent = Intent(context, activityClass).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("launched_from_provider", true)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch activity: ${activityClass.simpleName}", e)
        }
    }
    
    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        Log.d(TAG, "createDocument: $parentDocumentId, $mimeType, $displayName")
        throw UnsupportedOperationException("Create document not supported")
    }
    
    override fun deleteDocument(documentId: String) {
        Log.d(TAG, "deleteDocument: $documentId")
        throw UnsupportedOperationException("Delete document not supported")
    }
    
    override fun getDocumentType(documentId: String): String {
        return when (documentId) {
            DOC_ID_ROOT -> DocumentsContract.Document.MIME_TYPE_DIR
            DOC_ID_ADD_ROOT, DOC_ID_ADD_SMB, DOC_ID_BROWSE_ROOT -> "vnd.android.document/action"
            else -> "application/octet-stream"
        }
    }
    
    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return when (parentDocumentId) {
            DOC_ID_ROOT -> documentId in listOf(DOC_ID_ADD_ROOT, DOC_ID_ADD_SMB, DOC_ID_BROWSE_ROOT)
            else -> false
        }
    }
}
