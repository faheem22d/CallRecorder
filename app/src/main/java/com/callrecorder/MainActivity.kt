package com.callrecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.callrecorder.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RecordingsAdapter
    private val PERMISSION_REQUEST_CODE = 101

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RecordingsAdapter(
            onPlay = { file ->
                Toast.makeText(this, "Playing: ${file.name}", Toast.LENGTH_SHORT).show()
            },
            onDelete = { file ->
                file.delete()
                loadRecordings()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabRefresh.setOnClickListener {
            loadRecordings()
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show()
        }

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
        } else {
            binding.tvStatus.text = "✅ Ready — calls will be recorded automatically"
            loadRecordings()
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecordings()
    }

    private fun loadRecordings() {
        val dir = getRecordingsDir(this)
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        adapter.submitList(files)
        binding.tvRecordingCount.text = "${files.size} recording(s) saved"
        binding.tvEmpty.visibility = if (files.isEmpty())
            android.view.View.VISIBLE else android.view.View.GONE
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
                binding.tvStatus.text = "⚠️ Some permissions denied"
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
}
