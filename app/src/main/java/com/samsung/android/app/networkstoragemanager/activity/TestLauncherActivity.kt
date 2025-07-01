package com.samsung.android.app.networkstoragemanager.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.app.networkstoragemanager.R

/**
 * Simple launcher activity for testing the modernized components
 */
class TestLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_launcher)
        
        setupUI()
    }

    private fun setupUI() {
        findViewById<Button>(R.id.btn_test_add_root)?.setOnClickListener {
            startActivity(Intent(this, AddRootLocationActivity::class.java))
        }

        findViewById<Button>(R.id.btn_test_add_smb)?.setOnClickListener {
            startActivity(Intent(this, AddSmbShareActivity::class.java))
        }

        findViewById<Button>(R.id.btn_test_browse)?.setOnClickListener {
            startActivity(Intent(this, BrowseRootFoldersActivity::class.java))
        }

        findViewById<Button>(R.id.btn_test_provider)?.setOnClickListener {
            testDocumentProvider()
        }
    }

    private fun testDocumentProvider() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.putExtra("android.provider.extra.SHOW_ADVANCED", true)
            startActivity(intent)
            Toast.makeText(this, "Opening document picker to test provider", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
