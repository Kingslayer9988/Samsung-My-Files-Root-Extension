package com.samsung.android.app.networkstoragemanager.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.app.networkstoragemanager.R

/**
 * Activity for adding root storage locations 
 * Compatible with both Samsung My Files legacy integration and modern UI
 */
class AddRootLocationActivity : AppCompatActivity() {
    
    private var isLegacyMode = false
    
    // Modern launcher for directory picker
    private val directoryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { 
            handleDirectorySelected(it.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Detect if launched by Samsung My Files (legacy mode) or modern test app
        isLegacyMode = detectLegacyMode()
        
        if (isLegacyMode) {
            handleLegacyLaunch()
        } else {
            createModernUI()
        }
    }

    private fun detectLegacyMode(): Boolean {
        val data = intent.data
        val action = intent.action
        val extras = intent.extras
        
        // Samsung My Files launches with:
        // 1. ftp:// scheme (from manifest intent filter)
        // 2. Bundle extras with server data
        // 3. Specific action patterns
        return data?.scheme == "ftp" || 
               extras?.containsKey("serverId") == true ||
               extras?.containsKey("serverAddr") == true ||
               (action == Intent.ACTION_MAIN && intent.categories?.contains("android.intent.category.DEFAULT") == true)
    }

    private fun handleLegacyLaunch() {
        // For Samsung My Files compatibility, show simple feedback and return success
        showSimpleLegacyUI()
        
        // Log the intent details for debugging
        val data = intent.data
        val extras = intent.extras
        val message = "Samsung My Files Integration\n\n" +
                     "Scheme: ${data?.scheme}\n" +
                     "Action: ${intent.action}\n" +
                     "Has extras: ${extras != null}\n\n" +
                     "This would configure root storage.\n" +
                     "Use 'Modern Storage Manager Test' for full UI."
        
        showToast(message)
        
        // Return success to Samsung My Files
        setResult(RESULT_OK)
        
        // Auto-close after showing message
        findViewById<TextView>(android.R.id.text1)?.postDelayed({
            finish()
        }, 4000)
    }

    private fun showSimpleLegacyUI() {
        // Use simple built-in layout for legacy mode
        setContentView(android.R.layout.simple_list_item_1)
        findViewById<TextView>(android.R.id.text1)?.apply {
            text = "Root Storage Configuration\n\nLaunched by Samsung My Files\nConfiguring root storage access...\n\nFor full functionality, use:\n'Modern Storage Manager Test' app"
            textSize = 16f
            setPadding(32, 32, 32, 32)
        }
    }

    private fun createModernUI() {
        try {
            // Try to use the proper layout if it exists
            setContentView(R.layout.activity_add_root_location)
            setupModernUI()
        } catch (e: Exception) {
            // Fallback to programmatic layout if resource is missing
            createProgrammaticLayout()
        }
    }

    private fun createProgrammaticLayout() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Add Root Storage Location"
            textSize = 20f
            setPadding(0, 0, 0, 32)
        }

        val pathLabel = TextView(this).apply {
            text = "Selected Path:"
            setPadding(0, 0, 0, 8)
        }

        val pathText = TextView(this).apply {
            text = "No path selected"
            setPadding(0, 0, 0, 16)
            id = View.generateViewId()
        }

        val nameLabel = TextView(this).apply {
            text = "Storage Name:"
            setPadding(0, 0, 0, 8)
        }

        val nameInput = EditText(this).apply {
            hint = "Enter storage name"
            setPadding(0, 0, 0, 16)
            id = View.generateViewId()
        }

        val browseButton = Button(this).apply {
            text = "Browse for Root Folder"
            setPadding(0, 0, 0, 16)
            setOnClickListener { launchDirectoryPicker() }
        }

        val addButton = Button(this).apply {
            text = "Add Root Storage"
            isEnabled = false
            setOnClickListener { addRootStorage() }
            id = View.generateViewId()
        }

        layout.addView(title)
        layout.addView(pathLabel)
        layout.addView(pathText)
        layout.addView(nameLabel)
        layout.addView(nameInput)
        layout.addView(browseButton)
        layout.addView(addButton)

        setContentView(layout)
    }

    private fun setupModernUI() {
        // Set up the action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Add Root Location"
        }

        // Set up click listeners for buttons (if they exist in the layout)
        findViewById<Button>(R.id.btn_browse_root)?.setOnClickListener {
            launchDirectoryPicker()
        }

        findViewById<Button>(R.id.btn_add_root)?.setOnClickListener {
            addRootStorage()
        }
    }

    private fun launchDirectoryPicker() {
        try {
            directoryPickerLauncher.launch(null)
        } catch (e: Exception) {
            showToast("Cannot open directory picker: ${e.message}")
        }
    }

    private fun handleDirectorySelected(path: String) {
        findViewById<TextView>(R.id.tv_selected_path)?.text = path
        findViewById<Button>(R.id.btn_add_root)?.isEnabled = true
    }

    private fun addRootStorage() {
        val pathView = findViewById<TextView>(R.id.tv_selected_path)
        val nameView = findViewById<EditText>(R.id.et_storage_name)

        val path = pathView?.text?.toString()
        val name = nameView?.text?.toString()

        if (path.isNullOrEmpty() || path == "No path selected" || name.isNullOrEmpty()) {
            showToast("Please select a path and enter a storage name")
            return
        }

        showToast("Root storage would be added: $name at $path")
        
        // Return result and close
        setResult(RESULT_OK)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
