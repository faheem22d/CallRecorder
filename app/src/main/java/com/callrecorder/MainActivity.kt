package com.callrecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.callrecorder.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RecordingsAdapter

    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.PROCESS_OUTGOING_CALLS,
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()

        if (!allPermissionsGranted()) {
            showPermissionRationale()
        } else {
            loadRecordings()
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecordings()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "📞 Call Recorder"

        adapter = RecordingsAdapter(
            onPlay = { file -> playRecording(file) },
            onDelete = { file -> deleteRecording(file) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabRefresh.setOnClickListener {
            loadRecordings()
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show()
        }

        binding.tvStatus.text = if (allPermissionsGranted())
            "✅ Ready — calls will be recorded automatically"
        else
            "⚠️ Permissions required"
    }

    private fun loadRecordings() {
        val dir = getRecordingsDir()
        val files = dir.listFiles { f -> f.extension == "m4a" || f.extension == "3gp" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        adapter.submitList(files)

        binding.tvEmpty.visibility = if (files.isEmpty())
            android.view.View.VISIBLE else android.view.View.GONE

        binding.tvRecordingCount.text = "${files.size} recording(s) saved"
    }

    private fun playRecording(file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this@MainActivity,
                "${packageName}.provider",
                file
            )
            setDataAndType(uri, "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, "Play with..."))
        } catch (e: Exception) {
            Toast.makeText(this, "No audio player found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteRecording(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete Recording")
            .setMessage("Delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ ->
                file.delete()
                loadRecordings()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(
                "CallRecorder needs the following permissions:\n\n" +
                "• Microphone — to record audio\n" +
                "• Phone State — to detect calls\n" +
                "• Call Log — to get caller info\n\n" +
                "Please grant all permissions on the next screen."
            )
            .setPositiveButton("Grant") { _, _ -> requestPermissions() }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                binding.tvStatus.text = "✅ Ready — calls will be recorded automatically"
                loadRecordings()
            } else {
                Toast.makeText(
                    this,
                    "Some permissions denied. Recording may not work correctly.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        fun getRecordingsDir(context: android.content.Context): File {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "CallRecordings"
            )
            if (!dir.exists()) dir.mkdirs()
            return dir
        }
    }

    private fun getRecordingsDir(): File = Companion.getRecordingsDir(this)
}
