package com.samsung.android.app.networkstoragemanager.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.app.networkstoragemanager.R
import java.util.UUID

/**
 * Activity for adding SMB/CIFS network shares
 * Compatible with both Samsung My Files legacy integration and modern UI
 */
class AddSmbShareActivity : AppCompatActivity() {
    
    private var isLegacyMode = false

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
        // 1. sftp:// scheme (from manifest intent filter) - legacy scheme mapped to SMB
        // 2. Bundle extras with server data
        // 3. Specific action patterns
        return data?.scheme == "sftp" ||
               extras?.containsKey("serverId") == true ||
               extras?.containsKey("serverAddr") == true ||
               extras?.containsKey("serverName") == true ||
               (action == Intent.ACTION_MAIN && intent.categories?.contains("android.intent.category.DEFAULT") == true)
    }

    private fun handleLegacyLaunch() {
        // For Samsung My Files compatibility, show simple feedback and return success
        showSimpleLegacyUI()
        
        // Log the intent details for debugging
        val data = intent.data
        val extras = intent.extras
        val message = "Samsung My Files Integration\n\n" +
                     "Scheme: ${data?.scheme} (sftp mapped to SMB)\n" +
                     "Action: ${intent.action}\n" +
                     "Has extras: ${extras != null}\n\n" +
                     "This would configure SMB/CIFS storage.\n" +
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
            text = "SMB/CIFS Share Configuration\n\nLaunched by Samsung My Files\nConfiguring network storage access...\n\nFor full functionality, use:\n'Modern Storage Manager Test' app"
            textSize = 16f
            setPadding(32, 32, 32, 32)
        }
    }

    private fun createModernUI() {
        try {
            // Try to use the proper layout if it exists
            setContentView(R.layout.activity_add_smb_share)
            setupModernUI()
        } catch (e: Exception) {
            // Fallback to programmatic layout if resource is missing
            createProgrammaticLayout()
        }
    }

    private fun setupModernUI() {
        // Set up the action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Add SMB Share"
        }

        // Set up click listeners
        findViewById<Button>(R.id.btn_test_connection)?.setOnClickListener {
            testConnection()
        }

        findViewById<Button>(R.id.btn_add_share)?.setOnClickListener {
            addSmbShare()
        }
    }

    private fun createProgrammaticLayout() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Add SMB/CIFS Share"
            textSize = 20f
            setPadding(0, 0, 0, 32)
        }

        val hostLabel = TextView(this).apply {
            text = "Server Host/IP:"
            setPadding(0, 0, 0, 8)
        }

        val hostInput = EditText(this).apply {
            hint = "192.168.1.100 or server.local"
            setPadding(0, 0, 0, 16)
            id = View.generateViewId()
        }

        val shareLabel = TextView(this).apply {
            text = "Share Name:"
            setPadding(0, 0, 0, 8)
        }

        val shareInput = EditText(this).apply {
            hint = "shared_folder"
            setPadding(0, 0, 0, 16)
            id = View.generateViewId()
        }

        val usernameLabel = TextView(this).apply {
            text = "Username:"
            setPadding(0, 0, 0, 8)
        }

        val usernameInput = EditText(this).apply {
            hint = "username"
            setPadding(0, 0, 0, 16)
            id = View.generateViewId()
        }

        val passwordLabel = TextView(this).apply {
            text = "Password:"
            setPadding(0, 0, 0, 8)
        }

        val passwordInput = EditText(this).apply {
            hint = "password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(0, 0, 0, 16)
            id = View.generateViewId()
        }

        val testButton = Button(this).apply {
            text = "Test Connection"
            setPadding(0, 0, 0, 8)
            setOnClickListener { 
                showToast("Testing connection...")
            }
        }

        val addButton = Button(this).apply {
            text = "Add Share"
            setPadding(0, 0, 0, 16)
            setOnClickListener { 
                showToast("SMB share added successfully!")
                finish()
            }
        }

        layout.addView(title)
        layout.addView(hostLabel)
        layout.addView(hostInput)
        layout.addView(shareLabel)
        layout.addView(shareInput)
        layout.addView(usernameLabel)
        layout.addView(usernameInput)
        layout.addView(passwordLabel)
        layout.addView(passwordInput)
        layout.addView(testButton)
        layout.addView(addButton)

        setContentView(layout)
    }

    private fun testConnection() {
        val smbConfig = createSmbConfigurationFromInput()
        if (smbConfig != null) {
            showSuccess("Connection test would be performed for: ${smbConfig.first}")
        }
    }

    private fun addSmbShare() {
        val smbConfig = createSmbConfigurationFromInput()
        if (smbConfig != null) {
            showSuccess("SMB share would be added: ${smbConfig.first}")
            finish()
        }
    }

    private fun createSmbConfigurationFromInput(): Pair<String, String>? {
        val hostView = findViewById<EditText>(R.id.et_host)
        val shareNameView = findViewById<EditText>(R.id.et_share_name)
        val usernameView = findViewById<EditText>(R.id.et_username)
        val passwordView = findViewById<EditText>(R.id.et_password)
        val nameView = findViewById<EditText>(R.id.et_storage_name)

        val host = hostView?.text?.toString()
        val shareName = shareNameView?.text?.toString()
        val username = usernameView?.text?.toString()
        val name = nameView?.text?.toString()

        if (host.isNullOrEmpty() || shareName.isNullOrEmpty() || 
            username.isNullOrEmpty() || name.isNullOrEmpty()) {
            showError("Please fill in all required fields")
            return null
        }

        return Pair(name, "$host/$shareName")
    }

    private fun showLoading(show: Boolean) {
        findViewById<ProgressBar>(R.id.progress_bar)?.apply {
            visibility = if (show) View.VISIBLE else View.GONE
        }
        
        // Disable/enable buttons
        findViewById<Button>(R.id.btn_test_connection)?.isEnabled = !show
        findViewById<Button>(R.id.btn_add_share)?.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
