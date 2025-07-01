package com.samsung.android.app.networkstoragemanager.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samsung.android.app.networkstoragemanager.R

/**
 * Activity for browsing and managing root folders
 * Compatible with both Samsung My Files legacy integration and modern UI
 */
class BrowseRootFoldersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StorageAdapter
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
        // 1. smb:// scheme (from manifest intent filter)
        // 2. Bundle extras with browse/path data
        // 3. Specific action patterns for browsing storage
        return data?.scheme == "smb" ||
               extras?.containsKey("browseMode") == true ||
               extras?.containsKey("path") == true ||
               action == "com.samsung.myfiles.BROWSE_STORAGE" ||
               (action == Intent.ACTION_MAIN && intent.categories?.contains("android.intent.category.DEFAULT") == true)
    }

    private fun handleLegacyLaunch() {
        // For Samsung My Files compatibility, show simple feedback and return success
        showSimpleLegacyUI()
        
        // Log the intent details for debugging
        val data = intent.data
        val extras = intent.extras
        val message = "Samsung My Files Integration\n\n" +
                     "Scheme: ${data?.scheme} (smb browsing)\n" +
                     "Action: ${intent.action}\n" +
                     "Has extras: ${extras != null}\n\n" +
                     "This would browse root folders.\n" +
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
            text = "Root Folders Browser\n\nLaunched by Samsung My Files\nBrowsing available storage...\n\nFor full functionality, use:\n'Modern Storage Manager Test' app"
            textSize = 16f
            setPadding(32, 32, 32, 32)
        }
    }

    private fun createModernUI() {
        try {
            // Try to use the proper layout if it exists
            setContentView(R.layout.activity_browse_root_folders)
            setupModernUI()
            loadStorageLocations()
        } catch (e: Exception) {
            // Fallback to programmatic layout if resource is missing
            createProgrammaticLayout()
        }
    }

    private fun setupModernUI() {
        // Set up the action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Browse Root Folders"
        }

        // Set up RecyclerView
        recyclerView = findViewById(R.id.recycler_view_storage)
        adapter = StorageAdapter { storageName ->
            onStorageItemClicked(storageName)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Set up FAB
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add)?.setOnClickListener {
            showAddStorageMenu()
        }
    }

    private fun createProgrammaticLayout() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Browse Root Folders"
            textSize = 20f
            setPadding(0, 0, 0, 32)
        }

        val description = TextView(this).apply {
            text = "Manage your root storage locations and SMB/CIFS shares:"
            setPadding(0, 0, 0, 16)
        }

        // Mock storage items as clickable buttons
        val mockStorages = listOf(
            "Root Storage 1" to "/storage/root1",
            "SMB Share 1" to "192.168.1.100/share1",
            "Root Storage 2" to "/storage/root2"
        )

        mockStorages.forEach { (name, path) ->
            val storageButton = android.widget.Button(this).apply {
                text = "$name\n$path"
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    showToast("Selected: $name")
                }
            }
            layout.addView(storageButton)
        }

        val addButton = android.widget.Button(this).apply {
            text = "Add New Storage"
            setPadding(0, 16, 0, 0)
            setOnClickListener { 
                showAddStorageMenu()
            }
        }

        layout.addView(title)
        layout.addView(description)
        layout.addView(addButton)

        setContentView(layout)
    }

    private fun loadStorageLocations() {
        // Mock data for demonstration
        val mockStorages = listOf(
            "Root Storage 1" to "/storage/root1",
            "SMB Share 1" to "192.168.1.100/share1",
            "Root Storage 2" to "/storage/root2"
        )
        adapter.updateData(mockStorages)
        updateEmptyState(mockStorages.isEmpty())
    }

    private fun showAddStorageMenu() {
        val options = arrayOf("Add Root Location", "Add SMB/CIFS Share")
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Add Storage")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, AddRootLocationActivity::class.java))
                    1 -> startActivity(Intent(this, AddSmbShareActivity::class.java))
                }
            }
            .show()
    }

    private fun onStorageItemClicked(storageName: String) {
        val options = arrayOf("Edit", "Remove", "Test Connection")
        
        android.app.AlertDialog.Builder(this)
            .setTitle(storageName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showError("Edit functionality not yet implemented")
                    1 -> showSuccess("Would remove: $storageName")
                    2 -> showSuccess("Would test connection: $storageName")
                }
            }
            .show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        findViewById<View>(R.id.empty_state)?.visibility = 
            if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = 
            if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        findViewById<ProgressBar>(R.id.progress_bar)?.visibility = 
            if (show) View.VISIBLE else View.GONE
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

    override fun onResume() {
        super.onResume()
        // Refresh the list when returning to this activity
        loadStorageLocations()
    }
}

/**
 * Simple RecyclerView adapter for storage items
 */
class StorageAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<StorageAdapter.ViewHolder>() {

    private var items = listOf<Pair<String, String>>()

    fun updateData(newItems: List<Pair<String, String>>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_storage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tv_storage_name)
        private val typeText: TextView = itemView.findViewById(R.id.tv_storage_type)
        private val detailsText: TextView = itemView.findViewById(R.id.tv_storage_details)
        private val iconView: ImageView = itemView.findViewById(R.id.iv_storage_icon)

        fun bind(storage: Pair<String, String>) {
            nameText.text = storage.first
            detailsText.text = storage.second
            
            if (storage.second.startsWith("/")) {
                typeText.text = "Root Storage"
                iconView.setImageResource(R.drawable.ic_storage)
            } else {
                typeText.text = "SMB/CIFS Share"
                iconView.setImageResource(R.drawable.ic_network)
            }

            itemView.setOnClickListener {
                onItemClick(storage.first)
            }
        }
    }
}
